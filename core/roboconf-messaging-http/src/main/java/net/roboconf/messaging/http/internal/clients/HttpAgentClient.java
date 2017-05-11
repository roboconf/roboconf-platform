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
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.extensions.AbstractRoutingClient;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.internal.HttpUtils;
import net.roboconf.messaging.http.internal.messages.HttpMessage;
import net.roboconf.messaging.http.internal.messages.SubscriptionMessage;
import net.roboconf.messaging.http.internal.sockets.AgentWebSocket;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HttpAgentClient implements IMessagingClient {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final WeakReference<ReconfigurableClient<?>> reconfigurable;
	private final String dmIp;
	private final int dmPort;

	private RoboconfMessageQueue messageQueue;
	private String applicationName, scopedInstancePath;

	private AgentWebSocket socket;
	private Session clientSession;
	private WebSocketClient client;



	/**
	 * Constructor.
	 * @param reconfigurable
	 * @param dmIp
	 * @param dmPort
	 */
	public HttpAgentClient( ReconfigurableClient<?> reconfigurable, String dmIp, int dmPort ) {
		this.reconfigurable = new WeakReference<ReconfigurableClient<?>>( reconfigurable );
		this.dmIp = dmIp;
		this.dmPort = dmPort;
	}


	/**
	 * @return the wrapping reconfigurable client (may be {@code null}).
	 */
	public final ReconfigurableClient<?> getReconfigurableClient() {
		return this.reconfigurable.get();
	}


	@Override
	public void setMessageQueue( RoboconfMessageQueue messageQueue ) {
		this.messageQueue = messageQueue;
	}


	@Override
	public boolean isConnected() {
		return this.client != null && this.clientSession != null;
	}


	@Override
	public void setOwnerProperties( RecipientKind ownerKind, String domain, String applicationName, String scopedInstancePath ) {
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
		this.logger.fine( "Owner properties changed to " + getId());

		// "domain" is ignored, the DM is the domain here.
	}


	@Override
	public void openConnection() throws IOException {

		this.logger.info( getId() + " is opening a connection to the DM." );
		try {
			this.client = new WebSocketClient();
			this.socket = new AgentWebSocket( this.messageQueue );
			this.client.start();

			URI dmUri = new URI( "ws://" + this.dmIp + ":" + this.dmPort + HttpConstants.DM_SOCKET_PATH );
			this.logger.fine( "Connecting to " + dmUri );
			ClientUpgradeRequest request = new ClientUpgradeRequest();

			Future<Session> fut = this.client.connect( this.socket, dmUri, request );
			this.clientSession = fut.get();

		} catch( Exception e ) {
			throw new IOException( e );
		}
	}


	@Override
	public void closeConnection() throws IOException {

		this.logger.info( getId() + " is closing its connection to the DM." );
		try {
			if( this.client != null )
				this.client.stop();

		} catch( Exception e ) {
			throw new IOException( e );
		}
	}


	@Override
	public String getMessagingType() {
		return HttpConstants.FACTORY_HTTP;
	}


	@Override
	public Map<String,String> getConfiguration() {
		return HttpUtils.httpMessagingConfiguration( this.dmIp, this.dmPort );
	}


	@Override
	public void subscribe( MessagingContext ctx ) throws IOException {

		String ownerId = AbstractRoutingClient.buildOwnerId( RecipientKind.AGENTS, this.applicationName, this.scopedInstancePath );
		this.logger.fine( getId() + " is about to subscribe to " + ownerId );
		HttpUtils.sendAsynchronously(
				new SubscriptionMessage( ownerId, ctx, true ),
				this.clientSession.getRemote());
	}


	@Override
	public void unsubscribe( MessagingContext ctx ) throws IOException {

		String ownerId = AbstractRoutingClient.buildOwnerId( RecipientKind.AGENTS, this.applicationName, this.scopedInstancePath );
		this.logger.fine( getId() + " is about to unsubscribe to " + ownerId );
		HttpUtils.sendAsynchronously(
				new SubscriptionMessage( ownerId, ctx, false ),
				this.clientSession.getRemote());
	}


	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {

		String ownerId = AbstractRoutingClient.buildOwnerId( RecipientKind.AGENTS, this.applicationName, this.scopedInstancePath );
		this.logger.fine( getId() + " is about to publish a message (" + msg + ") to " + ownerId );
		HttpUtils.sendAsynchronously(
				new HttpMessage( ownerId, msg, ctx ),
				this.clientSession.getRemote());
	}


	@Override
	public void deleteMessagingServerArtifacts( Application application )
	throws IOException {
		// nothing
	}


	String getId() {
		return this.scopedInstancePath + " @ " + this.applicationName;
	}
}
