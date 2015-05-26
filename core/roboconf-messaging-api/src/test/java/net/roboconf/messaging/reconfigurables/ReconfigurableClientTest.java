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

package net.roboconf.messaging.reconfigurables;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.internal.client.test.TestClientDm;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.processors.AbstractMessageProcessorTest.EmptyTestDmMessageProcessor;

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
	public void testInvalidFactory_dm() throws Exception {

		// The internal client will be null.
		// But still, there will be no NPE or other exception.
		ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.switchMessagingType( null );
		client.openConnection();
	}


	@Test
	public void testInvalidFactory_agent() throws Exception {

		// The internal client will be null.
		// But still, there will be no NPE or other exception.
		ReconfigurableClientAgent client = new ReconfigurableClientAgent();
		client.switchMessagingType( null );
		client.openConnection();
	}


	@Test
	public void testDm() throws Exception {

		// The messaging client is never null
		ReconfigurableClientDm client = new ReconfigurableClientDm();
		Assert.assertNotNull( client.getMessagingClient());

		// Invoke other method, no matter in which order
		client.setMessageQueue( new LinkedBlockingQueue<Message> ());
		Assert.assertFalse( client.hasValidClient());
		Assert.assertFalse( client.isConnected());

		client.deleteMessagingServerArtifacts( new Application( "app", new ApplicationTemplate()));
		client.propagateAgentTermination( null, null );
	}


	@Test
	public void testAgent() throws Exception {

		// The messaging client is never null
		ReconfigurableClientAgent client = new ReconfigurableClientAgent();
		Assert.assertNotNull( client.getMessagingClient());

		// Invoke other method, no matter in which order
		client.setMessageQueue( new LinkedBlockingQueue<Message> ());
		Assert.assertFalse( client.hasValidClient());
		Assert.assertFalse( client.isConnected());
	}


	@Test( expected = IllegalArgumentException.class )
	public void testAssociateMessageProcessor_exception() {

		ReconfigurableClientDm client = new ReconfigurableClientDm();
		Assert.assertFalse( client.hasValidClient());

		EmptyTestDmMessageProcessor processor = new EmptyTestDmMessageProcessor();
		try {
			client.associateMessageProcessor( processor );

		} catch( Throwable t ) {
			Assert.fail( "No exception was expected here" );
		}

		client.associateMessageProcessor( processor );
	}
}