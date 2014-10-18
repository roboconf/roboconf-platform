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
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.AbstractMessagingTest;
import net.roboconf.messaging.internal.client.test.TestClientAgent;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Agent_BasicsTest {

	@Test
	public void testBasicStartAndStop() throws Exception {

		Agent agent = new Agent( MessagingConstants.FACTORY_TEST );
		agent.start();
		Thread.sleep( AbstractMessagingTest.DELAY );
		TestClientAgent client = (TestClientAgent) agent.messageProcessor.getMessagingClient();

		// Stop when not running => no problem
		Assert.assertTrue( client.isConnected());
		agent.stop();
		Thread.sleep( AbstractMessagingTest.DELAY );
		Assert.assertFalse( client.isConnected());

		// Start
		agent.start();
		Thread.sleep( AbstractMessagingTest.DELAY );
		client = (TestClientAgent) agent.messageProcessor.getMessagingClient();
		Assert.assertTrue( client.isConnected());

		// Start when already started => nothing
		IAgentClient oldClient = client;
		agent.start();
		Thread.sleep( AbstractMessagingTest.DELAY );
		client = (TestClientAgent) agent.messageProcessor.getMessagingClient();
		Assert.assertTrue( client.isConnected());
		Assert.assertEquals( oldClient, client );

		// Stop and start again
		agent.stop();
		Thread.sleep( AbstractMessagingTest.DELAY );
		Assert.assertFalse( client.isConnected());

		agent.start();
		Thread.sleep( AbstractMessagingTest.DELAY );
		client = (TestClientAgent) agent.messageProcessor.getMessagingClient();
		Assert.assertTrue( client.isConnected());

		agent.stop();
		Thread.sleep( AbstractMessagingTest.DELAY );
		Assert.assertFalse( client.isConnected());

		// Stop called twice => nothing
		agent.stop();
		Thread.sleep( AbstractMessagingTest.DELAY );
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testStop_withException() throws Exception {

		Agent agent = new Agent( MessagingConstants.FACTORY_TEST );
		agent.start();
		Thread.sleep( AbstractMessagingTest.DELAY );

		TestClientAgent client = (TestClientAgent) agent.messageProcessor.getMessagingClient();
		client.failMessageSending.set( true );

		AgentMessageProcessor oldProcessor = agent.messageProcessor;
		Assert.assertNotNull( oldProcessor );
		Assert.assertTrue( oldProcessor.getMessagingClient().isConnected());

		agent.stop();
		Thread.sleep( AbstractMessagingTest.DELAY );
		Assert.assertNull( agent.messageProcessor );
		Assert.assertFalse( oldProcessor.getMessagingClient().isConnected());
	}


	@Test
	public void testStop_notConnected() throws Exception {

		Agent agent = new Agent( MessagingConstants.FACTORY_TEST );
		agent.start();
		Thread.sleep( AbstractMessagingTest.DELAY );

		TestClientAgent client = (TestClientAgent) agent.messageProcessor.getMessagingClient();
		AgentMessageProcessor oldProcessor = agent.messageProcessor;

		Assert.assertTrue( oldProcessor.getMessagingClient().isConnected());
		client.closeConnection();
		Assert.assertFalse( oldProcessor.getMessagingClient().isConnected());
		Assert.assertNotNull( oldProcessor );

		agent.stop();
		Thread.sleep( AbstractMessagingTest.DELAY );
		Assert.assertNull( agent.messageProcessor );
		Assert.assertFalse( oldProcessor.getMessagingClient().isConnected());
	}


	@Test
	public void testStop_notStarted() throws Exception {

		Agent agent = new Agent( MessagingConstants.FACTORY_TEST );
		agent.stop();

		Thread.sleep( AbstractMessagingTest.DELAY );
		Assert.assertNull( agent.messageProcessor );
	}


	@Test
	public void testGetAgentId() {

		Agent agent = new Agent();
		Assert.assertFalse( agent.getAgentId().contains( "null" ));

		agent.applicationName = "my app";
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertTrue( agent.getAgentId().contains( "my app" ));

		agent.rootInstanceName = "root instance";
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertTrue( agent.getAgentId().contains( "my app" ));
		Assert.assertTrue( agent.getAgentId().contains( "root instance" ));

		agent.applicationName = null;
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertFalse( agent.getAgentId().contains( "my app" ));
		Assert.assertTrue( agent.getAgentId().contains( "root instance" ));
	}
}
