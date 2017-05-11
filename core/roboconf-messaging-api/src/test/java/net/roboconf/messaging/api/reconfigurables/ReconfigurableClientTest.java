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

package net.roboconf.messaging.api.reconfigurables;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

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
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.internal.jmx.JmxWrapperForMessagingClient;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.processors.AbstractMessageProcessorTest.EmptyTestDmMessageProcessor;
import net.roboconf.messaging.api.utils.OsgiHelper;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReconfigurableClientTest {

	@Test
	public void testTerminateClient_1() throws Exception {

		Logger logger = Logger.getLogger( getClass().getName());
		IMessagingClient client = Mockito.mock( IMessagingClient.class );
		ReconfigurableClient.terminateClient( client, "", logger );
		Mockito.verify( client, Mockito.only()).closeConnection();
	}


	@Test
	public void testTerminateClient_2() throws Exception {

		Logger logger = Logger.getLogger( getClass().getName());
		JmxWrapperForMessagingClient client = Mockito.mock( JmxWrapperForMessagingClient.class );
		ReconfigurableClient.terminateClient( client, "", logger );
		Mockito.verify( client ).closeConnection();
		Mockito.verify( client ).unregisterService();;
		Mockito.verifyNoMoreInteractions( client );
	}


	@Test
	public void testTerminateClient_3() throws Exception {

		Logger logger = Logger.getLogger( getClass().getName());
		JmxWrapperForMessagingClient client = Mockito.mock( JmxWrapperForMessagingClient.class );
		Mockito.doThrow( new IOException( "for test" )).when( client ).closeConnection();

		ReconfigurableClient.terminateClient( client, "", logger );
		Mockito.verify( client ).closeConnection();
		Mockito.verify( client ).unregisterService();;
		Mockito.verifyNoMoreInteractions( client );
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
		client.setMessageQueue( new RoboconfMessageQueue());
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
		client.setMessageQueue( new RoboconfMessageQueue());
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
	public void testRegistryCallbacks() throws Exception {

		// Instead of creating a messaging client explicitly, we rely
		// on the registry callbacks. At runtime, this can happen
		// when we configured the DM or an agent with a messaging type that was not
		// yet deployed or available in the OSGi registry.
		ReconfigurableClientDm client = new ReconfigurableClientDm();
		MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		client.setRegistry( registry );

		client.associateMessageProcessor( new AbstractMessageProcessor<IDmClient>( "dummy.messageProcessor" ) {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.switchMessagingType( "foo" );
		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertTrue(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());

		// Make a new factory appear.
		DummyMessagingClientFactory factory = new DummyMessagingClientFactory( "foo" );
		registry.addMessagingClientFactory( factory );

		// Verify a new client was instantiated, with the right type.
		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertFalse(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());

		Object internalClient = TestUtils.getInternalField(
				(client.getMessagingClient()),
				"messagingClient",
				IMessagingClient.class );

		Assert.assertEquals( DummyMessagingClient.class, internalClient.getClass());

		// Now, remove the factory (e.g. we remove the messaging bundle associated with "foo" ).
		registry.removeMessagingClientFactory( factory );
		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertTrue(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());
	}


	@Test
	public void testFactorySwitchClientDm() throws Exception {

		// Here, the factories are created BEFORE the registry is
		// associated with the reconfigurable client. It is a different scenario
		// than the one covered by #testRegistryCallbacks().

		// Create the messaging client factory registry, and register the "foo" and "bar" dummy factories.
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory( new DummyMessagingClientFactory( "foo" ));
		registry.addMessagingClientFactory( new DummyMessagingClientFactory( "bar" ));

		// Create the client DM
		final ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.associateMessageProcessor(new AbstractMessageProcessor<IDmClient>( "dummy.messageProcessor" ) {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.setRegistry( registry );
		client.console = Mockito.mock( PrintStream.class );

		// Check initial state.
		Assert.assertNull( client.getMessagingType());
		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertTrue(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());
		Assert.assertSame( registry, client.getRegistry());

		// Switch to foo!
		client.switchMessagingType( "foo" );
		Assert.assertEquals( "foo", client.getMessagingType());
		Assert.assertEquals( "foo", client.getMessagingClient().getMessagingType());

		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertFalse(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());

		Object internalClient = TestUtils.getInternalField(
				(client.getMessagingClient()),
				"messagingClient",
				IMessagingClient.class );

		Assert.assertEquals( DummyMessagingClient.class, internalClient.getClass());

		// Switch to bar!
		client.switchMessagingType( "bar" );
		Assert.assertEquals( "bar", client.getMessagingType());
		Assert.assertEquals( "bar", client.getMessagingClient().getMessagingType());

		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertFalse(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());

		internalClient = TestUtils.getInternalField(
				(client.getMessagingClient()),
				"messagingClient",
				IMessagingClient.class );

		Assert.assertEquals( DummyMessagingClient.class, internalClient.getClass());

		// Switch to null!
		client.switchMessagingType( null );
		Assert.assertNull( client.getMessagingType());
		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertTrue(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());

		// No error was output
		Mockito.verifyNoMoreInteractions( client.console );
	}


	@Test
	public void testFactorySwitchClientAgent() throws Exception {

		// Create the messaging client factory registry, and register the "foo" and "bar" dummy factories.
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory( new DummyMessagingClientFactory( "foo" ));
		registry.addMessagingClientFactory( new DummyMessagingClientFactory( "bar" ));

		// Create the client DM
		final ReconfigurableClientAgent client = new ReconfigurableClientAgent();
		client.console = Mockito.mock( PrintStream.class );
		client.setApplicationName( "app" );
		client.setScopedInstancePath( "/root" );
		client.associateMessageProcessor( new AbstractMessageProcessor<IAgentClient>( "dummy.messageProcessor" ) {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.setRegistry(registry);

		// Check initial state.
		Assert.assertNull( client.getMessagingType());
		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertTrue(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());
		Assert.assertSame( registry, client.getRegistry());

		// Switch to foo!
		client.switchMessagingType( "foo" );
		Assert.assertEquals( "foo", client.getMessagingType());
		Assert.assertEquals( "foo", client.getMessagingClient().getMessagingType());

		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertFalse(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());

		Object internalClient = TestUtils.getInternalField(
				(client.getMessagingClient()),
				"messagingClient",
				IMessagingClient.class );

		Assert.assertEquals( DummyMessagingClient.class, internalClient.getClass());

		// Switch to bar!
		client.switchMessagingType( "bar" );
		Assert.assertEquals( "bar", client.getMessagingType());
		Assert.assertEquals( "bar", client.getMessagingClient().getMessagingType());

		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertFalse(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());

		internalClient = TestUtils.getInternalField(
				(client.getMessagingClient()),
				"messagingClient",
				IMessagingClient.class );

		Assert.assertEquals( DummyMessagingClient.class, internalClient.getClass());
		Assert.assertEquals( MessagingConstants.FACTORY_TEST, client.getConfiguration().get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));

		// Switch to null!
		client.switchMessagingType( null );
		Assert.assertNull( client.getMessagingType());
		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertEquals( 0, client.getConfiguration().size());

		// No error was output
		Mockito.verifyNoMoreInteractions( client.console );
	}


	@Test
	public void testFactorySwitch_connectionError() throws Exception {

		// Two things to check here:
		// - There must always be a wrapper client.
		// - An error must be output in the console.

		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory( new DummyMessagingClientFactory( "foo" ) {
			@Override
			public IMessagingClient createClient( ReconfigurableClient<?> parent ) {
				return new DummyMessagingClient(this.type) {

					@Override
					public void openConnection() throws IOException {
						throw new IOException( "for test" );
					}
				};
			}
		});

		// Create the client DM
		final ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.associateMessageProcessor(new AbstractMessageProcessor<IDmClient>( "dummy.messageProcessor" ) {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.setRegistry( registry );
		client.console = Mockito.mock( PrintStream.class );

		// Switch to foo!
		client.switchMessagingType( "foo" );
		Assert.assertEquals( "foo", client.getMessagingType());
		Assert.assertEquals( "foo", client.getMessagingClient().getMessagingType());

		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertFalse(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());

		ArgumentCaptor<String> errorMsgCapture = ArgumentCaptor.forClass( String.class );
		Mockito.verify( client.console, Mockito.only()).println( errorMsgCapture.capture());
		Assert.assertTrue( errorMsgCapture.getValue().startsWith( "\n\n**** WARNING ****\n" ));

		// Switch to null!
		Mockito.reset( client.console );
		client.switchMessagingType( null );
		Assert.assertNull( client.getMessagingType());
		Assert.assertEquals( JmxWrapperForMessagingClient.class, client.getMessagingClient().getClass());
		Assert.assertTrue(((JmxWrapperForMessagingClient) client.getMessagingClient()).isDismissClient());
		Mockito.verifyNoMoreInteractions( client.console );
	}


	@Test
	public void testLookupMessagingClientFactoryRegistryService_noReference() {

		BundleContext bundleCtx = Mockito.mock( BundleContext.class );
		OsgiHelper osgiHelper = Mockito.mock( OsgiHelper.class );
		Mockito.when( osgiHelper.findBundleContext()).thenReturn( bundleCtx );

		MessagingClientFactoryRegistry registry = ReconfigurableClient.lookupMessagingClientFactoryRegistryService( osgiHelper );
		Assert.assertNull( registry );
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testLookupMessagingClientFactoryRegistryService_withReference() {

		BundleContext bundleCtx = Mockito.mock( BundleContext.class );
		OsgiHelper osgiHelper = Mockito.mock( OsgiHelper.class );
		Mockito.when( osgiHelper.findBundleContext()).thenReturn( bundleCtx );

		ServiceReference<MessagingClientFactoryRegistry> reference = Mockito.mock( ServiceReference.class );
		Mockito.when( bundleCtx.getServiceReference( MessagingClientFactoryRegistry.class )).thenReturn( reference );

		MessagingClientFactoryRegistry registryMock = Mockito.mock( MessagingClientFactoryRegistry.class );
		Mockito.when( bundleCtx.getService( reference )).thenReturn( registryMock );

		MessagingClientFactoryRegistry registry = ReconfigurableClient.lookupMessagingClientFactoryRegistryService( osgiHelper );
		Assert.assertNotNull( registry );
		Assert.assertEquals( registryMock, registry );
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
