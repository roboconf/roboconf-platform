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

package net.roboconf.dm.management;

import java.io.File;

import junit.framework.Assert;
import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.management.exceptions.DmWasNotInitializedException;
import net.roboconf.messaging.client.MessageServerClientFactory;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerWithNoConnectionTest {

	@Before
	public void resetManager() throws Exception {
		Manager.INSTANCE.getAppNameToManagedApplication().clear();
		Manager.INSTANCE.setMessagingClientFactory( new MessageServerClientFactory());
	}


	@Test( expected = DmWasNotInitializedException.class )
	public void testPerformWhenNotConnected() throws Exception {

		Application app = new Application( "app" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.perform( app.getName(), ApplicationAction.deploy.toString(), null, true );
	}


	@Test( expected = DmWasNotInitializedException.class )
	public void testDeleteApplicationWhenNotConnected() throws Exception {

		Application app = new Application( "app" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.deleteApplication( app.getName());
	}


	@Test( expected = DmWasNotInitializedException.class )
	public void testLoadNewApplicationWhenNotConnected() throws Exception {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.loadNewApplication( new File( "whatever--we-should-check-the-connection-first" ));
	}


	@Test
	public void testCleanupAllWhenNotConnected() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.cleanUpAll();
	}


	@Test
	public void testCleanMessagingServerWhenNotConnected() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		ManagedApplication ma = new ManagedApplication( new Application( "app" ), null );
		Manager.INSTANCE.cleanMessagingServer( ma );
	}


	@Test
	public void testCleanUpWhenNotConnected_notNull() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		ManagedApplication ma = new ManagedApplication( new Application( "app" ), null );
		Manager.INSTANCE.cleanUp( ma );
	}


	@Test
	public void testCleanUpWhenNotConnected_null() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		Manager.INSTANCE.cleanUp( null );
	}


	@Test
	public void testTerminateMachine_notConnected() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		Manager.INSTANCE.setIaasResolver( new TestIaasResolver());
		Manager.INSTANCE.terminateMachine( app.getName(), app.getMySqlVm());
	}


	@Test
	public void testTerminateMachine() {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		TestIaasResolver iaasResolver = new TestIaasResolver();
		Manager.INSTANCE.setIaasResolver( iaasResolver );

		iaasResolver.instanceToRunningStatus.put( app.getMySqlVm(), Boolean.TRUE );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "whatever" );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		Manager.INSTANCE.terminateMachine( app.getName(), app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertNotNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( Boolean.FALSE, iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
	}


	@Test
	public void testShutdown() throws Exception {

		Assert.assertFalse( Manager.INSTANCE.isConnectedToTheMessagingServer());
		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		TestIaasResolver iaasResolver = new TestIaasResolver();
		Manager.INSTANCE.setIaasResolver( iaasResolver );

		for( Instance rootInstance : app.getRootInstances()) {
			iaasResolver.instanceToRunningStatus.put( rootInstance, Boolean.TRUE );
			rootInstance.getData().put( Instance.MACHINE_ID, "whatever" );
			rootInstance.setStatus( InstanceStatus.DEPLOYED_STARTED );
		}

		Manager.INSTANCE.shutdownApplication( app.getName());

		for( Instance rootInstance : app.getRootInstances()) {
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());
			Assert.assertNull( rootInstance.getData().get( Instance.MACHINE_ID ));
			Assert.assertNotNull( iaasResolver.instanceToRunningStatus.get( rootInstance ));
			Assert.assertEquals( Boolean.FALSE, iaasResolver.instanceToRunningStatus.get( rootInstance ));
		}
	}
}
