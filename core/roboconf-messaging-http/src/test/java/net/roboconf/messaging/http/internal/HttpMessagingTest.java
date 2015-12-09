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

package net.roboconf.messaging.http.internal;

import java.io.IOException;

import net.roboconf.messaging.api.internal.client.AbstractMessagingTest;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.internal.HttpTestUtils.WebServer;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class HttpMessagingTest extends AbstractMessagingTest {

	private static WebServer webServerRunnable;


	@Before
	public void registerHttpFactory() throws InterruptedException {

		// Launch a new web server
		webServerRunnable = new WebServer();
		Thread webServerThread = new Thread( webServerRunnable, "Test for Roboconf HTTP Messaging" );
		webServerThread.start();

		for( int i=0; i<15; i++ ) {
			if( webServerRunnable.isRunning()
					&& webServerRunnable.isServerStarted())
				break;

			Thread.sleep( 50 );
		}

		// Register a new factory
		final HttpClientFactory factory = new HttpClientFactory();
		factory.setHttpServerIp( HttpConstants.DEFAULT_IP );
		factory.setHttpPort( HttpTestUtils.TEST_PORT );
		this.registry.addMessagingClientFactory( factory );

		// Clean remaining artifacts
		HttpClientFactory.reset();
	}


	@After
	public void stopWebServer() throws IOException, InterruptedException {

		webServerRunnable.stop();
	}


	@Override
	@Test
	public void testExchangesBetweenTheDmAndOneAgent() throws Exception {
		Assume.assumeTrue( webServerRunnable.isRunning());
		super.testExchangesBetweenTheDmAndOneAgent();
	}


	@Override
	@Test
	public void testExchangesBetweenTheDmAndThreeAgents() throws Exception {
		Assume.assumeTrue( webServerRunnable.isRunning());
		super.testExchangesBetweenTheDmAndThreeAgents();
	}


	@Override
	@Test
	public void testExportsBetweenAgents() throws Exception {
		Assume.assumeTrue( webServerRunnable.isRunning());
		super.testExportsBetweenAgents();
	}


	@Override
	@Test
	public void testExportsRequestsBetweenAgents() throws Exception {
		Assume.assumeTrue( webServerRunnable.isRunning());
		super.testExportsRequestsBetweenAgents();
	}


	@Override
	@Test
	public void testExportsBetweenSiblingAgents() throws Exception {
		Assume.assumeTrue( webServerRunnable.isRunning());
		super.testExportsBetweenSiblingAgents();
	}


	@Override
	@Test
	public void testPropagateAgentTermination() throws Exception {
		Assume.assumeTrue( webServerRunnable.isRunning());
		super.testPropagateAgentTermination();
	}


	@Override
	@Test
	public void testDmDebug() throws Exception {
		Assume.assumeTrue( webServerRunnable.isRunning());
		super.testDmDebug();
	}


	@Override
	protected long getDelay() {
		return 100;
	}


	@Override
	protected String getMessagingType() {
		return HttpConstants.FACTORY_HTTP;
	}
}
