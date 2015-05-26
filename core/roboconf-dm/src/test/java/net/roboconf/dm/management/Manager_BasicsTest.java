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
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
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
import net.roboconf.messaging.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.internal.client.test.TestClientDm;
import net.roboconf.messaging.internal.client.test.TestClientFactory;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
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
	private TestTargetResolver targetResolver;
	private MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();


	@Before
	public void resetManager() throws Exception {
		this.registry.addMessagingClientFactory(new TestClientFactory());

		File directory = this.folder.newFolder();
		this.targetResolver = new TestTargetResolver();

		this.manager = new Manager();
		this.manager.setTargetResolver( this.targetResolver );
		this.manager.setConfigurationDirectoryLocation( directory.getAbsolutePath());
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.manager.start();

		// Reconfigure with the messaging client factory registry set.
		this.manager.getMessagingClient().setRegistry(this.registry);
		this.manager.reconfigure();

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
		File dir = new File( "./applications" );
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
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

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
				ConfigurationUtils.findInstancesRelativeLocation( app.getName()));

		Assert.assertFalse( instancesFile.exists());
		this.manager.saveConfiguration( new ManagedApplication( app ));
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
	public void testStop_messagingException() throws Exception {

		this.msgClient.failListeningToTheDm.set( true );
		this.msgClient.failClosingConnection.set( true );

		Assert.assertNotNull( this.manager.timer );
		this.manager.stop();
		Assert.assertNull( this.manager.timer );
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDeleteApplication_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.manager.getNameToManagedApplication().put( app.getName(), ma );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.deleteApplication( ma );
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.manager.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.deleteApplication( ma );
		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		String existingInstanceName = app.getMySqlVm().getName();
		this.manager.addInstance( ma, null, new Instance( existingInstanceName ));
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_childInstance() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		String existingInstanceName = app.getMySql().getName();
		this.manager.addInstance( ma, app.getMySqlVm(), new Instance( existingInstanceName ));
	}


	@Test
	public void testAddInstance_successRoot() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Instance newInstance = new Instance( "mail-vm" ).component( app.getMySqlVm().getComponent());

		this.manager.addInstance( ma, null, newInstance );
		Assert.assertEquals( 3, app.getRootInstances().size());
		Assert.assertTrue( app.getRootInstances().contains( newInstance ));
	}


	@Test( expected = IOException.class )
	public void testAddInstance_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Instance newInstance = new Instance( "mail-vm" ).component( app.getMySqlVm().getComponent());

		this.manager = new Manager();
		this.manager.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.addInstance( ma, null, newInstance );
	}


	@Test
	public void testAddInstance_successChild() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.manager.getNameToManagedApplication().put( app.getName(), ma );

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
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.removeInstance( ma, app.getMySqlVm());
	}


	@Test
	public void testRemoveInstance_success_1() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.removeInstance( ma, app.getTomcatVm());

		Assert.assertEquals( 1, app.getRootInstances().size());
		Assert.assertEquals( app.getMySqlVm(), app.getRootInstances().iterator().next());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		List<Message> messages = ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, messages.get( 0 ).getClass());
		Assert.assertEquals(
				InstanceHelpers.computeInstancePath( app.getTomcatVm()),
				((MsgCmdRemoveInstance) messages.get( 0 )).getInstancePath());
	}


	@Test
	public void testRemoveInstance_success_2() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

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
		ManagedApplication ma = new ManagedApplication( app );

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager = new Manager();
		this.manager.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.removeInstance( ma, app.getTomcat());
	}


	@Test
	public void testCreateApplication_withTags() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Assert.assertEquals( 0, this.manager.getApplicationTemplates().size());

		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.getApplicationTemplates().size());

		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
		ManagedApplication ma = this.manager.createApplication( "toto", "desc", tpl.getName(), tpl.getQualifier());
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		Assert.assertEquals( ma.getDirectory().getName(), ma.getName());
		Assert.assertEquals( "toto", ma.getName());

		File expected = new File( this.manager.configurationDirectory, ConfigurationUtils.APPLICATIONS );
		Assert.assertEquals( expected, ma.getDirectory().getParentFile());
	}


	@Test( expected = AlreadyExistingException.class )
	public void testCreateApplication_conflict() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Assert.assertEquals( 0, this.manager.getApplicationTemplates().size());

		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.getApplicationTemplates().size());

		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
		ManagedApplication ma = this.manager.createApplication( "toto", "desc", tpl.getName(), tpl.getQualifier());
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		this.manager.createApplication( "toto", "desc", tpl );
	}


	@Test( expected = InvalidApplicationException.class )
	public void testCreateApplication_invalidTemplate() throws Exception {

		this.manager.createApplication( "toto", "desc", "whatever", null );
	}


	@Test( expected = InvalidApplicationException.class )
	public void testDeleteApplicationTemplate_inexisting() throws Exception {

		this.manager.deleteApplicationTemplate( "whatever", "version" );
	}


	@Test
	public void testDeleteApplicationTemplate_success() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Assert.assertEquals( 0, this.manager.getApplicationTemplates().size());

		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.getApplicationTemplates().size());

		this.manager.deleteApplicationTemplate( tpl.getName(), tpl.getQualifier());
		Assert.assertEquals( 0, this.manager.getApplicationTemplates().size());
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDeleteApplicationTemplate_unauthorized() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Assert.assertEquals( 0, this.manager.getApplicationTemplates().size());

		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.getApplicationTemplates().size());

		Application app = new Application( "test", tpl );
		this.manager.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));

		this.manager.deleteApplicationTemplate( tpl.getName(), tpl.getQualifier());
	}


	@Test
	public void testLoadNewApplication_success() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Assert.assertEquals( 0, this.manager.getApplicationTemplates().size());

		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.getApplicationTemplates().size());

		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
		ManagedApplication ma = this.manager.createApplication( "toto", "desc", tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		Assert.assertEquals( ma.getDirectory().getName(), ma.getName());
		Assert.assertEquals( "toto", ma.getApplication().getName());

		File expected = new File( this.manager.configurationDirectory, ConfigurationUtils.APPLICATIONS );
		Assert.assertEquals( expected, ma.getDirectory().getParentFile());
	}


	@Test( expected = AlreadyExistingException.class )
	public void testLoadApplicationTemplate_conflict() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		try {
			this.manager.loadApplicationTemplate( directory );

		} catch( Exception e ) {
			Assert.fail( "Loading the application the first time should not fail." );
		}

		this.manager.loadApplicationTemplate( directory );
	}


	@Test( expected = IOException.class )
	public void testLoadApplicationTemplate_invalidDirectory() throws Exception {

		File source = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( source.exists());

		File apps = new File( this.manager.configurationDirectory, ConfigurationUtils.TEMPLATES );
		File target = new File( apps, "Legacy LAMP - sample/sub/dir" );
		Utils.copyDirectory( source, target );
		this.manager.loadApplicationTemplate( target );
	}


	@Test( expected = IOException.class )
	public void testLoadApplicationTemplate_invalidConfiguration() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());

		this.manager = new Manager();
		this.manager.loadApplicationTemplate( directory );
	}


	@Test( expected = InvalidApplicationException.class )
	public void testApplicationTemplate_invalidTemplate() throws Exception {
		this.manager.loadApplicationTemplate( this.folder.newFolder());
	}


	@Test( expected = IOException.class )
	public void testCheckConfiguration_noConfiguration() throws Exception {

		this.manager = new Manager();
		this.manager.checkConfiguration();
	}


	@Test( expected = IOException.class )
	public void testCheckConfiguration_invalidConfiguration() throws Exception {

		this.manager.setMessagingType("whatever");
		this.manager.reconfigure();
		this.manager.checkConfiguration();
	}


	@Test
	public void testCheckConfiguration() throws Exception {
		this.manager.checkConfiguration();
	}


	@Test
	public void testReconfigure_noDirectory() {

		this.manager = new Manager();
		this.manager.reconfigure();

		File defaultConfigurationDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf-dm" );
		Assert.assertEquals( defaultConfigurationDirectory, this.manager.configurationDirectory );
	}


	@Test
	public void testConfigurationChanged_withApps_noInstanceDeployed() throws Exception {

		// Copy an application in the configuration
		File source = TestUtils.findApplicationDirectory( "lamp" );
		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( source );
		this.manager.createApplication( "lamp3", "test", tpl );

		// Reset the manager's configuration (simply reload it)
		this.manager.reconfigure();
		this.manager.checkConfiguration();

		// Check there is an application
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());
		ManagedApplication ma = this.manager.getNameToManagedApplication().get( "lamp3" );
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
		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
	}


	@Test
	public void testConfigurationChanged_andShutdown_withApps_withInstances() throws Exception {

		// Copy an application in the configuration
		File source = TestUtils.findApplicationDirectory( "lamp" );
		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( source );
		ManagedApplication ma = this.manager.createApplication( "lamp", "test", tpl );
		Instance apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertNotNull( apache );

		// Make sure the VM is considered as deployed in the pseudo-IaaS
		TargetHandler th = this.targetResolver.findTargetHandler( null, ma, apache ).getHandler();
		Assert.assertNotNull( th );
		th.createMachine( null, null, apache.getName(), ma.getName());

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
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());
		ma = this.manager.getNameToManagedApplication().get( "lamp" );
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
		File source = TestUtils.findApplicationDirectory( "lamp" );
		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( source );
		ManagedApplication ma = this.manager.createApplication( "lamp2", "test", tpl );
		Instance apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertNotNull( apache );

		// Make sure the VM is considered as deployed in the pseudo-IaaS
		TargetHandler th = this.targetResolver.findTargetHandler( null, ma, apache ).getHandler();
		Assert.assertNotNull( th );
		String machineId = th.createMachine( null, null, apache.getName(), ma.getName());

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
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());
		ma = this.manager.getNameToManagedApplication().get( "lamp2" );
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


	@Test
	public void testSendWhenNoConnection() throws Exception {

		ManagedApplication ma = new ManagedApplication( new TestApplication());

		this.manager.getMessagingClient().closeConnection();
		this.manager.send( ma, new MsgCmdSendInstances(), new Instance());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());

		this.manager.stop();
		this.manager.send( ma, new MsgCmdSendInstances(), new Instance());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
	}


	@Test
	public void testSendWhenInvalidConfiguration() throws Exception {

		ManagedApplication ma = new ManagedApplication( new TestApplication());
		this.manager = new Manager();
		this.manager.send( ma, new MsgCmdSendInstances(), new Instance());
	}


	@Test
	public void testSendWhenRabbitIsDown() throws Exception {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		ManagedApplication ma = new ManagedApplication( app );

		this.msgClient.connected.set( true );
		this.msgClient.failMessageSending.set( true );
		this.manager.send( ma, new MsgCmdSendInstances(), app.getMySqlVm());
	}


	@Test
	public void testResynchronizeAgents_withConnection() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

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
		ManagedApplication ma = new ManagedApplication( app );

		this.manager.getMessagingClient().closeConnection();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.manager.resynchronizeAgents( ma );
	}


	@Test( expected = IOException.class )
	public void testResynchronizeAgents_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
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
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		this.msgClient.sentMessages.clear();
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());

		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( app.getName(), app.getMySqlVm(), "192.168.1.45" );
		msg.setModelRequired( true );

		this.manager.getMessagingClient().getMessageProcessor().storeMessage( msg );
		Thread.sleep( 100 );
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());

		Message sentMessage = this.msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, sentMessage.getClass());
		Assert.assertNotNull(((MsgCmdSetScopedInstance) sentMessage).getScopedInstance());
	}


	@Test
	public void testMsgNotifHeartbeat_requestModel_nonRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		this.msgClient.sentMessages.clear();
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());

		// War is not a target / scoped instance: nothing will happen
		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( app.getName(), app.getWar(), "192.168.1.45" );
		msg.setModelRequired( true );

		this.manager.getMessagingClient().getMessageProcessor().storeMessage( msg );
		Thread.sleep( 100 );
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());

		// Let's try again, but we change the WAR installer
		app.getWar().getComponent().installerName( Constants.TARGET_INSTALLER );

		this.manager.getMessagingClient().getMessageProcessor().storeMessage( msg );
		Thread.sleep( 100 );
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());

		Message sentMessage = this.msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, sentMessage.getClass());
		Assert.assertNotNull(((MsgCmdSetScopedInstance) sentMessage).getScopedInstance());
	}


	@Test
	public void applicationsShouldBeDeletedEvenWhenNoMessagingServer() throws Exception {

		this.manager = new Manager();

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
		this.manager.getNameToManagedApplication().put( app.getName(), ma );
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		try {
			this.manager.checkConfiguration();
			Assert.fail( "An exception should have been thrown, there is no messaging server in this test!" );

		} catch( Exception e ) {
			// ignore
		}

		this.manager.deleteApplication( ma );
		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
	}


	@Test
	public void testSomeGetters() throws Exception {

		Assert.assertEquals( 0, this.manager.getRawApplicationTemplates().size());
		Assert.assertNull( this.manager.findApplicationByName( "invalid" ));

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( app, this.manager.findApplicationByName( app.getName()));
	}
}
