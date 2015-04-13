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

package net.roboconf.agent.internal;

import junit.framework.Assert;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.client.test.TestClientAgent;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Agent_BasicsTest {

	private Agent agent;


	@Before
	public void initializeAgent() throws Exception {
		this.agent = new Agent();
		this.agent.setMessagingFactoryType( MessagingConstants.FACTORY_TEST );
		this.agent.start();

		Thread.sleep( 200 );
		getInternalClient().messagesForTheDm.clear();
	}


	@After
	public void stopAgent() {
		this.agent.stop();
	}


	@Test
	public void tryToStopNotStartedAgent() {
		stopAgent();
		this.agent = new Agent();
		stopAgent();
	}


	@Test
	public void testForceHeartbeatSending() throws Exception {

		TestClientAgent client = getInternalClient();
		Assert.assertEquals( 0, client.messagesForTheDm.size());

		this.agent.forceHeartbeatSending();
		Assert.assertEquals( 1, client.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifHeartbeat.class, client.messagesForTheDm.get( 0 ).getClass());
	}


	@Test
	public void testBasicStartAndStop() throws Exception {

		TestClientAgent client = getInternalClient();

		// Stop when not running => no problem
		Assert.assertTrue( client.isConnected());
		this.agent.stop();
		Assert.assertFalse( client.isConnected());

		// Start
		this.agent.start();
		client = getInternalClient();
		Assert.assertTrue( client.isConnected());

		// Start when already started => nothing
		IAgentClient oldClient = client;
		this.agent.start();
		client = getInternalClient();
		Assert.assertTrue( client.isConnected());
		Assert.assertNotSame( oldClient, client );

		// Stop and start again
		this.agent.stop();
		Assert.assertFalse( client.isConnected());

		this.agent.start();
		client = getInternalClient();
		Assert.assertTrue( client.isConnected());

		this.agent.stop();
		Assert.assertFalse( client.isConnected());

		// Stop called twice => nothing
		this.agent.stop();
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testStop_withException() throws Exception {

		TestClientAgent client = getInternalClient();
		client.failMessageSending.set( true );
		Assert.assertTrue( client.isConnected());

		this.agent.stop();
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testStop_notConnected() throws Exception {

		TestClientAgent client = getInternalClient();
		client.closeConnection();
		Assert.assertFalse( client.isConnected());

		this.agent.stop();
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testGetAgentId() {

		Agent agent = new Agent();
		Assert.assertFalse( agent.getAgentId().contains( "null" ));

		agent.applicationName = "my app";
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertTrue( agent.getAgentId().contains( "my app" ));

		agent.scopedInstancePath = "/root";
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertTrue( agent.getAgentId().contains( "my app" ));
		Assert.assertTrue( agent.getAgentId().contains( "/root" ));

		agent.applicationName = null;
		Assert.assertFalse( agent.getAgentId().contains( "null" ));
		Assert.assertFalse( agent.getAgentId().contains( "my app" ));
		Assert.assertTrue( agent.getAgentId().contains( "/root" ));
	}


	@Test
	public void testNeedModel_startedButNothingReceived() {
		Assert.assertTrue( this.agent.needsModel());
	}


	@Test
	public void testNeedModel_notStarted() {
		Agent agent = new Agent();
		Assert.assertTrue( agent.needsModel());
	}


	@Test
	public void testNeedModel_modelReceived() {
		((AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor()).scopedInstance = new Instance( "root" );
		Assert.assertFalse( this.agent.needsModel());
	}


	@Test
	public void testMiscPlugins() {

		// No error expected.
		Agent agent = new Agent();
		agent.reconfigure();
	}


	@Test
	public void testReconfigureWhenAgentIsNotStarted() {

		Agent agent = new Agent();
		agent.pluginAppears( null );
		agent.pluginAppears( new PluginMock());
		agent.pluginAppears( new PluginMock());
		agent.listPlugins();
	}



	private TestClientAgent getInternalClient() throws IllegalAccessException {
		return TestUtils.getInternalField( this.agent.getMessagingClient(), "messagingClient", TestClientAgent.class );
	}
}
