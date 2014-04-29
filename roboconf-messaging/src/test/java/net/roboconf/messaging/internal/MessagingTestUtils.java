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

package net.roboconf.messaging.internal;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportAdd;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdImportRequest;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceAdd;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRemove;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStart;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStop;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceUndeploy;

/**
 * This class defines messaging tests, independently of the implementation.
 * <p>
 * They should work with RabbitMQ or any other messaging server that is
 * supported by Roboconf.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public abstract class MessagingTestUtils {

	private static final int DELAY = 700;
	private static final String URL = "localhost";


	/**
	 * Tests synchronous exchanges between the DM and an agent.
	 * @throws Exception
	 */
	public static void testExchangesBetweenTheDmAndOneAgent() throws Exception {

		// Initialize everything
		Application app = new Application( "app" );
		Instance rootInstance = new Instance( "root" );
		MessageServerClientFactory factory = new MessageServerClientFactory();

		IDmClient dmClient = factory.createDmClient();
		dmClient.setMessageServerIp( URL );
		StorageMessageProcessor dmProcessor = new StorageMessageProcessor();
		dmClient.openConnection( dmProcessor );

		IAgentClient agentClient = factory.createAgentClient();
		agentClient.setApplicationName( app.getName());
		agentClient.setMessageServerIp( URL );
		agentClient.setRootInstanceName( rootInstance.getName());
		StorageMessageProcessor agentProcessor = new StorageMessageProcessor();
		agentClient.openConnection( agentProcessor );

		// The DM sends some message to the client
		Assert.assertEquals( 0, agentProcessor.receivedMessages.size());
		dmClient.sendMessageToAgent( app, rootInstance, new MsgCmdInstanceAdd((String) null, rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, agentProcessor.receivedMessages.size());

		agentClient.listenToTheDm( ListenerCommand.START );
		dmClient.sendMessageToAgent( app, rootInstance, new MsgCmdInstanceRemove( rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, agentProcessor.receivedMessages.size());
		Assert.assertEquals( MsgCmdInstanceRemove.class, agentProcessor.receivedMessages.get( 0 ).getClass());

		// The agent sends a message to the DM
		Assert.assertEquals( 0, dmProcessor.receivedMessages.size());
		agentClient.sendMessageToTheDm( new MsgNotifHeartbeat( app.getName(), rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, dmProcessor.receivedMessages.size());

		dmClient.listenToAgentMessages( app, ListenerCommand.START );
		agentClient.sendMessageToTheDm( new MsgNotifMachineDown( app.getName(), rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, dmProcessor.receivedMessages.size());
		Assert.assertEquals( MsgNotifMachineDown.class, dmProcessor.receivedMessages.get( 0 ).getClass());

		// The DM sends another message
		dmClient.sendMessageToAgent( app, rootInstance, new MsgCmdInstanceStart( rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 2, agentProcessor.receivedMessages.size());
		Assert.assertEquals( MsgCmdInstanceRemove.class, agentProcessor.receivedMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdInstanceStart.class, agentProcessor.receivedMessages.get( 1 ).getClass());

		// The agent stops listening the DM
		agentClient.listenToTheDm( ListenerCommand.STOP );
		dmClient.sendMessageToAgent( app, rootInstance, new MsgCmdInstanceStart( rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 2, agentProcessor.receivedMessages.size());
		Thread.sleep( DELAY );
		Assert.assertEquals( 2, agentProcessor.receivedMessages.size());
		Thread.sleep( DELAY );
		Assert.assertEquals( 2, agentProcessor.receivedMessages.size());

		// The DM stops listening the agent
		dmClient.listenToAgentMessages( app, ListenerCommand.STOP );
		agentClient.sendMessageToTheDm( new MsgNotifHeartbeat( app.getName(), rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, dmProcessor.receivedMessages.size());
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, dmProcessor.receivedMessages.size());
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, dmProcessor.receivedMessages.size());

		// Disconnect the agent
		agentClient.closeConnection();

		// Clean artifacts
		dmClient.deleteMessagingServerArtifacts( app );

		// Disconnect the DM
		dmClient.closeConnection();
	}


	/**
	 * Makes sure messages go to the right agent.
	 * <p>
	 * This is about messages routing.
	 * </p>
	 *
	 * @throws Exception
	 */
	public static void testExchangesBetweenTheDmAndThreeAgents() throws Exception {

		// Initialize everything
		// 1 DM, 2 agents (root1 and root2) for application app1 and 1 agent (root) for app2.
		Application app1 = new Application( "app1" );
		Application app2 = new Application( "app2" );
		Instance app1_root1 = new Instance( "root1" );
		Instance app1_root2 = new Instance( "root2" );
		Instance app2_root = new Instance( "root" );
		MessageServerClientFactory factory = new MessageServerClientFactory();

		IDmClient dmClient = factory.createDmClient();
		dmClient.setMessageServerIp( URL );
		StorageMessageProcessor dmProcessor = new StorageMessageProcessor();
		dmClient.openConnection( dmProcessor );

		IAgentClient agentClientApp1_1 = factory.createAgentClient();
		agentClientApp1_1.setApplicationName( app1.getName());
		agentClientApp1_1.setMessageServerIp( URL );
		agentClientApp1_1.setRootInstanceName( app1_root1.getName());
		StorageMessageProcessor agentProcessorApp1_1 = new StorageMessageProcessor();
		agentClientApp1_1.openConnection( agentProcessorApp1_1 );
		agentClientApp1_1.listenToTheDm( ListenerCommand.START );

		IAgentClient agentClientApp1_2 = factory.createAgentClient();
		agentClientApp1_2.setApplicationName( app1.getName());
		agentClientApp1_2.setMessageServerIp( URL );
		agentClientApp1_2.setRootInstanceName( app1_root2.getName());
		StorageMessageProcessor agentProcessorApp1_2 = new StorageMessageProcessor();
		agentClientApp1_2.openConnection( agentProcessorApp1_2 );
		agentClientApp1_2.listenToTheDm( ListenerCommand.START );

		IAgentClient agentClientApp2 = factory.createAgentClient();
		agentClientApp2.setApplicationName( app2.getName());
		agentClientApp2.setMessageServerIp( URL );
		agentClientApp2.setRootInstanceName( app2_root.getName());
		StorageMessageProcessor agentProcessorApp2 = new StorageMessageProcessor();
		agentClientApp2.openConnection( agentProcessorApp2 );
		agentClientApp2.listenToTheDm( ListenerCommand.START );

		// The DM sends messages
		dmClient.sendMessageToAgent( app1, app1_root1, new MsgCmdInstanceAdd((String) null, app1_root1 ));
		dmClient.sendMessageToAgent( app2, app2_root, new MsgCmdInstanceAdd((String) null, app2_root ));
		dmClient.sendMessageToAgent( app1, app1_root2, new MsgCmdInstanceAdd((String) null, app1_root2 ));
		dmClient.sendMessageToAgent( app2, app2_root, new MsgCmdInstanceRemove( app2_root ));
		dmClient.sendMessageToAgent( app2, app2_root, new MsgCmdInstanceStop( app2_root ));
		dmClient.sendMessageToAgent( app1, app1_root2, new MsgCmdInstanceRemove( app1_root2 ));
		dmClient.sendMessageToAgent( app1, app1_root2, new MsgCmdInstanceRemove( app1_root2 ));
		dmClient.sendMessageToAgent( app1, app1_root2, new MsgCmdInstanceUndeploy( app1_root2 ));
		dmClient.sendMessageToAgent( app1, app1_root1, new MsgCmdInstanceRemove( app1_root1 ));
		dmClient.sendMessageToAgent( app1, app1_root1, new MsgCmdInstanceAdd((String) null, app1_root1 ));

		// Check what was received
		Thread.sleep( DELAY );

		Assert.assertEquals( 3, agentProcessorApp1_1.receivedMessages.size());
		Assert.assertEquals( MsgCmdInstanceAdd.class, agentProcessorApp1_1.receivedMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdInstanceRemove.class, agentProcessorApp1_1.receivedMessages.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdInstanceAdd.class, agentProcessorApp1_1.receivedMessages.get( 2 ).getClass());

		Assert.assertEquals( 4, agentProcessorApp1_2.receivedMessages.size());
		Assert.assertEquals( MsgCmdInstanceAdd.class, agentProcessorApp1_2.receivedMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdInstanceRemove.class, agentProcessorApp1_2.receivedMessages.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdInstanceRemove.class, agentProcessorApp1_2.receivedMessages.get( 2 ).getClass());
		Assert.assertEquals( MsgCmdInstanceUndeploy.class, agentProcessorApp1_2.receivedMessages.get( 3 ).getClass());

		Assert.assertEquals( 3, agentProcessorApp2.receivedMessages.size());
		Assert.assertEquals( MsgCmdInstanceAdd.class, agentProcessorApp2.receivedMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdInstanceRemove.class, agentProcessorApp2.receivedMessages.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdInstanceStop.class, agentProcessorApp2.receivedMessages.get( 2 ).getClass());

		// Disconnect the agents
		agentClientApp1_1.closeConnection();
		agentClientApp1_2.closeConnection();
		agentClientApp2.closeConnection();

		// Clean artifacts
		dmClient.deleteMessagingServerArtifacts( app1 );
		dmClient.deleteMessagingServerArtifacts( app2 );

		// Disconnect the DM
		dmClient.closeConnection();
	}


	/**
	 * Makes sure exports are exchanged correctly between agents.
	 * @throws Exception
	 */
	public static void testExportsBetweenAgents() throws Exception {

		// 3 agents (tomcat, mysql, apache) for application app1 and 1 agent (root) for app2.
		// This last one should not receive anything!
		Application app1 = new Application( "app1" );
		Application app2 = new Application( "app2" );

		Component tomcatComponent = new Component( "Tomcat" );
		tomcatComponent.getExportedVariables().put( "Tomcat.ip", "localhost" );
		tomcatComponent.getExportedVariables().put( "Tomcat.port", "8080" );
		tomcatComponent.getImportedVariables().put( "MySQL.port", Boolean.FALSE );
		tomcatComponent.getImportedVariables().put( "MySQL.ip", Boolean.FALSE );
		Instance tomcat = new Instance( "tomcat" ).component( tomcatComponent );

		Component mysqlComponent = new Component( "MySQL" );
		mysqlComponent.getExportedVariables().put( "MySQL.port", "3306" );
		mysqlComponent.getExportedVariables().put( "MySQL.ip", "192.168.1.15" );
		Instance mysql = new Instance( "mysql" ).component( mysqlComponent );

		Component apacheComponent = new Component( "Apache" );
		apacheComponent.getExportedVariables().put( "Apache.ip", "apache.roboconf.net" );
		apacheComponent.getImportedVariables().put( "Tomcat.port", Boolean.FALSE );
		apacheComponent.getImportedVariables().put( "Tomcat.ip", Boolean.FALSE );
		Instance apache = new Instance( "apache" ).component( apacheComponent );

		// This one is a good candidate to receive something when others publish something.
		// Except it is not in the same application.
		Component otherComponent = new Component( "other" );
		apacheComponent.getImportedVariables().put( "Tomcat.port", Boolean.FALSE );
		apacheComponent.getImportedVariables().put( "Tomcat.ip", Boolean.FALSE );
		apacheComponent.getImportedVariables().put( "Mongo.ip", Boolean.FALSE );
		mysqlComponent.getExportedVariables().put( "MySQL.port", "3306" );
		mysqlComponent.getExportedVariables().put( "MySQL.ip", "192.168.1.15" );
		Instance other = new Instance( "other" ).component( otherComponent );

		// Initialize the messaging
		MessageServerClientFactory factory = new MessageServerClientFactory();

		IAgentClient tomcatClient = factory.createAgentClient();
		tomcatClient.setApplicationName( app1.getName());
		tomcatClient.setMessageServerIp( URL );
		tomcatClient.setRootInstanceName( tomcat.getName());
		StorageMessageProcessor tomcatProcessor = new StorageMessageProcessor();
		tomcatClient.openConnection( tomcatProcessor );

		IAgentClient apacheClient = factory.createAgentClient();
		apacheClient.setApplicationName( app1.getName());
		apacheClient.setMessageServerIp( URL );
		apacheClient.setRootInstanceName( apache.getName());
		StorageMessageProcessor apacheProcessor = new StorageMessageProcessor();
		apacheClient.openConnection( apacheProcessor );

		IAgentClient mysqlClient = factory.createAgentClient();
		mysqlClient.setApplicationName( app1.getName());
		mysqlClient.setMessageServerIp( URL );
		mysqlClient.setRootInstanceName( mysql.getName());
		StorageMessageProcessor mysqlProcessor = new StorageMessageProcessor();
		mysqlClient.openConnection( mysqlProcessor );

		IAgentClient otherClient = factory.createAgentClient();
		otherClient.setApplicationName( app2.getName());
		otherClient.setMessageServerIp( URL );
		otherClient.setRootInstanceName( other.getName());
		StorageMessageProcessor otherProcessor = new StorageMessageProcessor();
		otherClient.openConnection( otherProcessor );

		// OK, let's start.
		// MySQL publishes its exports but nobody is listening.
		mysqlClient.publishExports( mysql );
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( 0, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 0, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());

		// Tomcat and Other are now listening.
		// Let's re-export MySQL.
		otherClient.listenToExportsFromOtherAgents( ListenerCommand.START, other );
		tomcatClient.listenToExportsFromOtherAgents( ListenerCommand.START, tomcat );
		mysqlClient.publishExports( mysql );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( 0, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( MsgCmdImportAdd.class, tomcatProcessor.receivedMessages.get( 0 ).getClass());

		MsgCmdImportAdd msg = (MsgCmdImportAdd) tomcatProcessor.receivedMessages.get( 0 );
		Assert.assertEquals( "MySQL", msg.getComponentOrFacetName());
		Assert.assertEquals( "/mysql", msg.getAddedInstancePath());
		Assert.assertEquals( 2, msg.getExportedVariables().size());
		Assert.assertTrue( msg.getExportedVariables().containsKey( "MySQL.port" ));
		Assert.assertTrue( msg.getExportedVariables().containsKey( "MySQL.ip" ));

		// Other publishes its exports.
		// Tomcat is not supposed to receive it.
		otherClient.publishExports( other );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( 0, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( MsgCmdImportAdd.class, tomcatProcessor.receivedMessages.get( 0 ).getClass());

		// Everybody is listening...
		// Tomcat publishes its exports.
		apacheClient.listenToExportsFromOtherAgents( ListenerCommand.START, apache );
		mysqlClient.listenToExportsFromOtherAgents( ListenerCommand.START, mysql );
		tomcatClient.publishExports( tomcat );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( 1, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( MsgCmdImportAdd.class, apacheProcessor.receivedMessages.get( 0 ).getClass());

		msg = (MsgCmdImportAdd) apacheProcessor.receivedMessages.get( 0 );
		Assert.assertEquals( "Tomcat", msg.getComponentOrFacetName());
		Assert.assertEquals( "/tomcat", msg.getAddedInstancePath());
		Assert.assertEquals( 2, msg.getExportedVariables().size());
		Assert.assertTrue( msg.getExportedVariables().containsKey( "Tomcat.port" ));
		Assert.assertTrue( msg.getExportedVariables().containsKey( "Tomcat.ip" ));

		// MySQL publishes (again) its exports
		mysqlClient.publishExports( mysql );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 2, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( MsgCmdImportAdd.class, tomcatProcessor.receivedMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdImportAdd.class, tomcatProcessor.receivedMessages.get( 1 ).getClass());

		msg = (MsgCmdImportAdd) tomcatProcessor.receivedMessages.get( 1 );
		Assert.assertEquals( "MySQL", msg.getComponentOrFacetName());
		Assert.assertEquals( "/mysql", msg.getAddedInstancePath());
		Assert.assertEquals( 2, msg.getExportedVariables().size());
		Assert.assertTrue( msg.getExportedVariables().containsKey( "MySQL.port" ));
		Assert.assertTrue( msg.getExportedVariables().containsKey( "MySQL.ip" ));

		// MySQL un-publishes its exports
		mysqlClient.unpublishExports( mysql );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 3, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( MsgCmdImportAdd.class, tomcatProcessor.receivedMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdImportAdd.class, tomcatProcessor.receivedMessages.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdImportRemove.class, tomcatProcessor.receivedMessages.get( 2 ).getClass());

		MsgCmdImportRemove newMsg = (MsgCmdImportRemove) tomcatProcessor.receivedMessages.get( 2 );
		Assert.assertEquals( "MySQL", newMsg.getComponentOrFacetName());
		Assert.assertEquals( "/mysql", newMsg.getRemovedInstancePath());

		// MySQL publishes (again) its exports
		// But this time, Tomcat does not listen anymore
		tomcatClient.listenToExportsFromOtherAgents( ListenerCommand.STOP, tomcat );
		mysqlClient.publishExports( mysql );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 3, tomcatProcessor.receivedMessages.size());

		// Shutdown everything
		apacheClient.closeConnection();
		mysqlClient.closeConnection();
		tomcatClient.closeConnection();
		otherClient.closeConnection();
	}


	/**
	 * Makes sure exports requests are exchanged correctly between agents.
	 * @throws Exception
	 */
	public static void testExportsRequestsBetweenAgents() throws Exception {

		// 3 agents (tomcat, mysql, apache) for application app1 and 1 agent (root) for app2.
		// This last one should not receive anything!
		Application app1 = new Application( "app1" );
		Application app2 = new Application( "app2" );

		Component tomcatComponent = new Component( "Tomcat" );
		tomcatComponent.getExportedVariables().put( "Tomcat.ip", "localhost" );
		tomcatComponent.getExportedVariables().put( "Tomcat.port", "8080" );
		tomcatComponent.getImportedVariables().put( "MySQL.port", Boolean.FALSE );
		tomcatComponent.getImportedVariables().put( "MySQL.ip", Boolean.FALSE );
		Instance tomcat = new Instance( "tomcat" ).component( tomcatComponent );

		Component mysqlComponent = new Component( "MySQL" );
		mysqlComponent.getExportedVariables().put( "MySQL.port", "3306" );
		mysqlComponent.getExportedVariables().put( "MySQL.ip", "192.168.1.15" );
		Instance mysql = new Instance( "mysql" ).component( mysqlComponent );

		Component apacheComponent = new Component( "Apache" );
		apacheComponent.getExportedVariables().put( "Apache.ip", "apache.roboconf.net" );
		apacheComponent.getImportedVariables().put( "Tomcat.port", Boolean.FALSE );
		apacheComponent.getImportedVariables().put( "Tomcat.ip", Boolean.FALSE );
		Instance apache = new Instance( "apache" ).component( apacheComponent );

		// This one is a good candidate to receive something when others publish something.
		// Except it is not in the same application.
		Component otherComponent = new Component( "other" );
		apacheComponent.getImportedVariables().put( "Tomcat.port", Boolean.FALSE );
		apacheComponent.getImportedVariables().put( "Tomcat.ip", Boolean.FALSE );
		apacheComponent.getImportedVariables().put( "Mongo.ip", Boolean.FALSE );
		mysqlComponent.getExportedVariables().put( "MySQL.port", "3306" );
		mysqlComponent.getExportedVariables().put( "MySQL.ip", "192.168.1.15" );
		Instance other = new Instance( "other" ).component( otherComponent );

		// Initialize the messaging
		MessageServerClientFactory factory = new MessageServerClientFactory();

		IAgentClient tomcatClient = factory.createAgentClient();
		tomcatClient.setApplicationName( app1.getName());
		tomcatClient.setMessageServerIp( URL );
		tomcatClient.setRootInstanceName( tomcat.getName());
		StorageMessageProcessor tomcatProcessor = new StorageMessageProcessor();
		tomcatClient.openConnection( tomcatProcessor );

		IAgentClient apacheClient = factory.createAgentClient();
		apacheClient.setApplicationName( app1.getName());
		apacheClient.setMessageServerIp( URL );
		apacheClient.setRootInstanceName( apache.getName());
		StorageMessageProcessor apacheProcessor = new StorageMessageProcessor();
		apacheClient.openConnection( apacheProcessor );

		IAgentClient mysqlClient = factory.createAgentClient();
		mysqlClient.setApplicationName( app1.getName());
		mysqlClient.setMessageServerIp( URL );
		mysqlClient.setRootInstanceName( mysql.getName());
		StorageMessageProcessor mysqlProcessor = new StorageMessageProcessor();
		mysqlClient.openConnection( mysqlProcessor );

		IAgentClient otherClient = factory.createAgentClient();
		otherClient.setApplicationName( app2.getName());
		otherClient.setMessageServerIp( URL );
		otherClient.setRootInstanceName( other.getName());
		StorageMessageProcessor otherProcessor = new StorageMessageProcessor();
		otherClient.openConnection( otherProcessor );

		// OK, let's start.
		// Tomcat requests MySQL exports but MySQL is not listening
		tomcatClient.requestExportsFromOtherAgents( tomcat );
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( 0, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 0, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());

		// Now, Other and MySQL are listening.
		// Only MySQL should receive it (Other is in another application).
		otherClient.listenToRequestsFromOtherAgents( ListenerCommand.START, other );
		mysqlClient.listenToRequestsFromOtherAgents( ListenerCommand.START, mysql );
		tomcatClient.requestExportsFromOtherAgents( tomcat );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 0, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, mysqlProcessor.receivedMessages.size());
		Assert.assertEquals( MsgCmdImportRequest.class, mysqlProcessor.receivedMessages.get( 0 ).getClass());

		MsgCmdImportRequest msg = (MsgCmdImportRequest) mysqlProcessor.receivedMessages.get( 0 );
		Assert.assertEquals( "MySQL", msg.getComponentOrFacetName());

		// Now, let's do it again but MySQL stops listening.
		mysqlClient.listenToRequestsFromOtherAgents( ListenerCommand.STOP, mysql );
		tomcatClient.requestExportsFromOtherAgents( tomcat );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 0, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, mysqlProcessor.receivedMessages.size());

		// Other requires exports from others.
		otherClient.requestExportsFromOtherAgents( other );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, apacheProcessor.receivedMessages.size());
		Assert.assertEquals( 0, tomcatProcessor.receivedMessages.size());
		Assert.assertEquals( 0, otherProcessor.receivedMessages.size());
		Assert.assertEquals( 1, mysqlProcessor.receivedMessages.size());

		// Shutdown everything
		apacheClient.closeConnection();
		mysqlClient.closeConnection();
		tomcatClient.closeConnection();
		otherClient.closeConnection();
	}


	/**
	 * Tests exchanges between sibling agents and several variable groups.
	 * @throws Exception
	 */
	public static void testExportsBetweenSiblingAgents() throws Exception {

		// The model
		Application app = new Application( "app" );

		Component component = new Component( "Component" );
		component.getExportedVariables().put( "Component.ip", "localhost" );
		component.getExportedVariables().put( "Component.port", "8080" );
		component.getExportedVariables().put( "facet.data", "hello" );

		component.getImportedVariables().put( "Component.port", Boolean.TRUE );
		component.getImportedVariables().put( "Component.ip", Boolean.TRUE );
		component.getImportedVariables().put( "facet.data", Boolean.TRUE );

		Instance instance1 = new Instance( "instance1" ).component( component );
		Instance instance2 = new Instance( "instance2" ).component( component );

		// Initialize the messaging
		MessageServerClientFactory factory = new MessageServerClientFactory();

		IAgentClient instanceClient1 = factory.createAgentClient();
		instanceClient1.setApplicationName( app.getName());
		instanceClient1.setMessageServerIp( URL );
		instanceClient1.setRootInstanceName( instance1.getName());
		StorageMessageProcessor instanceProcessor1 = new StorageMessageProcessor();
		instanceClient1.openConnection( instanceProcessor1 );

		IAgentClient instanceClient2 = factory.createAgentClient();
		instanceClient2.setApplicationName( app.getName());
		instanceClient2.setMessageServerIp( URL );
		instanceClient2.setRootInstanceName( instance2.getName());
		StorageMessageProcessor instanceProcessor2 = new StorageMessageProcessor();
		instanceClient2.openConnection( instanceProcessor2 );

		// OK, let's start.
		// Instance1 is alone.
		instanceClient1.requestExportsFromOtherAgents( instance1 );
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, instanceProcessor1.receivedMessages.size());
		Assert.assertEquals( 0, instanceProcessor2.receivedMessages.size());

		// Now, instance2 is listening.
		instanceClient2.listenToRequestsFromOtherAgents( ListenerCommand.START, instance2 );
		instanceClient1.requestExportsFromOtherAgents( instance1 );
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, instanceProcessor1.receivedMessages.size());
		Assert.assertEquals( 2, instanceProcessor2.receivedMessages.size());
		Assert.assertEquals( MsgCmdImportRequest.class, instanceProcessor2.receivedMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdImportRequest.class, instanceProcessor2.receivedMessages.get( 1 ).getClass());

		String facet1 = ((MsgCmdImportRequest) instanceProcessor2.receivedMessages.get( 0 )).getComponentOrFacetName();
		String facet2 = ((MsgCmdImportRequest) instanceProcessor2.receivedMessages.get( 1 )).getComponentOrFacetName();
		Assert.assertNotSame( facet1, facet2 );
		Assert.assertTrue( facet1.equals( "Component" ) || facet1.equals( "facet" ));
		Assert.assertTrue( facet2.equals( "Component" ) || facet2.equals( "facet" ));

		// instance1 is now listening
		// instance2 stops listening
		// instance1 should receive the notification it has sent. It will be up to the agent to ignore it.
		instanceClient2.listenToRequestsFromOtherAgents( ListenerCommand.STOP, instance2 );
		instanceClient1.listenToRequestsFromOtherAgents( ListenerCommand.START, instance1 );
		instanceClient1.requestExportsFromOtherAgents( instance1 );
		Thread.sleep( DELAY );

		Assert.assertEquals( 2, instanceProcessor2.receivedMessages.size());
		Assert.assertEquals( 2, instanceProcessor1.receivedMessages.size());
		Assert.assertEquals( MsgCmdImportRequest.class, instanceProcessor1.receivedMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdImportRequest.class, instanceProcessor1.receivedMessages.get( 1 ).getClass());

		facet1 = ((MsgCmdImportRequest) instanceProcessor1.receivedMessages.get( 0 )).getComponentOrFacetName();
		facet2 = ((MsgCmdImportRequest) instanceProcessor1.receivedMessages.get( 1 )).getComponentOrFacetName();
		Assert.assertNotSame( facet1, facet2 );
		Assert.assertTrue( facet1.equals( "Component" ) || facet1.equals( "facet" ));
		Assert.assertTrue( facet2.equals( "Component" ) || facet2.equals( "facet" ));

		// Shutdown everything
		instanceClient1.closeConnection();
		instanceClient2.closeConnection();
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class StorageMessageProcessor extends AbstractMessageProcessor {
		private final List<Message> receivedMessages = new ArrayList<Message> ();

		@Override
		protected void processMessage( Message message ) {
			this.receivedMessages.add( message );
		}
	}
}
