/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.openstack.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.openstack.internal.MachineLauncher.State;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class OpenstackIaasHandler implements TargetHandler {

	public static final String TARGET_ID = "iaas-openstack";
	private static String PROVIDER_NOVA = "openstack-nova";
	private static String PROVIDER_NEUTRON = "openstack-neutron";
	private static final int CHECK_DELAY = 20;

	static String IMAGE_NAME = "openstack.image-name";
	static String TENANT_NAME = "openstack.tenant-name";
	static String KEY_PAIR = "openstack.key-pair";
	static String FLAVOR_NAME = "openstack.flavor-name";
	static String SECURITY_GROUP = "openstack.security-group";
	static String API_URL = "openstack.nova-url";
	static String USER = "openstack.user";
	static String PASSWORD = "openstack.password";

	static String FLOATING_IP_POOL = "openstack.floating-ip-pool";
	static String NETWORK_ID = "openstack.network-id";
	// static String VOLUME_ID = "openstack.volumeId";
	// static String VOLUME_MOUNT_POINT = "openstack.volumeMountPoint";
	// static String VOLUME_SIZE_GB = "openstack.volumeSizeGb";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor( 1 );
	private final Map<String,MachineLauncher> serverIdToLaunchers = new ConcurrentHashMap<String,MachineLauncher> ();




	/**
	 * Starts a thread to periodically check machines under creation process.
	 * <p>
	 * Invoked by iPojo when necessary.
	 * </p>
	 */
	public void start() {
		this.timer.scheduleAtFixedRate(
				new CheckingRunnable( this.serverIdToLaunchers ),
				CHECK_DELAY,
				CHECK_DELAY,
				TimeUnit.SECONDS );
	}


	/**
	 * Stops the background thread.
	 * <p>
	 * Invoked by iPojo when necessary.
	 * </p>
	 */
	public void stop() {
		this.timer.shutdownNow();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#getTargetId()
	 */
	@Override
	public String getTargetId() {
		return TARGET_ID;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #createOrConfigureMachine(java.util.Map, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createOrConfigureMachine(
			Map<String,String> targetProperties,
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {

		this.logger.fine( "Creating a new machine." );

		// Create a handler...
		MachineLauncher handler = new MachineLauncher();
		handler.setApplicationName( applicationName );
		handler.setRootInstanceName( rootInstanceName );
		handler.setMessagingIp( messagingIp );
		handler.setMessagingPassword( messagingPassword );
		handler.setMessagingUsername( messagingUsername );
		handler.setTargetProperties( targetProperties );

		// ... and start it.
		try {
			handler.createVm();

		} catch( IOException e ) {
			throw new TargetException( e );
		}

		this.serverIdToLaunchers.put( handler.getServerId(), handler );
		return handler.getServerId();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String,String> targetProperties, String machineId ) throws TargetException {

		try {
			this.logger.info( "Terminating Openstack machine. Machine ID: " + machineId );

			NovaApi novaApi = novaApi( targetProperties );
			String anyZoneName = novaApi.getConfiguredZones().iterator().next();
			novaApi.getServerApiForZone( anyZoneName ).delete( machineId );
			novaApi.close();

		} catch( IOException e ) {
			throw new TargetException( e );
		}
	}


	/**
	 * Creates a JCloud context for Nova.
	 * @param targetProperties the target properties
	 * @return a non-null object
	 * @throws TargetException if the target properties are invalid
	 */
	static NovaApi novaApi( Map<String,String> targetProperties ) throws TargetException {

		validate( targetProperties );
		return ContextBuilder
				.newBuilder( PROVIDER_NOVA )
				.endpoint( targetProperties.get( API_URL ))
			    .credentials( identity( targetProperties ), targetProperties.get( PASSWORD ))
			    .buildApi( NovaApi.class );
	}


	/**
	 * Creates a JCloud context for Neutron.
	 * @param targetProperties the target properties
	 * @return a non-null object
	 * @throws TargetException if the target properties are invalid
	 */
	static NeutronApi neutronApi( Map<String,String> targetProperties ) throws TargetException {

		validate( targetProperties );
		return ContextBuilder
				.newBuilder( PROVIDER_NEUTRON )
				.endpoint( targetProperties.get( API_URL ))
			    .credentials( identity( targetProperties ), targetProperties.get( PASSWORD ))
			    .buildApi( NeutronApi.class );
	}


	/**
	 * Validates the target properties
	 * @param targetProperties the properties
	 * @throws TargetException if an error occurred during the validation
	 */
	static void validate( Map<String,String> targetProperties ) throws TargetException {

		checkProperty( API_URL, targetProperties );
		checkProperty( IMAGE_NAME, targetProperties );
		checkProperty( TENANT_NAME, targetProperties );
		checkProperty( FLAVOR_NAME, targetProperties );
		checkProperty( SECURITY_GROUP, targetProperties );
		checkProperty( KEY_PAIR, targetProperties );
		checkProperty( USER, targetProperties );
		checkProperty( PASSWORD, targetProperties );
	}


	/**
	 * @param targetProperties the target properties (assumed to be valid)
	 * @return the identity
	 */
	static String identity( Map<String,String> targetProperties ) {
		return targetProperties.get( TENANT_NAME ) + ":" + targetProperties.get( USER );
	}


	private static void checkProperty( String propertyName, Map<String,String> targetProperties )
	throws TargetException {

		if( ! targetProperties.containsKey( propertyName ))
			throw new TargetException( "Property '" + propertyName + "' is missing." );

		if( Utils.isEmptyOrWhitespaces( targetProperties.get( propertyName )))
			throw new TargetException( "Property '" + propertyName + "' must have a value." );
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class CheckingRunnable implements Runnable {
		private final Map<String,MachineLauncher> serverIdToLaunchers;
		private final Logger logger = Logger.getLogger( getClass().getName());


		/**
		 * Constructor.
		 * @param serverIdToLaunchers
		 */
		public CheckingRunnable( Map<String,MachineLauncher> serverIdToLaunchers ) {
			super();
			this.serverIdToLaunchers = serverIdToLaunchers;
		}


		@Override
		public void run() {

			// Check the state of all the launchers
			Set<String> keysToRemove = new HashSet<String> ();
			for( Map.Entry<String,MachineLauncher> entry : this.serverIdToLaunchers.entrySet()) {

				MachineLauncher handler = entry.getValue();
				if( handler.getState() == State.COMPLETE ) {
					keysToRemove.add( entry.getKey());
					continue;
				}

				State lastState = null;
				try {
					for( State currentState = handler.getState(); currentState != lastState; ) {
						handler.resume();
						lastState = currentState;
					}

				} catch( TargetException e ) {
					this.logger.severe( "An error occurred while configuring VM '" + handler.getVmName() + "'. " + e.getMessage());
					Utils.logException( this.logger, e );
					keysToRemove.add( entry.getKey());
				}
			}

			// Remove old keys
			for( String key : keysToRemove )
				this.serverIdToLaunchers.remove( key );
		}
	}
}
