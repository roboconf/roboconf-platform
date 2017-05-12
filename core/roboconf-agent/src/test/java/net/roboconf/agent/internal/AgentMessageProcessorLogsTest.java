/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.agent.internal.misc.AgentConstants;
import net.roboconf.agent.internal.test.AgentTestUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifLogs;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeLogLevel;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdGatherLogs;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentMessageProcessorLogsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Agent agent;
	private TestClient client;


	@Before
	public void initializeAgent() throws Exception {

		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory(new TestClientFactory());
		this.agent = new Agent();

		this.agent.karafEtc = this.folder.newFolder().getAbsolutePath();
		this.agent.karafData = this.folder.newFolder().getAbsolutePath();

		// We first need to start the agent, so it creates the reconfigurable messaging client.
		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();

		// We then set the factory registry of the created client, and reconfigure the agent, so the messaging client backend is created.
		this.agent.getMessagingClient().setRegistry(registry);
		this.agent.reconfigure();

		Thread.sleep( 200 );
		this.client = AgentTestUtils.getInternalClient( this.agent.getMessagingClient());
		this.client.clearMessages();
	}


	@After
	public void stopAgent() {
		this.agent.stop();
	}


	@Test
	public void testChangeLogLevel() throws Exception {

		File karafEtc = new File( this.agent.karafEtc );
		File configFile = new File( karafEtc, AgentConstants.KARAF_LOG_CONF_FILE );
		Assert.assertTrue( configFile.createNewFile());
		Assert.assertEquals( 0, configFile.length());

		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		MsgCmdChangeLogLevel msg = new MsgCmdChangeLogLevel( Level.FINEST );
		processor.processMessage( msg );

		String s = Utils.readFileContent( configFile );
		Assert.assertTrue( s.contains( Level.FINEST.toString() + ", roboconf" ));
	}


	@Test
	public void testGatherLogs() throws Exception {

		File karafData = new File( this.agent.karafData );
		File logFile = new File( karafData, AgentConstants.KARAF_LOGS_DIRECTORY + "/karaf.log" );
		Assert.assertTrue( logFile.getParentFile().mkdir());
		Assert.assertTrue( logFile.createNewFile());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());

		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		MsgCmdGatherLogs msg = new MsgCmdGatherLogs();
		processor.processMessage( msg );

		Assert.assertEquals( 1, this.client.messagesForTheDm.size());
		Message sentMsg = this.client.messagesForTheDm.get( 0 );
		Assert.assertEquals( MsgNotifLogs.class, sentMsg.getClass());
		Assert.assertEquals( 1, ((MsgNotifLogs) sentMsg).getLogFiles().size());
		Assert.assertTrue(((MsgNotifLogs) sentMsg).getLogFiles().containsKey( "karaf.log" ));
	}
}
