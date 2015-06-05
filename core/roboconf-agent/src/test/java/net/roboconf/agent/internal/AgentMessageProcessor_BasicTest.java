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

import java.util.List;

import junit.framework.Assert;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClientAgent;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentMessageProcessor_BasicTest {

	private Agent agent;
	private TestClientAgent client;


	@Before
	public void initializeAgent() throws Exception {
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory(new TestClientFactory());

		this.agent = new Agent();
		// We first need to start the agent, so it creates the reconfigurable messaging client.
		this.agent.setMessagingType(MessagingConstants.TEST_FACTORY_TYPE);
		this.agent.start();
		// We then set the factory registry of the created client, and reconfigure the agent, so the messaging client backend is created.
		this.agent.getMessagingClient().setRegistry(registry);
		this.agent.reconfigure();

		Thread.sleep( 200 );
		this.client = TestUtils.getInternalField( this.agent.getMessagingClient(), "messagingClient", TestClientAgent.class );
		this.client.messagesForTheDm.clear();
	}


	@After
	public void stopAgent() {
		this.agent.stop();
	}


	@Test
	public void testDmPingResponse() throws IllegalAccessException {
		// Simulate a ping message from the DM.
		MsgEcho ping = new MsgEcho( "PING:TEST", 1234L );
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		processor.processMessage( ping );

		// Check the agent has sent a 'PONG' response to the DM.
		List<Message> messages = this.client.messagesForTheDm;
		Assert.assertEquals( 1, messages.size() );
		Message message = messages.get(0);
		Assert.assertTrue( message instanceof MsgEcho );
		MsgEcho echo = (MsgEcho) message;
		Assert.assertEquals( "PONG:TEST", echo.getContent() );
		Assert.assertEquals( 1234L, echo.getExpirationTime() );
	}


	@Test
	public void testAddInstance() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();

		// Adding an child instance when there is no root
		Assert.assertNull( processor.scopedInstance );
		processor.processMessage( new MsgCmdAddInstance( app.getMySql()));
		Assert.assertNull( processor.scopedInstance );

		// Adding a root instance must fail too
		processor.processMessage( new MsgCmdAddInstance( app.getMySqlVm()));
		Assert.assertNull( processor.scopedInstance );

		// Create a copy of the root instance and insert a child
		Instance newMySqlVm = new Instance( app.getMySqlVm().getName()).component( app.getMySqlVm().getComponent());
		processor.scopedInstance = newMySqlVm;

		app.getMySql().overriddenExports.put( "some-value", "loop" );
		Assert.assertEquals( 0, newMySqlVm.getChildren().size());
		processor.processMessage( new MsgCmdAddInstance( app.getMySql()));
		Assert.assertEquals( 1, newMySqlVm.getChildren().size());

		Instance newChild = newMySqlVm.getChildren().iterator().next();
		Assert.assertEquals( app.getMySql(), newChild );
		Assert.assertEquals( 1, newChild.overriddenExports.size());
		Assert.assertEquals( "loop", newChild.overriddenExports.get( "some-value" ));

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
	public void testSetscopedInstance() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();

		// Insert a non-root element
		Assert.assertNull( processor.scopedInstance );
		processor.processMessage( new MsgCmdSetScopedInstance( app.getMySql()));
		Assert.assertNull( processor.scopedInstance );
		Assert.assertEquals( 0, this.client.messagesForAgentsCount.get());

		// Insert a root
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( "Expected a message for Tomcat, its VM and the WAR.", 3, this.client.messagesForAgentsCount.get());

		// We cannot change the root
		processor.processMessage( new MsgCmdSetScopedInstance( app.getMySqlVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );

		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcat()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );

		// Make sure the final state of the root instance is always "deployed and started"
		processor.scopedInstance = null;
		this.client.messagesForAgentsCount.set( 0 );
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( "Expected a message for Tomcat, its VM and the WAR.", 3, this.client.messagesForAgentsCount.get());
	}


	@Test
	public void testSetscopedInstance_nonRoot() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();
		app.getTomcat().getComponent().installerName( Constants.TARGET_INSTALLER );

		// Insert a non-root element
		Assert.assertNull( processor.scopedInstance );
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcat()));
		Assert.assertEquals( app.getTomcat(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( "Expected a message for Tomcat and the WAR.", 2, this.client.messagesForAgentsCount.get());
	}


	@Test
	public void testSetscopedInstance_rootWithTargetChild() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();
		app.getTomcat().getComponent().installerName( Constants.TARGET_INSTALLER );

		// Insert a root element
		Assert.assertNull( processor.scopedInstance );
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());
		Assert.assertEquals( "Expected a message for the Tomcat VM (children were removed).", 1, this.client.messagesForAgentsCount.get());

		// The Tomcat (child) instance was removed because it is a target (so managed by another agent)
		Assert.assertEquals( 1, InstanceHelpers.buildHierarchicalList( app.getTomcatVm()).size());
	}


	@Test
	public void testSendInstances() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		// No root instance
		processor.processMessage( new MsgCmdSendInstances());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());

		// With a root instance
		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();

		processor.processMessage( new MsgCmdSendInstances());
		Assert.assertEquals(
				InstanceHelpers.buildHierarchicalList( app.getTomcatVm()).size(),
				this.client.messagesForTheDm.size());

		for( int i=1; i<3; i++ )
			Assert.assertEquals( "Index " + i, MsgNotifInstanceChanged.class, this.client.messagesForTheDm.get( i ).getClass());
	}


	@Test
	public void testResynchronize() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		// No root instance
		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());
		Assert.assertEquals( 0, this.client.messagesForAgentsCount.get());

		// With a root instance which has no variable.
		// Unlike with a real messaging client, we do not check variables in our test client.
		// So, one processed instance = one message sent other agents.
		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();
		processor.scopedInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );

		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());
		Assert.assertEquals( 1, this.client.messagesForAgentsCount.get());

		// With a child instance
		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());
		Assert.assertEquals( 3, this.client.messagesForAgentsCount.get());

		// With another started child
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );
		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());
		Assert.assertEquals( 6, this.client.messagesForAgentsCount.get());
	}


	@Test
	public void testRemoveInstance() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		// Remove an instance when there is no model
		TestApplicationTemplate app = new TestApplicationTemplate();
		Assert.assertNull( processor.scopedInstance );
		processor.processMessage( new MsgCmdRemoveInstance( app.getMySqlVm()));
		Assert.assertNull( processor.scopedInstance );

		// Set a root instance and try to remove it => fail
		processor.scopedInstance = app.getMySqlVm();
		processor.processMessage( new MsgCmdRemoveInstance( app.getMySqlVm()));
		Assert.assertEquals( app.getMySqlVm(), processor.scopedInstance );

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

		Assert.assertEquals( 1, this.client.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifInstanceRemoved.class, this.client.messagesForTheDm.get( 0 ).getClass());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getMySql()),
				((MsgNotifInstanceRemoved) this.client.messagesForTheDm.get( 0 )).getInstancePath());
	}


	@Test
	public void testUnknownMessage() {

		AgentMessageProcessor processor = new AgentMessageProcessor( new Agent());
		processor.processMessage( new Message() {
			private static final long serialVersionUID = -3312628850227527510L;
		});
	}


	@Test
	public void checkIoExceptionsAreHandled() {

		this.client.failMessageSending.set( true );
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		processor.scopedInstance = new TestApplicationTemplate().getMySqlVm();
		processor.processMessage( new MsgCmdSendInstances());
		// The processor won't be able to send the model through the messaging.
	}


	@Test
	public void testFindPlugin_simulation() {

		Instance inst = new Instance( "my inst" );
		Agent agent = new Agent();

		// No component and no installer
		Assert.assertEquals( PluginMock.class, agent.findPlugin( inst ).getClass());

		// With an installer
		inst.setComponent( new Component( "comp" ).installerName( "inst" ));
		Assert.assertEquals( PluginMock.class, agent.findPlugin( inst ).getClass());
	}


	@Test
	public void testFindPlugin_ipojo() {

		Instance inst = new Instance( "my inst" );
		Agent agent = new Agent();
		agent.simulatePlugins = false;

		// No component and no installer
		Assert.assertNull( agent.findPlugin( inst ));

		// With an installer
		inst.setComponent( new Component( "comp" ).installerName( "inst" ));
		Assert.assertNull( agent.findPlugin( inst ));

		// With some plug-ins
		PluginMock plugin = new PluginMock();
		agent.plugins.add( plugin );
		Assert.assertNull( agent.findPlugin( inst ));

		inst.getComponent().setInstallerName( plugin.getPluginName());
		Assert.assertEquals( plugin, agent.findPlugin( inst ));
	}


	@Test
	public void testExtensibilityNotifications() {

		Agent agent = new Agent();
		PluginMock pi = new PluginMock();

		Assert.assertEquals( 0, agent.plugins.size());
		agent.pluginAppears( pi );
		Assert.assertEquals( 1, agent.plugins.size());
		agent.pluginWasModified( pi );
		Assert.assertEquals( 1, agent.plugins.size());
		agent.pluginDisappears( pi );
		Assert.assertEquals( 0, agent.plugins.size());

		agent.pluginDisappears( null );
		Assert.assertEquals( 0, agent.plugins.size());

		agent.pluginWasModified( pi );

		agent.plugins.add( new PluginMock());
		agent.plugins.add( new PluginMock());
		Assert.assertEquals( 2, agent.plugins.size());
	}
}
