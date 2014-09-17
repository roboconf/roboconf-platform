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
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.internal.TestMessageServerClient;
import net.roboconf.dm.internal.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Manager_BasicsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();



	@Before
	public void resetManager() throws Exception {

		// Shutdown used with a temporary folder will cause "IO Exceptions" (failed to save instances)
		Manager.INSTANCE.shutdown();
		Manager.INSTANCE.setIaasResolver( new TestIaasResolver());
		Manager.INSTANCE.setMessagingClientFactory( new DmMessageServerClientFactory());

		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );
		Manager.INSTANCE.initialize( conf );
	}


	@Test
	public void testSaveConfiguration_nullConfiguration() {

		Manager.INSTANCE.shutdown();
		Assert.assertNull( Manager.INSTANCE.configuration );
		Manager.INSTANCE.saveConfiguration( null );
		Manager.INSTANCE.saveConfiguration( new ManagedApplication( new TestApplication(), null ));
	}


	@Test
	public void testSaveConfiguration() {

		TestApplication app = new TestApplication();
		File instancesFile = new File(
				Manager.INSTANCE.configuration.getConfigurationDirectory(),
				ManagerConfiguration.INSTANCES + "/" + app.getName() + ".instances" );

		Assert.assertFalse( instancesFile.exists());
		Assert.assertNotNull( Manager.INSTANCE.configuration );
		Manager.INSTANCE.saveConfiguration( new ManagedApplication( app, null ));
		Assert.assertTrue( instancesFile.exists());
	}


	@Test
	public void testShutdown() throws Exception {

		Assert.assertNotNull( Manager.INSTANCE.messagingClient );
		Manager.INSTANCE.shutdown();
		Assert.assertNull( Manager.INSTANCE.messagingClient );

		// Test the idempotence of the shutdown() method
		Manager.INSTANCE.shutdown();
		Assert.assertNull( Manager.INSTANCE.messagingClient );
	}


	@Test
	public void testShutdown_notConnected() throws Exception {

		Assert.assertNotNull( Manager.INSTANCE.messagingClient );
		Manager.INSTANCE.messagingClient.closeConnection();
		Manager.INSTANCE.shutdown();
		Assert.assertNull( Manager.INSTANCE.messagingClient );
	}


	@Test
	public void testShutdown_withErrorOnCloseConnection() throws Exception {

		Manager.INSTANCE.messagingClient = new TestMessageServerClient() {
			@Override
			public void closeConnection() throws IOException {
				throw new IOException();
			}

			@Override
			public boolean isConnected() {
				return true;
			}
		};

		Manager.INSTANCE.shutdown();
		Assert.assertNull( Manager.INSTANCE.messagingClient );
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
		File f = this.folder.newFolder();

		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		Assert.assertNull( Manager.INSTANCE.findApplicationByName( app.getName()));

		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, f ));
		Assert.assertEquals( app, Manager.INSTANCE.findApplicationByName( app.getName()));
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDeleteApplication_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();
		ManagedApplication ma = new ManagedApplication( app, f );

		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Manager.INSTANCE.deleteApplication( ma );
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();
		ManagedApplication ma = new ManagedApplication( app, f );

		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
		Manager.INSTANCE.deleteApplication( ma );
		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
	}


	@Test
	public void testShutdownApplication() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestMessageServerClient client = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		Assert.assertFalse( client.connectionClosed.get());

		Manager.INSTANCE.shutdown();
		Assert.assertTrue( client.connectionClosed.get());
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		String existingInstanceName = app.getMySqlVm().getName();
		Manager.INSTANCE.addInstance( ma, null, new Instance( existingInstanceName ));
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_childInstance() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		String existingInstanceName = app.getMySql().getName();
		Manager.INSTANCE.addInstance( ma, app.getMySqlVm(), new Instance( existingInstanceName ));
	}


	@Test
	public void testAddInstance_successRoot() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Instance newInstance = new Instance( "mail-vm" ).component( app.getMySqlVm().getComponent());

		Manager.INSTANCE.addInstance( ma, null, newInstance );
		Assert.assertEquals( 3, app.getRootInstances().size());
		Assert.assertTrue( app.getRootInstances().contains( newInstance ));
	}


	@Test
	public void testAddInstance_successChild() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		// Insert a MySQL instance under the Tomcat VM
		Assert.assertEquals( 1, app.getTomcatVm().getChildren().size());
		Instance newInstance = new Instance( app.getMySql().getName()).component( app.getMySql().getComponent());

		Manager.INSTANCE.addInstance( ma, app.getTomcatVm(), newInstance );
		Assert.assertEquals( 2, app.getTomcatVm().getChildren().size());
		Assert.assertTrue( app.getTomcatVm().getChildren().contains( newInstance ));
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testRemoveInstance_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Manager.INSTANCE.removeInstance( ma, app.getMySqlVm());
	}


	@Test
	public void testRemoveInstance_success_1() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Manager.INSTANCE.removeInstance( ma, app.getTomcatVm());

		Assert.assertEquals( 1, app.getRootInstances().size());
		Assert.assertEquals( app.getMySqlVm(), app.getRootInstances().iterator().next());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());

		List<Message> messages = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, messages.get( 0 ).getClass());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getTomcatVm()),
				((MsgCmdRemoveInstance) messages.get( 0 )).getInstancePath());
	}


	@Test
	public void testRemoveInstance_success_2() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );
		TestMessageServerClient msgclient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Manager.INSTANCE.removeInstance( ma, app.getTomcat());

		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( app.getMySqlVm(), app.getRootInstances().iterator().next());
		Assert.assertEquals( 1, msgclient.sentMessages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, msgclient.sentMessages.get( 0 ).getClass());

		MsgCmdRemoveInstance msg = (MsgCmdRemoveInstance) msgclient.sentMessages.get( 0 );
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), msg.getInstancePath());
	}


	@Test
	public void testLoadNewApplication_success() throws Exception {

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		ManagedApplication ma = Manager.INSTANCE.loadNewApplication( directory );

		Assert.assertNotNull( ma );
		Assert.assertEquals( ma.getApplicationFilesDirectory().getName(), ma.getName());
		Assert.assertEquals( Utils.listAllFiles( directory ).size(), Utils.listAllFiles( ma.getApplicationFilesDirectory()).size());
		Assert.assertEquals( "Legacy LAMP", ma.getApplication().getName());

		File expected = new File(
				Manager.INSTANCE.configuration.getConfigurationDirectory(),
				ManagerConfiguration.APPLICATIONS );

		Assert.assertEquals( expected, ma.getApplicationFilesDirectory().getParentFile());
	}


	@Test( expected = AlreadyExistingException.class )
	public void testLoadNewApplication_conflict() throws Exception {

		Application app = new Application( "Legacy LAMP" );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		Manager.INSTANCE.loadNewApplication( directory );
	}


	@Test( expected = IOException.class )
	public void testLoadNewApplication_invalidDirectory() throws Exception {

		File source = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( source.exists());

		File apps = new File(
				Manager.INSTANCE.configuration.getConfigurationDirectory(),
				ManagerConfiguration.APPLICATIONS );

		File target = new File( apps, "Legacy LAMP/sub/dir" );
		Utils.copyDirectory( source, target );
		Manager.INSTANCE.loadNewApplication( target );
	}


	@Test
	public void testLoadNewApplication_restoration() throws Exception {

		File source = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( source.exists());

		File apps = new File(
				Manager.INSTANCE.configuration.getConfigurationDirectory(),
				ManagerConfiguration.APPLICATIONS );

		File target = new File( apps, "Legacy LAMP" );
		Utils.copyDirectory( source, target );
		ManagedApplication app = Manager.INSTANCE.loadNewApplication( target );
		Assert.assertNotNull( app );
	}


	@Test( expected = InvalidApplicationException.class )
	public void testLoadNewApplication_invalidApplication() throws Exception {
		Manager.INSTANCE.loadNewApplication( this.folder.newFolder());
	}


	@Test( expected = IOException.class )
	public void testInitialize_alreadyInitialized() throws Exception {

		ManagerConfiguration conf = Manager.INSTANCE.configuration;
		Manager.INSTANCE.initialize( conf );
	}


	@Test
	public void testInitialize_withApps_noInstance() throws Exception {

		// Copy an application in the configuration
		File source = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( source.exists());

		File apps = new File(
				Manager.INSTANCE.configuration.getConfigurationDirectory(),
				ManagerConfiguration.APPLICATIONS );

		File target = new File( apps, "Legacy LAMP" );
		Utils.copyDirectory( source, target );

		// Reset the manager
		ManagerConfiguration conf = Manager.INSTANCE.configuration;
		Manager.INSTANCE.shutdown();
		Manager.INSTANCE.initialize( conf );

		// Check there is an application
		Assert.assertEquals( 1, Manager.INSTANCE.getAppNameToManagedApplication().size());
		ManagedApplication ma = Manager.INSTANCE.getAppNameToManagedApplication().get( "Legacy LAMP" );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());
		Assert.assertEquals( 6, InstanceHelpers.getAllInstances( ma.getApplication()).size());

		for( Instance inst : InstanceHelpers.getAllInstances( ma.getApplication()))
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, inst.getStatus());
	}


	@Test
	public void testInitialize_withApps_withInstances() throws Exception {

		// Copy an application in the configuration
		File source = TestUtils.findTestFile( "/lamp" );
		ManagedApplication ma = Manager.INSTANCE.loadNewApplication( source );
		Instance apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertNotNull( apache );

		apache.getData().put( Instance.IP_ADDRESS, "192.168.1.23" );
		apache.getData().put( Instance.MACHINE_ID, "my id" );
		apache.getData().put( "whatever", "something" );
		apache.setStatus( InstanceStatus.PROBLEM );

		// Reset the manager
		ManagerConfiguration conf = Manager.INSTANCE.configuration;
		Manager.INSTANCE.shutdown();
		Assert.assertEquals( 0, Manager.INSTANCE.getAppNameToManagedApplication().size());
		Manager.INSTANCE.initialize( conf );

		// Check there is an application
		Assert.assertEquals( 1, Manager.INSTANCE.getAppNameToManagedApplication().size());
		ma = Manager.INSTANCE.getAppNameToManagedApplication().get( "Legacy LAMP" );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());
		Assert.assertEquals( 6, InstanceHelpers.getAllInstances( ma.getApplication()).size());

		for( Instance inst : InstanceHelpers.getAllInstances( ma.getApplication())) {
			if( ! inst.equals( apache ))
				Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}

		apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertEquals( InstanceStatus.PROBLEM, apache.getStatus());
		Assert.assertEquals( 4, apache.getData().size());
		Assert.assertEquals( "192.168.1.23", apache.getData().get( Instance.IP_ADDRESS ));
		Assert.assertEquals( "my id", apache.getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( "something", apache.getData().get( "whatever" ));
		Assert.assertEquals( ma.getName(), apache.getData().get( Instance.APPLICATION_NAME ));
	}


	@Test( expected = InvalidApplicationException.class )
	public void testCheckErrors_withCriticalError() throws Exception {

		RoboconfError error = new ModelError( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, 2 );
		Manager.INSTANCE.checkErrors( Arrays.asList( error ));
	}


	@Test
	public void testCheckErrors_withWarningOnly() throws Exception {

		RoboconfError error = new ModelError( ErrorCode.CO_NOT_OVERRIDING, 2 );
		Manager.INSTANCE.checkErrors( Arrays.asList( error ));
	}


	@Test
	public void testSendWhenNoConnection() throws Exception {

		ManagedApplication ma = new ManagedApplication( new TestApplication(), null );
		TestMessageServerClient client = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Manager.INSTANCE.messagingClient.closeConnection();
		Manager.INSTANCE.send( ma, new MsgCmdSendInstances(), new Instance());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 0, client.sentMessages.size());

		Manager.INSTANCE.shutdown();
		Manager.INSTANCE.send( ma, new MsgCmdSendInstances(), new Instance());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 0, client.sentMessages.size());
	}


	@Test
	public void testResynchronizeAgents_withConnection() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		TestMessageServerClient client = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Manager.INSTANCE.resynchronizeAgents( ma );
		Assert.assertEquals( 0, client.sentMessages.size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Manager.INSTANCE.resynchronizeAgents( ma );
		Assert.assertEquals( 1, client.sentMessages.size());
		Assert.assertEquals( MsgCmdResynchronize.class, client.sentMessages.get( 0 ).getClass());

		client.sentMessages.clear();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Manager.INSTANCE.resynchronizeAgents( ma );
		Assert.assertEquals( 2, client.sentMessages.size());
		Assert.assertEquals( MsgCmdResynchronize.class, client.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdResynchronize.class, client.sentMessages.get( 1 ).getClass());
	}


	@Test
	public void testResynchronizeAgents_noConnection() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		TestMessageServerClient client = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Manager.INSTANCE.messagingClient.closeConnection();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		Manager.INSTANCE.resynchronizeAgents( ma );
		Assert.assertEquals( 0, client.sentMessages.size());
	}
}
