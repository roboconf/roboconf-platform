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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.agent.internal.test.AgentTestUtils;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceChanged;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifInstanceRemoved;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdUpdateProbeConfiguration;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentMessageProcessorBasicTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Agent agent;
	private TestClient client;


	@Before
	public void initializeAgent() throws Exception {

		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory( new TestClientFactory());
		this.agent = new Agent();

		// We first need to start the agent, so it creates the reconfigurable messaging client.
		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();

		// We then set the factory registry of the created client and reconfigure the agent.
		// => the messaging client backend is created
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
	public void testDmPingResponse() throws IllegalAccessException {

		// Simulate a ping message from the DM.
		MsgEcho ping = new MsgEcho( "PING:TEST" );
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		processor.processMessage( ping );

		// Check the agent has sent a 'PONG' response to the DM.
		List<Message> messages = this.client.messagesForTheDm;
		Assert.assertEquals( 1, messages.size());
		Message message = messages.get(0);
		Assert.assertTrue( message instanceof MsgEcho );
		MsgEcho echo = (MsgEcho) message;
		Assert.assertEquals( "PONG:TEST", echo.getContent());
		Assert.assertEquals( ping.getUuid(), echo.getUuid());
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
	public void testSetScopedInstance() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();

		// Insert a non-root element
		Assert.assertNull( processor.scopedInstance );
		processor.processMessage( new MsgCmdSetScopedInstance( app.getMySql()));
		Assert.assertNull( processor.scopedInstance );
		Assert.assertEquals( 0, this.client.messagesForAgents.size());

		// Insert a root
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());

		// Only the WAR imports variables.
		Assert.assertEquals( 1, this.client.messagesForAgents.size());

		// We cannot change the root
		processor.processMessage( new MsgCmdSetScopedInstance( app.getMySqlVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );

		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcat()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );

		// Make sure the final state of the root instance is always "deployed and started"
		processor.scopedInstance = null;
		this.client.messagesForAgents.clear();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());

		// Only the WAR imports variables.
		Assert.assertEquals( "Expected a message for Tomcat, its VM and the WAR.", 1, this.client.messagesForAgents.size());
	}


	@Test
	public void testSetscopedInstance_withAppBindings_andExternalExports() {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();

		Map<String,String> externalExports = new HashMap<> ();
		externalExports.put( "Lamp", "Toto" );

		Map<String,Set<String>> applicationBindings = new HashMap<> ();
		applicationBindings.put( "Tpl1", new HashSet<>( Arrays.asList( "app1", "app3" )));
		applicationBindings.put( "Tpl2", new HashSet<>( Arrays.asList( "app2" )));

		Map<String,byte[]> scriptResources = new HashMap<> ();

		Message msg = new MsgCmdSetScopedInstance( app.getMySqlVm(), externalExports, applicationBindings, scriptResources );

		// Send the message
		Assert.assertNull( processor.scopedInstance );
		Assert.assertEquals( 0, processor.applicationBindings.size());

		processor.processMessage( msg );

		Assert.assertEquals( app.getMySqlVm(), processor.scopedInstance );
		Assert.assertEquals( 2, processor.applicationBindings.size());
		Assert.assertEquals( new HashSet<>( Arrays.asList( "app2" )), processor.applicationBindings.get( "Tpl2" ));
		Assert.assertEquals( new HashSet<>( Arrays.asList( "app1", "app3" )), processor.applicationBindings.get( "Tpl1" ));
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

		// Only the WAR imports variables.
		Assert.assertEquals( "Expected a message for Tomcat and the WAR.", 1, this.client.messagesForAgents.size());
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

		// The VM is the only kept instance.
		// And it does not export anything, so no sent message.
		Assert.assertEquals( "Expected a message for the Tomcat VM (children were removed).", 0, this.client.messagesForAgents.size());

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
		Assert.assertEquals( 0, this.client.messagesForAgents.size());

		// With a root instance which has no variable.
		// Unlike with a real messaging client, we do not check variables in our test client.
		// So, one processed instance = one message sent other agents.
		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();
		processor.scopedInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );

		// The VM is the only started component, and it does not export any variable.
		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());
		Assert.assertEquals( 0, this.client.messagesForAgents.size());

		// Same thing with the Tomcat instance.
		app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );
		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());
		Assert.assertEquals( 0, this.client.messagesForAgents.size());

		// Only the WAR imports variables.
		app.getWar().setStatus( InstanceStatus.DEPLOYED_STARTED );
		processor.processMessage( new MsgCmdResynchronize());
		Assert.assertEquals( 0, this.client.messagesForTheDm.size());
		Assert.assertEquals( 1, this.client.messagesForAgents.size());
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
	public void testUpdateProbeConfiguration_instanceNotFound() throws Exception {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();
		processor.scopedInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Make sure no resource was saved
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcatVm());
		Utils.deleteFilesRecursively( dir );
		Assert.assertFalse( dir.exists());

		try {
			// The VM is the only started component, and it does not export any variable.
			Map<String,byte[]> map = new HashMap<>( 1 );
			map.put( "VM.properties", new byte[ 0 ]);

			MsgCmdUpdateProbeConfiguration msg = new MsgCmdUpdateProbeConfiguration( "/invalid", map );
			processor.processMessage( msg );
			Assert.assertFalse( dir.exists());

		} finally {
			Utils.deleteFilesRecursively( dir );
		}
	}


	@Test
	public void testUpdateProbeConfiguration_withValidInstance() throws Exception {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();

		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();
		processor.scopedInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Make sure no resource was saved
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcatVm());
		Utils.deleteFilesRecursively( dir );
		Assert.assertFalse( dir.exists());

		try {
			// The VM is the only started component, and it does not export any variable.
			Map<String,byte[]> map = new HashMap<>( 1 );
			map.put( "VM.properties", new byte[ 0 ]);

			MsgCmdUpdateProbeConfiguration msg = new MsgCmdUpdateProbeConfiguration( "/" + app.getTomcatVm(), map );
			processor.processMessage( msg );
			Assert.assertTrue( dir.exists());

			File measuresFile = new File( dir, "VM.properties" );
			Assert.assertTrue( measuresFile.exists());
			Assert.assertEquals( "", Utils.readFileContent( measuresFile ));

			// Verify updates work
			final String content = "target: ec2";
			map.put( "VM.properties", content.getBytes( "UTF-8" ));
			msg = new MsgCmdUpdateProbeConfiguration( "/" + app.getTomcatVm(), map );
			processor.processMessage( msg );

			Assert.assertTrue( measuresFile.exists());
			Assert.assertEquals( content, Utils.readFileContent( measuresFile ));

		} finally {
			Utils.deleteFilesRecursively( dir );
		}
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
		Assert.assertEquals( PluginProxy.class, agent.findPlugin( inst ).getClass());

		// With an installer
		inst.setComponent( new Component( "comp" ).installerName( "inst" ));
		Assert.assertEquals( PluginProxy.class, agent.findPlugin( inst ).getClass());
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
		Assert.assertEquals( plugin, ((PluginProxy) agent.findPlugin( inst )).getPlugin());
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


	@Test
	public void testApplicationBinding_noPrevious_noImportAdded() throws IllegalAccessException {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();

		// Create a binding
		Assert.assertEquals( 0, processor.applicationBindings.size());
		Assert.assertEquals( 0, processor.applicationNameToExternalExports.size());

		MsgCmdChangeBinding msg = new MsgCmdChangeBinding( "ext", new HashSet<>( Arrays.asList( "demo" )));
		processor.processMessage( msg );

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( new HashSet<>( Arrays.asList( "demo" )), processor.applicationBindings.get( "ext" ));

		// No imported added before => nothing to update
		Assert.assertEquals( 0, processor.applicationNameToExternalExports.size());
	}


	@Test
	public void testApplicationBinding_noPrevious_noImportAdded_noNPE() throws IllegalAccessException {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();

		// Create a binding
		Assert.assertEquals( 0, processor.applicationBindings.size());
		Assert.assertEquals( 0, processor.applicationNameToExternalExports.size());

		MsgCmdChangeBinding msg = new MsgCmdChangeBinding( "ext", null );
		processor.processMessage( msg );

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertTrue( processor.applicationBindings.containsKey( "ext" ));
		Assert.assertEquals( null, processor.applicationBindings.get( "ext" ));

		// No imported added before => nothing to update
		Assert.assertEquals( 0, processor.applicationNameToExternalExports.size());
	}


	@Test
	public void testApplicationBinding_withPrevious_noImportAdded() throws IllegalAccessException {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();

		// Create a first binding
		MsgCmdChangeBinding msg = new MsgCmdChangeBinding( "ext", new HashSet<>( Arrays.asList( "demo1" )));
		processor.processMessage( msg );

		// Change the binding
		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( 0, processor.applicationNameToExternalExports.size());

		msg = new MsgCmdChangeBinding( "ext", new HashSet<>( Arrays.asList( "demo2" )));
		processor.processMessage( msg );

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( 1, processor.applicationBindings.get( "ext" ).size());
		Assert.assertTrue( processor.applicationBindings.get( "ext" ).contains( "demo2" ));
		Assert.assertEquals( 0, processor.applicationNameToExternalExports.size());
	}


	@Test
	public void testApplicationBinding_withPrevious_importsAdded() throws IllegalAccessException {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();
		processor.scopedInstance = app.getTomcatVm();

		ImportedVariable var = new ImportedVariable( "ext.ip", false, true );
		app.getTomcatVm().getComponent().importedVariables.put( var.getName(), var );

		// Simulate incoming "external" variables
		processor.processMessage( new MsgCmdAddImport( "demo1", "ext", "/toto-1", new HashMap<String,String>( 0 )));
		processor.processMessage( new MsgCmdAddImport( "demo2", "ext", "/toto", new HashMap<String,String>( 0 )));
		processor.processMessage( new MsgCmdAddImport( "demo1", "ext", "/toto-2", new HashMap<String,String>( 0 )));

		Assert.assertEquals( 0, processor.applicationBindings.size());
		Assert.assertEquals( 2, processor.applicationNameToExternalExports.size());
		Assert.assertEquals( 2, processor.applicationNameToExternalExports.get( "demo1" ).size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo2" ).size());

		Assert.assertEquals( 0, app.getTomcatVm().getImports().size());

		// Create a first binding
		MsgCmdChangeBinding msg = new MsgCmdChangeBinding( "ext", new HashSet<>( Arrays.asList( "demo1" )));
		processor.processMessage( msg );

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( new HashSet<>( Arrays.asList( "demo1" )), processor.applicationBindings.get( "ext" ));

		Assert.assertEquals( 2, processor.applicationNameToExternalExports.size());
		Assert.assertEquals( 2, processor.applicationNameToExternalExports.get( "demo1" ).size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo2" ).size());

		Assert.assertEquals( 1, app.getTomcatVm().getImports().size());
		Assert.assertEquals( 2, app.getTomcatVm().getImports().get( "ext" ).size());

		// Update the bindings
		msg = new MsgCmdChangeBinding( "ext", new HashSet<>( Arrays.asList( "demo1", "demo2" )));
		processor.processMessage( msg );

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( new HashSet<>( Arrays.asList( "demo1", "demo2" )), processor.applicationBindings.get( "ext" ));

		Assert.assertEquals( 2, processor.applicationNameToExternalExports.size());
		Assert.assertEquals( 2, processor.applicationNameToExternalExports.get( "demo1" ).size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo2" ).size());

		Assert.assertEquals( 1, app.getTomcatVm().getImports().size());
		Assert.assertEquals( 3, app.getTomcatVm().getImports().get( "ext" ).size());

		// Simulate a removed import
		processor.processMessage( new MsgCmdRemoveImport( "demo1", "ext", "/toto-1" ));

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( new HashSet<>( Arrays.asList( "demo1", "demo2" )), processor.applicationBindings.get( "ext" ));

		Assert.assertEquals( 2, processor.applicationNameToExternalExports.size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo1" ).size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo2" ).size());

		Assert.assertEquals( 1, app.getTomcatVm().getImports().size());
		Assert.assertEquals( 2, app.getTomcatVm().getImports().get( "ext" ).size());

		// Try to remove an invalid import
		processor.processMessage( new MsgCmdRemoveImport( "demo2", "ext", "/toto-invalid-path" ));

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( new HashSet<>( Arrays.asList( "demo1", "demo2" )), processor.applicationBindings.get( "ext" ));

		Assert.assertEquals( 2, processor.applicationNameToExternalExports.size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo1" ).size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo2" ).size());

		Assert.assertEquals( 1, app.getTomcatVm().getImports().size());
		Assert.assertEquals( 2, app.getTomcatVm().getImports().get( "ext" ).size());

		// And simulate another once
		processor.processMessage( new MsgCmdRemoveImport( "demo2", "ext", "/toto" ));

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( new HashSet<>( Arrays.asList( "demo1", "demo2" )), processor.applicationBindings.get( "ext" ));

		Assert.assertEquals( 1, processor.applicationNameToExternalExports.size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo1" ).size());
		Assert.assertNull( processor.applicationNameToExternalExports.get( "demo2" ));

		Assert.assertEquals( 1, app.getTomcatVm().getImports().size());

		// Update application bindings
		// The only remaining import is associated with "demo1"
		msg = new MsgCmdChangeBinding( "ext", new HashSet<>( Arrays.asList( "demo2" )));
		processor.processMessage( msg );

		Assert.assertEquals( 1, processor.applicationBindings.size());
		Assert.assertEquals( new HashSet<>( Arrays.asList( "demo2" )), processor.applicationBindings.get( "ext" ));

		Assert.assertEquals( 1, processor.applicationNameToExternalExports.size());
		Assert.assertEquals( 1, processor.applicationNameToExternalExports.get( "demo1" ).size());
		Assert.assertNull( processor.applicationNameToExternalExports.get( "demo2" ));

		// No more imports because the last referenced imports matches an application "demo1"
		// for which there is no more bound.
		Assert.assertEquals( 0, app.getTomcatVm().getImports().size());
	}


	@Test
	public void testReset_withScopedInstance() throws Exception {

		// Initialize all the stuff
		AgentMessageProcessor processor = (AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor();
		TestApplicationTemplate app = new TestApplicationTemplate();
		this.agent.karafEtc = this.folder.newFolder().getAbsolutePath();
		this.agent.setApplicationName( "my app" );
		this.agent.setScopedInstancePath( "/vm" );

		Properties props = new Properties();
		props.put( "application-name", this.agent.getApplicationName());
		props.put( "domain", "d" );
		props.put( "scoped-instance-path", this.agent.getScopedInstancePath());
		props.put( "parameters", "@iaas-xxx@" );
		props.put( Constants.MESSAGING_TYPE, "test" );

		File agentConfigFile = new File( this.agent.karafEtc, Constants.KARAF_CFG_FILE_AGENT );
		Utils.writePropertiesFile( props, agentConfigFile );

		// Set the root
		Assert.assertNull( processor.scopedInstance );
		processor.processMessage( new MsgCmdSetScopedInstance( app.getTomcatVm()));
		Assert.assertEquals( app.getTomcatVm(), processor.scopedInstance );
		Assert.assertEquals( app.getTomcatVm(), this.agent.getScopedInstance());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, processor.scopedInstance.getStatus());

		// Add some stuff in the maps
		processor.applicationBindings.put( "key1", new HashSet<>( Arrays.asList( "v1", "v2" )));
		processor.applicationBindings.put( "key2", new HashSet<>( Arrays.asList( "c" )));
		processor.applicationNameToExternalExports.put( "k", Arrays.asList( new Import( "/vm", "comp" )));

		// Verify the agent is in the correct state
		Assert.assertFalse( this.agent.resetInProgress.get());
		this.client.messagesForAgents.clear();
		this.client.messagesForTheDm.clear();

		// Reset
		processor.resetRequest();

		// Verify everything was cleaned
		Assert.assertNull( processor.scopedInstance );
		Assert.assertEquals( 0, processor.applicationBindings.size());
		Assert.assertEquals( 0, processor.applicationNameToExternalExports.size());
		Assert.assertEquals( 0, processor.getMessageQueue().size());

		Assert.assertNull( this.agent.getScopedInstance());
		Assert.assertNull( this.agent.getApplicationName());
		Assert.assertNull( this.agent.getScopedInstancePath());
		Assert.assertEquals( Constants.DEFAULT_DOMAIN, this.agent.getDomain());

		// We cannot verify assertions on the agent, we can only check configuration files
		props = Utils.readPropertiesFile( agentConfigFile );
		Assert.assertEquals( "", props.get( "application-name" ));
		Assert.assertEquals( "", props.get( "scoped-instance-path" ));
		Assert.assertEquals( Constants.DEFAULT_DOMAIN, props.get( "domain" ));
		Assert.assertEquals( "", props.get( "parameters" ));
		Assert.assertEquals( MessagingConstants.FACTORY_IDLE, props.get( Constants.MESSAGING_TYPE ));

		// Verify we sent a MessgeMachineDown
		Assert.assertEquals( 0, this.client.messagesForAgents.size());
		Assert.assertEquals( 1, this.client.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifMachineDown.class, this.client.messagesForTheDm.get( 0 ).getClass());
		Assert.assertEquals( "my app", ((MsgNotifMachineDown) this.client.messagesForTheDm.get( 0 )).getApplicationName());
		Assert.assertEquals( "/vm", ((MsgNotifMachineDown) this.client.messagesForTheDm.get( 0 )).getScopedInstancePath());

		// The reset request was reinitialized
		Assert.assertFalse( processor.resetWasRquested());
		Assert.assertFalse( this.agent.resetInProgress.get());
	}
}
