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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.LoadResult;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.environment.iaas.IaasResolver;
import net.roboconf.dm.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.BulkActionException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InexistingException;
import net.roboconf.dm.management.exceptions.InvalidActionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.utils.ResourceUtils;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
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
 *
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public final class Manager {

	public static final Manager INSTANCE = new Manager();

	private final Map<String,ManagedApplication> appNameToManagedApplication;
	private final Logger logger;

	private MessageServerClientFactory factory = new MessageServerClientFactory();
	private IaasResolver iaasResolver;
	private IDmClient messagingClient;
	private String messageServerIp;



	/**
	 * Constructor.
	 */
	private Manager() {
		this.appNameToManagedApplication = new HashMap<String,ManagedApplication> ();
		this.logger = Logger.getLogger( getClass().getName());
		this.iaasResolver = new IaasResolver();
		this.messagingClient = this.factory.createDmClient();
	}


	/**
	 * @return the appNameToManagedApplication
	 */
	public Map<String, ManagedApplication> getAppNameToManagedApplication() {
		return this.appNameToManagedApplication;
	}


	/**
	 * Configures the messaging client with a new message server IP.
	 * <p>
	 * Such a change is possible only if it was never set,
	 * or if there is no application registered.
	 * </p>
	 *
	 * @param messageServerIp the message Server IP to set
	 * @return true if the change was made, false otherwise
	 * @throws IOException
	 */
	public boolean tryToChangeMessageServerIp( String messageServerIp ) throws IOException {

		boolean canChange = messageServerIp == null
				|| this.appNameToManagedApplication.isEmpty();

		if( canChange ) {
			this.messageServerIp = messageServerIp;

			if( this.messagingClient.isConnected()) {
				this.messagingClient.closeConnection();
				this.messagingClient = this.factory.createDmClient();
			}

			this.logger.info( "Changing the message server IP to " + messageServerIp );
			this.messagingClient.setMessageServerIp( messageServerIp );
			this.messagingClient.openConnection( new DmMessageProcessor());

		} else {
			this.logger.info( "Discarding a request to change the message server IP to " + messageServerIp );
		}

		return canChange;
	}


	/**
	 * @return the messageServerIp
	 */
	public String getMessageServerIp() {
		return this.messageServerIp;
	}


	/**
	 * @return true if the DM is connected to the messaging server, false otherwise
	 */
	public boolean isConnectedToTheMessagingServer() {
		return this.messagingClient.isConnected();
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
		Application result = null;
		if( ma != null )
			result = ma.getApplication();

		return result;
	}


	/**
	 * Loads a new application.
	 * @param applicationFilesDirectory
	 * @return the managed application that was created
	 * @throws AlreadyExistingException if the application already exists
	 * @throws InvalidApplicationException if the application contains critical errors
	 * @throws IOException if the messaging could;not be initialized
	 */
	public ManagedApplication loadNewApplication( File applicationFilesDirectory ) throws AlreadyExistingException, InvalidApplicationException, IOException {

		LoadResult lr = RuntimeModelIo.loadApplication( applicationFilesDirectory );
		if( RoboconfErrorHelpers.containsCriticalErrors( lr.getLoadErrors()))
			throw new InvalidApplicationException( lr.getLoadErrors());

		Application application = lr.getApplication();
		if( null != findApplicationByName( application.getName()))
			throw new AlreadyExistingException( application.getName());

		this.messagingClient.listenToAgentMessages( application, ListenerCommand.START );

		ManagedApplication ma = new ManagedApplication( application, applicationFilesDirectory );
		this.appNameToManagedApplication.put( application.getName(), ma );
		ma.getLogger().fine( "Application " + application.getName() + " was successfully loaded and added." );

		return ma;
	}


	/**
	 * Sends the model to an agent.
	 * @param application the application associated with this agent
	 * @param rootInstance the root instance associated with this agent
	 * @param message the message to send (with the model)
	 * @throws IOException if the message could not be sent
	 */
	public void sendModelToAgent( Application application, Instance rootInstance, MsgCmdInstanceAdd message )
	throws IOException {

		if( this.messagingClient.isConnected())
			this.messagingClient.sendMessageToAgent( application, rootInstance, message );
	}


	/**
	 * Deletes an application.
	 * @param applicationName an application name
	 * @throws InexistingException if such an application does not exist
	 * @throws UnauthorizedActionException if parts of the application are still running
	 */
	public void deleteApplication( String applicationName ) throws InexistingException, UnauthorizedActionException {

		ManagedApplication ma = this.appNameToManagedApplication.get( applicationName );
		if( ma == null )
			throw new InexistingException( applicationName );

		// Check we can do this
		for( Instance rootInstance : ma.getApplication().getRootInstances()) {
			if( rootInstance.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( applicationName + " contains instances that are still deployed." );
		}

		// If yes, do it
		ma.getLogger().fine( "Deleting application " + applicationName + "." );
		try {
			this.messagingClient.listenToAgentMessages( ma.getApplication(), ListenerCommand.STOP );
			Utils.deleteFilesRecursively( ma.getApplicationFilesDirectory());

		} catch( IOException e ) {
			ma.getLogger().warning( "Application resources failed to be completely deleted. " + e.getMessage());
			ma.getLogger().finest( Utils.writeException( e ));
		}

		cleanUp( ma );
		cleanMessagingServer( ma );
		this.appNameToManagedApplication.remove( applicationName );
	}



	/**
	 * Performs an action on the instance of an application.
	 * <p>
	 * Most of the actions result in the sending of messages to the agents.
	 * </p>
	 *
	 * @param applicationName the application name (not null)
	 * @param actionAS the action to perform (see {@link ApplicationAction}).
	 * @param instancePath the instance path (null to apply to all the instances)
	 * @param applyToAllChildren if instancePath is not null, then true to start all the children too, false for this instance only
	 * @throws InexistingException if the application or the instance does not exist
	 * @throws InvalidActionException if the action is invalid
	 * @throws UnauthorizedActionException if an action could not be performed
	 * @throws BulkActionException if an error occurred while terminating machines
	 */
	public void perform( String applicationName, String actionAS, String instancePath, boolean applyToAllChildren )
	throws InexistingException, InvalidActionException, UnauthorizedActionException, BulkActionException {

		// Check the parameters
		ManagedApplication ma = this.appNameToManagedApplication.get( applicationName );
		if( ma == null )
			throw new InexistingException( applicationName );

		ApplicationAction action = ApplicationAction.whichAction( actionAS );
		if( action == null )
			throw new InvalidActionException( actionAS );

		if( instancePath == null && ! applyToAllChildren )
			throw new InvalidActionException( "specify an instance path or apply to all the children." );

		// Find the instances to work on
		// Undeploy are automatically applied to children on the agent
		// FIXME: there is something to do also for stop
		if( action == ApplicationAction.undeploy )
			applyToAllChildren = false;

		List<Instance> instances = findInstancesToProcess( ma.getApplication(), instancePath, applyToAllChildren );

		// Performing the actions only means we send messages to the agent.
		switch( action ) {
		case deploy:
			deploy( ma, instances );
			break;

		case undeploy:
			undeploy( ma, instances );
			break;

		case remove:
			remove( ma, instances );
			break;

		case start:
			start( ma, instances );
			break;

		case stop:
			stop( ma, instances );
			break;

		default:
			throw new InvalidActionException( actionAS );
		}

		// Log an entry
		StringBuilder sb = new StringBuilder();
		sb.append( "Action " );
		sb.append( actionAS );
		sb.append( " was succesfully transmitted to " );
		if( instancePath == null ) {
			sb.append( "all the instances" );

		} else {
			sb.append( instancePath );
			if( applyToAllChildren )
				sb.append( " and its children" );
		}

		sb.append( " in the application " );
		sb.append( applicationName );
		sb.append( "." );
		ma.getLogger().fine( sb.toString());
	}


	/**
	 * Adds an instance.
	 * @param applicationName the application name
	 * @param parentInstancePath the path of the instance to remove
	 * @param instance the instance to insert
	 * @throws InexistingException if the application or the parent instance does not exist
	 * @throws ImpossibleInsertionException if the instance could not be added
	 */
	public void addInstance( String applicationName, String parentInstancePath, Instance instance )
	throws InexistingException, ImpossibleInsertionException {

		ManagedApplication ma = this.appNameToManagedApplication.get( applicationName );
		if( ma == null )
			throw new InexistingException( applicationName );

		Instance parentInstance = null;
		if( parentInstancePath != null
				&& ( parentInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), parentInstancePath )) == null )
			throw new InexistingException( parentInstancePath );

		// 1. Insert the instance in the model first.
		// 2. Only then, propagate the information.
		if( ! InstanceHelpers.tryToInsertChildInstance( ma.getApplication(), parentInstance, instance ))
			throw new ImpossibleInsertionException( instance.getName());

		ma.getLogger().fine( "Instance " + InstanceHelpers.computeInstancePath( instance ) + " was successfully added in " + applicationName + "." );
	}


	/**
	 * Shutdowns an application.
	 * <p>
	 * It means Roboconf deletes every machine it
	 * created for this application.
	 * </p>
	 *
	 * @param applicationName the application name
	 * @throws InexistingException if the application or the parent instance does not exist
	 * @throws BulkActionException if an error occurred while terminating machines
	 */
	public void shutdownApplication( String applicationName ) throws InexistingException, BulkActionException {

		ManagedApplication ma = this.appNameToManagedApplication.get( applicationName );
		if( ma == null )
			throw new InexistingException( applicationName );

		// Undeploy everything correctly so that we keep the message server clean.
		// Also, this is useful for deployments on machines (not just on VM).
		BulkActionException bulkException = new BulkActionException( false );
		for( Instance instance : ma.getApplication().getRootInstances()) {
			if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED )
				undeploy( ma, Arrays.asList( instance ));
		}

		if( ! bulkException.getInstancesToException().isEmpty()) {
			ma.getLogger().severe( bulkException.getLogMessage( false ));
			ma.getLogger().finest( bulkException.getLogMessage( true ));
			throw bulkException;
		}

		cleanMessagingServer( ma );
		ma.getLogger().fine( "Application " + applicationName + " was successfully shutdown." );
	}


	/**
	 * Cleans all the connections and listeners.
	 * <p>
	 * This method should be called when the DM is shutdown or when it should be reinitialized.
	 * </p>
	 */
	public void cleanUpAll() {

		this.logger.info( "Cleaning up all the resources (connections, listeners, etc)." );
		for( ManagedApplication ma : this.appNameToManagedApplication.values()) {
			cleanUp( ma );
		}

		try {
			this.messagingClient.closeConnection();

		} catch( IOException e ) {
			this.logger.severe( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 * To use for tests only.
	 * @param iaasResolver the iaasResolver to set
	 */
	public void setIaasResolver( IaasResolver iaasResolver ) {
		this.iaasResolver = iaasResolver;
	}


	/**
	 * @return the iaasResolver
	 */
	public IaasResolver getIaasResolver() {
		return this.iaasResolver;
	}


	/**
	 * Mainly for tests.
	 * @return the messagingClient
	 */
	public IDmClient getMessagingClient() {
		return this.messagingClient;
	}


	/**
	 * To use for tests only.
	 * @param factory the factory for messaging clients
	 */
	public void setMessagingClientFactory( MessageServerClientFactory factory ) {
		this.factory = factory;
		this.messagingClient = factory.createDmClient();
	}


	/**
	 * Terminates a VM.
	 * @param applicationName the application name
	 * @param rootInstance the root instance associated with a machine
	 */
	public void terminateMachine( String applicationName, Instance rootInstance ) {

		try {
			ManagedApplication ma = this.appNameToManagedApplication.get( applicationName );
			if( ma == null ) {
				this.logger.severe( "Machine " + rootInstance.getName() + " failed to be resolved for application " + applicationName + "." );

			} else {
				this.logger.fine( "Machine " + rootInstance.getName() + " is about to be deleted." );
				IaasInterface iaasInterface = this.iaasResolver.findIaasInterface( ma, rootInstance );
				String machineId = rootInstance.getData().remove( Instance.MACHINE_ID );
				iaasInterface.terminateVM( machineId );

				this.logger.fine( "Machine " + rootInstance.getName() + " was successfully deleted." );
				rootInstance.setStatus( InstanceStatus.NOT_DEPLOYED );
				rootInstance.getImports().clear(); // DM won't send old imports upon restart...
			}

		} catch( IaasException e ) {
			rootInstance.setStatus( InstanceStatus.PROBLEM );
			this.logger.severe( "Machine " + rootInstance.getName() + " could not be deleted. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 * Lists the instances to process.
	 * @param app the application
	 * @param instancePath the instance path root (can be null if applyToChildren is false)
	 * @param applyToAllChildren true to include children
	 * @return a non-null list of instances
	 * @throws InexistingException if the instance path is not null and does not match any instance
	 */
	List<Instance> findInstancesToProcess( Application app, String instancePath, boolean applyToAllChildren )
	throws InexistingException {

		List<Instance> instances = new ArrayList<Instance> ();
		Instance initialInstance = null;
		if( instancePath == null ) {
			instances.addAll( InstanceHelpers.getAllInstances( app ));

		} else if(( initialInstance = InstanceHelpers.findInstanceByPath( app, instancePath )) == null ) {
			throw new InexistingException( instancePath );

		} else if( applyToAllChildren ) {
			instances.addAll( InstanceHelpers.buildHierarchicalList( initialInstance ));

		} else {
			instances.add( initialInstance );
		}

		return instances;
	}



	private void remove( ManagedApplication ma, List<Instance> instances ) throws UnauthorizedActionException, BulkActionException {

		for( Instance instance : instances ) {
			if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( "Instances are still deployed or running. They cannot be removed." );
		}

		BulkActionException bulkException = new BulkActionException( false );
		for( Instance instance : instances ) {

			if( instance.getParent() != null ) {
				try {
					MsgCmdInstanceRemove message = new MsgCmdInstanceRemove( InstanceHelpers.computeInstancePath( instance ));
					if( this.messagingClient.isConnected())
						this.messagingClient.sendMessageToAgent( ma.getApplication(), instance, message );

					// The instance will be removed once the agent has indicated it was removed.
					// See DmMessageProcessor.

				} catch( IOException e ) {
					// The instance does not have any problem, just keep trace of the exception
					bulkException.getInstancesToException().put( instance, e );
				}

			} else {
				ma.getApplication().getRootInstances().remove( instance );
			}
		}

		if( ! bulkException.getInstancesToException().isEmpty()) {
			ma.getLogger().severe( bulkException.getLogMessage( false ));
			ma.getLogger().finest( bulkException.getLogMessage( true ));
			throw bulkException;
		}
	}



	private void start( ManagedApplication ma, List<Instance> instances ) throws BulkActionException {

		BulkActionException bulkException = new BulkActionException( false );
		for( Instance instance : instances ) {
			if( instance.getParent() == null )
				continue;

			try {
				MsgCmdInstanceStart message = new MsgCmdInstanceStart( InstanceHelpers.computeInstancePath( instance ));
				if( this.messagingClient.isConnected())
					this.messagingClient.sendMessageToAgent( ma.getApplication(), instance, message );

			} catch( IOException e ) {
				// The instance does not have any problem, just keep trace of the exception
				bulkException.getInstancesToException().put( instance, e );
			}
		}

		if( ! bulkException.getInstancesToException().isEmpty()) {
			ma.getLogger().severe( bulkException.getLogMessage( false ));
			ma.getLogger().finest( bulkException.getLogMessage( true ));
			throw bulkException;
		}
	}



	private void stop( ManagedApplication ma, List<Instance> instances ) throws BulkActionException {

		BulkActionException bulkException = new BulkActionException( false );
		for( Instance instance : instances ) {
			if( instance.getParent() == null )
				continue;

			try {
				MsgCmdInstanceStop message = new MsgCmdInstanceStop( InstanceHelpers.computeInstancePath( instance ));
				if( this.messagingClient.isConnected())
					this.messagingClient.sendMessageToAgent( ma.getApplication(), instance, message );

			} catch( IOException e ) {
				// The instance does not have any problem, just keep trace of the exception
				bulkException.getInstancesToException().put( instance, e );
			}
		}

		if( ! bulkException.getInstancesToException().isEmpty()) {
			ma.getLogger().severe( bulkException.getLogMessage( false ));
			ma.getLogger().finest( bulkException.getLogMessage( true ));
			throw bulkException;
		}
	}



	private void undeploy( ManagedApplication ma, List<Instance> instances ) throws BulkActionException {

		BulkActionException bulkException = new BulkActionException( false );
		for( Instance instance : instances ) {
			try {
				MsgCmdInstanceUndeploy message = new MsgCmdInstanceUndeploy( InstanceHelpers.computeInstancePath( instance ));
				if( this.messagingClient.isConnected())
					this.messagingClient.sendMessageToAgent( ma.getApplication(), instance, message );

			} catch( IOException e ) {
				// The instance does not have any problem, just keep trace of the exception
				bulkException.getInstancesToException().put( instance, e );
			}
		}

		if( ! bulkException.getInstancesToException().isEmpty()) {
			ma.getLogger().severe( bulkException.getLogMessage( false ));
			ma.getLogger().finest( bulkException.getLogMessage( true ));
			throw bulkException;
		}
	}



	private void deploy( ManagedApplication ma, List<Instance> instances ) throws BulkActionException {

		BulkActionException bulkException = new BulkActionException( true );
		for( Instance instance : instances ) {
			if( instance.getParent() == null ) {
				try {

					// If the VM creation was already requested...
					// ... then its machine ID has already been set.
					// It does not mean the VM is already created, it may take stome time.
					String machineId = instance.getData().get( Instance.MACHINE_ID );
					if( machineId == null ) {
						instance.setStatus( InstanceStatus.DEPLOYING );
						IaasInterface iaasInterface = this.iaasResolver.findIaasInterface( ma, instance );
						machineId = iaasInterface.createVM(
								null,
								this.messageServerIp,
								instance.getName(),
								ma.getApplication().getName());

						// FIXME: the channel name is skipped here
						// As soon as we know what it is useful for, re-add it (it is in the instance)
						instance.getData().put( Instance.MACHINE_ID, machineId );
					}

				} catch( IaasException e ) {
					instance.setStatus( InstanceStatus.PROBLEM );
					bulkException.getInstancesToException().put( instance, e );
				}

			} else {
				try {
					// FIXME: we may have to add the instance on the agent too, just like for root instances
					Map<String,byte[]> instanceResources = ResourceUtils.storeInstanceResources( ma.getApplicationFilesDirectory(), instance );
					MsgCmdInstanceDeploy message = new MsgCmdInstanceDeploy( InstanceHelpers.computeInstancePath( instance ), instanceResources );
					if( this.messagingClient.isConnected())
						this.messagingClient.sendMessageToAgent( ma.getApplication(), instance, message );

				} catch( IOException e ) {
					// The instance does not have any problem, just keep trace of the exception
					bulkException.getInstancesToException().put( instance, e );
				}
			}
		}

		if( ! bulkException.getInstancesToException().isEmpty()) {
			ma.getLogger().severe( bulkException.getLogMessage( false ));
			ma.getLogger().finest( bulkException.getLogMessage( true ));
			throw bulkException;
		}
	}



	private void cleanUp( ManagedApplication ma ) {
		MachineMonitor monitor = ma.getMonitor();
		if( monitor != null )
			monitor.stopTimer();
	}



	private void cleanMessagingServer( ManagedApplication ma ) {

		try {
			this.messagingClient.deleteMessagingServerArtifacts( ma.getApplication());

		} catch( IOException e ) {
			ma.getLogger().warning( "Messaging server artifacts could not be cleaned for " + ma.getApplication().getName() + ". " + e.getMessage());
			ma.getLogger().finest( Utils.writeException( e ));
		}

	}
}
