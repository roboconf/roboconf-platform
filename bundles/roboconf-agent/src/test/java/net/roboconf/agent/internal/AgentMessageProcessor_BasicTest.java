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

package net.roboconf.agent.internal;

import java.io.IOException;

import junit.framework.Assert;
import net.roboconf.agent.internal.impl.InMemoryAgentImpl;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.agent.tests.TestAgentMessagingClient;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineUp;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentMessageProcessor_BasicTest {

	@Test
	public void testInitializePlugin() throws Exception {

		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestApplication app = new TestApplication();

		processor.initializePluginForInstance( app.getMySqlVm());
		Assert.assertNull( app.getMySqlVm().getData().get( PluginMock.INIT_PROPERTY ));

		processor.initializePluginForInstance( app.getMySql());
		Assert.assertEquals( "true", app.getMySql().getData().get( PluginMock.INIT_PROPERTY ));
	}


	@Test( expected = PluginException.class )
	public void testInitializePlugin_noPlugin() throws Exception {

		AbstractAgent agent = new InMemoryAgentImpl() {
			@Override
			public PluginInterface findPlugin( Instance instance ) {
				return null;
			}
		};

		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestApplication app = new TestApplication();
		processor.initializePluginForInstance( app.getMySql());
	}


	@Test
	public void testSetMessagingClient() throws Exception {

		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		Assert.assertNull( processor.messagingClient );
		Assert.assertNull( processor.newMessagingClient );

		// Client 1
		TestAgentMessagingClient client1 = new TestAgentMessagingClient();
		processor.setMessagingClient( client1 );

		Assert.assertEquals( client1, processor.messagingClient );
		Assert.assertNull( processor.newMessagingClient );
		Assert.assertEquals( 1, client1.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifMachineUp.class, client1.messagesForTheDm.get( 0 ).getClass());

		// Client 2
		TestAgentMessagingClient client2 = new TestAgentMessagingClient();
		processor.setMessagingClient( client2 );

		Assert.assertEquals( client2, processor.messagingClient );
		Assert.assertNull( processor.newMessagingClient );
		Assert.assertEquals( 1, client2.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifMachineUp.class, client1.messagesForTheDm.get( 0 ).getClass());

		// What happens if there is an awaiting message?
		// The messaging client should not be changed.
		processor.storeMessage( new MsgCmdRequestImport( "whatever" ));

		TestAgentMessagingClient client3 = new TestAgentMessagingClient();
		processor.setMessagingClient( client3 );

		Assert.assertNotSame( client3, processor.messagingClient );
		Assert.assertEquals( client2, processor.messagingClient );
		Assert.assertEquals( client3, processor.newMessagingClient );
		Assert.assertEquals( 0, client3.messagesForTheDm.size());

		// Run the processor.
		// The new message client should be picked up before processing the message.
		processor.start();
		Thread.sleep( 1000 );

		Assert.assertEquals( client3, processor.messagingClient );
		Assert.assertNull( processor.newMessagingClient );
		Assert.assertEquals( 1, client3.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifMachineUp.class, client1.messagesForTheDm.get( 0 ).getClass());

		processor.interrupt();
	}


	@Test
	public void testConfigureToTheLimits() {

		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );

		// No current and no new messaging client => no exception
		processor.configure();

		// No current messaging client => no exception
		processor.newMessagingClient = new TestAgentMessagingClient();
		processor.configure();

		// When the current client is not connected...
		processor.messagingClient = new TestAgentMessagingClient() {
			@Override
			public boolean isConnected() {
				return false;
			}
		};

		// ... and when the new client cannot connect (two cases at once)
		processor.newMessagingClient = new TestAgentMessagingClient() {
			@Override
			public void openConnection( AbstractMessageProcessor messageProcessor )
			throws IOException {
				throw new IOException( "For tests." );
			}
		};

		processor.configure();

		// Catch exceptions on connection management (2 cases at once)
		processor.newMessagingClient = new TestAgentMessagingClient();
		processor.messagingClient = new TestAgentMessagingClient() {
			@Override
			public void closeConnection() throws IOException {
				throw new IOException( "For tests." );
			}

			@Override
			public boolean isConnected() {
				return true;
			}
		};

		processor.configure();
	}


	@Test
	public void testAddInstance() {

		// Initialize all the stuff
		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.setMessagingClient( client );

		TestApplication app = new TestApplication();

		// Adding an child instance when there is no root
		Assert.assertNull( processor.rootInstance );
		processor.processMessage( new MsgCmdAddInstance( app.getMySql()));
		Assert.assertNull( processor.rootInstance );

		// Adding a root instance must fail too
		processor.processMessage( new MsgCmdAddInstance( app.getMySqlVm()));
		Assert.assertNull( processor.rootInstance );

		// Create a copy of the root instance and insert a child
		Instance newMySqlVm = new Instance( app.getMySqlVm().getName()).component( app.getMySqlVm().getComponent());
		processor.rootInstance = newMySqlVm;

		app.getMySql().getOverriddenExports().put( "some-value", "loop" );
		Assert.assertEquals( 0, newMySqlVm.getChildren().size());
		processor.processMessage( new MsgCmdAddInstance( app.getMySql()));
		Assert.assertEquals( 1, newMySqlVm.getChildren().size());

		Instance newChild = newMySqlVm.getChildren().iterator().next();
		Assert.assertEquals( app.getMySql(), newChild );
		Assert.assertEquals( 1, newChild.getOverriddenExports().size());
		Assert.assertEquals( "loop", newChild.getOverriddenExports().get( "some-value" ));
		Assert.assertEquals( "true", newChild.getData().get( PluginMock.INIT_PROPERTY ));

		// Inserting an existing child fails
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( newMySqlVm ).size());
		MsgCmdAddInstance msg = new MsgCmdAddInstance( app.getMySql());
		msg.setData( null );
		msg.setOverridenExports( null );

		processor.processMessage( msg );
		Assert.assertEquals( 1, newMySqlVm.getChildren().size());
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( newMySqlVm ).size());

		// Same thing with a root instance
		processor.processMessage( new MsgCmdAddInstance( app.getMySqlVm()));
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( newMySqlVm ).size());

		// Try to insert an instance whose component is not in the graph
		Instance inst = new Instance( "inst" ).component( new Component( "unknown" ));
		InstanceHelpers.insertChild( app.getMySqlVm(), inst );

		processor.processMessage( new MsgCmdAddInstance( inst ));
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( newMySqlVm ).size());
	}


	@Test
	public void testSetRootInstance() {

		// Initialize all the stuff
		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.setMessagingClient( client );

		TestApplication app = new TestApplication();

		// Insert a non-root element
		Assert.assertNull( processor.rootInstance );
		processor.processMessage( new MsgCmdSetRootInstance( app.getMySql()));
		Assert.assertNull( processor.rootInstance );
		Assert.assertEquals( 0, client.messagesForAgentsCount.get());

		// Insert a root
		processor.processMessage( new MsgCmdSetRootInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.rootInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.rootInstance.getStatus());
		Assert.assertEquals( "Expected a message for Tomcat, its VM and the WAR.", 3, client.messagesForAgentsCount.get());

		// We cannot change the root
		processor.processMessage( new MsgCmdSetRootInstance( app.getMySqlVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.rootInstance );

		processor.processMessage( new MsgCmdSetRootInstance( app.getTomcat()));
		Assert.assertEquals( app.getTomcatVm(), processor.rootInstance );

		// Make sure the final state of the root instance is always "deployed and started"
		processor.rootInstance = null;
		client.messagesForAgentsCount.set( 0 );
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		processor.processMessage( new MsgCmdSetRootInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.rootInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.rootInstance.getStatus());
		Assert.assertEquals( "Expected a message for Tomcat, its VM and the WAR.", 3, client.messagesForAgentsCount.get());

		// All the instances must have been initialized (their plug-in in fact).
		// All, except the root instance.
		for( Instance inst : InstanceHelpers.buildHierarchicalList( processor.rootInstance )) {
			if( inst.getParent() != null )
				Assert.assertEquals( inst.getName(), "true", inst.getData().get( PluginMock.INIT_PROPERTY ));
		}
	}


	@Test
	public void testSendInstances() {

		// Initialize all the stuff
		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.setMessagingClient( client );

		// No root instance
		Assert.assertEquals( 1, client.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifMachineUp.class, client.messagesForTheDm.iterator().next().getClass());

		processor.processMessage( new MsgCmdSendInstances());
		Assert.assertEquals( 1, client.messagesForTheDm.size());

		// With a root instance
		TestApplication app = new TestApplication();
		processor.rootInstance = app.getTomcatVm();

		processor.processMessage( new MsgCmdSendInstances());
		Assert.assertEquals(
				1 + InstanceHelpers.buildHierarchicalList( app.getTomcatVm()).size(),
				client.messagesForTheDm.size());

		for( int i=1; i<4; i++ )
			Assert.assertEquals( "Index " + i, MsgNotifInstanceChanged.class, client.messagesForTheDm.get( i ).getClass());
	}


	@Test
	public void testResynchronize() {

		// Initialize all the stuff
		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.setMessagingClient( client );

		// No root instance
		Assert.assertEquals( 1, client.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifMachineUp.class, client.messagesForTheDm.iterator().next().getClass());

		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 1, client.messagesForTheDm.size());
		Assert.assertEquals( 0, client.messagesForAgentsCount.get());

		// With a root instance which has no variable.
		// Unlike with a real messaging client, we do not check variables in our test client.
		// So, one processed instance = one message sent other agents.
		TestApplication app = new TestApplication();
		processor.rootInstance = app.getTomcatVm();
		processor.rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );

		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 1, client.messagesForTheDm.size());
		Assert.assertEquals( 1, client.messagesForAgentsCount.get());

		// With a child instance
		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 1, client.messagesForTheDm.size());
		Assert.assertEquals( 3, client.messagesForAgentsCount.get());

		// With another started child
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );
		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 1, client.messagesForTheDm.size());
		Assert.assertEquals( 6, client.messagesForAgentsCount.get());
	}


	@Test
	public void testRemoveInstance() {

		// Initialize all the stuff
		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		TestAgentMessagingClient client = new TestAgentMessagingClient();
		processor.setMessagingClient( client );

		// Remove an instance when there is no model
		TestApplication app = new TestApplication();
		Assert.assertNull( processor.rootInstance );
		processor.processMessage( new MsgCmdRemoveInstance( app.getMySqlVm()));
		Assert.assertNull( processor.rootInstance );

		// Set a root instance and try to remove it => fail
		processor.rootInstance = app.getMySqlVm();
		processor.processMessage( new MsgCmdRemoveInstance( app.getMySqlVm()));
		Assert.assertEquals( app.getMySqlVm(), processor.rootInstance );

		// Try to remove an invalid child
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( app.getMySqlVm()).size());
		processor.processMessage( new MsgCmdRemoveInstance( app.getTomcat()));
		processor.processMessage( new MsgCmdRemoveInstance( app.getTomcatVm()));
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( app.getMySqlVm()).size());

		// Try to remove a valid child (started) => fail
		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		processor.processMessage( new MsgCmdRemoveInstance( app.getMySql()));
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( app.getMySqlVm()).size());

		// Try to remove a valid child (not deployed) => OK
		app.getMySql().setStatus( InstanceStatus.NOT_DEPLOYED );
		processor.processMessage( new MsgCmdRemoveInstance( app.getMySql()));
		Assert.assertEquals( 1, InstanceHelpers.buildHierarchicalList( app.getMySqlVm()).size());

		Assert.assertEquals( 2, client.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifMachineUp.class, client.messagesForTheDm.get( 0 ).getClass());
		Assert.assertEquals( MsgNotifInstanceRemoved.class, client.messagesForTheDm.get( 1 ).getClass());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getMySql()),
				((MsgNotifInstanceRemoved) client.messagesForTheDm.get( 1 )).getInstancePath());
	}


	@Test
	public void testUnknownMessage() {

		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		processor.setMessagingClient( new TestAgentMessagingClient());

		processor.processMessage( new Message() {
			private static final long serialVersionUID = -3312628850227527510L;
		});
	}


	@Test
	public void checkIoExceptionsAreHandled() {

		AbstractAgent agent = new InMemoryAgentImpl();
		AgentMessageProcessor processor = new AgentMessageProcessor( agent );
		processor.setMessagingClient( new TestAgentMessagingClient() {
			@Override
			public void sendMessageToTheDm( Message message ) throws IOException {
				throw new IOException();
			}
		});

		processor.rootInstance = new TestApplication().getMySqlVm();
		processor.processMessage( new MsgCmdSendInstances());
		// The processor won't be able to send the model through the messaging.
	}
}
