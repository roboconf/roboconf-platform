/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.agent.internal.misc.AgentConstants;
import net.roboconf.agent.internal.misc.HeartbeatTask;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.agent.internal.misc.UserDataUtils;
import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
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
public class Agent implements AgentMessagingInterface {

	// Component properties (ipojo)
	String applicationName, scopedInstancePath, ipAddress, targetId, messagingType;
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



	/**
	 * Constructor.
	 */
	public Agent() {
		this.logger = Logger.getLogger( getClass().getName());

		// Set default value for IP address
		// Will be overridden in many cases (e.g. on IaaS with user-data).
		try {
			this.ipAddress = InetAddress.getLocalHost().getHostAddress();
			this.logger.finer( "Local IP address found by the agent: " + this.ipAddress );

		} catch( UnknownHostException e ) {
			this.ipAddress = "127.0.0.1";
			this.logger.warning( "The IP address could not be found. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Starts the agent.
	 * <p>
	 * It is invoked by iPojo when an instance becomes VALID.
	 * </p>
	 */
	public void start() {

		this.logger.info( "Agent '" + getAgentId() + "' is about to be launched." );

		this.messagingClient = new ReconfigurableClientAgent();
		AgentMessageProcessor messageProcessor = new AgentMessageProcessor( this );
		this.messagingClient.associateMessageProcessor( messageProcessor );

		// Do we need to override properties with user data?
		if( Utils.isEmptyOrWhitespaces( this.targetId )) {
			this.logger.warning( "No target ID was specified in the agent configuration. No user data will be retrieved." );

		} else if( ! this.overrideProperties ) {
			this.logger.fine( "User data are NOT supposed to be used." );

		} else {
			this.logger.fine( "User data are supposed to be used. Retrieving in progress..." );
			AgentProperties props = null;
			if( AgentConstants.PLATFORM_EC2.equalsIgnoreCase( this.targetId )
					|| AgentConstants.PLATFORM_OPENSTACK.equalsIgnoreCase( this.targetId ))
				props = UserDataUtils.findParametersForAmazonOrOpenStack( this.logger );

			else if( AgentConstants.PLATFORM_AZURE.equalsIgnoreCase( this.targetId ))
				props = UserDataUtils.findParametersForAzure( this.logger );

			else
				this.logger.warning( "Unknown target ID. No user data will be retrieved." );

			if( props != null ) {
				String errorMessage = props.validate();
				if( errorMessage != null )
					this.logger.severe( "An error was found in user data. " + errorMessage );

				this.applicationName = props.getApplicationName();
				this.ipAddress = props.getIpAddress();
				this.scopedInstancePath = props.getScopedInstancePath();

				try {
					this.logger.info( "Reconfiguring the agent with user data." );
					UserDataUtils.reconfigureMessaging(
						this.karafEtc,
						props.getMessagingConfiguration());

				} catch(IOException e) {
					this.logger.severe("Error in messaging reconfiguration from user data: " + e);
				}
			}
		}

		reconfigure();

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
	public void reconfigure() {

		// This method is invoked when properties change.
		// It is not related to life cycle (start/stop).
		if( this.messagingClient == null ) {
			this.logger.info( "The agent has not yet been started. Configuration is dropped." );
			return;
		}

		// Update the messaging connection
		this.messagingClient.setApplicationName( this.applicationName );
		this.messagingClient.setScopedInstancePath( this.scopedInstancePath );
		this.messagingClient.setIpAddress( this.ipAddress );
		this.messagingClient.setNeedsModel( needsModel());
		this.messagingClient.switchMessagingType( this.messagingType);

		this.logger.info( "The agent was successfully (re)configured." );
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
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress( String ipAddress ) {
		this.ipAddress = ipAddress;
		this.logger.finer( "New IP address set in the agent: " + ipAddress );
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
	 * @param messagingType the messaging type to set
	 */
	public void setMessagingType( String messagingType ) {
		this.messagingType = messagingType;
	}


	/**
	 * @return the agent's ID (a human-readable identifier)
	 */
	String getAgentId() {

		StringBuilder sb = new StringBuilder();
		sb.append( Utils.isEmptyOrWhitespaces( this.scopedInstancePath ) ? "?" : this.scopedInstancePath );
		if( ! Utils.isEmptyOrWhitespaces( this.applicationName ))
			sb.append(" @ ").append(this.applicationName);

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
}
