/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ManagementException;
import net.roboconf.dm.rest.services.internal.RestApplication;
import net.roboconf.messaging.MessagingConstants;

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

		this.manager = new Manager();
		this.manager.setMessagingFactoryType( MessagingConstants.FACTORY_TEST );
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setConfigurationDirectoryLocation( this.folder.newFolder().getAbsolutePath());
		this.manager.start();

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
		this.manager.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null ));

		apps = this.client.getManagementDelegate().listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( app.getName(), receivedApp.getName());
		Assert.assertEquals( app.getQualifier(), receivedApp.getQualifier());
	}


	@Test( expected = ManagementException.class )
	public void testShutdownApplication_failure() throws Exception {
		this.client.getManagementDelegate().shutdownApplication( "inexisting" );
	}


	@Test
	public void testShutdownApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		this.manager.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));
		this.client.getManagementDelegate().shutdownApplication( app.getName());
	}


	@Test( expected = ManagementException.class )
	public void testDeleteApplication_failure() throws Exception {
		this.client.getManagementDelegate().deleteApplication( "inexisting" );
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		this.manager.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().deleteApplication( app.getName());
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_success() throws Exception {

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().loadApplication( directory.getAbsolutePath());
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_alreadyExisting() throws Exception {

		Application app = new Application( "Legacy LAMP" );
		this.manager.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));
		File directory = TestUtils.findTestFile( "/lamp" );

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
		try {
			this.client.getManagementDelegate().loadApplication( directory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_invalidApplication() throws Exception {

		File directory = new File( "not/existing/file" );
		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
		try {
			this.client.getManagementDelegate().loadApplication( directory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_nullLocation() throws Exception {

		try {
			this.client.getManagementDelegate().loadApplication((String) null);
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_fileLocation() throws Exception {

		File targetDirectory = this.folder.newFile( "roboconf_app.zip" );
		try {
			this.client.getManagementDelegate().loadApplication( targetDirectory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_zip_success() throws Exception {

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( directory );

		File targetFile = this.folder.newFile( "roboconf_app.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().loadApplication( targetFile );
		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_zip_alreadyExisting() throws Exception {

		Application app = new Application( "Legacy LAMP" );
		this.manager.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( directory );

		File targetFile = this.folder.newFile( "roboconf_app.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
		try {
			this.client.getManagementDelegate().loadApplication( targetFile );
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 1, this.client.getManagementDelegate().listApplications().size());
	}


	@Test( expected = IOException.class )
	public void testLoadApplication_zip_inexistingZip() throws Exception {
		File targetFile = new File( "not/existing/file" );
		this.client.getManagementDelegate().loadApplication( targetFile );
	}


	@Test( expected = IOException.class )
	public void testLoadApplication_zip_nullZip() throws Exception {
		this.client.getManagementDelegate().loadApplication((File) null );
	}


	@Test( expected = IOException.class )
	public void testLoadApplication_zip_zipIsDirectory() throws Exception {
		File targetFile = new File( System.getProperty( "java.io.tmpdir" ));
		this.client.getManagementDelegate().loadApplication( targetFile );
	}


	@Test
	public void testLoadApplication_zip_invalidZip() throws Exception {

		File targetDirectory = this.folder.newFile( "roboconf_app.zip" );
		try {
			this.client.getManagementDelegate().loadApplication( targetDirectory );
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
	}
}
