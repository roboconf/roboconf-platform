/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.openstack.internal.MachineHandler.State;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class OpenstackIaasHandler implements TargetHandler {

	public static final String TARGET_ID = "iaas-openstack";
	private static final int CHECK_DELAY = 20;

	private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor( 1 );
	private final Map<String,MachineHandler> serverIdToHandlers = new ConcurrentHashMap<String,MachineHandler> ();
	private final Logger logger = Logger.getLogger( getClass().getName());


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#getTargetId()
	 */
	@Override
	public String getTargetId() {
		return TARGET_ID;
	}



	/**
	 * Starts a thread to periodically check machines under creation process.
	 * <p>
	 * Invoked by iPojo when necessary.
	 * </p>
	 */
	public void start() {
		this.timer.scheduleAtFixedRate(
				new CheckingRunnable( this.serverIdToHandlers ),
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

		// Create a bean to store the configuration
		OpenstackBean config = new OpenstackBean();
		config.setComputeUrl( targetProperties.get( OpenstackConstants.COMPUTE_URL ));
		config.setFixedIp( targetProperties.get( OpenstackConstants.FIXED_IP ));
		config.setFlavor( targetProperties.get( OpenstackConstants.FLAVOR ));
		config.setFloatingIpPool( targetProperties.get( OpenstackConstants.FLOATING_IP_POOL ));
		config.setIdentityUrl( targetProperties.get( OpenstackConstants.IDENTITY_URL ));
		config.setKeypair( targetProperties.get( OpenstackConstants.KEYPAIR ));
		config.setMachineImageId( targetProperties.get( OpenstackConstants.IMAGE ));
		config.setNetworkId( targetProperties.get( OpenstackConstants.NETWORK_ID ));
		config.setPassword( targetProperties.get( OpenstackConstants.PASSWORD ));
		config.setSecurityGroup( targetProperties.get( OpenstackConstants.SECURITY_GROUP ));
		config.setTenantId( targetProperties.get( OpenstackConstants.TENANT_ID ));
		config.setUser( targetProperties.get( OpenstackConstants.USER ));
		config.setVolumeId( targetProperties.get( OpenstackConstants.VOLUME_ID ));
		config.setVolumeMountPoint( targetProperties.get( OpenstackConstants.VOLUME_MOUNT_POINT ));
		config.setVolumeSize( targetProperties.get( OpenstackConstants.VOLUME_SIZE_GB ));

		// Create a handler...
		MachineHandler handler = new MachineHandler();
		handler.setApplicationName( applicationName );
		handler.setRootInstanceName( rootInstanceName );
		handler.setMessagingIp( messagingIp );
		handler.setMessagingPassword( messagingPassword );
		handler.setMessagingUsername( messagingUsername );
		handler.setConfig( config );

		// ... and start it.
		try {
			while( handler.getState() != State.WAITING )
				handler.resume();

		} catch( IOException e ) {
			throw new TargetException( e );
		}

		this.serverIdToHandlers.put( handler.getServerId(), handler );
		return handler.getServerId();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler
	 * #terminateMachine(java.util.Map, java.lang.String)
	 */
	@Override
	public void terminateMachine( Map<String,String> targetProperties, String instanceId ) throws TargetException {

		MachineHandler handler = this.serverIdToHandlers.remove( instanceId );
		if( handler != null ) {
			this.logger.info( "Terminating Openstack machine. Machine ID: " + instanceId );
			handler.terminate( instanceId );

		} else {
			this.logger.warning( "An Openstack machine could not be terminated, the instance ID did not match anything. Machine ID: " + instanceId );
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class CheckingRunnable implements Runnable {
		private final Map<String,MachineHandler> serverIdToHandlers;
		private final Logger logger = Logger.getLogger( getClass().getName());


		/**
		 * Constructor.
		 * @param serverIdToHandlers
		 */
		public CheckingRunnable( Map<String,MachineHandler> serverIdToHandlers ) {
			super();
			this.serverIdToHandlers = serverIdToHandlers;
		}


		@Override
		public void run() {

			for( Map.Entry<String,MachineHandler> entry : this.serverIdToHandlers.entrySet()) {

				MachineHandler handler = entry.getValue();
				if( handler.getState() == State.COMPLETED )
					continue;

				try {
					State lastState = null;
					for( State currentState = handler.getState(); currentState != lastState; ) {
						handler.resume();
						lastState = currentState;
					}

				} catch( IOException e ) {
					this.logger.severe( "An error occurred while updating the configuration for Openstack machine " + entry.getKey());
				}
			}
		}
	}
}
