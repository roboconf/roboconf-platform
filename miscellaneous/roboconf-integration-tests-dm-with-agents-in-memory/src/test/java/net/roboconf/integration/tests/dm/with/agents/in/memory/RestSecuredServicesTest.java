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

package net.roboconf.integration.tests.dm.with.agents.in.memory;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer;
import org.ops4j.pax.exam.spi.PaxExamRuntime;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * This test verifies that a client can interact with the DM's REST services.
 * @author Vincent Zurczak - Linagora
 */
public class RestSecuredServicesTest extends DmWithAgentInMemoryTest {

	private File karafDirectory;


	@Override
	public Option[] config() throws Exception {

		List<Option> options = getOptionsForInMemoryAsList();
		options.add( editConfigurationFilePut(
				"etc/net.roboconf.dm.rest.services.configuration.cfg",
				"enable-authentication",
				"true" ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.dm.rest.services.configuration.cfg",
				"authentication-realm",
				"karaf" ));

		return ItUtils.asArray( options );
	}


	@Test
	public void run() throws Exception {

		Assume.assumeTrue( RabbitMqTestUtils.checkRabbitMqIsRunning());
		File appDirectory = TestUtils.findApplicationDirectory( "lamp" );

		// Prepare to run an agent distribution
		ExamSystem system = PaxExamRuntime.createServerSystem( config());
		TestContainer container = PaxExamRuntime.createContainer( system );
		Assert.assertEquals( KarafTestContainer.class, container.getClass());

		WsClient client = null;
		try {
			// Start the DM's distribution... and wait... :(
			container.start();
			ItUtils.waitForDmRestServices( getCurrentPort());

			// Find the Karaf directory
			this.karafDirectory = TestUtils.getInternalField( container, "targetFolder", File.class );
			Assert.assertNotNull( this.karafDirectory );

			// Build a REST client
			String rootUrl = "http://localhost:" + getCurrentPort() + "/roboconf-dm";
			client = new WsClient( rootUrl );

			// Perform the checks
			testRestInteractions( appDirectory.getAbsolutePath(), rootUrl, client );

		} finally {
			container.stop();
			if( client != null )
				client.destroy();
		}
	}


	private void testRestInteractions( String appLocation, String rootUrl, WsClient client  )
	throws Exception {

		// Not logged in
		try {
			client.getManagementDelegate().listApplicationTemplates();
			Assert.fail( "An exception was expected, the user is not logged in." );

		} catch( Exception e ) {
			// nothing
		}

		// Log in
		String sessionId = client.getAuthenticationWsDelegate().login( "karaf", "karaf" );
		Assert.assertNotNull( sessionId );
		Assert.assertEquals( sessionId, client.getSessionId());

		// Load an application template
		Assert.assertEquals( 0, client.getManagementDelegate().listApplicationTemplates().size());
		client.getManagementDelegate().loadUnzippedApplicationTemplate( appLocation );
		List<ApplicationTemplate> templates = client.getManagementDelegate().listApplicationTemplates();
		Assert.assertEquals( 1, templates.size());

		ApplicationTemplate tpl = templates.get( 0 );
		Assert.assertEquals( "Legacy LAMP", tpl.getName());
		Assert.assertEquals( "1.0.1-SNAPSHOT", tpl.getVersion());

		// Log out
		client.getAuthenticationWsDelegate().logout( sessionId );
		Assert.assertNull( client.getSessionId());

		// We cannot retrieve anything else
		try {
			client.getManagementDelegate().listApplicationTemplates();
			Assert.fail( "An exception was expected, the user is not logged in." );

		} catch( Exception e ) {
			// nothing
		}
	}
}
