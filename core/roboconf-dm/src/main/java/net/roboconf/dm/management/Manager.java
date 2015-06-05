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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.delegates.ApplicationMngrDelegate;
import net.roboconf.dm.internal.delegates.ApplicationTemplateMngrDelegate;
import net.roboconf.dm.internal.delegates.InstanceMngrDelegate;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.environment.messaging.RCDm;
import net.roboconf.dm.internal.tasks.CheckerHeartbeatsTask;
import net.roboconf.dm.internal.tasks.CheckerMessagesTask;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.api.client.IClient.ListenerCommand;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * A class to manage a collection of applications.
 * <p>
 * This class acts as an interface to communicate with the agents.
 * It does not manage anything real (like life cycles). It only
 * handles some kind of cache (cache for application files, cache for
 * instance states).
 * </p>
 * <p>
 * The agents are the most well-placed to manage life cycles and the states
 * of instances. Therefore, the DM does the minimal set of actions on instances.
 * </p>
 * <p>
 * This class is designed to work with OSGi, iPojo and Admin Config.<br />
 * But it can also be used programmatically.
 * </p>
 * <code><pre>
 * // Configure
 * Manager manager = new Manager();
 * manager.setMessagingType( "rabbitmq" );
 *
 * // Change the way we resolve handlers for deployment targets
 * manager.setTargetResolver( ... );
 *
 * // Connect to the messaging server
 * manager.start();
 * </pre></code>
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
	protected final List<TargetHandler> targetHandlers = new ArrayList<> ();
	protected String configurationDirectoryLocation, messagingType;

	// Monitoring manager optional dependency. May be null.
	// @GuardedBy this
	private MonitoringManagerService monitoringManager;

	// Internal fields
	protected final Logger logger = Logger.getLogger( getClass().getName());
	protected final ApplicationTemplateMngrDelegate templateManager;
	protected final ApplicationMngrDelegate appManager;
	protected final InstanceMngrDelegate instanceManager;
	protected Timer timer;

	private final List<MsgEcho> echoMessages = new ArrayList<>();
	private RCDm messagingClient;
	File configurationDirectory;


	/**
	 * Constructor.
	 */
	public Manager() {
		super();
		this.templateManager = new ApplicationTemplateMngrDelegate();
		this.appManager = new ApplicationMngrDelegate();
		this.instanceManager = new InstanceMngrDelegate( this );
	}


	/**
	 * Starts the manager.
	 * <p>
	 * It is invoked by iPojo when an instance becomes VALID.
	 * </p>
	 */
	public void start() {

		this.logger.info( "The DM is about to be launched." );

		DmMessageProcessor messageProcessor = new DmMessageProcessor( this, this.appManager );
		this.messagingClient = new RCDm( this.appManager );
		this.messagingClient.associateMessageProcessor( messageProcessor );

		this.timer = new Timer( "Roboconf's Management Timer", false );
		this.timer.scheduleAtFixedRate( new CheckerMessagesTask( this.appManager, this.messagingClient ), 0, TIMER_PERIOD );
		this.timer.scheduleAtFixedRate( new CheckerHeartbeatsTask( this.appManager ), 0, Constants.HEARTBEAT_PERIOD );

		reconfigure();

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

		for( ManagedApplication ma : this.appManager.getManagedApplications()) {
			// Stop the monitoring.
			// May be called before the unbindMonitoringManager() method.
			synchronized (this) {
				if (this.monitoringManager != null) {
					this.logger.fine( "Stopping monitoring manager..." );
					// Disable the monitoring manager.
					this.monitoringManager.stopMonitoring();
					this.monitoringManager = null;
					this.logger.fine( "Monitoring manager stopped!" );
				}
			}

			saveConfiguration( ma );
		}

		this.logger.info( "The DM was stopped." );
	}


	/**
	 * Binds to the {@code MonitoringManagerService}.
	 * @param monitoringManager the {@code MonitoringManagerService}
	 */
	public synchronized void bindMonitoringManager( MonitoringManagerService monitoringManager ) {

		this.logger.fine( "Binding to monitoring manager service..." );
		if (this.configurationDirectory != null) {
			// We only start the monitoring if the configuration directory is configured.
			// If not, the monitoring will be started in the reconfigure() method.
			monitoringManager.startMonitoring( this.configurationDirectory );

			// Register the applications in the monitoring manager.
			for (ManagedApplication app : this.appManager.getManagedApplications())
				monitoringManager.addApplication( app.getApplication());

			this.monitoringManager = monitoringManager;
		}
		this.logger.fine( "Now bound to monitoring manager service!" );
	}


	/**
	 * Unbinds from the {@code MonitoringManagerService}.
	 * @param monitoringManager the {@code MonitoringManagerService}
	 */
	public synchronized void unbindMonitoringManager( MonitoringManagerService monitoringManager ) {

		// May be called after the stop method.
		if (this.monitoringManager != null) {
			this.logger.fine( "Unbinding from monitoring manager service..." );
			// Disable the monitoring manager.
			monitoringManager.stopMonitoring();
			this.monitoringManager = null;
			this.logger.fine( "Unbound from monitoring manager service!" );
		}
	}


	/**
	 * This method is invoked by iPojo every time a new target handler appears.
	 * @param targetItf the appearing target handler.
	 */
	public void targetAppears( TargetHandler targetItf ) {
		if( targetItf != null ) {
			this.logger.info( "Target handler '" + targetItf.getTargetId() + "' is now available in Roboconf's DM." );
			this.targetHandlers.add( targetItf );
			listTargets();
		}
	}


	/**
	 * This method is invoked by iPojo every time a target handler disappears.
	 * @param targetItf the disappearing target handler.ers
	 */
	public void targetDisappears( TargetHandler targetItf ) {

		// May happen if a target could not be instantiated
		// (iPojo uses proxies). In this case, it results in a NPE here.
		if( targetItf == null ) {
			this.logger.info( "An invalid target handler is removed." );
		} else {
			this.targetHandlers.remove( targetItf );
			this.logger.info( "Target handler '" + targetItf.getTargetId() + "' is not available anymore in Roboconf's DM." );
		}

		listTargets();
	}


	/**
	 * This method is invoked by iPojo every time a target is modified.
	 * @param targetItf the modified target handler.
	 */
	public void targetWasModified( TargetHandler targetItf ) {
		this.logger.info( "Target handler '" + targetItf.getTargetId() + "' was modified in Roboconf's DM." );
		listTargets();
	}


	/**
	 * This method lists the available target and logs it.
	 */
	public void listTargets() {

		if( this.targetHandlers.isEmpty()) {
			this.logger.info( "No target was found for Roboconf's DM." );

		} else {
			StringBuilder sb = new StringBuilder( "Available target in Roboconf's DM: " );
			for( Iterator<TargetHandler> it = this.targetHandlers.iterator(); it.hasNext(); ) {
				sb.append( it.next().getTargetId());
				if( it.hasNext())
					sb.append( ", " );
			}

			sb.append( "." );
			this.logger.info( sb.toString());
		}
	}


	/**
	 * @param configurationDirectoryLocation the configurationDirectoryLocation to set
	 */
	public void setConfigurationDirectoryLocation( String configurationDirectoryLocation ) {
		this.configurationDirectoryLocation = configurationDirectoryLocation;
	}


	/**
	 * @return the targetHandlers
	 */
	public List<TargetHandler> getTargetHandlers() {
		return Collections.unmodifiableList( this.targetHandlers );
	}


	/**
	 * @param messagingType the messagingType to set
	 */
	public void setMessagingType( String messagingType ) {
		this.messagingType = messagingType;
	}


	/**
	 * @return the configurationDirectoryLocation
	 */
	public String getConfigurationDirectoryLocation() {
		return this.configurationDirectoryLocation;
	}


	/**
	 * This method reconfigures the manager.
	 * <p>
	 * It is invoked by iPojo when the configuration changes.
	 * It may be invoked before the start() method is.
	 * </p>
	 */
	public void reconfigure() {

		// Backup the current
		for( ManagedApplication ma : this.appManager.getManagedApplications())
			saveConfiguration( ma );

		// Update the configuration directory
		File defaultConfigurationDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf-dm" );
		if( Utils.isEmptyOrWhitespaces( this.configurationDirectoryLocation )) {
			this.logger.warning( "Invalid location for the configuration directory (empty or null). Switching to " + defaultConfigurationDirectory );
			this.configurationDirectory = defaultConfigurationDirectory;

		} else {
			this.configurationDirectory = new File( this.configurationDirectoryLocation );
			try {
				Utils.createDirectory( this.configurationDirectory );

			} catch( IOException e ) {
				this.logger.warning( "Could not create " + this.configurationDirectory + ". Switching to " + defaultConfigurationDirectory );
				this.configurationDirectory = defaultConfigurationDirectory;
			}
		}

		// Restart the monitoring, using the new configuration directory.
		synchronized (this) {
			if( this.monitoringManager != null ) {
				this.monitoringManager.stopMonitoring();
				this.monitoringManager.startMonitoring( this.configurationDirectory );
				// Applications are re-added as they are reloaded, in the restoreApplications() method.
			}
		}

		// Update the messaging client
		if( this.messagingClient != null ) {
			this.messagingClient.switchMessagingType(this.messagingType);
			try {
				if( this.messagingClient.isConnected())
					this.messagingClient.listenToTheDm( ListenerCommand.START );

			} catch ( IOException e ) {
				this.logger.log( Level.WARNING, "Cannot start to listen to the debug queue", e );
			}
		}


		// Reset and restore applications.
		// We ALWAYS do it, because we must also reconfigure the new client with respect
		// to what we already deployed.
		this.templateManager.restoreTemplates( this.configurationDirectory );
		this.appManager.restoreApplications( this.configurationDirectory, this.templateManager );
		for( ManagedApplication ma : this.appManager.getManagedApplications())
			this.instanceManager.restoreInstanceStates( ma );

		this.logger.info( "The DM was successfully (re)configured." );
	}


	/**
	 * Finds an application by name.
	 * @param applicationName an application name (not null)
	 * @return the associated application, or null if it was not found
	 */
	public Application findApplicationByName( String applicationName ) {
		return this.appManager.findApplicationByName( applicationName );
	}


	/**
	 * Saves the configuration (instances).
	 * @param ma a non-null managed application
	 */
	public void saveConfiguration( ManagedApplication ma ) {
		ConfigurationUtils.saveInstances( ma, this.configurationDirectory );

		synchronized( this ) {
			if( this.monitoringManager != null ) {
				this.monitoringManager.updateApplication( ma.getApplication());
			}
		}
	}


	/**
	 * @return a non-null map (key = application name, value = managed application)
	 */
	public Map<String,ManagedApplication> getNameToManagedApplication() {
		return this.appManager.getNameToManagedApplication();
	}


	/**
	 * @return a non-null set of all the application templates
	 */
	public Set<ApplicationTemplate> getApplicationTemplates() {
		return this.templateManager.getAllTemplates();
	}


	/**
	 * @return the raw templates (never null)
	 */
	public Map<ApplicationTemplate,Boolean> getRawApplicationTemplates() {
		return this.templateManager.getRawTemplates();
	}


	/**
	 * @throws IOException if the configuration is invalid
	 */
	public void checkConfiguration() throws IOException {

		String msg = null;
		if( this.messagingClient == null )
			msg = "The DM was not started.";
		else if( ! this.messagingClient.hasValidClient())
			msg = "The DM's configuration is invalid. Please, review the messaging settings.";

		if( msg != null ) {
			this.logger.warning( msg );
			throw new IOException( msg );
		}
	}


	/**
	 * @return the messagingClient
	 */
	public ReconfigurableClientDm getMessagingClient() {
		return this.messagingClient;
	}


	/**
	 * Loads a new application template.
	 * @see ApplicationTemplateMngrDelegate#loadApplicationTemplate(File, File)
	 */
	public ApplicationTemplate loadApplicationTemplate( File applicationFilesDirectory )
	throws AlreadyExistingException, InvalidApplicationException, IOException {

		checkConfiguration();
		return this.templateManager.loadApplicationTemplate( applicationFilesDirectory, this.configurationDirectory );
	}


	/**
	 * @return the echo messages
	 */
	public List<MsgEcho> getEchoMessages() {
		return this.echoMessages;
	}


	/**
	 * Deletes an application template.
	 */
	public void deleteApplicationTemplate( String tplName, String tplQualifier )
	throws UnauthorizedActionException, InvalidApplicationException, IOException {

		checkConfiguration();
		ApplicationTemplate tpl = this.templateManager.findTemplate( tplName, tplQualifier );
		if( tpl == null )
			throw new InvalidApplicationException( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));

		if( this.appManager.isTemplateUsed( tpl ))
			throw new UnauthorizedActionException( tplName + " (" + tplQualifier + ") is still used by applications. It cannot be deleted." );
		else
			this.templateManager.deleteApplicationTemplate( tpl, this.configurationDirectory );
	}


	/**
	 * Creates a new application from a template.
	 * @return a managed application (never null)
	 * @throws IOException
	 * @throws AlreadyExistingException
	 * @throws InvalidApplicationException
	 */
	public ManagedApplication createApplication( String name, String description, String tplName, String tplQualifier )
	throws IOException, AlreadyExistingException, InvalidApplicationException {

		// Always verify the configuration first
		checkConfiguration();

		// Create the application
		ApplicationTemplate tpl = this.templateManager.findTemplate( tplName, tplQualifier );
		if( tpl == null )
			throw new InvalidApplicationException( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));

		return createApplication( name, description, tpl );
	}


	/**
	 * Creates a new application from a template.
	 * @return a managed application (never null)
	 * @throws IOException
	 * @throws AlreadyExistingException
	 */
	public ManagedApplication createApplication( String name, String description, ApplicationTemplate tpl )
	throws IOException, AlreadyExistingException {

		// Always verify the configuration first
		checkConfiguration();

		// Create the application
		ManagedApplication ma = this.appManager.createApplication( name, description, tpl, this.configurationDirectory );

		// Start listening to messages
		this.messagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.START );

		// Start the monitoring of the application.
		synchronized (this) {
			if (this.monitoringManager != null) {
				this.monitoringManager.addApplication( ma.getApplication());
			}
		}

		this.logger.fine( "Application " + ma.getApplication().getName() + " was successfully loaded and added." );
		return ma;
	}


	/**
	 * Deletes an application.
	 * @param ma the managed application
	 * @throws UnauthorizedActionException if parts of the application are still running
	 * @throws IOException if errors occurred with the messaging or the removal of resources
	 */
	public void deleteApplication( ManagedApplication ma )
	throws UnauthorizedActionException, IOException {

		// What really matters is that there is no agent running.
		// If all the root instances are not deployed, then nothing is deployed at all.
		String applicationName = ma.getApplication().getName();
		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			if( rootInstance.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( applicationName + " contains instances that are still deployed." );
		}

		// Stop listening to messages first
		try {
			checkConfiguration();
			this.messagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.STOP );
			this.messagingClient.deleteMessagingServerArtifacts( ma.getApplication());

		} catch( IOException e ) {
			Utils.logException( this.logger, e );
		}

		// Stop the monitoring of the outgoing application.
		synchronized (this) {
			if (this.monitoringManager != null) {
				this.monitoringManager.removeApplication( ma.getApplication());
			}
		}

		// Delete artifacts
		this.appManager.deleteApplication( ma.getApplication(), this.configurationDirectory );
	}


	/**
	 * Adds an instance.
	 * @see InstanceMngrDelegate#addInstance(ManagedApplication, Instance, Instance)
	 */
	public void addInstance( ManagedApplication ma, Instance parentInstance, Instance instance )
	throws ImpossibleInsertionException, IOException {

		checkConfiguration();
		this.instanceManager.addInstance( ma, parentInstance, instance );
		saveConfiguration( ma );
	}


	/**
	 * Removes an instance.
	 * @see InstanceMngrDelegate#removeInstance(ManagedApplication, Instance)
	 */
	public void removeInstance( ManagedApplication ma, Instance instance )
	throws UnauthorizedActionException, IOException {

		checkConfiguration();
		this.instanceManager.removeInstance( ma, instance );
		saveConfiguration( ma );
	}


	/**
	 * Notifies all the agents they must re-export their variables.
	 * <p>
	 * Such an operation can be used when the messaging server was down and
	 * that messages were lost.
	 * </p>
	 * @throws IOException
	 */
	public void resynchronizeAgents( ManagedApplication ma ) throws IOException {

		checkConfiguration();
		this.logger.fine( "Resynchronizing agents in " + ma.getName() + "..." );
		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			if( rootInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED )
				send ( ma, new MsgCmdResynchronize(), rootInstance );
		}

		this.logger.fine( "Requests were sent to resynchronize agents in " + ma.getName() + "." );
	}


	/**
	 * Changes the state of an instance.
	 * @see InstanceMngrDelegate#changeInstanceState(ManagedApplication, Instance, InstanceStatus)
	 */
	public void changeInstanceState( ManagedApplication ma, Instance instance, InstanceStatus newStatus )
	throws IOException, TargetException {

		checkConfiguration();
		this.instanceManager.changeInstanceState( ma, instance, newStatus );
	}


	/**
	 * Deploys and starts all the instances of an application.
	 * @see InstanceMngrDelegate#deployAndStartAll(ManagedApplication, Instance)
	 */
	public void deployAndStartAll( ManagedApplication ma, Instance instance ) throws IOException {

		checkConfiguration();
		this.instanceManager.deployAndStartAll( ma, instance );
	}


	/**
	 * Stops all the started instances of an application.
	 * @see InstanceMngrDelegate#stopAll(ManagedApplication, Instance)
	 */
	public void stopAll( ManagedApplication ma, Instance instance ) throws IOException {

		checkConfiguration();
		this.instanceManager.stopAll( ma, instance );
	}


	/**
	 * Undeploys all the instances of an application.
	 * @see InstanceMngrDelegate#undeployAll(ManagedApplication, Instance)
	 */
	public void undeployAll( ManagedApplication ma, Instance instance ) throws IOException {

		checkConfiguration();
		this.instanceManager.undeployAll( ma, instance );
	}


	/**
	 * Sends a message, or stores it if the targetHandlers machine is not yet online.
	 * @param ma the managed application
	 * @param message the message to send (or store)
	 * @param instance the targetHandlers instance
	 * @throws IOException if an error occurred with the messaging
	 */
	public void send( ManagedApplication ma, Message message, Instance instance ) throws IOException {

		if( this.messagingClient != null
				&& this.messagingClient.isConnected()) {

			// We do NOT send directly a message!
			ma.storeAwaitingMessage( instance, message );

			// If the message has been stored, let's try to send all the stored messages.
			// This preserves message ordering (FIFO).

			// If the VM is online, process awaiting messages to prevent waiting.
			// This can work concurrently with the messages timer.
			Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );
			if( scopedInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {

				List<Message> messages = ma.removeAwaitingMessages( instance );
				String path = InstanceHelpers.computeInstancePath( scopedInstance );
				this.logger.fine( "Forcing the sending of " + messages.size() + " awaiting message(s) for " + path + "." );

				for( Message msg : messages ) {
					try {
						this.messagingClient.sendMessageToAgent( ma.getApplication(), scopedInstance, msg );

					} catch( IOException e ) {
						this.logger.severe( "Error while sending a stored message. " + e.getMessage());
						Utils.logException( this.logger, e );
					}
				}
			}

		} else {
			this.logger.severe( "The connection with the messaging server was badly initialized. Message dropped." );
		}
	}


	/**
	 * @param targetResolver the target resolver to set
	 */
	public void setTargetResolver( ITargetResolver targetResolver ) {
		this.instanceManager.setTargetResolver( targetResolver );
	}


	// TODO: move the ping methods in its own delegate


	/**
	 * Pings the DM through the messaging queue.
	 *
	 * @param message the content of the Echo message to send.
	 * @param timeout the timeout in milliseconds (ms) to wait before considering the Echo message is lost.
	 * @return {@code true} if the sent Echo message has been received before the {@code timeout}, {@code false}
	 * otherwise.
	 * @throws java.lang.InterruptedException if interrupted while waiting for the Echo message.
	 * @throws java.io.IOException if something bad happened.
	 */
	public boolean pingMessageQueue( String message, long timeout )
	throws InterruptedException, IOException {

		// Step 1. Send the Echo message.
		final long deadline = System.currentTimeMillis() + timeout;
		final MsgEcho sentMessage = new MsgEcho( message, deadline );
		this.messagingClient.sendMessageToTheDm( sentMessage );
		this.logger.fine( "Sent Echo message on debug queue. Message=" + message + ", timeout=" + timeout + "ms, UUID=" + sentMessage.getUuid());

		// Step 2. Wait for the Echo message to be received.
		return waitForEchoMessage( sentMessage.getUuid(), deadline ) != null;
	}


	/**
	 * Pings an agent.
	 *
	 * @param app the application
	 * @param rootInstance the root instance name
	 * @param message the echo messages's content
	 * @param timeout the timeout in milliseconds (ms) to wait before considering the Echo message is lost
	 * @return {@code true} if the sent Echo message has been received before the {@code timeout}, {@code false} otherwise
	 * @throws java.lang.InterruptedException if interrupted while waiting for the ping
	 * @throws java.io.IOException if something bad happened
	 */
	public boolean pingAgent( Application app, Instance rootInstance, String message, long timeout )
	throws InterruptedException, IOException {

		// Step 1. Send the PING request message.
		final long deadline = System.currentTimeMillis() + timeout;
		MsgEcho ping = new MsgEcho( "PING:" + message, deadline );
		this.messagingClient.sendMessageToAgent( app, rootInstance, ping );
		this.logger.fine( "Sent PING request message=" + message + "timeout=" + timeout + "ms to application="
				+ app + ", agent=" + rootInstance.getName());

		// Step 2. Wait for the PONG response from the agent.
		return waitForEchoMessage( ping.getUuid(), deadline ) != null;
	}


	/**
	 * Notifies the DM that an Echo message has been received.
	 * @param message the received message.
	 */
	public void notifyMsgEchoReceived( MsgEcho message ) {
		synchronized ( this.echoMessages ) {
			this.echoMessages.add( message );
			this.echoMessages.notifyAll();
		}
	}


	/**
	 * Waits for an Echo message with the specified UUID to be received.
	 * <p>
	 * This method also removes expired echo messages (i.e whose expiration time is lower than
	 * {@code System.currentTimeMillis()}. This cleaning has low priority, and is interrupted as soon as the expected
	 * message is received, or the given deadline is passed.
	 * </p>
	 *
	 * @param uuid the UUID of the expected Echo message to wait for
	 * @param deadline the expiration time, after which this method returns {@code null}
	 * @return the received message, if it has the expected UUID <em>and</em> has been received <em>before</em> the
	 * expiration of the given deadline, {@code null} otherwise
	 * @throws java.lang.InterruptedException if interrupted while waiting for the expected message
	 */
	private MsgEcho waitForEchoMessage( final UUID uuid, final long deadline )
	throws InterruptedException {

		MsgEcho foundMessage = null;
		while( true ) {
			synchronized ( this.echoMessages ) {
				// Check all the Echo messages to find ours.
				for (Iterator<MsgEcho> i = this.echoMessages.iterator(); i.hasNext(); ) {
					final MsgEcho m = i.next();
					final long now = System.currentTimeMillis();
					if (now > deadline) {
						// Too late!
						break;
					} else if ( m.getExpirationTime() <= now ) {
						// Message has expired => remove it from the list.
						i.remove();
					} else if (uuid.equals( m.getUuid())) {
						// This is the message we are waiting for!
						foundMessage = m;
						i.remove();
						break;
					}
				}

				final long timeout = deadline - System.currentTimeMillis();
				if ( foundMessage != null || timeout <= 0 ) {
					// Message found, or deadline reached => exit the loop
					break;
				}
				// Wait for an Echo message notification!
				this.echoMessages.wait( timeout );
			}
		}

		return foundMessage;
	}

	public Map<String, String> getMessagingConfiguration() {
		return this.messagingClient.getConfiguration();
	}

}
