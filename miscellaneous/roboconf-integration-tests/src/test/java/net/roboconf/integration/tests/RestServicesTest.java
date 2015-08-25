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

package net.roboconf.integration.tests;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.UriUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.integration.probes.AbstractTest;
import net.roboconf.integration.probes.DmWithAgentInMemoryTest;
import net.roboconf.integration.tests.internal.RoboconfPaxRunner;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * This test verifies that a client can connect to the DM's websocket.
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class RestServicesTest extends DmWithAgentInMemoryTest {

	private static final String APP_LOCATION = "my.app.location";
	private static final String ROOT_URL = "http://localhost:8181/roboconf-dm";
	private static final String ICONS_URL = "http://localhost:8181/roboconf-icons";

	private WsClient client;



	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( AbstractTest.class );
		probe.addTest( DmWithAgentInMemoryTest.class );
		probe.addTest( InMemoryTargetResolver.class );
		probe.addTest( TestUtils.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		File resourcesDirectory = TestUtils.findApplicationDirectory( "lamp" );
		String appLocation = resourcesDirectory.getAbsolutePath();
		String jerseyVersion = MavenUtils.getArtifactVersion( "com.sun.jersey", "jersey-client" );

		return OptionUtils.combine(
				super.config(),

				mavenBundle()
					.groupId( "com.sun.jersey" )
					.artifactId( "jersey-client" )
					.version( jerseyVersion )
					.start(),

				mavenBundle()
					.groupId( "net.roboconf" )
					.artifactId( "roboconf-dm-rest-client" )
					.version( getRoboconfVersion())
					.start(),

				systemProperty( APP_LOCATION )
					.value( appLocation ));
	}


	@Before
	public void resetManager() throws Exception {

		// Configure the manager
		configureManagerForInMemoryUsage();

		// Initialize a new client
		this.client = new WsClient( ROOT_URL );

		// Wait for the REST services to be online.
		// By default, these tests only wait for the manager to be available. We must in addition,
		// be sure that the REST services are online. The most simple solution is to wait for the
		// applications listing to work.
		URI targetUri = UriUtils.urlToUri( ROOT_URL + "/applications" );
		for( int i=0; i<10; i++ ) {
			Thread.sleep( 1000 );
			String s = TestUtils.readUriContent( targetUri );
			if( "[]".equals( s ))
				break;
		}
	}


	@After
	public void destroyClient() {
		if( this.client != null )
			this.client.destroy();
	}


	@Override
	@Test
	public void run() throws Exception {

		String appLocation = System.getProperty( APP_LOCATION );
		Assert.assertNotNull( appLocation );
		Assert.assertTrue( new File( appLocation ).exists());

		// Load an application template
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
		this.client.getManagementDelegate().loadApplicationTemplate( appLocation );
		List<ApplicationTemplate> templates = this.client.getManagementDelegate().listApplicationTemplates();
		Assert.assertEquals( 1, templates.size());

		ApplicationTemplate tpl = templates.get( 0 );
		Assert.assertEquals( "Legacy LAMP", tpl.getName());
		Assert.assertEquals( "sample", tpl.getQualifier());

		// Create an application
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().createApplication( "app1", tpl.getName(), tpl.getQualifier());
		List<Application> apps = this.client.getManagementDelegate().listApplications();
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( "app1", receivedApp.getName());
		Assert.assertEquals( "Legacy LAMP", receivedApp.getTemplate().getName());
		Assert.assertEquals( "sample", receivedApp.getTemplate().getQualifier());

		// Check the JSon serialization
		URI targetUri = URI.create( ROOT_URL + "/app/app1/children?instance-path=/Apache%20VM" );
		String s = TestUtils.readUriContent( targetUri );
		Assert.assertEquals(
				"[{\"name\":\"Apache\",\"path\":\"/Apache VM/Apache\",\"status\":\"NOT_DEPLOYED\",\"component\":{\"name\":\"Apache\",\"installer\":\"puppet\"}}]",
				s );

		// Copy an image in the application's directory
		Application originalApp = this.manager.findApplicationByName( "app1" );
		Assert.assertNotNull( originalApp );
		File descDir = new File( originalApp.getDirectory(), Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.exists());

		BufferedImage img = new BufferedImage( 100, 100, BufferedImage.TYPE_INT_ARGB );
		File targetFile = new File( descDir, "application.png" );
		ImageIO.write( img, "png", targetFile );

		// Make sure we can get the image from the server
		URL url = new URL( ICONS_URL + "/app1/application.png" );
		InputStream in = null;
		try {
			in = url.openStream();
			Utils.copyStream( in, new ByteArrayOutputStream());

		} finally {
			Utils.closeQuietly( in );
		}

		// Make sure getting an invalid icon returns an error
		url = new URL( ICONS_URL + "/invalid-app-name/application.png" );
		in = null;
		try {
			in = url.openStream();
			Utils.copyStream( in, new ByteArrayOutputStream());
			Assert.fail( "An exception was expected here" );

		} catch( Exception e ) {
			// nothing

		} finally {
			Utils.closeQuietly( in );
		}

		// Test the debug resources.
		// Check the connection between the DM and the MQ.
		Assert.assertNotNull( this.client.getDebugDelegate().checkMessagingConnectionForTheDm( "TEST" ));

		// Deploy and start the "Apache VM" root instance.
		this.client.getApplicationDelegate().deployAndStartAll( "app1", "/Apache VM" );
		Thread.sleep( 1000L );

		// Ping the "Apache VM" root instance.
		Assert.assertNotNull( this.client.getDebugDelegate().checkMessagingConnectionWithAgent( "app1", "/Apache VM", "TEST" ));

		// Diagnose the "Legacy LAMP" application.
		List<Instance> allInstances = this.client.getApplicationDelegate().listChildrenInstances( "app1", null, true );
		Assert.assertEquals( 6, allInstances.size());

		List<Diagnostic> diags = this.client.getDebugDelegate().diagnoseApplication( "app1" );
		Assert.assertNotNull( diags );
		Assert.assertEquals( 6, diags.size());
	}
}
