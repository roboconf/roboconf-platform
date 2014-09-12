/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
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
				// FIXME: there should be a way to determine whether an update is necessary
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



	void deploy( Instance instance, PluginInterface plugin, Map<String,byte[]> fileNameToFileContent )
	throws IOException, PluginException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		if( instance.getParent() != null
				&& instance.getParent().getStatus() != InstanceStatus.DEPLOYED_STARTED
				&& instance.getParent().getStatus() != InstanceStatus.DEPLOYED_STOPPED ) {
			this.logger.warning( instancePath + " cannot be deployed because its parent is not deployed. Parent status: " + instance.getParent().getStatus() + "." );

		} else {
			this.logger.fine( "Deploying instance " + instancePath );

			// User reporting => deploying...
			instance.setStatus( InstanceStatus.DEPLOYING );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));

			// Clean up the potential remains of a previous installation
			AgentUtils.deleteInstanceResources( instance, plugin.getPluginName());

			// Copy the resources
			AgentUtils.copyInstanceResources( instance, plugin.getPluginName(), fileNameToFileContent );

			// Invoke the plug-in
			try {
				plugin.initialize( instance );
				plugin.deploy( instance );
				instance.setStatus( InstanceStatus.DEPLOYED_STOPPED );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));

			} catch( Exception e ) {
				this.logger.severe( "An error occured while deploying " + instancePath );
				this.logger.finest( Utils.writeException( e ));

				instance.setStatus( InstanceStatus.NOT_DEPLOYED );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));
			}
		}
	}


	void undeploy( Instance instance, PluginInterface plugin )
	throws IOException, PluginException {

		// Children may have to be marked as stopped.
		// From a plug-in point of view, we only use the one for the given instance.
		// Children are SUPPOSED to be stopped immediately.
		List<Instance> instancesToStop = InstanceHelpers.buildHierarchicalList( instance );
		Collections.reverse( instancesToStop );

		// Update the statuses if necessary
		for( Instance i : instancesToStop ) {
			if( i.getStatus() == InstanceStatus.NOT_DEPLOYED )
				continue;

			i.setStatus( InstanceStatus.UNDEPLOYING );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
			this.messagingClient.unpublishExports( i );
		}

		// Undeploy the initial instance
		try {
			plugin.undeploy( instance );

		} catch( Exception e ) {
			this.logger.severe( "An error occured while undeploying " + InstanceHelpers.computeInstancePath( instance ));
			this.logger.finest( Utils.writeException( e ));
			// Do not interrupt, clean everything - see below
		}

		// Update the status of all the instances
		for( Instance i : instancesToStop ) {
			// Delete files for undeployed instances
			String pluginName = i.getComponent().getInstallerName();
			AgentUtils.deleteInstanceResources( i, pluginName );

			// Prevent old imports from being resent later on
			i.getImports().clear();

			// Propagate the changes
			i.setStatus( InstanceStatus.NOT_DEPLOYED );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
		}
	}


	void start( Instance instance, PluginInterface plugin )
	throws IOException, PluginException {

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
			this.logger.finest( Utils.writeException( e ));
		}
	}


	void stop( Instance instance, PluginInterface plugin )
	throws IOException, PluginException {

		try {
			stopInstance( instance, plugin, false );

		} catch( Exception e ) {
			this.logger.severe( "An error occured while stopping " + InstanceHelpers.computeInstancePath( instance ));
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 * Stops an instance.
	 * <p>
	 * If necessary, it will stop its children.
	 * </p>
	 *
	 * @param instance an instance (must be deployed and started)
	 * @param plugin the plug-in to use for concrete modifications
	 */
	private void stopInstance( Instance instance, PluginInterface plugin, boolean isDueToImportsChange )
	throws PluginException, IOException {
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

			i.setStatus( InstanceStatus.STOPPING );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
			this.messagingClient.listenToRequestsFromOtherAgents( ListenerCommand.STOP, i );
			this.messagingClient.unpublishExports( i );
		}

		// Stop the initial instance
		plugin.stop( instance );

		// In the case where the instances were stopped because of a change in "imports",
		// we remain in the starting phase, so that we can start automatically when the required
		// imports arrive.
		InstanceStatus newStatus = isDueToImportsChange ? InstanceStatus.STARTING : InstanceStatus.DEPLOYED_STOPPED;
		for( Instance i : instancesToStop ) {
			if( i.getStatus() != InstanceStatus.STOPPING )
				continue;

			i.setStatus( newStatus );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
		}
	}
}
