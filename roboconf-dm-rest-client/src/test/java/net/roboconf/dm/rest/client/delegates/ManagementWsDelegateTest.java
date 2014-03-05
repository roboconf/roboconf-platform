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

import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.internal.TestEnvironmentInterface;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ManagementException;
import net.roboconf.dm.rest.client.test.RestTestUtils;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory;

/**
 * @author Vincent Zurczak - Linagora
 * TODO: test loadApplication and downloadModelData
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


	@Before
	public void resetManager() {
		Manager.INSTANCE.cleanUpAll();
		Manager.INSTANCE.getAppNameToManagedApplication().clear();
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
				new ManagedApplication( app, null, new TestEnvironmentInterface()));

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
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestEnvironmentInterface()));

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
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestEnvironmentInterface()));

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 1, client.getManagementDelegate().listApplications().size());
		client.getManagementDelegate().deleteApplication( app.getName());
		Assert.assertEquals( 0, client.getManagementDelegate().listApplications().size());
	}
}
