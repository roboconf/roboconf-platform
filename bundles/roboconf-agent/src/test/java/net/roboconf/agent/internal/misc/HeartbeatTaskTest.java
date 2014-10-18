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

package net.roboconf.agent.internal.misc;

import java.io.IOException;

import junit.framework.Assert;
import net.roboconf.agent.internal.Agent;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.client.test.TestClientAgent;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HeartbeatTaskTest {

	@Test
	public void testHeartbeat_connected() {

		TestClientAgent messagingClient = new TestClientAgent() {
			@Override
			public boolean isConnected() {
				return true;
			}
		};

		Agent agent = new MyAgent( messagingClient );
		HeartbeatTask task = new HeartbeatTask( agent );
		Assert.assertEquals( 0, messagingClient.messagesForTheDm.size());

		task.run();
		Assert.assertEquals( 1, messagingClient.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifHeartbeat.class, messagingClient.messagesForTheDm.get( 0 ).getClass());
	}


	@Test
	public void testHeartbeat_notConnected() {

		TestClientAgent messagingClient = new TestClientAgent();
		Agent agent = new MyAgent( messagingClient );
		HeartbeatTask task = new HeartbeatTask( agent );
		Assert.assertEquals( 0, messagingClient.messagesForTheDm.size());

		task.run();
		Assert.assertEquals( 0, messagingClient.messagesForTheDm.size());
	}


	@Test
	public void testHeartbeat_nullClient() {

		Agent agent = new MyAgent( null );
		HeartbeatTask task = new HeartbeatTask( agent );
		task.run();
	}


	@Test
	public void testHeartbeat_exception() {

		TestClientAgent messagingClient = new TestClientAgent() {
			@Override
			public void sendMessageToTheDm( Message message ) throws IOException {
				throw new IOException( "For test purpose" );
			}

			@Override
			public boolean isConnected() {
				return true;
			}
		};

		Agent agent = new MyAgent( messagingClient );
		HeartbeatTask task = new HeartbeatTask( agent );
		Assert.assertEquals( 0, messagingClient.messagesForTheDm.size());

		task.run();
		Assert.assertEquals( 0, messagingClient.messagesForTheDm.size());
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class MyAgent extends Agent {
		private final IAgentClient client;

		public MyAgent( IAgentClient client ) {
			this.client = client;
		}

		@Override
		public IAgentClient getMessagingClient() {
			return this.client;
		}

		@Override
		public boolean hasReceivedModel() {
			return true;
		}
	}
}
