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

package net.roboconf.dm.rest.client.delegates;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ManagementWsException;
import net.roboconf.dm.rest.services.internal.RestApplication;
import net.roboconf.messaging.api.MessagingConstants;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagementWsDelegateTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private static final String REST_URI = "http://localhost:8090";

	private WsClient client;
	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private HttpServer httpServer;



	@After
	public void after() {

		this.manager.stop();
		if( this.httpServer != null )
			this.httpServer.stop();

		if( this.client != null )
			this.client.destroy();
	}


	@Before
	public void before() throws Exception {

		// Create the manager
		this.manager = new Manager();
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		// Create the wrapper and complete configuration
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Prepare the client
		URI uri = UriBuilder.fromUri( REST_URI ).build();
		RestApplication restApp = new RestApplication( this.manager );
		this.httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );

		this.client = new WsClient( REST_URI );
	}


	@Test
	public void testListApplications() throws Exception {

		List<Application> apps = this.client.getManagementDelegate().listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 0, apps.size());

		TestApplication app = new TestApplication();
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), 	new ManagedApplication( app ));

		apps = this.client.getManagementDelegate().listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( app.getName(), receivedApp.getName());
		Assert.assertEquals( app.getDescription(), receivedApp.getDescription());
	}


	@Test
	public void testListApplicationTemplates() throws Exception {

		List<ApplicationTemplate> templates = this.client.getManagementDelegate().listApplicationTemplates();
		Assert.assertNotNull( templates );
		Assert.assertEquals( 0, templates.size());

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		this.managerWrapper.getApplicationTemplates().put( tpl, Boolean.TRUE );

		templates = this.client.getManagementDelegate().listApplicationTemplates();
		Assert.assertNotNull( templates );
		Assert.assertEquals( 1, templates.size());

		ApplicationTemplate receivedTpl = templates.get( 0 );
		Assert.assertEquals( tpl.getName(), receivedTpl.getName());
		Assert.assertEquals( tpl.getDescription(), receivedTpl.getDescription());
		Assert.assertEquals( tpl.getQualifier(), receivedTpl.getQualifier());
	}


	@Test( expected = ManagementWsException.class )
	public void testShutdownApplication_failure() throws Exception {
		this.client.getManagementDelegate().shutdownApplication( "inexisting" );
	}


	@Test
	public void testShutdownApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));
		this.client.getManagementDelegate().shutdownApplication( app.getName());
	}


	@Test( expected = ManagementWsException.class )
	public void testDeleteApplication_failure() throws Exception {
		this.client.getManagementDelegate().deleteApplication( "inexisting" );
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().deleteApplication( app.getName());
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_success() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
		this.client.getManagementDelegate().loadApplicationTemplate( directory.getAbsolutePath());
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_alreadyExisting() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "Legacy LAMP" ).qualifier( "sample" );
		this.managerWrapper.getApplicationTemplates().put( tpl, Boolean.TRUE );
		File directory = TestUtils.findApplicationDirectory( "lamp" );

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());
		try {
			this.client.getManagementDelegate().loadApplicationTemplate( directory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementWsException e ) {
			Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_invalidApplication() throws Exception {

		File directory = new File( "not/existing/file" );
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
		try {
			this.client.getManagementDelegate().loadApplicationTemplate( directory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementWsException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_nullLocation() throws Exception {

		try {
			this.client.getManagementDelegate().loadApplicationTemplate((String) null);
			Assert.fail( "An exception was expected." );

		} catch( ManagementWsException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_fileLocation() throws Exception {

		File targetDirectory = this.folder.newFile( "roboconf_app.zip" );
		try {
			this.client.getManagementDelegate().loadApplicationTemplate( targetDirectory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementWsException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_zip_success() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( directory );

		File targetFile = this.folder.newFile( "roboconf_app.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
		this.client.getManagementDelegate().loadApplicationTemplate( targetFile );
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_zip_alreadyExisting() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "Legacy LAMP" ).qualifier( "sample" );
		this.managerWrapper.getApplicationTemplates().put( tpl, Boolean.TRUE );

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( directory );

		File targetFile = this.folder.newFile( "roboconf_app.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());
		try {
			this.client.getManagementDelegate().loadApplicationTemplate( targetFile );
			Assert.fail( "An exception was expected." );

		} catch( ManagementWsException e ) {
			Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test( expected = IOException.class )
	public void testLoadApplicationTemplate_zip_inexistingZip() throws Exception {
		File targetFile = new File( "not/existing/file" );
		this.client.getManagementDelegate().loadApplicationTemplate( targetFile );
	}


	@Test( expected = IOException.class )
	public void testLoadApplicationTemplate_zip_nullZip() throws Exception {
		this.client.getManagementDelegate().loadApplicationTemplate((File) null );
	}


	@Test( expected = IOException.class )
	public void testLoadApplicationTemplate_zip_zipIsDirectory() throws Exception {
		File targetFile = new File( System.getProperty( "java.io.tmpdir" ));
		this.client.getManagementDelegate().loadApplicationTemplate( targetFile );
	}


	@Test
	public void testLoadApplicationTemplate_zip_invalidZip() throws Exception {

		File targetDirectory = this.folder.newFile( "roboconf_app.zip" );
		try {
			this.client.getManagementDelegate().loadApplicationTemplate( targetDirectory );
			Assert.fail( "An exception was expected." );

		} catch( ManagementWsException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test( expected = ManagementWsException.class )
	public void createApplication_IOException() throws Exception {
		this.client.getManagementDelegate().createApplication( "app", null, null );
	}


	@Test
	public void createApplication_success() throws Exception {

		// Create the template
		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
		this.client.getManagementDelegate().loadApplicationTemplate( directory.getAbsolutePath());
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());

		// Create two applications
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().createApplication( "app1", "Legacy LAMP", "sample" );
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().createApplication( "app2", "Legacy LAMP", "sample" );
		Assert.assertEquals( 2, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testDeleteApplicationTemplate() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
		this.client.getManagementDelegate().loadApplicationTemplate( directory.getAbsolutePath());
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());

		this.client.getManagementDelegate().deleteApplicationTemplate( "Legacy LAMP", "sample" );
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
	}


	@Test( expected = ManagementWsException.class )
	public void testDeleteApplicationTemplate_failure() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
		this.client.getManagementDelegate().loadApplicationTemplate( directory.getAbsolutePath());
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().createApplication( "app1", "Legacy LAMP", "sample" );
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());

		// We cannot delete it, an application is associated with this template
		this.client.getManagementDelegate().deleteApplicationTemplate( "Legacy LAMP", "sample" );
	}


	@Test
	public void testCreateAndDeleteApplication() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
		this.client.getManagementDelegate().loadApplicationTemplate( directory.getAbsolutePath());
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplicationTemplates().size());

		this.client.getManagementDelegate().createApplication( "app1", "Legacy LAMP", "sample" );
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());

		this.client.getManagementDelegate().deleteApplication( "app1" );
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());

		List<ApplicationTemplate> templates = this.client.getManagementDelegate().listApplicationTemplates();
		Assert.assertEquals( 1, templates.size());

		ApplicationTemplate tpl = templates.get( 0 );
		Assert.assertEquals( 0, tpl.getAssociatedApplications().size());

		// No associated application, delete it
		this.client.getManagementDelegate().deleteApplicationTemplate( "Legacy LAMP", "sample" );
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplicationTemplates().size());
	}
}
