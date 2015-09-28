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

package net.roboconf.messaging.http;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.internal.client.AbstractMessagingTest;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.processors.AbstractMessageProcessor;
import net.roboconf.messaging.http.internal.HttpClient;
import net.roboconf.messaging.http.internal.HttpClientFactory;
import net.roboconf.messaging.http.internal.MessagingWebSocket;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class HttpMessagingTest extends AbstractMessagingTest {

	private static boolean serverIsRunning = false;

	HttpClientFactory factory;

	@Before
	public void registerHttpFactory() {
		MessagingWebSocket.cleanup();
		HttpTestUtils.runWebServer();
		try {
			Thread.sleep(500); //TODO: any way to start webserver quicky (and remove this) ??
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
		serverIsRunning = true;
		factory = new HttpClientFactory();
		factory.setHttpServerIp(HttpConstants.DEFAULT_IP);
		factory.setHttpPort(HttpConstants.DEFAULT_PORT);
		this.registry.addMessagingClientFactory(factory);
	}

	@After
	public void stopWebServer() {
		HttpTestUtils.stopWebServer();
		MessagingWebSocket.cleanup();
		Set<HttpClient> httpClients = HttpClientFactory.getHttpClients();
		for(HttpClient c : httpClients) {
			try {
				c.closeConnection();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
		HttpClientFactory.getHttpClients().clear();
		serverIsRunning = false;
		try {
			Thread.sleep(500); //TODO: any way to stop webserver cleanly (and remove this) ??
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}

	@Override
	@Test
	public void testExchangesBetweenTheDmAndOneAgent() throws Exception {
		Assume.assumeTrue( serverIsRunning );
		super.testExchangesBetweenTheDmAndOneAgent();
	}

	@Override
	@Test
	public void testExchangesBetweenTheDmAndThreeAgents() throws Exception {
		Assume.assumeTrue( serverIsRunning );
		super.testExchangesBetweenTheDmAndThreeAgents();
	}

	@Override
	@Test
	public void testExportsBetweenAgents() throws Exception {
		Assume.assumeTrue( serverIsRunning );
		super.testExportsBetweenAgents();
	}

	@Override
	@Test
	public void testExportsRequestsBetweenAgents() throws Exception {
		Assume.assumeTrue( serverIsRunning );
		super.testExportsRequestsBetweenAgents();
	}

	@Override
	@Test
	public void testExportsBetweenSiblingAgents() throws Exception {
		Assume.assumeTrue( serverIsRunning );
		super.testExportsBetweenSiblingAgents();
	}

	@Override
	@Test
	public void testPropagateAgentTermination() throws Exception {
		Assume.assumeTrue( serverIsRunning );
		super.testPropagateAgentTermination();
	}

	@Override
	@Test
	public void testDmDebug() throws Exception {
		Assume.assumeTrue( serverIsRunning );
		super.testDmDebug();
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

	@Override
	protected String getMessagingType() {
		return HttpConstants.HTTP_FACTORY_TYPE;
	}
}
