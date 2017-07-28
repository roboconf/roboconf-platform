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
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.agent.internal.misc.AgentConstants;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.agent.internal.misc.UserDataHelper;
import net.roboconf.agent.internal.test.AgentTestUtils;
import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.core.utils.ProcessStore;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifLogs;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentBasicsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Agent agent;
	private MessagingClientFactoryRegistry registry;


	@Before
	public void initializeAgent() throws Exception {

		this.registry = new MessagingClientFactoryRegistry();
		this.registry.addMessagingClientFactory( new TestClientFactory());
		this.agent = new Agent();

		this.agent.karafEtc = this.folder.newFolder().getAbsolutePath();
		this.agent.karafData = this.folder.newFolder().getAbsolutePath();

		// We first need to start the agent, so it creates the reconfigurable messaging client.
		this.agent.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.agent.start();

		// We then set the factory registry of the created client, and reconfigure the agent, so the messaging client backend is created.
		this.agent.getMessagingClient().setRegistry( this.registry );
		this.agent.reconfigure();

		Thread.sleep( 200 );
		AgentTestUtils.getInternalClient( this.agent.getMessagingClient()).messagesForTheDm.clear();
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

		this.agent.setApplicationName( "app" );
		this.agent.setScopedInstancePath( "/vm" );

		TestClient client = AgentTestUtils.getInternalClient( this.agent.getMessagingClient());
		Assert.assertEquals( 0, client.messagesForTheDm.size());

		this.agent.forceHeartbeatSending();
		Assert.assertEquals( 1, client.messagesForTheDm.size());
		Assert.assertEquals( MsgNotifHeartbeat.class, client.messagesForTheDm.get( 0 ).getClass());
	}


	@Test
	public void testSetParameters_noNPE() {

		this.agent.setParameters( null );
		Assert.assertNull( this.agent.parameters );
	}


	@Test
	public void testBasicStartAndStop() throws Exception {

		TestClient client = AgentTestUtils.getInternalClient( this.agent.getMessagingClient());

		// Stop when not running => no problem
		Assert.assertTrue( client.isConnected());
		this.agent.stop();
		Assert.assertFalse( client.isConnected());

		// Start
		this.agent.start();
		this.agent.getMessagingClient().setRegistry(this.registry);
		this.agent.reconfigure();
		client = AgentTestUtils.getInternalClient( this.agent.getMessagingClient());
		Assert.assertTrue( client.isConnected());

		// Start when already started => nothing
		IMessagingClient oldClient = client;
		this.agent.start();
		this.agent.getMessagingClient().setRegistry(this.registry);
		this.agent.reconfigure();
		client = AgentTestUtils.getInternalClient( this.agent.getMessagingClient());
		Assert.assertTrue( client.isConnected());
		Assert.assertNotSame( oldClient, client );

		// Stop and start again
		this.agent.stop();
		Assert.assertFalse( client.isConnected());

		this.agent.start();
		this.agent.getMessagingClient().setRegistry(this.registry);
		this.agent.reconfigure();
		client = AgentTestUtils.getInternalClient( this.agent.getMessagingClient());
		Assert.assertTrue( client.isConnected());

		this.agent.stop();
		Assert.assertFalse( client.isConnected());

		// Stop called twice => nothing
		this.agent.stop();
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testStop_withException() throws Exception {

		TestClient client = AgentTestUtils.getInternalClient( this.agent.getMessagingClient());
		client.failMessageSending.set( true );
		Assert.assertTrue( client.isConnected());

		this.agent.stop();
		Assert.assertFalse( client.isConnected());
	}


	@Test
	public void testStop_notConnected() throws Exception {

		TestClient client = AgentTestUtils.getInternalClient( this.agent.getMessagingClient());
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


	@Test
	public void testSetIpAddress() {

		Agent agent = new Agent();
		agent.ipAddress = "something";

		agent.setIpAddress( "else" );
		Assert.assertEquals( "else", agent.ipAddress );

		agent.setIpAddress( null );
		Assert.assertEquals( "else", agent.ipAddress );

		agent.setIpAddress( "" );
		Assert.assertEquals( "else", agent.ipAddress );

		agent.setIpAddress( "something" );
		Assert.assertEquals( "something", agent.ipAddress );
	}


	@Test
	public void testSetNetworkInterface() throws Exception {

		Agent agent = new Agent();
		agent.ipAddress = "something";
		agent.overrideProperties = true;

		// The network interface is always updated.
		// The IP address is updated only when user data cannot override properties.
		agent.setNetworkInterface( "else" );
		Assert.assertEquals( "else", agent.networkInterface );
		Assert.assertEquals( "something", agent.ipAddress );

		agent.overrideProperties = false;
		agent.setNetworkInterface( "else2" );
		Assert.assertEquals( "else2", agent.networkInterface );
		Assert.assertEquals( InetAddress.getLocalHost().getHostAddress(), agent.ipAddress );
	}


	@Test
	public void testAgentStatus() throws Exception {

		// Setup
		String appName = "app";
		String scopedInstancePath = "/some-path";

		this.agent.applicationName = appName;
		this.agent.scopedInstancePath = scopedInstancePath;

		Assert.assertNotNull( this.agent.agentStatus());
		this.agent.stop();

		// First check: messages, no process
		this.agent.getMessagingClient().getMessageProcessor().getMessageQueue().add( Mockito.mock( MsgNotifHeartbeat.class ));
		this.agent.getMessagingClient().getMessageProcessor().getMessageQueue().add( Mockito.mock( MsgNotifAutonomic.class ));
		this.agent.getMessagingClient().getMessageProcessor().getMessageQueue().add( Mockito.mock( MsgNotifLogs.class ));

		String status = this.agent.agentStatus();
		Assert.assertFalse( status.startsWith( "There is no message" ));
		Assert.assertTrue( status.contains( "No recipe is under execution." ));
		Assert.assertFalse( status.contains( "Be careful. A recipe is under execution." ));

		// Now, let's assume a process is running
		ProcessStore.setProcess( appName, scopedInstancePath, Mockito.mock( Process.class ));

		status = this.agent.agentStatus();
		Assert.assertFalse( status.startsWith( "There is no message" ));
		Assert.assertFalse( status.contains( "No recipe is under execution." ));
		Assert.assertTrue( status.contains( "Be careful. A recipe is under execution." ));
	}


	@Test
	public void testReloadUserData_empty() {

		String oldIpAddress = this.agent.ipAddress;
		this.agent.parameters = "";
		this.agent.overrideProperties = true;
		this.agent.reloadUserData();

		Assert.assertEquals( "", this.agent.parameters );
		Assert.assertNull( this.agent.applicationName );
		Assert.assertNull( this.agent.scopedInstancePath );
		Assert.assertEquals( oldIpAddress, this.agent.ipAddress );
		Assert.assertEquals( Constants.DEFAULT_DOMAIN, this.agent.domain );
	}


	@Test
	public void testReloadUserData_fromFile() throws Exception {

		Map<String,String> messagingConfiguration = new HashMap<> ();
		String s = UserDataHelpers.writeUserDataAsString( messagingConfiguration, "d1", "app", "/vm2" );

		File outputFile = this.folder.newFile();
		Utils.writeStringInto( s, outputFile );
		String url = outputFile.toURI().toURL().toString();

		String oldIpAddress = this.agent.ipAddress;
		this.agent.parameters = url;
		this.agent.overrideProperties = true;
		this.agent.reloadUserData();

		Assert.assertEquals( url, this.agent.parameters );
		Assert.assertEquals( "app", this.agent.applicationName );
		Assert.assertEquals( "/vm2", this.agent.scopedInstancePath );
		Assert.assertEquals( "d1", this.agent.domain );
		Assert.assertEquals( oldIpAddress, this.agent.ipAddress );
	}


	@Test
	public void testReloadUserData_fromInexistingFile() throws Exception {

		String oldIpAddress = this.agent.ipAddress;
		this.agent.parameters = "file:/something";
		this.agent.overrideProperties = true;
		this.agent.reloadUserData();

		Assert.assertEquals( "file:/something", this.agent.parameters );
		Assert.assertNull( this.agent.applicationName );
		Assert.assertNull( this.agent.scopedInstancePath );
		Assert.assertEquals( oldIpAddress, this.agent.ipAddress );
		Assert.assertEquals( Constants.DEFAULT_DOMAIN, this.agent.domain );
	}


	@Test
	public void testReloadUserData_fromFile_propertiesCannotBeOverwritten() throws Exception {

		Map<String,String> messagingConfiguration = new HashMap<> ();
		String s = UserDataHelpers.writeUserDataAsString( messagingConfiguration, "d1", "app", "/vm2" );

		File outputFile = this.folder.newFile();
		Utils.writeStringInto( s, outputFile );
		String url = outputFile.toURI().toURL().toString();

		String oldIpAddress = this.agent.ipAddress;
		this.agent.parameters = url;
		this.agent.overrideProperties = false;
		this.agent.reloadUserData();

		Assert.assertEquals( url, this.agent.parameters );
		Assert.assertNull( this.agent.applicationName );
		Assert.assertNull( this.agent.scopedInstancePath );
		Assert.assertEquals( oldIpAddress, this.agent.ipAddress );
		Assert.assertEquals( Constants.DEFAULT_DOMAIN, this.agent.domain );
	}


	@Test
	public void testReloadUserData_ec2() throws Exception {

		this.agent.parameters = AgentConstants.PLATFORM_EC2;
		this.agent.overrideProperties = true;
		this.agent.userDataHelper = Mockito.mock( UserDataHelper.class );
		this.agent.reloadUserData();

		Assert.assertEquals( AgentConstants.PLATFORM_EC2, this.agent.parameters );
		Mockito.verify( this.agent.userDataHelper, Mockito.only()).findParametersForAmazonOrOpenStack( Mockito.any( Logger.class ));
	}


	@Test
	public void testReloadUserData_openstack() throws Exception {

		this.agent.parameters = AgentConstants.PLATFORM_OPENSTACK;
		this.agent.overrideProperties = true;
		this.agent.userDataHelper = Mockito.mock( UserDataHelper.class );
		this.agent.reloadUserData();

		Assert.assertEquals( AgentConstants.PLATFORM_OPENSTACK, this.agent.parameters );
		Mockito.verify( this.agent.userDataHelper, Mockito.only()).findParametersForAmazonOrOpenStack( Mockito.any( Logger.class ));
	}


	@Test
	public void testReloadUserData_azure() throws Exception {

		this.agent.parameters = AgentConstants.PLATFORM_AZURE;
		this.agent.overrideProperties = true;
		this.agent.userDataHelper = Mockito.mock( UserDataHelper.class );
		this.agent.reloadUserData();

		Assert.assertEquals( AgentConstants.PLATFORM_AZURE, this.agent.parameters );
		Mockito.verify( this.agent.userDataHelper, Mockito.only()).findParametersForAzure( Mockito.any( Logger.class ));
	}


	@Test
	public void testReloadUserData_vmware() throws Exception {

		this.agent.parameters = AgentConstants.PLATFORM_VMWARE;
		this.agent.overrideProperties = true;
		this.agent.userDataHelper = Mockito.mock( UserDataHelper.class );
		this.agent.reloadUserData();

		Assert.assertEquals( AgentConstants.PLATFORM_VMWARE, this.agent.parameters );
		Mockito.verify( this.agent.userDataHelper, Mockito.only()).findParametersForVmware( Mockito.any( Logger.class ));
	}


	@Test
	public void testReloadUserData_reset_messageUnderProcessing() throws Exception {

		// Prepare
		this.agent.parameters = Constants.AGENT_RESET;
		this.agent.overrideProperties = true;
		this.agent.userDataHelper = Mockito.mock( UserDataHelper.class );

		// Preconditions
		Assert.assertFalse(((AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor()).resetWasRquested());
		((AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor()).messageUnderProcessing.set( true );

		// Execute
		this.agent.reloadUserData();

		// Postconditions
		Assert.assertEquals( Constants.AGENT_RESET, this.agent.parameters );
		Assert.assertTrue(((AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor()).resetWasRquested());
	}


	@Test
	public void testReloadUserData_reset_noMessageUnderProceesing() throws Exception {

		// Prepare
		this.agent.parameters = Constants.AGENT_RESET;
		this.agent.overrideProperties = true;
		this.agent.userDataHelper = Mockito.mock( UserDataHelper.class );

		// Preconditions
		Assert.assertFalse(((AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor()).resetWasRquested());

		// Execute
		this.agent.reloadUserData();

		// Postconditions + no NPE
		Assert.assertEquals( Constants.AGENT_RESET, this.agent.parameters );
		Assert.assertFalse(((AgentMessageProcessor) this.agent.getMessagingClient().getMessageProcessor()).resetWasRquested());
	}


	@Test
	public void testReloadUserData_ec2_withIpAddress_withReconfigurationException() throws Exception {

		this.agent.parameters = AgentConstants.PLATFORM_EC2;
		this.agent.overrideProperties = true;
		this.agent.userDataHelper = Mockito.mock( UserDataHelper.class );

		AgentProperties props = new AgentProperties();
		props.setApplicationName( "a1" );
		props.setIpAddress( "192.168.1.17" );
		props.setScopedInstancePath( "/root-vm" );

		Map<String,String> messagingConfiguration = new HashMap<> ();
		messagingConfiguration.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "bird" );
		props.setMessagingConfiguration( messagingConfiguration );

		// We do not set the domain.
		// It will be set to its default value.

		Mockito
		.when( this.agent.userDataHelper.findParametersForAmazonOrOpenStack( Mockito.any( Logger.class )))
		.thenReturn( props );

		Mockito
		.doThrow( new RuntimeException( "for test" ))
		.when( this.agent.userDataHelper ).reconfigureMessaging( Mockito.anyString(), Mockito.anyMapOf( String.class, String.class ));

		this.agent.reloadUserData();

		Assert.assertEquals( AgentConstants.PLATFORM_EC2, this.agent.parameters );
		Mockito.verify( this.agent.userDataHelper ).findParametersForAmazonOrOpenStack( Mockito.any( Logger.class ));
		Mockito.verify( this.agent.userDataHelper ).reconfigureMessaging( Mockito.anyString(), Mockito.anyMapOf( String.class, String.class ));
		Mockito.verifyNoMoreInteractions( this.agent.userDataHelper );

		Assert.assertEquals( "a1", this.agent.applicationName );
		Assert.assertEquals( Constants.DEFAULT_DOMAIN, this.agent.domain );
		Assert.assertEquals( "192.168.1.17", this.agent.ipAddress );
		Assert.assertEquals( "/root-vm", this.agent.scopedInstancePath );
	}
}
