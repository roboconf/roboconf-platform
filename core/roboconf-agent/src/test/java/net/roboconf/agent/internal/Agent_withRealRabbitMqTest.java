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

package net.roboconf.agent.internal;

import junit.framework.Assert;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.AbstractMessagingTest;
import net.roboconf.messaging.internal.RabbitMqTestUtils;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Agent_withRealRabbitMqTest {
	private static boolean rabbitMqIsRunning = false;

	@BeforeClass
	public static void checkRabbitMqIsRunning() throws Exception {
		rabbitMqIsRunning = RabbitMqTestUtils.checkRabbitMqIsRunning();
	}


	@Test
	public void testUpdateConfiguration() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );

		final Agent agent = new Agent();
		Assert.assertNull( agent.messageProcessor );

		// Start the agent
		agent.messageServerIp = "localhost";
		agent.messageServerPassword = "guest";
		agent.messageServerUsername = "guest";
		agent.rootInstanceName = "root";
		agent.applicationName = "app";

		agent.start();
		Thread.sleep( AbstractMessagingTest.DELAY );

		Assert.assertNotNull( agent.messageProcessor );
		Assert.assertTrue( agent.messageProcessor.getMessagingClient().isConnected());
		Assert.assertTrue( agent.messageProcessor.isRunning());

		IAgentClient oldClient = agent.messageProcessor.getMessagingClient();
		AgentMessageProcessor oldProcessor = agent.messageProcessor;

		// Update the configuration
		agent.updateConfiguration();
		Thread.sleep( AbstractMessagingTest.DELAY );

		Assert.assertNotSame( oldClient, agent.messageProcessor.getMessagingClient());
		Assert.assertEquals( oldProcessor, agent.messageProcessor );

		Assert.assertFalse( oldClient.isConnected());
		Assert.assertTrue( agent.messageProcessor.getMessagingClient().isConnected());
		Assert.assertTrue( agent.messageProcessor.isRunning());

		// Stopping the agent stops the processor and closes the connection.
		oldClient = agent.messageProcessor.getMessagingClient();
		oldProcessor = agent.messageProcessor;

		agent.stop();
		Thread.sleep( AbstractMessagingTest.DELAY );

		Assert.assertNull( agent.messageProcessor );
		Assert.assertFalse( oldProcessor.getMessagingClient().isConnected());

		// And if we start again, we will have a new client and processor
		agent.start();
		Thread.sleep( AbstractMessagingTest.DELAY );

		Assert.assertNotNull( agent.messageProcessor );
		Assert.assertNotSame( oldClient, agent.messageProcessor.getMessagingClient());
		Assert.assertNotSame( oldProcessor, agent.messageProcessor );

		Assert.assertTrue( agent.messageProcessor.getMessagingClient().isConnected());
		Assert.assertTrue( agent.messageProcessor.isRunning());

		// Stopping the agent stops the processor and closes the connection.
		agent.stop();
		Thread.sleep( 200 );
		Assert.assertNull( agent.messageProcessor );
		Assert.assertFalse( oldProcessor.getMessagingClient().isConnected());
	}
}
