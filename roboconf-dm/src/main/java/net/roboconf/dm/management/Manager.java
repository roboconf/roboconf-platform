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
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.LoadResult;
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
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceAdd;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceDeploy;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRemove;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStart;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStop;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceUndeploy;

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
	private ManagerConfiguration configuration;
	private Timer timer;

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
		this.messagingClient.setMessageServerIp( configuration.getMessageServerIp());
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
			configuration.restoreInstances( ma );
		}

		// Start the timer
		ManagerTimerTask timerTask = new ManagerTimerTask( this.messagingClient );
		this.timer = new Timer( "Roboconf's Management Timer", true );
		this.timer.scheduleAtFixedRate( timerTask, 0, TIMER_PERIOD );
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
				this.configuration.saveInstances( ma );
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

		this.logger.fine( "Loading application from " + applicationFilesDirectory + "..." );
		LoadResult lr = RuntimeModelIo.loadApplication( applicationFilesDirectory );
		if( RoboconfErrorHelpers.containsCriticalErrors( lr.getLoadErrors()))
			throw new InvalidApplicationException( lr.getLoadErrors());

		for( RoboconfError warning : RoboconfErrorHelpers.findWarnings( lr.getLoadErrors())) {
			StringBuilder sb = new StringBuilder();
			sb.append( warning.getErrorCode().getMsg());
			if( ! Utils.isEmptyOrWhitespaces( warning.getDetails()))
				sb.append( " " + warning.getDetails());

			this.logger.warning( sb.toString());
		}

		Application application = lr.getApplication();
		if( null != findApplicationByName( application.getName()))
			throw new AlreadyExistingException( application.getName());

		File targetDirectory = this.configuration.findApplicationdirectory( application.getName());
		if( Utils.isAncestorFile( targetDirectory, applicationFilesDirectory ))
			throw new IOException( "Failed to move the " + applicationFilesDirectory + " in Roboconf's work directory." );

		ManagedApplication ma = new ManagedApplication( application, targetDirectory );
		this.messagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.START );
		this.appNameToManagedApplication.put( ma.getApplication().getName(), ma );
		ma.getLogger().fine( "Application " + ma.getApplication().getName() + " was successfully loaded and added." );

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

		this.messagingClient.deleteMessagingServerArtifacts( ma.getApplication());
		this.appNameToManagedApplication.remove( applicationName );
		this.logger.fine( "Application " + applicationName + " was successfully deleted." );
	}


	/**
	 * Shutdowns an application.
	 * <p>
	 * It means Roboconf deletes every machine it created for this application.
	 * </p>
	 *
	 * @param ma the managed application
	 * @throws IOException if there was an error with the messaging
	 */
	public void shutdownApplication( ManagedApplication ma ) throws IOException {

		this.logger.fine( "Deleting application " + ma.getApplication().getName() + "..." );
		for( Instance rootInstance : ma.getApplication().getRootInstances())
			undeploy( ma, rootInstance );

		this.logger.fine( "Application " + ma.getApplication().getName() + " was successfully shutdown." );
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
	 * @param instance the instance to insert
	 * @throws ImpossibleInsertionException if the instance could not be added
	 */
	public void addInstance( ManagedApplication ma, Instance parentInstance, Instance instance )
	throws ImpossibleInsertionException {

		if( ! InstanceHelpers.tryToInsertChildInstance( ma.getApplication(), parentInstance, instance ))
			throw new ImpossibleInsertionException( instance.getName());

		ma.getLogger().fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully added." );

		// Store the message because we want to make sure the message is not lost
		ma.storeAwaitingMessage( instance, new MsgCmdInstanceAdd( parentInstance, instance ));
		this.configuration.saveInstances( ma );
	}


	/**
	 * Removes an instance
	 * @param ma the managed application
	 * @param instance the instance to remove
	 * @throws UnauthorizedActionException if we try to remove an instance that seems to be running
	 * @throws IOException if an error occurred with the messaging
	 */
	public void removeInstance( ManagedApplication ma, Instance instance ) throws UnauthorizedActionException, IOException {

		for( Instance i : InstanceHelpers.buildHierarchicalList( instance )) {
			if( i.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( "Instances are still deployed or running. They cannot be removed." );
		}

		if( InstanceHelpers.findRootInstance( instance ).getStatus() == InstanceStatus.DEPLOYED_STARTED ) {
			// There should be a model on the agent. Even if it is not set, we expect the agent to acknowledge.
			MsgCmdInstanceRemove message = new MsgCmdInstanceRemove( InstanceHelpers.computeInstancePath( instance ));
			// Make sure the message will arrive.
			ma.storeAwaitingMessage( instance, message );
		}

		if( instance.getParent() == null )
			ma.getApplication().getRootInstances().remove( instance );
		else
			instance.getParent().getChildren().remove( instance );

		ma.getLogger().fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully removed." );
		this.configuration.saveInstances( ma );
	}


	/**
	 * Deploys an instance.
	 * @param ma the managed application
	 * @param instance the instance to deploy
	 * @param applyToChildren true to deploy its children, false to only deploy this instance
	 * @throws IOException if an error occurred with the messaging
	 */
	public void deploy( ManagedApplication ma, Instance instance, boolean applyToChildren ) throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		ma.getLogger().fine( "Deploying " + instancePath + "... Applied to children: " + applyToChildren + "." );
		if( instance.getParent() != null ) {
			ma.getLogger().fine( "Deploy action for " + instancePath + " is cancelled." );
			return;
		}

		List<Instance> instances = new ArrayList<Instance> ();
		if( applyToChildren )
			instances.addAll( InstanceHelpers.buildHierarchicalList( instance ));
		else
			instances.add( instance );

		for( Instance i : instances ) {
			Map<String,byte[]> instanceResources = ResourceUtils.storeInstanceResources( ma.getApplicationFilesDirectory(), i );
			MsgCmdInstanceDeploy message = new MsgCmdInstanceDeploy( i, instanceResources );
			send( ma, message, i );
		}

		ma.getLogger().fine( "A message was (or will be) sent to the agent to deploy " + instancePath );
	}


	/**
	 * Starts an instance.
	 * @param ma the managed application
	 * @param instance the instance to start
	 * @param applyToChildren true to start its children, false to only start this instance
	 * @throws IOException if an error occurred with the messaging
	 */
	public void start( ManagedApplication ma, Instance instance, boolean applyToChildren ) throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		ma.getLogger().fine( "Starting " + instancePath + "... Applied to children: " + applyToChildren + "." );
		if( instance.getParent() != null ) {
			MsgCmdInstanceStart message = new MsgCmdInstanceStart( instance, applyToChildren );
			send( ma, message, instance );
			ma.getLogger().fine( "A message was (or will be) sent to the agent to start " + instancePath );

		} else {
			ma.getLogger().fine( "Start action for " + instancePath + " is cancelled." );
		}
	}


	/**
	 * Stops an instance.
	 * @param ma the managed application
	 * @param instance the instance to stop
	 * @throws IOException if an error occurred with the messaging
	 */
	public void stop( ManagedApplication ma, Instance instance ) throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		ma.getLogger().fine( "Stopping " + instancePath + "..." );
		if( instance.getParent() != null ) {
			MsgCmdInstanceStop message = new MsgCmdInstanceStop( instance );
			send( ma, message, instance );
			ma.getLogger().fine( "A message was (or will be) sent to the agent to stop " + instancePath );

		} else {
			ma.getLogger().fine( "Stop action for " + instancePath + " is cancelled." );
		}
	}


	/**
	 * Undeploys an instance.
	 * @param ma the managed application
	 * @param instance the instance to undeploy
	 * @throws IOException if an error occurred with the messaging
	 */
	public void undeploy( ManagedApplication ma, Instance instance ) throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( instance );
		ma.getLogger().fine( "Undeploying " + instancePath + "..." );
		if( instance.getParent() != null ) {
			MsgCmdInstanceUndeploy message = new MsgCmdInstanceUndeploy( instance );
			send( ma, message, instance );
			ma.getLogger().fine( "A message was (or will be) sent to the agent to undeploy " + instancePath );

		} else {
			ma.getLogger().fine( "Undeploy action for " + instancePath + " is cancelled." );
		}
	}


	/**
	 * Deploys a root instance.
	 * @param ma the managed application
	 * @param rootInstance the instance to deploy
	 * @param applyToChildren true to also deploy its children, false otherwise
	 * @throws IOException if an error occurred with the messaging
	 * @throws IaasException if an error occurred with the IaaS
	 */
	public void deployRoot( ManagedApplication ma, Instance rootInstance, boolean applyToChildren ) throws IaasException, IOException {

		ma.getLogger().fine( "Deploying rootinstance " + rootInstance.getName() + "..." );
		if( rootInstance.getParent() != null ) {
			ma.getLogger().fine( "Deploy action for instance " + rootInstance.getName() + " is cancelled. Not a root instance." );
			return;
		}

		// If the VM creation was already requested, then its machine ID has already been set.
		// It does not mean the VM is already created, it may take some time.
		String machineId = rootInstance.getData().get( Instance.MACHINE_ID );
		if( machineId != null ) {
			ma.getLogger().fine( "Deploy action for instance " + rootInstance.getName() + " is cancelled. Already associated with a machine." );
			return;
		}

		try {
			rootInstance.setStatus( InstanceStatus.DEPLOYING );
			MsgCmdInstanceAdd msg = new MsgCmdInstanceAdd((String) null, rootInstance );
			ma.storeAwaitingMessage( rootInstance, msg );

			if( applyToChildren ) {
				for( Instance childInstance : rootInstance.getChildren())
					deploy( ma, childInstance, true );
			}

			IaasInterface iaasInterface = this.iaasResolver.findIaasInterface( ma, rootInstance );
			machineId = iaasInterface.createVM(
					null, this.configuration.getMessageServerIp(),
					rootInstance.getName(), ma.getApplication().getName());

			rootInstance.getData().put( Instance.MACHINE_ID, machineId );
			ma.getLogger().fine( "Root instance " + rootInstance.getName() + "'s deployment was successfully requested. Machine ID: " + machineId );

		} catch( IaasException e ) {
			ma.getLogger().severe( "Failed to deploy root instance " + rootInstance.getName() + " . " + e.getMessage());
			ma.getLogger().finest( Utils.writeException( e ));

			rootInstance.setStatus( InstanceStatus.PROBLEM );
			throw e;

		} finally {
			this.configuration.saveInstances( ma );
		}
	}


	/**
	 * Undeploys a root instance.
	 * @param ma the managed application
	 * @param rootInstance the instance to undeploy
	 * @throws IOException if an error occurred with the messaging
	 * @throws IaasException if an error occurred with the IaaS
	 */
	public void undeployRoot( ManagedApplication ma, Instance rootInstance ) throws IaasException, IOException {

		ma.getLogger().fine( "Undeploying rootinstance " + rootInstance.getName() + "..." );
		if( rootInstance.getParent() != null ) {
			ma.getLogger().fine( "Undeploy action for instance " + rootInstance.getName() + " is cancelled. Not a root instance." );
			return;
		}

		try {
			// Send a message to undeploy the model.
			// This is useful when the root was not created by the DM (example: a device)
			MsgCmdInstanceUndeploy message = new MsgCmdInstanceUndeploy( rootInstance );
			this.messagingClient.sendMessageToAgent( ma.getApplication(), rootInstance, message );

			// Terminate the machine
			this.logger.fine( "Machine " + rootInstance.getName() + " is about to be deleted." );
			IaasInterface iaasInterface = this.iaasResolver.findIaasInterface( ma, rootInstance );
			String machineId = rootInstance.getData().remove( Instance.MACHINE_ID );
			if( machineId != null )
				iaasInterface.terminateVM( machineId );

			this.logger.fine( "Machine " + rootInstance.getName() + " was successfully deleted." );
			for( Instance i : InstanceHelpers.buildHierarchicalList( rootInstance )) {
				i.setStatus( InstanceStatus.NOT_DEPLOYED );
				// DM won't send old imports upon restart...
				i.getImports().clear();
			}

			ma.getLogger().fine( "Root instance " + rootInstance.getName() + "'s undeployment was successfully requested." );

		} catch( IaasException e ) {
			ma.getLogger().severe( "Failed to undeploy root instance " + rootInstance.getName() + " . " + e.getMessage());
			ma.getLogger().finest( Utils.writeException( e ));

			rootInstance.setStatus( InstanceStatus.PROBLEM );
			throw e;

		} finally {
			this.configuration.saveInstances( ma );
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

		Instance rootInstance = InstanceHelpers.findRootInstance( instance );
		if( instance.getStatus() == InstanceStatus.DEPLOYING )
			ma.storeAwaitingMessage( rootInstance, message );
		else
			this.messagingClient.sendMessageToAgent( ma.getApplication(), rootInstance, message );
	}
}
