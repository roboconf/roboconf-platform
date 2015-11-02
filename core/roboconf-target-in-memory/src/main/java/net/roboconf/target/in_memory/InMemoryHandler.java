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

package net.roboconf.target.in_memory;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.api.factory.MessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;

/**
 * A target that runs agents in memory.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryHandler implements TargetHandler {

	public static final String TARGET_ID = "in-memory";
	static final String DELAY = "in-memory.delay";
	static final String EXECUTE_REAL_RECIPES = "in-memory.execute-real-recipes";

	// Injected by iPojo
	Factory agentFactory;
	Manager manager;

	// Internal fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	private final AtomicLong defaultDelay = new AtomicLong( 0L );
	private MessagingClientFactoryRegistry registry;

	final Map<String,Map.Entry<String,String>> machineIdToCtx = new HashMap<> ();



	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#createMachine(java.util.Map,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createMachine(
			Map<String,String> targetProperties,
			Map<String,String> messagingConfiguration,
			String scopedInstancePath,
			String applicationName )
	throws TargetException {

		this.logger.fine( "Creating a new agent in memory." );
		targetProperties = preventNull( targetProperties );

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

		// Reconfigure the messaging factory.
		final String messagingType = messagingConfiguration.get("net.roboconf.messaging.type");
		MessagingClientFactory messagingFactory = this.registry.getMessagingClientFactory(messagingType);
		if (messagingFactory != null)
			messagingFactory.setConfiguration(messagingConfiguration);

		// Prepare the properties of the new POJO
		Dictionary<String,Object> configuration = new Hashtable<> ();
		configuration.put( "application-name", applicationName );
		configuration.put( "scoped-instance-path", scopedInstancePath );
		configuration.put( "messaging-type", messagingType );

		boolean simulatePlugins = simulatePlugins( targetProperties );
		configuration.put( "simulate-plugins", String.valueOf( simulatePlugins ));
		if( simulatePlugins )
			this.logger.fine( "Plug-ins and recipes execution will be simulated for " + scopedInstancePath + " in " + applicationName );
		else
			this.logger.fine( "Plug-ins and recipes will be executed for real by " + scopedInstancePath + " in " + applicationName );

		String machineId = scopedInstancePath + " @ " + applicationName;
		configuration.put( Factory.INSTANCE_NAME_PROPERTY, machineId );

		// Create it
		try {
			ComponentInstance instance = this.agentFactory.createComponentInstance( configuration );
			instance.start();

		} catch( Exception e ) {
			throw new TargetException( "An in-memory agent could not be launched. Scoped instance path: " + scopedInstancePath, e );
		}

		Map.Entry<String,String> ctx = new AbstractMap.SimpleEntry<String,String>( scopedInstancePath, applicationName );
		this.machineIdToCtx.put( machineId, ctx );
		return machineId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#configureMachine(java.util.Map, java.util.Map,
	 * java.lang.String, java.lang.String, java.lang.String, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void configureMachine(
		Map<String,String> targetProperties,
		Map<String,String> messagingConfiguration,
		String machineId,
		String scopedInstancePath,
		String applicationName,
		Instance scopedInstance )
	throws TargetException {

		this.logger.fine( "Configuring machine '" + machineId + "': nothing to configure." );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #isMachineRunning(java.util.Map, java.lang.String)
	 */
	@Override
	public boolean isMachineRunning( Map<String,String> targetProperties, String machineId )
	throws TargetException {

		// No agent factory => no iPojo instance => not running
		boolean result = false;
		if( this.agentFactory != null )
			result = this.agentFactory.getInstancesNames().contains( machineId );

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String, String> targetProperties, String machineId ) throws TargetException {

		this.logger.fine( "Terminating an in-memory agent." );
		targetProperties = preventNull( targetProperties );
		Map.Entry<String,String> ctx = this.machineIdToCtx.remove( machineId );

		// If we executed real recipes, undeploy everything first.
		// That's because we do not really terminate the agent's machine, we just kill the agent.
		// So, it is important to stop and undeploy properly.
		if( ! simulatePlugins( targetProperties )) {
			this.logger.fine( "Stopping instances correctly (real recipes are used)." );
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
		if( this.agentFactory != null ) {
			for( ComponentInstance instance : this.agentFactory.getInstances()) {
				if( machineId.equals( instance.getInstanceName())) {
					instance.dispose();
					break;
				}
			}
		}
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
		String executeRealRecipesAS = targetProperties.get( EXECUTE_REAL_RECIPES );
		return ! Boolean.parseBoolean( executeRealRecipesAS );
	}

	static Map<String,String> preventNull( Map<String,String> targetProperties ) {
		return targetProperties != null ? targetProperties : new HashMap<String,String>( 0 );
	}
}
