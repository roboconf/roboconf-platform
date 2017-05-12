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

package net.roboconf.messaging.rabbitmq.internal;

import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.DEFAULT_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.FACTORY_RABBITMQ;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.GUEST;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_PASSWORD;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_USERNAME;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * Tests for the RabbitMQ {@link net.roboconf.messaging.api.factory.IMessagingClientFactory}.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class RabbitMqClientFactoryTest {

	private MessagingClientFactoryRegistry registry;
	private RabbitMqClientFactory factory;


	@Before
	public void registerRabbitMqFactory() {

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "localhost" );
		configuration.put( RABBITMQ_SERVER_USERNAME, "guest" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "guest" );

		this.factory = new RabbitMqClientFactory();
		this.factory.configuration = configuration;

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
		client.switchMessagingType(FACTORY_RABBITMQ);

		// Check the initial (default) configuration.
		final RabbitMqClient client1 = RabbitMqTestUtils.getMessagingClient(client);
		final Map<String,String> config1 = client1.getConfiguration();
		Assert.assertEquals(FACTORY_RABBITMQ, config1.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals("localhost", config1.get(RABBITMQ_SERVER_IP));
		Assert.assertEquals("guest", config1.get(RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals("guest", config1.get(RABBITMQ_SERVER_PASSWORD));
		Assert.assertEquals( 1, this.factory.clients.size());

		// Reconfigure the factory.
		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "127.0.0.1" );
		configuration.put( RABBITMQ_SERVER_USERNAME, "john.doe" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "1234" );

		this.factory.configuration = configuration;
		this.factory.reconfigure();

		// Check the client has been automatically changed.
		final RabbitMqClient client2 = RabbitMqTestUtils.getMessagingClient(client);
		Assert.assertNotSame(client1, client2);
		final Map<String,String> config2 = client2.getConfiguration();
		Assert.assertEquals(FACTORY_RABBITMQ, config2.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals("127.0.0.1", config2.get(RABBITMQ_SERVER_IP));
		Assert.assertEquals("john.doe", config2.get(RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals("1234", config2.get(RABBITMQ_SERVER_PASSWORD));
		Assert.assertEquals( 1, this.factory.clients.size());
	}


	@Test
	public void testSetConfiguration() {

		Map<String,String> map = new HashMap<>( 0 );
		Assert.assertFalse( this.factory.setConfiguration( map ));

		map.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "whatever" );
		Assert.assertFalse( this.factory.setConfiguration( map ));

		map.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, FACTORY_RABBITMQ );
		Assert.assertTrue( this.factory.setConfiguration( map ));
		Assert.assertEquals( DEFAULT_IP, this.factory.configuration.get( RABBITMQ_SERVER_IP ));
		Assert.assertEquals( GUEST, this.factory.configuration.get( RABBITMQ_SERVER_USERNAME ));
		Assert.assertEquals( GUEST, this.factory.configuration.get( RABBITMQ_SERVER_PASSWORD ));

		map.put( RABBITMQ_SERVER_IP, "127.0.0.1" );
		map.put( RABBITMQ_SERVER_USERNAME, "bob" );
		map.put( RABBITMQ_SERVER_PASSWORD, "2" );

		Assert.assertTrue( this.factory.setConfiguration( map ));
		Assert.assertEquals( "127.0.0.1", this.factory.configuration.get( RABBITMQ_SERVER_IP ));
		Assert.assertEquals( "bob", this.factory.configuration.get( RABBITMQ_SERVER_USERNAME ));
		Assert.assertEquals( "2", this.factory.configuration.get( RABBITMQ_SERVER_PASSWORD ));
	}


	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testSetConfiguration_dictionary() {

		Dictionary dictionary = new Hashtable();
		this.factory.setConfiguration( dictionary );

		dictionary.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "whatever" );
		this.factory.setConfiguration( dictionary );

		dictionary.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, FACTORY_RABBITMQ );
		this.factory.setConfiguration( dictionary );
		Assert.assertEquals( DEFAULT_IP, this.factory.configuration.get( RABBITMQ_SERVER_IP ));
		Assert.assertEquals( GUEST, this.factory.configuration.get( RABBITMQ_SERVER_USERNAME ));
		Assert.assertEquals( GUEST, this.factory.configuration.get( RABBITMQ_SERVER_PASSWORD ));

		dictionary.put( RABBITMQ_SERVER_IP, "127.0.0.1" );
		dictionary.put( RABBITMQ_SERVER_USERNAME, "bob" );
		dictionary.put( RABBITMQ_SERVER_PASSWORD, "2" );
		dictionary.put( "felix.fileinstall.filename", "sth" );

		this.factory.setConfiguration( dictionary );
		Assert.assertEquals( "127.0.0.1", this.factory.configuration.get( RABBITMQ_SERVER_IP ));
		Assert.assertEquals( "bob", this.factory.configuration.get( RABBITMQ_SERVER_USERNAME ));
		Assert.assertEquals( "2", this.factory.configuration.get( RABBITMQ_SERVER_PASSWORD ));
		Assert.assertFalse( this.factory.configuration.containsKey( "felix.fileinstall.filename" ));
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

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "" );
		configuration.put( RABBITMQ_SERVER_USERNAME, "" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "" );

		RabbitMqClient client = new RabbitMqClient( parent, configuration );
		this.factory.clients.add( client );
		Assert.assertEquals( 1, this.factory.clients.size());

		this.factory.stop();
		Assert.assertEquals( 0, this.factory.clients.size());
		Mockito.verify( parent, Mockito.times( 1 )).closeConnection();
	}
}
