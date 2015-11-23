/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.management;

import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.IRandomMngr;
import net.roboconf.dm.internal.api.impl.ApplicationMngrImpl;
import net.roboconf.dm.internal.api.impl.ApplicationTemplateMngrImpl;
import net.roboconf.dm.internal.api.impl.CommandsMngrImpl;
import net.roboconf.dm.internal.api.impl.ConfigurationMngrImpl;
import net.roboconf.dm.internal.api.impl.DebugMngrImpl;
import net.roboconf.dm.internal.api.impl.InstancesMngrImpl;
import net.roboconf.dm.internal.api.impl.MessagingMngrImpl;
import net.roboconf.dm.internal.api.impl.NotificationMngrImpl;
import net.roboconf.dm.internal.api.impl.PreferencesMngrImpl;
import net.roboconf.dm.internal.api.impl.RandomMngrImpl;
import net.roboconf.dm.internal.api.impl.TargetHandlerResolverImpl;
import net.roboconf.dm.internal.api.impl.TargetsMngrImpl;
import net.roboconf.dm.internal.autonomic.RuleBasedEventHandler;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.environment.messaging.RCDm;
import net.roboconf.dm.internal.tasks.CheckerHeartbeatsTask;
import net.roboconf.dm.internal.tasks.CheckerMessagesTask;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IApplicationTemplateMngr;
import net.roboconf.dm.management.api.ICommandsMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IDebugMngr;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.management.api.IRuleBasedEventHandler;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.target.api.TargetHandler;

/**
 * This class acts as a front-end to access the various features of the DM.
 * <p>
 * This class is designed to work with OSGi, iPojo and Admin Config.<br>
 * But it can also be used programmatically.
 * </p>
 * <pre><code>
 * // Configure
 * Manager manager = new Manager();
 * manager.setMessagingType( "rabbitmq" );
 *
 * // Change the way we resolve handlers for deployment targetsMngr
 * manager.setTargetResolver( ... );
 *
 * // Connect to the messaging server
 * manager.start();
 * </code></pre>
 *
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class Manager {

	// Constants
	private static final long TIMER_PERIOD = 6000;

	// Injected by iPojo or Admin Config
	protected String messagingType;

	// FIXME: move it into a new API
	protected int autonomicMaxRoots = -1;

	// Internal fields
	protected final Logger logger = Logger.getLogger( getClass().getName());
	protected Timer timer;
	protected String oldMessagingType;

	private RCDm messagingClient;

	// API access
	private final NotificationMngrImpl notificationMngr;
	private final MessagingMngrImpl messagingMngr;
	private final ApplicationMngrImpl applicationMngr;
	private final InstancesMngrImpl instancesMngr;
	private final IRandomMngr randomMngr;
	private final IPreferencesMngr preferencesMngr;

	private final IConfigurationMngr configurationMngr;
	private final IApplicationTemplateMngr applicationTemplateMngr;
	private final ITargetsMngr targetsMngr;
	private final IDebugMngr debugMngr;
	private final ICommandsMngr commandsMngr;

	private final TargetHandlerResolverImpl defaultTargetHandlerResolver;

	// Dirty hack
	private final IRuleBasedEventHandler ruleBasedHandler;


	/**
	 * Constructor.
	 */
	public Manager() {
		super();

		// Home-made DI.
		// We do not want to mix N frameworks.
		this.notificationMngr = new NotificationMngrImpl();
		this.configurationMngr = new ConfigurationMngrImpl();
		this.randomMngr = new RandomMngrImpl();
		this.preferencesMngr = new PreferencesMngrImpl();

		this.messagingMngr = new MessagingMngrImpl();
		this.defaultTargetHandlerResolver = new TargetHandlerResolverImpl();
		this.targetsMngr = new TargetsMngrImpl( this.configurationMngr );
		this.debugMngr = new DebugMngrImpl( this.messagingMngr, this.notificationMngr );
		this.commandsMngr = new CommandsMngrImpl( this );

		this.applicationMngr = new ApplicationMngrImpl( this.notificationMngr, this.configurationMngr, this.targetsMngr, this.messagingMngr, this.randomMngr );
		this.applicationTemplateMngr = new ApplicationTemplateMngrImpl( this.notificationMngr, this.targetsMngr, this.applicationMngr, this.configurationMngr );
		this.applicationMngr.setApplicationTemplateMngr( this.applicationTemplateMngr );

		this.instancesMngr = new InstancesMngrImpl( this.messagingMngr, this.notificationMngr, this.targetsMngr, this.randomMngr );
		this.instancesMngr.setTargetHandlerResolver( this.defaultTargetHandlerResolver );

		// FIXME: to update once we have the commands API
		this.ruleBasedHandler = new RuleBasedEventHandler( this );
		this.instancesMngr.setRuleBasedHandler( this.ruleBasedHandler );
	}


	// iPojo stuff


	/**
	 * Starts the manager.
	 * <p>
	 * It is invoked by iPojo when an instance becomes VALID.
	 * </p>
	 */
	public void start() {
		this.logger.info( "The DM is about to be launched." );

		// Start the messaging
		DmMessageProcessor messageProcessor = new DmMessageProcessor( this );
		this.messagingClient = new RCDm( this.applicationMngr );
		this.messagingClient.associateMessageProcessor( messageProcessor );
		this.messagingMngr.setMessagingClient( this.messagingClient );

		// Run the timer
		this.timer = new Timer( "Roboconf's Management Timer", false );
		this.timer.scheduleAtFixedRate( new CheckerMessagesTask( this.applicationMngr, this.messagingMngr ), 0, TIMER_PERIOD );
		this.timer.scheduleAtFixedRate( new CheckerHeartbeatsTask( this.applicationMngr ), 0, Constants.HEARTBEAT_PERIOD );

		// Configure the messaging
		reconfigure();

		// Restore what is necessary
		this.applicationTemplateMngr.restoreTemplates();
		this.applicationMngr.restoreApplications();

		// We must update instance states after we restored applications
		for( ManagedApplication ma : this.applicationMngr.getManagedApplications())
			this.instancesMngr.restoreInstanceStates( ma );

		this.logger.info( "The DM was launched." );
	}


	/**
	 * Stops the manager.
	 * <p>
	 * It is invoked by iPojo when an instance becomes INVALID.
	 * </p>
	 */
	public void stop() {

		this.logger.info( "The DM is about to be stopped." );
		if( this.timer != null ) {
			this.timer.cancel();
			this.timer =  null;
		}

		if( this.messagingClient != null ) {

			// Stops listening to the debug queue.
			try {
				this.messagingClient.listenToTheDm( ListenerCommand.STOP );
			} catch ( IOException e ) {
				this.logger.log( Level.WARNING, "Cannot stop to listen to the debug queue", e );
			}

			this.messagingClient.getMessageProcessor().stopProcessor();
			this.messagingClient.getMessageProcessor().interrupt();
			try {
				this.messagingClient.closeConnection();

			} catch( IOException e ) {
				this.logger.warning( "The messaging client could not be terminated correctly. " + e.getMessage());
				Utils.logException( this.logger, e );
			}
		}

		this.logger.info( "The DM was stopped." );
	}


	/**
	 * This method is invoked by iPojo every time a new target handler appears.
	 * @param targetItf the appearing target handler
	 */
	public void targetAppears( TargetHandler targetItf ) {
		this.defaultTargetHandlerResolver.addTargetHandler( targetItf );
	}


	/**
	 * This method is invoked by iPojo every time a target handler disappears.
	 * @param targetItf the disappearing target handler
	 */
	public void targetDisappears( TargetHandler targetItf ) {
		this.defaultTargetHandlerResolver.removeTargetHandler( targetItf );
	}


	/**
	 * This method is invoked by iPojo every time a DM listener appears.
	 * @param targetItf the appearing listener
	 */
	public void listenerAppears( IDmListener listener ) {
		this.notificationMngr.addListener( listener );
	}


	/**
	 * This method is invoked by iPojo every time a DM listener disappears.
	 * @param listener the disappearing listener
	 */
	public void listenerDisappears( IDmListener listener ) {
		this.notificationMngr.removeListener( listener );
	}


	// Reconfiguration


	/**
	 * This method reconfigures the manager.
	 * <p>
	 * It is NOT invoked DIRECTLY by iPojo anymore.
	 * </p>
	 */
	public void reconfigure() {

		// Update the messaging client
		if( this.messagingClient != null ) {
			this.messagingClient.switchMessagingType( this.messagingType );
			try {
				if( this.messagingClient.isConnected())
					this.messagingClient.listenToTheDm( ListenerCommand.START );

			} catch ( IOException e ) {
				this.logger.log( Level.WARNING, "Cannot start to listen to the debug queue", e );
			}
		}

		// We must update instance states after we switched the messaging configuration.
		for( ManagedApplication ma : this.applicationMngr.getManagedApplications())
			this.instancesMngr.restoreInstanceStates( ma );

		this.logger.info( "The DM was successfully (re)configured." );
	}


	// Setters

	public void setMessagingType( String messagingType ) {

		// Properties are injected on every modification.
		// so, we just want to track changes.
		if( ! Objects.equals( this.messagingType, messagingType )) {
			this.oldMessagingType = this.messagingType;
			this.messagingType = messagingType;

			// Explicitly require a reconfiguration.
			// We don't let iPojo deal with it anymore since there may be parameters in the
			// DM that are not related to the messaging. In fact, most of the messaging configuration
			// was moved in messaging bundles. So, we only want to reconfigure the messaging client
			// when the messaging type changes.
			reconfigure();
		}
	}


	public void setAutonomicMaxRoots( int autonomicMaxRoots ) {
		this.logger.info( "The autonomic's maximum roots number is set to " + autonomicMaxRoots + "." );
		this.autonomicMaxRoots = autonomicMaxRoots;
	}


	public void setTargetResolver( ITargetHandlerResolver targetHandlerResolver ) {

		if( targetHandlerResolver == null )
			this.instancesMngr.setTargetHandlerResolver( this.defaultTargetHandlerResolver );
		else
			this.instancesMngr.setTargetHandlerResolver( targetHandlerResolver );
	}


	// Getters

	public INotificationMngr notificationMngr() {
		return this.notificationMngr;
	}

	public IMessagingMngr messagingMngr() {
		return this.messagingMngr;
	}

	public IApplicationMngr applicationMngr() {
		return this.applicationMngr;
	}

	public IInstancesMngr instancesMngr() {
		return this.instancesMngr;
	}

	public IConfigurationMngr configurationMngr() {
		return this.configurationMngr;
	}

	public IApplicationTemplateMngr applicationTemplateMngr() {
		return this.applicationTemplateMngr;
	}

	public ITargetsMngr targetsMngr() {
		return this.targetsMngr;
	}

	public IDebugMngr debugMngr() {
		return this.debugMngr;
	}

	public ICommandsMngr commandsMngr() {
		return this.commandsMngr;
	}

	public IPreferencesMngr preferencesMngr() {
		return this.preferencesMngr;
	}

	/**
	 * FIXME: to remove once we have the autonomic configurator API.
	 * @return the autonomicMaxRoots
	 */
	public int getAutonomicMaxRoots() {
		return this.autonomicMaxRoots;
	}

	/**
	 * FIXME: to update once we have the commands API.
	 * @return the ruleBasedHandler
	 */
	public IRuleBasedEventHandler getRuleBasedHandler() {
		return this.ruleBasedHandler;
	}
}
