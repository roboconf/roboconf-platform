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

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;

import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.business.IAgentClient;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.internal.clients.HttpAgentClient;
import net.roboconf.messaging.http.internal.clients.HttpDmClient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Tests for the HTTP messaging factory.
 * @author Pierre-Yves Gibello - Linagora
 */
public class HttpClientFactoryTest {

	private MessagingClientFactoryRegistry registry;
	private HttpClientFactory factory;


	@Before
	public void registerHttpFactory() {

		this.registry = new MessagingClientFactoryRegistry();
		this.factory = new HttpClientFactory();
		this.factory.setHttpServerIp( HttpConstants.DEFAULT_IP );
		this.factory.setHttpPort( HttpConstants.DEFAULT_PORT );
		this.registry.addMessagingClientFactory( this.factory );
	}


	@Test
	public void testNonTrivialConstructor() {

		BundleContext bundleContext = Mockito.mock( BundleContext.class );
		HttpClientFactory factory = new HttpClientFactory( bundleContext );
		Assert.assertEquals( bundleContext, factory.bundleContext );
	}


	@Test
	public void testFactoryReconfigurationClientDm() throws IllegalAccessException {

		// Create the client DM
		final ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.associateMessageProcessor( new AbstractMessageProcessor<IDmClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.setRegistry(this.registry);
		client.switchMessagingType(HttpConstants.FACTORY_HTTP);

		// Check the initial (default) configuration.
		final HttpDmClient client1 = HttpTestUtils.getMessagingClientDm(client);
		final Map<String,String> config1 = client1.getConfiguration();
		Assert.assertEquals( HttpConstants.FACTORY_HTTP, config1.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
		Assert.assertEquals( HttpConstants.DEFAULT_IP, config1.get( HttpConstants.HTTP_SERVER_IP ));
		Assert.assertEquals( String.valueOf( HttpConstants.DEFAULT_PORT ), config1.get( HttpConstants.HTTP_SERVER_PORT ));
		Assert.assertEquals( 3, config1.size());

		// Reconfigure the factory.
		this.factory.setHttpServerIp( "192.168.1.18" );
		this.factory.setHttpPort( 10234 );
		this.factory.reconfigure();

		// Check the client has been automatically changed.
		final HttpDmClient client2 = HttpTestUtils.getMessagingClientDm(client);
		Assert.assertSame(client1, client2);

		final Map<String,String> config2 = client2.getConfiguration();
		Assert.assertEquals( HttpConstants.FACTORY_HTTP, config2.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
		Assert.assertEquals( "192.168.1.18", config2.get( HttpConstants.HTTP_SERVER_IP ));
		Assert.assertEquals( "10234", config2.get( HttpConstants.HTTP_SERVER_PORT ));
		Assert.assertEquals( 3, config2.size());
	}


	@Test
	public void testFactoryReconfigurationClientAgent() throws IllegalAccessException {

		// Create the client DM
		final ReconfigurableClientAgent client = new ReconfigurableClientAgent();
		client.associateMessageProcessor( new AbstractMessageProcessor<IAgentClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.setRegistry(this.registry);
		client.switchMessagingType(HttpConstants.FACTORY_HTTP);

		// Check the initial (default) configuration.
		final HttpAgentClient client1 = HttpTestUtils.getMessagingClientAgent(client);
		final Map<String, String> config1 = client1.getConfiguration();
		Assert.assertEquals( HttpConstants.FACTORY_HTTP, config1.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
		Assert.assertEquals( HttpConstants.DEFAULT_IP, config1.get(HttpConstants.HTTP_SERVER_IP ));
		Assert.assertEquals( String.valueOf( HttpConstants.DEFAULT_PORT ), config1.get(HttpConstants.HTTP_SERVER_PORT ));

		// Reconfigure the factory.
		this.factory.setHttpServerIp("localhost");
		this.factory.setHttpPort( 10234 );
		this.factory.reconfigure();

		// Check the client has been automatically changed.
		final HttpAgentClient client2 = HttpTestUtils.getMessagingClientAgent(client);
		Assert.assertNotSame(client1, client2);

		final Map<String,String> config2 = client2.getConfiguration();
		Assert.assertEquals( HttpConstants.FACTORY_HTTP, config2.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
		Assert.assertEquals( "localhost", config2.get( HttpConstants.HTTP_SERVER_IP ));
		Assert.assertEquals( "10234", config2.get( HttpConstants.HTTP_SERVER_PORT ));
	}


	@Test
	public void testStart_dmFound() throws Exception {

		// Mock
		Bundle b = Mockito.mock( Bundle.class );
		Mockito.when( b.getSymbolicName()).thenReturn( "net.roboconf.dm" );

		this.factory.httpService = Mockito.mock( HttpService.class );
		this.factory.bundleContext = Mockito.mock( BundleContext.class );
		Mockito.when( this.factory.bundleContext.getBundles()).thenReturn( new Bundle[] { b });

		// Start
		this.factory.start();

		// Check
		Mockito.verify( this.factory.httpService, Mockito.times( 1 )).registerServlet(
				Mockito.eq( HttpConstants.DM_SOCKET_PATH ),
				Mockito.any( Servlet.class ),
				Mockito.any( Dictionary.class ),
				Mockito.isNull( HttpContext.class ));
	}


	@Test
	public void testStart_dmNotFound() throws Exception {

		// Mock
		Bundle b = Mockito.mock( Bundle.class );
		Mockito.when( b.getSymbolicName()).thenReturn( "net.roboconf.NOT.dm" );

		this.factory.httpService = Mockito.mock( HttpService.class );
		this.factory.bundleContext = Mockito.mock( BundleContext.class );
		Mockito.when( this.factory.bundleContext.getBundles()).thenReturn( new Bundle[] { b });

		// Start
		this.factory.start();

		// Check
		Mockito.verifyZeroInteractions( this.factory.httpService );
	}


	@Test
	public void testStop() throws Exception {

		// No client, no error.
		Assert.assertEquals( 0, this.factory.agentClients.size());
		this.factory.stop();

		// Create a client.
		testFactoryReconfigurationClientAgent();

		// Verify there is a client.
		Assert.assertEquals( 1, this.factory.agentClients.size());
		this.factory.stop();
		Assert.assertEquals( 0, this.factory.agentClients.size());
	}


	@Test
	@SuppressWarnings({ "rawtypes" })
	public void testStop_errorOnClose() throws Exception {

		// Mockito does not like classes with generic... <_<
		ReconfigurableClient parent = Mockito.mock( ReconfigurableClientDm.class );
		Mockito.doThrow( new IOException( "For tests..." )).when( parent ).closeConnection();

		HttpAgentClient client = new HttpAgentClient( parent, "localhost", 24587 );
		this.factory.agentClients.add( client );
		Assert.assertEquals( 1, this.factory.agentClients.size());

		this.factory.stop();
		Assert.assertEquals( 0, this.factory.agentClients.size());
		Mockito.verify( parent, Mockito.times( 1 )).closeConnection();
	}


	@Test
	public void testSetConfiguration() {

		Map<String,String> map = new HashMap<String,String>( 0 );
		Assert.assertFalse( this.factory.setConfiguration( map ));

		map.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "whatever" );
		Assert.assertFalse( this.factory.setConfiguration( map ));

		map.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, HttpConstants.FACTORY_HTTP );
		Assert.assertTrue( this.factory.setConfiguration( map ));

		synchronized( this.factory ) {
			Assert.assertEquals( HttpConstants.DEFAULT_IP, this.factory.httpServerIp );
			Assert.assertEquals( HttpConstants.DEFAULT_PORT, this.factory.httpPort );
		}

		map.put( HttpConstants.HTTP_SERVER_IP, "127.0.0.4" );
		map.put( HttpConstants.HTTP_SERVER_PORT, "24658" );

		Assert.assertTrue( this.factory.setConfiguration( map ));
		synchronized( this.factory ) {
			Assert.assertEquals( "127.0.0.4", this.factory.httpServerIp );
			Assert.assertEquals( 24658, this.factory.httpPort );
		}
	}
}
