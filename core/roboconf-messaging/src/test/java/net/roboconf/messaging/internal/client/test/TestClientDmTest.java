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

package net.roboconf.messaging.internal.client.test;

import java.io.IOException;

import junit.framework.Assert;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;

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
	public void testSetParameters() throws Exception {

		TestClientDm client = new TestClientDm();
		Assert.assertNull( client.getMessageServerIp());
		Assert.assertNull( client.getMessageServerPassword());
		Assert.assertNull( client.getMessageServerUsername());

		client.setParameters( "192.168.1.15", "oops", "again" );
		Assert.assertEquals( "192.168.1.15", client.getMessageServerIp());
		Assert.assertEquals( "oops", client.getMessageServerUsername());
		Assert.assertEquals( "again", client.getMessageServerPassword());
	}


	@Test
	public void testSend_noException() throws Exception {

		TestClientDm client = new TestClientDm();
		Assert.assertEquals( 0, client.sentMessages.size());

		client.sendMessageToAgent( new Application( "app" ), new Instance( "whatever" ), new MsgCmdResynchronize());
		Assert.assertEquals( 1, client.sentMessages.size());

		client.sendMessageToAgent( new Application( "app-2" ), new Instance( "whatever" ), new MsgCmdResynchronize());
		Assert.assertEquals( 2, client.sentMessages.size());
	}


	@Test( expected = IOException.class )
	public void testSend_withException() throws Exception {

		TestClientDm client = new TestClientDm();
		client.failMessageSending.set( true );
		client.sendMessageToAgent( new Application( "app" ), new Instance( "whatever" ), new MsgCmdResynchronize());
	}


	@Test
	public void forCodeCoverageOnly() throws Exception {

		TestClientDm client = new TestClientDm();
		client.propagateAgentTermination();
		client.deleteMessagingServerArtifacts( new Application( "whatever" ));

		client.listenToAgentMessages( new Application( "app" ), ListenerCommand.START );
		client.listenToAgentMessages( new Application( "app" ), ListenerCommand.STOP );
	}
}
