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
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ApplicationException;
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
public class ApplicationWsDelegateTest extends JerseyTest {

	@Override
	protected AppDescriptor configure() {
		return RestTestUtils.buildTestDescriptor();
	}

	@Override
    public TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

	@Test
	public void testInstances() throws Exception {

		PropertyManager.INSTANCE.reset();
		PropertyManager.INSTANCE.loadApplications();
		WsClient client = RestTestUtils.buildWsClient();

		// App 1
		List<Instance> instances = client.getApplicationDelegate().listAllInstances( PropertyManager.APP_1 );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 0, instances.size());

		Instance inst = new Instance();
		inst.setName( "vm1" );
		inst.setComponent( PropertyManager.INSTANCE.apps.get( 0 ).getGraphs().getRootComponents().iterator().next());

		client.getApplicationDelegate().addInstance( PropertyManager.APP_1, null, inst );
		instances = client.getApplicationDelegate().listAllInstances( PropertyManager.APP_1 );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 1, instances.size());

		client.getApplicationDelegate().removeInstance( PropertyManager.APP_1, "/vm1" );
		instances = client.getApplicationDelegate().listAllInstances( PropertyManager.APP_1 );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 0, instances.size());

		// App 2
		instances = client.getApplicationDelegate().listAllInstances( PropertyManager.APP_2 );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 2, instances.size());
		Assert.assertEquals( "theVm", instances.get( 0 ).getName());
		Assert.assertEquals( "server_1", instances.get( 1 ).getName());

		Instance i = client.getApplicationDelegate().getInstance( PropertyManager.APP_2, "/theVm/server_1" );
		Assert.assertNotNull( i );
		Assert.assertEquals( "server_1", i.getName());
		Assert.assertNotNull( i.getComponent());
		Assert.assertEquals( "server", i.getComponent().getName());

		inst = new Instance();
		inst.setName( "database" );
		inst.setComponent( i.getComponent());

		client.getApplicationDelegate().addInstance( PropertyManager.APP_2, "/theVm", inst );
		instances = client.getApplicationDelegate().listAllInstances( PropertyManager.APP_2 );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 3, instances.size());
		Assert.assertEquals( "database", instances.get( 2 ).getName());

		try {
			client.getApplicationDelegate().addInstance( PropertyManager.APP_2, "/theVm", inst );
			Assert.fail( "Expected an exception for duplicate insertion." );

		} catch( ApplicationException e ) {
			// nothing
		}

		instances = client.getApplicationDelegate().listAllInstances( PropertyManager.APP_2 );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 3, instances.size());
		Assert.assertEquals( "database", instances.get( 2 ).getName());

		instances = client.getApplicationDelegate().listRootInstances( PropertyManager.APP_2 );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 1, instances.size());
		Assert.assertEquals( "theVm", instances.get( 0 ).getName());

		instances = client.getApplicationDelegate().listChildrenInstances( PropertyManager.APP_2, "/theVm" );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 2, instances.size());
		Assert.assertEquals( "server_1", instances.get( 0 ).getName());
		Assert.assertEquals( "database", instances.get( 1 ).getName());

		instances = client.getApplicationDelegate().listChildrenInstances( PropertyManager.APP_2, "invalid path" );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 0, instances.size());

		client.getApplicationDelegate().removeInstance( PropertyManager.APP_1, "/theVm" );
		instances = client.getApplicationDelegate().listAllInstances( PropertyManager.APP_1 );
		Assert.assertNotNull( instances );
		Assert.assertEquals( 0, instances.size());
	}


	@Test
	public void testGraphs() throws Exception {

		PropertyManager.INSTANCE.reset();
		WsClient client = RestTestUtils.buildWsClient();
		PropertyManager.INSTANCE.loadApplications();

		List<Component> components = client.getApplicationDelegate().listAllComponents( PropertyManager.APP_1 );
		Assert.assertNotNull( components );
		Assert.assertEquals( 1, components.size());
		Assert.assertEquals( "vm", components.iterator().next().getName());

		components = client.getApplicationDelegate().listAllComponents( PropertyManager.APP_2 );
		Assert.assertNotNull( components );
		Assert.assertEquals( 2, components.size());
		Assert.assertEquals( "vm", components.get( 0 ).getName());
		Assert.assertEquals( "server", components.get( 1 ).getName());

		List<String> possibleChildrenComponents = client.getApplicationDelegate().findPossibleComponentChildren( PropertyManager.APP_2, "|theVm" );
		Assert.assertNotNull( possibleChildrenComponents );
		Assert.assertEquals( 1, possibleChildrenComponents.size());
		Assert.assertEquals( "server", possibleChildrenComponents.get( 0 ));

		possibleChildrenComponents = client.getApplicationDelegate().findPossibleComponentChildren( PropertyManager.APP_2, "some_wrong_path" );
		Assert.assertNotNull( possibleChildrenComponents );
		Assert.assertEquals( 0, possibleChildrenComponents.size());

		List<String> possibleParentInstances = client.getApplicationDelegate().findPossibleParentInstances( PropertyManager.APP_2, "server" );
		Assert.assertNotNull( possibleParentInstances );
		Assert.assertEquals( 1, possibleParentInstances.size());
		Assert.assertEquals( "/theVm", possibleParentInstances.get( 0 ));

		// TODO: test instance creation
	}
}
