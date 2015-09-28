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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.factory.MessagingClientFactory;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.http.HttpConstants;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Updated;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.http.HttpService;

/**
 * Messaging client factory for HTTP.
 * @author Pierre-Yves Gibello - Linagora
 */
@Component(
		name = "roboconf-messaging-client-factory-http",
		publicFactory = false,
		managedservice = "net.roboconf.messaging.http"
)
@Provides(
		specifications = MessagingClientFactory.class,
		properties = @StaticServiceProperty(
				name = MessagingClientFactory.MESSAGING_TYPE_PROPERTY,
				type = "string",
				value = HttpConstants.HTTP_FACTORY_TYPE
))
@Instantiate(name = "Roboconf Http Messaging Client Factory")
public class HttpClientFactory implements MessagingClientFactory {

	// The created CLIENTS.
	// References to the CLIENTS are *weak*, so we never prevent their garbage collection.
	private final static Set<HttpClient> CLIENTS = Collections.newSetFromMap(new WeakHashMap<HttpClient, Boolean>());

	@Requires
	private HttpService http;

	// The logger
	private final Logger logger = Logger.getLogger( getClass().getName());

	private String httpServerIp;
	private String httpPort;

	@Property(name = HttpConstants.HTTP_SERVER_IP, value = HttpConstants.DEFAULT_IP)
	public synchronized void setHttpServerIp( final String serverIp ) {
		this.httpServerIp = serverIp;
		this.logger.finer("Server IP set to " + httpServerIp);
	}
	
	@Property(name = HttpConstants.HTTP_SERVER_PORT, value = HttpConstants.DEFAULT_PORT)
	public synchronized void setHttpPort( final String port ) {
		this.httpPort = port;
		this.logger.finer("Server port set to " + httpPort);
	}

	/**
	 * The method to use when all the dependencies are resolved.
	 * <p>
	 * It means iPojo guarantees that both the manager and the HTTP
	 * service are not null.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Validate
	public void start() throws Exception {
		this.logger.fine( "iPojo registers messaging-http web socket servlet." );

		// Register the web socket
		//TODO: Should be done on the DM only !!
		Hashtable<String,String> initParams = new Hashtable<String,String> ();
		initParams.put( "servlet-name", "Roboconf (agent) messaging-http websocket" );

		try {
			MessagingWebSocketServlet messagingServlet = new MessagingWebSocketServlet();
			this.http.registerServlet( "/messaging-http", messagingServlet, initParams, null );
		} catch(Throwable e) {
			e.printStackTrace(System.err);
			throw e;
		}
	}

	@Invalidate
	public void stop() {
		this.logger.fine("Stopping HTTP messaging client factory.");
		resetClients(true);
	}

	@Override
	public synchronized IDmClient createDmClient( final ReconfigurableClientDm parent ) {
		this.logger.finer("Creating a new HTTP messaging DM client.");

		// Create the DM client, with the current list of the agent addresses.
		final HttpDmClient client = new HttpDmClient(parent, this.httpServerIp, this.httpPort);

		// Keep track of the client, so it gets notified of the agent addresses adding/removals.
		CLIENTS.add(client);

		this.logger.finer("HTTP messaging DM client created.");
		return client;
	}

	@Override
	public synchronized IAgentClient createAgentClient( final ReconfigurableClientAgent parent ) {
		this.logger.finer("Creating a new HTTP messaging Agent client.");

		HttpAgentClient client = null;
		try {
			client = new HttpAgentClient(parent, this.httpServerIp, this.httpPort);
			CLIENTS.add(client);
			this.logger.finer("HTTP messaging Agent client created.");
		} catch (DeploymentException | IOException | URISyntaxException e) {
			this.logger.finer("HTTP messaging Agent error: " + e);
		}

		return client;
	}

	@Override
	public String getType() {
		return HttpConstants.HTTP_FACTORY_TYPE;
	}

	@Override
	public boolean setConfiguration( final Map<String, String> configuration ) {
		return HttpConstants.HTTP_FACTORY_TYPE.equals(configuration.get(MESSAGING_TYPE_PROPERTY));
	}
	
	@Updated
	public void reconfigure() {
		// Set the properties for all created CLIENTS.
		resetClients(false);
	}

	public static Set<HttpClient> getHttpClients() {
		return CLIENTS;
	}

	private void resetClients(boolean shutdown) {

		// Make fresh snapshots of the CLIENTS, as we don't want to reconfigure them while holding the lock.
		final ArrayList<HttpClient> clients;
		synchronized (this) {
			clients = new ArrayList<>(CLIENTS);
		}

		// Now reconfigure all the CLIENTS.
		for (HttpClient client : clients) {
			try {
				final ReconfigurableClient<?> reconfigurable = client.getReconfigurableClient();
				if (reconfigurable != null) {
					if (shutdown) {
						reconfigurable.closeConnection();
					} else {
						reconfigurable.switchMessagingType(HttpConstants.HTTP_FACTORY_TYPE);
						client.setIpAddress(this.httpServerIp);
						client.setPort(this.httpPort);
					}
				}

			} catch (Throwable t) {
				// Warn but continue to reconfigure the next CLIENTS!
				this.logger.warning("A client has thrown an exception on reconfiguration: " + client);
				Utils.logException(this.logger, new RuntimeException(t));
			}
		}
	}

}
