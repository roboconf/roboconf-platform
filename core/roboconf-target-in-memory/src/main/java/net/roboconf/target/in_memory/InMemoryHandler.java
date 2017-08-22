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

package net.roboconf.target.in_memory;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * A target that runs agents in memory.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandler implements TargetHandler {

	public static final String TARGET_ID = "in-memory";
	static final String DELAY = "in-memory.delay";
	static final String EXECUTE_REAL_RECIPES = "in-memory.execute-real-recipes";
	static final String AGENT_IP_ADDRESS = "in-memory.ip-address-of-the-agent";
	static final String WRITE_USER_DATA = "in-memory.write-user-data";

	// Injected by iPojo
	Factory standardAgentFactory, nazgulAgentFactory;
	Manager manager;

	// Internal fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,String> ipojoNameToUserDataFile = new HashMap<> ();
	private final AtomicLong defaultDelay = new AtomicLong( 0L );
	private MessagingClientFactoryRegistry registry;


	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/**
	 * Invoked by iPojo (deletes all the in-memory agent).
	 */
	public void stop() throws Exception {

		if( this.nazgulAgentFactory != null ) {
			for( ComponentInstance nazgul : this.nazgulAgentFactory.getInstances())
				deleteIPojoInstance( nazgul );
		}

		if( this.standardAgentFactory != null ) {
			for( ComponentInstance agent : this.standardAgentFactory.getInstances())
				deleteIPojoInstance( agent );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #createMachine(net.roboconf.target.api.TargetHandlerParameters)
	 */
	@Override
	public String createMachine( TargetHandlerParameters parameters ) throws TargetException {

		this.logger.fine( "Creating a new agent in memory." );
		Map<String,String> targetProperties = preventNull( parameters.getTargetProperties());

		// Need to wait?
		try {
			String delayAsString = targetProperties.get( DELAY );
			long delay = delayAsString != null ? Long.parseLong( delayAsString ) : this.defaultDelay.get();
			if( delay > 0 )
				Thread.sleep( delay );

		} catch( Exception e ) {
			this.logger.warning( "An error occurred while applying the delay property. " + e.getMessage());
			Utils.logException( this.logger, e );
		}

		String machineId = parameters.getScopedInstancePath() + " @ " + parameters.getApplicationName();
		Factory factory = findIPojoFactory( parameters );
		createIPojo(
				targetProperties,
				parameters.getMessagingProperties(),
				machineId,
				parameters.getScopedInstancePath(),
				parameters.getApplicationName(),
				parameters.getDomain(),
				factory );

		return machineId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#configureMachine(
	 * net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void configureMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		// It may require to be configured from the DM => add the right marker
		parameters.getScopedInstance().data.put( Instance.READY_FOR_CFG_MARKER, "true" );
		this.logger.fine( "Configuring machine '" + machineId + "'..." );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		this.logger.fine( "Verifying the in-memory agent for " + machineId + " is running." );
		Map<String,String> targetProperties = preventNull( parameters.getTargetProperties());

		// No agent factory => no iPojo instance => not running
		boolean result = false;
		if( this.standardAgentFactory != null )
			result = this.standardAgentFactory.getInstancesNames().contains( machineId );

		// On restoration, in-memory agents will ALL have disappeared.
		// So, it makes sense to recreate them if they do not exist anymore.
		// To determine whether we should restore them or no, we look for the model in the manager.
		if( ! result && ! simulatePlugins( targetProperties )) {

			Map.Entry<String,String> ctx = parseMachineId( machineId );
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( ctx.getValue());
			Instance scopedInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), ctx.getKey());

			// Is it supposed to be running?
			if( scopedInstance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
				this.logger.fine( "In-memory agent for " + machineId + " is supposed to be running but is not. It will be restored." );
				Map<String,String> messagingConfiguration = this.manager.messagingMngr().getMessagingClient().getConfiguration();
				Factory factory = findIPojoFactory( parameters );
				createIPojo( targetProperties, messagingConfiguration, machineId, ctx.getKey(), ctx.getValue(), this.manager.getDomain(), factory );
				result = true;
				// The agent will restore its model by asking it to the DM.
			}
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(net.roboconf.target.api.TargetHandlerParameters, java.lang.String)
	 */
	@Override
	public void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException {

		this.logger.fine( "Terminating an in-memory agent." );
		Map<String,String> targetProperties = preventNull( parameters.getTargetProperties());

		// If we executed real recipes, undeploy everything first.
		// That's because we do not really terminate the agent's machine, we just kill the agent.
		// So, it is important to stop and undeploy properly.
		if( ! simulatePlugins( targetProperties )) {
			this.logger.fine( "Stopping instances correctly (real recipes are used)." );
			Map.Entry<String,String> ctx = parseMachineId( machineId );
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( ctx.getValue());

			// We do not want to undeploy the scoped instances, but its children.
			try {
				Instance scopedInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), ctx.getKey());
				for( Instance childrenInstance : scopedInstance.getChildren())
					this.manager.instancesMngr().changeInstanceState( ma, childrenInstance, InstanceStatus.NOT_DEPLOYED );

			} catch( IOException e ) {
				throw new TargetException( e );
			}
		}

		// Destroy the IPojo
		Factory factory = findIPojoFactory( parameters );
		deleteIPojo( factory, machineId );
	}


	@Override
	public String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId )
	throws TargetException {
		// This method does not make sense for in-memory agents
		return null;
	}


	// Getters and setters

	public void setMessagingFactoryRegistry(MessagingClientFactoryRegistry registry) {
		this.registry = registry;
	}

	public long getDefaultDelay() {
		return this.defaultDelay.get();
	}

	public void setDefaultDelay( long defaultDelay ) {
		this.defaultDelay.set( defaultDelay );
	}


	// Helpers

	static boolean simulatePlugins( Map<String,String> targetProperties ) {
		String executeRealRecipesAS = targetProperties == null ? "" : targetProperties.get( EXECUTE_REAL_RECIPES );
		return ! Boolean.parseBoolean( executeRealRecipesAS );
	}

	static Map<String,String> preventNull( Map<String,String> targetProperties ) {
		return targetProperties != null ? targetProperties : new HashMap<String,String>( 0 );
	}

	static Map.Entry<String,String> parseMachineId( String machineId ) {

		// We omit some checks because the "machine ID" should only
		// look like "instance path @ application"
		int index = machineId.indexOf( '@' );
		String key = machineId.substring( 0, index ).trim();
		String value = machineId.substring( index + 1 ).trim();

		return new AbstractMap.SimpleEntry<>( key, value );
	}


	// Private methods

	private Factory findIPojoFactory( TargetHandlerParameters parameters ) {

		/*
		 * Nazguls and "classical" in-memory agents can coexist.
		 * The difference is that Nazguls execute real recipes,
		 * with a Sauron as a coordinator, while in-memory agents
		 * simulate recipes.
		 */

		return ! simulatePlugins( parameters.getTargetProperties()) ? this.nazgulAgentFactory : this.standardAgentFactory;
	}


	private void deleteIPojo( Factory factory, String machineId ) {

		if( factory != null ) {
			for( ComponentInstance instance : factory.getInstances()) {
				if( machineId.equals( instance.getInstanceName())) {
					deleteIPojoInstance( instance );
					break;
				}
			}
		}
	}


	private void deleteIPojoInstance( ComponentInstance instance ) {

		String userDataFilePath = this.ipojoNameToUserDataFile.get( instance.getInstanceName());
		if( userDataFilePath != null )
			Utils.deleteFilesRecursivelyAndQuietly( new File( userDataFilePath ));

		instance.dispose();
	}


	private void createIPojo(
			Map<String,String> targetProperties,
			Map<String,String> messagingConfiguration,
			String machineId,
			String scopedInstancePath,
			String applicationName,
			String domain,
			Factory factory )
	throws TargetException {

		// Reconfigure the messaging factory.
		final String messagingType = messagingConfiguration.get( MessagingConstants.MESSAGING_TYPE_PROPERTY );
		IMessagingClientFactory messagingFactory = this.registry.getMessagingClientFactory( messagingType );
		if( messagingFactory != null )
			messagingFactory.setConfiguration(messagingConfiguration);

		// Prepare the properties of the new POJO
		Dictionary<String,Object> configuration = new Hashtable<> ();
		configuration.put( "application-name", applicationName );
		configuration.put( "scoped-instance-path", scopedInstancePath );
		configuration.put( "domain", domain );
		configuration.put( Constants.MESSAGING_TYPE, messagingType );

		// Write user data?
		File userDataFile = null;
		try {
			if( "true".equalsIgnoreCase( targetProperties.get( WRITE_USER_DATA ))) {
				this.logger.fine( "Writing user data for in-memory agent..." );
				String s = UserDataHelpers.writeUserDataAsString( messagingConfiguration, domain, applicationName, scopedInstancePath );
				userDataFile = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString() + Constants.FILE_EXT_PROPERTIES );
				Utils.writeStringInto( s, userDataFile );
				configuration.put( "parameters", userDataFile.toURI().toURL().toString());
				configuration.put( "override-properties-with-user-data", "true" );
				this.logger.finer( "User data written in " + userDataFile.getAbsolutePath());
			}

		} catch( Exception e ) {
			throw new TargetException( "User data could not be written for an in-memory agent. Scoped instance path: " + scopedInstancePath, e );
		}

		// Execute real recipes?
		boolean simulatePlugins = simulatePlugins( targetProperties );
		configuration.put( "simulate-plugins", String.valueOf( simulatePlugins ));
		if( simulatePlugins )
			this.logger.fine( "Plug-ins and recipes execution will be simulated for " + scopedInstancePath + " in " + applicationName );
		else
			this.logger.fine( "Plug-ins and recipes will be executed for real by " + scopedInstancePath + " in " + applicationName );

		// Set the agent's IP address
		String ipAddress = targetProperties.get( AGENT_IP_ADDRESS );
		if( ! Utils.isEmptyOrWhitespaces( ipAddress ))
			configuration.put( "ip-address-of-the-agent", ipAddress.trim());
		else if( simulatePlugins )
			configuration.put( "ip-address-of-the-agent", "localhost" );
		else
			this.logger.fine( "No IP address was set in the agent's configuration. The agent will guess it." );

		// Give a name to the iPojo instance
		configuration.put( Factory.INSTANCE_NAME_PROPERTY, machineId );

		// Create it
		try {
			ComponentInstance instance = factory.createComponentInstance( configuration );
			instance.start();
			if( userDataFile != null )
				this.ipojoNameToUserDataFile.put( instance.getInstanceName(), userDataFile.getAbsolutePath());

		} catch( Exception e ) {
			Utils.deleteFilesRecursivelyAndQuietly( userDataFile );
			throw new TargetException( "An in-memory agent could not be launched. Scoped instance path: " + scopedInstancePath, e );
		}
	}
}
