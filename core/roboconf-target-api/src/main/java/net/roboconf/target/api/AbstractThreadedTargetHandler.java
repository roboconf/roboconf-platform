/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.Closeable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;

/**
 * An abstract implementation of target handler that supports time-consuming configurations.
 * <p>
 * Creating a machine is generally straight-forward. However, configuring it can take time.
 * Rather than blocking a thread, this class provides a timer to schedule configuration steps.
 * </p>
 * <p>
 * To use this class, just extend it and implement {@link MachineConfigurator}.<br>
 * A machine configurator is in charge of configuring a machine. There will be one instance per
 * VM instance. This instance will be invoked periodically until the machine configuration is completed.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractThreadedTargetHandler implements TargetHandler {

	protected static final int DEFAULT_DELAY = 1000;

	// Protected fields
	protected final Logger logger = Logger.getLogger( getClass().getName());
	protected long delay = DEFAULT_DELAY;

	// Private fields
	private ScheduledThreadPoolExecutor timer;
	private final Map<String,MachineConfigurator> machineIdToConfigurators = new ConcurrentHashMap<> ();
	private final CancelledMachines cancelledMachineIds = new CancelledMachines();


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

		this.timer = new ScheduledThreadPoolExecutor( 1 );
		this.timer.scheduleWithFixedDelay(
				new CheckingRunnable( this.machineIdToConfigurators, this.cancelledMachineIds ),
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
		this.timer = null;
	}


	@Override
	public final void configureMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException {

		submitMachineConfiguratorUseWithCaution( machineId, machineConfigurator( parameters, machineId ));
	}


	/**
	 * Gets or builds a machine configurator to (guess what!) configure a machine.
	 * @param parameters the target parameters
	 * @param machineId the ID machine of the machine to configure
	 * @return a machine configurator (can be null, in which case nothing will be done)
	 * @throws TargetException if something went wrong
	 */
	public abstract MachineConfigurator machineConfigurator( TargetHandlerParameters parameters, String machineId )
	throws TargetException;


	/**
	 * Cancels a machine configurator.
	 * <p>
	 * This method is useful if we try to terminate a machine that is
	 * still being configured. This make sure the configuration will stop
	 * anyway.
	 * </p>
	 * <p>
	 * If the configuration was already completed, this method does nothing.
	 * Same thing if the machine ID is null, invalid or not found.
	 * </p>
	 *
	 * @param machineId the machine ID
	 */
	protected void cancelMachineConfigurator( String machineId ) {
		this.logger.fine( "Cancelling machine '" + machineId + "'." );
		this.cancelledMachineIds.addMachineId( machineId );
	}


	/**
	 * Submits a machine configurator.
	 * <p>
	 * Most of the subclasses will never use this method. It is already invoked from
	 * the {@link #configureMachine(TargetHandlerParameters, String, Instance)} method.
	 * </p>
	 * <p>
	 * Most of target handlers only have long-running configurations but short
	 * terminations. In some cases though, terminating a machine might take time and using
	 * a configurator might be better. In such situations, a sub-class may invoke this method
	 * to run such a configurator. As a reminder, the target handler is in charge of generating a machine ID.
	 * And any submitted job can be cancelled with {@link #cancelMachineConfigurator(String)}.
	 * </p>
	 *
	 * @param machineId a machine ID (not null)
	 * @param configurator a configurator (will not be added if null)
	 */
	protected void submitMachineConfiguratorUseWithCaution( String machineId, MachineConfigurator configurator ) {
		if( configurator != null ) {
			this.logger.fine( "Configuring machine '" + machineId + "' (" + configurator.getClass().getSimpleName() + ")." );
			this.machineIdToConfigurators.put( machineId, configurator );
		}
	}


	/**
	 * @return a snapshot view of the machineIdToConfigurators map
	 */
	public Map<String,MachineConfigurator> getMachineIdToConfigurators() {
		return Collections.unmodifiableMap( this.machineIdToConfigurators );
	}


	/**
	 * @return a view of the cancelled machine IDs
	 */
	public Set<String> getCancelledMachineIds() {
		return this.cancelledMachineIds.getCancelledIds();
	}


	/**
	 * A class in charge of configuring a machine.
	 * <p>
	 * It is up to the implementation to handle states to resume the
	 * configuration of a given machine.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public interface MachineConfigurator extends Closeable {

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

		/**
		 * @return the scoped instance (cannot be null)
		 */
		Instance getScopedInstance();
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class CheckingRunnable implements Runnable {

		private final CancelledMachines cancelledMachineIds;
		private final Map<String,MachineConfigurator> machineIdToConfigurators;
		private final Logger logger = Logger.getLogger( getClass().getName());


		/**
		 * Constructor.
		 */
		public CheckingRunnable(
				Map<String,MachineConfigurator> machineIdToConfigurators,
				CancelledMachines cancelledMachineIds ) {

			super();
			this.machineIdToConfigurators = machineIdToConfigurators;
			this.cancelledMachineIds = cancelledMachineIds;
		}


		@Override
		public void run() {
			this.logger.finest( "Periodic check is running." );

			// Deal with cancelled configurations
			for( String machineId : this.cancelledMachineIds.removeSnapshot()) {
				MachineConfigurator handler = this.machineIdToConfigurators.remove( machineId );
				if( handler != null )
					closeConfigurator( machineId, handler );
			}

			// Check the state of all the launchers
			Set<String> keysToRemove = new HashSet<> ();
			for( Map.Entry<String,MachineConfigurator> entry : this.machineIdToConfigurators.entrySet()) {

				MachineConfigurator handler = entry.getValue();
				try {
					if( handler.configure()) {

						// Configure is completed, remove it from the things to check
						keysToRemove.add( entry.getKey());
						closeConfigurator( entry.getKey(), handler );

						// It may require to be configured from the DM => add the right marker
						Instance scopedInstance = handler.getScopedInstance();
						scopedInstance.data.put( Instance.READY_FOR_CFG_MARKER, "true" );
					}

				} catch( Throwable t ) {
					// We need to catch ALL the exceptions.
					// Otherwise, the timer will stop scheduling this runnable,
					// and this may result in unpredictable behaviors in Roboconf deployments.
					// That would impact all the VMs on a given target.
					this.logger.severe( "An error occurred while configuring machine '" + entry.getKey() + "'. " + t.getMessage());
					Utils.logException( this.logger, t );
					keysToRemove.add( entry.getKey());

					// If a problem occurs, try to close the handler anyway
					closeConfigurator( entry.getKey(), handler );

					// Update the scoped instance
					Instance scopedInstance = handler.getScopedInstance();
					if( scopedInstance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
						scopedInstance.setStatus( InstanceStatus.PROBLEM );
						scopedInstance.data.put( Instance.LAST_PROBLEM, "Configuration failed. " + t.getMessage());
					}
				}
			}

			// Remove old keys
			this.machineIdToConfigurators.keySet().removeAll( keysToRemove );
		}


		/**
		 * Closes the configurator.
		 * @param machineId
		 * @param handler
		 */
		private void closeConfigurator( String machineId, MachineConfigurator handler ) {
			try {
				this.logger.fine( "Closing the configurator for machine " + machineId );
				handler.close();

			} catch( Exception e ) {
				this.logger.warning( "An error occurred while closing the configurator for machine '" + machineId + "'. " + e.getMessage());
				Utils.logException( this.logger, e );
			}
		}
	}


	/**
	 * A class that guarantees concurrent operations on a set of cancelled IDs.
	 * @author Vincent Zurczak - Linagora
	 */
	static class CancelledMachines {
		private final Set<String> cancelledIds = new HashSet<> ();


		/**
		 * Adds a machine ID.
		 * @param machineId a machine ID (can be null)
		 */
		public synchronized void addMachineId( String machineId ) {
			if( machineId != null )
				this.cancelledIds.add( machineId );
		}

		/**
		 * Returns and removes all the cancelled IDs.
		 * @return a non-null set
		 */
		public synchronized Set<String> removeSnapshot() {

			Set<String> result = new HashSet<>( this.cancelledIds );
			this.cancelledIds.clear();

			return result;
		}

		/**
		 * @return a copy of the cancelledIds
		 */
		public synchronized Set<String> getCancelledIds() {
			return Collections.unmodifiableSet( this.cancelledIds );
		}
	}
}
