/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.io.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.environment.messaging.RCDm;
import net.roboconf.dm.internal.environment.target.TargetResolver;
import net.roboconf.dm.internal.management.CheckerHeartbeatsTask;
import net.roboconf.dm.internal.management.CheckerMessagesTask;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;
import net.roboconf.messaging.reconfigurables.ReconfigurableClientDm;
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
 * manager.setMessageServerIp( "localhost" );
 * manager.setMessageServerUsername( "guest" );
 * manager.setMessageServerPassword( "guest" );
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
 */
public class Manager {

	// Constants
	private static final long TIMER_PERIOD = 6000;

	// Injected by iPojo or Admin Config
	private final List<TargetHandler> targetHandlers = new ArrayList<TargetHandler> ();
	private String messageServerIp, messageServerUsername, messageServerPassword, configurationDirectoryLocation;

	// Internal fields
	private final Map<String,ManagedApplication> appNameToManagedApplication = new ConcurrentHashMap<String,ManagedApplication> ();
	private final Logger logger = Logger.getLogger( getClass().getName());
	private String messagingFactoryType;
	private RCDm messagingClient;

	File configurationDirectory;
	ITargetResolver targetResolver;
	Timer timer;


	/**
	 * Constructor.
	 */
	public Manager() {
		this.messagingFactoryType = MessagingConstants.FACTORY_RABBIT_MQ;
		this.targetResolver = new TargetResolver();
	}


	/**
	 * Starts the manager.
	 * <p>
	 * It is invoked by iPojo when an instance becomes VALID.
	 * </p>
	 */
	public void start() {

		this.logger.info( "The DM is about to be launched." );

		this.messagingClient = new RCDm( this );
		DmMessageProcessor messageProcessor = new DmMessageProcessor( this );
		this.messagingClient.associateMessageProcessor( messageProcessor );

		this.timer = new Timer( "Roboconf's Management Timer", true );
		this.timer.scheduleAtFixedRate( new CheckerMessagesTask( this, this.messagingClient ), 0, TIMER_PERIOD );
		this.timer.scheduleAtFixedRate( new CheckerHeartbeatsTask( this ), 0, Constants.HEARTBEAT_PERIOD );

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
			this.messagingClient.getMessageProcessor().stopProcessor();
			this.messagingClient.getMessageProcessor().interrupt();
			try {
				this.messagingClient.closeConnection();

			} catch( IOException e ) {
				this.logger.warning( "The messaging client could not be terminated correctly. " + e.getMessage());
				this.logger.finest( Utils.writeException( e ));
			}
		}

		for( ManagedApplication ma : this.appNameToManagedApplication.values())
			saveConfiguration( ma );

		this.logger.info( "The DM was stopped." );
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
		if( this.timer != null )
			this.timer.cancel();

		for( ManagedApplication ma : this.appNameToManagedApplication.values())
			saveConfiguration( ma );

		// Update the configuration directory
		File defaultConfigurationDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf-dm" );
		if( Utils.isEmptyOrWhitespaces( this.configurationDirectoryLocation )) {
			this.logger.warning( "Invalid location for the configuration directory (empty or null). Switching to " + defaultConfigurationDirectory );
			this.configurationDirectory = defaultConfigurationDirectory;

		} else {
			this.configurationDirectory = new File( this.configurationDirectoryLocation );
			if( ! this.configurationDirectory.isDirectory()
					&& ! this.configurationDirectory.mkdirs()) {

				this.logger.warning( "Could not create " + this.configurationDirectory + ". Switching to " + defaultConfigurationDirectory );
				this.configurationDirectory = defaultConfigurationDirectory;
			}
		}

		// Update the messaging client
		if( this.messagingClient != null )
			this.messagingClient.switchMessagingClient( this.messageServerIp, this.messageServerUsername, this.messageServerPassword, this.messagingFactoryType );

		// Reset and restore applications.
		// We ALWAYS do it, because we must also reconfigure the new client with respect
		// to what we already deployed.
		// FIXME: should we backup the current configuration first?
		restoreApplications();

		this.logger.info( "The DM was successfully (re)configured." );
	}


	/**
	 * Saves the configuration (instances).
	 * @param ma a non-null managed application
	 */
	public void saveConfiguration( ManagedApplication ma ) {
		ConfigurationUtils.saveInstances( ma, this.configurationDirectory );
	}


	/**
	 * This method is invoked by iPojo every time a new target handler appears.
	 * @param targetHandlers
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
	 * @param targetHandlers
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
	 * @param targetHandlers
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
	 * @param targetResolver the targetResolver to set
	 */
	public void setTargetResolver( ITargetResolver targetResolver ) {
		this.targetResolver = targetResolver;
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
	 * @param configurationDirectoryLocation the configurationDirectoryLocation to set
	 */
	public void setConfigurationDirectoryLocation( String configurationDirectoryLocation ) {
		this.configurationDirectoryLocation = configurationDirectoryLocation;
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
	 * @return a non-null map (key = application name, value = managed application)
	 */
	public Map<String,ManagedApplication> getAppNameToManagedApplication() {
		return this.appNameToManagedApplication;
	}


	/**
	 * @return the messagingFactoryType
	 */
	public String getMessagingFactoryType() {
		return this.messagingFactoryType;
	}


	/**
	 * @param messagingFactoryType the messagingFactoryType to set
	 */
	public void setMessagingFactoryType( String messagingFactoryType ) {
		this.messagingFactoryType = messagingFactoryType;
	}


	/**
	 * @return the targetHandlers
	 */
	public List<TargetHandler> getTargetHandlers() {
		return Collections.unmodifiableList( this.targetHandlers );
	}


	/**
	 * Finds an application by name.
	 * @param applicationName the application name (not null)
	 * @return an application, or null if it was not found
	 */
	public Application findApplicationByName( String applicationName ) {
		ManagedApplication ma = this.appNameToManagedApplication.get( applicationName );
		return ma != null ? ma.getApplication() : null;
	}


	/**
	 * Loads a new application.
	 * <p>
	 * An application cannot be loaded before the messaging client connection was established.
	 * </p>
	 *
	 * @param applicationFilesDirectory the application's directory
	 * @return the managed application that was created
	 * @throws AlreadyExistingException if the application already exists
	 * @throws InvalidApplicationException if the application contains critical errors
	 * @throws IOException if the messaging could not be initialized
	 */
	public ManagedApplication loadNewApplication( File applicationFilesDirectory )
	throws AlreadyExistingException, InvalidApplicationException, IOException {

		this.logger.info( "Loading application from " + applicationFilesDirectory + "..." );
		checkConfiguration();

		ApplicationLoadResult lr = RuntimeModelIo.loadApplication( applicationFilesDirectory );
		checkErrors( lr.getLoadErrors());

		Application application = lr.getApplication();
		if( null != findApplicationByName( application.getName()))
			throw new AlreadyExistingException( application.getName());

		File targetDirectory = ConfigurationUtils.findApplicationdirectory( application.getName(), this.configurationDirectory );
		if( ! applicationFilesDirectory.equals( targetDirectory )) {
			if( Utils.isAncestorFile( targetDirectory, applicationFilesDirectory ))
				throw new IOException( "Cannot move " + applicationFilesDirectory + " in Roboconf's work directory. Already a child directory." );
			else
				Utils.copyDirectory( applicationFilesDirectory, targetDirectory );
		}

		ManagedApplication ma = new ManagedApplication( application, targetDirectory );
		this.messagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.START );
		this.appNameToManagedApplication.put( ma.getApplication().getName(), ma );
		this.logger.fine( "Application " + ma.getApplication().getName() + " was successfully loaded and added." );

		return ma;
	}


	/**
	 * Deletes an application.
	 * @param ma the managed application
	 * @throws UnauthorizedActionException if parts of the application are still running
	 * @throws IOException
	 */
	public void deleteApplication( ManagedApplication ma ) throws UnauthorizedActionException, IOException {
		checkConfiguration();

		// Check we can do this
		String applicationName = ma.getApplication().getName();
		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			if( rootInstance.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( applicationName + " contains instances that are still deployed." );
		}

		// If yes, do it
		this.logger.fine( "Deleting application " + applicationName + "..." );
		this.messagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.STOP );
		Utils.deleteFilesRecursively( ma.getApplicationFilesDirectory());
		ConfigurationUtils.deleteInstancesFile( ma.getName(), this.configurationDirectory );

		this.messagingClient.deleteMessagingServerArtifacts( ma.getApplication());
		this.appNameToManagedApplication.remove( applicationName );
		this.logger.fine( "Application " + applicationName + " was successfully deleted." );
	}


	/**
	 * Adds an instance.
	 * @param ma the managed application
	 * @param parentInstance the parent instance
	 * @param instance the instance to insert (not null)
	 * @throws ImpossibleInsertionException if the instance could not be added
	 */
	public void addInstance( ManagedApplication ma, Instance parentInstance, Instance instance )
	throws ImpossibleInsertionException, IOException {

		checkConfiguration();
		if( ! InstanceHelpers.tryToInsertChildInstance( ma.getApplication(), parentInstance, instance ))
			throw new ImpossibleInsertionException( instance.getName());

		this.logger.fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully added in " + ma.getName() + "." );
		Instance rootInstance = InstanceHelpers.findRootInstance( instance );

		// Store the message because we want to make sure the message is not lost
		ma.storeAwaitingMessage( instance, new MsgCmdSetRootInstance( rootInstance ));
		saveConfiguration( ma );
	}


	/**
	 * Removes an instance.
	 * @param ma the managed application
	 * @param instance the instance to remove (not null)
	 * @throws UnauthorizedActionException if we try to remove an instance that seems to be running
	 * @throws IOException if an error occurred with the messaging
	 */
	public void removeInstance( ManagedApplication ma, Instance instance ) throws UnauthorizedActionException, IOException {

		checkConfiguration();
		for( Instance i : InstanceHelpers.buildHierarchicalList( instance )) {
			if( i.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( "Instances are still deployed or running. They cannot be removed in " + ma.getName() + "." );
		}

		// Whatever is the state of the agent, we try to send a message.
		MsgCmdRemoveInstance message = new MsgCmdRemoveInstance( instance );
		send( ma, message, instance );

		if( instance.getParent() == null )
			ma.getApplication().getRootInstances().remove( instance );
		else
			instance.getParent().getChildren().remove( instance );

		this.logger.fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully removed in " + ma.getName() + "." );
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

		this.logger.fine( "Resynchronizing agents in " + ma.getName() + "..." );
		checkConfiguration();
		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			if( rootInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED )
				send ( ma, new MsgCmdResynchronize(), rootInstance );
		}
	}


	/**
	 * Changes the state of an instance.
	 * @param ma the managed application
	 * @param instance the instance whose state must be updated
	 * @param newStatus the new status
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with a target
	 */
	public void changeInstanceState( ManagedApplication ma, Instance instance, InstanceStatus newStatus )
	throws IOException, TargetException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		this.logger.fine( "Changing state of " + instancePath + " to " + newStatus + " in " + ma.getName() + "..." );
		checkConfiguration();

		if( instance.getParent() == null ) {
			if( newStatus == InstanceStatus.NOT_DEPLOYED
					&& ( instance.getStatus() == InstanceStatus.DEPLOYED_STARTED
						|| instance.getStatus() == InstanceStatus.DEPLOYING
						|| instance.getStatus() == InstanceStatus.STARTING ))
				undeployRoot( ma, instance );

			else if( instance.getStatus() == InstanceStatus.NOT_DEPLOYED
					&& newStatus == InstanceStatus.DEPLOYED_STARTED )
				deployRoot( ma, instance );

			else
				this.logger.warning( "Ignoring a request to update a root instance's state." );

		} else {
			Map<String,byte[]> instanceResources = null;
			if( newStatus == InstanceStatus.DEPLOYED_STARTED
					|| newStatus == InstanceStatus.DEPLOYED_STOPPED )
				instanceResources = ResourceUtils.storeInstanceResources( ma.getApplicationFilesDirectory(), instance );

			MsgCmdChangeInstanceState message = new MsgCmdChangeInstanceState( instance, newStatus, instanceResources );
			send( ma, message, instance );
			this.logger.fine( "A message was (or will be) sent to the agent to change the state of " + instancePath + " in " + ma.getName() + "." );
		}
	}


	/**
	 * Deploys and starts all the instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we deploy and start (can be null)
	 * <p>
	 * This instance and all its children will be deployed and started.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws TargetException if a problem occurred with the target handler
	 * @throws IOException if a problem occurred with the messaging
	 */
	public void deployAndStartAll( ManagedApplication ma, Instance instance ) throws TargetException, IOException {

		checkConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		for( Instance initialInstance : initialInstances ) {
			for( Instance i : InstanceHelpers.buildHierarchicalList( initialInstance )) {
				if( i.getParent() == null )
					deployRoot( ma, i );
				else
					changeInstanceState( ma, i, InstanceStatus.DEPLOYED_STARTED );
			}
		}
	}


	/**
	 * Stops all the started instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we deploy and start (can be null)
	 * <p>
	 * This instance and all its children will be deployed and started.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws IOException if a problem occurred with the messaging
	 * @throws TargetException if a problem occurred with a target handler
	 */
	public void stopAll( ManagedApplication ma, Instance instance ) throws IOException, TargetException {

		checkConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to stop all the instances, just the first children
		for( Instance initialInstance : initialInstances ) {
			if( initialInstance.getParent() != null )
				changeInstanceState( ma, initialInstance, InstanceStatus.DEPLOYED_STOPPED );
			else for( Instance i : initialInstance.getChildren())
				changeInstanceState( ma, i, InstanceStatus.DEPLOYED_STOPPED );
		}
	}


	/**
	 * Undeploys all the instances of an application.
	 * @param ma an application
	 * @param instance the instance from which we deploy and start (can be null)
	 * <p>
	 * This instance and all its children will be deployed and started.
	 * If null, then all the application instances are considered.
	 * </p>
	 *
	 * @throws IOException if a problem occurred with the messaging
	 * @throws TargetException if a problem occurred with the target handler
	 */
	public void undeployAll( ManagedApplication ma, Instance instance ) throws IOException, TargetException {

		checkConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to undeploy all the instances, just the first instance
		for( Instance initialInstance : initialInstances ) {
			if( initialInstance.getParent() != null )
				changeInstanceState( ma, initialInstance, InstanceStatus.NOT_DEPLOYED );
			else
				undeployRoot( ma, initialInstance );
		}
	}


	/**
	 * Sends a message, or stores it if the targetHandlers machine is not yet online.
	 * @param ma the managed application
	 * @param message the message to send (or store)
	 * @param instance the targetHandlers instance
	 * @throws IOException if an error occurred with the messaging
	 */
	void send( ManagedApplication ma, Message message, Instance instance ) throws IOException {

		// We do NOT send directly a message!
		// We either ignore or store it.
		if( this.messagingClient == null
				|| ! this.messagingClient.isConnected())
			this.logger.severe( "The connection with the messaging server was badly initialized. Message dropped." );
		else
			ma.storeAwaitingMessage( instance, message );

		// If the message has been stored, let's try to send all the stored messages.
		// This preserve message ordering (FIFO).

		// If the VM is online, process awaiting messages to prevent waiting.
		// This can work concurrently with the messages timer.
		Instance rootInstance = InstanceHelpers.findRootInstance( instance );
		if( rootInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
			List<Message> messages = ma.removeAwaitingMessages( instance );
			if( ! messages.isEmpty())
				this.logger.fine( "Forcing the sending of " + messages.size() + " awaiting message(s) for " + rootInstance.getName() + "." );

			for( Message msg : messages ) {
				try {
					this.messagingClient.sendMessageToAgent( ma.getApplication(), rootInstance, msg );

				} catch( IOException e ) {
					this.logger.severe( "Error while sending a stored message. " + e.getMessage());
					this.logger.finest( Utils.writeException( e ));
				}
			}
		}
	}


	/**
	 * Check errors.
	 * @param errors a list of errors and warnings (not null but may be empty)
	 * @throws InvalidApplicationException if the list contains criticial errors
	 */
	void checkErrors( Collection<RoboconfError> errors ) throws InvalidApplicationException {

		if( RoboconfErrorHelpers.containsCriticalErrors( errors ))
			throw new InvalidApplicationException( errors );

		for( RoboconfError warning : RoboconfErrorHelpers.findWarnings( errors )) {
			StringBuilder sb = new StringBuilder();
			sb.append( warning.getErrorCode().getMsg());
			if( ! Utils.isEmptyOrWhitespaces( warning.getDetails()))
				sb.append( " " + warning.getDetails());

			this.logger.warning( sb.toString());
		}
	}


	/**
	 * Deploys a root instance.
	 * @param ma the managed application
	 * @param rootInstance the instance to deploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with the target handler
	 */
	void deployRoot( ManagedApplication ma, Instance rootInstance ) throws TargetException, IOException {

		this.logger.fine( "Deploying root instance " + rootInstance.getName() + " in " + ma.getName() + "..." );
		if( rootInstance.getParent() != null ) {
			this.logger.fine( "Deploy action for instance " + rootInstance.getName() + " is cancelled in " + ma.getName() + ". Not a root instance." );
			return;
		}

		// If the VM creation was already requested, then its machine ID has already been set.
		// It does not mean the VM is already created, it may take some time.
		String machineId = rootInstance.getData().get( Instance.MACHINE_ID );
		if( machineId != null ) {
			this.logger.fine( "Deploy action for instance " + rootInstance.getName() + " is cancelled in " + ma.getName() + ". Already associated with a machine." );
			return;
		}

		checkConfiguration();
		InstanceStatus initialStatus = rootInstance.getStatus();
		try {
			rootInstance.setStatus( InstanceStatus.DEPLOYING );
			MsgCmdSetRootInstance msg = new MsgCmdSetRootInstance( rootInstance );
			send( ma, msg, rootInstance );

			TargetHandler targetHandler = this.targetResolver.findTargetHandler( this.targetHandlers, ma, rootInstance );
			machineId = targetHandler.createOrConfigureMachine(
					this.messageServerIp, this.messageServerUsername, this.messageServerPassword,
					rootInstance.getName(),
					ma.getApplication().getName());

			rootInstance.getData().put( Instance.MACHINE_ID, machineId );
			this.logger.fine( "Root instance " + rootInstance.getName() + "'s deployment was successfully requested in " + ma.getName() + ". Machine ID: " + machineId );

		} catch( Exception e ) {
			this.logger.severe( "Failed to deploy root instance " + rootInstance.getName() + " in " + ma.getName() + ". " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));

			rootInstance.setStatus( initialStatus );
			if( e instanceof TargetException)
				throw (TargetException) e;
			else if( e instanceof IOException )
				throw (IOException) e;

		} finally {
			saveConfiguration( ma );
		}
	}


	/**
	 * Undeploys a root instance.
	 * @param ma the managed application
	 * @param rootInstance the instance to undeploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with the target handler
	 */
	void undeployRoot( ManagedApplication ma, Instance rootInstance ) throws TargetException, IOException {

		this.logger.fine( "Undeploying root instance " + rootInstance.getName() + " in " + ma.getName() + "..." );
		if( rootInstance.getParent() != null ) {
			this.logger.fine( "Undeploy action for instance " + rootInstance.getName() + " is cancelled in " + ma.getName() + ". Not a root instance." );
			return;
		}

		checkConfiguration();
		InstanceStatus initialStatus = rootInstance.getStatus();
		try {
			// Terminate the machine
			this.logger.fine( "Machine " + rootInstance.getName() + " is about to be deleted in " + ma.getName() + "." );
			TargetHandler targetHandler = this.targetResolver.findTargetHandler( this.targetHandlers, ma, rootInstance );
			String machineId = rootInstance.getData().remove( Instance.MACHINE_ID );
			if( machineId != null )
				targetHandler.terminateMachine( machineId );

			this.logger.fine( "Machine " + rootInstance.getName() + " was successfully deleted in " + ma.getName() + "." );
			for( Instance i : InstanceHelpers.buildHierarchicalList( rootInstance )) {
				i.setStatus( InstanceStatus.NOT_DEPLOYED );
				// DM won't send old imports upon restart...
				i.getImports().clear();
			}

			// Remove useless data for the configuration backup
			rootInstance.getData().clear();
			this.logger.fine( "Root instance " + rootInstance.getName() + "'s undeployment was successfully requested in " + ma.getName() + "." );

		} catch( Exception e ) {
			this.logger.severe( "Failed to undeploy root instance " + rootInstance.getName() + " in " + ma.getName() + ". " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));

			rootInstance.setStatus( initialStatus );
			if( e instanceof TargetException)
				throw (TargetException) e;
			else if( e instanceof IOException )
				throw (IOException) e;

		} finally {
			saveConfiguration( ma );
		}
	}


	/**
	 * Restores the applications.
	 */
	void restoreApplications() {

		this.appNameToManagedApplication.clear();
		for( File dir : ConfigurationUtils.findApplicationDirectories( this.configurationDirectory )) {
			try {
				loadNewApplication( dir );

			} catch( AlreadyExistingException e ) {
				this.logger.severe( "Cannot restore application in " + dir + " (already existing)." );
				this.logger.finest( Utils.writeException( e ));

			} catch( InvalidApplicationException e ) {
				this.logger.severe( "Cannot restore application in " + dir + " (invalid application)." );
				this.logger.finest( Utils.writeException( e ));

			} catch( IOException e ) {
				this.logger.severe( "Cannot restore application in " + dir + " (I/O exception)." );
				this.logger.finest( Utils.writeException( e ));
			}
		}

		// Restore instances
		for( ManagedApplication ma : this.appNameToManagedApplication.values()) {

			// Instance definition
			InstancesLoadResult ilr = ConfigurationUtils.restoreInstances( ma, this.configurationDirectory );
			try {
				checkErrors( ilr.getLoadErrors());
				if( ilr.getRootInstances().isEmpty())
					continue;

				ma.getApplication().getRootInstances().clear();
				ma.getApplication().getRootInstances().addAll( ilr.getRootInstances());

			} catch( InvalidApplicationException e ) {
				this.logger.severe( "Cannot restore instances for application " + ma.getName() + " (errors were found)." );
				this.logger.finest( Utils.writeException( e ));
			}

			// States
			for( Instance rootInstance : ma.getApplication().getRootInstances()) {
				try {
					this.messagingClient.sendMessageToAgent( ma.getApplication(), rootInstance, new MsgCmdSendInstances());

				} catch( IOException e ) {
					this.logger.severe( "Could not request states for agent " + rootInstance.getName() + " (I/O exception)." );
					this.logger.finest( Utils.writeException( e ));
				}
			}
		}
	}
}
