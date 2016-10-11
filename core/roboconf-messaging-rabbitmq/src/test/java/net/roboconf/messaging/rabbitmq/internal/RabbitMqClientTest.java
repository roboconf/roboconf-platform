/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Channel;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqClientTest {

	private static boolean rabbitMqIsRunning = false;

	@BeforeClass
	public static void checkRabbitMqIsRunning() throws Exception {
		rabbitMqIsRunning = RabbitMqTestUtils.checkRabbitMqIsRunning();
	}


	@Test
	public void testConnectAndDisconnect() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );

		RabbitMqClient client = new RabbitMqClient( null, "localhost", "guest", "guest", RecipientKind.DM );
		client.setOwnerProperties( RecipientKind.DM, "domain", "app", "/root" );

		Assert.assertEquals( RabbitMqConstants.FACTORY_RABBITMQ, client.getMessagingType());
		Assert.assertFalse( client.isConnected());
		Assert.assertNull( client.channel );

		LinkedBlockingQueue<Message> messagesQueue = new LinkedBlockingQueue<>();
		client.setMessageQueue( messagesQueue );
		client.openConnection();
		Assert.assertNotNull( client.channel );
		Assert.assertNotNull( client.consumerTag );
		Assert.assertTrue( client.isConnected());

		// openConnection is idem-potent
		Channel oldChannel = client.channel;
		client.openConnection();
		Assert.assertEquals( oldChannel, client.channel );

		// Delete server artifacts
		client.deleteMessagingServerArtifacts( new Application( "app", new ApplicationTemplate()));

		client.closeConnection();
		Assert.assertNull( client.channel );
		Assert.assertNull( client.consumerTag );

		// closeConnection is idem-potent
		client.closeConnection();
		Assert.assertNull( client.channel );
	}


	@Test
	public void testGetQueueName() throws Exception {

		RabbitMqClient client = new RabbitMqClient( null, "localhost", "guest", "guest", RecipientKind.DM );
		client.setOwnerProperties( RecipientKind.DM, "domain", "app", "/root" );
		Assert.assertEquals( "domain.roboconf-dm", client.getQueueName());

		client.setOwnerProperties( RecipientKind.AGENTS, "domain", "app", "/root" );
		Assert.assertEquals( "domain.app.root", client.getQueueName());

		client.setOwnerProperties( RecipientKind.AGENTS, "domain1", "app", "/root" );
		Assert.assertEquals( "domain1.app.root", client.getQueueName());
	}
}
