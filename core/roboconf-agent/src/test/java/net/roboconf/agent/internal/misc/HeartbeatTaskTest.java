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

package net.roboconf.agent.internal.misc;

import junit.framework.Assert;
import net.roboconf.agent.internal.Agent;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClientAgent;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HeartbeatTaskTest {

	private Agent agent;
	private TestClientAgent internalClient;


	@Before
	public void initializeAgent() throws Exception {
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory(new TestClientFactory());

		this.agent = new Agent();
		// We first need to start the agent, so it creates the reconfigurable messaging client.
		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();
		// We then set the factory registry of the created client, and reconfigure the agent, so the messaging client backend is created.
		this.agent.getMessagingClient().setRegistry(registry);
		this.agent.reconfigure();

		Thread.sleep( 200 );
		this.internalClient = TestUtils.getInternalField( this.agent.getMessagingClient(), "messagingClient", TestClientAgent.class );
		this.internalClient.messagesForTheDm.clear();
	}


	@After
	public void stopAgent() {
		this.agent.stop();
	}


	@Test
	public void testHeartbeat_connected() throws Exception {

		this.internalClient.openConnection();
		Assert.assertTrue( this.agent.getMessagingClient().isConnected());

		HeartbeatTask task = new HeartbeatTask( this.agent );
		Assert.assertEquals( 0, this.internalClient.messagesForTheDm.size());

		task.run();
		Assert.assertEquals( 1, this.internalClient.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifHeartbeat.class, this.internalClient.messagesForTheDm.get( 0 ).getClass());
	}


	@Test
	public void testHeartbeat_notConnected() throws Exception {

		this.internalClient.closeConnection();
		Assert.assertFalse( this.agent.getMessagingClient().isConnected());

		HeartbeatTask task = new HeartbeatTask( this.agent );
		Assert.assertEquals( 0, this.internalClient.messagesForTheDm.size());

		task.run();
		Assert.assertEquals( 0, this.internalClient.messagesForTheDm.size());
	}


	@Test
	public void testHeartbeat_nullClient() {

		Agent agent = new Agent();
		Assert.assertNull( agent.getMessagingClient());

		HeartbeatTask task = new HeartbeatTask( agent );
		task.run();
	}


	@Test
	public void testHeartbeat_exception() throws Exception {

		this.internalClient.openConnection();
		this.internalClient.failMessageSending.set( true );
		Assert.assertTrue( this.agent.getMessagingClient().isConnected());

		HeartbeatTask task = new HeartbeatTask( this.agent );
		Assert.assertEquals( 0, this.internalClient.messagesForTheDm.size());

		task.run();
		Assert.assertEquals( 0, this.internalClient.messagesForTheDm.size());
	}
}
