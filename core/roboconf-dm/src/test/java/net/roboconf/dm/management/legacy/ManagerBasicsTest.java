/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.management.legacy;

import static net.roboconf.core.Constants.LOCAL_RESOURCE_PREFIX;
import static net.roboconf.core.Constants.PROJECT_DIR_DESC;
import static net.roboconf.core.Constants.PROJECT_DIR_GRAPH;
import static net.roboconf.core.Constants.PROJECT_FILE_DESCRIPTOR;
import static net.roboconf.core.Constants.SCOPED_SCRIPT_AT_AGENT_SUFFIX;
import static net.roboconf.core.Constants.SCOPED_SCRIPT_AT_DM_CONFIGURE_SUFFIX;
import static net.roboconf.core.Constants.TARGET_INSTALLER;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ApplicationTemplateDescriptor;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifMachineDown;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.target.api.TargetHandler;
import net.roboconf.target.api.TargetHandlerParameters;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class ManagerBasicsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private TestClient msgClient;
	private TestTargetResolver targetResolver;


	@Before
	public void resetManager() throws Exception {

		File directory = this.folder.newFolder();
		resetManager( directory );
	}


	private void resetManager( File directory ) throws Exception {

		this.targetResolver = new TestTargetResolver();

		this.manager = new Manager();
		this.manager.setTargetResolver( this.targetResolver );
		this.manager.configurationMngr().setWorkingDirectory( directory );
		this.manager.setMessagingType( MessagingConstants.FACTORY_TEST );
		this.manager.start();

		// Register mocked listeners - mainly for code coverage reasons
		this.manager.listenerAppears( Mockito.mock( IDmListener.class ));

		// Create the wrapper
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		this.msgClient = (TestClient) this.managerWrapper.getInternalMessagingClient();
		this.msgClient.clearMessages();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class ).cancel();
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
	public void testStop() throws Exception {

		Timer timer = TestUtils.getInternalField( this.manager, "timer", Timer.class );
		Assert.assertNotNull( timer );
		this.manager.stop();

		timer = TestUtils.getInternalField( this.manager, "timer", Timer.class );
		Assert.assertNull( timer );

		this.manager.stop();
		Assert.assertNull( timer );
	}


	@Test
	public void testStop_invalidConfiguration() throws Exception {

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper = new TestManagerWrapper( this.manager );

		Timer timer = TestUtils.getInternalField( this.manager, "timer", Timer.class );
		Assert.assertNull( timer );

		this.manager.stop();
		timer = TestUtils.getInternalField( this.manager, "timer", Timer.class );
		Assert.assertNull( timer );
	}


	@Test
	public void testStop_messagingException() throws Exception {

		this.msgClient.failSubscribing.set( true );
		this.msgClient.failClosingConnection.set( true );

		Timer timer = TestUtils.getInternalField( this.manager, "timer", Timer.class );
		Assert.assertNotNull( timer );

		this.manager.stop();
		timer = TestUtils.getInternalField( this.manager, "timer", Timer.class );
		Assert.assertNull( timer );
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.managerWrapper.addManagedApplication( ma );

		String existingInstanceName = app.getMySqlVm().getName();
		this.manager.instancesMngr().addInstance( ma, null, new Instance( existingInstanceName ));
	}


	@Test( expected = ImpossibleInsertionException.class )
	public void testAddInstance_impossibleInsertion_childInstance() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.managerWrapper.addManagedApplication( ma );

		String existingInstanceName = app.getMySql().getName();
		this.manager.instancesMngr().addInstance( ma, app.getMySqlVm(), new Instance( existingInstanceName ));
	}


	@Test
	public void testAddInstance_successRoot() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Instance newInstance = new Instance( "mail-vm" ).component( app.getMySqlVm().getComponent());

		this.manager.instancesMngr().addInstance( ma, null, newInstance );
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
		this.managerWrapper = new TestManagerWrapper( this.manager );

		this.managerWrapper.addManagedApplication( ma );
		this.manager.instancesMngr().addInstance( ma, null, newInstance );
	}


	@Test
	public void testAddInstance_successChild() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.managerWrapper.addManagedApplication( ma );

		// Insert a MySQL instance under the Tomcat VM
		Assert.assertEquals( 1, app.getTomcatVm().getChildren().size());
		Instance newInstance = new Instance( app.getMySql().getName()).component( app.getMySql().getComponent());

		this.manager.instancesMngr().addInstance( ma, app.getTomcatVm(), newInstance );
		Assert.assertEquals( 2, app.getTomcatVm().getChildren().size());
		Assert.assertTrue( app.getTomcatVm().getChildren().contains( newInstance ));
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testRemoveInstance_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		app.getMySql().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().removeInstance( ma, app.getMySqlVm());
	}


	@Test
	public void testRemoveInstance_success_1() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().removeInstance( ma, app.getTomcatVm());

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
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().removeInstance( ma, app.getTomcat());

		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( app.getMySqlVm(), app.getRootInstances().iterator().next());
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, this.msgClient.allSentMessages.get( 0 ).getClass());

		MsgCmdRemoveInstance msg = (MsgCmdRemoveInstance) this.msgClient.allSentMessages.get( 0 );
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), msg.getInstancePath());
	}


	@Test( expected = IOException.class )
	public void testRemoveInstance_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 2, app.getRootInstances().size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper = new TestManagerWrapper( this.manager );

		this.managerWrapper.addManagedApplication( ma );
		this.manager.instancesMngr().removeInstance( ma, app.getTomcat());
	}


	@Test
	public void testCreateApplication_withTags() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());

		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());

		Assert.assertEquals( 0, this.managerWrapper.getNameToManagedApplication().size());
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "toto", "desc", tpl.getName(), tpl.getVersion());
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.managerWrapper.getNameToManagedApplication().size());

		Assert.assertEquals( ma.getDirectory().getName(), ma.getName());
		Assert.assertEquals( "toto", ma.getName());

		File expected = new File( this.manager.configurationMngr().getWorkingDirectory(), ConfigurationUtils.APPLICATIONS );
		Assert.assertEquals( expected, ma.getDirectory().getParentFile());
	}


	@Test( expected = AlreadyExistingException.class )
	public void testCreateApplication_conflict() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());

		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());

		Assert.assertEquals( 0, this.managerWrapper.getNameToManagedApplication().size());
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "toto", "desc", tpl.getName(), tpl.getVersion());
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.managerWrapper.getNameToManagedApplication().size());

		this.manager.applicationMngr().createApplication( "toto", "desc", tpl );
	}


	@Test
	public void testLoadNewApplication_success() throws Exception {

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());

		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());

		Assert.assertEquals( 0, this.managerWrapper.getNameToManagedApplication().size());
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "toto", "desc", tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.managerWrapper.getNameToManagedApplication().size());

		Assert.assertEquals( ma.getDirectory().getName(), ma.getName());
		Assert.assertEquals( "toto", ma.getApplication().getName());

		File expected = new File( this.manager.configurationMngr().getWorkingDirectory(), ConfigurationUtils.APPLICATIONS );
		Assert.assertEquals( expected, ma.getDirectory().getParentFile());
	}


	@Test
	public void testLoadNewApplication_targetConflict() throws Exception {

		// Copy an application and add it a target.properties
		File firstDirectory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( firstDirectory.exists());

		File directory = this.folder.newFolder();
		Utils.copyDirectory( firstDirectory, directory );

		File targetDir = new File( directory, PROJECT_DIR_GRAPH + "/VM" );
		Assert.assertTrue( targetDir.mkdir());

		String content = "id = test-target-conflict\nhandler: whatever";
		Utils.writeStringInto( content, new File( targetDir, "target.properties" ));

		// Load the template once
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 0, this.manager.targetsMngr().listAllTargets().size());
		ApplicationTemplate tpl1 = this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );

		Assert.assertNotNull( tpl1 );
		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 1, this.manager.targetsMngr().listAllTargets().size());

		// Update the source directory (change the application name, but keep the target properties).
		// So, we have two templates that share a same target properties => conflict.
		File f = new File( directory, PROJECT_DIR_DESC + "/" + PROJECT_FILE_DESCRIPTOR );
		ApplicationTemplateDescriptor desc = ApplicationTemplateDescriptor.load( f );
		desc.setName( "abcdefghijklmnopqrstuvwxyz" );
		ApplicationTemplateDescriptor.save( f, desc );

		// Load the new application: it should fail
		try {
			this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
			Assert.fail( "An exception was expected here." );

		} catch( Exception e ) {
			// nothing
		}

		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 1, this.manager.targetsMngr().listAllTargets().size());
	}


	@Test
	public void testLoadNewApplication_deleteIt_redeployIt_success() throws Exception {

		// Copy an application and add it a target.properties
		File firstDirectory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( firstDirectory.exists());

		File directory = this.folder.newFolder();
		Utils.copyDirectory( firstDirectory, directory );

		File targetDir = new File( directory, PROJECT_DIR_GRAPH + "/VM" );
		Assert.assertTrue( targetDir.mkdir());

		String content = "id = test-target\nhandler: whatever";
		Utils.writeStringInto( content, new File( targetDir, "target.properties" ));

		// Deploy the template
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 0, this.manager.targetsMngr().listAllTargets().size());

		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 1, this.manager.targetsMngr().listAllTargets().size());

		// Undeploy it
		this.manager.applicationTemplateMngr().deleteApplicationTemplate( tpl.getName(), tpl.getVersion());
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 1, this.manager.targetsMngr().listAllTargets().size());

		// Update a little bit the target properties
		content = "id = test-target\nhandler: whatever2";
		Utils.writeStringInto( content, new File( targetDir, "target.properties" ));

		// Deploy it again.
		// We should not have any conflict related to the target.
		tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 1, this.manager.targetsMngr().listAllTargets().size());

		// Undeploy it, again
		this.manager.applicationTemplateMngr().deleteApplicationTemplate( tpl.getName(), tpl.getVersion());
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 1, this.manager.targetsMngr().listAllTargets().size());

		// Change the target ID
		content = "id = test-other-target\nhandler: whatever2";
		Utils.writeStringInto( content, new File( targetDir, "target.properties" ));

		// Deploy it again.
		// We should not have any conflict (different target ID).
		tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
		Assert.assertNotNull( tpl );
		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 2, this.manager.targetsMngr().listAllTargets().size());
	}


	@Test
	public void testLoadNewApplication_withTargets_invalidAppLocation() throws Exception {

		// Copy an application and add it a target.properties
		File firstDirectory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( firstDirectory.exists());

		File normalTplDirectory = ConfigurationUtils.findTemplateDirectory(
				new ApplicationTemplate( "Legacy LAMP" ).version( "1.0.1-SNAPSHOT" ),
				this.manager.configurationMngr().getWorkingDirectory());

		File directory = new File( normalTplDirectory, "intermediate/subdir" );
		Assert.assertTrue( directory.mkdirs());
		Utils.copyDirectory( firstDirectory, directory );

		File targetDir = new File( directory, PROJECT_DIR_GRAPH + "/VM" );
		Assert.assertTrue( targetDir.mkdir());

		String content = "id = test-target-conflict\nhandler: whatever";
		Utils.writeStringInto( content, new File( targetDir, "target.properties" ));

		// Load the template.
		// It should fail, since it is located under the directory where the DM would save it.
		// We want to verify that the targets loaded from this template are correctly unregistered.
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 0, this.manager.targetsMngr().listAllTargets().size());
		try {
			this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
			Assert.fail( "An exception was expected here." );

		} catch( Exception e ) {
			// nothing
		}

		// No target and template were registered
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 0, this.manager.targetsMngr().listAllTargets().size());
	}


	@Test
	public void testLoadApplicationTemplate_invalidConfiguration() throws Exception {

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper = new TestManagerWrapper( this.manager );

		// No messaging is configured
		try {
			this.manager.messagingMngr().checkMessagingConfiguration();
			Assert.fail( "The configuration is supposed to be invalid." );

		} catch( IOException e ) {
			// nothing
		}

		File directory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( directory.exists());
		this.manager.applicationTemplateMngr().loadApplicationTemplate( directory );
	}


	@Test
	public void testConfigurationChanged_withApps_noInstanceDeployed() throws Exception {

		// Copy an application in the configuration
		File source = TestUtils.findApplicationDirectory( "lamp" );
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( source );
		this.manager.applicationMngr().createApplication( "lamp3", "test", tpl );

		// Reset the manager's configuration (simply reload it)
		this.manager.reconfigure();
		this.manager.messagingMngr().checkMessagingConfiguration();

		// Check there is an application
		Assert.assertEquals( 1, this.managerWrapper.getNameToManagedApplication().size());
		ManagedApplication ma = this.managerWrapper.getNameToManagedApplication().get( "lamp3" );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());
		Assert.assertEquals( 6, InstanceHelpers.getAllInstances( ma.getApplication()).size());

		for( Instance inst : InstanceHelpers.getAllInstances( ma.getApplication()))
			Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, inst.getStatus());
	}


	@Test
	public void testConfigurationChanged_andShutdown_withApps_withInstances() throws Exception {

		// Copy an application in the configuration
		File source = TestUtils.findApplicationDirectory( "lamp" );
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( source );
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "lamp", "test", tpl );
		Instance apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertNotNull( apache );
		String path = InstanceHelpers.computeInstancePath( apache );

		// Make sure the VM is considered as deployed in the pseudo-IaaS
		TargetHandler th = this.targetResolver.findTargetHandler( new HashMap<String,String>( 0 ));
		Assert.assertNotNull( th );
		th.createMachine( new TargetHandlerParameters().scopedInstancePath( path ).applicationName( ma.getName()));

		// Update the instances
		apache.data.put( Instance.IP_ADDRESS, "192.168.1.23" );
		apache.data.put( Instance.MACHINE_ID, path );
		apache.data.put( "whatever", "something" );
		apache.setStatus( InstanceStatus.PROBLEM );

		// Save the manager's state
		this.manager.stop();

		// Reset the manager (reload the configuration)
		this.manager.reconfigure();

		// Check there is the right application
		Assert.assertEquals( 1, this.managerWrapper.getNameToManagedApplication().size());
		ma = this.managerWrapper.getNameToManagedApplication().get( "lamp" );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 3, ma.getApplication().getRootInstances().size());
		Assert.assertEquals( 6, InstanceHelpers.getAllInstances( ma.getApplication()).size());

		for( Instance inst : InstanceHelpers.getAllInstances( ma.getApplication())) {
			if( ! inst.equals( apache ))
				Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}

		apache = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/Apache VM" );
		Assert.assertEquals( "192.168.1.23", apache.data.get( Instance.IP_ADDRESS ));
		Assert.assertEquals( path, apache.data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( "something", apache.data.get( "whatever" ));
		Assert.assertEquals( ma.getName(), apache.data.get( Instance.APPLICATION_NAME ));
	}


	@Test
	public void testResynchronizeAgents_withConnection() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.manager.instancesMngr().resynchronizeAgents( ma );
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		this.manager.instancesMngr().resynchronizeAgents( ma );
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());
		Assert.assertEquals( MsgCmdResynchronize.class, this.msgClient.allSentMessages.get( 0 ).getClass());

		this.msgClient.allSentMessages.clear();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().resynchronizeAgents( ma );
		Assert.assertEquals( 2, this.msgClient.allSentMessages.size());
		Assert.assertEquals( MsgCmdResynchronize.class, this.msgClient.allSentMessages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdResynchronize.class, this.msgClient.allSentMessages.get( 1 ).getClass());
	}


	@Test( expected = IOException.class )
	public void testResynchronizeAgents_noConnection() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.managerWrapper.getMessagingClient().closeConnection();
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.manager.instancesMngr().resynchronizeAgents( ma );
	}


	@Test( expected = IOException.class )
	public void testResynchronizeAgents_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		ManagedApplication ma = new ManagedApplication( app );
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.manager.instancesMngr().resynchronizeAgents( ma );
	}


	@Test
	public void testMsgNotifHeartbeat_requestModel() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		this.msgClient.allSentMessages.clear();
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( app.getName(), app.getMySqlVm(), "192.168.1.45" );
		msg.setModelRequired( true );

		this.managerWrapper.getMessagingClient().getMessageProcessor().storeMessage( msg );
		Thread.sleep( 100 );
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());

		Message sentMessage = this.msgClient.allSentMessages.get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, sentMessage.getClass());
		Assert.assertNotNull(((MsgCmdSetScopedInstance) sentMessage).getScopedInstance());
	}


	@Test
	public void testMsgNotifHeartbeat_requestModel_nonRoot() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		this.msgClient.allSentMessages.clear();
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		// War is not a target / scoped instance: nothing will happen
		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( app.getName(), app.getWar(), "192.168.1.45" );
		msg.setModelRequired( true );

		this.managerWrapper.getMessagingClient().getMessageProcessor().storeMessage( msg );
		Thread.sleep( 100 );
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		// Let's try again, but we change the WAR installer
		app.getWar().getComponent().installerName( TARGET_INSTALLER );

		this.managerWrapper.getMessagingClient().getMessageProcessor().storeMessage( msg );
		Thread.sleep( 100 );
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());

		Message sentMessage = this.msgClient.allSentMessages.get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, sentMessage.getClass());
		Assert.assertNotNull(((MsgCmdSetScopedInstance) sentMessage).getScopedInstance());
	}


	@Test
	public void applicationsShouldBeDeletedEvenWhenNoMessagingServer() throws Exception {

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper = new TestManagerWrapper( this.manager );

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		Assert.assertEquals( 0, this.managerWrapper.getNameToManagedApplication().size());
		this.managerWrapper.addManagedApplication( ma );
		Assert.assertEquals( 1, this.managerWrapper.getNameToManagedApplication().size());

		try {
			this.manager.messagingMngr().checkMessagingConfiguration();
			Assert.fail( "An exception should have been thrown, there is no messaging server in this test!" );

		} catch( Exception e ) {
			// ignore
		}

		this.manager.applicationMngr().deleteApplication( ma );
		Assert.assertEquals( 0, this.managerWrapper.getNameToManagedApplication().size());
	}


	@Test
	public void testSomeGetters() throws Exception {

		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertNull( this.manager.applicationMngr().findApplicationByName( "invalid" ));

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( app, this.manager.applicationMngr().findApplicationByName( app.getName()));
	}


	@Test
	public void verifyMsgNotifMachineDown_allowsRedeployment() throws Exception {

		// Prepare the model
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		this.managerWrapper.addManagedApplication( ma );
		String targetId = this.manager.targetsMngr().createTarget( "id: tid\nhandler: h" );
		this.manager.targetsMngr().associateTargetWith( targetId, app, null );

		// Try a first deployment
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		this.manager.instancesMngr().deployAndStartAll( ma, app.getMySqlVm());
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());

		// Simulate the incoming of a heart beat message
		DmMessageProcessor processor = (DmMessageProcessor) this.managerWrapper.getMessagingClient().getMessageProcessor();
		processor.processMessage( new MsgNotifHeartbeat( app.getName(), app.getMySqlVm(), "127.0.0.1" ));
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());

		// Simulate the incoming of a "machine down" notification
		processor.processMessage( new MsgNotifMachineDown( app.getName(), app.getMySqlVm()));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		// Try to redeploy: it should work (no remains of the previous attempt)
		this.manager.instancesMngr().deployAndStartAll( ma, app.getMySqlVm());
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
	}


	@Test
	public void verifyApplicationWithRandomVariables_load() throws Exception {

		// Deploy the template
		File dir = TestUtils.findApplicationDirectory( "app-with-random-ports" );
		Assert.assertTrue( dir.isDirectory());

		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( dir );
		Assert.assertNotNull( tpl );

		Instance instance1 = InstanceHelpers.findInstanceByPath( tpl, "/vm/container1" );
		Assert.assertNotNull( instance1 );

		// No value set for random values in templates
		Assert.assertNull( InstanceHelpers.findAllExportedVariables( instance1 ).get( "Container1.httpPort" ));

		// Create an application
		ManagedApplication app1 = this.manager.applicationMngr().createApplication( "app1", "", tpl );
		Assert.assertNotNull( app1 );

		instance1 = InstanceHelpers.findInstanceByPath( app1.getApplication(), "/vm/container1" );
		Assert.assertNotNull( instance1 );

		// Random values are set in applications
		Assert.assertEquals( "10000", InstanceHelpers.findAllExportedVariables( instance1 ).get( "Container1.ajpPort" ));
		Assert.assertEquals( "10001", InstanceHelpers.findAllExportedVariables( instance1 ).get( "Container1.httpPort" ));

		// Verify the 2nd container
		Instance instance2 = InstanceHelpers.findInstanceByPath( app1.getApplication(), "/vm/container2" );
		Assert.assertNotNull( instance2 );

		// This value is not generated since it was set manually in the instances definition
		Assert.assertEquals( "45012", InstanceHelpers.findAllExportedVariables( instance2 ).get( "Container2.port" ));
	}


	@Test
	public void verifyApplicationWithRandomVariables_restore() throws Exception {

		// Deploy and validate
		verifyApplicationWithRandomVariables_load();

		// Stop the manager
		this.manager.stop();

		// Reset it (except the configuration directory!)
		resetManager( this.manager.configurationMngr().getWorkingDirectory());

		// Verify what was restored.
		// We expect the same ports than before.
		ManagedApplication app1 = this.managerWrapper.getNameToManagedApplication().get( "app1" );
		Assert.assertNotNull( app1 );

		Instance instance1 = InstanceHelpers.findInstanceByPath( app1.getApplication(), "/vm/container1" );
		Assert.assertNotNull( instance1 );
		Assert.assertEquals( "10000", InstanceHelpers.findAllExportedVariables( instance1 ).get( "Container1.ajpPort" ));
		Assert.assertEquals( "10001", InstanceHelpers.findAllExportedVariables( instance1 ).get( "Container1.httpPort" ));

		Instance instance2 = InstanceHelpers.findInstanceByPath( app1.getApplication(), "/vm/container2" );
		Assert.assertNotNull( instance2 );
		Assert.assertEquals( "45012", InstanceHelpers.findAllExportedVariables( instance2 ).get( "Container2.port" ));
	}


	@Test
	public void testTargetScriptsChain() throws Exception {

		// We need to add targets
		File originalDirectory = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( originalDirectory.exists());

		File directoryCopy = this.folder.newFolder();
		Utils.copyDirectory( originalDirectory, directoryCopy );

		File targetDir = new File( directoryCopy, PROJECT_DIR_GRAPH + "/VM" );
		Assert.assertTrue( targetDir.mkdir());
		Assert.assertTrue( new File( targetDir, "apple/lib" ).mkdirs());
		Assert.assertTrue( new File( targetDir, "oops" ).mkdir());

		Utils.writeStringInto( "id: apple\nhandler: h", new File( targetDir, "apple.properties" ));
		Utils.writeStringInto( "", new File( targetDir, "apple/" + SCOPED_SCRIPT_AT_AGENT_SUFFIX + "sh" ));
		Utils.writeStringInto( "", new File( targetDir, "apple/" + LOCAL_RESOURCE_PREFIX + "-" + SCOPED_SCRIPT_AT_DM_CONFIGURE_SUFFIX + "sh" ));
		Utils.writeStringInto( "", new File( targetDir, "apple/lib/apple2.properties" ));
		Utils.writeStringInto( "id: oops\nhandler: h", new File( targetDir, "oops.properties" ));
		Utils.writeStringInto( "", new File( targetDir, "oops/oops.sh" ));

		// Load the application template
		Assert.assertEquals( 0, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 0, this.manager.targetsMngr().listAllTargets().size());
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( directoryCopy );
		Assert.assertEquals( 1, this.manager.applicationTemplateMngr().getApplicationTemplates().size());
		Assert.assertEquals( 2, this.manager.targetsMngr().listAllTargets().size());
		Assert.assertEquals( 2, this.manager.targetsMngr().listPossibleTargets( tpl ).size());

		// Associate scoped instances with targets
		this.manager.targetsMngr().associateTargetWith( "apple", tpl, null );
		this.manager.targetsMngr().associateTargetWith( "oops", tpl, "/MySQL VM" );
		Assert.assertEquals( "apple", this.manager.targetsMngr().findTargetId( tpl, null ));
		Assert.assertEquals( "oops", this.manager.targetsMngr().findTargetId( tpl, "/MySQL VM" ));

		// Verify the resources sent to an agent
		Map<String,byte[]> resources = this.manager.targetsMngr().findScriptResourcesForAgent( "apple" );
		Assert.assertEquals( 2, resources.size());
		Assert.assertNotNull( resources.get( SCOPED_SCRIPT_AT_AGENT_SUFFIX + "sh" ));
		Assert.assertNotNull( resources.get( "lib/apple2.properties" ));

		resources = this.manager.targetsMngr().findScriptResourcesForAgent( "oops" );
		Assert.assertEquals( 1, resources.size());
		Assert.assertNotNull( resources.get( "oops.sh" ));

		// Verify the DM's scripts
		File scriptFile = this.manager.targetsMngr().findScriptForDm( tpl, null );
		Assert.assertNotNull( scriptFile );
		Assert.assertTrue( scriptFile.exists());

		Instance scopedInstance = InstanceHelpers.findInstanceByPath( tpl, "/MySQL VM" );
		Assert.assertNotNull( scopedInstance );

		scriptFile = this.manager.targetsMngr().findScriptForDm( tpl, scopedInstance );
		Assert.assertNull( scriptFile );
	}
}
