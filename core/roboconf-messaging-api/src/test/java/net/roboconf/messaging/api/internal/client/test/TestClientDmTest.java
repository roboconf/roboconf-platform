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

package net.roboconf.messaging.api.internal.client.test;

import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestClientDmTest {

	@Test
	public void testConnectAndDisconnect() throws Exception {

		TestClientDm client = new TestClientDm();
		Assert.assertFalse( client.isConnected());

		client.openConnection();
		Assert.assertTrue( client.isConnected());

		client.closeConnection();
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testGetConfiguration() throws Exception {

		TestClientDm client = new TestClientDm();

		final Map<String, String> config = client.getConfiguration();
		Assert.assertEquals(1, config.size());
		Assert.assertEquals(MessagingConstants.TEST_FACTORY_TYPE, config.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
	}


	@Test
	public void testSend_noException() throws Exception {

		TestClientDm client = new TestClientDm();
		Assert.assertEquals( 0, client.sentMessages.size());

		client.sendMessageToAgent( new Application( "app", null ), new Instance( "whatever" ), new MsgCmdResynchronize());
		Assert.assertEquals( 1, client.sentMessages.size());

		client.sendMessageToAgent( new Application( "app-2", null ), new Instance( "whatever" ), new MsgCmdResynchronize());
		Assert.assertEquals( 2, client.sentMessages.size());
	}


	@Test( expected = IOException.class )
	public void testSend_withException() throws Exception {

		TestClientDm client = new TestClientDm();
		client.failMessageSending.set( true );
		client.sendMessageToAgent( new Application( "app", null ), new Instance( "whatever" ), new MsgCmdResynchronize());
	}


	@Test
	public void testSendToDebug_noException() throws Exception {

		TestClientDm client = new TestClientDm();
		Assert.assertEquals( 0, client.sentMessages.size());

		client.sendMessageToTheDm( new MsgEcho( "hello" ));
		Assert.assertEquals( 1, client.sentMessages.size());

		client.sendMessageToTheDm( new MsgEcho( "hello 2" ));
		Assert.assertEquals( 2, client.sentMessages.size());
	}


	@Test( expected = IOException.class )
	public void testSendToDebug_withException() throws Exception {

		TestClientDm client = new TestClientDm();
		client.failMessageSending.set( true );
		client.sendMessageToTheDm( new MsgEcho( "hello" ));
	}


	@Test( expected = IOException.class )
	public void testCloseConnection_withException() throws Exception {

		TestClientDm client = new TestClientDm();
		client.failClosingConnection.set( true );
		client.closeConnection();
	}


	@Test( expected = IOException.class )
	public void testListeningToTheDm_withException() throws Exception {

		TestClientDm client = new TestClientDm();
		client.failListeningToTheDm.set( true );
		client.listenToTheDm( ListenerCommand.START );
	}


	@Test
	public void forCodeCoverageOnly() throws Exception {

		TestClientDm client = new TestClientDm();
		client.propagateAgentTermination( new Application( null ), new Instance());
		client.deleteMessagingServerArtifacts( new Application( "whatever", null ));

		client.listenToAgentMessages( new Application( "app", null ), ListenerCommand.START );
		client.listenToAgentMessages( new Application( "app", null ), ListenerCommand.STOP );

		client.listenToTheDm( ListenerCommand.START );
		client.listenToTheDm( ListenerCommand.STOP );
	}
}
