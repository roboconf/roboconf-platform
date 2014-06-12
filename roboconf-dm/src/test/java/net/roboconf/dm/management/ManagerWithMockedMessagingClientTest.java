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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.internal.TestMessageServerClient;
import net.roboconf.dm.internal.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceAdd;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceDeploy;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceRemove;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStart;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStop;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceUndeploy;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerWithMockedMessagingClientTest {




	@Before
	public void resetManager() {

		Manager.INSTANCE.shutdown();
		Manager.INSTANCE.setIaasResolver( new TestIaasResolver());
		Manager.INSTANCE.setMessagingClientFactory( new DmMessageServerClientFactory());
	}


	@Test
	public void testInitializeAndShutdown() throws Exception {

		Manager.INSTANCE.shutdown();
		// ManagerConfiguration conf = n
	}


	@Test
	public void testListApplications() throws Exception {

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, f ));
			List<Application> applications = Manager.INSTANCE.listApplications();
			Assert.assertEquals( 1, applications.size());
			Assert.assertEquals( app, applications.get( 0 ));

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testFindApplicationByName() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		Assert.assertNull( Manager.INSTANCE.findApplicationByName( app.getName()));

		try {
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, f ));
			Assert.assertEquals( app, Manager.INSTANCE.findApplicationByName( app.getName()));

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testShutdownApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
			Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());

			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
			Manager.INSTANCE.deployRoot( ma, app.getMySqlVm(), true );
			Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());

			Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
			Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
			Manager.INSTANCE.shutdownApplication( ma );
			Assert.assertFalse( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDeleteApplication_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );
		ManagedApplication ma = new ManagedApplication( app, f );

		try {
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
			app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
			Manager.INSTANCE.deleteApplication( ma );

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );
		ManagedApplication ma = new ManagedApplication( app, f );

		try {
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
			Manager.INSTANCE.deleteApplication( ma );
			Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testShutdown() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			TestMessageServerClient client = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
			Assert.assertFalse( client.connectionClosed.get());

			Manager.INSTANCE.shutdown();
			Assert.assertTrue( client.connectionClosed.get());

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			String existingInstanceName = app.getMySqlVm().getName();
			Manager.INSTANCE.addInstance( ma, null, new Instance( existingInstanceName ));

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_childInstance() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			String existingInstanceName = app.getMySql().getName();
			Manager.INSTANCE.addInstance( ma, app.getMySqlVm(), new Instance( existingInstanceName ));

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testAddInstance_successRoot() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			Assert.assertEquals( 2, app.getRootInstances().size());
			Instance newInstance = new Instance( "mail-vm" ).component( app.getMySqlVm().getComponent());

			Manager.INSTANCE.addInstance( ma, null, newInstance );

			Assert.assertEquals( 3, app.getRootInstances().size());
			Assert.assertTrue( app.getRootInstances().contains( newInstance ));

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testAddInstance_successChild() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			// Insert a MySQL instance under the Tomcat VM
			Assert.assertEquals( 1, app.getTomcatVm().getChildren().size());
			Instance newInstance = new Instance( app.getMySql().getName()).component( app.getMySql().getComponent());

			Manager.INSTANCE.addInstance( ma, app.getTomcatVm(), newInstance );
			Assert.assertEquals( 2, app.getTomcatVm().getChildren().size());
			Assert.assertTrue( app.getTomcatVm().getChildren().contains( newInstance ));

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testDeploy() throws Exception {
		TestApplication app = new TestApplication();

		// Create temporary directories
		final File rootDir = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_app" );
		if( ! rootDir.exists()
				&& ! rootDir.mkdir())
			throw new IOException( "Failed to create a root directory for tests." );

		for( Instance inst : InstanceHelpers.getAllInstances( app )) {
			File f = ResourceUtils.findInstanceResourcesDirectory( rootDir, inst );
			if( ! f.exists()
					&& ! f.mkdirs())
				throw new IOException( "Failed to create a directory for tests. " + f.getAbsolutePath());
		}

		// Load the application and check assertions
		try {
			ManagedApplication ma = new ManagedApplication( app, rootDir );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
			Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

			TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
			Assert.assertEquals( 0, msgClient.sentMessages.size());

			Manager.INSTANCE.deploy( ma, app.getMySqlVm(), true );

			Assert.assertNotNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
			Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
			Assert.assertEquals( 1, msgClient.sentMessages.size());

			final String vmPath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
			final String serverPath = InstanceHelpers.computeInstancePath( app.getMySql());

			for( Message msg : msgClient.sentMessages ) {

				if( msg instanceof MsgCmdInstanceDeploy ) {
					Assert.assertEquals( serverPath, ((MsgCmdInstanceDeploy) msg).getInstancePath());

				} else if( msg instanceof MsgCmdInstanceAdd ) {
					Assert.assertNull( vmPath, ((MsgCmdInstanceAdd) msg).getParentInstancePath());
					Assert.assertEquals( app.getMySqlVm(), ((MsgCmdInstanceAdd) msg).getInstanceToAdd());

				} else {
					Assert.fail( "Unknown message type:" + msg.getClass());
				}
			}

		} finally {
			Utils.deleteFilesRecursively( rootDir );
		}
	}


	@Test
	public void testStart() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		Manager.INSTANCE.start( ma, app.getMySqlVm(), true );

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getMySql()),
				((MsgCmdInstanceStart) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test
	public void testStop() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Stopping a root => no message sent to the children
		Manager.INSTANCE.stop( ma, app.getMySqlVm());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Stop a child directly when it is not a VM => 1 message
		Manager.INSTANCE.stop( ma, app.getMySql());

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getMySql()),
				((MsgCmdInstanceStop) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test
	public void testUndeploy() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		Manager.INSTANCE.undeploy( ma, app.getMySql());

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getMySql()),
				((MsgCmdInstanceUndeploy) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testRemoveInstance_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Manager.INSTANCE.removeInstance( ma, null );
	}


	@Test
	public void testRemoveInstance_success_1() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		// Set up a "trap".
		// Remove MySQL will fail. But removing Tomcat instances will succeed.
		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		Manager.INSTANCE.removeInstance( ma, app.getTomcatVm());

		Assert.assertEquals( 1, app.getRootInstances().size());
		Assert.assertEquals( app.getMySqlVm(), app.getRootInstances().iterator().next());
		Assert.assertEquals( 2, msgClient.sentMessages.size());

		List<String> paths = new ArrayList<String> ();
		paths.add( InstanceHelpers.computeInstancePath( app.getWar()));
		paths.add( InstanceHelpers.computeInstancePath( app.getTomcat()));

		for( Message msg : msgClient.sentMessages ) {
			String path = ((MsgCmdInstanceRemove) msg).getInstancePath();
			paths.remove( path );
		}

		Assert.assertEquals( 0, paths.size());
	}


	@Test
	public void testRemoveInstance_success_2() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		Manager.INSTANCE.removeInstance( ma, null );

		Assert.assertEquals( 0, app.getRootInstances().size());
		Assert.assertEquals( 3, msgClient.sentMessages.size());
	}


	@Test
	public void testLoadNewApplication_success() throws Exception {

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		ManagedApplication ma = Manager.INSTANCE.loadNewApplication( directory );

		Assert.assertNotNull( ma );
		Assert.assertEquals( directory, ma.getApplicationFilesDirectory());
		Assert.assertEquals( "Legacy LAMP", ma.getApplication().getName());
	}


	@Test( expected = AlreadyExistingException.class )
	public void testLoadNewApplication_conflict() throws Exception {

		Application app = new Application( "Legacy LAMP" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		Manager.INSTANCE.loadNewApplication( directory );
	}


	@Test( expected = InvalidApplicationException.class )
	public void testLoadNewApplication_invalidApplication() throws Exception {
		Manager.INSTANCE.loadNewApplication( new File( System.getProperty( "java.io.tmpdir" )));
	}
}
