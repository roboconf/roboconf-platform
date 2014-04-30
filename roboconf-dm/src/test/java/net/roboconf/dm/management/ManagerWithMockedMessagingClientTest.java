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
import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.internal.TestMessageServerClient;
import net.roboconf.dm.internal.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InexistingException;
import net.roboconf.dm.management.exceptions.InvalidActionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.utils.ResourceUtils;
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

		Manager.INSTANCE.getAppNameToManagedApplication().clear();
		Manager.INSTANCE.setIaasResolver( new TestIaasResolver());
		Manager.INSTANCE.setMessagingClientFactory( new DmMessageServerClientFactory());
	}


	@Test
	public void testMessageServerIp() throws Exception {

		final String ip = "192.168.1.15";
		final String newIp = "192.168.1.14";

		Assert.assertNull( Manager.INSTANCE.getMessageServerIp());
		Assert.assertTrue( Manager.INSTANCE.tryToChangeMessageServerIp( ip ));
		Assert.assertEquals( ip, Manager.INSTANCE.getMessageServerIp());

		Manager.INSTANCE.getAppNameToManagedApplication().put( "app1", null );
		Assert.assertFalse( Manager.INSTANCE.tryToChangeMessageServerIp( newIp ));
		Assert.assertEquals( ip, Manager.INSTANCE.getMessageServerIp());

		Manager.INSTANCE.getAppNameToManagedApplication().clear();
		Assert.assertTrue( Manager.INSTANCE.tryToChangeMessageServerIp( newIp ));
		Assert.assertEquals( newIp, Manager.INSTANCE.getMessageServerIp());
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


	@Test( expected = InexistingException.class )
	public void testShutdownApplication_inexisting() throws Exception {
		Manager.INSTANCE.shutdownApplication( "inexisting" );
	}


	@Test
	public void testShutdownApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
			app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
			Manager.INSTANCE.shutdownApplication( app.getName());

			TestMessageServerClient client = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
			Assert.assertEquals( 1, client.sentMessages.size());
			Assert.assertEquals( MsgCmdInstanceUndeploy.class, client.sentMessages.get( 0 ).getClass());

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test( expected = InexistingException.class )
	public void testDeleteApplication_inexisting() throws Exception {
		Manager.INSTANCE.deleteApplication( "inexisting" );
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDeleteApplication_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, f ));
			app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
			Manager.INSTANCE.deleteApplication( app.getName());

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, f ));
			Manager.INSTANCE.deleteApplication( app.getName());
			Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test
	public void testCleanUpAll() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

			TestMessageServerClient client = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
			Assert.assertFalse( client.connectionClosed.get());

			Manager.INSTANCE.cleanUpAll();
			Assert.assertTrue( client.connectionClosed.get());

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test( expected = InexistingException.class )
	public void testAddInstance_inexistingApplication() throws Exception {
		Manager.INSTANCE.addInstance( "inexisting", null, null );
	}


	@Test( expected = InexistingException.class )
	public void testAddInstance_inexistingParent() throws Exception {

		TestApplication app = new TestApplication();
		File f = File.createTempFile( "roboconf_", ".folder" );

		try {
			ManagedApplication ma = new ManagedApplication( app, f );
			Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
			Manager.INSTANCE.addInstance( app.getName(), "inexisting", new Instance( "mail-vm" ));

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
			Manager.INSTANCE.addInstance( app.getName(), null, new Instance( existingInstanceName ));

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

			String parentPath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
			String existingInstanceName = app.getMySql().getName();
			Manager.INSTANCE.addInstance( app.getName(), parentPath, new Instance( existingInstanceName ));

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

			Manager.INSTANCE.addInstance( app.getName(), null, newInstance );

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
			String instancePath = InstanceHelpers.computeInstancePath( app.getTomcatVm());

			Manager.INSTANCE.addInstance( app.getName(), instancePath, newInstance );
			Assert.assertEquals( 2, app.getTomcatVm().getChildren().size());
			Assert.assertTrue( app.getTomcatVm().getChildren().contains( newInstance ));

		} finally {
			Utils.deleteFilesRecursively( f );
		}
	}


	@Test( expected = InexistingException.class )
	public void testPerform_inexisstingAppliation() throws Exception {
		Manager.INSTANCE.perform( "inexisting", ApplicationAction.deploy.toString(), null, true );
	}


	@Test( expected = InvalidActionException.class )
	public void testPerform_invalidAction_1() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
		Manager.INSTANCE.perform( app.getName(), "eat", null, true );
	}


	@Test( expected = InvalidActionException.class )
	public void testPerform_invalidAction_2() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
		Manager.INSTANCE.perform( app.getName(), ApplicationAction.deploy.toString(), null, false );
	}


	@Test
	public void testPerformDeploy() throws Exception {
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

			TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.getIaasResolver();
			Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

			TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
			Assert.assertEquals( 0, msgClient.sentMessages.size());

			String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
			Manager.INSTANCE.perform( app.getName(), ApplicationAction.deploy.toString(), instancePath, true );

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
	public void testPerformStart() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.getIaasResolver();
		Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		Manager.INSTANCE.perform( app.getName(), ApplicationAction.start.toString(), instancePath, true );

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getMySql()),
				((MsgCmdInstanceStart) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test
	public void testPerformStop() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.getIaasResolver();
		Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		Manager.INSTANCE.perform( app.getName(), ApplicationAction.stop.toString(), instancePath, true );

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getMySql()),
				((MsgCmdInstanceStop) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test
	public void testPerformUndeploy() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.getIaasResolver();
		Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		Manager.INSTANCE.perform( app.getName(), ApplicationAction.undeploy.toString(), instancePath, true );

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getMySqlVm()),
				((MsgCmdInstanceUndeploy) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testPerformRemove_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Manager.INSTANCE.perform( app.getName(), ApplicationAction.remove.toString(), null, true );
	}


	@Test
	public void testPerformRemove_success_1() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();

		// Set up a "trap".
		// Remove MySQL will fail. But removing Tomcat instances will succeed.
		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		String tomcatVmInstancePath = InstanceHelpers.computeInstancePath( app.getTomcatVm());
		Manager.INSTANCE.perform( app.getName(), ApplicationAction.remove.toString(), tomcatVmInstancePath, true );

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
	public void testPerformRemove_success_2() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		Manager.INSTANCE.perform( app.getName(), ApplicationAction.remove.toString(), null, true );

		Assert.assertEquals( 0, app.getRootInstances().size());
		Assert.assertEquals( 3, msgClient.sentMessages.size());
	}


	@Test
	public void testFindInstancesToProcess_success() throws Exception {

		TestApplication app = new TestApplication();
		List<Instance> instances = Manager.INSTANCE.findInstancesToProcess( app, null, true );
		Assert.assertEquals( 5, instances.size());
		Assert.assertTrue( instances.contains( app.getMySql()));
		Assert.assertTrue( instances.contains( app.getMySqlVm()));
		Assert.assertTrue( instances.contains( app.getTomcat()));
		Assert.assertTrue( instances.contains( app.getTomcatVm()));
		Assert.assertTrue( instances.contains( app.getWar()));

		instances = Manager.INSTANCE.findInstancesToProcess( app, InstanceHelpers.computeInstancePath( app.getMySqlVm()), true );
		Assert.assertEquals( 2, instances.size());
		Assert.assertTrue( instances.contains( app.getMySql()));
		Assert.assertTrue( instances.contains( app.getMySqlVm()));

		instances = Manager.INSTANCE.findInstancesToProcess( app, InstanceHelpers.computeInstancePath( app.getMySqlVm()), false );
		Assert.assertEquals( 1, instances.size());
		Assert.assertTrue( instances.contains( app.getMySqlVm()));

		instances = Manager.INSTANCE.findInstancesToProcess( app, InstanceHelpers.computeInstancePath( app.getTomcat()), true );
		Assert.assertEquals( 2, instances.size());
		Assert.assertTrue( instances.contains( app.getTomcat()));
		Assert.assertTrue( instances.contains( app.getWar()));

		instances = Manager.INSTANCE.findInstancesToProcess( app, InstanceHelpers.computeInstancePath( app.getTomcat()), false );
		Assert.assertEquals( 1, instances.size());
		Assert.assertTrue( instances.contains( app.getTomcat()));

		instances = Manager.INSTANCE.findInstancesToProcess( app, InstanceHelpers.computeInstancePath( app.getWar()), true );
		Assert.assertEquals( 1, instances.size());
		Assert.assertTrue( instances.contains( app.getWar()));

		instances = Manager.INSTANCE.findInstancesToProcess( app, InstanceHelpers.computeInstancePath( app.getWar()), false );
		Assert.assertEquals( 1, instances.size());
		Assert.assertTrue( instances.contains( app.getWar()));
	}


	@Test( expected = InexistingException.class )
	public void testFindInstancesToProcess_inexistingInstance() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.findInstancesToProcess( app, "/pop", true );
	}


	@Test
	public void testIsConnectedToTheMessagingServer() {
		Assert.assertTrue( Manager.INSTANCE.isConnectedToTheMessagingServer());
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
