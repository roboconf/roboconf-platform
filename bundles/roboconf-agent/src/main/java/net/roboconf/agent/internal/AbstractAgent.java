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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.agent.IAgent;
import net.roboconf.agent.internal.misc.AgentConstants;
import net.roboconf.agent.internal.misc.AgentUtils;
import net.roboconf.agent.internal.misc.HeartbeatTask;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.core.Constants;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
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
public abstract class AbstractAgent implements IAgent {

	// Component properties (ipojo)
	protected String messageServerIp, messageServerUsername, messageServerPassword;
	protected String applicationName, rootInstanceName, ipAddress, iaasType;
	protected boolean overrideProperties = true;

	// Internal fields
	protected final Logger logger;
	protected IAgentClient messagingClient;
	protected MessageServerClientFactory factory = new MessageServerClientFactory();

	final AgentMessageProcessor messageProcessor;
	Timer heartBeatTimer;
	boolean running = false;


	/**
	 * Constructor.
	 */
	public AbstractAgent() {
		this.logger = Logger.getLogger( getClass().getName());
		this.messageProcessor = new AgentMessageProcessor( this );
	}


	/* (non-Javadoc)
	 * @see net.roboconf.agent.IAgent#start()
	 */
	@Override
	public void start() {

		// Do nothing if it is already running
		if( this.running )
			return;

		// Keep a trace of the launching
		this.logger.fine( "Agent " + getAgentId() + " is being launched." );

		// Get the configuration
		updateConfiguration();

		// Configure the message processor (required for the first launch)
		this.messageProcessor.configure();
		this.running = true;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.agent.IAgent#stop()
	 */
	@Override
	public void stop() {

		this.logger.fine( "Agent " + getAgentId() + " is being stopped." );
		try {
			if( this.heartBeatTimer != null )
				this.heartBeatTimer.cancel();

			if( this.messagingClient != null
					&& this.messagingClient.isConnected()) {

				this.messagingClient.sendMessageToTheDm( new MsgNotifMachineDown( this.applicationName, this.rootInstanceName ));
				this.messagingClient.closeConnection();
			}

			this.logger.fine( "Agent " + getAgentId() + " was successfully stopped." );

		} catch( IOException e ) {
			this.logger.severe( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}

		this.running = false;
	}


	/**
	 * Finds the right plug-in for an instance.
	 * <p>
	 * If {@link #simulatePlugins} is true, this method returns an instance
	 * of {@link PluginMock}, no matter what is the installer name.
	 * </p>
	 *
	 * @param instance a non-null instance
	 * @return the plug-in associated with the instance's installer name
	 */
	public PluginInterface findPlugin( Instance instance ) {

		PluginInterface result = new PluginMock();
		result.setNames( this.applicationName, this.rootInstanceName );

		return result;
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
				props = AgentUtils.findParametersForAzure( this.logger );

			else if( AgentConstants.PLATFORM_EC2.equals( this.iaasType )
					|| AgentConstants.PLATFORM_OPENSTACK.equals( this.iaasType ))
				props = AgentUtils.findParametersForAmazonOrOpenStack( this.logger );

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

		// Create a new client
		this.messagingClient = this.factory.createAgentClient();
		this.messagingClient.setParameters( this.messageServerIp, this.messageServerUsername, this.messageServerPassword );
		this.messagingClient.setApplicationName( this.applicationName );
		this.messagingClient.setRootInstanceName( this.rootInstanceName );

		// Configure the message processor
		this.messageProcessor.setMessagingClient( this.messagingClient );

		// Initialize a timer to regularly send a heart beat
		if( this.heartBeatTimer != null )
			this.heartBeatTimer.cancel();

		TimerTask timerTask = new HeartbeatTask( this.applicationName, this.rootInstanceName, this.messagingClient );
		this.heartBeatTimer = new Timer( "Roboconf's Heartbeat Timer @ Agent", true );
		this.heartBeatTimer.scheduleAtFixedRate( timerTask, 0, Constants.HEARTBEAT_PERIOD );
	}


	/**
	 * @return the messageServerIp
	 */
	public String getMessageServerIp() {
		return this.messageServerIp;
	}

	/**
	 * @param messageServerIp the messageServerIp to set
	 */
	public void setMessageServerIp( String messageServerIp ) {
		this.messageServerIp = messageServerIp;
	}

	/**
	 * @return the messageServerUsername
	 */
	public String getMessageServerUsername() {
		return this.messageServerUsername;
	}

	/**
	 * @param messageServerUsername the messageServerUsername to set
	 */
	public void setMessageServerUsername( String messageServerUsername ) {
		this.messageServerUsername = messageServerUsername;
	}

	/**
	 * @return the messageServerPassword
	 */
	public String getMessageServerPassword() {
		return this.messageServerPassword;
	}

	/**
	 * @param messageServerPassword the messageServerPassword to set
	 */
	public void setMessageServerPassword( String messageServerPassword ) {
		this.messageServerPassword = messageServerPassword;
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
	}

	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}

	/**
	 * @return the rootInstanceName
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}

	/**
	 * @param rootInstanceName the rootInstanceName to set
	 */
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress( String ipAddress ) {
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the iaasType
	 */
	public String getIaasType() {
		return this.iaasType;
	}

	/**
	 * @param iaasType the iaasType to set
	 */
	public void setIaasType( String iaasType ) {
		this.iaasType = iaasType;
	}

	/**
	 * @return the overrideProperties
	 */
	public boolean isOverrideProperties() {
		return this.overrideProperties;
	}

	/**
	 * @param overrideProperties the overrideProperties to set
	 */
	public void setOverrideProperties( boolean overrideProperties ) {
		this.overrideProperties = overrideProperties;
	}

	/**
	 * @param factory the factory to set (for test)
	 */
	void setFactory( MessageServerClientFactory factory ) {
		this.factory = factory;
	}

	/**
	 * @return the agent's ID (a human-readable identifier)
	 */
	private String getAgentId() {
		return this.rootInstanceName + " @ " + this.applicationName;
	}
}
