/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.agent.AgentData;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRequest;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineUp;
import net.roboconf.messaging.utils.MessagingUtils;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public final class MessagingService {

	public static final String THOSE_THAT_EXPORT = "those.that.export.";
	public static final String THOSE_THAT_IMPORT = "those.that.import.";

	private final Logger logger = Logger.getLogger( getClass().getName());

	private IMessageServerClient client;
	private AgentData agentData;
	private Agent agent;
	private Timer heartBeatTimer;



	/**
	 * Initializes the connection with the message server.
	 * @param agentData the agent's data
	 * @param agentName the agent's name
	 * @param pluginManager the plug-in manager
	 * @throws IOException if something went wrong
	 */
	public void initializeAgentConnection(
			final AgentData agentData,
			String agentName,
			PluginManager pluginManager )
	throws IOException {

		this.agentData = agentData;
		this.agent = new Agent( agentName, pluginManager );
		this.agent.setMessagingService( this );
		this.agent.setAgentData( agentData );

		this.client = new MessageServerClientFactory().create();
		this.client.setMessageServerIp( agentData.getMessageServerIp());
		this.client.setApplicationName( agentData.getApplicationName());;
		this.client.setSourceName( agentData.getRootInstanceName());
		this.client.openConnection( this.agent );
		this.client.bind( MessagingUtils.buildRoutingKeyToAgent( agentData.getRootInstanceName()));

		// Indicate this machine is up
		MsgNotifMachineUp machineIsUp = new MsgNotifMachineUp( agentData.getRootInstanceName(), agentData.getIpAddress());
		this.client.publish( true, MessagingUtils.buildRoutingKeyToDm(), machineIsUp );


		// Add a hook for when the VM shutdowns
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				agentIsTerminating();
			}
		}));


		// Regularly send a heart beat message
		this.heartBeatTimer = new Timer( "Roboconf's Heartbeat Timer @ Agent", true );
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {

				try {
					MsgNotifHeartbeat heartBeat = new MsgNotifHeartbeat( agentData.getRootInstanceName());
					MessagingService.this.client.publish( true, MessagingUtils.buildRoutingKeyToDm(), heartBeat );

				} catch( IOException e ) {
					MessagingService.this.logger.severe( e.getMessage());
					MessagingService.this.logger.finest( Utils.writeException( e ));
					// FIXME: remove the cancel from here.
					// If the message server is down, we do not want to stop sending heart beats!
					cancel();
				}
			}
		};

		this.heartBeatTimer.scheduleAtFixedRate( timerTask, 0, MessagingUtils.HEARTBEAT_PERIOD );
	}


	/**
	 * Publishes a message.
	 * @param toDm true to send the message to the DM, false to send it to agents
	 * @param routingKey the routing key
	 * @param msg the message to publish
	 * @throws IOException if something went wrong
	 */
	public void publish( boolean toDm, String routingKey, Message msg ) throws IOException {
		this.client.publish( toDm, routingKey, msg );
		this.logger.info( this.agent.getAgentName() + " is publishing " + msg.getClass().getSimpleName() + " to " + routingKey );
	}


	/**
	 * Stops the heart beat timer.
	 * <p>
	 * Useless on a VM, but essential for in-memory or real-machine tests.
	 * </p>
	 */
	public void stopHeartBeatTimer() {

		if( this.heartBeatTimer != null )
			this.heartBeatTimer.cancel();
	}


	/**
	 * To call when the agent is terminating.
	 */
	public void agentIsTerminating() {

		stopHeartBeatTimer();
		try {
			MsgNotifMachineDown machineIsDown = new MsgNotifMachineDown( this.agentData.getRootInstanceName());
			MessagingService.this.client.publish( true, MessagingUtils.buildRoutingKeyToDm(), machineIsDown );
			MessagingService.this.client.closeConnection();

		} catch( IOException e ) {
			MessagingService.this.logger.severe( e.getMessage());
			MessagingService.this.logger.finest( Utils.writeException( e ));
		}

		this.logger.info( "Agent is stopping now." );
	}


	/**
	 * Configure the "instance messaging".
	 * @param instance the instance
	 * @param init true to set up the messaging, false to set it down
	 * @throws IOException if something went wrong
	 */
	public void configureInstanceMessaging( Instance instance, boolean init ) throws IOException {
		String applicationName = this.agentData.getApplicationName();

		// Process the exports
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {
			if( init )
				configureExports( applicationName, facetOrComponentName, instance );
			else
				unconfigureExports( applicationName, facetOrComponentName, instance );
		}

		// Process the imports
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {
			if( init )
				configureImports( applicationName, facetOrComponentName, instance );
			else
				unconfigureImports( applicationName, facetOrComponentName, instance );
		}
	}


	/**
	 * Publishes import or export messages on the messaging server.
	 * @param facetOrComponentName a facet or component name
	 * @param msg the message to publish
	 * @param target {@link #THOSE_THAT_EXPORT} or {@link #THOSE_THAT_IMPORT}
	 * <p>
	 * {@link #THOSE_THAT_EXPORT} means you send a message to the components
	 * that listen to other components that export variables. Said differently,
	 * this value means you send a message to components that import variables.
	 * </p>
	 * <p>
	 * {@link #THOSE_THAT_IMPORT} is used by components that import variables to
	 * notify exporting components they need variables values.
	 * </p>
	 *
	 * @throws IOException if something goes wrong
	 */
	public void publishExportOrImport( String facetOrComponentName, Message msg, String target )
	throws IOException {

		String routingKey = target + facetOrComponentName;
		publish( false, routingKey, msg );
	}


	private void unconfigureImports( String applicationName, String facetOrComponentName, Instance instance ) throws IOException {

		this.logger.fine( "Instance " + instance.getName() + " is unsubscribing to components that export variables it needs (prefix = " + facetOrComponentName + ")." );
		this.client.unbind( THOSE_THAT_EXPORT + facetOrComponentName );
	}


	private void configureImports( String applicationName, String facetOrComponentName, Instance instance ) throws IOException {

		this.logger.fine( "Instance " + instance.getName() + " is subscribing to components that export variables it needs (prefix = " + facetOrComponentName + ")." );
		this.client.bind( THOSE_THAT_EXPORT + facetOrComponentName );

		this.logger.fine( "Instance " + instance.getName() + " is notifying other components about the variables it needs." );
		MsgCmdImportRequest message = new MsgCmdImportRequest( facetOrComponentName );
		publishExportOrImport( facetOrComponentName, message, THOSE_THAT_IMPORT );
	}


	private void unconfigureExports( String applicationName, String facetOrComponentName, Instance instance ) throws IOException {

		this.logger.fine( "Instance " + instance.getName() + " is unsubscribing from components that need variables it exports (prefix = " + facetOrComponentName + ")." );
		this.client.unbind( THOSE_THAT_IMPORT + facetOrComponentName );

		// FIXME: maybe we should filter the map to only keep the required variables. For security?
		this.logger.fine( "Instance " + instance.getName() + " is signaling it does not export variables anymore." );
		MsgCmdImportRemove message = new MsgCmdImportRemove( facetOrComponentName, InstanceHelpers.computeInstancePath(instance));
		publishExportOrImport( facetOrComponentName, message, THOSE_THAT_EXPORT );
	}


	private void configureExports( String applicationName, String facetOrComponentName, Instance instance ) throws IOException {

		this.logger.fine( "Instance " + instance.getName() + " is subscribing to components that need variables it exports (prefix = " + facetOrComponentName + ")." );
		this.client.bind( THOSE_THAT_IMPORT + facetOrComponentName );
	}
}
