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

package net.roboconf.messaging.internal.client.rabbitmq;

import java.io.IOException;
import java.util.List;

import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.internal.AbstractMessagingTest;
import net.roboconf.messaging.internal.RabbitMqTestUtils;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.processors.AbstractMessageProcessorAgent;
import net.roboconf.messaging.processors.AbstractMessageProcessorDm;

import org.junit.Assume;
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
	protected AbstractMessageProcessorDm createDmProcessor( final List<Message> dmMessages ) {
		return new AbstractMessageProcessorDm( MessagingConstants.FACTORY_RABBIT_MQ ) {
			@Override
			protected void processMessage( Message message ) {
				dmMessages.add( message );
			}

			@Override
			protected void openConnection( IDmClient newMessagingClient ) throws IOException {
				newMessagingClient.openConnection();
			}
		};
	}


	@Override
	protected AbstractMessageProcessorAgent createAgentProcessor( final List<Message> agentMessages, final String appName, final String rootName ) {
		return new AbstractMessageProcessorAgent( MessagingConstants.FACTORY_RABBIT_MQ ) {
			@Override
			protected void processMessage( Message message ) {
				agentMessages.add( message );
			}

			@Override
			protected void openConnection( IAgentClient newMessagingClient ) throws IOException {
				newMessagingClient.setApplicationName( appName );
				newMessagingClient.setRootInstanceName( rootName );
				newMessagingClient.openConnection();
			}

		};
	}
}
