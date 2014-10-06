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

import java.io.IOException;

import junit.framework.Assert;
import net.roboconf.agent.tests.TestAgentMessagingClient;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.messaging.messages.Message;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentTest {

	@Test
	public void testBasicStartAndStop() {

		final TestAgentMessagingClient client = new TestAgentMessagingClient();
		Agent agent = new Agent();
		agent.setFactory( new MessageServerClientFactory() {
			@Override
			public IAgentClient createAgentClient() {
				return client;
			}
		});

		// Stop when not running => no problem
		Assert.assertFalse( client.isConnected());
		Assert.assertFalse( agent.running );
		agent.stop();
		Assert.assertFalse( client.isConnected());
		Assert.assertFalse( agent.running );

		// Start
		agent.start();
		Assert.assertTrue( agent.running );
		Assert.assertTrue( client.isConnected());

		// Start when already started => nothing
		agent.start();
		Assert.assertTrue( agent.running );
		Assert.assertTrue( client.isConnected());

		// Stop and start again
		agent.stop();
		Assert.assertFalse( agent.running );
		Assert.assertFalse( client.isConnected());

		agent.start();
		Assert.assertTrue( agent.running );
		Assert.assertTrue( client.isConnected());

		agent.stop();
		Assert.assertFalse( agent.running );
		Assert.assertFalse( client.isConnected());

		// Stop called twice => nothing
		agent.stop();
		Assert.assertFalse( agent.running );
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testExceptions_start() {

		// Setup
		final TestAgentMessagingClient client = new TestAgentMessagingClient() {
			@Override
			public void openConnection( AbstractMessageProcessor messageProcessor ) throws IOException {
				throw new IOException( "For tests" );
			}

			@Override
			public void closeConnection() throws IOException {
				throw new IOException( "For tests" );
			}
		};

		Agent agent = new Agent();
		agent.setFactory( new MessageServerClientFactory() {
			@Override
			public IAgentClient createAgentClient() {
				return client;
			}
		});

		// Start => errors are kept quiet. The agent must run all the time.
		agent.start();
		Assert.assertTrue( agent.running );
		Assert.assertFalse( client.isConnected());

		// Stop
		agent.stop();
		Assert.assertFalse( agent.running );
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testExceptions_stop() {

		// Setup
		final TestAgentMessagingClient client = new TestAgentMessagingClient() {
			@Override
			public void sendMessageToTheDm( Message message ) throws IOException {
				throw new IOException( "For tests" );
			}
		};

		Agent agent = new Agent();
		agent.setFactory( new MessageServerClientFactory() {
			@Override
			public IAgentClient createAgentClient() {
				return client;
			}
		});

		// Start => errors are kept quiet. The agent must run all the time.
		agent.start();
		Assert.assertTrue( agent.running );
		Assert.assertTrue( client.isConnected());

		// Stop => the agent is still running if it cannot send a message to the DM.
		// And the client must remain connected for another invocation to "stop".
		agent.stop();
		Assert.assertTrue( agent.running );
		Assert.assertTrue( client.isConnected());
	}


	@Test
	public void testUpdateConfiguration() {

		Agent agent = new Agent();
		agent.setFactory( new MessageServerClientFactory() {
			@Override
			public IAgentClient createAgentClient() {
				return new TestAgentMessagingClient();
			}
		});

		agent.start();
		Assert.assertEquals( agent.messagingClient, agent.messageProcessor.messagingClient );
		Assert.assertEquals( TestAgentMessagingClient.class, agent.messagingClient.getClass());

		IAgentClient oldClient = agent.messagingClient;
		agent.setOverrideProperties( true );
		agent.setIaasType( "a iaas that does not rely on user data" );

		agent.updateConfiguration();
		Assert.assertEquals( agent.messagingClient, agent.messageProcessor.messagingClient );
		Assert.assertEquals( TestAgentMessagingClient.class, agent.messagingClient.getClass());
		Assert.assertNotSame( agent.messagingClient, oldClient );
	}


	@Test
	public void testGetAgentId() {

		Agent agent = new Agent();
		Assert.assertFalse( agent.getAgentId().contains( "null" ));

		agent.setApplicationName( "my app" );
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertTrue( agent.getAgentId().contains( "my app" ));

		agent.setRootInstanceName( "root instance" );
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertTrue( agent.getAgentId().contains( "my app" ));
		Assert.assertTrue( agent.getAgentId().contains( "root instance" ));

		agent.setApplicationName( null );
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertFalse( agent.getAgentId().contains( "my app" ));
		Assert.assertTrue( agent.getAgentId().contains( "root instance" ));
	}
}
