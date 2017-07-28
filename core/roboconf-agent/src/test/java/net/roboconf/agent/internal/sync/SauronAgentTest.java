/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.internal.sync;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.agent.internal.AgentMessageProcessor;
import net.roboconf.agent.internal.sync.SauronAgent.SauronAgentClient;
import net.roboconf.messaging.api.client.idle.IdleClient;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SauronAgentTest {

	@Test
	public void testBasics() {

		SauronAgent sauron = new SauronAgent();
		Assert.assertEquals( "Sauron", sauron.getAgentId());
		sauron.reconfigure();

		AgentMessageProcessor processor = sauron.newMessageProcessor();
		Assert.assertEquals( SauronMessageProcessor.class, processor.getClass());

		ReconfigurableClientAgent messagingClient = sauron.newReconfigurableClientAgent();
		Assert.assertEquals( SauronAgentClient.class, messagingClient.getClass());
	}


	@Test
	public void testStoreMessage() {

		Message msg = new MsgEcho( "ok" );
		SauronAgent sauron = new SauronAgent();

		// No NPE
		sauron.processMessageInSequence( msg );

		// Define a message processor
		AgentMessageProcessor processor = sauron.newMessageProcessor();
		Assert.assertEquals( 0, processor.getMessageQueue().size());
		sauron.processMessageInSequence( msg );
		Assert.assertEquals( 1, processor.getMessageQueue().size());
	}


	@Test
	public void sauronMessagingClient() throws Exception {

		SauronAgentClient client = new SauronAgentClient();
		IMessagingClient realClient = client.createMessagingClient( "xyz" );
		Assert.assertNotNull( realClient );
		Assert.assertEquals( IdleClient.class, realClient.getClass());
	}
}
