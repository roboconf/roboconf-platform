/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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
import net.roboconf.dm.environment.iaas.IaasResolver;
import net.roboconf.dm.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;

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
 * <pre>
 * // To use a custom Iaas resolver
 * Manager.INSTANCE.setIaasResolver( ... );
 *
 * // To use a custom messaging client
 * Manager.INSTANCE.setMessagingClientFactory( ... );
 *
 * // Initialize the DM
 * Manager.INSTANCE.initialize( ... );
 *
 * // Only then, you can load new applications.
 * ..
 *
 * // Shutdown the manager
 * Manager.INSTANCE.shutdown();
 * </pre>
 *
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public final class Manager {

	public static final Manager INSTANCE = new Manager();
	private static final long TIMER_PERIOD = 6000;

	private final Map<String,ManagedApplication> appNameToManagedApplication;
	private final Logger logger;
	private MessageServerClientFactory factory;

	// Injected by iPojo
	private IaasInterface[] iaas;

	Timer timer;
	ManagerConfiguration configuration;
	IaasResolver iaasResolver;
	IDmClient messagingClient;



	/**
	 * Constructor.
	 */
	private Manager() {
		this.appNameToManagedApplication = new ConcurrentHashMap<String,ManagedApplication> ();
		this.logger = Logger.getLogger( getClass().getName());

		this.factory = new MessageServerClientFactory();
		this.iaasResolver = new IaasResolver();
	}


	/**
	 * Initializes the DM.
	 * <p>
	 * This method is in charge of connecting to the messaging server.
	 * It also handles the restoration of a previous state.
	 * </p>
	 *
	 * @param configuration the manager's configuration
	 * @throws IOException if an error occurs with the messaging
	 */
	public void initialize( ManagerConfiguration configuration ) throws IOException {

		if( this.configuration != null )
			throw new IOException( "The Deployment Manager was already initialized." );

		// Load the configuration and initialize the messaging
		this.configuration = configuration;
		this.messagingClient = this.factory.createDmClient();
		this.logger.info( "Setting the message server IP to " + configuration.getMessageServerIp());
		this.messagingClient.setParameters(
				configuration.getMessageServerIp(),
				configuration.getMessageServerUsername(),
				configuration.getMessageServerPassword());

		this.messagingClient.openConnection( new DmMessageProcessor());

		// Restore applications
		for( File dir : configuration.findApplicationDirectories()) {
			try {
				loadNewApplication( dir );

			} catch( AlreadyExistingException e ) {
				this.logger.severe( "Cannot restore application in " + dir + " (already existing)." );
				this.logger.finest( Utils.writeException( e ));

			} catch( InvalidApplicationException e ) {
				this.logger.severe( "Cannot restore application in " + dir + " (invalid application)." );
				this.logger.finest( Utils.writeException( e ));
			}
		}

		// Restore instances
		for( ManagedApplication ma : this.appNameToManagedApplication.values()) {

			// Instance definition
			InstancesLoadResult ilr = configuration.restoreInstances( ma );
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
			for( Instance rootInstance : ma.getApplication().getRootInstances())
				this.messagingClient.sendMessageToAgent( ma.getApplication(), rootInstance, new MsgCmdSendInstances());
		}

		// Start the timers
		this.timer = new Timer( "Roboconf's Management Timer", true );
		this.timer.scheduleAtFixedRate( new CheckerMessagesTask( this.messagingClient ), 0, TIMER_PERIOD );
		this.timer.scheduleAtFixedRate( new CheckerHeartbeatsTask(), 0, Constants.HEARTBEAT_PERIOD );
	}


	/**
	 * Saves the configuration (instances).
	 * @param ma a non-null managed application
	 */
	public void saveConfiguration( ManagedApplication ma ) {
		if( this.configuration != null )
			this.configuration.saveInstances( ma );
	}


	/**
	 * Shutdowns the manager.
	 * <p>
	 * This method is idem-potent.
	 * </p>
	 */
	public void shutdown() {

		if( this.timer != null )
			this.timer.cancel();

		this.logger.info( "Cleaning up all the resources (connections, listeners, etc)." );
		try {
			if( this.messagingClient != null
					&& this.messagingClient.isConnected())
				this.messagingClient.closeConnection();

		} catch( IOException e ) {
			this.logger.severe( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}

		if( this.configuration != null ) {
			for( ManagedApplication ma : this.appNameToManagedApplication.values())
				saveConfiguration( ma );
		}

		this.appNameToManagedApplication.clear();
		this.configuration = null;
		this.timer = null;
		this.messagingClient = null;
	}


	/**
	 * @return a non-null map (key = application name, value = managed application)
	 */
	public Map<String,ManagedApplication> getAppNameToManagedApplication() {
		return this.appNameToManagedApplication;
	}


	/**
	 * @return the messagingClient (for tests only)
	 */
	public IDmClient getMessagingClient() {
		return this.messagingClient;
	}


	/**
	 * @return a non-null list of applications
	 */
	public List<Application> listApplications() {

		List<Application> result = new ArrayList<Application> ();
		for( ManagedApplication ma : this.appNameToManagedApplication.values())
			result.add( ma.getApplication());

		return result;
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
		ApplicationLoadResult lr = RuntimeModelIo.loadApplication( applicationFilesDirectory );
		checkErrors( lr.getLoadErrors());

		Application application = lr.getApplication();
		if( null != findApplicationByName( application.getName()))
			throw new AlreadyExistingException( application.getName());

		File targetDirectory = this.configuration.findApplicationdirectory( application.getName());
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
		this.configuration.deleteInstancesFile( ma.getName());

		this.messagingClient.deleteMessagingServerArtifacts( ma.getApplication());
		this.appNameToManagedApplication.remove( applicationName );
		this.logger.fine( "Application " + applicationName + " was successfully deleted." );
	}


	/**
	 * To use for tests only.
	 * @param iaasResolver the iaasResolver to set
	 */
	public void setIaasResolver( IaasResolver iaasResolver ) {
		this.iaasResolver = iaasResolver;
	}


	/**
	 * To use for tests only.
	 * @param factory the factory for messaging clients
	 */
	public void setMessagingClientFactory( MessageServerClientFactory factory ) {
		this.factory = factory;
	}


	/**
	 * Adds an instance.
	 * @param ma the managed application
	 * @param parentInstance the parent instance
	 * @param instance the instance to insert (not null)
	 * @throws ImpossibleInsertionException if the instance could not be added
	 */
	public void addInstance( ManagedApplication ma, Instance parentInstance, Instance instance )
	throws ImpossibleInsertionException {

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
	 * Deploys an instance.
	 * @param ma the managed application
	 * @param instance the instance to deploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 */
	public void deploy( ManagedApplication ma, Instance instance ) throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		this.logger.fine( "Deploying " + instancePath + " in " + ma.getName() + "..." );
		if( instance.getParent() != null ) {

			Map<String,byte[]> instanceResources = ResourceUtils.storeInstanceResources( ma.getApplicationFilesDirectory(), instance );
			MsgCmdChangeInstanceState message = new MsgCmdChangeInstanceState( instance, instanceResources );
			send( ma, message, instance );
			this.logger.fine( "A message was (or will be) sent to the agent to deploy " + instancePath + " in " + ma.getName() + "." );

		} else {
			this.logger.fine( "Deploy action for " + instancePath + " is cancelled in " + ma.getName() + "." );
		}
	}


	/**
	 * Starts an instance.
	 * @param ma the managed application
	 * @param instance the instance to start (not null)
	 * @throws IOException if an error occurred with the messaging
	 */
	public void start( ManagedApplication ma, Instance instance ) throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		this.logger.fine( "Starting " + instancePath + " in " + ma.getName() + "..." );
		if( instance.getParent() != null ) {
			MsgCmdStartInstance message = new MsgCmdStartInstance( instance );
			send( ma, message, instance );
			this.logger.fine( "A message was (or will be) sent to the agent to start " + instancePath + " in " + ma.getName() + "." );

		} else {
			this.logger.fine( "Start action for " + instancePath + " is cancelled in " + ma.getName() + "." );
		}
	}


	/**
	 * Stops an instance.
	 * @param ma the managed application
	 * @param instance the instance to stop (not null)
	 * @throws IOException if an error occurred with the messaging
	 */
	public void stop( ManagedApplication ma, Instance instance ) throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		this.logger.fine( "Stopping " + instancePath + " in " + ma.getName() + "..." );
		if( instance.getParent() != null ) {
			MsgCmdStopInstance message = new MsgCmdStopInstance( instance );
			send( ma, message, instance );
			this.logger.fine( "A message was (or will be) sent to the agent to stop " + instancePath + " in " + ma.getName() + "." );

		} else {
			this.logger.fine( "Stop action for " + instancePath + " is cancelled in " + ma.getName() + "." );
		}
	}


	/**
	 * Undeploys an instance.
	 * @param ma the managed application
	 * @param instance the instance to undeploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 */
	public void undeploy( ManagedApplication ma, Instance instance ) throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		this.logger.fine( "Undeploying " + instancePath + " in " + ma.getName() + "..." );
		if( instance.getParent() != null ) {
			MsgCmdUndeployInstance message = new MsgCmdUndeployInstance( instance );
			send( ma, message, instance );
			this.logger.fine( "A message was (or will be) sent to the agent to undeploy " + instancePath + " in " + ma.getName() + "." );

		} else {
			this.logger.fine( "Undeploy action for " + instancePath + " is cancelled in " + ma.getName() + "." );
		}
	}


	/**
	 * Deploys a root instance.
	 * @param ma the managed application
	 * @param rootInstance the instance to deploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 * @throws IaasException if an error occurred with the IaaS
	 */
	public void deployRoot( ManagedApplication ma, Instance rootInstance ) throws IaasException, IOException {

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

		try {
			rootInstance.setStatus( InstanceStatus.DEPLOYING );
			MsgCmdSetRootInstance msg = new MsgCmdSetRootInstance( rootInstance );
			send( ma, msg, rootInstance );

			IaasInterface iaasInterface = this.iaasResolver.findIaasInterface( this.iaas, ma, rootInstance );
			machineId = iaasInterface.createVM(
					this.configuration.getMessageServerIp(),
					this.configuration.getMessageServerUsername(),
					this.configuration.getMessageServerPassword(),
					rootInstance.getName(),
					ma.getApplication().getName());

			rootInstance.getData().put( Instance.MACHINE_ID, machineId );
			this.logger.fine( "Root instance " + rootInstance.getName() + "'s deployment was successfully requested in " + ma.getName() + ". Machine ID: " + machineId );

		} catch( IaasException e ) {
			this.logger.severe( "Failed to deploy root instance " + rootInstance.getName() + " in " + ma.getName() + ". " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));

			rootInstance.setStatus( InstanceStatus.NOT_DEPLOYED );
			throw e;

		} finally {
			saveConfiguration( ma );
		}
	}


	/**
	 * Undeploys a root instance.
	 * @param ma the managed application
	 * @param rootInstance the instance to undeploy (not null)
	 * @throws IOException if an error occurred with the messaging
	 * @throws IaasException if an error occurred with the IaaS
	 */
	public void undeployRoot( ManagedApplication ma, Instance rootInstance ) throws IaasException, IOException {

		this.logger.fine( "Undeploying root instance " + rootInstance.getName() + " in " + ma.getName() + "..." );
		if( rootInstance.getParent() != null ) {
			this.logger.fine( "Undeploy action for instance " + rootInstance.getName() + " is cancelled in " + ma.getName() + ". Not a root instance." );
			return;
		}

		try {
			// Terminate the machine
			this.logger.fine( "Machine " + rootInstance.getName() + " is about to be deleted in " + ma.getName() + "." );
			IaasInterface iaasInterface = this.iaasResolver.findIaasInterface( this.iaas, ma, rootInstance );
			String machineId = rootInstance.getData().remove( Instance.MACHINE_ID );
			if( machineId != null )
				iaasInterface.terminateVM( machineId );

			this.logger.fine( "Machine " + rootInstance.getName() + " was successfully deleted in " + ma.getName() + "." );
			for( Instance i : InstanceHelpers.buildHierarchicalList( rootInstance )) {
				i.setStatus( InstanceStatus.NOT_DEPLOYED );
				// DM won't send old imports upon restart...
				i.getImports().clear();
			}

			// Remove useless data for the configuration backup
			rootInstance.getData().clear();
			this.logger.fine( "Root instance " + rootInstance.getName() + "'s undeployment was successfully requested in " + ma.getName() + "." );

		} catch( IaasException e ) {
			this.logger.severe( "Failed to undeploy root instance " + rootInstance.getName() + " in " + ma.getName() + ". " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));

			rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
			throw e;

		} finally {
			saveConfiguration( ma );
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
	 * @throws IaasException if a problem occurred with the IaaS
	 * @throws IOException if a problem occurred with the messaging
	 */
	public void deployAndStartAll( ManagedApplication ma, Instance instance ) throws IaasException, IOException {

		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		for( Instance initialInstance : initialInstances ) {
			for( Instance i : InstanceHelpers.buildHierarchicalList( initialInstance )) {
				if( i.getParent() == null ) {
					deployRoot( ma, i );

				} else {
					deploy( ma, i );
					start( ma, i );
				}
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
	 */
	public void stopAll( ManagedApplication ma, Instance instance ) throws IOException {

		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to stop all the instances, just the first children
		for( Instance initialInstance : initialInstances ) {
			if( initialInstance.getParent() != null )
				stop( ma, initialInstance );
			else for( Instance i : initialInstance.getChildren())
				stop( ma, i );
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
	 * @throws IaasException if a problem occurred with the IaaS
	 */
	public void undeployAll( ManagedApplication ma, Instance instance ) throws IOException, IaasException {

		Collection<Instance> initialInstances;
		if( instance != null )
			initialInstances = Arrays.asList( instance );
		else
			initialInstances = ma.getApplication().getRootInstances();

		// We do not need to undeploy all the instances, just the first instance
		for( Instance initialInstance : initialInstances ) {
			if( initialInstance.getParent() != null )
				undeploy( ma, initialInstance );
			else
				undeployRoot( ma, initialInstance );
		}
	}


	/**
	 * Sends a message, or stores it if the target machine is not yet online.
	 * @param ma the managed application
	 * @param message the message to send (or store)
	 * @param instance the target instance
	 * @throws IOException if an error occurred with the messaging
	 */
	void send( ManagedApplication ma, Message message, Instance instance ) throws IOException {

		// Either ignore or store the message
		if( this.messagingClient == null
				|| ! this.messagingClient.isConnected())
			this.logger.severe( "The connection with the messaging server was badly initialized. Message dropped." );
		else
			ma.storeAwaitingMessage( instance, message );

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
}
