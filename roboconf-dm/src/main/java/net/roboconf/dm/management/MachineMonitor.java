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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.utils.MessagingUtils;

/**
 * @author Noël - LIG
 */
public class MachineMonitor {

	final static int MISSED_HEARTBEATS_THRESHOLD = 2;

	private final Application application;
	private final Map<Instance,Integer> rootInstanceToMissedHeartBeatsCount;
	private final Timer timer;
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Constructor.
	 * @param application
	 */
	public MachineMonitor( Application application ) {

		this.application = application;
		this.rootInstanceToMissedHeartBeatsCount = new ConcurrentHashMap<Instance,Integer> ();
		this.timer = new Timer( "Roboconf's Heartbeat Timer", true );

		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				monitorAllMachine();
			}
		};

		this.timer.scheduleAtFixedRate( timerTask, 0, MessagingUtils.HEARTBEAT_PERIOD );
	}


	/**
	 * Stops the timer.
	 */
	public void stopTimer() {
		this.timer.cancel();
	}


	/**
	 * This method updates the status of instances.
	 */
	void monitorAllMachine() {

		// Check the state of the machines
		// We copy the list of instances to avoid concurrent modifications.
		Collection<Instance> instances = new ArrayList<Instance>( this.application.getRootInstances());
		for( Instance rootInstance : instances ) {

			// Never started instances,
			// or root instances that have been stopped by an agent,
			// are not processed anymore here
			if( rootInstance.getStatus() == InstanceStatus.NOT_DEPLOYED
					|| rootInstance.getStatus() == InstanceStatus.DEPLOYING
					|| rootInstance.getStatus() == InstanceStatus.UNDEPLOYING ) {
				this.rootInstanceToMissedHeartBeatsCount.remove( rootInstance );
				continue;
			}

			// Otherwise
			Integer count = this.rootInstanceToMissedHeartBeatsCount.get( rootInstance );
			if( count == null ){
				// We visited it once
				count = 1;

			} else if( ++ count > MISSED_HEARTBEATS_THRESHOLD ) {
				rootInstance.setStatus( InstanceStatus.PROBLEM );

				if( count == MISSED_HEARTBEATS_THRESHOLD + 1 )
					this.logger.severe( "Machine " + rootInstance.getName() + " has not sent heartbeats for quite a long time. Status changed to PROBLEM." );
			}

			this.rootInstanceToMissedHeartBeatsCount.put( rootInstance, count );
		}
	}


	/**
	 * Acknowledges a heart beat.
	 * @param rootInstance a root instance
	 */
	public void acknowledgeHeartBeat( Instance rootInstance ) {

		Integer count = this.rootInstanceToMissedHeartBeatsCount.get( rootInstance );
		if( count != null
				&& count > MISSED_HEARTBEATS_THRESHOLD
				&& rootInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED )
			this.logger.info( "Machine " + rootInstance.getName() + " is alive and reachable again." );

		rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.rootInstanceToMissedHeartBeatsCount.put( rootInstance, 0 );
	}


	/**
	 * @return the rootInstanceToMissedHeartBeatsCount
	 */
	Map<Instance,Integer> getRootInstanceToMissedHeartBeatsCount() {
		return this.rootInstanceToMissedHeartBeatsCount;
	}
}
