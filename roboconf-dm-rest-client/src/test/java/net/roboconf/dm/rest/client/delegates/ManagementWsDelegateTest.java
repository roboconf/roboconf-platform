/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.internal.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ManagementException;
import net.roboconf.dm.rest.client.test.RestTestUtils;
import net.roboconf.messaging.client.MessageServerClientFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagementWsDelegateTest extends JerseyTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Override
	protected AppDescriptor configure() {
		return RestTestUtils.buildTestDescriptor();
	}


	@Override
    public TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }


	@Before
	public void resetManager() {
		Manager.INSTANCE.cleanUpAll();
		Manager.INSTANCE.getAppNameToManagedApplication().clear();

		Manager.INSTANCE.setIaasResolver( new TestIaasResolver());
		Manager.INSTANCE.setMessagingClientFactory( new DmMessageServerClientFactory());
	}


	@Test
	public void testListApplications() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		List<Application> apps = client.getManagementDelegate().listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 0, apps.size());

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null ));

		apps = client.getManagementDelegate().listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( app.getName(), receivedApp.getName());
		Assert.assertEquals( app.getQualifier(), receivedApp.getQualifier());
	}


	@Test( expected = ManagementException.class )
	public void testShutdownApplication_failure() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		client.getManagementDelegate().shutdownApplication( "inexisting" );
	}


	@Test
	public void testShutdownApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		WsClient client = RestTestUtils.buildWsClient();
		client.getManagementDelegate().shutdownApplication( app.getName());
	}


	@Test( expected = ManagementException.class )
	public void testDeleteApplication_failure() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		client.getManagementDelegate().deleteApplication( "inexisting" );
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 1, client.getManagementDelegate().listApplications().size());
		client.getManagementDelegate().deleteApplication( app.getName());
		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testDeleteApplication_notConnected() throws Exception {

		Manager.INSTANCE.setMessagingClientFactory( new MessageServerClientFactory());
		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		WsClient client = RestTestUtils.buildWsClient();
		try {
			client.getManagementDelegate().deleteApplication( app.getName());
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), e.getResponseStatus());
		}
	}


	@Test
	public void testLoadApplication_localPath_success() throws Exception {

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());

		client.getManagementDelegate().loadApplication( directory.getAbsolutePath());
		Assert.assertEquals( 1, client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_alreadyExisting() throws Exception {

		Application app = new Application( "Legacy LAMP" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));
		File directory = TestUtils.findTestFile( "/lamp" );

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 1, client.getManagementDelegate().listApplications().size());

		try {
			client.getManagementDelegate().loadApplication( directory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 1, client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_invalidApplication() throws Exception {

		File directory = new File( "/not/existing/file" );
		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());

		try {
			client.getManagementDelegate().loadApplication( directory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_nullLocation() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		try {
			client.getManagementDelegate().loadApplication((String) null);
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_localPath_fileLocation() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		File targetDirectory = this.folder.newFile( "roboconf_app.zip" );
		try {
			client.getManagementDelegate().loadApplication( targetDirectory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_zip_success() throws Exception {

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( directory );

		File targetFile = this.folder.newFile( "roboconf_app.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());

		client.getManagementDelegate().loadApplication( targetFile );
		Assert.assertEquals( 1, client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_zip_alreadyExisting() throws Exception {

		Application app = new Application( "Legacy LAMP" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( directory );

		File targetFile = this.folder.newFile( "roboconf_app.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 1, client.getManagementDelegate().listApplications().size());

		try {
			client.getManagementDelegate().loadApplication( targetFile );
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 1, client.getManagementDelegate().listApplications().size());
	}


	@Test( expected = IOException.class )
	public void testLoadApplication_zip_inexistingZip() throws Exception {

		File targetFile = new File( "/not/existing/file" );
		WsClient client = RestTestUtils.buildWsClient();
		client.getManagementDelegate().loadApplication( targetFile );
	}


	@Test( expected = IOException.class )
	public void testLoadApplication_zip_nullZip() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		client.getManagementDelegate().loadApplication((File) null );
	}


	@Test( expected = IOException.class )
	public void testLoadApplication_zip_zipIsDirectory() throws Exception {

		File targetFile = new File( System.getProperty( "java.io.tmpdir" ));
		WsClient client = RestTestUtils.buildWsClient();
		client.getManagementDelegate().loadApplication( targetFile );
	}


	@Test
	public void testLoadApplication_zip_invalidZip() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		File targetDirectory = this.folder.newFile( "roboconf_app.zip" );
		try {
			client.getManagementDelegate().loadApplication( targetDirectory );
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), e.getResponseStatus());
		}

		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());
	}


	@Test
	public void testLoadApplication_notConnected() throws Exception {

		Manager.INSTANCE.setMessagingClientFactory( new MessageServerClientFactory());
		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());

		WsClient client = RestTestUtils.buildWsClient();
		try {
			client.getManagementDelegate().loadApplication( directory.getAbsolutePath());
			Assert.fail( "An exception was expected." );

		} catch( ManagementException e ) {
			Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), e.getResponseStatus());
		}
	}
}
