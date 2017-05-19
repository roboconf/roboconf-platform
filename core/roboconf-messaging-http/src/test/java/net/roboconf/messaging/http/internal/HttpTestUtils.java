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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.internal.clients.HttpAgentClient;
import net.roboconf.messaging.http.internal.clients.HttpDmClient;
import net.roboconf.messaging.http.internal.sockets.DmWebSocketServlet;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public abstract class HttpTestUtils {

	static final int TEST_PORT = 9999;


	/**
	 * Empty constructor.
	 */
	private HttpTestUtils() {
		// nothing
	}


	/**
	 * Gets the delegate messaging client of a reconfigurable messaging client.
	 *
	 * @param reconfigurable the reconfigurable messaging client.
	 * @param type           the expected type of the internal messaging client.
	 * @param <T>            the expected type of the internal messaging client.
	 * @return the internal messaging client, or {@code null} if it is not defined, or has the wrong type.
	 * @throws IllegalAccessException if the internal messaging client could not be read.
	 */
	public static HttpDmClient getMessagingClientDm( ReconfigurableClientDm reconfigurable )
	throws IllegalAccessException {

		IMessagingClient wrapperClient = TestUtils.getInternalField( reconfigurable, "messagingClient", IMessagingClient.class );
		return TestUtils.getInternalField( wrapperClient, "messagingClient", HttpDmClient.class );
	}


	/**
	 * Gets the delegate messaging client of a reconfigurable messaging client.
	 *
	 * @param reconfigurable the reconfigurable messaging client.
	 * @param type           the expected type of the internal messaging client.
	 * @param <T>            the expected type of the internal messaging client.
	 * @return the internal messaging client, or {@code null} if it is not defined, or has the wrong type.
	 * @throws IllegalAccessException if the internal messaging client could not be read.
	 */
	public static HttpAgentClient getMessagingClientAgent( ReconfigurableClientAgent reconfigurable )
	throws IllegalAccessException {

		IMessagingClient wrapperClient = TestUtils.getInternalField( reconfigurable, "messagingClient", IMessagingClient.class );
		return TestUtils.getInternalField( wrapperClient, "messagingClient", HttpAgentClient.class );
	}


	/**
	 * @author Pierre-Yves Gibello - Linagora
	 */
	static class WebServer implements Runnable {

		Server server;
		boolean running;
		HttpClientFactory httpClientFactory;


		/**
		 * Constructor.
		 */
		public WebServer( HttpClientFactory httpClientFactory ) {
			this.httpClientFactory = httpClientFactory;
		}


		@Override
		public void run() {

			try {
				this.server = new Server();
				ServerConnector connector = new ServerConnector( this.server );
				connector.setPort( TEST_PORT );
				this.server.addConnector( connector );

				// Setup the basic application "context" for this application at "/"
				// This is also known as the handler tree (in jetty speak)
				ServletContextHandler context = new ServletContextHandler( ServletContextHandler.SESSIONS );
				context.setContextPath( "/" );
				context.addServlet( new ServletHolder( new DmWebSocketServlet( this.httpClientFactory )), HttpConstants.DM_SOCKET_PATH );

				this.server.setHandler( context );
				this.server.start();
				// this.server.dump( System.err );

				this.running = true;
				this.server.join();

			} catch( Exception e ) {
				e.printStackTrace( System.err );
			}
		}


		/**
		 * Stops the server.
		 */
		public void stop() {

			try {
				this.server.stop();
				this.server.destroy();

			} catch( Exception e ) {
				e.printStackTrace( System.err );
			}

			this.running = false;
		}


		/**
		 * @return true if the server is started
		 */
		public boolean isServerStarted() {
			return this.server.isStarted();
		}


		/**
		 * @return true if the server is stopped
		 */
		public boolean isServerStopped() {
			return this.server.isStopped();
		}


		/**
		 * @return true if the server is running (running is our own indicator)
		 */
		public boolean isRunning() {
			return this.running;
		}
	}
}
