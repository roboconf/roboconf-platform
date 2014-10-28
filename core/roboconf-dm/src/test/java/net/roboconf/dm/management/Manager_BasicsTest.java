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
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.test.TargetHandlerMock;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.internal.AbstractMessagingTest;
import net.roboconf.messaging.internal.client.test.TestClientDm;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;

import org.junit.After;
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
	private Manager manager;


	@Before
	public void resetManager() throws Exception {

		File directory = this.folder.newFolder();

		this.manager = new Manager( MessagingConstants.FACTORY_TEST );
		this.manager.setConfigurationDirectoryLocation( directory.getAbsolutePath());
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.update();

		// Disable the messages timer for predictability
		this.manager.timer.cancel();

		// Wait for the client to be picked up by the processor
		Thread.sleep( AbstractMessagingTest.DELAY );
		((TestClientDm) this.manager.getMessagingClient()).sentMessages.clear();
	}


	@After
	public void stopManager() {
		this.manager.shutdown();
	}


	@Test
	public void testSaveConfiguration() {

		TestApplication app = new TestApplication();
		File instancesFile = new File(
				this.manager.configurationDirectory,
				ConfigurationUtils.INSTANCES + "/" + app.getName() + ".instances" );

		Assert.assertFalse( instancesFile.exists());
		Assert.assertTrue( this.manager.validConfiguration );
		this.manager.saveConfiguration( new ManagedApplication( app, null ));
		Assert.assertTrue( instancesFile.exists());
	}


	@Test
	public void testShutdown() throws Exception {

		Assert.assertNotNull( this.manager.timer );
		this.manager.shutdown();
		Assert.assertNull( this.manager.timer );

		this.manager.shutdown();
		Assert.assertNull( this.manager.timer );
	}


	@Test
	public void testShutdown_invalidConfiguration() throws Exception {

		this.manager = new Manager();

		Assert.assertNull( this.manager.timer );
		this.manager.shutdown();
		Assert.assertNull( this.manager.timer );
	}


	@Test
	public void testFindApplicationByName() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		Assert.assertEquals( 0, this.manager.getAppNameToManagedApplication().size());
		Assert.assertNull( this.manager.findApplicationByName( app.getName()));

		this.manager.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, f ));
		Assert.assertEquals( app, this.manager.findApplicationByName( app.getName()));
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDeleteApplication_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();
		ManagedApplication ma = new ManagedApplication( app, f );

		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.deleteApplication( ma );
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();
		ManagedApplication ma = new ManagedApplication( app, f );

		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.deleteApplication( ma );
		Assert.assertEquals( 0, this.manager.getAppNameToManagedApplication().size());
	}


	@Test( expected = IOException.class )
	public void testDeleteApplication_invalidConfiguration() throws Exception {

		this.manager = new Manager();

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();
		ManagedApplication ma = new ManagedApplication( app, f );

		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.deleteApplication( ma );
		Assert.assertEquals( 0, this.manager.getAppNameToManagedApplication().size());
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		String existingInstanceName = app.getMySqlVm().getName();
		this.manager.addInstance( ma, null, new Instance( existingInstanceName ));
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_childInstance() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		String existingInstanceName = app.getMySql().getName();
		this.manager.addInstance( ma, app.getMySqlVm(), new Instance( existingInstanceName ));
	}


	@Test
	public void testAddInstance_successRoot() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Instance newInstance = new Instance( "mail-vm" ).component( app.getMySqlVm().getComponent());

		this.manager.addInstance( ma, null, newInstance );
		Assert.assertEquals( 3, app.getRootInstances().size());
		Assert.assertTrue( app.getRootInstances().contains( newInstance ));
	}


	@Test( expected = IOException.class )
	public void testAddInstance_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Instance newInstance = new Instance( "mail-vm" ).component( app.getMySqlVm().getComponent());

		this.manager = new Manager();
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.addInstance( ma, null, newInstance );
	}


	@Test
	public void testAddInstance_successChild() throws Exception {

		TestApplication app = new TestApplication();
		File f = this.folder.newFolder();

		ManagedApplication ma = new ManagedApplication( app, f );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		// Insert a MySQL instance under the Tomcat VM
		Assert.assertEquals( 1, app.getTomcatVm().getChildren().size());
		Instance newInstance = new Instance( app.getMySql().getName()).component( app.getMySql().getComponent());

		this.manager.addInstance( ma, app.getTomcatVm(), newInstance );
		Assert.assertEquals( 2, app.getTomcatVm().getChildren().size());
		Assert.assertTrue( app.getTomcatVm().getChildren().contains( newInstance ));
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testRemoveInstance_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.removeInstance( ma, app.getMySqlVm());
	}


	@Test
	public void testRemoveInstance_success_1() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.removeInstance( ma, app.getTomcatVm());

		Assert.assertEquals( 1, app.getRootInstances().size());
		Assert.assertEquals( app.getMySqlVm(), app.getRootInstances().iterator().next());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());

		List<Message> messages = ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm());
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
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		TestClientDm msgclient = (TestClientDm) this.manager.getMessagingClient();

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.removeInstance( ma, app.getTomcat());

		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( app.getMySqlVm(), app.getRootInstances().iterator().next());
		Assert.assertEquals( 1, msgclient.sentMessages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, msgclient.sentMessages.get( 0 ).getClass());

		MsgCmdRemoveInstance msg = (MsgCmdRemoveInstance) msgclient.sentMessages.get( 0 );
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), msg.getInstancePath());
	}


	@Test( expected = IOException.class )
	public void testRemoveInstance_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager = new Manager();
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.removeInstance( ma, app.getTomcat());
	}


	@Test
	public void testLoadNewApplication_success() throws Exception {

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		ManagedApplication ma = this.manager.loadNewApplication( directory );

		Assert.assertNotNull( ma );
		Assert.assertEquals( ma.getApplicationFilesDirectory().getName(), ma.getName());
		Assert.assertEquals( Utils.listAllFiles( directory ).size(), Utils.listAllFiles( ma.getApplicationFilesDirectory()).size());
		Assert.assertEquals( "Legacy LAMP", ma.getApplication().getName());

		File expected = new File( this.manager.configurationDirectory, ConfigurationUtils.APPLICATIONS );
		Assert.assertEquals( expected, ma.getApplicationFilesDirectory().getParentFile());
	}


	@Test( expected = AlreadyExistingException.class )
	public void testLoadNewApplication_conflict() throws Exception {

		Application app = new Application( "Legacy LAMP" );
		this.manager.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());
		this.manager.loadNewApplication( directory );
	}


	@Test( expected = IOException.class )
	public void testLoadNewApplication_invalidDirectory() throws Exception {

		File source = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( source.exists());

		File apps = new File( this.manager.configurationDirectory, ConfigurationUtils.APPLICATIONS );
		File target = new File( apps, "Legacy LAMP/sub/dir" );
		Utils.copyDirectory( source, target );
		this.manager.loadNewApplication( target );
	}


	@Test( expected = IOException.class )
	public void testLoadNewApplication_invalidConfiguration() throws Exception {

		File directory = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( directory.exists());

		this.manager = new Manager();
		this.manager.loadNewApplication( directory );
	}


	@Test
	public void testLoadNewApplication_restoration() throws Exception {

		File source = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( source.exists());

		File apps = new File( this.manager.configurationDirectory, ConfigurationUtils.APPLICATIONS );
		File target = new File( apps, "Legacy LAMP" );
		Utils.copyDirectory( source, target );

		ManagedApplication app = this.manager.loadNewApplication( target );
		Assert.assertNotNull( app );
	}


	@Test( expected = InvalidApplicationException.class )
	public void testLoadNewApplication_invalidApplication() throws Exception {
		this.manager.loadNewApplication( this.folder.newFolder());
	}


	@Test( expected = IOException.class )
	public void testCheckConfiguration_noConfiguration() throws Exception {

		this.manager = new Manager();
		this.manager.checkConfiguration();
	}


	@Test( expected = IOException.class )
	public void testCheckConfiguration_invalidConfiguration() throws Exception {

		this.manager.shutdown();
		this.manager.messageProcessor = new DmMessageProcessor( this.manager ) {
			@Override
			public IDmClient switchMessagingClient( String messageServerIp, String messageServerUser, String messageServerPwd )
			throws IOException {
				throw new IOException( "For test purpose." );
			}
		};

		this.manager.messageProcessor.start();
		this.manager.update();

		Thread.sleep( AbstractMessagingTest.DELAY );
		this.manager.checkConfiguration();
	}


	@Test
	public void testCheckConfiguration() throws Exception {
		this.manager.checkConfiguration();
		Assert.assertTrue( this.manager.validConfiguration );
	}


	@Test
	public void testConfigurationChanged_withApps_noInstance() throws Exception {

		// Copy an application in the configuration
		Assert.assertTrue( this.manager.validConfiguration );
		File source = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( source.exists());

		File apps = new File( this.manager.configurationDirectory, ConfigurationUtils.APPLICATIONS );
		File target = new File( apps, "Legacy LAMP" );
		Utils.copyDirectory( source, target );

		// Reset the manager's configuration (simply reload it)
		this.manager.update();
		Assert.assertTrue( this.manager.validConfiguration );

		// Check there is an application
		Assert.assertEquals( 1, this.manager.getAppNameToManagedApplication().size());
		ManagedApplication ma = this.manager.getAppNameToManagedApplication().get( "Legacy LAMP" );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());
		Assert.assertEquals( 6, InstanceHelpers.getAllInstances( ma.getApplication()).size());

		for( Instance inst : InstanceHelpers.getAllInstances( ma.getApplication()))
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, inst.getStatus());

		// Change the configuration's directory => no more application in the manager
		File newDirectory = this.folder.newFolder();
		this.manager.setConfigurationDirectoryLocation( newDirectory.getAbsolutePath());
		this.manager.update();
		Thread.sleep( AbstractMessagingTest.DELAY );

		Assert.assertTrue( this.manager.validConfiguration );
		Assert.assertEquals( newDirectory, this.manager.configurationDirectory );
		Assert.assertEquals( 0, this.manager.getAppNameToManagedApplication().size());
	}


	@Test
	public void testConfigurationChanged_andShutdown_withApps_withInstances() throws Exception {

		// Copy an application in the configuration
		File source = TestUtils.findTestFile( "/lamp" );
		ManagedApplication ma = this.manager.loadNewApplication( source );
		Instance apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertNotNull( apache );

		// Update the instances
		apache.getData().put( Instance.IP_ADDRESS, "192.168.1.23" );
		apache.getData().put( Instance.MACHINE_ID, "my id" );
		apache.getData().put( "whatever", "something" );
		apache.setStatus( InstanceStatus.PROBLEM );

		// Save the manager's state
		this.manager.shutdown();

		// Reset the manager (reload the configuration)
		this.manager.update();

		// Check there is the right application
		Assert.assertEquals( 1, this.manager.getAppNameToManagedApplication().size());
		ma = this.manager.getAppNameToManagedApplication().get( "Legacy LAMP" );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());
		Assert.assertEquals( 6, InstanceHelpers.getAllInstances( ma.getApplication()).size());

		for( Instance inst : InstanceHelpers.getAllInstances( ma.getApplication())) {
			if( ! inst.equals( apache ))
				Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}

		apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertEquals( InstanceStatus.PROBLEM, apache.getStatus());
		Assert.assertEquals( "192.168.1.23", apache.getData().get( Instance.IP_ADDRESS ));
		Assert.assertEquals( "my id", apache.getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( "something", apache.getData().get( "whatever" ));
		Assert.assertEquals( ma.getName(), apache.getData().get( Instance.APPLICATION_NAME ));
	}


	@Test( expected = InvalidApplicationException.class )
	public void testCheckErrors_withCriticalError() throws Exception {

		RoboconfError error = new ModelError( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, 2 );
		this.manager.checkErrors( Arrays.asList( error ));
	}


	@Test
	public void testCheckErrors_withWarningOnly() throws Exception {

		RoboconfError error = new ModelError( ErrorCode.CO_NOT_OVERRIDING, 2 );
		this.manager.checkErrors( Arrays.asList( error ));
	}


	@Test
	public void testCheckErrors_withnoErrorOrWarning() throws Exception {
		this.manager.checkErrors( new ArrayList<RoboconfError> ());
	}


	@Test
	public void testSendWhenNoConnection() throws Exception {

		ManagedApplication ma = new ManagedApplication( new TestApplication(), null );
		TestClientDm client = (TestClientDm) this.manager.getMessagingClient();

		this.manager.getMessagingClient().closeConnection();
		this.manager.send( ma, new MsgCmdSendInstances(), new Instance());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, client.sentMessages.size());

		this.manager.shutdown();
		this.manager.send( ma, new MsgCmdSendInstances(), new Instance());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, client.sentMessages.size());
	}


	@Test
	public void testSendWhenInvalidConfiguration() throws Exception {

		ManagedApplication ma = new ManagedApplication( new TestApplication(), null );
		this.manager = new Manager();
		this.manager.send( ma, new MsgCmdSendInstances(), new Instance());
	}


	@Test
	public void testSendWhenRabbitIsDown() throws Exception {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		ManagedApplication ma = new ManagedApplication( app, null );

		TestClientDm client = (TestClientDm) this.manager.getMessagingClient();
		client.failMessageSending.set( true );

		this.manager.update();
		this.manager.send( ma, new MsgCmdSendInstances(), app.getMySqlVm());
	}


	@Test
	public void testResynchronizeAgents_withConnection() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		TestClientDm client = (TestClientDm) this.manager.getMessagingClient();

		this.manager.resynchronizeAgents( ma );
		Assert.assertEquals( 0, client.sentMessages.size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		this.manager.resynchronizeAgents( ma );
		Assert.assertEquals( 1, client.sentMessages.size());
		Assert.assertEquals( MsgCmdResynchronize.class, client.sentMessages.get( 0 ).getClass());

		client.sentMessages.clear();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.resynchronizeAgents( ma );
		Assert.assertEquals( 2, client.sentMessages.size());
		Assert.assertEquals( MsgCmdResynchronize.class, client.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdResynchronize.class, client.sentMessages.get( 1 ).getClass());
	}


	@Test
	public void testResynchronizeAgents_noConnection() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		TestClientDm client = (TestClientDm) this.manager.getMessagingClient();

		this.manager.getMessagingClient().closeConnection();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.manager.resynchronizeAgents( ma );
		Assert.assertEquals( 0, client.sentMessages.size());
	}


	@Test( expected = IOException.class )
	public void testResynchronizeAgents_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.manager = new Manager();
		this.manager.resynchronizeAgents( ma );
	}


	@Test
	public void testExtensibilityNotifications() {

		Assert.assertEquals( 0, this.manager.getTargetHandlers().size());
		this.manager.targetAppears( new TargetHandlerMock( "hey" ));
		Assert.assertEquals( 1, this.manager.getTargetHandlers().size());
		this.manager.targetDisappears( new TargetHandlerMock( "hey" ));
		Assert.assertEquals( 0, this.manager.getTargetHandlers().size());

		this.manager.targetDisappears( new TargetHandlerMock( "ho" ));
		Assert.assertEquals( 0, this.manager.getTargetHandlers().size());

		this.manager.targetDisappears( null );
		Assert.assertEquals( 0, this.manager.getTargetHandlers().size());

		this.manager.targetAppears( new TargetHandlerMock( "oops" ));
		Assert.assertEquals( 1, this.manager.getTargetHandlers().size());
		this.manager.targetWasModified( new TargetHandlerMock( "oops" ));
		Assert.assertEquals( 1, this.manager.getTargetHandlers().size());

		this.manager.targetAppears( new TargetHandlerMock( "new_oops" ));
		Assert.assertEquals( 2, this.manager.getTargetHandlers().size());
	}
}