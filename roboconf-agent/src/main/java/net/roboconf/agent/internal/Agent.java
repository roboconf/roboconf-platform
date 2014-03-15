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
import java.util.logging.Logger;

import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportAdd;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportNotification;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
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

	private Instance rootInstance;
	private MessagingService messagingService;
	private final PluginManager pluginManager;


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
	 * @return the rootInstance
	 */
	public Instance getRootInstance() {
		return this.rootInstance;
	}


	/**
	 * Performs an action on an instance.
	 * @param action an action
	 * @param instancePath the instance's path
	 */
	public void performAction( ApplicationAction action, String instancePath ) {
		Instance instance;
		PluginInterface plugin;

		if(( instance = InstanceHelpers.findInstanceByPath( this.rootInstance, instancePath )) == null ) {
			this.logger.severe( "Instance " + instancePath + " was not found on this agent." );

		} else if(( plugin = this.pluginManager.findPlugin( instance, this.logger )) != null ) {
			try {
				switch( action ) {
				case deploy:
					if( instance.getStatus() == InstanceStatus.NOT_DEPLOYED ) {
						updateAndNotifyNewStatus( instance, InstanceStatus.DEPLOYING );
						plugin.deploy( instance );
						updateAndNotifyNewStatus( instance, InstanceStatus.DEPLOYED_STOPPED );

					} else {
						this.logger.info(
								"Invalid status for instance " + instancePath + ". Status = "
								+ instance.getStatus() + ". Deploy request is dropped." );
					}
					break;

				case start:
					if( instance.getStatus() == InstanceStatus.DEPLOYED_STOPPED ) {
						updateAndNotifyNewStatus( instance, InstanceStatus.STARTING );
						plugin.start( instance );
						updateAndNotifyNewStatus( instance, InstanceStatus.DEPLOYED_STARTED );

					} else {
						this.logger.info(
								"Invalid status for instance " + instancePath + ". Status = "
								+ instance.getStatus() + ". Start request is dropped." );
					}
					break;

				case stop:
					if( instance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
						updateAndNotifyNewStatus( instance, InstanceStatus.STOPPING );
						plugin.stop( instance );
						updateAndNotifyNewStatus( instance, InstanceStatus.DEPLOYED_STOPPED );

					} else {
						this.logger.info(
								"Invalid status for instance " + instancePath + ". Status = "
								+ instance.getStatus() + ". Stop request is dropped." );
					}
					break;

				case undeploy:
					if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
						updateAndNotifyNewStatus( instance, InstanceStatus.UNDEPLOYING );
						plugin.undeploy( instance );
						updateAndNotifyNewStatus( instance, InstanceStatus.NOT_DEPLOYED );

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

			} catch( Exception e ) {
				this.logger.severe( e.getMessage());
				this.logger.finest( Utils.writeException( e ));
			}
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
	 */
	public void addInstance( String parentInstancePath, Instance newInstance ) {

		// Root instance
		if( parentInstancePath == null ) {
			if( this.rootInstance == null ) {

				// Update the model
				this.rootInstance = newInstance;

				// Start listening
				try {
					for( Instance instance : InstanceHelpers.buildHierarchicalList( this.rootInstance )) {
						if( instance.getParent() != null )
							this.messagingService.configureInstanceMessaging( instance, true );
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

		else if( message instanceof MsgCmdImportNotification )
			processMsgImportNotification((MsgCmdImportNotification) message );

		else
			this.logger.warning( this.agentName + ": got an undetermined message to process. " + message.getClass().getName());
	}


	@Override
	public String toString() {
		return this.agentName != null ? this.agentName : super.toString();
	}


	private void processMsgImportNotification( MsgCmdImportNotification msg ) {

//		String instancePath = msg.getInstancePath();
//		this.logger.fine( "Removing instance " + instancePath + "." );
//		performAction( ApplicationAction.remove, instancePath );
	}

	private void processMsgImportRemove( MsgCmdImportRemove msg ) {

//		String instancePath = msg.getInstancePath();
//		this.logger.fine( "Removing instance " + instancePath + "." );
//		performAction( ApplicationAction.remove, instancePath );
	}

	private void processMsgImportAdd( MsgCmdImportAdd msg ) {

//		String instancePath = msg.getInstancePath();
//		this.logger.fine( "Removing instance " + instancePath + "." );
//		performAction( ApplicationAction.remove, instancePath );
	}

	private void processMsgInstanceAdd( MsgCmdInstanceAdd msg ) {

		Instance newInstance = msg.getInstanceToAdd();
		String parentInstancePath = msg.getParentInstancePath();

		this.logger.fine( "Adding instance " + newInstance.getName() + " under " + parentInstancePath + "." );
		addInstance( parentInstancePath, newInstance );
	}


	private void processMsgInstanceRemove( MsgCmdInstanceRemove msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Removing instance " + instancePath + "." );
		performAction( ApplicationAction.remove, instancePath );
	}


	private void processMsgInstanceDeploy( MsgCmdInstanceDeploy msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Deploying instance " + instancePath + "." );
		performAction( ApplicationAction.deploy, instancePath );
	}


	private void processMsgInstanceUndeploy( MsgCmdInstanceUndeploy msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Undeploying instance " + instancePath + "." );
		performAction( ApplicationAction.undeploy, instancePath );
	}


	private void processMsgInstanceStart( MsgCmdInstanceStart msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Starting instance " + instancePath + "." );
		performAction( ApplicationAction.start, instancePath );
	}


	private void processMsgInstanceStop( MsgCmdInstanceStop msg ) {

		String instancePath = msg.getInstancePath();
		this.logger.fine( "Stopping instance " + instancePath + "." );
		performAction( ApplicationAction.stop, instancePath );
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
}
