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

package net.roboconf.integration.tests.dm;

import java.net.URI;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer;
import org.ops4j.pax.exam.spi.PaxExamRuntime;

import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.dm.probes.DmTest;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * This test verifies that a client can interact with the DM's websocket.
 * @author Vincent Zurczak - Linagora
 */
public class WebSocketTest extends DmTest {

	@Test
	public void run() throws Exception {

		Assume.assumeTrue( RabbitMqTestUtils.checkRabbitMqIsRunning());

		// Prepare to run an agent distribution
		Option[] options = super.config();
		ExamSystem system = PaxExamRuntime.createServerSystem( options );
		TestContainer container = PaxExamRuntime.createContainer( system );
		Assert.assertEquals( KarafTestContainer.class, container.getClass());

		try {
			// Start the agent's distribution... and wait... :(
			container.start();
			ItUtils.waitForDmRestServices( getCurrentPort());

			// Try to connect to our web socket.
			WebSocketClient client = new WebSocketClient();
			TestWebsocket socket = new TestWebsocket();
			try {
				client.start();
				URI echoUri = new URI( "ws://localhost:" + getCurrentPort() + "/roboconf-dm-websocket" );
				ClientUpgradeRequest request = new ClientUpgradeRequest();
				client.connect( socket, echoUri, request );

				// Wait more or less (Travis builds with Java 8 may need it).
				for( int i=0; i<10; i++ ) {
					Thread.sleep( 2000 );
					if( socket.wasConnected )
						break;
				}

			} finally {
				client.stop();
			}

			// Did the connection work?
			Assert.assertTrue( socket.wasConnected );

		} finally {
			container.stop();
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static final class TestWebsocket extends WebSocketAdapter {
		private boolean wasConnected = false;

		@Override
		public void onWebSocketConnect( Session sess ) {
			this.wasConnected = true;
		}
	}
}
