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
 */
public abstract class AbstractRoutingClient<T> implements IMessagingClient {

	/**
	 * A bean to wrap routing contexts.
	 * <p>
	 * At the beginning, routing contexts were stored as static final maps.
	 * Sub-classes were thus all sharing the same routing information. When used
	 * in stand-alone mode, it was fine. However, Roboconf allows to switch messaging
	 * type and such an organization would have had side effects.
	 * </p>
	 * <p>
	 * So, we replaced the static maps by a class.
	 * Each messaging factory should extend this class and pass it as an
	 * arguments to the agents it creates. This way, messaging clients that
	 * extends this class are isolated from other implementations.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static abstract class RoutingContext {
		public final Map<String,Set<MessagingContext>> subscriptions = new ConcurrentHashMap<> ();
	}


	protected final RoutingContext routingContext;
	protected final AtomicBoolean connected = new AtomicBoolean( false );
	protected final Logger logger = Logger.getLogger( getClass().getName());

	protected String ownerId, applicationName, scopedInstancePath, domain;
	protected boolean connectionIsRequired = true;



	/**
	 * Constructor.
	 * @param routingContext
	 * @param ownerKind
	 */
	public AbstractRoutingClient( RoutingContext routingContext, RecipientKind ownerKind ) {
		this.routingContext = routingContext;
		setOwnerProperties( ownerKind, null, null, null );
	}


	@Override
	public void closeConnection() throws IOException {
		this.logger.fine( getOwnerId() + " is closing its connection." );
		this.connected.set( false );
	}


	@Override
	public void openConnection() throws IOException {
		this.logger.fine( getOwnerId() + " is opening a connection." );
		this.connected.set( true );
	}


	@Override
	public Map<String,String> getConfiguration() {
		return Collections.singletonMap(MessagingConstants.MESSAGING_TYPE_PROPERTY, getMessagingType());
	}


	@Override
	public void deleteMessagingServerArtifacts( Application application ) throws IOException {

		this.logger.fine( getOwnerId() + " is deleting server artifacts for " + application );
		getStaticContextToObject().remove( this.ownerId );
		this.routingContext.subscriptions.remove( this.ownerId );
	}


	@Override
	public boolean isConnected() {
		return this.connected.get();
	}


	@Override
	public void subscribe( MessagingContext ctx ) throws IOException {
		this.logger.fine( getOwnerId() + " is subscribing to " + buildOwnerId( ctx ));
		subscribe( this.ownerId, ctx );
	}


	@Override
	public void unsubscribe( MessagingContext ctx ) throws IOException {
		this.logger.fine( getOwnerId() + " is unsubscribing to " + buildOwnerId( ctx ));
		unsubscribe( this.ownerId, ctx );
	}


	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {

		this.logger.fine( getOwnerId() + " is publishing message (" + msg + ") to " + buildOwnerId( ctx ));
		if( ! canProceed()) {
			this.logger.fine( getOwnerId() + " is dropping message (" + msg + ") for " + buildOwnerId( ctx ));
			return;
		}

		for( Map.Entry<String,Set<MessagingContext>> entry : this.routingContext.subscriptions.entrySet()) {
			if( ! entry.getValue().contains( ctx ))
				continue;

			T obj = getStaticContextToObject().get( entry.getKey());
			if( obj != null )
				process( obj, msg );
		}
	}


	@Override
	public void setOwnerProperties( RecipientKind ownerKind, String domain, String applicationName, String scopedInstancePath ) {

		// Store the fields (the owner kind is not supposed to change)
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
		this.domain = domain;

		// Update the client's owner ID
		String newOwnerId = buildOwnerId( ownerKind, applicationName, scopedInstancePath );
		this.logger.fine( "New owner ID in " + getMessagingType() + " client: " + newOwnerId );
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

			Set<MessagingContext> subscriptions = this.routingContext.subscriptions.remove( oldOwnerId );
			if( subscriptions != null )
				this.routingContext.subscriptions.put( newOwnerId, subscriptions );
		}
	}


	/**
	 * @return the routing context
	 */
	public RoutingContext getRoutingContext() {
		return this.routingContext;
	}


	/**
	 * @return the owner ID
	 */
	public String getOwnerId() {
		return this.ownerId;
	}


	/**
	 * Builds a unique ID.
	 * @param ownerKind the owner kind (not null)
	 * @param applicationName the application name (can be null)
	 * @param scopedInstancePath the scoped instance path (can be null)
	 * @return a non-null string
	 */
	public static String buildOwnerId( RecipientKind ownerKind, String applicationName, String scopedInstancePath ) {

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

		// The "domain" is not used here.
		// "domain" was not designed for self-hosted messaging but for "real" messaging servers.
		return sb.toString().trim();
	}


	/**
	 * Builds a unique ID.
	 * @param ownerKind the owner kind (not null)
	 * @param applicationName the application name (can be null)
	 * @param scopedInstancePath the scoped instance path (can be null)
	 * @return a non-null string
	 */
	public static String buildOwnerId( MessagingContext ctx ) {
		return ctx == null ? null : buildOwnerId( ctx.getKind(), ctx.getApplicationName(), ctx.getComponentOrFacetName());
	}


	/**
	 * Registers a subscription between an ID and a context.
	 * @param id a client ID
	 * @param ctx a messaging context
	 * @throws IOException
	 */
	protected void subscribe( String id, MessagingContext ctx ) throws IOException {

		if( ! canProceed())
			return;

		Set<MessagingContext> sub = this.routingContext.subscriptions.get( id );
		if( sub == null ) {
			sub = new HashSet<> ();
			this.routingContext.subscriptions.put( id, sub );
		}

		sub.add( ctx );
	}


	/**
	 * Unregisters a subscription between an ID and a context.
	 * @param id a client ID
	 * @param ctx a messaging context
	 * @throws IOException
	 */
	protected void unsubscribe( String id, MessagingContext ctx ) throws IOException {

		if( ! canProceed())
			return;

		Set<MessagingContext> sub = this.routingContext.subscriptions.get( id );
		if( sub != null ) {
			sub.remove( ctx );
			if( sub.isEmpty())
				this.routingContext.subscriptions.remove( id );
		}
	}


	/**
	 * Determines whether a messaging operation can be done.
	 * <p>
	 * A messaging operation can be publishing a message or
	 * dealing with subscriptions.
	 * </p>
	 * <p>
	 * Example: verify a connection/login was established.
	 * </p>
	 *
	 * @return true if we can proceed, false otherwise
	 */
	protected boolean canProceed() {
		return ! this.connectionIsRequired || this.connected.get();
	}


	protected abstract Map<String,T> getStaticContextToObject();
	protected abstract void process( T obj, Message message ) throws IOException;
}
