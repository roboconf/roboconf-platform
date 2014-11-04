/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.messaging.reconfigurables;

import java.io.IOException;

import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.internal.client.test.TestClientDm;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReconfigurableClientTest {

	@Test
	public void testCloseConnection() throws Exception {

		IDmClient client = new TestClientDm();
		Assert.assertFalse( client.isConnected());
		ReconfigurableClient.closeConnection( client, "" );

		client = new TestClientDm();
		client.openConnection();
		Assert.assertTrue( client.isConnected());
		ReconfigurableClient.closeConnection( client, "" );
		Assert.assertFalse( client.isConnected());

		client = new TestClientDm() {
			@Override
			public void closeConnection() throws IOException {
				throw new IOException( "For test purpose" );
			}
		};

		client.openConnection();
		Assert.assertTrue( client.isConnected());
		ReconfigurableClient.closeConnection( client, "" );
	}


	@Test
	public void testInvalidFactory() throws Exception {

		// The internal client will be null.
		// But still, there will be no NPE or other exception.
		ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.switchMessagingClient( null, null, null, null );
		client.openConnection();
	}
}
