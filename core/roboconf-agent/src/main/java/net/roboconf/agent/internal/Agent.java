/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.agent.internal.misc.AgentConstants;
import net.roboconf.agent.internal.misc.AgentUtils;
import net.roboconf.agent.internal.misc.HeartbeatTask;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.agent.internal.misc.UserDataHelper;
import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.runtime.IReconfigurable;
import net.roboconf.core.utils.ProcessStore;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.plugin.api.PluginInterface;

/**
 * An implementation of a Roboconf agent.
 * @author Vincent Zurczak - Linagora
 */
public class Agent implements AgentMessagingInterface, IReconfigurable {

	// Component properties (ipojo)
	String applicationName, scopedInstancePath, ipAddress, parameters, messagingType;
	String domain = Constants.DEFAULT_DOMAIN;
	String networkInterface = AgentConstants.DEFAULT_NETWORK_INTERFACE;
	boolean overrideProperties = false, simulatePlugins = true;

	// Fields that should be injected (ipojo)
	final List<PluginInterface> plugins = new ArrayList<> ();

	// Internal fields
	private final Logger logger;
	private ReconfigurableClientAgent messagingClient;
	private Instance scopedInstance;
	Timer heartBeatTimer;

	// Set as a class attribute to be overridden for tests.
	String karafEtc = System.getProperty( Constants.KARAF_ETC );
	String karafData = System.getProperty( Constants.KARAF_DATA );
	UserDataHelper userDataHelper = new UserDataHelper();

	// Global status
	public final AtomicBoolean resetInProgress = new AtomicBoolean( false );



	/**
	 * Constructor.
	 */
	public Agent() {
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Starts the agent.
	 * <p>
	 * It is invoked by iPojo when an instance becomes VALID.
	 * </p>
	 */
	public void start() {

		// Basic properties
		this.logger.info( "Agent '" + getAgentId() + "' is about to be launched." );
		if( Utils.isEmptyOrWhitespaces( this.ipAddress )) {
			this.ipAddress = AgentUtils.findIpAddress( this.networkInterface );
			this.logger.info( "IP address resolved to " + this.ipAddress );
		}

		// Create a messaging client
		this.messagingClient = newReconfigurableClientAgent();
		this.messagingClient.setDomain( this.domain );
		AgentMessageProcessor messageProcessor = newMessageProcessor();
		this.messagingClient.associateMessageProcessor( messageProcessor );

		// Deal with dynamic parameters
		reloadUserData();
		reconfigure();

		// Prepare the timer for scheduled tasks
		TimerTask timerTask = new HeartbeatTask( this );
		this.heartBeatTimer = new Timer( "Roboconf's Heartbeat Timer @ Agent", true );
		this.heartBeatTimer.scheduleAtFixedRate( timerTask, Constants.HEARTBEAT_PERIOD, Constants.HEARTBEAT_PERIOD );

		this.logger.info( "Agent '" + getAgentId() + "' was launched." );
	}


	/**
	 * Stops the agent.
	 * <p>
	 * It is invoked by iPojo when an instance becomes INVALID.
	 * </p>
	 */
	public void stop() {

		this.logger.info( "Agent '" + getAgentId() + "' is about to be stopped." );

		// Stop the timer
		if( this.heartBeatTimer != null ) {
			this.heartBeatTimer.cancel();
			this.heartBeatTimer = null;
		}

		// Prevent NPE for successive calls to #stop()
		if( this.messagingClient == null )
			return;

		// Send a last message to the DM
		try {
			if( this.messagingClient.isConnected()) {
				this.messagingClient.sendMessageToTheDm( new MsgNotifMachineDown( this.applicationName, this.scopedInstancePath ));
				this.logger.fine( "Agent " + getAgentId() + " notified the DM it was about to stop." );
			}

		} catch( IOException e ) {
			this.logger.warning( e.getMessage());
			Utils.logException( this.logger, e );
		}

		// Close the connection
		try {
			this.messagingClient.getMessageProcessor().stopProcessor();
			this.messagingClient.getMessageProcessor().interrupt();
			this.messagingClient.closeConnection();

		} catch( IOException e ) {
			this.logger.warning( e.getMessage());
			Utils.logException( this.logger, e );
		}

		this.logger.info( "Agent '" + getAgentId() + "' was stopped." );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.agent.AgentMessagingInterface
	 * #forceHeartbeatSending()
	 */
	@Override
	public void forceHeartbeatSending() {
		new HeartbeatTask( this ).run();
	}


	/**
	 * @return true if this agent needs the DM to send its model
	 */
	public boolean needsModel() {

		AgentMessageProcessor messageProcessor = null;
		if( this.messagingClient != null )
			messageProcessor = (AgentMessageProcessor) this.messagingClient.getMessageProcessor();

		return messageProcessor == null || messageProcessor.scopedInstance == null;
	}


	/**
	 * @return the client for the messaging server
	 */
	@Override
	public ReconfigurableClientAgent getMessagingClient() {
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
			this.logger.finer( "Simulating plugins..." );
			result = new PluginMock();

		} else {
			String installerName = null;
			if( instance.getComponent() != null )
				installerName = ComponentHelpers.findComponentInstaller( instance.getComponent());

			// Run through available plug-ins
			for( PluginInterface pi : this.plugins ) {
				if( pi.getPluginName().equalsIgnoreCase( installerName )) {
					result = pi;
					break;
				}
			}

			if( result == null )
				this.logger.severe( "No plugin was found for instance '" + instance.getName() + "' with installer '" + installerName + "'." );
		}

		// Initialize the result, if any
		if( result != null )
			result.setNames( this.applicationName, this.scopedInstancePath );

		return result == null ? null : new PluginProxy( result );
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
	 * @param pi the appearing plugin.
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
	 * @param pi the disappearing plugin.
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
	 * @param pi the modified plugin.
	 */
	public void pluginWasModified( PluginInterface pi ) {
		this.logger.info( "Plugin '" + pi.getPluginName() + "' was modified in Roboconf's agent." );
		listPlugins();
	}


	/**
	 * This method reconfigures the agent.
	 * <p>
	 * It is invoked by iPojo when the configuration changes.
	 * It may be invoked before the start() method is.
	 * </p>
	 */
	@Override
	public void reconfigure() {

		// This method is invoked when properties change.
		// It is not related to life cycle (start/stop).
		this.logger.info( "Reconfiguration requested in agent " + getAgentId());
		if( this.messagingClient == null ) {
			this.logger.info( "The agent has not yet been started. Configuration is dropped." );
			return;
		}

		// Update the messaging connection
		this.messagingClient.setApplicationName( this.applicationName );
		this.messagingClient.setScopedInstancePath( this.scopedInstancePath );
		this.messagingClient.setIpAddress( this.ipAddress );
		this.messagingClient.setNeedsModel( needsModel());
		this.messagingClient.setDomain( this.domain );
		this.messagingClient.switchMessagingType( this.messagingType);

		// Deal with injected configurations
		AgentUtils.injectConfigurations(
				this.karafEtc,
				this.applicationName,
				this.scopedInstancePath,
				this.domain,
				this.ipAddress );

		this.logger.info( "The agent was successfully (re)configured." );
	}


	/**
	 * @return a new message processor
	 */
	protected AgentMessageProcessor newMessageProcessor() {
		return new AgentMessageProcessor( this );
	}


	/**
	 * @return a new reconfigurable (messaging) client for the agent
	 */
	protected ReconfigurableClientAgent newReconfigurableClientAgent() {
		return new ReconfigurableClientAgent();
	}


	/**
	 * Reloads user data.
	 */
	void reloadUserData() {

		if( Utils.isEmptyOrWhitespaces( this.parameters )) {
			this.logger.warning( "No parameters were specified in the agent configuration. No user data will be retrieved." );

		} else if( ! this.overrideProperties ) {
			this.logger.fine( "User data are NOT supposed to be used." );

		} else if( Constants.AGENT_RESET.equalsIgnoreCase( this.parameters )) {
			if( getMessagingClient() != null
					&& getMessagingClient().getMessageProcessor() != null )
				((AgentMessageProcessor) getMessagingClient().getMessageProcessor()).resetRequest();

		} else {

			// Retrieve the agent's configuration
			AgentProperties props = null;
			this.logger.fine( "User data are supposed to be used. Retrieving in progress..." );
			if( AgentConstants.PLATFORM_EC2.equalsIgnoreCase( this.parameters )
					|| AgentConstants.PLATFORM_OPENSTACK.equalsIgnoreCase( this.parameters ))
				props = this.userDataHelper.findParametersForAmazonOrOpenStack( this.logger );

			else if( AgentConstants.PLATFORM_AZURE.equalsIgnoreCase( this.parameters ))
				props = this.userDataHelper.findParametersForAzure( this.logger );

			else if( AgentConstants.PLATFORM_VMWARE.equalsIgnoreCase( this.parameters ))
				props = this.userDataHelper.findParametersForVmware( this.logger );

			else if( Constants.AGENT_RESET.equalsIgnoreCase( this.parameters ))
				props = new AgentProperties();

			else
				props = this.userDataHelper.findParametersFromUrl( this.parameters, this.logger );

			// If there was a configuration...
			if( props != null ) {

				// Error messages do not matter when we reset an agent
				String errorMessage = null;
				if( ! Constants.AGENT_RESET.equalsIgnoreCase( this.parameters )
						&& (errorMessage = props.validate()) != null ) {
					this.logger.severe( "An error was found in user data. " + errorMessage );
				}

				this.applicationName = props.getApplicationName();
				this.domain = props.getDomain();
				this.scopedInstancePath = props.getScopedInstancePath();
				if( ! Utils.isEmptyOrWhitespaces( props.getIpAddress())) {
					this.ipAddress = props.getIpAddress();
					this.logger.info( "The agent's address was overwritten from user data and set to " + this.ipAddress );
				}

				try {
					this.logger.info( "Reconfiguring the agent with user data." );
					this.userDataHelper.reconfigureMessaging(
						this.karafEtc,
						props.getMessagingConfiguration());

				} catch( Exception e ) {
					this.logger.severe("Error in messaging reconfiguration from user data: " + e);
				}
			}
		}
	}


	/**
	 * @return the application name
	 */
	@Override
	public String getApplicationName() {
		return this.applicationName;
	}


	/**
	 * @return the scoped instance's path
	 */
	@Override
	public String getScopedInstancePath() {
		return this.scopedInstancePath;
	}


	/**
	 * @return the IP address
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}


	/**
	 * @return the messagingType
	 */
	public String getMessagingType() {
		return this.messagingType;
	}


	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	/**
	 * @param scopedInstancePath the scopedInstancePath to set
	 */
	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
	}


	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters( String parameters ) {
		this.parameters = parameters;

		// Parameters can be changed dynamically
		reloadUserData();
		reconfigure();
	}


	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress( String ipAddress ) {
		if(! Utils.isEmptyOrWhitespaces(ipAddress)) {
			this.ipAddress = ipAddress;
			this.logger.finer( "New IP address set in the agent: " + ipAddress );
		}
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
	 * @param networkInterface the networkInterface to set
	 */
	public void setNetworkInterface( String networkInterface ) {
		this.networkInterface = networkInterface;

		this.logger.info( "New network interface set: " + networkInterface );
		if( ! this.overrideProperties ) {
			this.logger.info( "Resetting the agent's IP address..." );
			this.ipAddress = AgentUtils.findIpAddress( networkInterface );
			this.logger.info( "New IP address: " + this.ipAddress );

		} else {
			this.logger.info( "User data are used. The IP address will not be refreshed." );
		}
	}


	/**
	 * @param domain the domain to set
	 */
	public void setDomain( String domain ) {
		this.domain = domain;
	}


	@Override
	public String getDomain() {
		return this.domain;
	}


	/**
	 * @param messagingType the messaging type to set
	 */
	public void setMessagingType( String messagingType ) {
		this.messagingType = messagingType;
	}


	/**
	 * @return the agent's ID (a human-readable identifier)
	 */
	public String getAgentId() {

		StringBuilder sb = new StringBuilder();
		sb.append( Utils.isEmptyOrWhitespaces( this.scopedInstancePath ) ? "?" : this.scopedInstancePath );
		if( ! Utils.isEmptyOrWhitespaces( this.applicationName ))
			sb.append(" @ ").append( this.applicationName );

		return sb.toString();
	}


	@Override
	public Instance getScopedInstance() {
		return this.scopedInstance;
	}


	public void setScopedInstance(Instance scopedInstance) {
		this.scopedInstance = scopedInstance;
	}


	public List<PluginInterface> getPlugins() {
		return this.plugins;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.agent.AgentMessagingInterface
	 * #agentStatus()
	 */
	@Override
	public String agentStatus() {

		StringBuilder sb = new StringBuilder();

		// Messages
		LinkedBlockingQueue<Message> agentQueue = this.messagingClient.getMessageProcessor().getMessageQueue();
		if( agentQueue.isEmpty() ) {
			sb.append( "There is no message being processed in agent queue\n" );

		} else {
			sb.append( "Agent " + getScopedInstancePath() + " (" + getApplicationName() + ")\n" );
			sb.append( "The number total of messages in agent queue is : " + agentQueue.size() + "\n" );
			sb.append( "The types of messages being processed are : " + "\n");
			for( Message msg : agentQueue ) {
				sb.append( msg.getClass().getSimpleName() + "\n" );
			}
		}

		// Running processes
		Process p = ProcessStore.getProcess(this.applicationName, this.scopedInstancePath);
		if( p != null )
			sb.append( "Be careful. A recipe is under execution." );
		else
			sb.append( "No recipe is under execution." );

		return sb.toString();
	}
}
