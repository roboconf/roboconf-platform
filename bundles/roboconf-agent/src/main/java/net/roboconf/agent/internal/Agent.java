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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import net.roboconf.agent.internal.misc.AgentConstants;
import net.roboconf.agent.internal.misc.HeartbeatTask;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.agent.internal.misc.UserDataUtils;
import net.roboconf.core.Constants;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.plugin.api.PluginInterface;

/**
 * An abstract implementation for a Roboconf agent.
 * <p>
 * This class does not have any method to implement.
 * It is only here to be extended.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class Agent {

	// Component properties (ipojo)
	private String messageServerIp, messageServerUsername, messageServerPassword;
	private String applicationName, rootInstanceName, ipAddress, iaasType;
	private boolean overrideProperties = false, simulatePlugins = true;

	// Fields that should be injected (ipojo)
	private PluginInterface[] plugins;

	// Internal fields
	private final Logger logger;
	private MessageServerClientFactory factory = new MessageServerClientFactory();
	private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<Message> ();

	AgentMessageProcessor messageProcessor;
	IAgentClient messagingClient;
	Timer heartBeatTimer;
	boolean running = false;


	/**
	 * Constructor.
	 */
	public Agent() {
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Starts listening and processing messages.
	 * <p>
	 * If the agent was already started, this method does nothing.
	 * </p>
	 */
	public void start() {

		// Do nothing if it is already running
		if( this.running )
			return;

		// Keep a trace of the launching
		this.logger.info( "Agent '" + getAgentId() + "' is being launched." );

		// Get the configuration
		updateConfiguration();
		this.running = true;
	}


	/**
	 * Stops listening and processing messages.
	 * <p>
	 * This method does not interrupt the current message processing.
	 * But it will not process any further message, unless the start method
	 * is called after.
	 * </p>
	 * <p>
	 * If the agent is already stopped, this method does nothing.
	 * </p>
	 */
	public void stop() {

		this.logger.info( "Agent '" + getAgentId() + "' is being stopped." );
		if( this.heartBeatTimer != null )
			this.heartBeatTimer.cancel();

		try {
			// Send a message to the DM.
			// We cannot consider the agent to be stopped if this message is not sent.
			if( this.messagingClient != null ) {

				if( this.messagingClient.isConnected()) {
					this.messagingClient.sendMessageToTheDm( new MsgNotifMachineDown( this.applicationName, this.rootInstanceName ));
					this.logger.fine( "Agent " + getAgentId() + " notified the DM it was about to stop." );
				}

				// We can ignore errors when we disconnect the agent's client.
				// We can thus consider it is stopped.
				this.running = false;

				this.messagingClient.closeConnection();
				this.messagingClient = null;
				this.messageProcessor = null;
				this.logger.fine( "Agent " + getAgentId() + " was successfully stopped." );

			} else {
				this.running = false;
			}

		} catch( IOException e ) {
			this.logger.severe( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 * Finds the right plug-in for an instance.
	 * @param instance a non-null instance
	 * @return the plug-in associated with the instance's installer name
	 */
	public PluginInterface findPlugin( Instance instance ) {

		// Find a plug-in
		PluginInterface result = null;
		if( this.simulatePlugins ) {
			result = new PluginMock();

		} else {
			String installerName = null;
			if( instance.getComponent() != null )
				installerName = instance.getComponent().getInstallerName();

			// Run through available plug-ins
			if( this.plugins != null ) {
				for( PluginInterface pi : this.plugins ) {
					if( pi.getPluginName().equalsIgnoreCase( installerName )) {
						result = pi;
						break;
					}
				}
			}

			if( result == null )
				this.logger.severe( "No plugin was found for instance '" + instance.getName() + "' with installer '" + installerName + "'." );
		}

		// Initialize the result, if any
		if( result != null )
			result.setNames( this.applicationName, this.rootInstanceName );

		return result;
	}


	/**
	 * This method lists the available plug-ins and logs it.
	 */
	public void listPlugins() {

		if( this.plugins == null || this.plugins.length == 0 ) {
			this.logger.info( "No plug-in was found for Roboconf's agent." );

		} else {
			StringBuilder sb = new StringBuilder( "Available plug-ins in Roboconf's agent: " );
			for( Iterator<PluginInterface> it = Arrays.asList( this.plugins).iterator(); it.hasNext(); ) {
				sb.append( it.next().getPluginName());
				if( it.hasNext())
					sb.append( ", " );
			}

			sb.append( "." );
			this.logger.info( sb.toString());
		}
	}


	/**
	 * This method is invoked by iPojo every time a new plug-in appears.
	 * @param pi
	 */
	public void pluginAppears( PluginInterface pi ) {
		this.logger.info( "Plugin '" + pi.getPluginName() + "' is now available in Roboconf's agent." );
		listPlugins();
	}


	/**
	 * This method is invoked by iPojo every time a plug-in disappears.
	 * @param pi
	 */
	public void pluginDisappears( PluginInterface pi ) {

		// May happen if a plug-in could not be instantiated
		// (iPojo uses proxies). In this case, it results in a NPE here.
		if( pi == null )
			this.logger.info( "An invalid plugin is removed." );
		else
			this.logger.info( "Plugin '" + pi.getPluginName() + "' is not available anymore in Roboconf's agent." );

		listPlugins();
	}


	/**
	 * This method is invoked by iPojo every time a plug-in is modified.
	 * @param pi
	 */
	public void pluginWasModified( PluginInterface pi ) {
		this.logger.info( "Plugin '" + pi.getPluginName() + "' was modified in Roboconf's agent." );
		listPlugins();
	}


	/**
	 * The method to invoke once one or several properties were changed.
	 * <p>
	 * This method creates a new messaging client and reconfigures the message processor.
	 * It also creates a new heart beat timer.
	 * </p>
	 */
	public void updateConfiguration() {

		// Do we need to override properties with user data?
		if( this.overrideProperties ) {

			AgentProperties props = null;
			if( AgentConstants.PLATFORM_AZURE.equals( this.iaasType ))
				props = UserDataUtils.findParametersForAzure( this.logger );

			else if( AgentConstants.PLATFORM_EC2.equals( this.iaasType )
					|| AgentConstants.PLATFORM_OPENSTACK.equals( this.iaasType ))
				props = UserDataUtils.findParametersForAmazonOrOpenStack( this.logger );

			String s;
			if( props != null ) {
				if(( s = props.validate()) != null )
					this.logger.severe( "An error was found in user data. " + s );

				this.applicationName = props.getApplicationName();
				this.ipAddress = props.getIpAddress();
				this.rootInstanceName = props.getRootInstanceName();

				this.messageServerIp = props.getMessageServerIp();
				this.messageServerUsername = props.getMessageServerUsername();
				this.messageServerPassword = props.getMessageServerPassword();
			}
		}

		// Indicate to the message processor it will be replaced.
		if( this.messageProcessor != null )
			this.messageProcessor.thisIsTheLastMessageYouProcess();

		// Otherwise, initialize the messaging client.
		else
			switchMessageProcessor();

		this.logger.info( "The agent configuration was updated..." );
	}


	/**
	 * Switches the message processor.
	 */
	public void switchMessageProcessor() {

		// If there was a client, release the connection.
		// This is supposed to stop the message processor too.
		if( this.messagingClient != null ) {

			try {
				this.messagingClient.closeConnection();

			} catch( IOException e ) {
				this.logger.severe( "An error occured while releasing the messaging client. " + e.getMessage());
				this.logger.finest( Utils.writeException( e ));
			}
		}

		try {
			// Store and configure a new client
			this.messagingClient = this.factory.createAgentClient();
			this.messagingClient.setParameters( this.messageServerIp, this.messageServerUsername, this.messageServerPassword );
			this.messagingClient.setApplicationName( this.applicationName );
			this.messagingClient.setRootInstanceName( this.rootInstanceName );

			// And switch the processor
			this.messageProcessor = new AgentMessageProcessor( this );
			this.messagingClient.openConnection( this.messageProcessor );
			this.messagingClient.listenToTheDm( ListenerCommand.START );

			// Send an "UP" message (heart beat)
			this.messagingClient.sendMessageToTheDm( new MsgNotifHeartbeat( this.applicationName, this.rootInstanceName, this.ipAddress ));

			// Initialize a timer to regularly send a heart beat
			if( this.heartBeatTimer != null )
				this.heartBeatTimer.cancel();

			TimerTask timerTask = new HeartbeatTask( this.applicationName, this.rootInstanceName, this.ipAddress, this.messagingClient );
			this.heartBeatTimer = new Timer( "Roboconf's Heartbeat Timer @ Agent", true );
			this.heartBeatTimer.scheduleAtFixedRate( timerTask, 0, Constants.HEARTBEAT_PERIOD );

		} catch( IOException e ) {
			this.logger.severe( "An error occured while initializing a new messaging client. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 * @return the messageServerIp
	 */
	public String getMessageServerIp() {
		return this.messageServerIp;
	}

	/**
	 * @return the messageServerUsername
	 */
	public String getMessageServerUsername() {
		return this.messageServerUsername;
	}

	/**
	 * @return the messageServerPassword
	 */
	public String getMessageServerPassword() {
		return this.messageServerPassword;
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
	}

	/**
	 * @return the rootInstanceName
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}

	/**
	 * @return the iaasType
	 */
	public String getIaasType() {
		return this.iaasType;
	}

	/**
	 * @return the overrideProperties
	 */
	public boolean isOverrideProperties() {
		return this.overrideProperties;
	}

	/**
	 * @return the simulatePlugins
	 */
	public boolean isSimulatePlugins() {
		return this.simulatePlugins;
	}

	/**
	 * @param simulatePlugins the simulatePlugins to set
	 */
	public void setSimulatePlugins( boolean simulatePlugins ) {
		this.simulatePlugins = simulatePlugins;
	}

	/**
	 * @param messageServerIp the messageServerIp to set
	 */
	public void setMessageServerIp( String messageServerIp ) {
		this.messageServerIp = messageServerIp;
	}

	/**
	 * @param messageServerUsername the messageServerUsername to set
	 */
	public void setMessageServerUsername( String messageServerUsername ) {
		this.messageServerUsername = messageServerUsername;
	}

	/**
	 * @param messageServerPassword the messageServerPassword to set
	 */
	public void setMessageServerPassword( String messageServerPassword ) {
		this.messageServerPassword = messageServerPassword;
	}

	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}

	/**
	 * @param iaasType the iaasType to set
	 */
	public void setIaasType( String iaasType ) {
		this.iaasType = iaasType;
	}

	/**
	 * @param overrideProperties the overrideProperties to set
	 */
	public void setOverrideProperties( boolean overrideProperties ) {
		this.overrideProperties = overrideProperties;
	}

	/**
	 * @param rootInstanceName the rootInstanceName to set
	 */
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}

	/**
	 * @param factory the factory to set (for test)
	 */
	void setFactory( MessageServerClientFactory factory ) {
		this.factory = factory;
	}

	/**
	 * @return the messagingClient
	 */
	public IAgentClient getMessagingClient() {
		return this.messagingClient;
	}

	/**
	 * @return the agent's ID (a human-readable identifier)
	 */
	String getAgentId() {

		StringBuilder sb = new StringBuilder();
		sb.append( Utils.isEmptyOrWhitespaces( this.rootInstanceName ) ? "?" : this.rootInstanceName );
		if( ! Utils.isEmptyOrWhitespaces( this.applicationName ))
			sb.append( " @ " + this.applicationName );

		return sb.toString();
	}

	/**
	 * @param plugins the plugins to set
	 */
	void setPlugins( PluginInterface[] plugins ) {
		this.plugins = plugins;
	}

	/**
	 * @return the messages
	 */
	LinkedBlockingQueue<Message> getMessages() {
		return this.messages;
	}
}
