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

package net.roboconf.agent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.agent.internal.AgentMessageProcessor;
import net.roboconf.agent.internal.PluginManager;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineUp;
import net.roboconf.plugin.api.ExecutionLevel;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentLauncher {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String agentName;
	private final AgentData agentData;

	private Timer heartBeatTimer;
	private IAgentClient messagingClient;
	private MessageServerClientFactory factory = new MessageServerClientFactory();


	/**
	 * Constructor.
	 * @param agentData
	 */
	public AgentLauncher( AgentData agentData ) {
		this.agentData = agentData;
	}


	/**
	 * Constructor.
	 * @param agentName
	 * @param agentData
	 */
	public AgentLauncher( String agentName, AgentData agentData ) {
		this( agentData );
		this.agentName = agentName;
	}


	/**
	 * @param factory the factory to set
	 */
	public void setFactory( MessageServerClientFactory factory ) {
		this.factory = factory;
	}


	/**
	 * Launches an agent.
	 * @param agentData the agent data
	 * @param executionLevel the execution level
	 * @param dumpDirectory the dump directory (if execution level is {@link ExecutionLevel#GENERATE_FILES})
	 * @throws IOException if there was a problem while initializing the messaging
	 */
	public void launchAgent( ExecutionLevel executionLevel, File dumpDirectory ) throws IOException {

		// Update the agent's name if necessary
		if( this.agentName == null ) {
			try {
				this.agentName = "Roboconf Agent - " + InetAddress.getLocalHost().getHostName();

			} catch( UnknownHostException e ) {
				this.logger.warning( "Network information could not be retrieved. Setting the agent name to default." );
				this.agentName = "Roboconf Agent";
			}
		}

		// Keep a trace of the launching
		this.logger.fine( "Agent " + this.agentName + " is being launched." );

		// The plug-ins configuration
		PluginManager pluginManager = new PluginManager();
		pluginManager.setDumpDirectory( dumpDirectory );
		pluginManager.setExecutionLevel( executionLevel );

		// Create the messaging client
		this.messagingClient = this.factory.createAgentClient();
		this.messagingClient.setMessageServerIp( this.agentData.getMessageServerIp());
		this.messagingClient.setApplicationName( this.agentData.getApplicationName());
		this.messagingClient.setRootInstanceName( this.agentData.getRootInstanceName());

		// Create the message processor
		AgentMessageProcessor messageProcessor = new AgentMessageProcessor(
				this.agentName,
				this.agentData,
				pluginManager,
				this.messagingClient,
				this.heartBeatTimer );

		// Open a connection with the messaging server
		this.messagingClient.listenToTheDm( ListenerCommand.START );
		this.messagingClient.openConnection( messageProcessor );

		// Add a hook for when the VM shutdowns
		Runtime.getRuntime().addShutdownHook( new Thread(new Runnable() {
			@Override
			public void run() {
				stopAgent();
			}
		}));

		// Send an "UP" message
		MsgNotifMachineUp machineIsUp = new MsgNotifMachineUp(
				this.agentData.getApplicationName(),
				this.agentData.getRootInstanceName(),
				this.agentData.getIpAddress());
		this.messagingClient.sendMessageToTheDm( machineIsUp );

		// Initialize a timer to regularly send a heart beat
		this.heartBeatTimer = new Timer( "Roboconf's Heartbeat Timer @ Agent", true );
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					MsgNotifHeartbeat heartBeat = new MsgNotifHeartbeat(
							AgentLauncher.this.agentData.getApplicationName(),
							AgentLauncher.this.agentData.getRootInstanceName());
					AgentLauncher.this.messagingClient.sendMessageToTheDm( heartBeat );

				} catch( IOException e ) {
					AgentLauncher.this.logger.severe( e.getMessage());
					AgentLauncher.this.logger.finest( Utils.writeException( e ));
				}
			}
		};

		this.heartBeatTimer.scheduleAtFixedRate( timerTask, 0, Constants.HEARTBEAT_PERIOD );
	}


	/**
	 * Forces the agent to stop.
	 * <p>
	 * The agent will stop sending heart beats and send a MachineDown notification.
	 * </p>
	 */
	public void stopAgent() {

		this.logger.fine( "Agent " + this.agentName + " is being stopped." );
		try {
			this.heartBeatTimer.cancel();

			MsgNotifMachineDown machineIsDown = new MsgNotifMachineDown(
					this.agentData.getApplicationName(),
					this.agentData.getRootInstanceName());

			this.messagingClient.sendMessageToTheDm( machineIsDown );
			this.messagingClient.closeConnection();

			this.logger.fine( "Agent " + this.agentName + " was successfully stopped." );

		} catch( IOException e ) {
			this.logger.severe( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}
}
