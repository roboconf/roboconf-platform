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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.ops4j.pax.url.mvn.MavenResolver;

import com.sun.jersey.core.header.FormDataContentDisposition;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.internal.client.test.TestClient;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagementResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private ManagementResource resource;
	private TestClient msgClient;


	@After
	public void after() {
		this.manager.stop();
	}


	@Before
	public void before() throws Exception {

		// Create the manager
		this.manager = new Manager();
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		// Create the wrapper and complete configuration
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Get the messaging client
		this.msgClient = (TestClient) this.managerWrapper.getInternalMessagingClient();
		this.msgClient.clearMessages();
		this.resource = new ManagementResource( this.manager );
	}


	@Test
	public void testListApplications() throws Exception {

		// Prepare
		List<Application> apps = this.resource.listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 0, apps.size());

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( new ManagedApplication( app ));

		// Get ALL the applications
		apps = this.resource.listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( app.getName(), receivedApp.getName());
		Assert.assertEquals( app.getDescription(), receivedApp.getDescription());

		// Get the "filter" application
		apps = this.resource.listApplications( "filter" );
		Assert.assertNotNull( apps );
		Assert.assertEquals( 0, apps.size());

		// Get the test application
		apps = this.resource.listApplications( app.getName());
		Assert.assertNotNull( apps );
		Assert.assertEquals( 1, apps.size());

		receivedApp = apps.get( 0 );
		Assert.assertEquals( app.getName(), receivedApp.getName());
		Assert.assertEquals( app.getDescription(), receivedApp.getDescription());

		// Tricky get
		apps = this.resource.listApplications( app.getName() + "0" );
		Assert.assertNotNull( apps );
		Assert.assertEquals( 0, apps.size());
	}


	@Test
	public void testListApplicationTemplates() throws Exception {

		// Prepare
		List<ApplicationTemplate> templates = this.resource.listApplicationTemplates();
		Assert.assertNotNull( templates );
		Assert.assertEquals( 0, templates.size());

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		this.managerWrapper.getApplicationTemplates().put( tpl, Boolean.TRUE );

		// Improve code coverage by setting the log level to finest
		Logger logger = Logger.getLogger( ManagementResource.class.getName());
		LogManager.getLogManager().addLogger( logger );
		logger.setLevel( Level.FINEST );

		// Get ALL the templates
		templates = this.resource.listApplicationTemplates();
		Assert.assertNotNull( templates );
		Assert.assertEquals( 1, templates.size());

		ApplicationTemplate receivedTpl = templates.get( 0 );
		Assert.assertEquals( tpl.getName(), receivedTpl.getName());
		Assert.assertEquals( tpl.getDescription(), receivedTpl.getDescription());
		Assert.assertEquals( tpl.getVersion(), receivedTpl.getVersion());

		// Get the "filter" template
		templates = this.resource.listApplicationTemplates( "filter", null, null );
		Assert.assertNotNull( templates );
		Assert.assertEquals( 0, templates.size());

		// Get the test template with no specific qualifier
		templates = this.resource.listApplicationTemplates( tpl.getName(), null, null );
		Assert.assertNotNull( templates );
		Assert.assertEquals( 1, templates.size());

		receivedTpl = templates.get( 0 );
		Assert.assertEquals( tpl.getName(), receivedTpl.getName());
		Assert.assertEquals( tpl.getDescription(), receivedTpl.getDescription());
		Assert.assertEquals( tpl.getVersion(), receivedTpl.getVersion());

		// Get the test template with the exact qualifier
		templates = this.resource.listApplicationTemplates( tpl.getName(), tpl.getVersion(), null );
		Assert.assertNotNull( templates );
		Assert.assertEquals( 1, templates.size());

		receivedTpl = templates.get( 0 );
		Assert.assertEquals( tpl.getName(), receivedTpl.getName());
		Assert.assertEquals( tpl.getDescription(), receivedTpl.getDescription());
		Assert.assertEquals( tpl.getVersion(), receivedTpl.getVersion());

		// Get the test template with the exact qualifier but no specific name
		templates = this.resource.listApplicationTemplates( null, tpl.getVersion(), null );
		Assert.assertNotNull( templates );
		Assert.assertEquals( 1, templates.size());

		receivedTpl = templates.get( 0 );
		Assert.assertEquals( tpl.getName(), receivedTpl.getName());
		Assert.assertEquals( tpl.getDescription(), receivedTpl.getDescription());
		Assert.assertEquals( tpl.getVersion(), receivedTpl.getVersion());

		// Invalid qualifier
		templates = this.resource.listApplicationTemplates( null, tpl.getVersion() + "2", null );
		Assert.assertNotNull( templates );
		Assert.assertEquals( 0, templates.size());

		// Invalid name
		templates = this.resource.listApplicationTemplates( tpl.getName() + "1", null, null );
		Assert.assertNotNull( templates );
		Assert.assertEquals( 0, templates.size());

		// Right tag
		final String tag = "cool";
		tpl.addTag( tag );

		templates = this.resource.listApplicationTemplates( null, tpl.getVersion(), tag );
		Assert.assertNotNull( templates );
		Assert.assertEquals( 1, templates.size());

		receivedTpl = templates.get( 0 );
		Assert.assertEquals( tpl.getName(), receivedTpl.getName());
		Assert.assertEquals( tpl.getDescription(), receivedTpl.getDescription());
		Assert.assertEquals( tpl.getVersion(), receivedTpl.getVersion());

		// Invalid tag
		templates = this.resource.listApplicationTemplates( null, tpl.getVersion() + "2", "invalid tag" );
		Assert.assertNotNull( templates );
		Assert.assertEquals( 0, templates.size());
	}


	@Test
	public void testShutdownApplication_failure() throws Exception {
		Assert.assertEquals(
				Status.NOT_FOUND.getStatusCode(),
				this.resource.shutdownApplication( "inexisting" ).getStatus());
	}


	@Test
	public void testShutdownApplication_IOException() throws Exception {

		this.msgClient.connected.set( false );
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		this.managerWrapper.addManagedApplication( new ManagedApplication( app ));
		Assert.assertEquals(
				Status.FORBIDDEN.getStatusCode(),
				this.resource.shutdownApplication( app.getName()).getStatus());
	}


	@Test
	public void testShutdownApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		this.managerWrapper.addManagedApplication( new ManagedApplication( app ));
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.shutdownApplication( app.getName()).getStatus());
	}


	@Test
	public void testDeleteApplication_failure() throws Exception {
		Assert.assertEquals(
				Status.NOT_FOUND.getStatusCode(),
				this.resource.deleteApplication( "inexisting" ).getStatus());
	}


	@Test
	public void testDeleteApplication_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		this.managerWrapper.addManagedApplication( new ManagedApplication( app ));
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		Assert.assertEquals( 1, this.resource.listApplications().size());
		Assert.assertEquals(
				Status.FORBIDDEN.getStatusCode(),
				this.resource.deleteApplication( app.getName()).getStatus());

		Assert.assertEquals( 1, this.resource.listApplications().size());
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( new ManagedApplication( app ));

		Assert.assertEquals( 1, this.resource.listApplications().size());
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.deleteApplication( app.getName()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplications().size());
	}


	@Test
	public void testUnzippedLoadApplicationTemplate_success() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.loadUnzippedApplicationTemplate( directory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadUnzippedApplicationTemplate_alreadyExisting() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "Legacy LAMP" ).version( "1.0.1-SNAPSHOT" );
		this.managerWrapper.getApplicationTemplates().put( tpl, Boolean.TRUE );
		File directory = TestUtils.findApplicationDirectory( "lamp" );

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.FORBIDDEN.getStatusCode(),
				this.resource.loadUnzippedApplicationTemplate( directory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadUnzippedApplicationTemplate_invalidApplication() throws Exception {

		File directory = new File( "not/existing/file" );
		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadUnzippedApplicationTemplate( directory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadUnzippedApplicationTemplate_nullLocation() throws Exception {

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadUnzippedApplicationTemplate( null ).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadUnzippedApplicationTemplate_fileLocation() throws Exception {

		File targetDirectory = this.folder.newFile( "roboconf_app.zip" );
		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadUnzippedApplicationTemplate( targetDirectory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testUnzippedLoadApplicationTemplate_failure() throws Exception {

		this.msgClient.connected.set( false );
		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadUnzippedApplicationTemplate( "some-file" ).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadUploadedZippedApplicationTemplate_success() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( directory );

		File targetFile = this.folder.newFile( "roboconf_app.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		InputStream in = null;
		try {
			FormDataContentDisposition fd = FormDataContentDisposition
					.name( targetFile.getName())
					.fileName( targetFile.getName()).build();

			in = new FileInputStream( targetFile );
			Assert.assertEquals(
					Status.OK.getStatusCode(),
					this.resource.loadUploadedZippedApplicationTemplate( in, fd ).getStatus());

			Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());

		} finally {
			Utils.closeQuietly( in );
		}
	}


	@Test
	public void testLoadZippedApplicationTemplate_mvnUrl_withMavenResolver() throws Exception {

		// Create a ZIP file
		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( directory );

		File targetFile = this.folder.newFile( "roboconf_app.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		// Mock a Maven resolver
		MavenResolver mavenResolver = Mockito.mock( MavenResolver.class );
		Mockito.when( mavenResolver.resolve( Mockito.anyString())).thenReturn( targetFile );
		this.resource.setMavenResolver( mavenResolver );

		// No matter the URL, we should not even reach this part
		final String mavenUrl = "mvn:net/roboconf/app/1.0";
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.loadZippedApplicationTemplate( mavenUrl ).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
		Mockito.verify( mavenResolver ).resolve( mavenUrl );
	}


	@Test
	public void testLoadZippedApplicationTemplate_mvnUrl_withoutMavenResolver() throws Exception {

		this.resource.setMavenResolver( null );
		Assert.assertEquals(
				Status.UNAUTHORIZED.getStatusCode(),
				this.resource.loadZippedApplicationTemplate( "mvn:net/roboconf/app/1.0" ).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadZippedApplicationTemplate_nullUrl() throws Exception {

		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadZippedApplicationTemplate( null ).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadZippedApplicationTemplate_notAZip() throws Exception {

		Assert.assertEquals(
				Status.UNAUTHORIZED.getStatusCode(),
				this.resource.loadZippedApplicationTemplate( this.folder.newFile().toURI().toURL().toString()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void createApplication_IOException() throws Exception {

		this.msgClient.connected.set( false );
		Assert.assertEquals(
				Status.UNAUTHORIZED.getStatusCode(),
				this.resource.createApplication( new Application( null )).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void test_complexScenario() throws Exception {

		// Deploy a template
		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.loadUnzippedApplicationTemplate( directory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());

		// Create two applications
		ApplicationTemplate tpl = new ApplicationTemplate( "Legacy LAMP" ).version( "1.0.1-SNAPSHOT" );
		Assert.assertEquals( 0, this.resource.listApplications().size());
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.createApplication( new Application( "app1", tpl )).getStatus());

		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.createApplication( new Application( "app2", tpl )).getStatus());

		Assert.assertEquals( 2, this.resource.listApplications().size());

		// Try to create an already existing application
		Assert.assertEquals(
				Status.FORBIDDEN.getStatusCode(),
				this.resource.createApplication( new Application( "app1", tpl )).getStatus());

		Assert.assertEquals( 2, this.resource.listApplications().size());

		// Try to create an application associated with an unknown template
		ApplicationTemplate tpl2 = new ApplicationTemplate( "Legacy LAMP" ).version( "oops" );
		Assert.assertEquals(
				Status.NOT_FOUND.getStatusCode(),
				this.resource.createApplication( new Application( "app1", tpl2 )).getStatus());

		Assert.assertEquals( 2, this.resource.listApplications().size());

		// Try to create an application associated with an invalid template
		Assert.assertEquals(
				Status.NOT_FOUND.getStatusCode(),
				this.resource.createApplication( new Application( "app1", null )).getStatus());

		Assert.assertEquals( 2, this.resource.listApplications().size());

		// Delete an application
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.deleteApplication( "app1" ).getStatus());

		Assert.assertEquals( 1, this.resource.listApplications().size());

		// Try to delete the template
		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.FORBIDDEN.getStatusCode(),
				this.resource.deleteApplicationTemplate( tpl.getName(), tpl.getVersion()).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());

		// Delete the second application
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.deleteApplication( "app2" ).getStatus());

		Assert.assertEquals( 0, this.resource.listApplications().size());

		// Delete an invalid template
		Assert.assertEquals(
				Status.NOT_FOUND.getStatusCode(),
				this.resource.deleteApplicationTemplate( tpl.getName(), "invalid" ).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());

		// Delete the template
		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.deleteApplicationTemplate( tpl.getName(), tpl.getVersion()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}
}
