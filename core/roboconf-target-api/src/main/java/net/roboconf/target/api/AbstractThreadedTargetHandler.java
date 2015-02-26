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

package net.roboconf.target.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;

/**
 * An abstract implementation of target handler that supports time-consuming configurations.
 * <p>
 * Creating a machine is generally straight-forward. However, configuring it can take time.
 * Rather than blocking a thread, this class provides a timer to schedule configuration steps.
 * </p>
 * <p>
 * To use this class, just extend it and implement {@link MachineConfigurator}.<br />
 * A machine configurator is in charge of configuring a machine. There will be one instance per
 * VM instance. This instance will be invoked periodically until the machine configuration is completed.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractThreadedTargetHandler implements TargetHandler {

	protected static final int DEFAULT_DELAY = 1000;

	protected final Logger logger = Logger.getLogger( getClass().getName());
	protected long delay = DEFAULT_DELAY;

	private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor( 1 );
	private final Map<String,MachineConfigurator> machineIdToConfigurators = new ConcurrentHashMap<String,MachineConfigurator> ();


	/**
	 * Starts a thread to periodically check machines under creation process.
	 * <p>
	 * The period is defined by {@link #delay} whose value is expressed in milliseconds
	 * and whose default value is {@value #DEFAULT_DELAY}.
	 * </p>
	 * <p>
	 * This method should be made invokable by iPojo.
	 * </p>
	 */
	public void start() {

		// FIXME: should we create a new timer on every start?
		this.timer.scheduleAtFixedRate(
				new CheckingRunnable( this.machineIdToConfigurators ),
				0, this.delay, TimeUnit.MILLISECONDS );
	}


	/**
	 * Stops the background thread.
	 * <p>
	 * This method should be made invokable by iPojo.
	 * </p>
	 */
	public void stop() {
		this.timer.shutdownNow();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.target.api.TargetHandler#configureMachine(java.util.Map, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void configureMachine(
			Map<String,String> targetProperties,
			String machineId,
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName )
	throws TargetException {

		this.logger.fine( "Configuring machine '" + machineId + "'." );
		this.machineIdToConfigurators.put( machineId, machineConfigurator(
				targetProperties,
				machineId,
				messagingIp,
				messagingUsername,
				messagingPassword,
				rootInstanceName,
				applicationName ));
	}


	/**
	 * Gets or builds a machine configurator to (guess what!) configure a machine.
	 *
	 * @param targetProperties the target properties (e.g. access key, secret key, etc.)
	 * @param machineId the ID machine of the machine to configure
	 * @param messagingIp the IP of the messaging server
	 * @param messagingUsername the user name to connect to the messaging server
	 * @param messagingPassword the password to connect to the messaging server
	 * @param applicationName the application name
	 * @param rootInstanceName the name of the root instance associated with this VM
	 * @return a machine configurator
	 */
	public abstract MachineConfigurator machineConfigurator(
			Map<String,String> targetProperties,
			String machineId,
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName );


	/**
	 * A class in charge of configuring a machine.
	 * <p>
	 * It is up to the implementation to handle states to resume the
	 * configuration of a given machine.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public interface MachineConfigurator {

		/**
		 * Configures a machine.
		 * @return true if the machine is completely configured, false otherwise
		 * <p>
		 * If <code>false</code> is returned, another invocation will be scheduled.
		 * </p>
		 *
		 * @throws TargetException if something went wrong during the configuration
		 */
		boolean configure() throws TargetException;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class CheckingRunnable implements Runnable {
		private final Map<String,MachineConfigurator> machineIdToConfigurators;
		private final Logger logger = Logger.getLogger( getClass().getName());


		/**
		 * Constructor.
		 * @param machineIdToConfigurators
		 */
		public CheckingRunnable( Map<String,MachineConfigurator> machineIdToConfigurators ) {
			super();
			this.machineIdToConfigurators = machineIdToConfigurators;
		}


		@Override
		public void run() {

			// Check the state of all the launchers
			Set<String> keysToRemove = new HashSet<String> ();
			for( Map.Entry<String,MachineConfigurator> entry : this.machineIdToConfigurators.entrySet()) {

				MachineConfigurator handler = entry.getValue();
				try {
					if( handler.configure())
						keysToRemove.add( entry.getKey());

				} catch( Exception e ) {
					// We need to catch ALL the exceptions.
					// Otherwise, the timer will stop scheduling this runnable,
					// and this may result in unpredictable behaviors in Roboconf deployments.
					// That would impact all the VMs on a given target.
					this.logger.severe( "An error occurred while configuring machine '" + entry.getKey() + "'. " + e.getMessage());
					Utils.logException( this.logger, e );
					keysToRemove.add( entry.getKey());
				}
			}

			// Remove old keys
			this.machineIdToConfigurators.keySet().removeAll( keysToRemove );
		}
	}
}
