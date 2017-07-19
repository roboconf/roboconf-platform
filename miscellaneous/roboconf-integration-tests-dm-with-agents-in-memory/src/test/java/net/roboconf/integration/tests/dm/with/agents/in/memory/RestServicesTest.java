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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer;
import org.ops4j.pax.exam.spi.PaxExamRuntime;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * This test verifies that a client can interact with the DM's REST services.
 * @author Vincent Zurczak - Linagora
 */
public class RestServicesTest extends DmWithAgentInMemoryTest {

	private File karafDirectory;


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


	private void testRestInteractions( String appLocation, String rootUrl, WsClient client  ) throws Exception {

		// Load an application template
		Assert.assertEquals( 0, client.getManagementDelegate().listApplicationTemplates().size());
		client.getManagementDelegate().loadUnzippedApplicationTemplate( appLocation );
		List<ApplicationTemplate> templates = client.getManagementDelegate().listApplicationTemplates();
		Assert.assertEquals( 1, templates.size());

		ApplicationTemplate tpl = templates.get( 0 );
		Assert.assertEquals( "Legacy LAMP", tpl.getName());
		Assert.assertEquals( "1.0.1-SNAPSHOT", tpl.getVersion());

		// Create an application
		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());
		client.getManagementDelegate().createApplication( "app1", tpl.getName(), tpl.getVersion());
		List<Application> apps = client.getManagementDelegate().listApplications();
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( "app1", receivedApp.getName());
		Assert.assertEquals( "Legacy LAMP", receivedApp.getTemplate().getName());
		Assert.assertEquals( "1.0.1-SNAPSHOT", receivedApp.getTemplate().getVersion());

		// Check the JSon serialization
		URI targetUri = URI.create( rootUrl + "/app/app1/instances?instance-path=/Apache%20VM" );
		String s = Utils.readUrlContent( targetUri.toString());;
		Assert.assertEquals(
				"[{\"name\":\"Apache\",\"path\":\"/Apache VM/Apache\",\"status\":\"NOT_DEPLOYED\",\"component\":{\"name\":\"Apache\",\"installer\":\"script\"}}]",
				s );

		// Copy an image in the application's directory
		copyImage();

		// Make sure we can get the image from the server
		String iconsUrl = "http://localhost:" + getCurrentPort() + "/roboconf-icons";
		URL url = new URL( iconsUrl + "/app1/application.png" );
		InputStream in = url.openStream();
		Utils.copyStreamSafely( in, new ByteArrayOutputStream());

		// Make sure getting an invalid icon returns an error
		url = new URL( iconsUrl + "/invalid-app-name/application.png" );
		try {
			in = url.openStream();
			Utils.copyStreamSafely( in, new ByteArrayOutputStream());
			Assert.fail( "An exception was expected here" );

		} catch( Exception e ) {
			// nothing

		} finally {
			Utils.closeQuietly( in );
		}

		// Test the debug resources.
		// Check the connection between the DM and the MQ.
		Assert.assertNotNull( client.getDebugDelegate().checkMessagingConnectionForTheDm( "TEST" ));

		// Define a target and set it as the default for the application
		Assert.assertEquals( 0, client.getTargetWsDelegate().listAllTargets().size());

		String targetId = client.getTargetWsDelegate().createTarget( "id:tid\nhandler: in-memory" );
		Assert.assertNotNull( targetId );

		Assert.assertEquals( 1, client.getTargetWsDelegate().listAllTargets().size());
		client.getTargetWsDelegate().associateTarget( receivedApp, null, targetId, true );

		// Deploy and start the "Apache VM" root instance.
		client.getApplicationDelegate().deployAndStartAll( "app1", "/Apache VM" );
		Thread.sleep( 1000L );

		// Ping the "Apache VM" root instance.
		Assert.assertNotNull( client.getDebugDelegate().checkMessagingConnectionWithAgent( "app1", "/Apache VM", "TEST" ));

		// Diagnose the "Legacy LAMP" application.
		List<Instance> allInstances = client.getApplicationDelegate().listChildrenInstances( "app1", null, true );
		Assert.assertEquals( 6, allInstances.size());

		List<Diagnostic> diags = client.getDebugDelegate().diagnoseApplication( "app1" );
		Assert.assertNotNull( diags );
		Assert.assertEquals( 6, diags.size());
	}


	private void copyImage() throws IOException {

		File rbcfDirectory = new File(
				this.karafDirectory,
				"data/roboconf/applications/app1/" + Constants.PROJECT_DIR_DESC );

		Assert.assertTrue( rbcfDirectory.exists());
		BufferedImage img = new BufferedImage( 100, 100, BufferedImage.TYPE_INT_ARGB );
		File targetFile = new File( rbcfDirectory, "application.png" );
		ImageIO.write( img, "png", targetFile );
	}
}
