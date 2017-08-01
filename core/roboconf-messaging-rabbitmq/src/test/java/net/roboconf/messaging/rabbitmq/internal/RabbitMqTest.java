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

package net.roboconf.messaging.rabbitmq.internal;

import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_PASSWORD;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_USERNAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.messaging.api.business.IClient;
import net.roboconf.messaging.api.business.ListenerCommand;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.internal.client.AbstractMessagingTest;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqTest extends AbstractMessagingTest {

	private static boolean rabbitMqIsRunning = false;


	@BeforeClass
	public static void checkRabbitMqIsRunning() throws Exception {
		rabbitMqIsRunning = RabbitMqTestUtils.checkRabbitMqIsRunning();
	}


	@Override
	protected long getDelay() {
		return 700;
	}


	@Before
	public void registerRabbitMqFactory() {

		final RabbitMqClientFactory factory = new RabbitMqClientFactory();
		factory.configuration = getMessagingConfiguration();
		this.registry.addMessagingClientFactory( factory );
	}


	@After
	public void cleanRabbitMq() throws Exception {

		if( rabbitMqIsRunning ) {
			RabbitMqClient client = new RabbitMqClient(
					null,
					getMessagingConfiguration(),
					RecipientKind.DM );

			client.openConnection();
			for( String domain : Arrays.asList( null, "domain0", "domain1", "domain2", "domain" )) {
				client.setOwnerProperties( RecipientKind.DM, domain, null, null );
				client.deleteMessagingServerArtifacts( new Application( "app", null ));
				client.deleteMessagingServerArtifacts( new Application( "app1", null ));
				client.deleteMessagingServerArtifacts( new Application( "app2", null ));

				client.channel.exchangeDelete( RabbitMqUtils.buildExchangeNameForTheDm( domain ));
				client.channel.exchangeDelete( RabbitMqUtils.buildExchangeNameForInterApp( domain ));
			}

			client.closeConnection();
		}
	}


	/**
	 * @return the messaging configuration to use during the tests
	 */
	protected Map<String,String> getMessagingConfiguration() {

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, getMessagingIp());
		configuration.put( RABBITMQ_SERVER_USERNAME, getMessagingUsername());
		configuration.put( RABBITMQ_SERVER_PASSWORD, getMessagingPassword());

		return configuration;
	}


	@Override
	@Test
	public void testExchangesBetweenTheDmAndOneAgent() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testExchangesBetweenTheDmAndOneAgent();
	}


	@Override
	@Test
	public void testExchangesBetweenTheDmAndThreeAgents() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testExchangesBetweenTheDmAndThreeAgents();
	}


	@Override
	@Test
	public void testExportsBetweenAgents() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testExportsBetweenAgents();
	}


	@Override
	@Test
	public void testExportsRequestsBetweenAgents() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testExportsRequestsBetweenAgents();
	}


	@Override
	@Test
	public void testExportsBetweenSiblingAgents() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testExportsBetweenSiblingAgents();
	}


	@Override
	@Test
	public void testPropagateAgentTermination() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testPropagateAgentTermination();
	}


	@Override
	@Test
	public void testDmDebug() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testDmDebug();
	}


	@Override
	@Test
	public void testExternalExports_withTwoApplications() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testExternalExports_withTwoApplications();
	}


	@Test
	@Override
	public void testExternalExports_twoApplicationsAndTheDm_verifyAgentTerminationPropagation()
	throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.testExternalExports_twoApplicationsAndTheDm_verifyAgentTerminationPropagation();
	}


	@Test
	@Override
	public void test_applicationRegeneration() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );
		super.test_applicationRegeneration();
	}


	/*
	 * This test is specific to RabbiMQ, as it is one of the rare
	 * implementations to actually use "domains".
	 */
	@Test
	public void testExchangesBetweenDmAndOneAgent_with_twoDomains() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );

		// Initialize everything
		Application app = new Application( "app", new ApplicationTemplate());
		Instance rootInstance = new Instance( "root" );

		final int finalSize = 3;
		final ReconfigurableClientDm[] dmClients = new ReconfigurableClientDm[ finalSize ];
		final ReconfigurableClientAgent[] agentClients = new ReconfigurableClientAgent[ finalSize ];
		final Map<IClient,List<Message>> map = new LinkedHashMap<> ();

		for( int i=0; i<finalSize; i++ ) {

			List<Message> dmMessages = new ArrayList<> ();
			dmClients[ i ] = new ReconfigurableClientDm();
			dmClients[ i ].setRegistry( this.registry );
			dmClients[ i ].setDomain( "domain" + i );
			dmClients[ i ].associateMessageProcessor( createDmProcessor( dmMessages ));
			dmClients[ i ].switchMessagingType( getMessagingType());
			map.put( dmClients[ i ], dmMessages );
			this.clients.add( dmClients[ i ]);

			List<Message> agentMessages = new ArrayList<> ();
			agentClients[ i ] = new ReconfigurableClientAgent();
			agentClients[ i ].setRegistry( this.registry );
			agentClients[ i ].setDomain( "domain" + i );
			agentClients[ i ].associateMessageProcessor( createAgentProcessor( agentMessages ));
			agentClients[ i ].setApplicationName( app.getName());
			agentClients[ i ].setScopedInstancePath( "/" + rootInstance.getName());
			agentClients[ i ].setExternalMapping( app.getExternalExports());
			agentClients[ i ].switchMessagingType( getMessagingType());
			map.put( agentClients[ i ], agentMessages );
			this.clients.add( agentClients[ i ]);
		}

		// No message yet
		Thread.sleep( getDelay());
		for( Map.Entry<IClient,List<Message>> entry : map.entrySet())
			Assert.assertEquals( entry.getKey().getDomain(), 0, entry.getValue().size());

		// Domain 0: the agent is already listening to the DM.
		List<Message> agentMessages0 = map.get( agentClients[ 0 ]);
		List<Message> dmMessages0 = map.get( dmClients[ 0 ]);

		dmClients[ 0 ].sendMessageToAgent( app, rootInstance, new MsgCmdSetScopedInstance( rootInstance ));
		Thread.sleep( getDelay());
		Assert.assertEquals( 1, agentMessages0.size());
		Assert.assertEquals( MsgCmdSetScopedInstance.class, agentMessages0.get( 0 ).getClass());

		agentClients[ 0 ].listenToTheDm( ListenerCommand.START );
		dmClients[ 0 ].sendMessageToAgent( app, rootInstance, new MsgCmdRemoveInstance( rootInstance ));
		Thread.sleep( getDelay());
		Assert.assertEquals( 2, agentMessages0.size());
		Assert.assertEquals( MsgCmdSetScopedInstance.class, agentMessages0.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRemoveInstance.class, agentMessages0.get( 1 ).getClass());

		// Other domains were not notified
		for( int i=1; i<finalSize; i++ ) {
			Assert.assertEquals( 0, map.get( dmClients[ i ]).size());
			Assert.assertEquals( 0, map.get( agentClients[ i ]).size());
		}

		// The agent sends a message to the DM
		Assert.assertEquals( 0, dmMessages0.size());
		agentClients[ 0 ].sendMessageToTheDm( new MsgNotifHeartbeat( app.getName(), rootInstance, "192.168.1.45" ));
		Thread.sleep( getDelay());
		Assert.assertEquals( 0, dmMessages0.size());

		dmClients[ 0 ].listenToAgentMessages( app, ListenerCommand.START );
		agentClients[ 0 ].sendMessageToTheDm( new MsgNotifMachineDown( app.getName(), rootInstance ));
		Thread.sleep( getDelay());
		Assert.assertEquals( 1, dmMessages0.size());
		Assert.assertEquals( MsgNotifMachineDown.class, dmMessages0.get( 0 ).getClass());

		// The DM sends another message
		dmClients[ 0 ].sendMessageToAgent( app, rootInstance, new MsgCmdChangeInstanceState( rootInstance, InstanceStatus.DEPLOYED_STARTED ));
		Thread.sleep( getDelay());
		Assert.assertEquals( 3, agentMessages0.size());
		Assert.assertEquals( MsgCmdSetScopedInstance.class, agentMessages0.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdRemoveInstance.class, agentMessages0.get( 1 ).getClass());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, agentMessages0.get( 2 ).getClass());

		// Verify other domains
		for( int i=1; i<finalSize; i++ ) {
			Assert.assertEquals( 0, map.get( dmClients[ i ]).size());
			Assert.assertEquals( 0, map.get( agentClients[ i ]).size());
		}

		// Let's deploy an agent from another domain
		List<Message> agentMessages2 = map.get( agentClients[ 2 ]);

		dmClients[ 2 ].sendMessageToAgent( app, rootInstance, new MsgCmdChangeInstanceState( rootInstance, InstanceStatus.DEPLOYED_STARTED ));
		Thread.sleep( getDelay());
		Assert.assertEquals( 1, agentMessages2.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, agentMessages2.get( 0 ).getClass());

		// Verify other domains
		Assert.assertEquals( 1, map.get( dmClients[ 0 ]).size());
		Assert.assertEquals( 3, map.get( agentClients[ 0 ]).size());

		Assert.assertEquals( 0, map.get( dmClients[ 1 ]).size());
		Assert.assertEquals( 0, map.get( agentClients[ 1 ]).size());

		Assert.assertEquals( 0, map.get( dmClients[ 2 ]).size());
		Assert.assertEquals( 1, map.get( agentClients[ 2 ]).size());

		// The agent stops listening the DM
		agentClients[ 0 ].listenToTheDm( ListenerCommand.STOP );
		Thread.sleep( getDelay());

		// The agent is not listening to the DM anymore.
		// With RabbitMQ, the next invocation will result in a NO_ROUTE error in the channel.
		dmClients[ 0 ].sendMessageToAgent( app, rootInstance, new MsgCmdChangeInstanceState( rootInstance, InstanceStatus.DEPLOYED_STARTED ));
		Thread.sleep( getDelay());
		Assert.assertEquals( 3, agentMessages0.size());
		Thread.sleep( getDelay());
		Assert.assertEquals( 3, agentMessages0.size());
		Thread.sleep( getDelay());
		Assert.assertEquals( 3, agentMessages0.size());

		// Verify all the domains
		Assert.assertEquals( 1, map.get( dmClients[ 0 ]).size());
		Assert.assertEquals( 3, map.get( agentClients[ 0 ]).size());

		Assert.assertEquals( 0, map.get( dmClients[ 1 ]).size());
		Assert.assertEquals( 0, map.get( agentClients[ 1 ]).size());

		Assert.assertEquals( 0, map.get( dmClients[ 2 ]).size());
		Assert.assertEquals( 1, map.get( agentClients[ 2 ]).size());

		// The DM stops listening the agent
		dmClients[ 0 ].listenToAgentMessages( app, ListenerCommand.STOP );
		agentClients[ 0 ].sendMessageToTheDm( new MsgNotifHeartbeat( app.getName(), rootInstance, "192.168.1.47" ));
		Thread.sleep( getDelay());
		Assert.assertEquals( 1, dmMessages0.size());
		Thread.sleep( getDelay());
		Assert.assertEquals( 1, dmMessages0.size());
		Thread.sleep( getDelay());
		Assert.assertEquals( 1, dmMessages0.size());

		// Verify other domains
		Assert.assertEquals( 1, map.get( dmClients[ 0 ]).size());
		Assert.assertEquals( 3, map.get( agentClients[ 0 ]).size());

		Assert.assertEquals( 0, map.get( dmClients[ 1 ]).size());
		Assert.assertEquals( 0, map.get( agentClients[ 1 ]).size());

		Assert.assertEquals( 0, map.get( dmClients[ 2 ]).size());
		Assert.assertEquals( 1, map.get( agentClients[ 2 ]).size());
	}


	private String getMessagingIp() {
		return "localhost";
	}


	private String getMessagingUsername() {
		return RabbitMqTestUtils.GUEST;
	}


	private String getMessagingPassword() {
		return RabbitMqTestUtils.GUEST;
	}


	@Override
	protected String getMessagingType() {
		return RabbitMqConstants.FACTORY_RABBITMQ;
	}
}
