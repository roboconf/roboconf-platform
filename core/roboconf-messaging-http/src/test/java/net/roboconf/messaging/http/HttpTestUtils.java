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

package net.roboconf.messaging.http;

import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.http.internal.HttpAgentClient;
import net.roboconf.messaging.http.internal.HttpDmClient;
import net.roboconf.messaging.http.internal.MessagingWebSocket;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public abstract class HttpTestUtils {

	static WebServer WEBSERVER = new WebServer();

	/**
	 * Empty constructor.
	 */
	private HttpTestUtils() {
		// nothing
	}

	/**
	 * Get the delegate messaging client of a reconfigurable messaging client.
	 *
	 * @param reconfigurable the reconfigurable messaging client.
	 * @param type           the expected type of the internal messaging client.
	 * @param <T>            the expected type of the internal messaging client.
	 * @return the internal messaging client, or {@code null} if it is not defined, or has the wrong type.
	 * @throws IllegalAccessException if the internal messaging client could not be read.
	 */
	public static HttpDmClient getMessagingClientDm( ReconfigurableClientDm reconfigurable )
			throws IllegalAccessException {
		return TestUtils.getInternalField(reconfigurable, "messagingClient", HttpDmClient.class);
	}

	/**
	 * Get the delegate messaging client of a reconfigurable messaging client.
	 *
	 * @param reconfigurable the reconfigurable messaging client.
	 * @param type           the expected type of the internal messaging client.
	 * @param <T>            the expected type of the internal messaging client.
	 * @return the internal messaging client, or {@code null} if it is not defined, or has the wrong type.
	 * @throws IllegalAccessException if the internal messaging client could not be read.
	 */
	public static HttpAgentClient getMessagingClientAgent( ReconfigurableClientAgent reconfigurable )
			throws IllegalAccessException {
		return TestUtils.getInternalField(reconfigurable, "messagingClient", HttpAgentClient.class);
	}

	/**
	 * Run a jetty Web Server with instantiated Messaging WebSocket
	 */
	public static void runWebServer() {
		new Thread(WEBSERVER).start();
	}
	
	public static void stopWebServer() {
		WEBSERVER.stop();
	}
}

class WebServer implements Runnable {

	Server server;

	@Override
	public void run() {
		try {
			server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(8080);
			server.addConnector(connector);

			// Setup the basic application "context" for this application at "/"
			// This is also known as the handler tree (in jetty speak)
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			server.setHandler(context);

			// Initialize javax.websocket layer
			ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

			// Add WebSocket endpoint to javax.websocket layer
			wscontainer.addEndpoint(MessagingWebSocket.class);
			server.start();
			server.dump(System.err);
			server.join();
		} catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public void stop() {
		try {
			server.stop();
			server.destroy();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
