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

package net.roboconf.agent.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.roboconf.agent.AgentData;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportAdd;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRequest;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceAdd;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceDeploy;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRemove;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRestore;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStart;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStop;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceUndeploy;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * The class (thread) in charge of processing messages received by the agent.
 * @author Vincent Zurczak - Linagora
 */
public class AgentMessageProcessor extends AbstractMessageProcessor {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final PluginManager pluginManager;
	private final IAgentClient messagingClient;
	private final String ipAddress, appName;

	private Instance rootInstance;


	/**
	 * Constructor.
	 * @param threadName
	 * @param agentData
	 * @param pluginManager
	 * @param messagingClient
	 */
	public AgentMessageProcessor(
			String threadName,
			AgentData agentData,
			PluginManager pluginManager,
			IAgentClient messagingClient ) {

		super( threadName );
		this.messagingClient = messagingClient;
		this.pluginManager = pluginManager;

		this.ipAddress = agentData.getIpAddress();
		this.appName = agentData.getApplicationName();
	}



	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.AbstractMessageProcessor
	 * #processMessage(net.roboconf.messaging.messages.Message)
	 */
	@Override
	protected void processMessage( Message message ) {

		// Otherwise process the message
		try {
			if( message instanceof MsgCmdInstanceAdd )
				processMsgInstanceAdd((MsgCmdInstanceAdd) message );

			else if( message instanceof MsgCmdInstanceRemove )
				processMsgInstanceRemove((MsgCmdInstanceRemove) message );

			else if( message instanceof MsgCmdInstanceDeploy )
				processMsgInstanceDeploy((MsgCmdInstanceDeploy) message );

			else if( message instanceof MsgCmdInstanceUndeploy )
				processMsgInstanceUndeploy((MsgCmdInstanceUndeploy) message );

			else if( message instanceof MsgCmdInstanceStart )
				processMsgInstanceStart((MsgCmdInstanceStart) message );

			else if( message instanceof MsgCmdInstanceStop )
				processMsgInstanceStop((MsgCmdInstanceStop) message );

			else if( message instanceof MsgCmdImportAdd )
				processMsgImportAdd((MsgCmdImportAdd) message );

			else if( message instanceof MsgCmdImportRemove )
				processMsgImportRemove((MsgCmdImportRemove) message );

			else if( message instanceof MsgCmdImportRequest )
				processMsgImportRequest((MsgCmdImportRequest) message );

			else if( message instanceof MsgCmdInstanceRestore )
				processMsgInstanceRestore((MsgCmdInstanceRestore) message );

			else
				this.logger.warning( getName() + " got an undetermined message to process. " + message.getClass().getName());

		} catch( IOException e ) {
			this.logger.severe( "A problem occurred with the messaging. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));

		}  catch( PluginException e ) {
			this.logger.severe( "A problem occurred with a plug-in. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 * Sends the local states to the DM.
	 * @param message the initial request
	 * @throws IOException if an error occurred with the messaging
	 */
	void processMsgInstanceRestore( MsgCmdInstanceRestore message ) throws IOException {
		for( Instance i : InstanceHelpers.buildHierarchicalList( this.rootInstance ))
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, i ));
	}



	/**
	 * Adds an instance to the local model.
	 * @param msg the message to process
	 * @return true if an insertion was made, false otherwise
	 * @throws IOException if an error occurred with the messaging
	 */
	boolean processMsgInstanceAdd( MsgCmdInstanceAdd msg ) throws IOException {
		boolean result = false;

		Instance newInstance = msg.getInstanceToAdd();
		String parentInstancePath = msg.getParentInstancePath();
		List<Instance> instancesToProcess = new ArrayList<Instance> ();

		// Insert a child when there is no root instance...
		if( parentInstancePath != null
				&& this.rootInstance == null ) {
			this.logger.severe( "A request to change the root instance was received. Request to add " + newInstance.getName() + " is dropped." );
		}

		// Set the root instance
		else if( parentInstancePath == null ) {
			if( this.rootInstance == null ) {
				this.logger.fine( "Setting the root instance." );
				this.rootInstance = newInstance;
				instancesToProcess.addAll( InstanceHelpers.buildHierarchicalList( this.rootInstance ));
				result = true;

			} else {
				this.logger.severe( "A request to change the root instance was received. Request to add " + newInstance.getName() + " is dropped." );
			}
		}

		// Insert a child
		else {
			Instance parentInstance = InstanceHelpers.findInstanceByPath( this.rootInstance, parentInstancePath );
			if( parentInstance == null )
				this.logger.severe( "No instance matched " + parentInstancePath + " on the agent. Request to add " + newInstance.getName() + " is dropped." );
			else if( ! InstanceHelpers.tryToInsertChildInstance( null, parentInstance, newInstance ))
				this.logger.severe( "Instance " + newInstance.getName() + " could not be inserted under " + parentInstancePath + ". Request is dropped." );
			else {
				this.logger.fine( "Instance " + newInstance.getName() + " was successfully under " + parentInstancePath + "." );
				instancesToProcess.add( newInstance );
				result = true;
			}
		}

		// Configure the messaging
		for( Instance instanceToProcess : instancesToProcess ) {
			VariableHelpers.updateNetworkVariables( instanceToProcess.getExports(), this.ipAddress );
			this.messagingClient.listenToExportsFromOtherAgents( ListenerCommand.START, instanceToProcess );
			this.messagingClient.requestExportsFromOtherAgents( instanceToProcess );
		}

		return result;
	}


	/**
	 * Removes an instance to the local model.
	 * @param msg the message to process
	 * @return true if an removal was made, false otherwise
	 * @throws IOException if an error occurred with the messaging
	 */
	boolean processMsgInstanceRemove( MsgCmdInstanceRemove msg ) throws IOException {
		boolean result = false;

		// Remove the instance
		Instance instance = InstanceHelpers.findInstanceByPath( this.rootInstance, msg.getInstancePath());
		if( instance == null ) {
			this.logger.severe( "No instance matched " + msg.getInstancePath() + " on the agent. Request to remove it from the model is dropped." );

		} else if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
			this.logger.severe( "Instance " + msg.getInstancePath() + " cannot be removed. Instance status: " + instance.getStatus() + "." );
			// We do not have to check children's status.
			// We cannot have a parent in NOT_DEPLOYED and a child in STARTED (as an example).

		} else if( instance.getParent() != null ) {
			instance.getParent().getChildren().remove( instance );
			this.logger.fine( "Child instance " + msg.getInstancePath() + " was removed from the model." );
			result = true;

		} else {
			this.rootInstance = null;
			this.logger.fine( "Root instance " + msg.getInstancePath() + " was set to null." );
			result = true;
		}

		// Configure the messaging
		if( instance != null ) {
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceRemoved( this.appName, instance ));
			for( Instance instanceToProcess : InstanceHelpers.buildHierarchicalList( instance )) {
				this.messagingClient.listenToExportsFromOtherAgents( ListenerCommand.STOP, instanceToProcess );
			}
		}

		return result;
	}


	/**
	 * Deploys an instance.
	 * @param msg the message to process
	 * @return true if the deployment was successful, false otherwise
	 * @throws IOException if an error occurred with the messaging or while manipulating the file system
	 */
	boolean processMsgInstanceDeploy( MsgCmdInstanceDeploy msg ) throws IOException {
		boolean result = false;

		PluginInterface plugin;
		Instance instance = InstanceHelpers.findInstanceByPath( this.rootInstance, msg.getInstancePath());
		if( instance == null ) {
			this.logger.severe( "No instance matched " + msg.getInstancePath() + " on the agent. Request to deploy it is dropped." );

		} else if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
			this.logger.severe( "Instance " + msg.getInstancePath() + " cannot be deployed. Instance status: " + instance.getStatus() + "." );

		} else if( instance.getParent() != null
				&& instance.getParent().getStatus() != InstanceStatus.DEPLOYED_STARTED
				&& instance.getParent().getStatus() != InstanceStatus.DEPLOYED_STOPPED ) {
			this.logger.warning( "Instance " + msg.getInstancePath() + " cannot be deployed because its parent is not deployed. Parent status: " + instance.getParent().getStatus() + "." );

		} else if(( plugin = this.pluginManager.findPlugin( instance, this.logger )) == null ) {
			this.logger.severe( "No plug-in was found to deploy " + msg.getInstancePath() + "." );

		} else {
			this.logger.fine( "Deploying instance " + msg.getInstancePath() + "." );

			// User reporting => deploying...
			instance.setStatus( InstanceStatus.DEPLOYING );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));

			// Clean up the potential remains of a previous installation
			AgentUtils.deleteInstanceResources( instance, plugin.getPluginName());

			// Copy the resources
			AgentUtils.copyInstanceResources( instance, plugin.getPluginName(), msg.getFileNameToFileContent());

			// Invoke the plug-in
			try {
				PluginManager.initializePluginForInstance( instance, this.pluginManager.getExecutionLevel());
				plugin.deploy( instance );
				instance.setStatus( InstanceStatus.DEPLOYED_STOPPED );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));
				result = true;

			} catch( Exception e ) {
				this.logger.severe( "An error occured while deploying " + msg.getInstancePath());
				this.logger.finest( Utils.writeException( e ));

				instance.setStatus( InstanceStatus.NOT_DEPLOYED );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));
			}
		}

		return result;
	}


	/**
	 * Un-deploys an instance.
	 * @param msg the message to process
	 * @return true if the un-deployment was successful, false otherwise
	 * @throws IOException if an error occurred with the messaging
	 */
	boolean processMsgInstanceUndeploy( MsgCmdInstanceUndeploy msg ) throws IOException {
		boolean result = false;

		String instancePath = msg.getInstancePath();
		Instance instance;
		PluginInterface plugin;

		// No root instance
		if(( instance = InstanceHelpers.findInstanceByPath( this.rootInstance, msg.getInstancePath())) == null ) {
			this.logger.severe( "No instance matched " + msg.getInstancePath() + " on the agent. Request to undeploy it is dropped." );
		}

		// If it is already undeployed, do nothing
		else if( instance.getStatus() == InstanceStatus.NOT_DEPLOYED ) {
			this.logger.info( "Instance " + instancePath + " is already un-deployed. Undeploy request is dropped." );
		}

		// Is it the root instance to undeploy?
		else if( instance.getParent() == null ) {
			this.logger.severe( "Request to undeploy the root instance is dropped." );
		}

		// Do we have the right plug-in?
		else if(( plugin = this.pluginManager.findPlugin( instance, this.logger )) == null ) {
			this.logger.severe( "No plug-in was found to undeploy " + msg.getInstancePath() + "." );
		}

		// Otherwise, process it
		else {

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
				result = true;

			} catch( Exception e ) {
				this.logger.severe( "An error occured while undeploying " + msg.getInstancePath());
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

		return result;
	}


	/**
	 * Starts an instance (only if all its imports are resolved).
	 * @param msg the message to process
	 * @return true if the instance was successfully started, false otherwise
	 * @throws IOException if an error occurred with the messaging
	 */
	boolean processMsgInstanceStart( MsgCmdInstanceStart msg ) throws IOException {
		boolean result = false;

		PluginInterface plugin;
		Instance instance = InstanceHelpers.findInstanceByPath( this.rootInstance, msg.getInstancePath());
		if( instance == null ) {
			this.logger.severe( "No instance matched " + msg.getInstancePath() + " on the agent. Request to start it is dropped." );

		} else if( instance.getStatus() != InstanceStatus.DEPLOYED_STOPPED
				&& instance.getStatus() != InstanceStatus.STARTING ) {

			// Starting may be blocked because of concurrency issues.
			// So, we allow starting something which is already starting...
			this.logger.info( "Invalid status for instance " + msg.getInstancePath() + ". Status = "
					+ instance.getStatus() + ". Start request is dropped." );

		} else if(( plugin = this.pluginManager.findPlugin( instance, this.logger )) == null ) {
			this.logger.severe( "No plug-in was found to start " + msg.getInstancePath() + "." );

		} else {
			try {
				instance.setStatus( InstanceStatus.STARTING );
				this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));
				if( ImportHelpers.hasAllRequiredImports( instance, this.logger )) {
					updateStateFromImports( instance, plugin, null, InstanceStatus.STARTING );
					result = true;

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

		return result;
	}


	/**
	 * Stops an instance (and its children).
	 * @param msg the message to process
	 * @return true if the instance was successfully stopped, false otherwise
	 * @throws IOException if an error occurred with the messaging
	 */
	boolean processMsgInstanceStop( MsgCmdInstanceStop msg ) throws IOException {
		boolean result = false;

		PluginInterface plugin;
		Instance instance = InstanceHelpers.findInstanceByPath( this.rootInstance, msg.getInstancePath());
		if( instance == null ) {
			this.logger.severe( "No instance matched " + msg.getInstancePath() + " on the agent. Request to stop it is dropped." );

		} else if( instance.getStatus() != InstanceStatus.DEPLOYED_STARTED
				&& instance.getStatus() != InstanceStatus.STARTING ) {

			this.logger.info( "Invalid status for instance " + msg.getInstancePath() + ". Status = "
					+ instance.getStatus() + ". Stop request is dropped." );

		} else if(( plugin = this.pluginManager.findPlugin( instance, this.logger )) == null ) {
			this.logger.severe( "No plug-in was found to stop " + msg.getInstancePath() + "." );

		} else {
			try {
				stopInstance( instance, plugin, false );
				result = true;

			} catch( Exception e ) {
				this.logger.severe( "An error occured while stopping " + InstanceHelpers.computeInstancePath( instance ));
				this.logger.finest( Utils.writeException( e ));
			}
		}

		return result;
	}


	/**
	 * Publishes its exports when required.
	 * @param msg the message process
	 * @throws IOException if an error occurred with the messaging
	 */
	void processMsgImportRequest( MsgCmdImportRequest msg ) throws IOException {

		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.rootInstance )) {
			if( instance.getStatus() == InstanceStatus.DEPLOYED_STARTED )
				this.messagingClient.publishExports( instance, msg.getComponentOrFacetName());
		}
	}


	/**
	 * Removes (if necessary) an import from the model instances.
	 * @param msg the message process
	 * @throws IOException if an error occurred with the messaging
	 * @throws PluginException if an error occurred with a plug-in
	 */
	void processMsgImportRemove( MsgCmdImportRemove msg ) throws IOException, PluginException {

		// Go through all the instances to see which ones are impacted
		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.rootInstance )) {

			Set<String> importPrefixes = VariableHelpers.findPrefixesForImportedVariables( instance );
			if( ! importPrefixes.contains( msg.getComponentOrFacetName()))
				continue;

			// Is there an import to remove?
			Collection<Import> imports = instance.getImports().get( msg.getComponentOrFacetName());
			Import toRemove = ImportHelpers.findImportByExportingInstance( imports, msg.getRemovedInstancePath());
			if( toRemove == null )
				continue;

			// Remove the import and publish an update to the DM
			imports.remove( toRemove );
			this.logger.fine( "Removing import from " + InstanceHelpers.computeInstancePath( instance )
					+ ". Removed exporting instance: " + msg.getRemovedInstancePath());

			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));

			// Update the life cycle if necessary
			PluginInterface plugin = this.pluginManager.findPlugin( instance, this.logger );
			if( plugin != null )
				updateStateFromImports( instance, plugin, toRemove, InstanceStatus.DEPLOYED_STOPPED );
		}
	}


	/**
	 * Receives and adds (if necessary) a new import to the model instances.
	 * @param msg the message process
	 * @throws IOException if an error occurred with the messaging
	 * @throws PluginException if an error occurred with a plug-in
	 */
	void processMsgImportAdd( MsgCmdImportAdd msg ) throws IOException, PluginException {

		// Go through all the instances to see which ones need an update
		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.rootInstance )) {

			// This instance does not depends on it
			Set<String> importPrefixes = VariableHelpers.findPrefixesForImportedVariables( instance );
			if( ! importPrefixes.contains( msg.getComponentOrFacetName()))
				continue;

			// If an instance depends on its component, make sure it does not add itself to the imports.
			// Example: MongoDB may depend on other MongoDB instances.
			if( Utils.areEqual(
					InstanceHelpers.computeInstancePath( instance ),
					msg.getAddedInstancePath()))
				continue;

			// Create the right import
			Import imp = ImportHelpers.buildTailoredImport( instance, msg.getAddedInstancePath(), msg.getExportedVariables());

			// Add the import and publish an update to the DM
			this.logger.fine( "Adding import to " + InstanceHelpers.computeInstancePath( instance ) + ". New import: " + imp );
			ImportHelpers.addImport( instance, msg.getComponentOrFacetName(), imp );
			this.messagingClient.sendMessageToTheDm( new MsgNotifInstanceChanged( this.appName, instance ));

			// Update the life cycle if necessary
			PluginInterface plugin = this.pluginManager.findPlugin( instance, this.logger );
			if( plugin != null )
				updateStateFromImports( instance, plugin, imp, InstanceStatus.DEPLOYED_STARTED );
		}
	}


	/**
	 * Updates the status of an instance based on the imports.
	 * @param impactedInstance the instance whose imports may have changed
	 * @param plugin the plug-in to use to apply a concrete modification
	 * @param statusChanged The changed status of the instance that changed (eg. that provided new imports)
	 * @param importChanged The individual imports that changed
	 */
	void updateStateFromImports( Instance impactedInstance, PluginInterface plugin, Import importChanged, InstanceStatus statusChanged ) throws IOException, PluginException {

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


	/**
	 * Stops an instance.
	 * <p>
	 * If necessary, it will stop its children.
	 * </p>
	 *
	 * @param instance an instance (must be deployed and started)
	 * @param plugin the plug-in to use for concrete modifications
	 */
	private void stopInstance( Instance instance, PluginInterface plugin, boolean isDueToImportsChange ) throws PluginException, IOException {
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
