/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.http.internal.clients;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.websocket.api.Session;

import net.roboconf.messaging.api.extensions.AbstractRoutingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.internal.HttpClientFactory.HttpRoutingContext;
import net.roboconf.messaging.http.internal.HttpUtils;
import net.roboconf.messaging.http.internal.messages.HttpMessage;
import net.roboconf.messaging.http.internal.messages.SubscriptionMessage;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HttpDmClient extends AbstractRoutingClient<Session> {

	// Internal field (for a convenient access).
	private static final String DM_OWNER_ID = AbstractRoutingClient.buildOwnerId( RecipientKind.DM, null, null );

	private final Map<String,Session> ctxToSession;
	private RoboconfMessageQueue messageQueue;
	private final AtomicInteger openConnections = new AtomicInteger( 0 );

	private String httpServerIp;
	private int httpPort;


	/**
	 * Constructor.
	 */
	public HttpDmClient( HttpRoutingContext routingContext ) {
		super( routingContext, RecipientKind.DM );
		this.connectionIsRequired = false;
		this.ctxToSession = routingContext.ctxToSession;
	}


	@Override
	public void openConnection() throws IOException {

		// There is only one instance per Http Factory.
		// So, we do not want to close the connection someone is still using it.
		this.openConnections.incrementAndGet();
		super.openConnection();
	}


	@Override
	public void closeConnection() throws IOException {

		// There is only one instance per Http Factory.
		// So, we do not want to close the connection someone is still using it.
		if( this.openConnections.decrementAndGet() == 0 )
			super.closeConnection();
	}


	@Override
	public void setMessageQueue( RoboconfMessageQueue messageQueue ) {
		this.messageQueue = messageQueue;
	}


	@Override
	protected Map<String,Session> getStaticContextToObject() {
		return this.ctxToSession;
	}


	@Override
	public String getMessagingType() {
		return HttpConstants.FACTORY_HTTP;
	}


	@Override
	public Map<String,String> getConfiguration() {
		return HttpUtils.httpMessagingConfiguration( this.httpServerIp, this.httpPort );
	}


	@Override
	protected void process( Session session, Message message ) throws IOException {

		if( session.isOpen()) {
			HttpUtils.sendAsynchronously( message, session.getRemote());

		} else {
			this.logger.finer( "Session is not available anymore. No message can be published." );
		}
	}


	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {
		this.logger.fine( "The DM's HTTP client is about to publish a message (" + msg + ") to " + ctx );

		// The DM has no session.
		// So, we intercept messages for the DM and determine whether the
		// message should be enqueued or ignored. This decision is based on subscriptions and the connection.
		if( ctx.getKind() == RecipientKind.DM
				&& this.connected.get()) {

			Set<MessagingContext> subs = this.routingContext.subscriptions.get( DM_OWNER_ID );
			if( subs != null && subs.contains( ctx ))
				this.messageQueue.add( msg );
		}

		// Agents => use the standard publish action.
		else {
			super.publish( ctx, msg );
		}
	}


	/**
	 * Processes a message received from a web socket (i.e. sent by agents).
	 * @param message a message
	 * @throws IOException
	 */
	public void processReceivedMessage( Message message, Session session ) throws IOException {
		this.logger.fine( "The DM's HTTP client is about to process a message (" + message + ") received through a web socket." );

		// HttpMessage
		if( message instanceof HttpMessage ) {
			HttpMessage httpMsg = (HttpMessage) message;

			// Store the session
			registerSession( httpMsg.getOwnerId(), session );

			// Publish the message
			publish( httpMsg.getCtx(), httpMsg.getMessage());
		}

		// Subscription message
		else if( message instanceof SubscriptionMessage ) {
			SubscriptionMessage sub = (SubscriptionMessage) message;

			// Store the session
			registerSession( sub.getOwnerId(), session );

			// Update the subscriptions
			if( sub.isSubscribe())
				subscribe( sub.getOwnerId(), sub.getCtx());
			else
				unsubscribe( sub.getOwnerId(), sub.getCtx());
		}
	}


	/**
	 * Indicates a message was received but could not be decoded.
	 */
	public void errorWhileReceivingMessage() {
		this.messageQueue.errorWhileReceivingMessage();
	}


	/**
	 * Sets the DM's IP address (to propagate it through its configuration).
	 * @param httpServerIp the DM's IP address (must be visible/reachable from agents)
	 */
	public void setHttpServerIp( String httpServerIp ) {
		this.httpServerIp = httpServerIp;
		this.logger.info( "The DM's IP address was changed to " + httpServerIp );
	}


	/**
	 * Sets the DM's port (to propagate it through its configuration).
	 * @param httpPort the DM's port
	 */
	public void setHttpPort( int httpPort ) {
		this.httpPort = httpPort;
		this.logger.info( "The DM's port was changed to " + httpPort );
	}


	private void registerSession( String ownerId, Session session ) {

		if( session != null )
			this.ctxToSession.put( ownerId, session );
	}
}
