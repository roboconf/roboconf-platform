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

package net.roboconf.dm.management;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.messaging.api.messages.Message;

/**
 * A class to store runtime information for an application.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class ManagedApplication {

	static final String MISSED_HEARTBEATS = "dm.missed.heartbeats";
	static final int THRESHOLD = 2;

	private final Application application;
	private final Logger logger = Logger.getLogger( getClass().getName());

	private final Map<Instance,List<Message>> scopedInstanceToAwaitingMessages;



	/**
	 * Constructor.
	 */
	public ManagedApplication( Application application ) {
		Objects.requireNonNull( application );
		Objects.requireNonNull( application.getTemplate());

		this.application = application;
		this.scopedInstanceToAwaitingMessages = new HashMap<> ();
	}


	public Map<Instance,List<Message>> getScopedInstanceToAwaitingMessages() {
		return this.scopedInstanceToAwaitingMessages;
	}


	public Application getApplication() {
		return this.application;
	}


	public File getDirectory() {
		return this.application.getDirectory();
	}


	public String getName() {
		return this.application.getName();
	}


	public Graphs getGraphs() {
		return this.application.getTemplate().getGraphs();
	}


	public File getTemplateDirectory() {
		return this.application.getTemplate().getDirectory();
	}


	@Override
	public String toString() {
		return String.valueOf( this.application );
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

		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );
		this.logger.finer( "Storing message " + msg.getClass().getSimpleName() + " for instance " + scopedInstance );

		// We need synchronized access to the map.
		// ConcurrentHashMap does not suit. We need atomic insertion in the lists (which are map values).
		synchronized( this.scopedInstanceToAwaitingMessages ) {
			List<Message> messages = this.scopedInstanceToAwaitingMessages.get( scopedInstance );
			if( messages == null ) {
				messages = new ArrayList<>( 1 );
				this.scopedInstanceToAwaitingMessages.put( scopedInstance, messages );
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

		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );
		List<Message> result = null;

		// We reduce the spent time in the synchronized section.
		synchronized( this.scopedInstanceToAwaitingMessages ) {
			result = this.scopedInstanceToAwaitingMessages.remove( scopedInstance );
		}

		return result != null ? result : new ArrayList<Message>( 0 );
	}


	/**
	 * Acknowledges a heart beat.
	 * @param scopedInstance a root instance
	 */
	public void acknowledgeHeartBeat( Instance scopedInstance ) {

		String count = scopedInstance.data.get( MISSED_HEARTBEATS );
		if( count != null
				&& Integer.parseInt( count ) > THRESHOLD )
			this.logger.info( "Agent " + InstanceHelpers.computeInstancePath( scopedInstance ) + " is alive and reachable again." );

		// Store the moment the first ACK (without interruption) was received.
		// If we were deploying, store it.
		// If we were in problem, store it.
		// If we were already deployed and started, do NOT override it.
		if( scopedInstance.getStatus() != InstanceStatus.DEPLOYED_STARTED
				|| ! scopedInstance.data.containsKey( Instance.RUNNING_FROM ))
			scopedInstance.data.put( Instance.RUNNING_FROM, String.valueOf( new Date().getTime()));

		scopedInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
		scopedInstance.data.remove( MISSED_HEARTBEATS );
	}


	/**
	 * Check the scoped instances states with respect to missed heart beats.
	 * @param notificationMngr
	 */
	public void checkStates( INotificationMngr notificationMngr ) {

		// Check the status of scoped instances
		Collection<Instance> scopedInstances = InstanceHelpers.findAllScopedInstances( this.application );
		for( Instance scopedInstance : scopedInstances ) {

			// Never started instances,
			// or scoped instances that have been stopped by an agent,
			// are not processed anymore here
			if( scopedInstance.getStatus() == InstanceStatus.NOT_DEPLOYED
					|| scopedInstance.getStatus() == InstanceStatus.DEPLOYING
					|| scopedInstance.getStatus() == InstanceStatus.UNDEPLOYING ) {
				scopedInstance.data.remove( MISSED_HEARTBEATS );
				continue;
			}

			// Otherwise
			String countAs = scopedInstance.data.get( MISSED_HEARTBEATS );
			int count = countAs == null ? 0 : Integer.parseInt( countAs );
			if( ++ count > THRESHOLD ) {
				scopedInstance.setStatus( InstanceStatus.PROBLEM );
				notificationMngr.instance( scopedInstance, this.application, EventType.CHANGED );
				this.logger.severe( "Agent " + InstanceHelpers.computeInstancePath( scopedInstance ) + " has not sent heart beats for quite a long time. Status changed to PROBLEM." );
			}

			scopedInstance.data.put( MISSED_HEARTBEATS, String.valueOf( count ));
		}
	}
}
