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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.roboconf.agent.AgentData;
import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportAdd;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRequest;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineReadyToBeDeleted;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceAdd;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceDeploy;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRemove;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStart;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStop;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceUndeploy;
import net.roboconf.messaging.utils.MessagingUtils;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Agent implements IMessageProcessor {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String agentName;
	private final PluginManager pluginManager;

	private Instance rootInstance;
	private MessagingService messagingService;
	private AgentData agentData;



	/**
	 * Constructor.
	 * @param agentName the agent name
	 */
	public Agent( String agentName, PluginManager pluginManager ) {
		this.agentName = agentName;
		this.pluginManager = pluginManager;
	}


	/**
	 * @param messagingService the messaging service
	 */
	public void setMessagingService( MessagingService messagingService ) {
		this.messagingService = messagingService;
	}


	/**
	 * @return the agentName
	 */
	public String getAgentName() {
		return this.agentName;
	}


	/**
	 * @param agentData the agentData to set
	 */
	public void setAgentData( AgentData agentData ) {
		this.agentData = agentData;
	}


	/**
	 * @return the rootInstance
	 */
	public Instance getRootInstance() {
		return this.rootInstance;
	}


	/**
	 * Performs an action on an instance.
	 * @param action an action
	 * @param instancePath the instance's path
	 * @param originalMessage the original message
	 * FIXME: this method is a mess!
	 */
	public void performAction( ApplicationAction action, String instancePath, Message originalMessage ) {
		Instance instance;
		PluginInterface plugin;

		try {
			if( this.rootInstance == null ) {

				// Incomplete model but request to undeploy the root?
				// Stop everything and indicate to the DM it can delete the VM.
				if( action == ApplicationAction.undeploy
						&& InstanceHelpers.countInstances( instancePath ) == 1 ) {

					MsgNotifMachineReadyToBeDeleted msg = new MsgNotifMachineReadyToBeDeleted( this.rootInstance.getName());
					this.messagingService.publish( true, MessagingUtils.buildRoutingKeyToDm(), msg );
					this.messagingService.stopHeartBeatTimer();

				} else {
					this.logger.info( "The agent's model has not yet been initialized. Request " + originalMessage.getClass().getSimpleName() + " is dropped." );
				}

			} else if(( instance = InstanceHelpers.findInstanceByPath( this.rootInstance, instancePath )) == null ) {
				this.logger.severe( "Instance " + instancePath + " was not found on this agent." );

			} else if(( plugin = this.pluginManager.findPlugin( instance, this.logger )) != null ) {
				switch( action ) {
				case deploy:
					if( instance.getStatus() == InstanceStatus.NOT_DEPLOYED ) {

						// Clean up the potential remains of a previous installation
						deleteInstanceResources( instance, plugin.getPluginName());

						updateAndNotifyNewStatus( instance, InstanceStatus.DEPLOYING );
						copyInstanceResources(
								instance, plugin.getPluginName(),
								((MsgCmdInstanceDeploy) originalMessage).getFileNameToFileContent());

						try {
							PluginManager.initializePluginForInstance( instance, this.pluginManager.getExecutionLevel());
							plugin.deploy( instance );
							updateAndNotifyNewStatus( instance, InstanceStatus.DEPLOYED_STOPPED );

						} catch( Exception e ) {
							this.logger.severe( "An error occured while deploying " + instancePath );
							this.logger.finest( Utils.writeException( e ));

							updateAndNotifyNewStatus( instance, InstanceStatus.NOT_DEPLOYED );
						}

					} else {
						this.logger.info(
								"Invalid status for instance " + instancePath + ". Status = "
								+ instance.getStatus() + ". Deploy request is dropped." );
					}
					break;

				case start:
					if( instance.getStatus() == InstanceStatus.DEPLOYED_STOPPED ) {
						updateAndNotifyNewStatus( instance, InstanceStatus.STARTING );
						updateStateFromImports( instance, plugin );

					} else {
						this.logger.info(
								"Invalid status for instance " + instancePath + ". Status = "
								+ instance.getStatus() + ". Start request is dropped." );
					}
					break;

				case stop:
					if( instance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
						stopInstance( instance, plugin, false );

					} else if( instance.getStatus() == InstanceStatus.STARTING ) {
						for( Instance i : InstanceHelpers.buildHierarchicalList( instance )) {
							if( i.getStatus() == InstanceStatus.STARTING )
								updateAndNotifyNewStatus( i, InstanceStatus.DEPLOYED_STOPPED );
						}

					} else {
						this.logger.info(
								"Invalid status for instance " + instancePath + ". Status = "
								+ instance.getStatus() + ". Stop request is dropped." );
					}
					break;

				case undeploy:
					if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
						undeployInstance( instance, plugin );

					} else {
						this.logger.info(
								"Invalid status for instance " + instancePath + ". Status = "
								+ instance.getStatus() + ". Undeploy request is dropped." );
					}
					break;

				case remove:
					String filterName = MessagingUtils.buildRoutingKeyToDm();
					if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
						this.logger.severe( "Instance " + instancePath + " cannot be removed. Instance status: " + instance.getStatus() + "." );

					} else if( instance.getParent() != null ) {
						// Remove the instance
						instance.getParent().getChildren().remove( instance );
						this.logger.fine( "Child instance " + instancePath + " was removed from the model." );

						// Stop listening messages
						this.messagingService.configureInstanceMessaging( instance, false );

						// Send a message to confirm the removal
						MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( instance );
						this.messagingService.publish( true, filterName, msg );

					} else {
						this.rootInstance = null;
						this.logger.fine( "Root instance " + instancePath + " was set to null." );

						MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( instance );
						this.messagingService.publish( true, filterName, msg );
					}
					break;

				default:
					break;
				}
			}

		} catch( Exception e ) {
			this.logger.severe( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 *
	 * @param instance
	 * @throws Exception
	 */
	public void update( Instance instance ) throws Exception {
		PluginInterface plugin = this.pluginManager.findPlugin( instance, this.logger );
		if( plugin != null )
			plugin.update( instance );
	}


	/**
	 * Adds an instance to the local model.
	 * @param parentInstancePath the parent instance path (null to add a root instance)
	 * @param newInstance the new instance to add
	 * @throws Exception if the instance could not be added
	 */
	public void addInstance( String parentInstancePath, Instance newInstance ) throws Exception {

		// Update the network exports
		if( newInstance != null )
			VariableHelpers.updateNetworkVariables( newInstance.getExports(), this.agentData.getIpAddress());

		// Root instance
		if( parentInstancePath == null ) {
			if( this.rootInstance == null ) {

				// Update the model
				this.rootInstance = newInstance;
				updateAndNotifyNewStatus( newInstance, InstanceStatus.DEPLOYED_STARTED );

				// Start listening
				try {
					for( Instance instance : InstanceHelpers.buildHierarchicalList( this.rootInstance )) {
						if( instance.getParent() == null )
							continue;

						this.messagingService.configureInstanceMessaging( instance, true );
						VariableHelpers.updateNetworkVariables( instance.getExports(), this.agentData.getIpAddress());
					}

				} catch( IOException e ) {
					this.logger.severe( "Messaging could not be initialized for the instance " + InstanceHelpers.computeInstancePath( newInstance ));
					this.logger.finest( Utils.writeException( e ));
				}

			} else {
				this.logger.severe( "A request to change the root instance was received. Request to add " + newInstance.getName() + " is dropped." );
			}
		}

		// Error
		else if( this.rootInstance == null ) {
			this.logger.severe( "A request to insert a child instance was received, but the root instance on the agent is null. Request to add " + newInstance.getName() + " is dropped." );
		}

		// Child instance
		else {
			// Update the model
			Instance parentInstance = InstanceHelpers.findInstanceByPath( this.rootInstance, parentInstancePath );
			if( parentInstance == null )
				this.logger.severe( "No instance matched " + parentInstancePath + " on the agent. Request to add " + newInstance.getName() + " is dropped." );
			else if( ! InstanceHelpers.tryToInsertChildInstance( null, parentInstance, newInstance ))
				this.logger.severe( "Instance " + newInstance.getName() + " could not be inserted under " + parentInstancePath + ". Request is dropped." );

			// Start listening
			try {
				this.messagingService.configureInstanceMessaging( newInstance, true );

			} catch( IOException e ) {
				this.logger.severe( "Messaging could not be initialized for the instance " + InstanceHelpers.computeInstancePath( newInstance ));
				this.logger.finest( Utils.writeException( e ));
			}
		}
	}


	/**
	 * Processes a message (dispatch method).
	 * @param message a message (not null)
	 */
	@Override
	public void processMessage( Message message ) {

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

			else
				this.logger.warning( this.agentName + ": got an undetermined message to process. " + message.getClass().getName());

		} catch( Exception e ) {
			e.printStackTrace();
			this.logger.severe( "A problem occurred while processing a message on the agent " + this.agentName + ". " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public String toString() {
		return this.agentName != null ? this.agentName : super.toString();
	}


	private void processMsgImportRequest( MsgCmdImportRequest msg ) throws IOException {

		// Go through all the instances to see if one
		// of them owns the required variable prefix
		String name = msg.getComponentOrFacetName();
		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.rootInstance )) {
			if( instance.getStatus() != InstanceStatus.DEPLOYED_STARTED )
				continue;

			Set<String> exportPrefixes = VariableHelpers.findExportedVariablePrefixes( instance );
			if( ! exportPrefixes.contains( name ))
				continue;

			MsgCmdImportAdd newMsg = new MsgCmdImportAdd( name, instance.getName(), instance.getExports());
			this.messagingService.publishExportOrImport( name, newMsg, MessagingService.THOSE_THAT_EXPORT );
		}
	}


	private void processMsgImportRemove( MsgCmdImportRemove msg ) throws Exception {

		// Go through all the instances to see which ones are impacted
		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.rootInstance )) {

			Set<String> importPrefixes = VariableHelpers.findImportedVariablePrefixes( instance );
			if( ! importPrefixes.contains( msg.getComponentOrFacetName()))
				continue;

			// Is there an import to remove?
			Collection<Import> imports = instance.getImports().get( msg.getComponentOrFacetName());
			if( imports == null )
				continue;

			Import imp = new Import( msg.getRemovedInstancePath());
			if( ! imports.remove( imp ))
				continue;

			// Remove the import and publish an update to the DM
			this.logger.fine(
					"Removing import from " + InstanceHelpers.computeInstancePath( instance )
					+ ". Removed exporting instance: " + msg.getRemovedInstancePath());

			this.messagingService.publish(
					true,
					MessagingUtils.buildRoutingKeyToDm(),
					new MsgNotifInstanceChanged( instance ));

			// Update the life cycle if necessary
			PluginInterface plugin = this.pluginManager.findPlugin( instance, this.logger );
			if( plugin != null )
				updateStateFromImports( instance, plugin );
		}
	}


	private void processMsgImportAdd( MsgCmdImportAdd msg ) throws Exception {

		// Create the import
		Import imp = new Import(
				msg.getAddedInstancePath(),
				msg.getExportedVariables());

		// Go through all the instances to see which ones need an update
		for( Instance instance : InstanceHelpers.buildHierarchicalList( this.rootInstance )) {

			// This instance does not depends on it
			Set<String> importPrefixes = VariableHelpers.findImportedVariablePrefixes( instance );
			if( ! importPrefixes.contains( msg.getComponentOrFacetName()))
				continue;

			// If an instance depends on its component, make sure it does not add itself to the imports.
			// Example: MongoDB may depend on other MongoDB instances.
			if( Utils.areEqual(
					InstanceHelpers.computeInstancePath( instance ),
					msg.getAddedInstancePath()))
				continue;

			// Add the import and publish an update to the DM
			this.logger.fine( "Adding import to " + InstanceHelpers.computeInstancePath( instance ) + ". New import: " + imp );
			instance.addImport( msg.getComponentOrFacetName(), imp );
			this.messagingService.publish(
					true,
					MessagingUtils.buildRoutingKeyToDm(),
					new MsgNotifInstanceChanged( instance ));

			// Update the life cycle if necessary
			PluginInterface plugin = this.pluginManager.findPlugin( instance, this.logger );
			if( plugin != null )
				updateStateFromImports( instance, plugin );
		}
	}


	private void processMsgInstanceAdd( MsgCmdInstanceAdd msg ) {

		Instance newInstance = msg.getInstanceToAdd();
		String parentInstancePath = msg.getParentInstancePath();

		this.logger.fine( "Adding instance " + newInstance.getName() + " under " + parentInstancePath + "." );
		try {
			addInstance( parentInstancePath, newInstance );

		} catch( Exception e ) {
			this.logger.severe( "The instance could not be added. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	private void processMsgInstanceRemove( MsgCmdInstanceRemove msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Removing instance " + instancePath + "." );
		performAction( ApplicationAction.remove, instancePath, msg );
	}


	private void processMsgInstanceDeploy( MsgCmdInstanceDeploy msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Deploying instance " + instancePath + "." );
		performAction( ApplicationAction.deploy, instancePath, msg );
	}


	private void processMsgInstanceUndeploy( MsgCmdInstanceUndeploy msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Undeploying instance " + instancePath + "." );
		performAction( ApplicationAction.undeploy, instancePath, msg );
	}


	private void processMsgInstanceStart( MsgCmdInstanceStart msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Starting instance " + instancePath + "." );
		performAction( ApplicationAction.start, instancePath, msg );
	}


	private void processMsgInstanceStop( MsgCmdInstanceStop msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Stopping instance " + instancePath + "." );
		performAction( ApplicationAction.stop, instancePath, msg );
	}


	/**
	 * Updates the status of an instance and notifies the DM of this change.
	 * @param instance
	 * @param newStatus
	 * @throws IOException
	 */
	private void updateAndNotifyNewStatus( Instance instance, InstanceStatus newStatus ) throws IOException {
		instance.setStatus( newStatus );
		this.messagingService.publish(
				true,
				MessagingUtils.buildRoutingKeyToDm(),
				new MsgNotifInstanceChanged( instance ));
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
	private void stopInstance( Instance instance, PluginInterface plugin, boolean isDueToImportsChange ) throws Exception {

		// Children may have to be stopped too.
		// From a plug-in point of view, we only use the one for the given instance.
		// Children are supposed to be stopped immediately.
		List<Instance> instancesToStop = InstanceHelpers.buildHierarchicalList( instance );
		Collections.reverse( instancesToStop );

		// Update the statuses if necessary
		for( Instance i : instancesToStop ) {
			if( i.getStatus() != InstanceStatus.DEPLOYED_STARTED )
				continue;

			// Update the statuses
			updateAndNotifyNewStatus( instance, InstanceStatus.STOPPING );

			// Inform other agents this instance was removed
			for( String facetOrComponentName : VariableHelpers.findExportedVariablePrefixes( instance )) {
				MsgCmdImportRemove msg = new MsgCmdImportRemove(
						facetOrComponentName,
						InstanceHelpers.computeInstancePath( instance ));
				this.messagingService.publishExportOrImport( facetOrComponentName, msg, MessagingService.THOSE_THAT_EXPORT );
			}
		}

		// Stop the initial instance - if it makes sense!
		if( instance.getParent() != null )
			plugin.stop( instance );

		// Indicate everything is stopped
		updateAndNotifyNewStatus( instance, InstanceStatus.DEPLOYED_STOPPED );

		// In the case where the instances were stopped because of a change in "imports",
		// we remain in the starting phase, so that we can start automatically when the required
		// imports arrive.
		InstanceStatus newStatus = isDueToImportsChange ? InstanceStatus.STARTING : InstanceStatus.DEPLOYED_STOPPED;
		for( Instance i : instancesToStop )
			updateAndNotifyNewStatus( i, newStatus );
	}


	private void undeployInstance( Instance instance, PluginInterface plugin ) throws Exception {

		// Children may have to be marked as stopped.
		// From a plug-in point of view, we only use the one for the given instance.
		// Children are supposed to be stopped immediately.
		List<Instance> instancesToStop = InstanceHelpers.buildHierarchicalList( instance );
		Collections.reverse( instancesToStop );

		// Update the statuses if necessary
		for( Instance i : instancesToStop ) {
			if( i.getStatus() == InstanceStatus.NOT_DEPLOYED )
				continue;

			// Update the statuses
			updateAndNotifyNewStatus( i, InstanceStatus.UNDEPLOYING );

			// Inform other agents this instance was removed
			for( String facetOrComponentName : VariableHelpers.findExportedVariablePrefixes( i )) {
				MsgCmdImportRemove msg = new MsgCmdImportRemove(
						facetOrComponentName,
						InstanceHelpers.computeInstancePath( i ));
				this.messagingService.publishExportOrImport( facetOrComponentName, msg, MessagingService.THOSE_THAT_EXPORT );
			}
		}

		// Undeploy the initial instance - if it makes sense!
		if( instance.getParent() != null )
			plugin.undeploy( instance );

		// Update the status of all the instances
		for( Instance i : instancesToStop ) {
			// Delete files for undeployed instances
			deleteInstanceResources( i, plugin.getPluginName());
			updateAndNotifyNewStatus( i, InstanceStatus.NOT_DEPLOYED );
		}

		// If the instance is a root instance, signal to the DM it is ready to be deleted
		if( instance.equals( this.rootInstance )) {
			MsgNotifMachineReadyToBeDeleted msg = new MsgNotifMachineReadyToBeDeleted( this.rootInstance.getName());
			this.messagingService.publish( true, MessagingUtils.buildRoutingKeyToDm(), msg );
			this.messagingService.stopHeartBeatTimer();
		}
	}


	/**
	 * Updates the status of an instance based on the imports.
	 * @param impactedInstance the instance whose imports may have changed
	 * @param plugin the plug-in to use to apply a concrete modification
	 */
	private void updateStateFromImports( Instance impactedInstance, PluginInterface plugin ) throws Exception {

		// Do we have all the imports we need?
		boolean haveAllImports = true;
		for( String facetOrComponentName : VariableHelpers.findImportedVariablePrefixes( impactedInstance )) {
			Collection<Import> imports = impactedInstance.getImports().get( facetOrComponentName );
			if( imports != null && ! imports.isEmpty())
				continue;

			haveAllImports = false;
			this.logger.fine( InstanceHelpers.computeInstancePath( impactedInstance ) + " is still missing dependencies '" + facetOrComponentName + ".*'." );
			break;
		}

		// Update the life cycle of this instance if necessary
		// Maybe we have something to start
		if( haveAllImports ) {
			if( impactedInstance.getStatus() == InstanceStatus.STARTING ) {

				// Start this instance
				plugin.start( impactedInstance );
				updateAndNotifyNewStatus( impactedInstance, InstanceStatus.DEPLOYED_STARTED );

				// Inform other agents this instance was removed
				for( String facetOrComponentName : VariableHelpers.findExportedVariablePrefixes( impactedInstance )) {

					// FIXME: maybe we should filter the map to only keep the required variables. For security?
					Map<String,String> instanceExports = InstanceHelpers.getExportedVariables( impactedInstance );
					VariableHelpers.updateNetworkVariables( instanceExports, this.agentData.getIpAddress());
					MsgCmdImportAdd msg = new MsgCmdImportAdd(
							facetOrComponentName,
							InstanceHelpers.computeInstancePath( impactedInstance ),
							instanceExports );

					this.messagingService.publishExportOrImport( facetOrComponentName, msg, MessagingService.THOSE_THAT_EXPORT );
				}

			} else if( impactedInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
				// FIXME: there should be a way to determine whether an update is necessary
				plugin.update( impactedInstance );

			} else {
				this.logger.fine( InstanceHelpers.computeInstancePath( impactedInstance ) + " checked import changes but has nothing to update." );
			}
		}

		// Or maybe we have something to stop
		else {
			if( impactedInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
				stopInstance( impactedInstance, plugin, true );

			} else {
				this.logger.fine( InstanceHelpers.computeInstancePath( impactedInstance ) + " checked import changes but has nothing to update." );
			}
		}
	}



	private void copyInstanceResources( Instance instance, String pluginName, Map<String,byte[]> fileNameToFileContent )
	throws IOException {

		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( instance, pluginName );
		if( ! dir.exists()
				&& ! dir.mkdirs())
			throw new IOException( this.agentName + " could not create directory " + dir.getAbsolutePath());

		for( Map.Entry<String,byte[]> entry : fileNameToFileContent.entrySet()) {

			File f = new File( dir, entry.getKey());
			if( ! f.getParentFile().exists()
					&& ! f.getParentFile().mkdirs())
				throw new IOException( this.agentName + " could not create directory " + dir.getAbsolutePath());

			ByteArrayInputStream in = new ByteArrayInputStream( entry.getValue());
			Utils.copyStream( in, f );
		}
	}


	private void deleteInstanceResources( Instance instance, String pluginName )
	throws IOException {

		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( instance, pluginName );
		Utils.deleteFilesRecursively( dir );
	}
}
