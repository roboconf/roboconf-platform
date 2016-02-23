/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.IRandomMngr;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IAutonomicMngr;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.events.EventType;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdUpdateProbeConfiguration;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class InstancesMngrImpl implements IInstancesMngr {

	private static final Object LOCK = new Object();
	private final Logger logger = Logger.getLogger( getClass().getName());

	private final IMessagingMngr messagingMngr;
	private final INotificationMngr notificationMngr;
	private final ITargetsMngr targetsMngr;
	private final IRandomMngr randomMngr;
	private IAutonomicMngr autonomicMngr;

	private ITargetHandlerResolver targetHandlerResolver;


	/**
	 * Constructor.
	 * @param targetsMngr
	 * @param messagingMngr
	 * @param notificationMngr
	 * @param randomMngr
	 */
	public InstancesMngrImpl(
			IMessagingMngr messagingMngr,
			INotificationMngr notificationMngr,
			ITargetsMngr targetsMngr,
			IRandomMngr randomMngr ) {

		this.targetsMngr = targetsMngr;
		this.messagingMngr = messagingMngr;
		this.notificationMngr = notificationMngr;
		this.randomMngr = randomMngr;
	}


	/**
	 * @param targetHandlerResolver the targetHandlerResolver to set
	 */
	public void setTargetHandlerResolver( ITargetHandlerResolver targetHandlerResolver ) {
		this.targetHandlerResolver = targetHandlerResolver;
	}


	/**
	 * @param autonomicMngr the autonomicMngr to set
	 */
	public void setRuleBasedHandler( IAutonomicMngr autonomicMngr ) {
		this.autonomicMngr = autonomicMngr;
	}


	@Override
	public void addInstance( ManagedApplication ma, Instance parentInstance, Instance instance )
	throws ImpossibleInsertionException, IOException {

		// Insert it, if possible
		this.messagingMngr.checkMessagingConfiguration();
		if( ! InstanceHelpers.tryToInsertChildInstance( ma.getApplication(), parentInstance, instance ))
			throw new ImpossibleInsertionException( instance.getName());

		// Generate values for random variables, if any
		this.randomMngr.generateRandomValues( ma.getApplication(), instance );

		// Send it to the agent
		this.logger.fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully added in " + ma.getName() + "." );
		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );

		// Store the message because we want to make sure the message is not lost
		ma.storeAwaitingMessage( instance, new MsgCmdAddInstance( scopedInstance ));

		ConfigurationUtils.saveInstances( ma );
		this.notificationMngr.instance( instance, ma.getApplication(), EventType.CREATED );
	}


	@Override
	public void instanceWasUpdated( Instance instance, ManagedApplication ma ) {

		this.notificationMngr.instance( instance, ma.getApplication(), EventType.CHANGED );
		ConfigurationUtils.saveInstances( ma );
	}


	@Override
	public void removeInstance( ManagedApplication ma, Instance instance )
	throws UnauthorizedActionException, IOException {

		this.messagingMngr.checkMessagingConfiguration();
		for( Instance i : InstanceHelpers.buildHierarchicalList( instance )) {
			if( i.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( "Instances are still deployed or running. They cannot be removed in " + ma.getName() + "." );
		}

		// Whatever is the state of the agent, we try to send a message.
		MsgCmdRemoveInstance message = new MsgCmdRemoveInstance( instance );
		this.messagingMngr.sendMessageSafely( ma, instance, message );

		// Remove it from the model
		if( instance.getParent() == null ) {
			ma.getApplication().getRootInstances().remove( instance );
			this.autonomicMngr.notifyVmWasDeletedByHand( instance );

		} else {
			instance.getParent().getChildren().remove( instance );
		}

		// Release random values, if any
		this.randomMngr.releaseRandomValues( ma.getApplication(), instance );

		// Persist the model and notify
		this.logger.fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully removed in " + ma.getName() + "." );
		ConfigurationUtils.saveInstances( ma );
		this.notificationMngr.instance( instance, ma.getApplication(), EventType.DELETED );
	}


	@Override
	public void restoreInstanceStates( ManagedApplication ma, TargetHandler targetHandler ) {

		for( Instance scopedInstance : InstanceHelpers.findAllScopedInstances( ma.getApplication())) {
			try {
				// Not associated with a VM? => Everything must be not deployed.
				String machineId = scopedInstance.data.get( Instance.MACHINE_ID );
				if( machineId == null ) {
					markScopedInstanceAsNotDeployed( scopedInstance );
					continue;
				}

				// Find the target handler
				String scopedInstancePath = InstanceHelpers.computeInstancePath( scopedInstance );
				Map<String,String> targetProperties = this.targetsMngr.findRawTargetProperties( ma.getApplication(), scopedInstancePath );
				targetProperties.putAll( scopedInstance.data );

				TargetHandler readTargetHandler = null;
				try {
					readTargetHandler = this.targetHandlerResolver.findTargetHandler( targetProperties );

				} catch( Exception e ) {
					// nothing
				}

				// If it is not the right handler, ignore this scoped instance.
				if( readTargetHandler == null
						|| ! Objects.equals( targetHandler.getTargetId(), readTargetHandler.getTargetId()))
					continue;

				// Not a running VM? => Everything must be not deployed.
				if( ! targetHandler.isMachineRunning( targetProperties, machineId )) {
					markScopedInstanceAsNotDeployed( scopedInstance );
				}

				// Otherwise, ask the agent to resent the states under this scoped instance
				else {
					scopedInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
					this.messagingMngr.sendMessageDirectly( ma, scopedInstance, new MsgCmdSendInstances());
				}

			} catch( Exception e ) {
				this.logger.severe( "Could not request states for agent " + scopedInstance.getName() + " (I/O exception)." );
				Utils.logException( this.logger, e );
			}
		}
	}


	@Override
	public void resynchronizeAgents( ManagedApplication ma ) throws IOException {

		this.messagingMngr.checkMessagingConfiguration();
		this.logger.fine( "Resynchronizing agents in " + ma.getName() + "..." );
		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			if( rootInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED )
				this.messagingMngr.sendMessageDirectly( ma, rootInstance, new MsgCmdResynchronize());
		}

		this.logger.fine( "Requests were sent to resynchronize agents in " + ma.getName() + "." );
	}


	@Override
	public void changeInstanceState( ManagedApplication ma, Instance instance, InstanceStatus newStatus )
	throws IOException, TargetException {

		this.messagingMngr.checkMessagingConfiguration();
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
				this.logger.warning( "Ignoring a request to update a scoped instance's state. New state was " + newStatus );

		} else {
			Map<String,byte[]> instanceResources = null;
			if( newStatus == InstanceStatus.DEPLOYED_STARTED
					|| newStatus == InstanceStatus.DEPLOYED_STOPPED )
				instanceResources = ResourceUtils.storeInstanceResources( ma.getTemplateDirectory(), instance );

			MsgCmdChangeInstanceState message = new MsgCmdChangeInstanceState( instance, newStatus, instanceResources );
			this.messagingMngr.sendMessageSafely( ma, instance, message );
			this.logger.fine( "A message was (or will be) sent to the agent to change the state of " + instancePath + " in " + ma.getName() + "." );
		}
	}


	@Override
	public void deployAndStartAll( ManagedApplication ma, Instance instance ) throws IOException {

		this.messagingMngr.checkMessagingConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Collections.singletonList(instance);
		else
			initialInstances = ma.getApplication().getRootInstances();

		List<Exception> exceptions = new ArrayList<> ();
		for( Instance initialInstance : initialInstances ) {
			for( Instance i : InstanceHelpers.buildHierarchicalList( initialInstance )) {
				try {
					changeInstanceState( ma, i, InstanceStatus.DEPLOYED_STARTED );

				} catch( Exception e ) {
					exceptions.add( e );
				}
			}
		}

		processExceptions( this.logger, exceptions, "One or several errors occurred while deploying and starting instances." );
	}


	@Override
	public void stopAll( ManagedApplication ma, Instance instance ) throws IOException {

		this.messagingMngr.checkMessagingConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Collections.singletonList(instance);
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to stop all the instances, just the first children.
		// Stop does not mean anything for targetsMngr.
		List<Exception> exceptions = new ArrayList<> ();
		for( Instance initialInstance : initialInstances ) {
			try {
				if( ! InstanceHelpers.isTarget( initialInstance ))
					changeInstanceState( ma, initialInstance, InstanceStatus.DEPLOYED_STOPPED );
				else for( Instance i : initialInstance.getChildren())
					changeInstanceState( ma, i, InstanceStatus.DEPLOYED_STOPPED );

			} catch( Exception e ) {
				exceptions.add( e );
			}
		}

		processExceptions( this.logger, exceptions, "One or several errors occurred while stopping instances." );
	}


	@Override
	public void undeployAll( ManagedApplication ma, Instance instance ) throws IOException {

		this.messagingMngr.checkMessagingConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Collections.singletonList(instance);
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to undeploy all the instances, just the first instance
		List<Exception> exceptions = new ArrayList<> ();
		for( Instance initialInstance : initialInstances ) {
			try {
				changeInstanceState( ma, initialInstance, InstanceStatus.NOT_DEPLOYED );

			} catch( Exception e ) {
				exceptions.add( e );
			}
		}

		processExceptions( this.logger, exceptions, "One or several errors occurred while undeploying instances." );
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
			// Send the model
			scopedInstance.setStatus( InstanceStatus.DEPLOYING );
			MsgCmdSetScopedInstance msgModel = new MsgCmdSetScopedInstance(
					scopedInstance,
					ma.getApplication().getExternalExports(),
					ma.getApplication().applicationBindings );

			this.messagingMngr.sendMessageSafely( ma, scopedInstance, msgModel );

			// Send the probe files (if any)
			Map<String,byte[]> probeResources = ResourceUtils.storeInstanceProbeResources( ma.getDirectory(), scopedInstance );
			if( ! probeResources.isEmpty()) {
				MsgCmdUpdateProbeConfiguration msgProbes = new MsgCmdUpdateProbeConfiguration( scopedInstance, probeResources );
				this.messagingMngr.sendMessageSafely( ma, scopedInstance, msgProbes );
			}

			// Prepare the creation
			Map<String,String> targetProperties = this.targetsMngr.lockAndGetTarget( ma.getApplication(), scopedInstance );
			targetProperties.putAll( scopedInstance.data );

			TargetHandler targetHandler = this.targetHandlerResolver.findTargetHandler( targetProperties );
			Map<String,String> messagingConfiguration = this.messagingMngr.getMessagingClient().getConfiguration();
			String scopedInstancePath = InstanceHelpers.computeInstancePath( scopedInstance );

			// FIXME: there can be many problems here.
			// Not sure we handle all the possible problems correctly.
			try {
				machineId = targetHandler.createMachine( targetProperties, messagingConfiguration, scopedInstancePath, ma.getName());

			} catch( TargetException e ) {
				this.targetsMngr.unlockTarget( ma.getApplication(), scopedInstance );
				throw e;
			}

			scopedInstance.data.put( Instance.MACHINE_ID, machineId );
			this.logger.fine( "Scoped instance " + path + "'s deployment was successfully requested in " + ma.getName() + ". Machine ID: " + machineId );

			// If the configuration fails, keep the lock on the target.
			// This will prevent us from having ghost VMs with no target.
			targetHandler.configureMachine(
					targetProperties, messagingConfiguration,
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
		String machineId = scopedInstance.data.remove( Instance.MACHINE_ID );
		try {
			// Terminate the machine...
			// ...  and notify other agents this agent was killed.
			this.logger.fine( "Agent '" + path + "' is about to be deleted in " + ma.getName() + "." );
			if( machineId != null ) {
				String scopedInstancePath = InstanceHelpers.computeInstancePath( scopedInstance );
				Map<String,String> targetProperties = this.targetsMngr.findRawTargetProperties( ma.getApplication(), scopedInstancePath );
				targetProperties.putAll( scopedInstance.data );

				TargetHandler targetHandler = this.targetHandlerResolver.findTargetHandler( targetProperties );
				targetHandler.terminateMachine( targetProperties, machineId );

				this.targetsMngr.unlockTarget( ma.getApplication(), scopedInstance );
				this.messagingMngr.getMessagingClient().propagateAgentTermination( ma.getApplication(), scopedInstance );
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
			scopedInstance.data.remove( Instance.RUNNING_FROM );
			this.logger.fine( "Scoped instance " + path + "'s undeployment was successfully requested in " + ma.getName() + "." );

		} catch( TargetException | IOException e ) {
			scopedInstance.setStatus( initialStatus );
			scopedInstance.data.put( Instance.MACHINE_ID, machineId );

			this.logger.severe( "Failed to undeploy scoped instance '" + path + "' in " + ma.getName() + ". " + e.getMessage());
			Utils.logException( this.logger, e );
			throw e;

		} finally {
			ma.removeAwaitingMessages( scopedInstance );
		}
	}


	/**
	 * Reports a set of collections (for bulk actions).
	 * @param logger
	 * @param exceptions
	 * @param msg
	 * @throws IOException
	 */
	static void processExceptions( Logger logger, List<Exception> exceptions, String msg ) throws IOException {

		for( Exception e : exceptions )
			Utils.logException( logger, e );

		if( exceptions.size() > 0 ) {
			logger.info( msg );
			throw new IOException( msg );
		}
	}


	/**
	 * Updates a scoped instance to be marked as not deployed.
	 * <p>
	 * This includes its children.
	 * </p>
	 *
	 * @param scopedInstance a non-null scoped instance
	 */
	private void markScopedInstanceAsNotDeployed( Instance scopedInstance ) {

		scopedInstance.data.remove( Instance.IP_ADDRESS );
		scopedInstance.data.remove( Instance.MACHINE_ID );
		scopedInstance.data.remove( Instance.TARGET_ACQUIRED );
		for( Instance i : InstanceHelpers.buildHierarchicalList( scopedInstance ))
			i.setStatus( InstanceStatus.NOT_DEPLOYED );
	}
}
