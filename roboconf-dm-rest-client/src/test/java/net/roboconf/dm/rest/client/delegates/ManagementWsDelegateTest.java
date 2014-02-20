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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Application.ApplicationStatus;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ManagementException;
import net.roboconf.dm.rest.client.mocks.helper.PropertyManager;
import net.roboconf.dm.rest.client.test.RestTestUtils;

import org.junit.Test;

import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagementWsDelegateTest extends JerseyTest {

	@Override
	protected AppDescriptor configure() {
		return RestTestUtils.buildTestDescriptor();
	}

	@Override
    public TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

	@Test
	public void testApplications() throws Exception {

		PropertyManager.INSTANCE.reset();
		WsClient client = RestTestUtils.buildWsClient();
		List<Application> apps = client.getManagementDelegate().listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 0, apps.size());

		PropertyManager.INSTANCE.loadApplications();
		apps = client.getManagementDelegate().listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 2, apps.size());

		Application app = apps.get( 0 );
		Assert.assertEquals( PropertyManager.APP_1, app.getName());
		Assert.assertNotNull( app.getDescription());
		Assert.assertEquals( "v1", app.getQualifier());
		Assert.assertEquals( ApplicationStatus.STOPPED, app.getStatus());

		app = apps.get( 1 );
		Assert.assertEquals( PropertyManager.APP_2, app.getName());
		Assert.assertNull( app.getDescription());
		Assert.assertEquals( "v1", app.getQualifier());
		Assert.assertEquals( ApplicationStatus.STOPPED, app.getStatus());

		client.getManagementDelegate().startApplication( PropertyManager.APP_1 );
		app = client.getManagementDelegate().getApplicationByName( PropertyManager.APP_1 );
		Assert.assertNotNull( app );
		Assert.assertEquals( PropertyManager.APP_1, app.getName());
		Assert.assertEquals( ApplicationStatus.STARTED, app.getStatus());

		client.getManagementDelegate().stopApplication( PropertyManager.APP_1 );
		app = client.getManagementDelegate().getApplicationByName( PropertyManager.APP_1 );
		Assert.assertNotNull( app );
		Assert.assertEquals( PropertyManager.APP_1, app.getName());
		Assert.assertEquals( ApplicationStatus.STOPPED, app.getStatus());

		client.getManagementDelegate().deleteApplication( PropertyManager.APP_1 );
		app = client.getManagementDelegate().getApplicationByName( PropertyManager.APP_1 );
		Assert.assertNull( app );

		apps = client.getManagementDelegate().listApplications();
		Assert.assertNotNull( apps );
		Assert.assertEquals( 1, apps.size());

		app = apps.get( 0 );
		Assert.assertEquals( PropertyManager.APP_2, app.getName());

		try {
			client.getManagementDelegate().startApplication( PropertyManager.APP_2 );
			Assert.fail( "An exception was expected" );
		} catch( ManagementException e ) {
			// nothing
		}

		try {
			client.getManagementDelegate().stopApplication( PropertyManager.APP_2 );
			Assert.fail( "An exception was expected" );
		} catch( ManagementException e ) {
			// nothing
		}

		try {
			client.getManagementDelegate().deleteApplication( PropertyManager.APP_2 );
			Assert.fail( "An exception was expected" );
		} catch( ManagementException e ) {
			// nothing
		}
	}


	@Test
	public void testDeployment_1() throws Exception {

		PropertyManager.INSTANCE.reset();
		WsClient client = RestTestUtils.buildWsClient();

		Assert.assertEquals( 0, PropertyManager.INSTANCE.remoteFiles.size());
		client.getManagementDelegate().loadApplication( "some file path" );
		Assert.assertEquals( 1, PropertyManager.INSTANCE.remoteFiles.size());
		Assert.assertEquals( "some file path", PropertyManager.INSTANCE.remoteFiles.get( 0 ));
	}



	@Test
	public void testDeployment_2() throws Exception {

		// Prepare the ZIP file
		File zipFile = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString() + ".zip" );
		zipFile.deleteOnExit();

		Map<String,String> entryToContent = TestUtils.buildZipContent();
		TestUtils.createZipFile( entryToContent, zipFile );

		// Upload the file
		PropertyManager.INSTANCE.reset();
		WsClient client = RestTestUtils.buildWsClient();

		client.getManagementDelegate().loadApplication( zipFile );
		Assert.assertEquals( 1, PropertyManager.INSTANCE.remoteFiles.size());

		String filePath = PropertyManager.INSTANCE.remoteFiles.get( 0 );
		Assert.assertNotNull( filePath );

		File f = new File( filePath );
		Assert.assertEquals( "_" + zipFile.getName(), f.getName());
		Assert.assertTrue( f.exists());
		Assert.assertTrue( f.isFile());

		// Compare the content
		TestUtils.compareZipContent( f, entryToContent );
	}


	@Test
	public void testDownload() throws Exception {

		File f = File.createTempFile( "test_", ".zip" );
		f.deleteOnExit();

		PropertyManager.INSTANCE.reset();
		WsClient client = RestTestUtils.buildWsClient();
		client.getManagementDelegate().downloadApplicationModelData( "some app", f );
		Assert.assertTrue( f.exists());

		Map<String,String> entryToContent = TestUtils.buildZipContent();
		TestUtils.compareZipContent( f, entryToContent );
	}
}
