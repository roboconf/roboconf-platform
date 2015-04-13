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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.environment.messaging.RCDm;
import net.roboconf.dm.internal.environment.target.TargetHelpers;
import net.roboconf.dm.internal.environment.target.TargetResolver;
import net.roboconf.dm.internal.management.CheckerHeartbeatsTask;
import net.roboconf.dm.internal.management.CheckerMessagesTask;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ITargetResolver.Target;
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
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.messages.from_dm_to_dm.MsgEcho;
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
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class Manager {

	// Constants
	private static final long TIMER_PERIOD = 6000;
	private static final Object LOCK = new Object();

	// Injected by iPojo or Admin Config
	private final List<TargetHandler> targetHandlers = new ArrayList<TargetHandler> ();
	private String messageServerIp, messageServerUsername, messageServerPassword, configurationDirectoryLocation;

	// Internal fields
	private final Map<String,ManagedApplication> appNameToManagedApplication = new ConcurrentHashMap<String,ManagedApplication> ();
	private final Logger logger = Logger.getLogger( getClass().getName());
	private String messagingFactoryType;
	private RCDm messagingClient;

	final List<MsgEcho> echoMessages = new ArrayList<MsgEcho>();
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

		DmMessageProcessor messageProcessor = new DmMessageProcessor( this );
		this.messagingClient = new RCDm( this );
		this.messagingClient.associateMessageProcessor( messageProcessor );

		this.timer = new Timer( "Roboconf's Management Timer", false );
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
		for( ManagedApplication ma : this.appNameToManagedApplication.values())
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

		// Update the messaging client
		if( this.messagingClient != null ) {
			this.messagingClient.switchMessagingClient( this.messageServerIp, this.messageServerUsername, this.messageServerPassword, this.messagingFactoryType );
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
	 * @return the echoMessages
	 */
	public List<MsgEcho> getEchoMessages() {
		return this.echoMessages;
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
		TargetHelpers.verifyTargets( this.targetResolver, ma, this.targetHandlers );

		this.appNameToManagedApplication.put( ma.getApplication().getName(), ma );
		this.messagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.START );
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

		// What really matters is that there is no agent running.
		// If all the root instances are not deployed, then nothing is deployed at all.
		String applicationName = ma.getApplication().getName();
		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			if( rootInstance.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( applicationName + " contains instances that are still deployed." );
		}

		this.logger.fine( "Deleting application " + applicationName + "..." );

		// Deal with the messaging stuff
		try {
			checkConfiguration();
			this.messagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.STOP );
			this.messagingClient.deleteMessagingServerArtifacts( ma.getApplication());

		} catch( Exception e ) {
			Utils.logException( this.logger, e );
		}

		// Delete the files
		Utils.deleteFilesRecursively( ma.getApplicationFilesDirectory());
		ConfigurationUtils.deleteInstancesFile( ma.getName(), this.configurationDirectory );

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
		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );

		// Store the message because we want to make sure the message is not lost
		ma.storeAwaitingMessage( instance, new MsgCmdSetScopedInstance( scopedInstance ));
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
		this.logger.fine( "Trying to change the state of " + instancePath + " to " + newStatus + " in " + ma.getName() + "..." );
		checkConfiguration();

		if( InstanceHelpers.isTarget( instance )) {
			if( newStatus == InstanceStatus.NOT_DEPLOYED
					&& ( instance.getStatus() == InstanceStatus.DEPLOYED_STARTED
						|| instance.getStatus() == InstanceStatus.DEPLOYING
						|| instance.getStatus() == InstanceStatus.STARTING ))
				undeployTarget( ma, instance );

			else if( instance.getStatus() == InstanceStatus.NOT_DEPLOYED
					&& newStatus == InstanceStatus.DEPLOYED_STARTED )
				deployTarget( ma, instance );

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
	 * @throws IOException if a problem occurred with the messaging
	 */
	public void deployAndStartAll( ManagedApplication ma, Instance instance ) throws IOException {

		checkConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		boolean gotExceptions = false;
		for( Instance initialInstance : initialInstances ) {
			for( Instance i : InstanceHelpers.buildHierarchicalList( initialInstance )) {
				try {
					changeInstanceState( ma, i, InstanceStatus.DEPLOYED_STARTED );

				} catch( Exception e ) {
					gotExceptions = true;
				}
			}
		}

		if( gotExceptions ) {
			this.logger.info( "One or several errors occurred while deploying and starting instances." );
			throw new IOException( "One or several errors occurred while deploying and starting instances." );
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
	 */
	public void stopAll( ManagedApplication ma, Instance instance ) throws IOException {

		checkConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to stop all the instances, just the first children
		boolean gotExceptions = false;
		for( Instance initialInstance : initialInstances ) {
			try {
				if( initialInstance.getParent() != null )
					changeInstanceState( ma, initialInstance, InstanceStatus.DEPLOYED_STOPPED );
				else for( Instance i : initialInstance.getChildren())
					changeInstanceState( ma, i, InstanceStatus.DEPLOYED_STOPPED );

			} catch( Exception e ) {
				gotExceptions = true;
			}
		}

		if( gotExceptions ) {
			this.logger.info( "One or several errors occurred while stopping instances." );
			throw new IOException( "One or several errors occurred while stopping instances." );
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
	 */
	public void undeployAll( ManagedApplication ma, Instance instance ) throws IOException {

		checkConfiguration();
		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to undeploy all the instances, just the first instance
		boolean gotExceptions = false;
		for( Instance initialInstance : initialInstances ) {
			try {
				if( initialInstance.getParent() != null )
					changeInstanceState( ma, initialInstance, InstanceStatus.NOT_DEPLOYED );
				else
					undeployTarget( ma, initialInstance );

			} catch( Exception e ) {
				gotExceptions = true;
			}
		}

		if( gotExceptions ) {
			this.logger.info( "One or several errors occurred while undeploying instances." );
			throw new IOException( "One or several errors occurred while undeploying instances." );
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
		// This preserves message ordering (FIFO).

		// If the VM is online, process awaiting messages to prevent waiting.
		// This can work concurrently with the messages timer.
		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );
		if( scopedInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
			List<Message> messages = ma.removeAwaitingMessages( instance );
			if( ! messages.isEmpty())
				this.logger.fine( "Forcing the sending of " + messages.size() + " awaiting message(s) for " + InstanceHelpers.computeInstancePath( scopedInstance ) + "." );

			for( Message msg : messages ) {
				try {
					this.messagingClient.sendMessageToAgent( ma.getApplication(), scopedInstance, msg );

				} catch( IOException e ) {
					this.logger.severe( "Error while sending a stored message. " + e.getMessage());
					Utils.logException( this.logger, e );
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
	 * Deploys a scoped instance.
	 * @param ma the managed application
	 * @param scopedInstance the instance to deploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with the target handler
	 */
	void deployTarget( ManagedApplication ma, Instance scopedInstance ) throws TargetException, IOException {

		// It only makes sense for scoped instances.
		String path = InstanceHelpers.computeInstancePath( scopedInstance );
		this.logger.fine( "Deploying scoped instance '" + path + "' in " + ma.getName() + "..." );
		if( ! InstanceHelpers.isTarget( scopedInstance )) {
			this.logger.fine( "Deploy action for instance '" + path + "' is cancelled in " + ma.getName() + ". Not a root instance." );
			return;
		}

		// We must prevent the concurrent creation of several VMs for a same root instance.
		// See #80.
		synchronized( LOCK ) {
			if( scopedInstance.data.get( Instance.TARGET_ACQUIRED ) == null ) {
				scopedInstance.data.put( Instance.TARGET_ACQUIRED, "yes" );
			} else {
				this.logger.finer( "Scoped instance '" + path + "' is already under deployment. This redundant request is dropped." );
				return;
			}
		}

		// If the VM creation was already done, then its machine ID has already been set.
		// It does not mean the VM is already configured, it may take some time.
		String machineId = scopedInstance.data.get( Instance.MACHINE_ID );
		if( machineId != null ) {
			this.logger.fine( "Deploy action for instance " + path + " is cancelled in " + ma.getName() + ". Already associated with a machine." );
			return;
		}

		checkConfiguration();
		InstanceStatus initialStatus = scopedInstance.getStatus();
		try {
			scopedInstance.setStatus( InstanceStatus.DEPLOYING );
			MsgCmdSetScopedInstance msg = new MsgCmdSetScopedInstance( scopedInstance );
			send( ma, msg, scopedInstance );

			Target target = this.targetResolver.findTargetHandler( this.targetHandlers, ma, scopedInstance );
			Map<String,String> targetProperties = new HashMap<String,String>( target.getProperties());
			targetProperties.putAll( scopedInstance.data );

			String scopedInstancePath = InstanceHelpers.computeInstancePath( scopedInstance );
			machineId = target.getHandler().createMachine(
					targetProperties, this.messageServerIp, this.messageServerUsername, this.messageServerPassword,
					scopedInstancePath, ma.getName());

			scopedInstance.data.put( Instance.MACHINE_ID, machineId );
			this.logger.fine( "Scoped instance " + path + "'s deployment was successfully requested in " + ma.getName() + ". Machine ID: " + machineId );

			target.getHandler().configureMachine(
					targetProperties, machineId,
					this.messageServerIp, this.messageServerUsername, this.messageServerPassword,
					scopedInstancePath, ma.getName());

			this.logger.fine( "Scoped instance " + path + "'s configuration is on its way in " + ma.getName() + "." );

		} catch( Exception e ) {
			this.logger.severe( "Failed to deploy scoped instance '" + path + "' in " + ma.getName() + ". " + e.getMessage());
			Utils.logException( this.logger, e );

			scopedInstance.setStatus( initialStatus );
			if( e instanceof TargetException)
				throw (TargetException) e;
			else if( e instanceof IOException )
				throw (IOException) e;

		} finally {
			saveConfiguration( ma );
		}
	}


	/**
	 * Undeploys a scoped instance.
	 * @param ma the managed application
	 * @param scopedInstance the instance to undeploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 * @throws TargetException if an error occurred with the target handler
	 */
	void undeployTarget( ManagedApplication ma, Instance scopedInstance ) throws TargetException, IOException {

		String path = InstanceHelpers.computeInstancePath( scopedInstance );
		this.logger.fine( "Undeploying root instance '" + path + "' in " + ma.getName() + "..." );
		if( ! InstanceHelpers.isTarget( scopedInstance )) {
			this.logger.fine( "Undeploy action for instance '" + path + "' is cancelled in " + ma.getName() + ". Not a root instance." );
			return;
		}

		checkConfiguration();
		InstanceStatus initialStatus = scopedInstance.getStatus();
		try {
			// Terminate the machine...
			// ...  and notify other agents this agent was killed.
			this.logger.fine( "Agent '" + path + "' is about to be deleted in " + ma.getName() + "." );
			Target target = this.targetResolver.findTargetHandler( this.targetHandlers, ma, scopedInstance );
			String machineId = scopedInstance.data.remove( Instance.MACHINE_ID );
			if( machineId != null ) {
				Map<String,String> targetProperties = new HashMap<String,String>( target.getProperties());
				targetProperties.putAll( scopedInstance.data );

				target.getHandler().terminateMachine( targetProperties, machineId );
				this.messagingClient.propagateAgentTermination( ma.getApplication(), scopedInstance );
			}

			this.logger.fine( "Agent '" + path + "' was successfully deleted in " + ma.getName() + "." );
			for( Instance i : InstanceHelpers.buildHierarchicalList( scopedInstance )) {
				i.setStatus( InstanceStatus.NOT_DEPLOYED );
				// DM won't send old imports upon restart...
				i.getImports().clear();
			}

			// Remove useless data for the configuration backup
			scopedInstance.data.remove( Instance.IP_ADDRESS );
			scopedInstance.data.remove( Instance.TARGET_ACQUIRED );
			this.logger.fine( "Scoped instance " + path + "'s undeployment was successfully requested in " + ma.getName() + "." );

		} catch( Exception e ) {
			this.logger.severe( "Failed to undeploy scoped instance '" + path + "' in " + ma.getName() + ". " + e.getMessage());
			Utils.logException( this.logger, e );

			scopedInstance.setStatus( initialStatus );
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

		// Restore applications
		this.appNameToManagedApplication.clear();
		for( File dir : ConfigurationUtils.findApplicationDirectories( this.configurationDirectory )) {
			try {
				loadNewApplication( dir );

			} catch( AlreadyExistingException e ) {
				this.logger.warning( "Cannot restore application in " + dir + " (already existing)." );
				Utils.logException( this.logger, e );

			} catch( InvalidApplicationException e ) {
				this.logger.warning( "Cannot restore application in " + dir + " (invalid application)." );
				Utils.logException( this.logger, e );

			} catch( IOException e ) {
				this.logger.warning( "Application restoration was incomplete from " + dir + " (I/O exception). The messaging configuration is probably invalid." );
				Utils.logException( this.logger, e );
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
				Utils.logException( this.logger, e );
			}

			// States
			for( Instance rootInstance : ma.getApplication().getRootInstances()) {
				try {
					// Not associated with a VM? => Everything must be not deployed.
					String machineId = rootInstance.data.get( Instance.MACHINE_ID );
					if( machineId == null ) {
						rootInstance.data.remove( Instance.IP_ADDRESS );
						rootInstance.data.remove( Instance.TARGET_ACQUIRED );
						for( Instance i : InstanceHelpers.buildHierarchicalList( rootInstance ))
							i.setStatus( InstanceStatus.NOT_DEPLOYED );

						continue;
					}

					// Not a running VM? => Everything must be not deployed.
					Target target = this.targetResolver.findTargetHandler( this.targetHandlers, ma, rootInstance );
					Map<String,String> targetProperties = new HashMap<String,String>( target.getProperties());
					targetProperties.putAll( rootInstance.data );

					if( ! target.getHandler().isMachineRunning( targetProperties, machineId )) {
						rootInstance.data.remove( Instance.IP_ADDRESS );
						rootInstance.data.remove( Instance.MACHINE_ID );
						rootInstance.data.remove( Instance.TARGET_ACQUIRED );
						for( Instance i : InstanceHelpers.buildHierarchicalList( rootInstance ))
							i.setStatus( InstanceStatus.NOT_DEPLOYED );
					}

					// Otherwise, ask the agent to resent the states
					else {
						rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
						this.messagingClient.sendMessageToAgent( ma.getApplication(), rootInstance, new MsgCmdSendInstances());
					}

				} catch( Exception e ) {
					this.logger.severe( "Could not request states for agent " + rootInstance.getName() + " (I/O exception)." );
					Utils.logException( this.logger, e );
				}
			}
		}
	}

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
}
