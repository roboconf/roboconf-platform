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

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.InteractionType;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.utils.MessagingUtils;
import net.roboconf.plugin.api.ExecutionLevel;
import net.roboconf.plugin.api.PluginInterface;
import net.roboconf.plugin.bash.PluginBash;
import net.roboconf.plugin.logger.PluginLogger;
import net.roboconf.plugin.puppet.PluginPuppet;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Agent {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String agentName;

	private Instance rootInstance;
	private IMessageServerClient client;
	private ExecutionLevel executionLevel = ExecutionLevel.RUNNING;
	private File dumpDirectory;


	/**
	 * Constructor.
	 * @param agentName the agent name
	 */
	public Agent( String agentName ) {
		this.agentName = agentName;
	}


	/**
	 * @param client the client to set
	 */
	public void setClient( IMessageServerClient client ) {
		this.client = client;
	}


	/**
	 * @param executionLevel the executionLevel to set
	 */
	public void setExecutionLevel( ExecutionLevel executionLevel ) {
		this.executionLevel = executionLevel;
	}


	/**
	 * @param dumpDirectory the dumpDirectory to set.
	 * p>
	 * Only required if execution level is
	 * {@link ExecutionLevel#GENERATE_FILES}.
	 * </p>
	 */
	public void setDumpDirectory( File dumpDirectory ) {
		this.dumpDirectory = dumpDirectory;
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

		} else if(( plugin = findPlugin( instance )) != null ) {
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
						instance.getParent().getChildren().remove( instance );
						this.logger.fine( "Child instance " + instancePath + " was removed from the model." );

						MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( instance );
						this.client.publish( InteractionType.DM_AND_AGENT, filterName, msg );

					} else {
						this.rootInstance = null;
						this.logger.fine( "Root instance " + instancePath + " was set to null." );

						MsgNotifInstanceRemoved msg = new MsgNotifInstanceRemoved( instance );
						this.client.publish( InteractionType.DM_AND_AGENT, filterName, msg );
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
		PluginInterface plugin = findPlugin( instance );
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
			if( this.rootInstance == null )
				this.rootInstance = newInstance;
			else
				this.logger.severe( "A request to change the root instance was received. Request to add " + newInstance.getName() + " is dropped." );
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

			// Start listening?
		}
	}


	@Override
	public String toString() {
		return this.agentName != null ? this.agentName : super.toString();
	}


	/**
	 * Finds the right plug-in.
	 * @param instance an instance (not null)
	 * @return the right plug-in, or null if none was found
	 */
	private PluginInterface findPlugin( Instance instance ) {

		PluginInterface result = null;
		String installerName = instance.getComponent().getInstallerName();

		if( "bash".equalsIgnoreCase( installerName ))
			result = new PluginBash();

		else if( "puppet".equalsIgnoreCase( installerName ))
			result = new PluginPuppet();

		else if( "logger".equalsIgnoreCase( installerName ))
			result = new PluginLogger();

		else
			this.logger.severe( "No plugin was found for instance " + instance.getName() + " with installer " + installerName + "." );

		if( result != null ) {
			result.setExecutionLevel( this.executionLevel );
			result.setDumpDirectory( this.dumpDirectory );
		}

		return result;
	}


	/**
	 * Updates the status of an instance and notifies the DM of this change.
	 * @param instance
	 * @param newStatus
	 * @throws IOException
	 */
	private void updateAndNotifyNewStatus( Instance instance, InstanceStatus newStatus ) throws IOException {
		instance.setStatus( newStatus );
		this.client.publish(
				InteractionType.DM_AND_AGENT,
				MessagingUtils.buildRoutingKeyToDm(),
				new MsgNotifInstanceChanged( instance ));
	}
}
