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

package net.roboconf.agent.monitoring.internal.nagios;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class NagiosHandlerTest {

	private static final String RESULT = "ok";
	private static final Level LOG_LEVEL = Level.FINE;

	private static final String EVENT_NAME = "whatever";
	private static final String APP_NAME = "app";
	private static final String SCOPED_INSTANCE_PATH = "/root";

	private final Logger logger = Logger.getLogger( getClass().getName());


	@Test
	public void testConstructor() {

		final String query = "ok\nok";
		final String url = "http://192.168.1.18";

		NagiosHandler handler = new NagiosHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, query );

		Assert.assertEquals( query, handler.nagiosInstructions );
		Assert.assertNull( handler.host );
		Assert.assertEquals( -1, handler.port );

		handler = new NagiosHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME,
				NagiosHandler.NAGIOS_CONFIG + " " + url + "\n" + query );

		Assert.assertEquals( query, handler.nagiosInstructions );
		Assert.assertEquals( url, handler.host );
		Assert.assertEquals( -1, handler.port );

		handler = new NagiosHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME,
				NagiosHandler.NAGIOS_CONFIG + " " + url + ":1717\n" + query );

		Assert.assertEquals( query, handler.nagiosInstructions );
		Assert.assertEquals( url, handler.host );
		Assert.assertEquals( 1717, handler.port );

		handler = new NagiosHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, NagiosHandler.NAGIOS_CONFIG );

		Assert.assertEquals( "", handler.nagiosInstructions );
		Assert.assertEquals( "", handler.host );
		Assert.assertEquals( -1, handler.port );
	}


	@Test
	public void testProcess_noConnection() throws Exception {

		NagiosHandler handler = new NagiosHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "" );
		Assert.assertNull( handler.process());
	}


	@Test
	public void testProcess_invalidHost() throws Exception {

		NagiosHandler handler = new NagiosHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, "" );
		handler.host = "my-unknown-host-for-tests";
		Assert.assertNull( handler.process());
	}


	@Test
	public void testProcess_noColumns() throws Exception {

		MsgNotifAutonomic msg = queryMockedNagios( "whatever" );
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( SCOPED_INSTANCE_PATH, msg.getScopedInstancePath());
		Assert.assertEquals( RESULT, msg.getEventInfo());
	}


	@Test
	public void testProcess_withOneColumn() throws Exception {

		MsgNotifAutonomic msg = queryMockedNagios( "Columns: host_name" );
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( SCOPED_INSTANCE_PATH, msg.getScopedInstancePath());
		Assert.assertEquals( "host_name\n" + RESULT, msg.getEventInfo());
	}


	@Test
	public void testProcess_withColumns() throws Exception {

		MsgNotifAutonomic msg = queryMockedNagios( "Columns: host_name acknowledged" );
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( SCOPED_INSTANCE_PATH, msg.getScopedInstancePath());
		Assert.assertEquals( "host_name;acknowledged\n" + RESULT, msg.getEventInfo());
	}



	private MsgNotifAutonomic queryMockedNagios( final String nagiosQuery ) throws Exception {
		final int port = 50002;

		// Then, we mock Nagios by running a simple socket server.
		// This server will only handle ONE connection.
		Thread thread = new Thread() {
			@Override
			public void run() {

				ServerSocket socketServer = null;
				try {
					try {
						NagiosHandlerTest.this.logger.log( LOG_LEVEL, "The socket server is about to start." );
						socketServer = new ServerSocket( port );
						NagiosHandlerTest.this.logger.log( LOG_LEVEL, "The socket server was started." );
						Socket socket = socketServer.accept();
						NagiosHandlerTest.this.logger.log( LOG_LEVEL, "The socket server received a connection." );

						PrintWriter writer = new PrintWriter( new OutputStreamWriter( socket.getOutputStream(), StandardCharsets.UTF_8 ), false );
						writer.print( RESULT );
						writer.flush();
						socket.shutdownOutput();
						socket.close();

					} finally {
						if( socketServer != null )
							socketServer.close();

						NagiosHandlerTest.this.logger.log( LOG_LEVEL, "The socket server was closed." );
					}

				} catch( IOException e ) {
					// nothing
				}
			}
		};

		// Start our server
		thread.start();
		Thread.sleep( 500 );

		// Then, prepare our client.
		NagiosHandler handler = new NagiosHandler();
		handler.setAgentId( APP_NAME, SCOPED_INSTANCE_PATH );
		handler.reset( null, EVENT_NAME, nagiosQuery );
		handler.port = port;
		MsgNotifAutonomic msg = handler.process();

		// Wait for the server to die.
		thread.join();
		return msg;
	}
}
