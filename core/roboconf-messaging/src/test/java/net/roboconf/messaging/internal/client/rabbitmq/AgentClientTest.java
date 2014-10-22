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

package net.roboconf.messaging.internal.client.rabbitmq;

import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.Assert;
import net.roboconf.messaging.internal.RabbitMqTestUtils;
import net.roboconf.messaging.messages.Message;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Channel;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentClientTest {
	private static boolean rabbitMqIsRunning = false;

	@BeforeClass
	public static void checkRabbitMqIsRunning() throws Exception {
		rabbitMqIsRunning = RabbitMqTestUtils.checkRabbitMqIsRunning();
	}


	@Test
	public void testConnectAndDisconnect() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );

		RabbitMqClientAgent agentClient = new RabbitMqClientAgent();
		agentClient.setParameters( "localhost", "guest", "guest" );
		agentClient.setApplicationName( "app" );
		agentClient.setRootInstanceName( "root" );

		Assert.assertFalse( agentClient.isConnected());
		Assert.assertNull( agentClient.channel );

		LinkedBlockingQueue<Message> messagesQueue = new LinkedBlockingQueue<Message> ();
		agentClient.setMessageQueue( messagesQueue );
		agentClient.openConnection();
		Assert.assertNotNull( agentClient.channel );
		Assert.assertNotNull( agentClient.consumerTag );
		Assert.assertTrue( agentClient.isConnected());

		// openConnection is idem-potent
		Channel oldChannel = agentClient.channel;
		agentClient.openConnection();
		Assert.assertEquals( oldChannel, agentClient.channel );

		agentClient.closeConnection();
		Assert.assertNull( agentClient.channel );
		Assert.assertNull( agentClient.consumerTag );

		// closeConnection is idem-potent
		agentClient.closeConnection();
		Assert.assertNull( agentClient.channel );
	}
}
