/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.internal.client.in_memory;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Assert;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdAddInstance;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryClientTest {

	@Before
	public void reset() {
		InMemoryClient.reset();
	}


	@Test
	public void testGetMessagingType() {

		InMemoryClient client = new InMemoryClient( RecipientKind.DM );
		Assert.assertEquals( MessagingConstants.FACTORY_IN_MEMORY, client.getMessagingType());
	}


	@Test
	public void testGetConfiguration() {

		InMemoryClient client = new InMemoryClient( RecipientKind.DM );
		Map<String,String> conf = client.getConfiguration();

		Assert.assertEquals( 1, conf.size());
		Assert.assertEquals( MessagingConstants.FACTORY_IN_MEMORY, conf.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
	}


	@Test
	public void testScenarios_subscriptions() throws Exception {

		InMemoryClient client = new InMemoryClient( RecipientKind.DM );
		MessagingContext ctx = new MessagingContext( RecipientKind.AGENTS, "app" );

		// Not connected, subscriptions cannot work
		Assert.assertFalse( client.isConnected());
		Assert.assertNull( client.getSubscriptions());
		client.subscribe( ctx );
		Assert.assertNull( client.getSubscriptions());
		client.unsubscribe( ctx );
		Assert.assertNull( client.getSubscriptions());

		// Connection
		client.openConnection();
		Assert.assertTrue( client.isConnected());
		client.subscribe( ctx );
		Assert.assertEquals( 1, client.getSubscriptions().size());
		Assert.assertTrue( client.getSubscriptions().contains( ctx ));

		client.unsubscribe( ctx );
		Assert.assertNull( client.getSubscriptions());

		client.unsubscribe( ctx );
		client.unsubscribe( null );
		Assert.assertNull( client.getSubscriptions());

		// Cleaning artifacts
		client.subscribe( ctx );
		Assert.assertEquals( 1, client.getSubscriptions().size());

		client.deleteMessagingServerArtifacts( null );
		Assert.assertNull( client.getSubscriptions());
	}


	@Test
	public void testScenarios_publications() throws Exception {

		InMemoryClient client = new InMemoryClient( RecipientKind.DM );
		LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<Message> ();
		client.setMessageQueue( queue );

		MessagingContext ctx = new MessagingContext( RecipientKind.AGENTS, "app" );

		// Not connected, publications cannot work
		Message msg = new MsgCmdAddInstance( new Instance( "" ));

		Assert.assertFalse( client.isConnected());
		Assert.assertEquals( 0, queue.size());
		client.publish( ctx, msg );
		Assert.assertEquals( 0, queue.size());

		// Connection
		client.openConnection();
		Assert.assertTrue( client.isConnected());
		client.publish( ctx, msg );
		Assert.assertEquals( 0, queue.size());

		// We need to subscribe to this context to dispatch the message.
		// The messaging tests verify routing more precisely.
		client.subscribe( ctx );
		client.publish( ctx, msg );

		Assert.assertEquals( 1, queue.size());
		Assert.assertEquals( msg, queue.element());
	}


	@Test
	public void testSetOwnerProperties_ownerId() {

		Set<String> ownerIds = new HashSet<> ();

		InMemoryClient client = new InMemoryClient( RecipientKind.DM );
		client.setOwnerProperties( RecipientKind.DM, null, null );
		ownerIds.add( client.ownerId );

		client.setOwnerProperties( RecipientKind.AGENTS, "app1", "root1" );
		ownerIds.add( client.ownerId );

		client.setOwnerProperties( RecipientKind.AGENTS, "app1", "root2" );
		ownerIds.add( client.ownerId );

		client.setOwnerProperties( RecipientKind.AGENTS, "app2", "root2" );
		ownerIds.add( client.ownerId );

		Assert.assertEquals( 4, ownerIds.size());
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testSetOwnerProperties_propertiesAreMoved() throws Exception {

		// Init...
		InMemoryClient client = new InMemoryClient( RecipientKind.DM );
		LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<Message> ();
		client.setMessageQueue( queue );
		client.openConnection();

		MessagingContext ctx = new MessagingContext( RecipientKind.AGENTS, "app" );
		client.subscribe( ctx );
		String ownerId_1 = client.ownerId;

		// Verify associations
		Field field = InMemoryClient.class.getDeclaredField( "CTX_TO_QUEUE" );
		field.setAccessible( true );
		Map<?,?> ctxToQueue = (Map<?,?>) field.get( client );
		Assert.assertEquals( queue, ctxToQueue.get( ownerId_1 ));

		field = InMemoryClient.class.getDeclaredField( "SUBSCRIPTIONS" );
		field.setAccessible( true );
		Map<?,?> sub = (Map<?,?>) field.get( client );

		Set<MessagingContext> subscribedContexts = (Set<MessagingContext>) sub.get( ownerId_1 );
		Assert.assertNotNull( subscribedContexts );
		Assert.assertEquals( 1, subscribedContexts.size());
		Assert.assertTrue( subscribedContexts.contains( ctx ));

		// Change the owner ID
		client.setOwnerProperties( RecipientKind.AGENTS, "app1", "root1" );

		// Verify properties were kept
		String ownerId_2 = client.ownerId;
		Assert.assertFalse( ownerId_2.equals( ownerId_1 ));

		Assert.assertEquals( queue, ctxToQueue.get( ownerId_2 ));
		Assert.assertNull( ctxToQueue.get( ownerId_1 ));
		Assert.assertNull( sub.get( ownerId_1 ));

		subscribedContexts = (Set<MessagingContext>) sub.get( ownerId_2 );
		Assert.assertNotNull( subscribedContexts );
		Assert.assertEquals( 1, subscribedContexts.size());
		Assert.assertTrue( subscribedContexts.contains( ctx ));
	}
}
