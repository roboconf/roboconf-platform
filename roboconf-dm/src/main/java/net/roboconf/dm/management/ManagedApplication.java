/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.messages.Message;

/**
 * A class to store runtime information for an application.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class ManagedApplication {

	final static int MISSED_HEARTBEATS_THRESHOLD = 2;

	private final Application application;
	private final File applicationFilesDirectory;
	private final Logger logger;

	final Map<Instance,List<Message>> rootInstanceToAwaitingMessages;
	final Map<Instance,Integer> rootInstanceToMissedHeartBeatsCount;



	/**
	 * Constructor.
	 */
	public ManagedApplication( Application application, File applicationFilesDirectory ) {
		this.applicationFilesDirectory = applicationFilesDirectory;
		this.application = application;
		this.logger = Logger.getLogger( Manager.class.getName() + "." + application.getName());

		this.rootInstanceToAwaitingMessages = new HashMap<Instance,List<Message>> ();
		this.rootInstanceToMissedHeartBeatsCount = new ConcurrentHashMap<Instance,Integer> ();
	}


	public File getApplicationFilesDirectory() {
		return this.applicationFilesDirectory;
	}


	public Application getApplication() {
		return this.application;
	}


	public Logger getLogger() {
		return this.logger;
	}


	/**
	 * Stores a message to send once the root instance is online.
	 * <p>
	 * Can be called concurrently with {@link #removeAwaitingMessages(Instance)}.
	 * </p>
	 *
	 * @param instance an instance (any instance is fine, the root will be determined)
	 * @param msg the message to store (not null)
	 */
	public void storeAwaitingMessage( Instance instance, Message msg ) {

		Instance rootInstance = InstanceHelpers.findRootInstance( instance );

		// We need synchronized access to the map.
		// ConcurrentHashMap does not suit. We need atomic insertion in the lists (which are map values).
		synchronized( this.rootInstanceToAwaitingMessages ) {
			List<Message> messages = this.rootInstanceToAwaitingMessages.get( rootInstance );
			if( messages == null ) {
				messages = new ArrayList<Message>( 1 );
				this.rootInstanceToAwaitingMessages.put( rootInstance, messages );
			}

			messages.add( msg );
		}
	}


	/**
	 * Removes all the waiting messages for a given instance.
	 * <p>
	 * Can be called concurrently with {@link #storeAwaitingMessage(Instance, Message)}.
	 * </p>
	 *
	 * @param instance an instance (any instance is fine, the root will be determined)
	 * @return a non-null list
	 */
	public List<Message> removeAwaitingMessages( Instance instance ) {

		Instance rootInstance = InstanceHelpers.findRootInstance( instance );
		List<Message> result = null;

		// We reduce the spent time in the synchronized section.
		synchronized( this.rootInstanceToAwaitingMessages ) {
			result = this.rootInstanceToAwaitingMessages.remove( rootInstance );
		}

		return result != null ? result : new ArrayList<Message>( 0 );
	}


	/**
	 * Acknowledges a heart beat.
	 * @param rootInstance a root instance
	 */
	public void acknowledgeHeartBeat( Instance rootInstance ) {

		Integer count = this.rootInstanceToMissedHeartBeatsCount.get( rootInstance );
		if( count != null
				&& count > MISSED_HEARTBEATS_THRESHOLD )
			this.logger.info( "Machine " + rootInstance.getName() + " is alive and reachable again." );

		rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.rootInstanceToMissedHeartBeatsCount.remove( rootInstance );
	}


	/**
	 * Check the root instances states with respect to missed heart beats.
	 */
	public void checkStates() {

		// Check the status of root instances
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
}