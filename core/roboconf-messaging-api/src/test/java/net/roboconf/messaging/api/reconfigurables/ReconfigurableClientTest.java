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

package net.roboconf.messaging.api.reconfigurables;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.dismiss.DismissClientAgent;
import net.roboconf.messaging.api.internal.client.dismiss.DismissClientDm;
import net.roboconf.messaging.api.internal.client.test.TestClientAgent;
import net.roboconf.messaging.api.internal.client.test.TestClientDm;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.processors.AbstractMessageProcessor;
import net.roboconf.messaging.api.processors.AbstractMessageProcessorTest.EmptyTestDmMessageProcessor;
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

	@Test
	public void testFactorySwitchClientDm() {
		// Create the messaging client factory registry, and register the "foo" and "bar" dummy factories.
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory(new DummyMessagingClientFactory("foo"));
		registry.addMessagingClientFactory(new DummyMessagingClientFactory("bar"));

		// Create the client DM
		final ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.associateMessageProcessor(new AbstractMessageProcessor<IDmClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {

			}
		});
		client.setRegistry(registry);

		// Check initial state.
		Assert.assertNull(client.getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DismissClientDm);
		Assert.assertSame(registry, client.getRegistry());

		// Switch to foo!
		client.switchMessagingType("foo");
		Assert.assertEquals("foo", client.getMessagingType());
		Assert.assertEquals("foo", client.getMessagingClient().getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DummyMessagingDmClient);

		// Switch to bar!
		client.switchMessagingType("bar");
		Assert.assertEquals("bar", client.getMessagingType());
		Assert.assertEquals("bar", client.getMessagingClient().getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DummyMessagingDmClient);

		// Switch to null!
		client.switchMessagingType(null);
		Assert.assertNull(client.getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DismissClientDm);
	}

	@Test
	public void testFactorySwitchClientAgent() {
		// Create the messaging client factory registry, and register the "foo" and "bar" dummy factories.
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory(new DummyMessagingClientFactory("foo"));
		registry.addMessagingClientFactory(new DummyMessagingClientFactory("bar"));

		// Create the client DM
		final ReconfigurableClientAgent client = new ReconfigurableClientAgent();
		client.associateMessageProcessor(new AbstractMessageProcessor<IAgentClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {

			}
		});
		client.setRegistry(registry);

		// Check initial state.
		Assert.assertNull(client.getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DismissClientAgent);
		Assert.assertSame(registry, client.getRegistry());

		// Switch to foo!
		client.switchMessagingType("foo");
		Assert.assertEquals("foo", client.getMessagingType());
		Assert.assertEquals("foo", client.getMessagingClient().getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DummyMessagingAgentClient);

		// Switch to bar!
		client.switchMessagingType("bar");
		Assert.assertEquals("bar", client.getMessagingType());
		Assert.assertEquals("bar", client.getMessagingClient().getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DummyMessagingAgentClient);

		// Switch to null!
		client.switchMessagingType(null);
		Assert.assertNull(client.getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DismissClientAgent);
	}

	/**
	 * A stupid messaging client.
	 */
	private static class DummyMessagingClientFactory implements IMessagingClientFactory {

		/**
		 * The type of the messaging factory.
		 */
		final String type;

		/**
		 * Constructor.
		 *
		 * @param type the type of the messaging factory.
		 */
		DummyMessagingClientFactory( final String type ) {
			this.type = type;
		}

		@Override
		public String getType() {
			return this.type;
		}

		@Override
		public IDmClient createDmClient( final ReconfigurableClientDm parent ) {
			return new DummyMessagingDmClient(this.type);
		}

		@Override
		public IAgentClient createAgentClient( final ReconfigurableClientAgent parent ) {
			return new DummyMessagingAgentClient(this.type);
		}

		@Override
		public boolean setConfiguration( final Map<String, String> configuration ) {
			return this.type.equals(configuration.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		}
	}

	/**
	 * A dummy DM messaging client.
	 */
	private static class DummyMessagingDmClient extends TestClientDm {

		/**
		 * The type of the messaging client.
		 */
		final String type;

		/**
		 * Constructor.
		 *
		 * @param type the type of the messaging client.
		 */
		DummyMessagingDmClient( final String type ) {
			this.type = type;
		}

		@Override
		public String getMessagingType() {
			return this.type;
		}
	}

	/**
	 * A dummy Agent messaging client.
	 */
	private static class DummyMessagingAgentClient extends TestClientAgent {

		/**
		 * The type of the messaging client.
		 */
		final String type;

		/**
		 * Constructor.
		 *
		 * @param type the type of the messaging client.
		 */
		DummyMessagingAgentClient( final String type ) {
			this.type = type;
		}

		@Override
		public String getMessagingType() {
			return this.type;
		}
	}

}
