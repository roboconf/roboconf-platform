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
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.agent.internal.misc.AgentConstants;
import net.roboconf.agent.internal.misc.HeartbeatTask;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.agent.internal.misc.UserDataUtils;
import net.roboconf.core.Constants;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
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
	String messageServerIp, messageServerUsername, messageServerPassword;
	String applicationName, rootInstanceName, ipAddress, targetId;
	boolean overrideProperties = false, simulatePlugins = true;

	// Fields that should be injected (ipojo)
	final List<PluginInterface> plugins = new ArrayList<PluginInterface> ();

	// Internal fields
	private IAgentClient messagingClient;
	private final Logger logger;
	private final String messagingFactoryType;
	AgentMessageProcessor messageProcessor;
	Timer heartBeatTimer;


	/**
	 * Constructor.
	 */
	public Agent() {
		this( MessagingConstants.FACTORY_RABBIT_MQ );
	}


	/**
	 * Constructor.
	 * @param messagingFactoryType the messaging factory type
	 * @see MessagingConstants
	 */
	public Agent( String messagingFactoryType ) {
		this.logger = Logger.getLogger( getClass().getName());
		this.messagingFactoryType = messagingFactoryType;
	}


	/**
	 * Starts listening and processing messageQueue.
	 * <p>
	 * If the agent was already started, this method does nothing.
	 * </p>
	 */
	public void start() {

		TimerTask timerTask = new HeartbeatTask( this );
		this.heartBeatTimer = new Timer( "Roboconf's Heartbeat Timer @ Agent", true );
		this.heartBeatTimer.scheduleAtFixedRate( timerTask, 0, Constants.HEARTBEAT_PERIOD );

		if( this.messageProcessor == null ) {
			this.logger.info( "Agent '" + getAgentId() + "' is being launched." );
			this.messageProcessor = new AgentMessageProcessor( this );
			this.messageProcessor.start();
			updateConfiguration();
		}
	}


	/**
	 * Stops listening and processing messageQueue.
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

		if( this.heartBeatTimer != null ) {
			this.heartBeatTimer.cancel();
			this.heartBeatTimer = null;
		}

		if( this.messageProcessor == null )
			return;

		this.logger.info( "Agent '" + getAgentId() + "' is being stopped." );
		try {
			IAgentClient messagingClient = this.messageProcessor.getMessagingClient();
			if( messagingClient != null ) {
				if( messagingClient.isConnected()) {
					messagingClient.sendMessageToTheDm( new MsgNotifMachineDown( this.applicationName, this.rootInstanceName ));
					this.logger.fine( "Agent " + getAgentId() + " notified the DM it was about to stop." );
				}
			}

		} catch( IOException e ) {
			this.logger.warning( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}

		this.messageProcessor.stopProcessor();
		this.messageProcessor = null;
		this.logger.fine( "Agent " + getAgentId() + " was successfully stopped." );
	}


	/**
	 * @return true if this agent received its model, false otherwise
	 */
	public boolean hasReceivedModel() {
		return this.messageProcessor != null && this.messageProcessor.rootInstance != null;
	}


	/**
	 * @return the most up-to-date client for the messaging server
	 */
	public IAgentClient getMessagingClient() {
		return this.messagingClient;
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

		if( this.plugins.isEmpty()) {
			this.logger.info( "No plug-in was found for Roboconf's agent." );

		} else {
			StringBuilder sb = new StringBuilder( "Available plug-ins in Roboconf's agent: " );
			for( Iterator<PluginInterface> it = this.plugins.iterator(); it.hasNext(); ) {
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
		if( pi != null ) {
			this.logger.info( "Plugin '" + pi.getPluginName() + "' is now available in Roboconf's agent." );
			this.plugins.add( pi );
			listPlugins();
		}
	}


	/**
	 * This method is invoked by iPojo every time a plug-in disappears.
	 * @param pi
	 */
	public void pluginDisappears( PluginInterface pi ) {

		// May happen if a plug-in could not be instantiated
		// (iPojo uses proxies). In this case, it results in a NPE here.
		if( pi == null ) {
			this.logger.info( "An invalid plugin is removed." );
		} else {
			this.plugins.remove( pi );
			this.logger.info( "Plugin '" + pi.getPluginName() + "' is not available anymore in Roboconf's agent." );
		}

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

		// If the instance is not started (i.e. if the message processor
		// is null), do nothing. This method will be invoked once the
		// instance is started by iPojo.
		if( this.messageProcessor == null )
			return;

		// Do we need to override properties with user data?
		if( this.overrideProperties ) {
			AgentProperties props = null;
			if( AgentConstants.PLATFORM_AZURE.equals( this.targetId ))
				props = UserDataUtils.findParametersForAzure( this.logger );

			else if( AgentConstants.PLATFORM_EC2.equals( this.targetId )
					|| AgentConstants.PLATFORM_OPENSTACK.equals( this.targetId ))
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

		// Update the messaging connection
		try {
			this.messagingClient = this.messageProcessor.switchMessagingClient(
					this.messageServerIp,
					this.messageServerUsername,
					this.messageServerPassword );

		} catch( IOException e ) {
			this.logger.severe( "An error occured while reconfiguring the agent. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}

		this.logger.info( "The agent configuration was updated..." );
	}


	/**
	 * @return the application name
	 */
	public String getApplicationName() {
		return this.applicationName;
	}


	/**
	 * @return the root instance name
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}


	/**
	 * @return the IP address
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}


	/**
	 * @return the messagingFactoryType
	 */
	public String getMessagingFactoryType() {
		return this.messagingFactoryType;
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
	 * @param rootInstanceName the rootInstanceName to set
	 */
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}


	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress( String ipAddress ) {
		this.ipAddress = ipAddress;
	}


	/**
	 * @param targetId the targetId to set
	 */
	public void setTargetId( String targetId ) {
		this.targetId = targetId;
	}


	/**
	 * @param overrideProperties the overrideProperties to set
	 */
	public void setOverrideProperties( boolean overrideProperties ) {
		this.overrideProperties = overrideProperties;
	}


	/**
	 * @param simulatePlugins the simulatePlugins to set
	 */
	public void setSimulatePlugins( boolean simulatePlugins ) {
		this.simulatePlugins = simulatePlugins;
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
}
