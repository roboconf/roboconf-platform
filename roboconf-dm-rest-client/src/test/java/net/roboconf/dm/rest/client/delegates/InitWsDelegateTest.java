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

import junit.framework.Assert;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.InitializationException;
import net.roboconf.dm.rest.client.test.RestTestUtils;
import net.roboconf.messaging.client.MessageServerClientFactory;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InitWsDelegateTest extends JerseyTest {

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
		Manager.INSTANCE.setMessagingClientFactory( new MessageServerClientFactory());
	}


	@Test
	public void testInitialization_success() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertFalse( client.getInitDelegate().isDeploymentManagerInitialized());
		client.getInitDelegate().initializeDeploymentManager( "127.0.0.1" );
		Assert.assertEquals( "127.0.0.1", Manager.INSTANCE.getMessageServerIp());
		Assert.assertTrue( client.getInitDelegate().isDeploymentManagerInitialized());
	}


	@Test
	public void testInitialization_default() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertFalse( client.getInitDelegate().isDeploymentManagerInitialized());
		client.getInitDelegate().initializeDeploymentManager( null );
		Assert.assertEquals( "localhost", Manager.INSTANCE.getMessageServerIp());
		Assert.assertTrue( client.getInitDelegate().isDeploymentManagerInitialized());
	}


	@Test( expected = InitializationException.class )
	public void testInitialization_failure() throws Exception {

		testInitialization_success();
		Manager.INSTANCE.getAppNameToManagedApplication().put( "myApp", null );

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertTrue( client.getInitDelegate().isDeploymentManagerInitialized());
		client.getInitDelegate().initializeDeploymentManager( "192.168.1.9" );
		Assert.assertTrue( client.getInitDelegate().isDeploymentManagerInitialized());
	}
}
