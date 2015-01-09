/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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
import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestClientAgentTest {

	@Test
	public void testConnectAndDisconnect() throws Exception {

		TestClientAgent client = new TestClientAgent();
		Assert.assertFalse( client.isConnected());

		client.openConnection();
		Assert.assertTrue( client.isConnected());

		client.closeConnection();
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testSetParameters() throws Exception {

		TestClientAgent client = new TestClientAgent();
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

		TestClientAgent client = new TestClientAgent();
		Assert.assertEquals( 0, client.messagesForAgentsCount.get());

		client.publishExports( new Instance( "whatever" ));
		Assert.assertEquals( 1, client.messagesForAgentsCount.get());

		client.publishExports( new Instance( "whatever" ), "facet" );
		Assert.assertEquals( 2, client.messagesForAgentsCount.get());

		client.unpublishExports( new Instance( "whatever" ));
		Assert.assertEquals( 3, client.messagesForAgentsCount.get());

		client.requestExportsFromOtherAgents( new Instance( "something" ));
		Assert.assertEquals( 4, client.messagesForAgentsCount.get());

		Assert.assertEquals( 0, client.messagesForTheDm.size());
		client.sendMessageToTheDm( new MsgNotifMachineDown( "app", "root" ));
		Assert.assertEquals( 1, client.messagesForTheDm.size());
	}


	@Test( expected = IOException.class )
	public void testSendToDm_exception() throws Exception {

		TestClientAgent client = new TestClientAgent();
		client.failMessageSending.set( true );
		client.sendMessageToTheDm( new MsgNotifMachineDown( "app", "root" ));
	}


	@Test( expected = IOException.class )
	public void testSendToAgent_exception_1() throws Exception {

		TestClientAgent client = new TestClientAgent();
		client.failMessageSending.set( true );
		client.requestExportsFromOtherAgents( new Instance( "something" ));
	}


	@Test( expected = IOException.class )
	public void testSendToAgent_exception_2() throws Exception {

		TestClientAgent client = new TestClientAgent();
		client.failMessageSending.set( true );
		client.unpublishExports( new Instance( "whatever" ));
	}


	@Test( expected = IOException.class )
	public void testSendToAgent_exception_3() throws Exception {

		TestClientAgent client = new TestClientAgent();
		client.failMessageSending.set( true );
		client.publishExports( new Instance( "whatever" ));
	}


	@Test( expected = IOException.class )
	public void testSendToAgent_exception_4() throws Exception {

		TestClientAgent client = new TestClientAgent();
		client.failMessageSending.set( true );
		client.publishExports( new Instance( "whatever" ), "facet" );
	}


	@Test
	public void forCodeCoverageOnly() throws Exception {

		TestClientAgent client = new TestClientAgent();
		client.listenToExportsFromOtherAgents( ListenerCommand.START, new Instance( "hop" ));
		client.listenToExportsFromOtherAgents( ListenerCommand.STOP, new Instance( "hop" ));

		client.listenToRequestsFromOtherAgents( ListenerCommand.START, new Instance( "hop" ));
		client.listenToRequestsFromOtherAgents( ListenerCommand.STOP, new Instance( "hop" ));

		client.listenToTheDm( ListenerCommand.START );
		client.listenToTheDm( ListenerCommand.STOP );
	}
}
