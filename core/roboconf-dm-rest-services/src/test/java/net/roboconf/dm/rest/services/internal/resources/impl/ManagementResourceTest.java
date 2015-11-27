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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import junit.framework.Assert;
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
import net.roboconf.dm.rest.services.internal.resources.IManagementResource;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.internal.client.test.TestClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.core.header.FormDataContentDisposition;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagementResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private IManagementResource resource;
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

		List<Application> apps = this.resource.listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 0, apps.size());

		TestApplication app = new TestApplication();
		this.managerWrapper.getNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app ));

		apps = this.resource.listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( app.getName(), receivedApp.getName());
		Assert.assertEquals( app.getDescription(), receivedApp.getDescription());
	}


	@Test
	public void testListApplicationTemplates() throws Exception {

		List<ApplicationTemplate> templates = this.resource.listApplicationTemplates();
		Assert.assertNotNull( templates );
		Assert.assertEquals( 0, templates.size());

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		this.managerWrapper.getApplicationTemplates().put( tpl, Boolean.TRUE );

		templates = this.resource.listApplicationTemplates();
		Assert.assertNotNull( templates );
		Assert.assertEquals( 1, templates.size());

		ApplicationTemplate receivedTpl = templates.get( 0 );
		Assert.assertEquals( tpl.getName(), receivedTpl.getName());
		Assert.assertEquals( tpl.getDescription(), receivedTpl.getDescription());
		Assert.assertEquals( tpl.getQualifier(), receivedTpl.getQualifier());
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
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));
		Assert.assertEquals(
				Status.FORBIDDEN.getStatusCode(),
				this.resource.shutdownApplication( app.getName()).getStatus());
	}


	@Test
	public void testShutdownApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));
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
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));
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
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));

		Assert.assertEquals( 1, this.resource.listApplications().size());
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.deleteApplication( app.getName()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplications().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_success() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.OK.getStatusCode(),
				this.resource.loadApplicationTemplate( directory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_alreadyExisting() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "Legacy LAMP" ).qualifier( "sample" );
		this.managerWrapper.getApplicationTemplates().put( tpl, Boolean.TRUE );
		File directory = TestUtils.findApplicationDirectory( "lamp" );

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.FORBIDDEN.getStatusCode(),
				this.resource.loadApplicationTemplate( directory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_invalidApplication() throws Exception {

		File directory = new File( "not/existing/file" );
		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadApplicationTemplate( directory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_nullLocation() throws Exception {

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadApplicationTemplate( null ).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_localPath_fileLocation() throws Exception {

		File targetDirectory = this.folder.newFile( "roboconf_app.zip" );
		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadApplicationTemplate( targetDirectory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void testLoadApplicationTemplate_zip_success() throws Exception {

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
					this.resource.loadApplicationTemplate( in, fd ).getStatus());

			Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());

		} finally {
			Utils.closeQuietly( in );
		}
	}


	@Test
	public void testLoadApplicationTemplate_zip_failure() throws Exception {

		this.msgClient.connected.set( false );
		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
		Assert.assertEquals(
				Status.NOT_ACCEPTABLE.getStatusCode(),
				this.resource.loadApplicationTemplate( "some-file" ).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}


	@Test
	public void createApplication_IOException() throws Exception {

		this.msgClient.connected.set( false );
		Assert.assertEquals(
				Status.FORBIDDEN.getStatusCode(),
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
				this.resource.loadApplicationTemplate( directory.getAbsolutePath()).getStatus());

		Assert.assertEquals( 1, this.resource.listApplicationTemplates().size());

		// Create two applications
		ApplicationTemplate tpl = new ApplicationTemplate( "Legacy LAMP" ).qualifier( "sample" );
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
		ApplicationTemplate tpl2 = new ApplicationTemplate( "Legacy LAMP" ).qualifier( "oops" );
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
				this.resource.deleteApplicationTemplate( tpl.getName(), tpl.getQualifier()).getStatus());

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
				this.resource.deleteApplicationTemplate( tpl.getName(), tpl.getQualifier()).getStatus());

		Assert.assertEquals( 0, this.resource.listApplicationTemplates().size());
	}
}
