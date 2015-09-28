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

package net.roboconf.messaging.http.internal;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.eclipse.jetty.websocket.jsr356.ClientContainer;

import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.client.IClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.api.utils.SerializationUtils;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.HttpMessage;

/**
 * Common HTTP client-related stuffs.
 * @author Pierre-Yves Gibello - Linagora
 */
public abstract class HttpClient implements IClient {

	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	private final WeakReference<ReconfigurableClient<?>> reconfigurable;
	private Map<String, Session> sessions = new HashMap<String, Session>();
	protected LinkedBlockingQueue<Message> messageQueue;
	
	private String ipAddress;
	private String port;
	protected boolean isConnected = false;

	/**
	 * Constructor.
	 * @param reconfigurable
	 */
	protected HttpClient( ReconfigurableClient<?> reconfigurable, String ip, String port ) {
		this.reconfigurable = new WeakReference<ReconfigurableClient<?>>(reconfigurable);
		this.ipAddress = ip;
		this.port = port;
	}

	/**
	 * @return the wrapping reconfigurable client (may be {@code null}).
	 */
	public final ReconfigurableClient<?> getReconfigurableClient() {
		return this.reconfigurable.get();
	}

	/**
	 * Retrieve the unique ID of this client.
	 * @return a unique ID
	 */
	public abstract String getId();

	/**
	 * MQ is injected (by the DM or agent).
	 */
	@Override
	public final void setMessageQueue( LinkedBlockingQueue<Message> messageQueue ) {
		this.messageQueue = messageQueue;
	}


	@Override
	public final String getMessagingType() {
		return HttpConstants.HTTP_FACTORY_TYPE;
	}

	@Override
	public synchronized void openConnection() throws IOException {

		// Already connected? Do nothing
		if (this.isConnected) {
			this.logger.finest("The HTTP client is already open");
			return;
		}

		this.isConnected = true;
		this.logger.finest("Connection for the HTTP client opened");
	}


	@Override
	public synchronized void closeConnection() throws IOException {

		// Already closed? Do nothing
		if (!this.isConnected) {
			this.logger.finest("The HTTP client is already closed");
			return;
		}

		this.isConnected = false;
		this.logger.finest("Connection for the HTTP client closed");
	}

	@Override
	public synchronized boolean isConnected() {
		return this.isConnected;
	}

	void sendMessage(Session session, String exchangeName, String routingKey, Message message) throws IOException {
		
		if(! this.isConnected) throw new IOException("HttpClient NOT CONNECTED !!");
		
		HttpMessage msg = new HttpMessage(getId(), message);
		msg.setExchangeName(exchangeName);
		msg.setRoutingKey(routingKey);
		sendMessage(session, msg);
	}

	void sendMessage(String exchangeName, String routingKey, Message message) throws IOException {
		sendMessage(openOrFindSession(exchangeName, routingKey),
				exchangeName, routingKey, message);
	}
	
	void sendMessage(Session session, HttpMessage message) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(SerializationUtils.serializeObject(message));
		session.getBasicRemote().sendBinary(buf);
	}
	
	protected Session openOrFindSession(String applicationName, String routingKey) throws IOException {
		Session session = sessions.get(applicationName
				+ (routingKey == null ? "" : "|" + routingKey));
		if(session == null) {

			//WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			//WebSocketContainer container = JettyClientContainerProvider.getWebSocketContainer();
			ClientContainer container = new ClientContainer();
			try {
				container.start();
			} catch (Exception e) {
				throw new IOException(e);
			}
			try {
				session = container.connectToServer(MessagingWebSocket.class, URI.create("ws://" + getIpAddress() + ":" + getPort() + "/messaging-http"
						+ "?exchange=" + URLEncoder.encode(applicationName, "UTF-8")
						+ (routingKey == null ? "" : "&routing=" + URLEncoder.encode(routingKey, "UTF-8"))));
				sessions.put(applicationName + (routingKey == null ? "" : "|" + routingKey), session);
			} catch (DeploymentException e) {
				throw new IOException(e);
			}
		}
		return session;
	}
	
	protected void closeSession(String applicationName, String scopedInstancePath) throws IOException {
		Session sess = sessions.remove(applicationName);
		if(sess != null) sess.close();
	}
	
	@Override
	public final Map<String, String> getConfiguration() {
		final Map<String, String> configuration = new LinkedHashMap<>();
		configuration.put(MessagingConstants.MESSAGING_TYPE_PROPERTY, HttpConstants.HTTP_FACTORY_TYPE);
		configuration.put(HttpConstants.HTTP_SERVER_IP, this.ipAddress);
		configuration.put(HttpConstants.HTTP_SERVER_PORT, this.port);
		return Collections.unmodifiableMap(configuration);
	}
	
	public final String getIpAddress() { return this.ipAddress; }
	public final String getPort() { return this.port; }
	public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
	public void setPort(String port) { this.port = port; }

	public abstract void addMessageToMQ(Message message);
}
