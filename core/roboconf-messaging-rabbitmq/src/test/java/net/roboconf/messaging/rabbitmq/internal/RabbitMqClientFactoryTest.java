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

import java.util.Map;

import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.processors.AbstractMessageProcessor;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the RabbitMQ {@link net.roboconf.messaging.api.factory.MessagingClientFactory}.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class RabbitMqClientFactoryTest {

	/**
	 * The messaging client factory registry.
	 */
	private final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();

	/**
	 * The RabbitMq messaging client factory.
	 */
	private final RabbitMqClientFactory factory = new RabbitMqClientFactory();

	@Before
	public void registerRabbitMqFactory() {
		this.factory.setMessageServerIp("localhost");
		this.factory.setMessageServerUsername("guest");
		this.factory.setMessageServerPassword("guest");
		this.registry.addMessagingClientFactory(this.factory);
	}

	@Test
	public void testFactoryReconfigurationClientDm() throws IllegalAccessException {
		// Create the client DM
		final ReconfigurableClientDm client = new ReconfigurableClientDm();
		client.associateMessageProcessor(new AbstractMessageProcessor<IDmClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {

			}
		});
		client.setRegistry(this.registry);
		client.switchMessagingType(RabbitMqConstants.RABBITMQ_FACTORY_TYPE);

		// Check the initial (default) configuration.
		final RabbitMqClientDm client1 = RabbitMqTestUtils.getMessagingClientDm(client);
		final Map<String, String> config1 = client1.getConfiguration();
		Assert.assertEquals(RabbitMqConstants.RABBITMQ_FACTORY_TYPE, config1.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals("localhost", config1.get(RabbitMqConstants.RABBITMQ_SERVER_IP));
		Assert.assertEquals("guest", config1.get(RabbitMqConstants.RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals("guest", config1.get(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD));

		// Reconfigure the factory.
		factory.setMessageServerIp("127.0.0.1");
		factory.setMessageServerUsername("john.doe");
		factory.setMessageServerPassword("1234");
		factory.reconfigure();

		// Check the client has been automatically changed.
		final RabbitMqClientDm client2 = RabbitMqTestUtils.getMessagingClientDm(client);
		Assert.assertNotSame(client1, client2);
		final Map<String, String> config2 = client2.getConfiguration();
		Assert.assertEquals(RabbitMqConstants.RABBITMQ_FACTORY_TYPE, config2.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals("127.0.0.1", config2.get(RabbitMqConstants.RABBITMQ_SERVER_IP));
		Assert.assertEquals("john.doe", config2.get(RabbitMqConstants.RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals("1234", config2.get(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD));
	}

	@Test
	public void testFactoryReconfigurationClientAgent() throws IllegalAccessException {
		// Create the client agent.
		final ReconfigurableClientAgent client = new ReconfigurableClientAgent();
		client.associateMessageProcessor(new AbstractMessageProcessor<IAgentClient>("dummy.messageProcessor") {
			@Override
			protected void processMessage( final Message message ) {

			}
		});
		client.setRegistry(this.registry);
		client.setApplicationName("test");
		client.setScopedInstancePath("/test");
		client.switchMessagingType(RabbitMqConstants.RABBITMQ_FACTORY_TYPE);

		// Check the initial (default) configuration.
		final RabbitMqClientAgent client1 = RabbitMqTestUtils.getMessagingClientAgent(client);
		final Map<String, String> config1 = client1.getConfiguration();
		Assert.assertEquals(RabbitMqConstants.RABBITMQ_FACTORY_TYPE, config1.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals("localhost", config1.get(RabbitMqConstants.RABBITMQ_SERVER_IP));
		Assert.assertEquals("guest", config1.get(RabbitMqConstants.RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals("guest", config1.get(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD));

		// Reconfigure the factory.
		factory.setMessageServerIp("127.0.0.1");
		factory.setMessageServerUsername("john.doe");
		factory.setMessageServerPassword("1234");
		factory.reconfigure();

		// Check the client has been automatically changed.
		final RabbitMqClientAgent client2 = RabbitMqTestUtils.getMessagingClientAgent(client);
		Assert.assertNotSame(client1, client2);
		final Map<String, String> config2 = client2.getConfiguration();
		Assert.assertEquals(RabbitMqConstants.RABBITMQ_FACTORY_TYPE, config2.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals("127.0.0.1", config2.get(RabbitMqConstants.RABBITMQ_SERVER_IP));
		Assert.assertEquals("john.doe", config2.get(RabbitMqConstants.RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals("1234", config2.get(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD));
	}

}
