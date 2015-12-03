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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestClientTest {

	@Test
	public void testConnectAndDisconnect() throws Exception {

		TestClient client = new TestClient();
		Assert.assertFalse( client.isConnected());

		client.openConnection();
		Assert.assertTrue( client.isConnected());

		client.closeConnection();
		Assert.assertFalse( client.isConnected());

		client.deleteMessagingServerArtifacts( null );
	}


	@Test( expected = IOException.class )
	public void testCloseConnection_exception() throws Exception {

		TestClient client = new TestClient();
		client.failClosingConnection.set( true );
		client.closeConnection();
	}


	@Test
	public void testGetMessagingType() {

		TestClient client = new TestClient();
		Assert.assertEquals( MessagingConstants.FACTORY_TEST, client.getMessagingType());
	}


	@Test
	public void testGetConfiguration() throws Exception {

		TestClient client = new TestClient();

		final Map<String, String> config = client.getConfiguration();
		Assert.assertEquals(1, config.size());
		Assert.assertEquals(MessagingConstants.FACTORY_TEST, config.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
	}


	@Test
	public void testSubscriptions() throws Exception {

		MessagingContext ctx1 = new MessagingContext( RecipientKind.DM, "app1" );
		MessagingContext ctx2 = new MessagingContext( RecipientKind.AGENTS, "app2" );
		MessagingContext ctx3 = new MessagingContext( RecipientKind.AGENTS, "app1" );

		TestClient client = new TestClient();
		client.subscribe( ctx1 );
		client.subscribe( ctx2 );
		client.subscribe( ctx3 );
		client.subscribe( ctx1 );
		Assert.assertEquals( 3, client.subscriptions.size());

		client.unsubscribe( ctx1 );
		Assert.assertEquals( 2, client.subscriptions.size());
	}


	@Test( expected = IOException.class )
	public void testSubscriptions_exception() throws Exception {

		TestClient client = new TestClient();
		client.failSubscribing.set( true );
		client.subscribe( new MessagingContext( RecipientKind.DM, "app1" ));
	}


	@Test
	public void testPublish() throws Exception {

		TestClient client = new TestClient();
		MessagingContext ctx = new MessagingContext( RecipientKind.DM, "app1" );

		Assert.assertEquals( 0, client.ctxToMessages.size());
		client.publish( ctx, new MsgCmdChangeBinding( "prefix", "app" ));
		client.publish( ctx, new MsgCmdChangeBinding( "prefix", "app" ));

		Assert.assertEquals( 1, client.ctxToMessages.size());
		List<Message> messages = client.ctxToMessages.get( ctx );

		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdChangeBinding.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdChangeBinding.class, messages.get( 1 ).getClass());
	}


	@Test( expected = IOException.class )
	public void testPublish_exception() throws Exception {

		TestClient client = new TestClient();
		client.failMessageSending.set( true );
		client.publish( null, null );
	}


	@Test
	public void testClearMessages() {

		TestClient client = new TestClient();

		List<Message> messages = new ArrayList<Message> ();
		messages.add( new MsgCmdRemoveInstance( "/root" ));
		client.ctxToMessages.put( new MessagingContext( RecipientKind.DM, "app" ), messages );

		client.messagesForAgents.add( new MsgCmdRemoveInstance( "/root1" ));
		client.messagesForTheDm.add( new MsgCmdRemoveInstance( "/root2" ));
		client.allSentMessages.add( new MsgCmdRemoveInstance( "/root2" ));

		client.clearMessages();
		Assert.assertEquals( 0, client.messagesForAgents.size());
		Assert.assertEquals( 0, client.messagesForTheDm.size());
		Assert.assertEquals( 0, client.ctxToMessages.size());
		Assert.assertEquals( 0, client.allSentMessages.size());
	}
}
