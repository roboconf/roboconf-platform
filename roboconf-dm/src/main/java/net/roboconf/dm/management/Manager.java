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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.LoadResult;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.environment.EnvironmentInterfaceFactory;
import net.roboconf.dm.environment.IEnvironmentInterface;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.BulkActionException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InexistingException;
import net.roboconf.dm.management.exceptions.InvalidActionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.api.IApplicationWs.ApplicationAction;
import net.roboconf.dm.utils.ResourceUtils;
import net.roboconf.iaas.api.exceptions.IaasException;
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

	private String messageServerIp;
	private EnvironmentInterfaceFactory factory;


	/**
	 * Constructor.
	 */
	private Manager() {
		this.appNameToManagedApplication = new HashMap<String,ManagedApplication> ();
		this.factory = new EnvironmentInterfaceFactory();
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * @return the appNameToManagedApplication
	 */
	public Map<String, ManagedApplication> getAppNameToManagedApplication() {
		return this.appNameToManagedApplication;
	}


	/**
	 * Tries to change the message server IP.
	 * <p>
	 * Such a change is possible only if it was never set,
	 * or if there is no application registered.
	 * </p>
	 *
	 * @param messageServerIp the message Server IP to set
	 * @return true if the change was made, false otherwise
	 */
	public boolean tryToChangeMessageServerIp( String messageServerIp ) {

		boolean canChange = messageServerIp == null
				|| this.appNameToManagedApplication.isEmpty();

		if( canChange ) {
			this.messageServerIp = messageServerIp;
			this.logger.info( "Changing the message server IP to " + messageServerIp );
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
	 * @param application
	 * @throws AlreadyExistingException
	 */
	public void loadNewApplication( File applicationFilesDirectory ) throws AlreadyExistingException, InvalidApplicationException {

		LoadResult lr = RuntimeModelIo.loadApplication( applicationFilesDirectory );
		if( RoboconfErrorHelpers.containsCriticalErrors( lr.getLoadErrors()))
			throw new InvalidApplicationException( lr.getLoadErrors());

		Application application = lr.getApplication();
		if( null != findApplicationByName( application.getName()))
			throw new AlreadyExistingException( application.getName());

		IEnvironmentInterface envInterface = this.factory.create( this.messageServerIp, application, applicationFilesDirectory );
		envInterface.initializeResources();

		ManagedApplication ma = new ManagedApplication( application, applicationFilesDirectory, envInterface );
		this.appNameToManagedApplication.put( application.getName(), ma );
		ma.getLogger().fine( "Application " + application.getName() + " was successfully loaded and added." );
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

		cleanUp( ma );
		this.appNameToManagedApplication.remove( applicationName );
		try {
			Utils.deleteFilesRecursively( ma.getApplicationFilesDirectory());

		} catch( IOException e ) {
			ma.getLogger().warning( "An application's directory could not be deleted. " + e.getMessage());
		}
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
		List<Instance> instances = findInstancesToProcess( ma.getApplication(), instancePath, applyToAllChildren );

		// Performing the actions only means we send messages to the agent.
		IEnvironmentInterface env = ma.getEnvironmentInterface();
		switch( action ) {
		case deploy:
			deploy( instances, ma );
			break;

		case undeploy:
			undeploy( instances, ma );
			break;

		case remove:
			remove( ma.getApplication(), instances, env );
			break;

		case start:
			start( instances, env );
			break;

		case stop:
			stop( instances, env );
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

		BulkActionException bulkException = new BulkActionException( false );
		for( Instance instance : InstanceHelpers.getAllInstances( ma.getApplication())) {
			if( instance.getParent() != null ) {
				instance.setStatus( InstanceStatus.NOT_DEPLOYED );

			} else if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
				instance.setStatus( InstanceStatus.UNDEPLOYING );
				try {
					ma.getEnvironmentInterface().terminateMachine( instance );

				} catch( IaasException e ) {
					instance.setStatus( InstanceStatus.PROBLEM );
					bulkException.getInstancesToException().put( instance, e );
				}

				// Assumption: the IaaS shutdowns the system before terminating the VM.
				// It means the JVM hook setup by the agent is invoked.
				// It means the NOT_DEPLOYED state will be set asynchronously by the agent.
			}
		}

		if( ! bulkException.getInstancesToException().isEmpty()) {
			ma.getLogger().severe( bulkException.getLogMessage( false ));
			ma.getLogger().finest( bulkException.getLogMessage( true ));
			throw bulkException;
		}

		ma.getLogger().fine( "Application " + applicationName + " was successfully shutdown." );
	}


	/**
	 * Acknowledges a heart beat message.
	 * @param applicationName the application name
	 * @param rootInstance the root instance that sent this heart beat
	 * @throws InexistingException if the application or the parent instance does not exist
	 */
	public void acknowledgeHeartBeat( String applicationName, Instance rootInstance ) throws InexistingException {

		ManagedApplication ma = this.appNameToManagedApplication.get( applicationName );
		if( ma == null )
			throw new InexistingException( applicationName );

		ma.getMonitor().acknowledgeHeartBeat( rootInstance );
		ma.getLogger().finest( "A heart beat was acknowledged for " + rootInstance.getName() + " in the application " + applicationName + "." );
	}


	/**
	 * Cleans all the connections and listeners.
	 * <p>
	 * This method should be called when the DM is shutdown or when it should be reinitialized.
	 * The message server IP is reset to null by this method.
	 * </p>
	 */
	public void cleanUpAll() {

		this.messageServerIp = null;
		for( ManagedApplication ma : this.appNameToManagedApplication.values()) {
			if( ma != null )
				cleanUp( ma );
		}

		this.logger.info( "Cleaning up all the resources (connections, listeners, etc)." );
	}


	/**
	 * Changes the factory (for tests only).
	 * @param factory the factory to set
	 */
	void setFactory( EnvironmentInterfaceFactory factory ) {
		this.factory = factory;
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



	private void remove( Application app, List<Instance> instances, IEnvironmentInterface env ) throws UnauthorizedActionException {

		for( Instance instance : instances ) {
			if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED )
				throw new UnauthorizedActionException( "Instances are still deployed or running. They cannot be removed." );
		}

		for( Instance instance : instances ) {
			if( instance.getParent() != null ) {
				instance.getParent().getChildren().remove( instance );
				MsgCmdInstanceRemove message = new MsgCmdInstanceRemove( InstanceHelpers.computeInstancePath( instance ));
				env.sendMessage( message, InstanceHelpers.findRootInstance( instance ));

			} else {
				app.getRootInstances().remove( instance );
			}
		}
	}



	private void start( List<Instance> instances, IEnvironmentInterface env ) {

		for( Instance instance : instances ) {
			if( instance.getParent() != null ) {
				MsgCmdInstanceStart message = new MsgCmdInstanceStart( InstanceHelpers.computeInstancePath( instance ));
				env.sendMessage( message, InstanceHelpers.findRootInstance( instance ));
			}
		}
	}



	private void stop( List<Instance> instances, IEnvironmentInterface env ) {

		for( Instance instance : instances ) {
			if( instance.getParent() != null ) {
				MsgCmdInstanceStop message = new MsgCmdInstanceStop( InstanceHelpers.computeInstancePath( instance ));
				env.sendMessage( message, InstanceHelpers.findRootInstance( instance ));
			}
		}
	}



	private void undeploy( List<Instance> instances, ManagedApplication ma ) throws BulkActionException {

		BulkActionException bulkException = new BulkActionException( false );
		for( Instance instance : instances ) {
			if( instance.getParent() == null ) {
				try {
					ma.getEnvironmentInterface().terminateMachine( instance );

				} catch( IaasException e ) {
					instance.setStatus( InstanceStatus.PROBLEM );
					bulkException.getInstancesToException().put( instance, e );
				}

			} else {
				MsgCmdInstanceUndeploy message = new MsgCmdInstanceUndeploy( InstanceHelpers.computeInstancePath( instance ));
				ma.getEnvironmentInterface().sendMessage( message, InstanceHelpers.findRootInstance( instance ));
			}
		}

		if( ! bulkException.getInstancesToException().isEmpty()) {
			ma.getLogger().severe( bulkException.getLogMessage( false ));
			ma.getLogger().finest( bulkException.getLogMessage( true ));
			throw bulkException;
		}
	}



	private void deploy( List<Instance> instances, ManagedApplication ma ) throws BulkActionException {

		BulkActionException bulkException = new BulkActionException( true );
		for( Instance instance : instances ) {
			if( instance.getParent() == null ) {
				try {
					ma.getEnvironmentInterface().createMachine( instance );
					MsgCmdInstanceAdd message = new MsgCmdInstanceAdd( null, instance );
					ma.getEnvironmentInterface().sendMessage( message, InstanceHelpers.findRootInstance( instance ));

				} catch( IaasException e ) {
					instance.setStatus( InstanceStatus.PROBLEM );
					bulkException.getInstancesToException().put( instance, e );
				}

			} else {
				try {
					Map<String,byte[]> instanceResources = ResourceUtils.storeInstanceResources( ma.getApplicationFilesDirectory(), instance );
					MsgCmdInstanceDeploy message = new MsgCmdInstanceDeploy( InstanceHelpers.computeInstancePath( instance ), instanceResources );
					ma.getEnvironmentInterface().sendMessage( message, InstanceHelpers.findRootInstance( instance ));

				} catch( IOException e ) {
					instance.setStatus( InstanceStatus.PROBLEM );
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

		try {
			MachineMonitor monitor = ma.getMonitor();
			if( monitor != null )
				monitor.stopTimer();

		} catch( Exception e ) {
			ma.getLogger().finest( Utils.writeException( e ));
		}

		try {
			IEnvironmentInterface env = ma.getEnvironmentInterface();
			if( env != null )
				env.cleanResources();

		} catch( Exception e ) {
			ma.getLogger().finest( Utils.writeException( e ));
		}
	}
}
