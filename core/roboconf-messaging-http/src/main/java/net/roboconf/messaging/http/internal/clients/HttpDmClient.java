/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.messaging.api.extensions.AbstractRoutingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.utils.SerializationUtils;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.internal.messages.HttpMessage;
import net.roboconf.messaging.http.internal.messages.SubscriptionMessage;

import org.eclipse.jetty.websocket.api.Session;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HttpDmClient extends AbstractRoutingClient<Session> {

	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class HttpRoutingContext extends RoutingContext {
		public final Map<String,Session> ctxToSession = new ConcurrentHashMap<> ();
	}

	// Internal field (for a convenient access).
	private static final String DM_OWNER_ID = AbstractRoutingClient.buildOwnerId( RecipientKind.DM, null, null );
	private final Map<String,Session> ctxToSession;
	private LinkedBlockingQueue<Message> messageQueue;


	/**
	 * Constructor.
	 * @param routingContext
	 */
	public HttpDmClient() {
		super( new HttpRoutingContext(), RecipientKind.DM );
		this.connectionIsRequired = false;
		this.ctxToSession = ((HttpRoutingContext) this.routingContext).ctxToSession;
	}


	@Override
	public void setMessageQueue( LinkedBlockingQueue<Message> messageQueue ) {
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
	protected void process( Session session, Message message ) throws IOException {

		byte[] rawData = SerializationUtils.serializeObject( message );
		ByteBuffer data = ByteBuffer.wrap( rawData );
		session.getRemote().sendBytes( data );
	}


	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {

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


	private void registerSession( String ownerId, Session session ) {

		if( session != null )
			this.ctxToSession.put( ownerId, session );
	}
}
