/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.roboconf.dm.internal.delegates;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.target.TargetResolver;
import net.roboconf.dm.management.ITargetResolver;
import net.roboconf.dm.management.ITargetResolver.Target;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.target.api.TargetException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceMngrDelegate {

	private static final Object LOCK = new Object();
	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Manager manager;
	ITargetResolver targetResolver;


	/**
	 * Constructor.
	 * @param manager the DM.
	 */
	public InstanceMngrDelegate( Manager manager ) {
		this.manager = manager;
		this.targetResolver = new TargetResolver();
	}


	/**
	 * Adds an instance.
	 * @param ma the managed application
	 * @param parentInstance the parent instance
	 * @param instance the instance to insert (not null)
	 * @throws ImpossibleInsertionException if the instance could not be added
	 */
	public void addInstance( ManagedApplication ma, Instance parentInstance, Instance instance )
	throws ImpossibleInsertionException, IOException {

		if( ! InstanceHelpers.tryToInsertChildInstance( ma.getApplication(), parentInstance, instance ))
			throw new ImpossibleInsertionException( instance.getName());

		this.logger.fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully added in " + ma.getName() + "." );
		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );

		// Store the message because we want to make sure the message is not lost
		ma.storeAwaitingMessage( instance, new MsgCmdSetScopedInstance( scopedInstance ));
	}


	/**
	 * Removes an instance.
	 * @param ma the managed application
	 * @param instance the instance to remove (not null)
	 * @throws UnauthorizedActionException if we try to remove an instance that seems to be running
	 * @throws IOException if an error occurred with the messaging
	 */
	public void removeInstance( ManagedApplication ma, Instance instance ) throws UnauthorizedActionException, IOException {

		for( Instance i : InstanceHelpers.buildHierarchicalList( instance )) {
			if( i.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( "Instances are still deployed or running. They cannot be removed in " + ma.getName() + "." );
		}

		// Whatever is the state of the agent, we try to send a message.
		MsgCmdRemoveInstance message = new MsgCmdRemoveInstance( instance );
		this.manager.send( ma, message, instance );

		if( instance.getParent() == null )
			ma.getApplication().getRootInstances().remove( instance );
		else
			instance.getParent().getChildren().remove( instance );

		this.logger.fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully removed in " + ma.getName() + "." );
	}


	/**
	 * Changes the state of an instance.
	 * @param ma the managed application
	 * @param instance the instance whose state must be updated
	 * @param newStatus the new status
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with a target
	 */
	public void changeInstanceState( ManagedApplication ma, Instance instance, InstanceStatus newStatus )
	throws IOException, TargetException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		this.logger.fine( "Trying to change the state of " + instancePath + " to " + newStatus + " in " + ma.getName() + "..." );

		if( InstanceHelpers.isTarget( instance )) {
			List<InstanceStatus> es = Arrays.asList(
					InstanceStatus.DEPLOYED_STARTED,
					InstanceStatus.DEPLOYING,
					InstanceStatus.STARTING,
					InstanceStatus.PROBLEM );

			if( newStatus == InstanceStatus.NOT_DEPLOYED
					&& es.contains( instance.getStatus()))
				undeployTarget( ma, instance );

			else if( instance.getStatus() == InstanceStatus.NOT_DEPLOYED
					&& newStatus == InstanceStatus.DEPLOYED_STARTED )
				deployTarget( ma, instance );

			else
				this.logger.warning( "Ignoring a request to update a scoped instance's state." );

		} else {
			Map<String,byte[]> instanceResources = null;
			if( newStatus == InstanceStatus.DEPLOYED_STARTED
					|| newStatus == InstanceStatus.DEPLOYED_STOPPED )
				instanceResources = ResourceUtils.storeInstanceResources( ma.getTemplateDirectory(), instance );

			MsgCmdChangeInstanceState message = new MsgCmdChangeInstanceState( instance, newStatus, instanceResources );
			this.manager.send( ma, message, instance );
			this.logger.fine( "A message was (or will be) sent to the agent to change the state of " + instancePath + " in " + ma.getName() + "." );
		}
	}


	/**
	 * Deploys and starts all the instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we deploy and start (can be null)
	 * <p>
	 * This instance and all its children will be deployed and started.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws IOException if a problem occurred with the messaging
	 */
	public void deployAndStartAll( ManagedApplication ma, Instance instance ) throws IOException {

		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Collections.singletonList(instance);
		else
			initialInstances = ma.getApplication().getRootInstances();

		boolean gotExceptions = false;
		for( Instance initialInstance : initialInstances ) {
			for( Instance i : InstanceHelpers.buildHierarchicalList( initialInstance )) {
				try {
					changeInstanceState( ma, i, InstanceStatus.DEPLOYED_STARTED );

				} catch( Exception e ) {
					Utils.logException( this.logger, e );
					gotExceptions = true;
				}
			}
		}

		if( gotExceptions ) {
			this.logger.info( "One or several errors occurred while deploying and starting instances." );
			throw new IOException( "One or several errors occurred while deploying and starting instances." );
		}
	}


	/**
	 * Stops all the started instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we stop (can be null)
	 * <p>
	 * This instance and all its children will be stopped.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws IOException if a problem occurred with the messaging
	 */
	public void stopAll( ManagedApplication ma, Instance instance ) throws IOException {

		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Collections.singletonList(instance);
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to stop all the instances, just the first children.
		// Stop does not mean anything for targets.
		boolean gotExceptions = false;
		for( Instance initialInstance : initialInstances ) {
			try {
				if( ! InstanceHelpers.isTarget( initialInstance ))
					changeInstanceState( ma, initialInstance, InstanceStatus.DEPLOYED_STOPPED );
				else for( Instance i : initialInstance.getChildren())
					changeInstanceState( ma, i, InstanceStatus.DEPLOYED_STOPPED );

			} catch( Exception e ) {
				Utils.logException( this.logger, e );
				gotExceptions = true;
			}
		}

		if( gotExceptions ) {
			this.logger.info( "One or several errors occurred while stopping instances." );
			throw new IOException( "One or several errors occurred while stopping instances." );
		}
	}


	/**
	 * Undeploys all the instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we undeploy (can be null)
	 * <p>
	 * This instance and all its children will be undeployed.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws IOException if a problem occurred with the messaging
	 */
	public void undeployAll( ManagedApplication ma, Instance instance ) throws IOException {

		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Collections.singletonList(instance);
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to undeploy all the instances, just the first instance
		boolean gotExceptions = false;
		for( Instance initialInstance : initialInstances ) {
			try {
				changeInstanceState( ma, initialInstance, InstanceStatus.NOT_DEPLOYED );

			} catch( Exception e ) {
				Utils.logException( this.logger, e );
				gotExceptions = true;
			}
		}

		if( gotExceptions ) {
			this.logger.info( "One or several errors occurred while undeploying instances." );
			throw new IOException( "One or several errors occurred while undeploying instances." );
		}
	}


	/**
	 * Restores instances states for a given application.
	 * @param ma a managed application (not null)
	 */
	public void restoreInstanceStates( ManagedApplication ma ) {

		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			try {
				// Not associated with a VM? => Everything must be not deployed.
				String machineId = rootInstance.data.get( Instance.MACHINE_ID );
				if( machineId == null ) {
					rootInstance.data.remove( Instance.IP_ADDRESS );
					rootInstance.data.remove( Instance.TARGET_ACQUIRED );
					for( Instance i : InstanceHelpers.buildHierarchicalList( rootInstance ))
						i.setStatus( InstanceStatus.NOT_DEPLOYED );

					continue;
				}

				// Not a running VM? => Everything must be not deployed.
				Target target = this.targetResolver.findTargetHandler( this.manager.getTargetHandlers(), ma, rootInstance );
				Map<String,String> targetProperties = new HashMap<>( target.getProperties());
				targetProperties.putAll( rootInstance.data );

				if( ! target.getHandler().isMachineRunning( targetProperties, machineId )) {
					rootInstance.data.remove( Instance.IP_ADDRESS );
					rootInstance.data.remove( Instance.MACHINE_ID );
					rootInstance.data.remove( Instance.TARGET_ACQUIRED );
					for( Instance i : InstanceHelpers.buildHierarchicalList( rootInstance ))
						i.setStatus( InstanceStatus.NOT_DEPLOYED );
				}

				// Otherwise, ask the agent to resent the states
				else {
					rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
					this.manager.getMessagingClient().sendMessageToAgent( ma.getApplication(), rootInstance, new MsgCmdSendInstances());
				}

			} catch( Exception e ) {
				this.logger.severe( "Could not request states for agent " + rootInstance.getName() + " (I/O exception)." );
				Utils.logException( this.logger, e );
			}
		}
	}


	/**
	 * @param targetResolver the target resolver to set
	 */
	public void setTargetResolver( ITargetResolver targetResolver ) {
		this.targetResolver = targetResolver;
	}


	/**
	 * Deploys a scoped instance.
	 * @param ma the managed application
	 * @param scopedInstance the instance to deploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with the target handler
	 */
	private void deployTarget( ManagedApplication ma, Instance scopedInstance ) throws TargetException, IOException {

		String path = InstanceHelpers.computeInstancePath( scopedInstance );
		this.logger.fine( "Deploying scoped instance '" + path + "' in " + ma.getName() + "..." );

		// We must prevent the concurrent creation of several VMs for a same root instance.
		// See #80.
		synchronized( LOCK ) {
			if( scopedInstance.data.get( Instance.TARGET_ACQUIRED ) == null ) {
				scopedInstance.data.put( Instance.TARGET_ACQUIRED, "yes" );
			} else {
				this.logger.finer( "Scoped instance '" + path + "' is already under deployment. This redundant request is dropped." );
				return;
			}
		}

		// If the VM creation was already done, then its machine ID has already been set.
		// It does not mean the VM is already configured, it may take some time.
		String machineId = scopedInstance.data.get( Instance.MACHINE_ID );
		if( machineId != null ) {
			this.logger.fine( "Deploy action for instance " + path + " is cancelled in " + ma.getName() + ". Already associated with a machine." );
			return;
		}

		InstanceStatus initialStatus = scopedInstance.getStatus();
		try {
			scopedInstance.setStatus( InstanceStatus.DEPLOYING );
			MsgCmdSetScopedInstance msg = new MsgCmdSetScopedInstance( scopedInstance );
			this.manager.send( ma, msg, scopedInstance );

			Target target = this.targetResolver.findTargetHandler( this.manager.getTargetHandlers(), ma, scopedInstance );
			Map<String,String> targetProperties = new HashMap<>( target.getProperties());
			targetProperties.putAll( scopedInstance.data );

			String scopedInstancePath = InstanceHelpers.computeInstancePath( scopedInstance );
			machineId = target.getHandler().createMachine(
					targetProperties,
					this.manager.getMessagingConfiguration(),
					scopedInstancePath, ma.getName());

			scopedInstance.data.put( Instance.MACHINE_ID, machineId );
			this.logger.fine( "Scoped instance " + path + "'s deployment was successfully requested in " + ma.getName() + ". Machine ID: " + machineId );

			target.getHandler().configureMachine(
					targetProperties,
					this.manager.getMessagingConfiguration(),
					machineId, scopedInstancePath, ma.getName(), scopedInstance );

			this.logger.fine( "Scoped instance " + path + "'s configuration is on its way in " + ma.getName() + "." );

		} catch( TargetException | IOException e ) {
			this.logger.severe( "Failed to deploy scoped instance '" + path + "' in " + ma.getName() + ". " + e.getMessage());
			Utils.logException( this.logger, e );

			// The creation failed, remove the lock for another retry
			synchronized( LOCK ) {
				scopedInstance.data.remove( Instance.TARGET_ACQUIRED );
			}

			// Restore the state and propagate the exception
			scopedInstance.setStatus( initialStatus );
			throw e;
		}
	}


	/**
	 * Undeploys a scoped instance.
	 * @param ma the managed application
	 * @param scopedInstance the instance to undeploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with the target handler
	 */
	private void undeployTarget( ManagedApplication ma, Instance scopedInstance ) throws TargetException, IOException {

		String path = InstanceHelpers.computeInstancePath( scopedInstance );
		this.logger.fine( "Undeploying scoped instance '" + path + "' in " + ma.getName() + "..." );
		InstanceStatus initialStatus = scopedInstance.getStatus();
		try {
			// Terminate the machine...
			// ...  and notify other agents this agent was killed.
			this.logger.fine( "Agent '" + path + "' is about to be deleted in " + ma.getName() + "." );
			Target target = this.targetResolver.findTargetHandler( this.manager.getTargetHandlers(), ma, scopedInstance );
			String machineId = scopedInstance.data.remove( Instance.MACHINE_ID );
			if( machineId != null ) {
				Map<String,String> targetProperties = new HashMap<String,String>( target.getProperties());
				targetProperties.putAll( scopedInstance.data );

				target.getHandler().terminateMachine( targetProperties, machineId );
				this.manager.getMessagingClient().propagateAgentTermination( ma.getApplication(), scopedInstance );
			}

			this.logger.fine( "Agent '" + path + "' was successfully deleted in " + ma.getName() + "." );
			for( Instance i : InstanceHelpers.buildHierarchicalList( scopedInstance )) {
				i.setStatus( InstanceStatus.NOT_DEPLOYED );
				// DM won't send old imports upon restart...
				i.getImports().clear();
			}

			// Remove useless data for the configuration backup
			scopedInstance.data.remove( Instance.IP_ADDRESS );
			scopedInstance.data.remove( Instance.TARGET_ACQUIRED );
			this.logger.fine( "Scoped instance " + path + "'s undeployment was successfully requested in " + ma.getName() + "." );

		} catch( TargetException | IOException e ) {
			scopedInstance.setStatus( initialStatus );
			this.logger.severe( "Failed to undeploy scoped instance '" + path + "' in " + ma.getName() + ". " + e.getMessage());
			Utils.logException( this.logger, e );
			throw e;

		}
	}
}
