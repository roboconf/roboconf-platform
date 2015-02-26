/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
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
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TargetHandlerMock;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.internal.client.test.TestClientDm;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;
import net.roboconf.messaging.messages.from_dm_to_dm.MsgEcho;
import net.roboconf.target.api.TargetHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class Manager_BasicsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Manager manager;
	private TestClientDm msgClient;


	@Before
	public void resetManager() throws Exception {

		File directory = this.folder.newFolder();

		this.manager = new Manager();
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setConfigurationDirectoryLocation( directory.getAbsolutePath());
		this.manager.setMessagingFactoryType( MessagingConstants.FACTORY_TEST );
		this.manager.start();

		this.msgClient = TestUtils.getInternalField( this.manager.getMessagingClient(), "messagingClient", TestClientDm.class );
		this.msgClient.sentMessages.clear();

		// Disable the messages timer for predictability
		this.manager.timer.cancel();
	}


	@After
	public void stopManager() throws Exception {
		this.manager.stop();

		// Some tests create a new manager, which save instances
		// at the current project's root when it is stopped.
		File dir = new File( "./instances" );
		Utils.deleteFilesRecursively( dir );
	}


	@Test
	public void testSendPingMessageQueue() throws Exception {

		this.manager.pingMessageQueue( "TEST", 0L ); // false because there is no MQ
		List<Message> sentMessages = this.msgClient.sentMessages;
		Assert.assertEquals( 1, sentMessages.size());

		Message message = sentMessages.get( 0 );
		Assert.assertTrue( message instanceof MsgEcho );

		MsgEcho echo = (MsgEcho) message;
		Assert.assertEquals( "TEST", echo.getContent() );
	}


	@Test
	public void testSendPingAgent() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, this.folder.newFolder() );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		// Ping all the root instances.
		for (Instance i : app.getRootInstances()) {
			this.manager.pingAgent( app, i, "TEST " + i.getName(), 0L ); // false because there is no MQ
		}

		List<Message> sentMessages = this.msgClient.sentMessages;
		// Now check the DM has sent a ping every agent.
		Assert.assertTrue( sentMessages.size() == app.getRootInstances().size() );
		int index = 0;
		for (Instance i : app.getRootInstances()) {
			Message message = sentMessages.get( index++ );
			Assert.assertTrue( message instanceof MsgEcho );
			MsgEcho echo = (MsgEcho) message;
			Assert.assertEquals( "PING:TEST " + i.getName(), echo.getContent() );
		}
	}


	@Test
	public void testSaveConfiguration() {

		TestApplication app = new TestApplication();
		File instancesFile = new File(
				this.manager.configurationDirectory,
				ConfigurationUtils.INSTANCES + "/" + app.getName() + ".instances" );

		Assert.assertFalse( instancesFile.exists());
		this.manager.saveConfiguration( new ManagedApplication( app, null ));
		Assert.assertTrue( instancesFile.exists());
	}


	@Test
	public void testStop() throws Exception {

		Assert.assertNotNull( this.manager.timer );
		this.manager.stop();
		Assert.assertNull( this.manager.timer );

		this.manager.stop();
		Assert.assertNull( this.manager.timer );
	}


	@Test
	public void testStop_invalidConfiguration() throws Exception {

		this.manager = new Manager();

		Assert.assertNull( this.manager.timer );
		this.manager.stop();
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

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.removeInstance( ma, app.getTomcat());

		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( app.getMySqlVm(), app.getRootInstances().iterator().next());
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, this.msgClient.sentMessages.get( 0 ).getClass());

		MsgCmdRemoveInstance msg = (MsgCmdRemoveInstance) this.msgClient.sentMessages.get( 0 );
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

		this.manager.setMessageServerIp( "whatever" );
		this.manager.setMessagingFactoryType( "whatever" );
		this.manager.reconfigure();
		this.manager.checkConfiguration();
	}


	@Test
	public void testCheckConfiguration() throws Exception {
		this.manager.checkConfiguration();
	}


	@Test
	public void testConfigurationChanged_withApps_noInstance() throws Exception {

		// Copy an application in the configuration
		this.manager.checkConfiguration();
		File source = TestUtils.findTestFile( "/lamp" );
		Assert.assertTrue( source.exists());

		File apps = new File( this.manager.configurationDirectory, ConfigurationUtils.APPLICATIONS );
		File target = new File( apps, "Legacy LAMP" );
		Utils.copyDirectory( source, target );

		// Reset the manager's configuration (simply reload it)
		this.manager.reconfigure();
		this.manager.checkConfiguration();

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
		this.manager.reconfigure();
		this.manager.checkConfiguration();
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

		// Make sure the VM is considered as deployed in the pseudo-IaaS
		TargetHandler th = this.manager.targetResolver.findTargetHandler( null, ma, apache ).getHandler();
		Assert.assertNotNull( th );
		th.createMachine( null, null, null, null, apache.getName(), ma.getName());

		// Update the instances
		apache.data.put( Instance.IP_ADDRESS, "192.168.1.23" );
		apache.data.put( Instance.MACHINE_ID, "my id" );
		apache.data.put( "whatever", "something" );
		apache.setStatus( InstanceStatus.PROBLEM );

		// Save the manager's state
		this.manager.stop();

		// Reset the manager (reload the configuration)
		this.manager.reconfigure();

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
		Assert.assertEquals( "192.168.1.23", apache.data.get( Instance.IP_ADDRESS ));
		Assert.assertEquals( "my id", apache.data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( "something", apache.data.get( "whatever" ));
		Assert.assertEquals( ma.getName(), apache.data.get( Instance.APPLICATION_NAME ));

		// It is considered started because upon a reconfiguration, the IaaS is contacted
		// to determine whether a VM runs or not.
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, apache.getStatus());
	}


	@Test
	public void testConfigurationChanged_andShutdown_withApps_withInstances_vmWasKilled() throws Exception {

		// Copy an application in the configuration
		File source = TestUtils.findTestFile( "/lamp" );
		ManagedApplication ma = this.manager.loadNewApplication( source );
		Instance apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertNotNull( apache );

		// Make sure the VM is considered as deployed in the pseudo-IaaS
		TargetHandler th = this.manager.targetResolver.findTargetHandler( null, ma, apache ).getHandler();
		Assert.assertNotNull( th );
		String machineId = th.createMachine( null, null, null, null, apache.getName(), ma.getName());

		// Update the instances
		apache.data.put( Instance.IP_ADDRESS, "192.168.1.23" );
		apache.data.put( Instance.MACHINE_ID, "my id" );
		apache.data.put( "whatever", "something" );
		apache.setStatus( InstanceStatus.PROBLEM );

		// Save the manager's state
		this.manager.stop();

		//
		// Here is the difference with #testConfigurationChanged_andShutdown_withApps_withInstances
		// We simulate the fact that the VM was killed why the DM was stopped.
		//
		th.terminateMachine( null, machineId );

		// Reset the manager (reload the configuration)
		this.manager.reconfigure();

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
		Assert.assertNull( apache.data.get( Instance.IP_ADDRESS ));
		Assert.assertNull( apache.data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( "something", apache.data.get( "whatever" ));
		Assert.assertEquals( ma.getName(), apache.data.get( Instance.APPLICATION_NAME ));

		// The VM was killed outside the DM. Upon restoration, the DM
		// contacts the IaaS and sets the NOT_DEPLOYED status.
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, apache.getStatus());
	}


	@Test( expected = InvalidApplicationException.class )
	public void testCheckErrors_withCriticalError() throws Exception {

		RoboconfError error = new ModelError( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, 2 );
		this.manager.checkErrors( Arrays.asList( error ));
	}


	@Test
	public void testCheckErrors_withWarningOnly() throws Exception {

		RoboconfError error = new ModelError( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, 2 );
		this.manager.checkErrors( Arrays.asList( error ));
	}


	@Test
	public void testCheckErrors_withnoErrorOrWarning() throws Exception {
		this.manager.checkErrors( new ArrayList<RoboconfError> ());
	}


	@Test
	public void testSendWhenNoConnection() throws Exception {

		ManagedApplication ma = new ManagedApplication( new TestApplication(), null );

		this.manager.getMessagingClient().closeConnection();
		this.manager.send( ma, new MsgCmdSendInstances(), new Instance());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());

		this.manager.stop();
		this.manager.send( ma, new MsgCmdSendInstances(), new Instance());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
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

		this.msgClient.failMessageSending.set( true );
		this.manager.reconfigure();
		this.manager.send( ma, new MsgCmdSendInstances(), app.getMySqlVm());
	}


	@Test
	public void testResynchronizeAgents_withConnection() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );

		this.manager.resynchronizeAgents( ma );
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		this.manager.resynchronizeAgents( ma );
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());
		Assert.assertEquals( MsgCmdResynchronize.class, this.msgClient.sentMessages.get( 0 ).getClass());

		this.msgClient.sentMessages.clear();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.resynchronizeAgents( ma );
		Assert.assertEquals( 2, this.msgClient.sentMessages.size());
		Assert.assertEquals( MsgCmdResynchronize.class, this.msgClient.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdResynchronize.class, this.msgClient.sentMessages.get( 1 ).getClass());
	}


	@Test( expected = IOException.class )
	public void testResynchronizeAgents_noConnection() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );

		this.manager.getMessagingClient().closeConnection();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.manager.resynchronizeAgents( ma );
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
		this.manager.targetAppears( null );
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


	@Test
	public void testMsgNotifHeartbeat_requestModel() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		this.msgClient.sentMessages.clear();
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());

		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( app.getName(), app.getMySqlVm(), "192.168.1.45" );
		msg.setModelRequired( true );

		this.manager.getMessagingClient().getMessageProcessor().storeMessage( msg );
		Thread.sleep( 100 );
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());

		Message sentMessage = this.msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdSetRootInstance.class, sentMessage.getClass());
		Assert.assertNotNull(((MsgCmdSetRootInstance) sentMessage).getRootInstance());
	}
}
