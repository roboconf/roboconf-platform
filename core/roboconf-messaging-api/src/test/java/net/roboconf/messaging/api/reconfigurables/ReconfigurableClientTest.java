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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.business.IAgentClient;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.dismiss.DismissClient;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.processors.AbstractMessageProcessorTest.EmptyTestDmMessageProcessor;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReconfigurableClientTest {

	@Test
	public void testCloseConnection() throws Exception {

		IMessagingClient client = new TestClient();
		Assert.assertFalse( client.isConnected());
		ReconfigurableClient.closeConnection( client, "" );

		client = new TestClient();
		client.openConnection();
		Assert.assertTrue( client.isConnected());
		ReconfigurableClient.closeConnection( client, "" );
		Assert.assertFalse( client.isConnected());

		client = new TestClient() {
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
		client.setApplicationName( "app" );
		client.setScopedInstancePath( "/root" );
		client.switchMessagingType( null );
		client.openConnection();
	}


	@Test
	public void testHasValidClient() {

		ReconfigurableClientAgent client = new ReconfigurableClientAgent();
		Assert.assertFalse( client.hasValidClient());
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
	public void testSetFactory() throws Exception {

		// Set a first factory
		ReconfigurableClientDm client = new ReconfigurableClientDm();
		final MessagingClientFactoryRegistry registry1 = new MessagingClientFactoryRegistry();

		ConcurrentLinkedQueue<?> listeners1 = TestUtils.getInternalField( registry1, "listeners", ConcurrentLinkedQueue.class );
		Assert.assertEquals( 0, listeners1.size());
		Assert.assertNull( TestUtils.getInternalField( client, "registry", MessagingClientFactoryRegistry.class ));

		client.setRegistry( registry1 );

		Assert.assertEquals( 1, listeners1.size());
		Assert.assertEquals( registry1, TestUtils.getInternalField( client, "registry", MessagingClientFactoryRegistry.class ));

		// Change the factory
		final MessagingClientFactoryRegistry registry2 = new MessagingClientFactoryRegistry();

		ConcurrentLinkedQueue<?> listeners2 = TestUtils.getInternalField( registry2, "listeners", ConcurrentLinkedQueue.class );
		Assert.assertEquals( 0, listeners2.size());

		client.setRegistry( registry2 );

		Assert.assertEquals( 0, listeners1.size());
		Assert.assertEquals( 1, listeners2.size());
		Assert.assertEquals( registry2, TestUtils.getInternalField( client, "registry", MessagingClientFactoryRegistry.class ));
	}


	@Test
	public void testRegistryCallbacks() {

		// Instead of creating a messaging client explicitly, we rely
		// on the registry callbacks. At runtime, this can happen
		// when we configured the DM or an agent with a messaging type that was not
		// yet deployed or available in the OSGi registry.
		ReconfigurableClientDm client = new ReconfigurableClientDm();
		MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		client.setRegistry( registry );

		client.associateMessageProcessor( new AbstractMessageProcessor<IDmClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.switchMessagingType("foo");
		Assert.assertEquals( DismissClient.class, client.getMessagingClient().getClass());

		// Make a new factory appear.
		DummyMessagingClientFactory factory = new DummyMessagingClientFactory("foo");
		registry.addMessagingClientFactory( factory );

		// Verify a new client was instantiated, with the right type.
		Assert.assertEquals( DummyMessagingClient.class, client.getMessagingClient().getClass());

		// Now, remove the factory (e.g. we remove the messaging bundle associated with "foo").
		registry.removeMessagingClientFactory( factory );
		Assert.assertEquals( DismissClient.class, client.getMessagingClient().getClass());
	}


	@Test
	public void testFactorySwitchClientDm() {

		// Here, the factories are created BEFORE the registry is
		// associated with the reconfigurable client. It is a different scenario
		// than the one covered by #testRegistryCallbacks().

		// Create the messaging client factory registry, and register the "foo" and "bar" dummy factories.
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory( new DummyMessagingClientFactory("foo"));
		registry.addMessagingClientFactory( new DummyMessagingClientFactory("bar"));

		// Create the client DM
		final ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.associateMessageProcessor(new AbstractMessageProcessor<IDmClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.setRegistry(registry);

		// Check initial state.
		Assert.assertNull(client.getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DismissClient);
		Assert.assertSame(registry, client.getRegistry());

		// Switch to foo!
		client.switchMessagingType("foo");
		Assert.assertEquals("foo", client.getMessagingType());
		Assert.assertEquals("foo", client.getMessagingClient().getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DummyMessagingClient);

		// Switch to bar!
		client.switchMessagingType("bar");
		Assert.assertEquals("bar", client.getMessagingType());
		Assert.assertEquals("bar", client.getMessagingClient().getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DummyMessagingClient);

		// Switch to null!
		client.switchMessagingType(null);
		Assert.assertNull(client.getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DismissClient);
	}


	@Test
	public void testFactorySwitchClientAgent() {

		// Create the messaging client factory registry, and register the "foo" and "bar" dummy factories.
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory( new DummyMessagingClientFactory("foo"));
		registry.addMessagingClientFactory( new DummyMessagingClientFactory("bar"));

		// Create the client DM
		final ReconfigurableClientAgent client = new ReconfigurableClientAgent();
		client.setApplicationName( "app" );
		client.setScopedInstancePath( "/root" );
		client.associateMessageProcessor(new AbstractMessageProcessor<IAgentClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.setRegistry(registry);

		// Check initial state.
		Assert.assertNull(client.getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DismissClient);
		Assert.assertSame(registry, client.getRegistry());

		// Switch to foo!
		client.switchMessagingType("foo");
		Assert.assertEquals("foo", client.getMessagingType());
		Assert.assertEquals("foo", client.getMessagingClient().getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DummyMessagingClient);

		// Switch to bar!
		client.switchMessagingType("bar");
		Assert.assertEquals("bar", client.getMessagingType());
		Assert.assertEquals("bar", client.getMessagingClient().getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DummyMessagingClient);
		Assert.assertEquals( MessagingConstants.FACTORY_TEST, client.getConfiguration().get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));

		// Switch to null!
		client.switchMessagingType(null);
		Assert.assertNull(client.getMessagingType());
		Assert.assertTrue(client.getMessagingClient() instanceof DismissClient);
		Assert.assertEquals( 0, client.getConfiguration().size());
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
		public IMessagingClient createClient( ReconfigurableClient<?> parent ) {
			return new DummyMessagingClient(this.type);
		}

		@Override
		public boolean setConfiguration( final Map<String, String> configuration ) {
			return this.type.equals(configuration.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		}
	}


	/**
	 * A dummy DM messaging client.
	 */
	private static class DummyMessagingClient extends TestClient {

		/**
		 * The type of the messaging client.
		 */
		final String type;

		/**
		 * Constructor.
		 * @param type the type of the messaging client.
		 */
		DummyMessagingClient( final String type ) {
			this.type = type;
		}

		@Override
		public String getMessagingType() {
			return this.type;
		}
	}
}
