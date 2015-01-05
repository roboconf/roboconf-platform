/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class RestHandlerTest {

	private static final Level LOG_LEVEL = Level.FINE;

	private static final String EVENT_NAME = "whatever";
	private static final String APP_NAME = "app";
	private static final String ROOT_INSTANCE_NAME = "root";

	private final Logger logger = Logger.getLogger( getClass().getName());


	@Test
	public void testConstructor() {

		final String url = "http://localhost:1234";
		final String filter = "lag=0";
		final String query = "url:" + url + "\nfilter:" + filter;

		RestHandler handler = new RestHandler(EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, query);

		Assert.assertEquals(url, handler.getUrl());
		Assert.assertEquals(filter, handler.getCondition());
	}


	@SuppressWarnings("serial")
	@Test
	public void testEvalCondition() throws Exception {

		RestHandler handler = new RestHandler(EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, "url:nothing\nfilter:lag=0");
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>())); // Empty map, always false
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("undefined", 1.0); } })); // Always false
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 1.0); } }));
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("lag", -1.0); } }));
		Assert.assertTrue(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 0.0); } }));
		
		handler = new RestHandler(EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, "url:nothing\nfilter:lag>=0");
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("lag", -1.0); } }));
		Assert.assertTrue(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 1.0); } }));
		Assert.assertTrue(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 0.0); } }));
		
		handler = new RestHandler(EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, "url:nothing\nfilter:lag>0");
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("lag", -1.0); } }));
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 0.0); } }));
		Assert.assertTrue(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 1.0); } }));
		
		handler = new RestHandler(EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, "url:nothing\nfilter:lag<=0");
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 1.0); } }));
		Assert.assertTrue(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 0.0); } }));
		Assert.assertTrue(handler.evalCondition(new HashMap<String, Double>() { { put("lag", -1.0); } }));
		
		handler = new RestHandler(EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, "url:nothing\nfilter:lag<0");
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 1.0); } }));
		Assert.assertFalse(handler.evalCondition(new HashMap<String, Double>() { { put("lag", 0.0); } }));
		Assert.assertTrue(handler.evalCondition(new HashMap<String, Double>() { { put("lag", -1.0); } }));
	}

	@Test
	public void testProcess() throws Exception {

		String expected = "{\"lag\":0}";
		MsgNotifAutonomic msg = queryMockedHttpServer(expected);
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( ROOT_INSTANCE_NAME, msg.getRootInstanceName());
		Assert.assertEquals(expected, msg.getEventInfo());
		
		expected = "{\"lag\":100}";
		msg = queryMockedHttpServer(expected);
		Assert.assertNull(msg);
	}

	private MsgNotifAutonomic queryMockedHttpServer(String result) throws Exception {

		int port = 50002;		
		final String query = "url:http://localhost:50002\nfilter:lag=0";
		
		// Mock http server by running a simple socket server.
		// This server will only handle OSystem.out.println(msg.getEventInfo());NE connection.
		Thread thread = new WebServerThread(RestHandlerTest.this.logger, port, result);

		// Start our server
		thread.start();
		Thread.sleep(500);

		// Then, prepare our client.
		RestHandler handler = new RestHandler(EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, query);
		MsgNotifAutonomic msg = handler.process();

		// Wait for the server to die.
		thread.join();
		return msg;
	}
	
	private static class WebServerThread extends Thread {
		
		private Logger logger;
		private int port;
		private String result;

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
					writer.print("HTTP/1.1 200 OK\n\n" + result);
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
