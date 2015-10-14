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

package net.roboconf.messaging.rabbitmq.internal;

import java.util.List;

import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.internal.client.AbstractMessagingTest;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.processors.AbstractMessageProcessor;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqTest extends AbstractMessagingTest {
	private static boolean rabbitMqIsRunning = false;

	@BeforeClass
	public static void checkRabbitMqIsRunning() throws Exception {
		rabbitMqIsRunning = RabbitMqTestUtils.checkRabbitMqIsRunning();
	}

	@Before
	public void registerRabbitMqFactory() {

		final RabbitMqClientFactory factory = new RabbitMqClientFactory();
		factory.setMessageServerIp(getMessagingIp());
		factory.setMessageServerUsername(getMessagingUsername());
		factory.setMessageServerPassword(getMessagingPassword());
		this.registry.addMessagingClientFactory(factory);
	}


	@After
	public void cleanRabbitMq() throws Exception {

		if( rabbitMqIsRunning ) {
			RabbitMqClientDm client = new RabbitMqClientDm(null, getMessagingIp(), getMessagingUsername(), getMessagingPassword());
			client.openConnection();
			client.deleteMessagingServerArtifacts( new Application( "app", null ));
			client.deleteMessagingServerArtifacts( new Application( "app1", null ));
			client.deleteMessagingServerArtifacts( new Application( "app2", null ));
			client.closeConnection();
		}
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


	@Override
	protected AbstractMessageProcessor<IDmClient> createDmProcessor( final List<Message> dmMessages ) {
		return new AbstractMessageProcessor<IDmClient>( "DM Processor - Test" ) {
			@Override
			protected void processMessage( Message message ) {
				dmMessages.add( message );
			}
		};
	}


	@Override
	protected AbstractMessageProcessor<IAgentClient> createAgentProcessor( final List<Message> agentMessages ) {
		return new AbstractMessageProcessor<IAgentClient>( "Agent Processor - Test" ) {
			@Override
			protected void processMessage( Message message ) {
				agentMessages.add( message );
			}
		};
	}


	private String getMessagingIp() {
		return "localhost";
	}


	private String getMessagingUsername() {
		return "guest";
	}


	private String getMessagingPassword() {
		return "guest";
	}


	@Override
	protected String getMessagingType() {
		return RabbitMqConstants.RABBITMQ_FACTORY_TYPE;
	}
}
