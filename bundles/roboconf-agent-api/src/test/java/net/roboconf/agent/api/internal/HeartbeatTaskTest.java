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

package net.roboconf.agent.api.internal;

import java.io.IOException;

import junit.framework.Assert;
import net.roboconf.agent.api.internal.HeartbeatTask;
import net.roboconf.agent.api.tests.TestAgentMessagingClient;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HeartbeatTaskTest {

	@Test
	public void testHeartbeat() {

		TestAgentMessagingClient messagingClient = new TestAgentMessagingClient();
		HeartbeatTask task = new HeartbeatTask( "app", "root", messagingClient );
		Assert.assertEquals( 0, messagingClient.messagesForTheDm.size());

		task.run();
		Assert.assertEquals( 1, messagingClient.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifHeartbeat.class, messagingClient.messagesForTheDm.get( 0 ).getClass());
		Assert.assertEquals( "app", ((MsgNotifHeartbeat) messagingClient.messagesForTheDm.get( 0 )).getApplicationName());
		Assert.assertEquals( "root", ((MsgNotifHeartbeat) messagingClient.messagesForTheDm.get( 0 )).getRootInstanceName());
	}


	@Test
	public void testHeartbeat_exception() {

		TestAgentMessagingClient messagingClient = new TestAgentMessagingClient() {
			@Override
			public void sendMessageToTheDm( Message message ) throws IOException {
				throw new IOException( "For test purpose" );
			}
		};

		HeartbeatTask task = new HeartbeatTask( "app", "root", messagingClient );
		Assert.assertEquals( 0, messagingClient.messagesForTheDm.size());

		task.run();
		Assert.assertEquals( 0, messagingClient.messagesForTheDm.size());
	}
}
