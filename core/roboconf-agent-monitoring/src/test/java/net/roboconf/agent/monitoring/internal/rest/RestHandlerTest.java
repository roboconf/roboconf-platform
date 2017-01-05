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

package net.roboconf.agent.monitoring.internal.rest;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.agent.monitoring.internal.rest.RestHandler.LocalHostnameVerifier;
import net.roboconf.agent.monitoring.internal.rest.RestHandler.LocalX509TrustManager;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class RestHandlerTest {

	private static final Level LOG_LEVEL = Level.FINE;

	private static final String EVENT_NAME = "whatever";
	private static final String APP_NAME = "app";
	private static final String SCOPED_INSTANCE_PATH = "/root";

	private final Logger logger = Logger.getLogger( getClass().getName());


	@Test
	public void testConstructor() {

		final String url = "http://localhost:1234";
		final String query = "check " + url + " that ";

		String filter = "lag = 0";
		RestHandler handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertEquals( url, handler.url );
		Assert.assertEquals( "lag", handler.conditionParameter );
		Assert.assertEquals( "0", handler.conditionThreshold );
		Assert.assertEquals( "=", handler.conditionOperator );

		filter = "lag == 0";
		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertEquals( url, handler.url );
		Assert.assertEquals( "lag", handler.conditionParameter );
		Assert.assertEquals( "0", handler.conditionThreshold );
		Assert.assertEquals( "==", handler.conditionOperator );

		filter = "toto >= 10.0";
		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertEquals( url, handler.url );
		Assert.assertEquals( "toto", handler.conditionParameter );
		Assert.assertEquals( "10.0", handler.conditionThreshold );
		Assert.assertEquals( ">=", handler.conditionOperator );

		filter = " titi < 	toto ";
		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertEquals( url, handler.url );
		Assert.assertEquals( "titi", handler.conditionParameter );
		Assert.assertEquals( "toto", handler.conditionThreshold );
		Assert.assertEquals( "<", handler.conditionOperator );

		filter = "something == 'this'";
		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertEquals( url, handler.url );
		Assert.assertEquals( "something", handler.conditionParameter );
		Assert.assertEquals( "'this'", handler.conditionThreshold );
		Assert.assertEquals( "==", handler.conditionOperator );

		filter = "lag <= 0";
		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertEquals( url, handler.url );
		Assert.assertEquals( "lag", handler.conditionParameter );
		Assert.assertEquals( "0", handler.conditionThreshold );
		Assert.assertEquals( "<=", handler.conditionOperator );

		filter = "toto > 11235";
		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertEquals( url, handler.url );
		Assert.assertEquals( "toto", handler.conditionParameter );
		Assert.assertEquals( "11235", handler.conditionThreshold );
		Assert.assertEquals( ">", handler.conditionOperator );

		filter = "toto >== 11235";
		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertNull( handler.url );
		Assert.assertNull( handler.conditionParameter );
		Assert.assertNull( handler.conditionThreshold );
		Assert.assertNull( handler.conditionOperator );

		filter = "toto>11235";
		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query + filter );

		Assert.assertNull( handler.url );
		Assert.assertNull( handler.conditionParameter );
		Assert.assertNull( handler.conditionThreshold );
		Assert.assertNull( handler.conditionOperator );
	}


	@Test
	public void testEvalCondition() throws Exception {

		RestHandler handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "check url that lag = 0" );

		Map<String,String> map = new HashMap<String,String> ();
		Assert.assertFalse( handler.evalCondition( map ));

		map.put( "undefined", "1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "-1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0.0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();


		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "check url that lag >= 0" );

		map.put( "lag", "1.0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "-1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0.0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();

		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "check url that lag == 0" );

		map.put( "lag", "1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "-1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "yes" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0.0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();

		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "check url that lag > 0" );

		map.put( "lag", "1.0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "-1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "check url that lag <= 0" );

		map.put( "lag", "1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "-1.0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0.0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();

		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "check url that lag < 0" );

		map.put( "lag", "1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "-1.0" );
		Assert.assertTrue( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "yes" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "check url that lag == yes" );

		map.put( "lag", "1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "-1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "yes" );
		Assert.assertTrue( handler.evalCondition( map ));

		handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "check url that lag =       yes" );

		map.put( "lag", "1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "-1.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "0.0" );
		Assert.assertFalse( handler.evalCondition( map ));
		map.clear();

		map.put( "lag", "yes" );
		Assert.assertTrue( handler.evalCondition( map ));
	}


	@Test
	public void testLocalClasses() {

		LocalX509TrustManager trustManager = new LocalX509TrustManager();
		Assert.assertNull( trustManager.getAcceptedIssuers());
		trustManager.checkClientTrusted( null, null );
		trustManager.checkServerTrusted( null, null );

		Assert.assertFalse( new LocalHostnameVerifier().verify( null, null ));
	}


	@Test
	public void testProcess_http() throws Exception {
		final String url = "http://localhost";

		String expected = "\"lag\":0";
		MsgNotifAutonomic msg = queryMockedHttpServer( url, expected );
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( SCOPED_INSTANCE_PATH, msg.getScopedInstancePath());
		Assert.assertEquals( expected, msg.getEventInfo());

		expected = "{\"lag\":100}";
		msg = queryMockedHttpServer( url, expected );
		Assert.assertNull( msg );
	}


	@Test
	public void testProcess_http_invalidResult() throws Exception {
		final String url = "http://localhost";

		String expected = "{\"lag\":100:80}";
		MsgNotifAutonomic msg = queryMockedHttpServer( url, expected );
		Assert.assertNull( msg );
	}


	@Test
	public void testProcess_invalidHttp() throws Exception {

		final String query = "Check http://localhost:8985 that lag = 0";

		RestHandler handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query );

		Assert.assertNull( handler.process());
	}


	@Test
	public void testProcess_invalidHttps() throws Exception {

		final String query = "Check https://localhost:8985 that lag = 0";

		RestHandler handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query );

		Assert.assertNull( handler.process());
	}


	@Test
	public void testProcess_invalidQuery() throws Exception {

		final String query = "Check https://localhost:8985 that lag";

		RestHandler handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query );

		Assert.assertNull( handler.process());
	}


	private MsgNotifAutonomic queryMockedHttpServer( String url, String result ) throws Exception {

		int port = 50080;
		final String query = "Check " + url + ":" + port + " that lag = 0";

		// Mock a HTTP server by running a simple socket server.
		// This server will only handle one connection.
		Thread thread = new WebServerThread(RestHandlerTest.this.logger, port, result);

		// Start our server
		thread.start();
		Thread.sleep( 500 );

		// Then, prepare our client.
		RestHandler handler = new RestHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query );

		MsgNotifAutonomic msg = handler.process();

		// Wait for the server to die.
		thread.join();
		return msg;
	}


	/**
	 * @author Pierre-Yves Gibello - Linagora
	 */
	private static class WebServerThread extends Thread {

		private final Logger logger;
		private final int port;
		private final String result;

		public WebServerThread(Logger logger, int port, String result) {
			this.logger = logger;
			this.port = port;
			this.result = result;
		}

		@Override
		public void run() {

			ServerSocket socketServer = null;
			try {
				try {
					this.logger.log( LOG_LEVEL, "The socket server is about to start." );
					socketServer = new ServerSocket(this.port);
					this.logger.log( LOG_LEVEL, "The socket server was started." );
					Socket socket = socketServer.accept();
					this.logger.log( LOG_LEVEL, "The socket server received a connection." );

					PrintWriter writer = new PrintWriter( new OutputStreamWriter( socket.getOutputStream(), StandardCharsets.UTF_8 ), false );
					writer.print("HTTP/1.1 200 OK\n\n" + this.result);
					writer.flush();
					socket.shutdownOutput();
					socket.close();

				} finally {
					if( socketServer != null )
						socketServer.close();

					this.logger.log( LOG_LEVEL, "The socket server was closed." );
				}

			} catch( IOException e ) {
				// nothing
			}
		}
	}
}
