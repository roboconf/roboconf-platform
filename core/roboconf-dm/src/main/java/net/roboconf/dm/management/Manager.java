/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import net.roboconf.core.Constants;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.runtime.IReconfigurable;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.IRandomMngr;
import net.roboconf.dm.internal.api.impl.ApplicationMngrImpl;
import net.roboconf.dm.internal.api.impl.ApplicationTemplateMngrImpl;
import net.roboconf.dm.internal.api.impl.AutonomicMngrImpl;
import net.roboconf.dm.internal.api.impl.CommandsMngrImpl;
import net.roboconf.dm.internal.api.impl.ConfigurationMngrImpl;
import net.roboconf.dm.internal.api.impl.DebugMngrImpl;
import net.roboconf.dm.internal.api.impl.InstancesMngrImpl;
import net.roboconf.dm.internal.api.impl.MessagingMngrImpl;
import net.roboconf.dm.internal.api.impl.NotificationMngrImpl;
import net.roboconf.dm.internal.api.impl.PreferencesMngrImpl;
import net.roboconf.dm.internal.api.impl.RandomMngrImpl;
import net.roboconf.dm.internal.api.impl.TargetConfiguratorImpl;
import net.roboconf.dm.internal.api.impl.TargetHandlerResolverImpl;
import net.roboconf.dm.internal.api.impl.TargetsMngrImpl;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.environment.messaging.RCDm;
import net.roboconf.dm.internal.tasks.CheckerForHeartbeatsTask;
import net.roboconf.dm.internal.tasks.CheckerForStoredMessagesTask;
import net.roboconf.dm.internal.tasks.CheckerForTargetsConfigurationTask;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.jmx.ManagerMBean;
import net.roboconf.dm.management.api.IApplicationMngr;
import net.roboconf.dm.management.api.IApplicationTemplateMngr;
import net.roboconf.dm.management.api.IAutonomicMngr;
import net.roboconf.dm.management.api.ICommandsMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IDebugMngr;
import net.roboconf.dm.management.api.IInstancesMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.messaging.api.business.ListenerCommand;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
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
public class Manager implements IReconfigurable, ManagerMBean {

	// Constants
	private static final long TIMER_PERIOD = 6000;

	// Injected by iPojo or Admin Config
	protected String messagingType;
	protected String domain = Constants.DEFAULT_DOMAIN;
	protected IPreferencesMngr preferencesMngr;
	protected DataSource dataSource;

	// Internal fields
	protected final Logger logger = Logger.getLogger( getClass().getName());
	protected Timer timer;

	private RCDm messagingClient;

	// API access
	private final NotificationMngrImpl notificationMngr;
	private final MessagingMngrImpl messagingMngr;
	private final ApplicationMngrImpl applicationMngr;
	private final InstancesMngrImpl instancesMngr;

	private final IConfigurationMngr configurationMngr;
	private final IApplicationTemplateMngr applicationTemplateMngr;
	private final ITargetsMngr targetsMngr;
	private final IDebugMngr debugMngr;
	private final ICommandsMngr commandsMngr;
	private final IAutonomicMngr autonomicMngr;

	private final TargetHandlerResolverImpl defaultTargetHandlerResolver;

	// Private API
	private final IRandomMngr randomMngr;
	private final TargetConfiguratorImpl targetConfigurator;


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

		this.messagingMngr = new MessagingMngrImpl();
		this.defaultTargetHandlerResolver = new TargetHandlerResolverImpl();
		this.targetsMngr = new TargetsMngrImpl( this.configurationMngr );
		this.debugMngr = new DebugMngrImpl( this.messagingMngr, this.notificationMngr );
		this.commandsMngr = new CommandsMngrImpl( this );

		this.autonomicMngr = new AutonomicMngrImpl( this.commandsMngr );
		this.applicationMngr = new ApplicationMngrImpl(
				this.notificationMngr, this.configurationMngr,
				this.targetsMngr, this.messagingMngr,
				this.randomMngr, this.autonomicMngr );

		this.applicationTemplateMngr = new ApplicationTemplateMngrImpl( this.notificationMngr, this.targetsMngr, this.applicationMngr, this.configurationMngr );
		this.applicationMngr.setApplicationTemplateMngr( this.applicationTemplateMngr );

		this.targetConfigurator = new TargetConfiguratorImpl();
		this.targetConfigurator.setTargetHandlerResolver( this.defaultTargetHandlerResolver );

		this.instancesMngr = new InstancesMngrImpl( this.messagingMngr, this.notificationMngr, this.targetsMngr, this.randomMngr, this.targetConfigurator );
		this.instancesMngr.setTargetHandlerResolver( this.defaultTargetHandlerResolver );
		this.instancesMngr.setRuleBasedHandler( this.autonomicMngr );
		this.instancesMngr.setDmDomain( this.domain );

		// The manager is supposed to be an API.
		// To make it simple to use in non-OSGi environments, we instantiate a default set of preferences.
		// This will prevent NPEs. In OSGi environments, iPojo will override it.
		setPreferencesMngr( new PreferencesMngrImpl());
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
		this.messagingClient.setDomain( this.domain );
		this.messagingClient.associateMessageProcessor( messageProcessor );
		this.messagingMngr.setMessagingClient( this.messagingClient );

		// Start the target configurator
		this.targetConfigurator.start();

		// Run the timer
		this.timer = new Timer( "Roboconf's Management Timer", false );
		this.timer.scheduleAtFixedRate( new CheckerForStoredMessagesTask( this.applicationMngr, this.messagingMngr ), 0, TIMER_PERIOD );
		this.timer.scheduleAtFixedRate( new CheckerForTargetsConfigurationTask( this.targetConfigurator ), 0, TIMER_PERIOD );
		this.timer.scheduleAtFixedRate(
				new CheckerForHeartbeatsTask( this.applicationMngr, this.notificationMngr ),
				0, Constants.HEARTBEAT_PERIOD );

		// Configure the messaging
		reconfigure();

		// Restore what is necessary
		this.applicationTemplateMngr.restoreTemplates();
		this.applicationMngr.restoreApplications();

		// We must update instance states after we restored applications
		restoreAllInstances();

		// Enable notifications to listeners
		this.notificationMngr.enableNotifications();

		this.logger.info( "The DM was launched." );
	}


	/**
	 * Stops the manager.
	 * <p>
	 * It is invoked by iPojo when an instance becomes INVALID.
	 * </p>
	 */
	public void stop() {

		// Cancel the timer
		this.logger.info( "The DM is about to be stopped." );
		if( this.timer != null ) {
			this.timer.cancel();
			this.timer =  null;
		}

		// Save the instances
		for( ManagedApplication ma : this.applicationMngr.getManagedApplications())
			ConfigurationUtils.saveInstances( ma );

		// Disable notifications to listeners
		this.notificationMngr.disableNotifications();

		// Stop the target configurator
		this.targetConfigurator.stop();

		// Stops listening to the debug queue.
		if( this.messagingClient != null ) {
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

		// When a target is deployed, we may also have to update instance states.
		// Consider as an example when the DM restarts. Targets may be injected
		// before and after the pojo was started by iPojo.

		// See #519 for more details.
		// Notice we restore instances only when the DM was started (the messaging
		// must be ready). If it is not started, do nothing. The "start" method
		// will trigger the restoration.

		// We consider the DM is started if the timer is not null.
		if( this.timer != null )
			restoreInstancesFrom( targetItf );
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
	@Override
	public void reconfigure() {

		// Update the messaging client
		this.logger.info( "Reconfiguration requested in the DM." );
		if( this.messagingClient != null ) {
			this.messagingClient.setDomain( this.domain );
			this.messagingClient.switchMessagingType( this.messagingType );
			try {
				if( this.messagingClient.isConnected())
					this.messagingClient.listenToTheDm( ListenerCommand.START );

			} catch ( IOException e ) {
				this.logger.log( Level.WARNING, "Cannot start to listen to the debug queue", e );
			}
		}

		// We must update instance states after we switched the messaging configuration.
		restoreAllInstances();

		this.logger.info( "The DM was successfully (re)configured." );
	}


	// Setters


	/**
	 * Sets the messaging type.
	 * <p>
	 * If the set messaging type is different than the previous one,
	 * this method triggers a re configuration.
	 * </p>
	 *
	 * @param messagingType the messaging type
	 */
	public void setMessagingType( String messagingType ) {

		// Properties are injected on every modification.
		// So, we just want to track changes.

		// We only want to reconfigure the messaging client
		// when the messaging type changes.
		if( ! Objects.equals( this.messagingType, messagingType )) {
			this.messagingType = messagingType;
			this.logger.fine( "Messaging type set to " + this.messagingType );

			// Explicitly require a reconfiguration.
			reconfigure();
		}
	}


	/**
	 * @param domain the domain to set
	 */
	public void setDomain( String domain ) {

		// Properties are injected on every modification.
		// So, we just want to track changes.

		// We only want to reconfigure the messaging client
		// when the domain changes.
		if( ! Objects.equals( this.domain, domain )) {
			this.domain = domain;
			this.logger.fine( "Domain set to " + domain );
			this.instancesMngr.setDmDomain( domain );

			// Explicitly require a reconfiguration.
			reconfigure();
		}
	}


	/**
	 * @param preferencesMngr the preferencesMngr to set
	 */
	public void setPreferencesMngr( IPreferencesMngr preferencesMngr ) {
		this.preferencesMngr = preferencesMngr;
		((RandomMngrImpl) this.randomMngr).setPreferencesMngr( preferencesMngr );
		((AutonomicMngrImpl) this.autonomicMngr).setPreferencesMngr( preferencesMngr );
	}


	/**
	 * Sets the target resolver.
	 * @param targetHandlerResolver a resolver for target handlers
	 */
	public void setTargetResolver( ITargetHandlerResolver targetHandlerResolver ) {

		if( targetHandlerResolver == null ) {
			this.targetConfigurator.setTargetHandlerResolver( this.defaultTargetHandlerResolver );
			this.instancesMngr.setTargetHandlerResolver( this.defaultTargetHandlerResolver );
		} else {
			this.targetConfigurator.setTargetHandlerResolver( targetHandlerResolver );
			this.instancesMngr.setTargetHandlerResolver( targetHandlerResolver );
		}
	}


	// Getters


	/**
	 * @return the domain
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 * @return the notification API
	 */
	public INotificationMngr notificationMngr() {
		return this.notificationMngr;
	}

	/**
	 * @return the messaging API
	 */
	public IMessagingMngr messagingMngr() {
		return this.messagingMngr;
	}

	/**
	 * @return the applications management API
	 */
	public IApplicationMngr applicationMngr() {
		return this.applicationMngr;
	}

	/**
	 * @return the instances management API
	 */
	public IInstancesMngr instancesMngr() {
		return this.instancesMngr;
	}

	/**
	 * @return the configuration API
	 */
	public IConfigurationMngr configurationMngr() {
		return this.configurationMngr;
	}

	/**
	 * @return the application templates management API
	 */
	public IApplicationTemplateMngr applicationTemplateMngr() {
		return this.applicationTemplateMngr;
	}

	/**
	 * @return the targets management API
	 */
	public ITargetsMngr targetsMngr() {
		return this.targetsMngr;
	}

	/**
	 * @return the debug API
	 */
	public IDebugMngr debugMngr() {
		return this.debugMngr;
	}

	/**
	 * @return the commands API
	 */
	public ICommandsMngr commandsMngr() {
		return this.commandsMngr;
	}

	/**
	 * @return the preferences API
	 */
	public IPreferencesMngr preferencesMngr() {
		return this.preferencesMngr;
	}

	/**
	 * @return the autonomic API
	 */
	public IAutonomicMngr autonomicMngr() {
		return this.autonomicMngr;
	}


	// Convenience methods for non-OSGi environments

	/**
	 * Adds a messaging client factory.
	 * <p>
	 * WARNING: this method is made available only to be used in non-OSGi environments
	 * (e.g. Maven, embedded mode, etc). If you are not sure, do not use it.
	 * </p>
	 *
	 * @param clientFactory a non-null client factory
	 */
	public void addMessagingFactory( IMessagingClientFactory clientFactory ) {

		if( this.messagingClient.getRegistry() == null ) {
			MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
			this.messagingClient.setRegistry( registry );
		}

		this.messagingClient.getRegistry().addMessagingClientFactory( clientFactory );
	}


	/**
	 * Removes a messaging client factory.
	 * <p>
	 * WARNING: this method is made available only to be used in non-OSGi environments
	 * (e.g. Maven, embedded mode, etc). If you are not sure, do not use it.
	 * </p>
	 *
	 * @param clientFactory a non-null client factory
	 */
	public void removeMessagingFactory( IMessagingClientFactory clientFactory ) {

		if( this.messagingClient.getRegistry() != null )
			this.messagingClient.getRegistry().removeMessagingClientFactory( clientFactory );
	}


	/**
	 * @return the data source (use at your own risks!)
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}


	// MBean methods


	@Override
	public int getApplicationCount() {
		return this.applicationMngr.getManagedApplications().size();
	}


	@Override
	public int getApplicationTemplateCount() {
		return this.applicationTemplateMngr.getApplicationTemplates().size();
	}


	@Override
	public int getInstancesCount() {

		int result = 0;
		for( ManagedApplication ma : this.applicationMngr.getManagedApplications())
			result += InstanceHelpers.getAllInstances( ma.getApplication()).size();

		return result;
	}


	@Override
	public int getScopedInstancesCount() {

		int result = 0;
		for( ManagedApplication ma : this.applicationMngr.getManagedApplications())
			result += InstanceHelpers.findAllScopedInstances( ma.getApplication()).size();

		return result;
	}


	// Private utilities


	/**
	 * Restores the states of all the instances from the current target handlers.
	 */
	void restoreAllInstances() {

		// instancesMngr() instead of this.instancesMngr (for unit tests).
		this.logger.fine( "Restoring all the instance states from the current target handlers." );
		for( ManagedApplication ma : this.applicationMngr.getManagedApplications()) {

			// Build a new snapshot on every loop
			List<TargetHandler> snapshot = this.defaultTargetHandlerResolver.getTargetHandlersSnapshot();
			for( TargetHandler targetHandler : snapshot )
				instancesMngr().restoreInstanceStates( ma, targetHandler );
		}
	}


	/**
	 * Restores the states of all the instances from a given target handler.
	 */
	void restoreInstancesFrom( TargetHandler targetHandler ) {

		// instancesMngr() instead of this.instancesMngr (for unit tests).
		this.logger.fine( "Restoring the instance states with the '" + targetHandler.getTargetId() + "' target handler." );
		for( ManagedApplication ma : this.applicationMngr.getManagedApplications())
			instancesMngr().restoreInstanceStates( ma, targetHandler );
	}
}
