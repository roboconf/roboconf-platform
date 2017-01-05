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

package net.roboconf.messaging.http.internal;

import static net.roboconf.messaging.http.HttpConstants.DEFAULT_IP;
import static net.roboconf.messaging.http.HttpConstants.HTTP_SERVER_IP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.AbstractRoutingClient.RoutingContext;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.internal.clients.HttpAgentClient;
import net.roboconf.messaging.http.internal.clients.HttpDmClient;
import net.roboconf.messaging.http.internal.sockets.DmWebSocketServlet;

import org.eclipse.jetty.websocket.api.Session;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

/**
 * Messaging client factory for HTTP.
 * <p>
 * In this factory, there is a singleton instance of the client for
 * the DM. This is because of the web sockets. We do not want to lost
 * and/or confuse message routing. So, there is no reconfiguration for
 * this client and we keep a single instance.
 * </p>
 *
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class HttpClientFactory implements IMessagingClientFactory {

	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class HttpRoutingContext extends RoutingContext {
		public final Map<String,Session> ctxToSession = new ConcurrentHashMap<> ();
	}

	// The created CLIENTS.
	// References to the CLIENTS are *weak*, so we never prevent their garbage collection.
	final Set<HttpAgentClient> agentClients = Collections.newSetFromMap( new WeakHashMap<HttpAgentClient,Boolean> ());

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final HttpRoutingContext routingContext = new HttpRoutingContext();
	private final HttpDmClient dmClient = new HttpDmClient( this.routingContext );

	// Injected by iPojo
	BundleContext bundleContext;
	HttpService httpService;

	String httpServerIp;
	int httpPort;



	/**
	 * Constructor.
	 */
	public HttpClientFactory() {
		// nothing
	}


	/**
	 * Constructor with the bundle context.
	 * <p>
	 * This constructor is automatically invoked by iPojo.
	 * </p>
	 *
	 * @param context
	 */
	public HttpClientFactory( BundleContext bundleContext ) {
		this.bundleContext = bundleContext;
	}


	// Getters and Setters

	public synchronized void setHttpServerIp( final String serverIp ) {
		this.httpServerIp = serverIp;
		this.dmClient.setHttpServerIp( serverIp );
		this.logger.finer( "Server IP set to " + this.httpServerIp );
	}


	public synchronized void setHttpPort( final int port ) {
		this.httpPort = port;
		this.dmClient.setHttpPort( port );
		this.logger.finer( "Server port set to " + this.httpPort );
	}


	public HttpDmClient getDmClient() {
		return this.dmClient;
	}


	// iPojo methods


	/**
	 * The method to use when all the dependencies are resolved.
	 * <p>
	 * It means iPojo guarantees that both the manager and the HTTP
	 * service are not null.
	 * </p>
	 *
	 * @throws Exception
	 */
	public void start() throws Exception {

		// Is the DM part of the distribution?
		boolean found = false;
		for( Bundle b : this.bundleContext.getBundles()) {
			if( "net.roboconf.dm".equals( b.getSymbolicName())) {
				found = true;
				break;
			}
		}

		// If we are on an agent, we have nothing to do.
		// Otherwise, we must register a servlet.
		if( found ) {
			this.logger.fine( "iPojo registers a servlet for HTTP messaging." );

			Hashtable<String,String> initParams = new Hashtable<String,String> ();
			initParams.put( "servlet-name", "Roboconf DM (HTTP messaging)" );

			DmWebSocketServlet messagingServlet = new DmWebSocketServlet( this );
			this.httpService.registerServlet( HttpConstants.DM_SOCKET_PATH, messagingServlet, initParams, null );

		} else {
			this.logger.warning( "Roboconf's DM bundle was not found. No servlet will be registered." );
		}
	}


	/**
	 * Stops all the agent clients.
	 * <p>
	 * Invoked by iPojo.
	 * </p>
	 */
	public void stop() {
		this.logger.fine( "iPojo unregisters a servlet for HTTP messaging." );
		resetClients( true );
	}


	/**
	 * Stops all the clients (agents and DM).
	 * <p>
	 * Mostly for tests.
	 * </p>
	 */
	void stopAll() {

		try {
			this.dmClient.closeConnection();
			stop();

		} catch( Throwable t ) {
			this.logger.warning( "An error occurred while closing the connection of the DM client." );
			Utils.logException( this.logger, new RuntimeException( t ));
		}
	}


	@Override
	public IMessagingClient createClient( ReconfigurableClient<?> parent ) {

		this.logger.fine( "Creating a new HTTP client with owner = " + parent.getOwnerKind());
		IMessagingClient client;
		if( parent.getOwnerKind() == RecipientKind.DM ) {
			client = this.dmClient;

		} else {
			synchronized( this ) {
				client = new HttpAgentClient( parent, this.httpServerIp, this.httpPort );
			}

			this.agentClients.add((HttpAgentClient) client);
		}

		return client;
	}


	@Override
	public String getType() {
		return HttpConstants.FACTORY_HTTP;
	}


	@Override
	public boolean setConfiguration( final Map<String, String> configuration ) {

		boolean valid = HttpConstants.FACTORY_HTTP.equals( configuration.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
		if( valid ) {
			boolean hasChanged = false;

			// Get the new values
			String ip = Utils.getValue( configuration, HTTP_SERVER_IP, DEFAULT_IP );
			String portAS = configuration.get( HttpConstants.HTTP_SERVER_PORT );
			int port = portAS == null ? HttpConstants.DEFAULT_PORT : Integer.parseInt( portAS );

			// Avoid unnecessary (and potentially problematic) reconfiguration if nothing has changed.
			// First we detect for changes, and set the parameters accordingly.
			synchronized( this ) {

				if( ! Objects.equals( this.httpServerIp, ip )) {
					this.httpServerIp = ip;
					hasChanged = true;
				}

				if( this.httpPort != port ) {
					this.httpPort = port;
					hasChanged = true;
				}
			}

			// Then, if changes has occurred, we reconfigure the factory. This will invalidate every created client.
			// Otherwise, if nothing has changed, we do nothing. Thus we avoid invalidating clients uselessly, and
			// prevent any message loss.
			if( hasChanged )
				reconfigure();
		}

		return valid;
	}


	public void reconfigure() {
		this.logger.fine( "HTTP clients are about to be reconfigured." );
		resetClients( false );
	}


	/**
	 * Closes messaging clients or requests a replacement to the reconfigurable client.
	 * @param shutdown true to close, false to request...
	 */
	private void resetClients( boolean shutdown ) {

		// Only agent clients need to be reconfigured.
		// Make fresh snapshots of the CLIENTS, as we don't want to reconfigure them while holding the lock.
		final List<HttpAgentClient> clients;
		synchronized( this ) {

			// Get the snapshot.
			clients = new ArrayList<>( this.agentClients );

			// Remove the clients, new ones will be created if necessary.
			this.agentClients.clear();
		}

		// Now reconfigure all the CLIENTS.
		for( HttpAgentClient client : clients ) {
			try {
				final ReconfigurableClient<?> reconfigurable = client.getReconfigurableClient();
				if (shutdown)
					reconfigurable.closeConnection();
				else
					reconfigurable.switchMessagingType( HttpConstants.FACTORY_HTTP );

			} catch( Throwable t ) {
				// Warn but continue to reconfigure the next CLIENTS!
				this.logger.warning( "A client has thrown an exception on reconfiguration: " + client );
				Utils.logException( this.logger, new RuntimeException( t ));
			}
		}
	}
}
