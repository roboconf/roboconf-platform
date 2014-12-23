/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.internal.lifecycle;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.agent.internal.misc.AgentUtils;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractLifeCycleManager {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String appName;
	private final IAgentClient messagingClient;


	/**
	 * Constructor.
	 * @param appName
	 */
	protected AbstractLifeCycleManager( String appName, IAgentClient messagingClient ) {
		this.appName = appName;
		this.messagingClient = messagingClient;
	}


	/**
	 * Builds the right handler depending on the current instance's state.
	 * @param instance an instance
	 * @param appName the application name
	 * @param messagingClient the messaging client
	 * @return a non-null manager to update the instance's life cycle
	 */
	public static AbstractLifeCycleManager build( Instance instance, String appName, IAgentClient messagingClient ) {

		AbstractLifeCycleManager result;
		switch( instance.getStatus()) {
		case DEPLOYED_STARTED:
			result = new DeployedStarted( appName, messagingClient );
			break;

		case DEPLOYED_STOPPED:
			result = new DeployedStopped( appName, messagingClient );
			break;

		case NOT_DEPLOYED:
			result = new NotDeployed( appName, messagingClient );
			break;

		default:
			result = new TransitiveStates( appName, messagingClient );
			break;
		}

		return result;
	}


	/**
	 * Undertakes the appropriate actions to change the state of an instance.
	 * @param instance the instance to work on
	 * @param plugin the plug-in associated with the instance's installer
	 * @param newStatus the target state to reach
	 * @param fileNameToFileContent the recipe resources (for deployment only)
	 * @throws IOException if something went wrong
	 * @throws PluginException if something went wrong
	 */
	public abstract void changeInstanceState(
			Instance instance, PluginInterface plugin,
			InstanceStatus newStatus , Map<String,byte[]> fileNameToFileContent )
	throws IOException, PluginException;


	/**
	 * Updates the status of an instance based on the imports.
	 * @param impactedInstance the instance whose imports may have changed
	 * @param plugin the plug-in to use to apply a concrete modification
	 * @param statusChanged The changed status of the instance that changed (eg. that provided new imports)
	 * @param importChanged The individual imports that changed
	 */
	public void updateStateFromImports( Instance impactedInstance, PluginInterface plugin, Import importChanged, InstanceStatus statusChanged )
	throws IOException, PluginException {

		// Do we have all the imports we need?
		boolean haveAllImports = ImportHelpers.hasAllRequiredImports( impactedInstance, this.logger );

		// Update the life cycle of this instance if necessary
		// Maybe we have something to start
		if( haveAllImports ) {
			if( impactedInstance.getStatus() == InstanceStatus.STARTING ) {

				// Start this instance
				plugin.start( impactedInstance );
				impactedInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, impactedInstance ));
				this.messagingClient.publishExports( impactedInstance );
				this.messagingClient.listenToRequestsFromOtherAgents( ListenerCommand.START, impactedInstance );

			} else if( impactedInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
				plugin.update( impactedInstance, importChanged, statusChanged );

			} else {
				this.logger.fine( InstanceHelpers.computeInstancePath( impactedInstance ) + " checked import changes but has nothing to update (1)." );
			}
		}

		// Or maybe we have something to stop
		else {
			if( impactedInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
				stopInstance( impactedInstance, plugin, true );

			} else {
				this.logger.fine( InstanceHelpers.computeInstancePath( impactedInstance ) + " checked import changes but has nothing to update (2)." );
			}
		}
	}


	/**
	 * Deploys an instance (prerequisite: NOT_DEPLOYED).
	 * @param instance the instance
	 * @param plugin the associated plug-in
	 * @param fileNameToFileContent a map containing resources for the plug-in
	 * @throws IOException if something went wrong
	 */
	void deploy( Instance instance, PluginInterface plugin, Map<String,byte[]> fileNameToFileContent )
	throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
			this.logger.fine( instancePath + " cannot be deployed. Prerequisite status: NOT_DEPLOYED (but was " + instance.getStatus() + ")." );

		} else if( instance.getParent() != null
				&& instance.getParent().getStatus() != InstanceStatus.DEPLOYED_STARTED
				&& instance.getParent().getStatus() != InstanceStatus.DEPLOYED_STOPPED ) {
			this.logger.fine( instancePath + " cannot be deployed because its parent is not deployed. Parent status: " + instance.getParent().getStatus() + "." );

		} else {
			this.logger.fine( "Deploying instance " + instancePath );

			// User reporting => deploying...
			instance.setStatus( InstanceStatus.DEPLOYING );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));

			// Clean up the potential remains of a previous installation
			AgentUtils.deleteInstanceResources( instance );

			// Copy the resources
			AgentUtils.copyInstanceResources( instance, fileNameToFileContent );

			// Invoke the plug-in
			try {
				plugin.deploy( instance );
				instance.setStatus( InstanceStatus.DEPLOYED_STOPPED );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));

			} catch( Exception e ) {
				this.logger.severe( "An error occured while deploying " + instancePath );
				Utils.logException( this.logger, e );

				instance.setStatus( InstanceStatus.NOT_DEPLOYED );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));
			}
		}
	}


	/**
	 * Undeploys an instance (prerequisite: DEPLOYED_STOPPED).
	 * @param instance the instance
	 * @param plugin the associated plug-in
	 * @throws IOException if something went wrong
	 */
	void undeploy( Instance instance, PluginInterface plugin )
	throws IOException {

		// Preliminary check
		String instancePath = InstanceHelpers.computeInstancePath( instance );
		if( instance.getStatus() != InstanceStatus.DEPLOYED_STOPPED ) {
			this.logger.fine( instancePath + " cannot be undeployed. Prerequisite status: DEPLOYED_STOPPED (but was " + instance.getStatus() + ")." );
			return;
		}

		// Children may have to be marked as stopped.
		// From a plug-in point of view, we only use the one for the given instance.
		// Children are SUPPOSED to be stopped immediately.
		List<Instance> instancesToUndeploy = InstanceHelpers.buildHierarchicalList( instance );
		Collections.reverse( instancesToUndeploy );

		// Update the statuses if necessary
		for( Instance i : instancesToUndeploy ) {
			if( i.getStatus() == InstanceStatus.NOT_DEPLOYED )
				continue;

			i.setStatus( InstanceStatus.UNDEPLOYING );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
			this.messagingClient.unpublishExports( i );
		}

		// Hypothesis: undeploying an instance undeploys its children too.
		InstanceStatus newStatus = InstanceStatus.NOT_DEPLOYED;
		try {
			plugin.undeploy( instance );

			// Delete files for undeployed instances
			for( Instance i : instancesToUndeploy )
				AgentUtils.deleteInstanceResources( i );

		} catch( PluginException e ) {
			this.logger.severe( "An error occured while undeploying " + InstanceHelpers.computeInstancePath( instance ));
			Utils.logException( this.logger, e );
			newStatus = InstanceStatus.DEPLOYED_STOPPED;
		}

		// Update the status of all the instances
		for( Instance i : instancesToUndeploy ) {
			// Prevent old imports from being resent later on
			i.getImports().clear();

			// Propagate the changes
			i.setStatus( newStatus );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
		}
	}


	/**
	 * Starts an instance (prerequisite: DEPLOYED_STOPPED).
	 * @param instance the instance
	 * @param plugin the associated plug-in
	 * @throws IOException if something went wrong
	 */
	void start( Instance instance, PluginInterface plugin )
	throws IOException {

		// Preliminary check
		String instancePath = InstanceHelpers.computeInstancePath( instance );
		if( instance.getStatus() != InstanceStatus.DEPLOYED_STOPPED ) {
			this.logger.fine( instancePath + " cannot be started. Prerequisite status: DEPLOYED_STOPPED (but was " + instance.getStatus() + ")." );

		} else if( instance.getParent() != null
				&& instance.getParent().getStatus() != InstanceStatus.DEPLOYED_STARTED ) {
			this.logger.fine( instancePath + " cannot be started because its parent is not started. Parent status: " + instance.getParent().getStatus() + "." );

		} else {
			// Perform the "start" operation
			try {
				instance.setStatus( InstanceStatus.STARTING );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));
				if( ImportHelpers.hasAllRequiredImports( instance, this.logger )) {
					updateStateFromImports( instance, plugin, null, InstanceStatus.STARTING );

				} else {
					this.logger.fine(
							"Instance " + InstanceHelpers.computeInstancePath( instance )
							+ " cannot be started, dependencies are missing. Requesting exports from other agents." );
					this.messagingClient.requestExportsFromOtherAgents( instance );
				}

			} catch( PluginException e ) {
				instance.setStatus( InstanceStatus.DEPLOYED_STOPPED );
				this.logger.severe( "An error occured while starting " + InstanceHelpers.computeInstancePath( instance ));
				Utils.logException( this.logger, e );
			}
		}
	}


	/**
	 * Stops an instance (prerequisite: DEPLOYED_STARTED).
	 * @param instance the instance
	 * @param plugin the associated plug-in
	 * @throws IOException if something went wrong
	 */
	void stop( Instance instance, PluginInterface plugin ) throws IOException {

		// Preliminary check
		String instancePath = InstanceHelpers.computeInstancePath( instance );
		if( instance.getStatus() != InstanceStatus.DEPLOYED_STARTED ) {
			this.logger.fine( instancePath + " cannot be stopped. Prerequisite status: DEPLOYED_STARTED (but was " + instance.getStatus() + ")." );
			return;
		}

		// Perform the "stop" operation
		stopInstance( instance, plugin, false );
	}


	/**
	 * Stops an instance.
	 * <p>
	 * If necessary, it will stop its children.
	 * </p>
	 *
	 * @param instance an instance (must be deployed and started)
	 * @param plugin the plug-in to use for concrete modifications
	 * @param isDueToImportsChange true if this method is called because an import changed, false it matches a manual life cycle change
	 * @throws IOException if something went wrong
	 */
	private void stopInstance( Instance instance, PluginInterface plugin, boolean isDueToImportsChange )
	throws IOException {

		this.logger.fine( "Stopping instance " + InstanceHelpers.computeInstancePath( instance ) + "..." );

		// Children may have to be stopped too.
		// From a plug-in point of view, we only use the one for the given instance.
		// Children are SUPPOSED to be stopped immediately.
		List<Instance> instancesToStop = InstanceHelpers.buildHierarchicalList( instance );
		Collections.reverse( instancesToStop );

		// Update the statuses if necessary
		for( Instance i : instancesToStop ) {
			if( i.getStatus() != InstanceStatus.DEPLOYED_STARTED
					&& i.getStatus() != InstanceStatus.STARTING )
				continue;

			if( i.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
				i.setStatus( InstanceStatus.STOPPING );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
			}

			this.messagingClient.listenToRequestsFromOtherAgents( ListenerCommand.STOP, i );
			this.messagingClient.unpublishExports( i );
		}

		// Stop the initial instance.
		// Even if the plugin invocation fails, we cannot consider these instances
		// to be reliable anymore. So, we keep on as if the operation went well.
		InstanceStatus newStatus = isDueToImportsChange ? InstanceStatus.STARTING : InstanceStatus.DEPLOYED_STOPPED;
		try {
			plugin.stop( instance );

		} catch( PluginException e ) {
			this.logger.severe( "An error occured while stopping/updating " + InstanceHelpers.computeInstancePath( instance ));
			Utils.logException( this.logger, e );
		}

		// In the case where the instances were stopped because of a change in "imports",
		// we remain in the starting phase, so that we can start automatically when the required
		// imports arrive.
		for( Instance i : instancesToStop ) {
			if( i.getStatus() != InstanceStatus.STOPPING
					&& i.getStatus() != InstanceStatus.STARTING )
				continue;

			i.setStatus( newStatus );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
		}
	}
}
