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
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.internal.client.rabbitmq.RabbitMqClientDm;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;
import net.roboconf.messaging.processors.AbstractMessageProcessorAgent;
import net.roboconf.messaging.processors.AbstractMessageProcessorDm;

import org.junit.AfterClass;

/**
 * This class defines messaging tests, independently of the implementation.
 * <p>
 * They should work with RabbitMQ or any other messaging server that is
 * supported by Roboconf.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractMessagingTest {

	public static final long DELAY = MessagingConstants.MESSAGE_POLLING_PERIOD + 10;

	private static final String URL = "localhost";
	private static final String USER = "guest";
	private static final String PWD = "guest";


	@AfterClass
	public static void cleanRabbitMq() throws Exception {

		RabbitMqClientDm client = new RabbitMqClientDm();
		client.setParameters( URL, USER, PWD );
		client.openConnection();
		client.deleteMessagingServerArtifacts( new Application( "app" ));
		client.deleteMessagingServerArtifacts( new Application( "app1" ));
		client.deleteMessagingServerArtifacts( new Application( "app2" ));
		client.closeConnection();
	}



	/**
	 * Tests synchronous exchanges between the DM and an agent.
	 * @throws Exception
	 */
	public void testExchangesBetweenTheDmAndOneAgent() throws Exception {

		// Initialize everything
		Application app = new Application( "app" );
		Instance rootInstance = new Instance( "root" );

		List<Message> dmMessages = new ArrayList<Message> ();
		List<Message> agentMessages = new ArrayList<Message> ();

		AbstractMessageProcessorDm dmProcessor = createDmProcessor( dmMessages );
		dmProcessor.start();
		dmProcessor.switchMessagingClient( URL, USER, PWD );

		AbstractMessageProcessorAgent agentProcessor = createAgentProcessor( agentMessages, app.getName(), rootInstance.getName());
		agentProcessor.start();
		agentProcessor.switchMessagingClient( URL, USER, PWD );

		Thread.sleep( DELAY );
		Assert.assertNotNull( dmProcessor.getMessagingClient());
		Assert.assertNotNull( agentProcessor.getMessagingClient());

		// No message yet
		Assert.assertTrue( agentProcessor.hasNoMessage());
		Assert.assertTrue( dmProcessor.hasNoMessage());
		Assert.assertEquals( 0, dmMessages.size());
		Assert.assertEquals( 0, agentMessages.size());

		// The agent is not listening to the DM.
		// With RabbitMQ, the next invocation will result in a NO_ROUTE error in the channel.
		dmProcessor.getMessagingClient().sendMessageToAgent( app, rootInstance, new MsgCmdSetRootInstance( rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, agentMessages.size());

		agentProcessor.getMessagingClient().listenToTheDm( ListenerCommand.START );
		dmProcessor.getMessagingClient().sendMessageToAgent( app, rootInstance, new MsgCmdRemoveInstance( rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, agentMessages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, agentMessages.get( 0 ).getClass());

		// The agent sends a message to the DM
		Assert.assertEquals( 0, dmMessages.size());
		agentProcessor.getMessagingClient().sendMessageToTheDm( new MsgNotifHeartbeat( app.getName(), rootInstance, "192.168.1.45" ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, dmMessages.size());

		dmProcessor.getMessagingClient().listenToAgentMessages( app, ListenerCommand.START );
		agentProcessor.getMessagingClient().sendMessageToTheDm( new MsgNotifMachineDown( app.getName(), rootInstance ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, dmMessages.size());
		Assert.assertEquals( MsgNotifMachineDown.class, dmMessages.get( 0 ).getClass());

		// The DM sends another message
		dmProcessor.getMessagingClient().sendMessageToAgent( app, rootInstance, new MsgCmdChangeInstanceState( rootInstance, InstanceStatus.DEPLOYED_STARTED ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 2, agentMessages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, agentMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, agentMessages.get( 1 ).getClass());

		// The agent stops listening the DM
		agentProcessor.getMessagingClient().listenToTheDm( ListenerCommand.STOP );

		// The agent is not listening to the DM anymore.
		// With RabbitMQ, the next invocation will result in a NO_ROUTE error in the channel.
		dmProcessor.getMessagingClient().sendMessageToAgent( app, rootInstance, new MsgCmdChangeInstanceState( rootInstance, InstanceStatus.DEPLOYED_STARTED ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 2, agentMessages.size());
		Thread.sleep( DELAY );
		Assert.assertEquals( 2, agentMessages.size());
		Thread.sleep( DELAY );
		Assert.assertEquals( 2, agentMessages.size());

		// The DM stops listening the agent
		dmProcessor.getMessagingClient().listenToAgentMessages( app, ListenerCommand.STOP );
		agentProcessor.getMessagingClient().sendMessageToTheDm( new MsgNotifHeartbeat( app.getName(), rootInstance, "192.168.1.47" ));
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, dmMessages.size());
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, dmMessages.size());
		Thread.sleep( DELAY );
		Assert.assertEquals( 1, dmMessages.size());

		// Disconnect the agent
		agentProcessor.stopProcessor();

		// Clean artifacts
		dmProcessor.getMessagingClient().deleteMessagingServerArtifacts( app );

		// Disconnect the DM
		dmProcessor.stopProcessor();
	}


	/**
	 * Makes sure messages go to the right agent.
	 * <p>
	 * This is about messages routing.
	 * </p>
	 *
	 * @throws Exception
	 */
	public void testExchangesBetweenTheDmAndThreeAgents() throws Exception {

		// Initialize everything
		// 1 DM, 2 agents (root1 and root2) for application app1 and 1 agent (root) for app2.
		Application app1 = new Application( "app1" );
		Application app2 = new Application( "app2" );
		Instance app1_root1 = new Instance( "root1" );
		Instance app1_root2 = new Instance( "root2" );
		Instance app2_root = new Instance( "root" );

		List<Message> dmMessages = new ArrayList<Message> ();
		AbstractMessageProcessorDm dmProcessor = createDmProcessor( dmMessages );
		dmProcessor.start();
		dmProcessor.switchMessagingClient( URL, USER, PWD );

		List<Message> agentMessages_11 = new ArrayList<Message> ();
		AbstractMessageProcessorAgent agentProcessorApp1_1 = createAgentProcessor( agentMessages_11, app1.getName(), app1_root1.getName());
		agentProcessorApp1_1.start();
		agentProcessorApp1_1.switchMessagingClient( URL, USER, PWD );

		List<Message> agentMessages_12 = new ArrayList<Message> ();
		AbstractMessageProcessorAgent agentProcessorApp1_2 = createAgentProcessor( agentMessages_12, app1.getName(), app1_root2.getName());
		agentProcessorApp1_2.start();
		agentProcessorApp1_2.switchMessagingClient( URL, USER, PWD );

		List<Message> agentMessages_2 = new ArrayList<Message> ();
		AbstractMessageProcessorAgent agentProcessorApp2 = createAgentProcessor( agentMessages_2, app2.getName(), app2_root.getName());
		agentProcessorApp2.start();
		agentProcessorApp2.switchMessagingClient( URL, USER, PWD );

		Thread.sleep( DELAY );
		Assert.assertNotNull( dmProcessor.getMessagingClient());
		Assert.assertNotNull( agentProcessorApp1_1.getMessagingClient());
		Assert.assertNotNull( agentProcessorApp1_2.getMessagingClient());
		Assert.assertNotNull( agentProcessorApp2.getMessagingClient());

		// Everybody starts listening...
		agentProcessorApp1_1.getMessagingClient().listenToTheDm( ListenerCommand.START );
		agentProcessorApp1_2.getMessagingClient().listenToTheDm( ListenerCommand.START );
		agentProcessorApp2.getMessagingClient().listenToTheDm( ListenerCommand.START );

		// The DM sends messages
		dmProcessor.getMessagingClient().sendMessageToAgent( app1, app1_root1, new MsgCmdSetRootInstance( app1_root1 ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app2, app2_root, new MsgCmdSetRootInstance( app2_root ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app1, app1_root2, new MsgCmdSetRootInstance( app1_root2 ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app2, app2_root, new MsgCmdRemoveInstance( app2_root ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app2, app2_root, new MsgCmdChangeInstanceState( app2_root, InstanceStatus.DEPLOYED_STOPPED ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app1, app1_root2, new MsgCmdRemoveInstance( app1_root2 ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app1, app1_root2, new MsgCmdRemoveInstance( app1_root2 ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app1, app1_root2, new MsgCmdChangeInstanceState( app1_root2, InstanceStatus.NOT_DEPLOYED ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app1, app1_root1, new MsgCmdRemoveInstance( app1_root1 ));
		dmProcessor.getMessagingClient().sendMessageToAgent( app1, app1_root1, new MsgCmdSetRootInstance( app1_root1 ));

		// Check what was received
		Thread.sleep( DELAY );

		Assert.assertEquals( 3, agentMessages_11.size());
		Assert.assertEquals( MsgCmdSetRootInstance.class, agentMessages_11.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRemoveInstance.class, agentMessages_11.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdSetRootInstance.class, agentMessages_11.get( 2 ).getClass());

		Assert.assertEquals( 4, agentMessages_12.size());
		Assert.assertEquals( MsgCmdSetRootInstance.class, agentMessages_12.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRemoveInstance.class, agentMessages_12.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdRemoveInstance.class, agentMessages_12.get( 2 ).getClass());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, agentMessages_12.get( 3 ).getClass());

		Assert.assertEquals( 3, agentMessages_2.size());
		Assert.assertEquals( MsgCmdSetRootInstance.class, agentMessages_2.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRemoveInstance.class, agentMessages_2.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, agentMessages_2.get( 2 ).getClass());

		// Disconnect the agents
		agentProcessorApp1_1.stopProcessor();
		agentProcessorApp1_2.stopProcessor();
		agentProcessorApp2.stopProcessor();

		// Clean artifacts
		dmProcessor.getMessagingClient().deleteMessagingServerArtifacts( app1 );

		// Disconnect the DM
		dmProcessor.stopProcessor();
	}


	/**
	 * Makes sure exports are exchanged correctly between agents.
	 * @throws Exception
	 */
	public void testExportsBetweenAgents() throws Exception {

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
		List<Message> tomcatMessages = new ArrayList<Message> ();
		AbstractMessageProcessorAgent tomcatProcessor = createAgentProcessor( tomcatMessages, app1.getName(), tomcat.getName());
		tomcatProcessor.start();
		tomcatProcessor.switchMessagingClient( URL, USER, PWD );

		List<Message> apacheMessages = new ArrayList<Message> ();
		AbstractMessageProcessorAgent apacheProcessor = createAgentProcessor( apacheMessages, app1.getName(), apache.getName());
		apacheProcessor.start();
		apacheProcessor.switchMessagingClient( URL, USER, PWD );

		List<Message> mySqlMessages = new ArrayList<Message> ();
		AbstractMessageProcessorAgent mySqlProcessor = createAgentProcessor( mySqlMessages, app1.getName(), mysql.getName());
		mySqlProcessor.start();
		mySqlProcessor.switchMessagingClient( URL, USER, PWD );

		List<Message> otherMessages = new ArrayList<Message> ();
		AbstractMessageProcessorAgent otherProcessor = createAgentProcessor( otherMessages, app2.getName(), other.getName());
		otherProcessor.start();
		otherProcessor.switchMessagingClient( URL, USER, PWD );

		Thread.sleep( DELAY );
		Assert.assertNotNull( tomcatProcessor.getMessagingClient());
		Assert.assertNotNull( apacheProcessor.getMessagingClient());
		Assert.assertNotNull( mySqlProcessor.getMessagingClient());
		Assert.assertNotNull( otherProcessor.getMessagingClient());

		// OK, let's start.
		// MySQL publishes its exports but nobody is listening.
		mySqlProcessor.getMessagingClient().publishExports( mysql );
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, apacheMessages.size());
		Assert.assertEquals( 0, tomcatMessages.size());
		Assert.assertEquals( 0, otherMessages.size());

		// Tomcat and Other are now listening.
		// Let's re-export MySQL.
		otherProcessor.getMessagingClient().listenToExportsFromOtherAgents( ListenerCommand.START, other );
		tomcatProcessor.getMessagingClient().listenToExportsFromOtherAgents( ListenerCommand.START, tomcat );
		mySqlProcessor.getMessagingClient().publishExports( mysql );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, apacheMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, tomcatMessages.size());
		Assert.assertEquals( MsgCmdAddImport.class, tomcatMessages.get( 0 ).getClass());

		MsgCmdAddImport msg = (MsgCmdAddImport) tomcatMessages.get( 0 );
		Assert.assertEquals( "MySQL", msg.getComponentOrFacetName());
		Assert.assertEquals( "/mysql", msg.getAddedInstancePath());
		Assert.assertEquals( 2, msg.getExportedVariables().size());
		Assert.assertTrue( msg.getExportedVariables().containsKey( "MySQL.port" ));
		Assert.assertTrue( msg.getExportedVariables().containsKey( "MySQL.ip" ));

		// Let's publish an unknown facet. Nobody should receive it.
		mySqlProcessor.getMessagingClient().publishExports( mysql, "an-unknown-facet-or-component-name" );

		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, apacheMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, tomcatMessages.size());

		// Other publishes its exports.
		// Tomcat is not supposed to receive it.
		otherProcessor.getMessagingClient().publishExports( other );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, apacheMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, tomcatMessages.size());
		Assert.assertEquals( MsgCmdAddImport.class, tomcatMessages.get( 0 ).getClass());

		// Everybody is listening...
		// Tomcat publishes its exports.
		apacheProcessor.getMessagingClient().listenToExportsFromOtherAgents( ListenerCommand.START, apache );
		mySqlProcessor.getMessagingClient().listenToExportsFromOtherAgents( ListenerCommand.START, mysql );
		tomcatProcessor.getMessagingClient().publishExports( tomcat );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, tomcatMessages.size());
		Assert.assertEquals( 1, apacheMessages.size());
		Assert.assertEquals( MsgCmdAddImport.class, apacheMessages.get( 0 ).getClass());

		msg = (MsgCmdAddImport) apacheMessages.get( 0 );
		Assert.assertEquals( "Tomcat", msg.getComponentOrFacetName());
		Assert.assertEquals( "/tomcat", msg.getAddedInstancePath());
		Assert.assertEquals( 2, msg.getExportedVariables().size());
		Assert.assertTrue( msg.getExportedVariables().containsKey( "Tomcat.port" ));
		Assert.assertTrue( msg.getExportedVariables().containsKey( "Tomcat.ip" ));

		// MySQL publishes (again) its exports
		mySqlProcessor.getMessagingClient().publishExports( mysql );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, apacheMessages.size());
		Assert.assertEquals( 2, tomcatMessages.size());
		Assert.assertEquals( MsgCmdAddImport.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdAddImport.class, tomcatMessages.get( 1 ).getClass());

		msg = (MsgCmdAddImport) tomcatMessages.get( 1 );
		Assert.assertEquals( "MySQL", msg.getComponentOrFacetName());
		Assert.assertEquals( "/mysql", msg.getAddedInstancePath());
		Assert.assertEquals( 2, msg.getExportedVariables().size());
		Assert.assertTrue( msg.getExportedVariables().containsKey( "MySQL.port" ));
		Assert.assertTrue( msg.getExportedVariables().containsKey( "MySQL.ip" ));

		// MySQL un-publishes its exports
		mySqlProcessor.getMessagingClient().unpublishExports( mysql );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, apacheMessages.size());
		Assert.assertEquals( 3, tomcatMessages.size());
		Assert.assertEquals( MsgCmdAddImport.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdAddImport.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdRemoveImport.class, tomcatMessages.get( 2 ).getClass());

		MsgCmdRemoveImport newMsg = (MsgCmdRemoveImport) tomcatMessages.get( 2 );
		Assert.assertEquals( "MySQL", newMsg.getComponentOrFacetName());
		Assert.assertEquals( "/mysql", newMsg.getRemovedInstancePath());

		// MySQL publishes (again) its exports
		// But this time, Tomcat does not listen anymore
		tomcatProcessor.getMessagingClient().listenToExportsFromOtherAgents( ListenerCommand.STOP, tomcat );
		mySqlProcessor.getMessagingClient().publishExports( mysql );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, apacheMessages.size());
		Assert.assertEquals( 3, tomcatMessages.size());

		// Shutdown everything
		apacheProcessor.stopProcessor();
		mySqlProcessor.stopProcessor();
		tomcatProcessor.stopProcessor();
		otherProcessor.stopProcessor();
	}


	/**
	 * Makes sure exports requests are exchanged correctly between agents.
	 * @throws Exception
	 */
	public void testExportsRequestsBetweenAgents() throws Exception {

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

		List<Message> tomcatMessages = new ArrayList<Message> ();
		AbstractMessageProcessorAgent tomcatProcessor = createAgentProcessor( tomcatMessages, app1.getName(), tomcat.getName());
		tomcatProcessor.start();
		tomcatProcessor.switchMessagingClient( URL, USER, PWD );

		List<Message> apacheMessages = new ArrayList<Message> ();
		AbstractMessageProcessorAgent apacheProcessor = createAgentProcessor( apacheMessages, app1.getName(), apache.getName());
		apacheProcessor.start();
		apacheProcessor.switchMessagingClient( URL, USER, PWD );

		List<Message> mySqlMessages = new ArrayList<Message> ();
		AbstractMessageProcessorAgent mySqlProcessor = createAgentProcessor( mySqlMessages, app1.getName(), mysql.getName());
		mySqlProcessor.start();
		mySqlProcessor.switchMessagingClient( URL, USER, PWD );

		List<Message> otherMessages = new ArrayList<Message> ();
		AbstractMessageProcessorAgent otherProcessor = createAgentProcessor( otherMessages, app2.getName(), other.getName());
		otherProcessor.start();
		otherProcessor.switchMessagingClient( URL, USER, PWD );

		Thread.sleep( DELAY );
		Assert.assertNotNull( tomcatProcessor.getMessagingClient());
		Assert.assertNotNull( apacheProcessor.getMessagingClient());
		Assert.assertNotNull( mySqlProcessor.getMessagingClient());
		Assert.assertNotNull( otherProcessor.getMessagingClient());

		// OK, let's start.
		// Tomcat requests MySQL exports but MySQL is not listening
		tomcatProcessor.getMessagingClient().requestExportsFromOtherAgents( tomcat );
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, mySqlMessages.size());
		Assert.assertEquals( 0, apacheMessages.size());
		Assert.assertEquals( 0, tomcatMessages.size());
		Assert.assertEquals( 0, otherMessages.size());

		// Now, Other and MySQL are listening.
		// Only MySQL should receive it (Other is in another application).
		otherProcessor.getMessagingClient().listenToRequestsFromOtherAgents( ListenerCommand.START, other );
		mySqlProcessor.getMessagingClient().listenToRequestsFromOtherAgents( ListenerCommand.START, mysql );
		tomcatProcessor.getMessagingClient().requestExportsFromOtherAgents( tomcat );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, apacheMessages.size());
		Assert.assertEquals( 0, tomcatMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, mySqlMessages.size());
		Assert.assertEquals( MsgCmdRequestImport.class, mySqlMessages.get( 0 ).getClass());

		MsgCmdRequestImport msg = (MsgCmdRequestImport) mySqlMessages.get( 0 );
		Assert.assertEquals( "MySQL", msg.getComponentOrFacetName());

		// Now, let's do it again but MySQL stops listening.
		mySqlProcessor.getMessagingClient().listenToRequestsFromOtherAgents( ListenerCommand.STOP, mysql );
		tomcatProcessor.getMessagingClient().requestExportsFromOtherAgents( tomcat );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, apacheMessages.size());
		Assert.assertEquals( 0, tomcatMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, mySqlMessages.size());

		// Other requires exports from others.
		otherProcessor.getMessagingClient().requestExportsFromOtherAgents( other );
		Thread.sleep( DELAY );

		Assert.assertEquals( 0, apacheMessages.size());
		Assert.assertEquals( 0, tomcatMessages.size());
		Assert.assertEquals( 0, otherMessages.size());
		Assert.assertEquals( 1, mySqlMessages.size());

		// Shutdown everything
		apacheProcessor.stopProcessor();
		mySqlProcessor.stopProcessor();
		tomcatProcessor.stopProcessor();
		otherProcessor.stopProcessor();
	}


	/**
	 * Tests exchanges between sibling agents and several variable groups.
	 * @throws Exception
	 */
	public void testExportsBetweenSiblingAgents() throws Exception {

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
		List<Message> messages1 = new ArrayList<Message> ();
		AbstractMessageProcessorAgent processor1 = createAgentProcessor( messages1, app.getName(), instance1.getName());
		processor1.start();
		processor1.switchMessagingClient( URL, USER, PWD );

		List<Message> messages2 = new ArrayList<Message> ();
		AbstractMessageProcessorAgent processor2 = createAgentProcessor( messages2, app.getName(), instance2.getName());
		processor2.start();
		processor2.switchMessagingClient( URL, USER, PWD );

		Thread.sleep( DELAY );
		Assert.assertNotNull( processor1.getMessagingClient());
		Assert.assertNotNull( processor2.getMessagingClient());

		// OK, let's start.
		// Instance1 is alone.
		processor1.getMessagingClient().requestExportsFromOtherAgents( instance1 );
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, messages1.size());
		Assert.assertEquals( 0, messages2.size());

		// Now, instance2 is listening.
		processor2.getMessagingClient().listenToRequestsFromOtherAgents( ListenerCommand.START, instance2 );
		processor1.getMessagingClient().requestExportsFromOtherAgents( instance1 );
		Thread.sleep( DELAY );
		Assert.assertEquals( 0, messages1.size());
		Assert.assertEquals( 2, messages2.size());
		Assert.assertEquals( MsgCmdRequestImport.class, messages2.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRequestImport.class, messages2.get( 1 ).getClass());

		String facet1 = ((MsgCmdRequestImport) messages2.get( 0 )).getComponentOrFacetName();
		String facet2 = ((MsgCmdRequestImport) messages2.get( 1 )).getComponentOrFacetName();
		Assert.assertNotSame( facet1, facet2 );
		Assert.assertTrue( facet1.equals( "Component" ) || facet1.equals( "facet" ));
		Assert.assertTrue( facet2.equals( "Component" ) || facet2.equals( "facet" ));

		// instance1 is now listening
		// instance2 stops listening
		// instance1 should receive the notification it has sent. It will be up to the agent to ignore it.
		processor2.getMessagingClient().listenToRequestsFromOtherAgents( ListenerCommand.STOP, instance2 );
		processor1.getMessagingClient().listenToRequestsFromOtherAgents( ListenerCommand.START, instance1 );
		processor1.getMessagingClient().requestExportsFromOtherAgents( instance1 );
		Thread.sleep( DELAY );

		Assert.assertEquals( 2, messages2.size());
		Assert.assertEquals( 2, messages1.size());
		Assert.assertEquals( MsgCmdRequestImport.class, messages1.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRequestImport.class, messages1.get( 1 ).getClass());

		facet1 = ((MsgCmdRequestImport) messages1.get( 0 )).getComponentOrFacetName();
		facet2 = ((MsgCmdRequestImport) messages1.get( 1 )).getComponentOrFacetName();
		Assert.assertNotSame( facet1, facet2 );
		Assert.assertTrue( facet1.equals( "Component" ) || facet1.equals( "facet" ));
		Assert.assertTrue( facet2.equals( "Component" ) || facet2.equals( "facet" ));

		// Shutdown everything
		processor1.stopProcessor();
		processor2.stopProcessor();
	}


	protected abstract AbstractMessageProcessorDm createDmProcessor( List<Message> agentMessages );
	protected abstract AbstractMessageProcessorAgent createAgentProcessor( List<Message> agentMessages, String appName, String rootName );
}
