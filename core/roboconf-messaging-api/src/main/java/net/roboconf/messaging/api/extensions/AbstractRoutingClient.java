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

package net.roboconf.messaging.api.extensions;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.messages.Message;

/**
 * A class to dispatch messages directly into message queues.
 * <p>
 * This class can be used to replace a messaging server.
 * It will directly route messages to the right "recipients".
 * </p>
 *
 * @param <T> a handler class where messages will be directed to
 * @author Vincent Zurczak - Linagora
 *
 * TODO: to test with HTTP messaging. If working, make InMemory inherit from it.
 */
public abstract class AbstractRoutingClient<T> implements IMessagingClient {

	protected static final Map<String,Set<MessagingContext>> SUBSCRIPTIONS = new ConcurrentHashMap<> ();

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final AtomicBoolean connected = new AtomicBoolean( false );
	String ownerId;


	/**
	 * Constructor.
	 * @param ownerKind
	 */
	public AbstractRoutingClient( RecipientKind ownerKind ) {
		setOwnerProperties( ownerKind, null, null );
	}


	@Override
	public void closeConnection() throws IOException {
		this.connected.set( false );
	}


	@Override
	public void openConnection() throws IOException {
		this.connected.set( true );
	}


	@Override
	public String getMessagingType() {
		return MessagingConstants.FACTORY_IN_MEMORY;
	}


	@Override
	public Map<String,String> getConfiguration() {
		return Collections.singletonMap(MessagingConstants.MESSAGING_TYPE_PROPERTY, MessagingConstants.FACTORY_IN_MEMORY);
	}


	@Override
	public void deleteMessagingServerArtifacts( Application application ) throws IOException {

		getStaticContextToObject().remove( this.ownerId );
		SUBSCRIPTIONS.remove( this.ownerId );
	}


	@Override
	public boolean isConnected() {
		return this.connected.get();
	}


	@Override
	public void subscribe( MessagingContext ctx ) throws IOException {

		if( ! this.connected.get())
			return;

		Set<MessagingContext> sub = SUBSCRIPTIONS.get( this.ownerId );
		if( sub == null ) {
			sub = new HashSet<> ();
			SUBSCRIPTIONS.put( this.ownerId, sub );
		}

		sub.add( ctx );
	}


	@Override
	public void unsubscribe( MessagingContext ctx ) throws IOException {

		if( ! this.connected.get())
			return;

		Set<MessagingContext> sub = SUBSCRIPTIONS.get( this.ownerId );
		if( sub != null ) {
			sub.remove( ctx );
			if( sub.isEmpty())
				SUBSCRIPTIONS.remove( this.ownerId );
		}
	}


	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {

		if( ! this.connected.get())
			return;

		for( Map.Entry<String,Set<MessagingContext>> entry : SUBSCRIPTIONS.entrySet()) {
			if( ! entry.getValue().contains( ctx ))
				continue;

			T obj = getStaticContextToObject().get( entry.getKey());
			if( obj != null )
				process( obj, msg );
		}
	}


	@Override
	public void setOwnerProperties( RecipientKind ownerKind, String applicationName, String scopedInstancePath ) {

		// Build a unique ID
		StringBuilder sb = new StringBuilder();
		if( ownerKind == RecipientKind.DM ) {
			sb.append( "@DM@" );

		} else {
			if( scopedInstancePath !=null ) {
				sb.append( scopedInstancePath );
				sb.append( " " );
			}

			if( applicationName != null ) {
				sb.append( "@ " );
				sb.append( applicationName );
			}
		}

		// Update the client's owner ID
		String newOwnerId = sb.toString().trim();
		this.logger.fine( "New owner ID in in-memory client: " + newOwnerId );
		if( this.ownerId == null) {
			this.ownerId = newOwnerId;

		} else if( ! newOwnerId.equals( this.ownerId )) {

			// Switch the owner ID first.
			// Other method calls will thus use the new version.
			String oldOwnerId = this.ownerId;
			this.ownerId = newOwnerId;

			// Remove old values and associate them with the new key.
			// FIXME: I am wondering whether we should not have synchronized accesses to the static maps.
			// This could be a problem with dynamic reconfiguration of agents.
			T obj = getStaticContextToObject().remove( oldOwnerId );
			if( obj != null )
				getStaticContextToObject().put( newOwnerId, obj );

			Set<MessagingContext> subscriptions = SUBSCRIPTIONS.remove( oldOwnerId );
			if( subscriptions != null )
				SUBSCRIPTIONS.put( newOwnerId, subscriptions );
		}
	}


	protected abstract Map<String,T> getStaticContextToObject();
	protected abstract void process( T obj, Message message );
}
