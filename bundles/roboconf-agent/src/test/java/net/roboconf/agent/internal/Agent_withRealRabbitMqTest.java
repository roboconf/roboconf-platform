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

import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.Assert;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.AbstractRabbitMqTest;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;

import org.junit.Assume;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Agent_withRealRabbitMqTest extends AbstractRabbitMqTest {

	@Test
	public void testUpdateConfiguration() throws Exception {
		Assume.assumeTrue( this.rabbitMqIsRunning );

		// Link the agent with the agent's message processor, like in the real client implementation.
		final Agent agent = new Agent();
		Assert.assertNull( agent.messageProcessor );
		Assert.assertNull( agent.messagingClient );

		// Start the agent
		agent.setMessageServerIp( "localhost" );
		agent.setMessageServerPassword( "guest" );
		agent.setMessageServerUsername( "guest" );
		agent.setRootInstanceName( "root" );
		agent.setApplicationName( "app" );
		agent.start();

		Thread.sleep( 200 );
		Assert.assertTrue( agent.messagingClient.isConnected());
		Assert.assertTrue( agent.messageProcessor.isRunning());

		LinkedBlockingQueue<Message> oldMessageQueue = agent.getMessages();
		IAgentClient oldClient = agent.messagingClient;
		AgentMessageProcessor oldProcessor = agent.messageProcessor;

		// Update the configuration
		// No message => the current processor must be stopped and a new one must be created by the agent.
		agent.updateConfiguration();

		Thread.sleep( AbstractMessageProcessor.MESSAGE_POLLING_PERIOD + 10 );
		Assert.assertNotSame( oldClient, agent.messagingClient );
		Assert.assertNotSame( oldProcessor, agent.messageProcessor );
		Assert.assertEquals( oldMessageQueue, agent.getMessages());

		Assert.assertFalse( oldProcessor.isRunning());
		Assert.assertFalse( oldClient.isConnected());

		Assert.assertTrue( agent.messagingClient.isConnected());
		Assert.assertTrue( agent.messageProcessor.isRunning());

		// Stopping the agent stops the processor and closes the connection.
		agent.stop();
		Thread.sleep( 200 );
		Assert.assertNull( agent.messageProcessor );
		Assert.assertNull( agent.messagingClient );

		// And if we start again, we will have a new client and processor
		agent.start();

		Thread.sleep( AbstractMessageProcessor.MESSAGE_POLLING_PERIOD + 10 );
		Assert.assertNotSame( oldClient, agent.messagingClient );
		Assert.assertNotSame( oldProcessor, agent.messageProcessor );
		Assert.assertEquals( oldMessageQueue, agent.getMessages());

		Assert.assertTrue( agent.messageProcessor.isRunning());
		Assert.assertTrue( agent.messagingClient.isConnected());

		// Stopping the agent stops the processor and closes the connection.
		agent.stop();
		Thread.sleep( 200 );
		Assert.assertNull( agent.messageProcessor );
		Assert.assertNull( agent.messagingClient );
	}


	@Test
	public void testUpdateConfiguration_2() throws Exception {
		Assume.assumeTrue( this.rabbitMqIsRunning );

		// Since the "interruption" is sent BEFORE processing the message,
		// the test message will not be processed. At least, not by the initial
		// message processor.

		/*
		 * Global idea
		 * ============
		 *
		 * A message processor for the agent is running.
		 * The agent notifies it it is processing its last message.
		 * Then, another message processor runs.
		 */

		Agent agent = new Agent();
		agent.setMessageServerIp( "localhost" );
		agent.setMessageServerPassword( "guest" );
		agent.setMessageServerUsername( "guest" );
		agent.setRootInstanceName( "root" );
		agent.setApplicationName( "app" );

		Assert.assertNull( agent.messageProcessor );
		Assert.assertNull( agent.messagingClient );
		agent.start();
		Assert.assertTrue( agent.messageProcessor.isRunning());
		Assert.assertTrue( agent.messagingClient.isConnected());

		IAgentClient oldClient = agent.messagingClient;
		AgentMessageProcessor oldProcessor = agent.messageProcessor;

		// OK, the processor is running.
		// Let's add a message to process and prepare the agent to switch...
		// There is no DM, so we can send anything we want.
		agent.getMessages().add( new MsgNotifMachineDown( "app", "root1" ));
		Thread.sleep( 100 );

		Assert.assertFalse( oldProcessor.doNotProcessNewMessages );
		agent.updateConfiguration();
		Assert.assertTrue( oldProcessor.doNotProcessNewMessages );
		Assert.assertEquals( oldClient, agent.messagingClient );
		Assert.assertEquals( oldProcessor, agent.messageProcessor );

		// Add a new message, things should change
		Assert.assertTrue( oldProcessor.isRunning());
		agent.getMessages().add( new MsgNotifMachineDown( "app", "root2" ));

		Assert.assertTrue( agent.messageProcessor.isRunning());
		Assert.assertTrue( agent.messageProcessor.isAlive());
		Assert.assertTrue( agent.messagingClient.isConnected());

		// Make sure the old thread dies
		oldProcessor.join( 5000 );
		Assert.assertFalse( oldClient.isConnected());
		Assert.assertFalse( oldProcessor.isRunning());
		Assert.assertFalse( oldProcessor.isAlive());

		agent.stop();
	}
}
