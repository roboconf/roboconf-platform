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

package net.roboconf.messaging.rabbitmq.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for the RabbitMQ {@link net.roboconf.messaging.api.factory.IMessagingClientFactory}.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class RabbitMqClientFactoryTest {

	private MessagingClientFactoryRegistry registry;
	private RabbitMqClientFactory factory;


	@Before
	public void registerRabbitMqFactory() {

		this.factory = new RabbitMqClientFactory();
		this.factory.setMessageServerIp("localhost");
		this.factory.setMessageServerUsername("guest");
		this.factory.setMessageServerPassword("guest");

		this.registry = new MessagingClientFactoryRegistry();
		this.registry.addMessagingClientFactory(this.factory);
	}


	@Test
	public void testFactoryReconfiguration() throws IllegalAccessException {

		// Create the client DM
		final ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.associateMessageProcessor(new AbstractMessageProcessor<IDmClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {
				// nothing
			}
		});

		client.setRegistry(this.registry);
		client.switchMessagingType(RabbitMqConstants.FACTORY_RABBITMQ);

		// Check the initial (default) configuration.
		final RabbitMqClient client1 = RabbitMqTestUtils.getMessagingClient(client);
		final Map<String,String> config1 = client1.getConfiguration();
		Assert.assertEquals(RabbitMqConstants.FACTORY_RABBITMQ, config1.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals("localhost", config1.get(RabbitMqConstants.RABBITMQ_SERVER_IP));
		Assert.assertEquals("guest", config1.get(RabbitMqConstants.RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals("guest", config1.get(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD));
		Assert.assertEquals( 1, this.factory.clients.size());

		// Reconfigure the factory.
		this.factory.setMessageServerIp("127.0.0.1");
		this.factory.setMessageServerUsername("john.doe");
		this.factory.setMessageServerPassword("1234");
		this.factory.reconfigure();

		// Check the client has been automatically changed.
		final RabbitMqClient client2 = RabbitMqTestUtils.getMessagingClient(client);
		Assert.assertNotSame(client1, client2);
		final Map<String,String> config2 = client2.getConfiguration();
		Assert.assertEquals(RabbitMqConstants.FACTORY_RABBITMQ, config2.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals("127.0.0.1", config2.get(RabbitMqConstants.RABBITMQ_SERVER_IP));
		Assert.assertEquals("john.doe", config2.get(RabbitMqConstants.RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals("1234", config2.get(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD));
		Assert.assertEquals( 1, this.factory.clients.size());
	}


	@Test
	public void testSetConfiguration() {

		Map<String,String> map = new HashMap<String,String>( 0 );
		Assert.assertFalse( this.factory.setConfiguration( map ));

		map.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "whatever" );
		Assert.assertFalse( this.factory.setConfiguration( map ));

		map.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, RabbitMqConstants.FACTORY_RABBITMQ );
		Assert.assertTrue( this.factory.setConfiguration( map ));
		Assert.assertEquals( RabbitMqConstants.DEFAULT_IP, this.factory.messageServerIp );
		Assert.assertEquals( RabbitMqConstants.GUEST, this.factory.messageServerUsername );
		Assert.assertEquals( RabbitMqConstants.GUEST, this.factory.messageServerPassword );

		map.put( RabbitMqConstants.RABBITMQ_SERVER_IP, "127.0.0.1" );
		map.put( RabbitMqConstants.RABBITMQ_SERVER_USERNAME, "bob" );
		map.put( RabbitMqConstants.RABBITMQ_SERVER_PASSWORD, "2" );

		Assert.assertTrue( this.factory.setConfiguration( map ));
		Assert.assertEquals( "127.0.0.1", this.factory.messageServerIp );
		Assert.assertEquals( "bob", this.factory.messageServerUsername );
		Assert.assertEquals( "2", this.factory.messageServerPassword );
	}


	@Test
	public void testStop() throws Exception {

		// No client, no error.
		Assert.assertEquals( 0, this.factory.clients.size());
		this.factory.stop();

		// Create a client.
		testFactoryReconfiguration();

		// Verify there is a client.
		Assert.assertEquals( 1, this.factory.clients.size());
		this.factory.stop();
		Assert.assertEquals( 0, this.factory.clients.size());
	}


	@Test( expected = NullPointerException.class )
	public void testCreateClient_nullParent() {

		this.factory.createClient( null );
	}


	@Test
	@SuppressWarnings({ "rawtypes" })
	public void testStop_errorOnClose() throws Exception {

		// Mockito does not like classes with generic... <_<
		ReconfigurableClient parent = Mockito.mock( ReconfigurableClientDm.class );
		Mockito.doThrow( new IOException( "For tests..." )).when( parent ).closeConnection();

		RabbitMqClient client = new RabbitMqClient( parent, "", "", "" );
		this.factory.clients.add( client );
		Assert.assertEquals( 1, this.factory.clients.size());

		this.factory.stop();
		Assert.assertEquals( 0, this.factory.clients.size());
		Mockito.verify( parent, Mockito.times( 1 )).closeConnection();
	}
}
