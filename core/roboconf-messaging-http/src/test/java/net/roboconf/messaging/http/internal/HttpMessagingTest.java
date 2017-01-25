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

package net.roboconf.messaging.http.internal;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.AbstractMessagingTest;
import net.roboconf.messaging.http.HttpConstants;
import net.roboconf.messaging.http.internal.HttpTestUtils.WebServer;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class HttpMessagingTest extends AbstractMessagingTest {

	private static final int ATTEMPTS = 30;
	private WebServer webServerRunnable;
	private HttpClientFactory factory;


	@Before
	public void registerHttpFactory() throws InterruptedException {

		// Register a new factory
		this.factory = new HttpClientFactory();
		this.factory.setHttpServerIp( HttpConstants.DEFAULT_IP );
		this.factory.setHttpPort( HttpTestUtils.TEST_PORT );

		this.registry = new MessagingClientFactoryRegistry();
		this.registry.addMessagingClientFactory( this.factory );

		// Launch a new web server
		this.webServerRunnable = new WebServer( this.factory );
		Thread webServerThread = new Thread( this.webServerRunnable, "Test for Roboconf HTTP Messaging" );
		webServerThread.start();

		// For diagnostic
		for( int i=0; i<ATTEMPTS; i++ ) {
			if( this.webServerRunnable.isRunning()
					&& this.webServerRunnable.isServerStarted())
				break;

			Thread.sleep( 50 );
		}

		Assert.assertTrue( this.webServerRunnable.isServerStarted());
	}


	@After
	public void stopWebServer() throws IOException, InterruptedException {

		// Stop all the clients
		this.factory.stopAll();

		// Stop the web server
		this.webServerRunnable.stop();

		// For diagnostic
		for( int i=0; i<ATTEMPTS; i++ ) {
			if( this.webServerRunnable.isServerStopped())
				break;

			Thread.sleep( 50 );
		}

		Assert.assertTrue( this.webServerRunnable.isServerStopped());
	}


	@Override
	@Test
	public void testExchangesBetweenTheDmAndOneAgent() throws Exception {
		Assume.assumeTrue( this.webServerRunnable.isRunning());
		super.testExchangesBetweenTheDmAndOneAgent();
	}


	@Override
	@Test
	public void testExchangesBetweenTheDmAndThreeAgents() throws Exception {
		Assume.assumeTrue( this.webServerRunnable.isRunning());
		super.testExchangesBetweenTheDmAndThreeAgents();
	}


	@Override
	@Test
	public void testExportsBetweenAgents() throws Exception {
		Assume.assumeTrue( this.webServerRunnable.isRunning());
		super.testExportsBetweenAgents();
	}


	@Override
	@Test
	public void testExportsRequestsBetweenAgents() throws Exception {
		Assume.assumeTrue( this.webServerRunnable.isRunning());
		super.testExportsRequestsBetweenAgents();
	}


	@Override
	@Test
	public void testExportsBetweenSiblingAgents() throws Exception {
		Assume.assumeTrue( this.webServerRunnable.isRunning());
		super.testExportsBetweenSiblingAgents();
	}


	@Override
	@Test
	public void testPropagateAgentTermination() throws Exception {
		Assume.assumeTrue( this.webServerRunnable.isRunning());
		super.testPropagateAgentTermination();
	}


	@Test
	@Override
	public void test_applicationRegeneration() throws Exception {
		Assume.assumeTrue( this.webServerRunnable.isRunning());
		super.test_applicationRegeneration();
	}


	@Override
	@Test
	public void testDmDebug() throws Exception {
		Assume.assumeTrue( this.webServerRunnable.isRunning());
		super.testDmDebug();
	}


	@Override
	protected long getDelay() {
		return 300;
	}


	@Override
	protected String getMessagingType() {
		return HttpConstants.FACTORY_HTTP;
	}
}
