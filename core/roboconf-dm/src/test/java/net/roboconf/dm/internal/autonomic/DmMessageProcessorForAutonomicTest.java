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

package net.roboconf.dm.internal.autonomic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
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
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmMessageProcessorForAutonomicTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private ManagedApplication ma;
	private TestApplication app;
	private DmMessageProcessor processor;
	private RuleBasedEventHandler ruleBasedHandler;

	private Manager manager;
	private TestManagerWrapper managerWrapper;


	@Before
	public void resetManager() throws Exception {

		// Create the manager
		File dir = this.folder.newFolder();

		this.manager = new Manager();
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setMessagingType(MessagingConstants.TEST_FACTORY_TYPE);
		this.manager.configurationMngr().setWorkingDirectory( dir );
		this.manager.start();

		// Create the wrapper and complete configuration
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class ).cancel();

		// Reset the processor
		if( this.processor != null )
			this.processor.stopProcessor();

		this.processor = (DmMessageProcessor) this.managerWrapper.getMessagingClient().getMessageProcessor();
		this.ruleBasedHandler = TestUtils.getInternalField( this.processor, "ruleBasedHandler", RuleBasedEventHandler.class );
		Assert.assertNotNull( this.ruleBasedHandler );

		// Create an application
		this.app = new TestApplication();
		File appDirectory = ConfigurationUtils.findApplicationDirectory( this.app.getName(), dir );
		this.app.setDirectory( appDirectory );

		this.ma = new ManagedApplication( this.app );
		this.managerWrapper.getNameToManagedApplication().clear();
		this.managerWrapper.getNameToManagedApplication().put( this.app.getName(), this.ma );

		// Create a target and associate it with the application
		String targetId = this.manager.targetsMngr().createTarget( "" );
		this.manager.targetsMngr().associateTargetWithScopedInstance( targetId, this.app, null );

		// Copy resources
		dir = new File( this.ma.getDirectory(), Constants.PROJECT_DIR_AUTONOMIC );
		Assert.assertTrue( dir.mkdirs());

		File targetFile = new File( dir, Constants.FILE_RULES );
		File sourceFile = TestUtils.findTestFile( "/autonomic/rules.cfg" );
		Utils.copyStream( sourceFile, targetFile );
		Assert.assertTrue( targetFile.exists());
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testAutonomic_createDeleteEtc_skipChecks() throws Exception {

		// Skip checks
		this.ruleBasedHandler.disableChecks = true;

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Simulate a first message to delete an instance
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Now, send an email
		this.processor.processMessage( newMessage( "up" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Add a WAR
		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 6, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 6, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 9, InstanceHelpers.getAllInstances( this.app ).size());

		// Log something
		this.processor.processMessage( newMessage( "log" ));
		Assert.assertEquals( instanceCount + 9, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "unknown event" ));
		Assert.assertEquals( instanceCount + 9, InstanceHelpers.getAllInstances( this.app ).size());

		// Reduce the number of instances
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount + 6, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Process a message that does not come from a valid instance
		Message msg = new MsgNotifAutonomic( this.app.getName(), "invalid", "up", "we do not care" );
		this.processor.processMessage( msg );
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// We must have the initial instances
		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertTrue( allInstances.contains( this.app.getMySqlVm()));
		Assert.assertTrue( allInstances.contains( this.app.getMySql()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcatVm()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcat()));
		Assert.assertTrue( allInstances.contains( this.app.getWar()));
	}


	@Test
	public void testAutonomic_replicateDelete_skipChecks() throws Exception {

		// Skip checks
		this.ruleBasedHandler.disableChecks = true;

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Simulate a first message to delete an instance
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Replicate the Tomcat VM
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 6, InstanceHelpers.getAllInstances( this.app ).size());

		// Reduce the number of instances
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// We must have the initial instances
		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertTrue( allInstances.contains( this.app.getMySqlVm()));
		Assert.assertTrue( allInstances.contains( this.app.getMySql()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcatVm()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcat()));
		Assert.assertTrue( allInstances.contains( this.app.getWar()));
	}


	@Test
	public void testAutonomic_replicate_failedPermission() throws Exception {

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Simulate a first message to delete an instance
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Replicate the Tomcat VM
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Since there is no real agent in this state, none of the new instances
		// will be deployed and started. Verify it.
		List<Instance> rootInstances = new ArrayList<>( this.app.getRootInstances());
		rootInstances.remove( this.app.getMySqlVm());
		rootInstances.remove( this.app.getTomcatVm());

		Assert.assertEquals( 1, rootInstances.size());
		Assert.assertEquals( InstanceStatus.DEPLOYING, rootInstances.get( 0 ).getStatus());

		// Therefore, when checks are enabled, the reaction should be skipped.
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Reduce the number of instances
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// We should be able to replicate the instance again
		// (important, make sure we clean up our cache correctly).
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Reduce the number of instances (again)
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// We must have the initial instances
		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertTrue( allInstances.contains( this.app.getMySqlVm()));
		Assert.assertTrue( allInstances.contains( this.app.getMySql()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcatVm()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcat()));
		Assert.assertTrue( allInstances.contains( this.app.getWar()));
	}


	@Test
	public void testAutonomic_create_failedPermission() throws Exception {

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Simulate a first message to delete an instance
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Replicate the Tomcat VM
		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Since there is no real agent in this state, none of the new instances
		// will be deployed and started. Verify it.
		List<Instance> rootInstances = new ArrayList<>( this.app.getRootInstances());
		rootInstances.remove( this.app.getMySqlVm());
		rootInstances.remove( this.app.getTomcatVm());

		Assert.assertEquals( 1, rootInstances.size());
		Assert.assertEquals( InstanceStatus.DEPLOYING, rootInstances.get( 0 ).getStatus());

		// Therefore, when checks are enabled, the reaction should be skipped.
		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Reduce the number of instances
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// We should be able to replicate the instance again
		// (important, make sure we clean up our cache correctly).
		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Reduce the number of instances (again)
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// We must have the initial instances
		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertTrue( allInstances.contains( this.app.getMySqlVm()));
		Assert.assertTrue( allInstances.contains( this.app.getMySql()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcatVm()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcat()));
		Assert.assertTrue( allInstances.contains( this.app.getWar()));
	}


	@Test
	public void testAutonomic_delete_failedPermission() throws Exception {

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Simulate a first message to delete an instance
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Replicate the Tomcat VM and simulate everything went fine.
		// We proceed this way because the autonomic can only delete machines it has created.
		this.processor.processMessage( newMessage( "loaded" ));
		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertEquals( instanceCount + 3, allInstances.size());

		for( Instance inst : allInstances )
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.processor.processMessage( newMessage( "loaded" ));

		allInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertEquals( instanceCount + 6, allInstances.size());

		for( Instance inst : allInstances )
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Reduce the number of instances
		this.processor.processMessage( newMessage( "peaceful" ));
		List<Instance> newAllInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertEquals( instanceCount + 3, newAllInstances.size());

		// The last 3 remaining instances have been deleted from the model
		allInstances.removeAll( newAllInstances );
		Assert.assertEquals( 3, allInstances.size());

		// Reducing the number of "autonomous" instances will work this time
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Try to reduce the number of instances (again)
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// We must have the initial instances
		allInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertTrue( allInstances.contains( this.app.getMySqlVm()));
		Assert.assertTrue( allInstances.contains( this.app.getMySql()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcatVm()));
		Assert.assertTrue( allInstances.contains( this.app.getTomcat()));
		Assert.assertTrue( allInstances.contains( this.app.getWar()));
	}


	@Test
	public void testAutonomic_replicate_permissionWithLastInstanceDeleted() throws Exception {

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Simulate a first message to delete an instance
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// Replicate the Tomcat VM
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Since there is no real agent in this state, none of the new instances
		// will be deployed and started. Verify it.
		List<Instance> rootInstances = new ArrayList<>( this.app.getRootInstances());
		rootInstances.remove( this.app.getMySqlVm());
		rootInstances.remove( this.app.getTomcatVm());

		Assert.assertEquals( 1, rootInstances.size());
		Assert.assertEquals( InstanceStatus.DEPLOYING, rootInstances.get( 0 ).getStatus());

		// Do not modify the instance states, but delete them from the model.
		// The same reaction should work then.
		this.app.getRootInstances().remove( rootInstances.get( 0 ));
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testAutonomic_create_shouldNotCollideWithotherApplications() throws Exception {

		// Create a second application
		Application app2 = new TestApplication();
		app2.setName( "NewAppForTest" );

		File appDirectory = new File( this.app.getDirectory().getParentFile(), app2.getName());
		app2.setDirectory( appDirectory );
		Assert.assertTrue( app2.getDirectory().mkdirs());

		this.managerWrapper.getNameToManagedApplication().put( app2.getName(), new ManagedApplication( app2 ));
		Assert.assertEquals( 2, this.managerWrapper.getNameToManagedApplication().size());

		// Copy resources for the 2nd application
		ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( app2.getName());
		Assert.assertNotNull( ma );

		File dir = new File( ma.getDirectory(), Constants.PROJECT_DIR_AUTONOMIC );
		Assert.assertTrue( dir.mkdirs());

		File targetFile = new File( dir, Constants.FILE_RULES );
		File sourceFile = TestUtils.findTestFile( "/autonomic/rules.cfg" );
		Utils.copyStream( sourceFile, targetFile );
		Assert.assertTrue( targetFile.exists());

		// Get some information about application 1
		int instanceCount1 = InstanceHelpers.getAllInstances( this.app ).size();

		// Replicate the Tomcat VM
		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( instanceCount1 + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// We should be able to perform the same thing in the other application.
		// Both applications have the same graph.
		int instanceCount2 = InstanceHelpers.getAllInstances( app2 ).size();
		this.processor.processMessage( new MsgNotifAutonomic( app2.getName(), this.app.getTomcatVm().getName(), "loaded", "we do not care" ));
		Assert.assertEquals( instanceCount2 + 3, InstanceHelpers.getAllInstances( app2 ).size());
	}


	@Test
	public void testAutonomic_replicateWithUpperLimit() {

		final int maxCount = 4;
		this.manager.setAutonomicMaxRoots( maxCount );

		// Create...
		for( int i=0; i<maxCount; i++ ) {

			Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
			for( Instance inst : allInstances )
				inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

			this.processor.processMessage( newMessage( "replicated" ));
			Assert.assertEquals( allInstances.size() + 3, InstanceHelpers.getAllInstances( this.app ).size());
		}

		// Next creation should fail
		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		for( Instance inst : allInstances )
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( allInstances.size(), InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testAutonomic_createWithUpperLimit() {

		final int maxCount = 3;
		this.manager.setAutonomicMaxRoots( maxCount );

		// Create...
		for( int i=0; i<maxCount; i++ ) {

			Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
			for( Instance inst : allInstances )
				inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

			this.processor.processMessage( newMessage( "loaded" ));
			Assert.assertEquals( allInstances.size() + 3, InstanceHelpers.getAllInstances( this.app ).size());
		}

		// Next creation should fail
		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		for( Instance inst : allInstances )
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( allInstances.size(), InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testAutonomic_replicateWithDelay() throws Exception {

		// Override the rules
		File f = new File( this.app.getDirectory(), Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES );
		Assert.assertTrue( f.exists());

		File sourceFile = TestUtils.findTestFile( "/autonomic/rules-with-delays.cfg" );
		Utils.copyStream( sourceFile, f );

		// Execute a same reaction twice.
		// The first one will work. Simulate everything is working and an ACK.
		// The second one will then not work, rejected because of the delay.

		// This test assumes reactions will be triggered within a second, which is realistic
		// on modern machines. It may fail on slower machines.

		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		for( Instance inst : allInstances )
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.processor.processMessage( newMessage( "replicated" ));

		Collection<Instance> newAllInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertEquals( allInstances.size() + 3, newAllInstances.size());

		for( Instance inst : newAllInstances )
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		// For instance creation, delay verifications rely on stored time when ACK are received
		for( Instance inst : newAllInstances ) {
			if( InstanceHelpers.isTarget( inst ))
				this.ma.acknowledgeHeartBeat( inst );
		}

		// Next creation should fail
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( allInstances.size() + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Wait a second
		Thread.sleep( 1000 );

		// It should work now
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( allInstances.size() + 6, InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testAutonomic_createAndDeleteWithDelay() throws Exception {

		// Override the rules
		File f = new File( this.app.getDirectory(), Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES );
		Assert.assertTrue( f.exists());

		File sourceFile = TestUtils.findTestFile( "/autonomic/rules-with-delays.cfg" );
		Utils.copyStream( sourceFile, f );

		// Execute a same reaction twice.
		// The first one will work. Simulate everything is working and an ACK.
		// The second one will then not work, rejected because of the delay.

		// This test assumes reactions will be triggered within a second, which is realistic
		// on modern machines. It may fail on slower machines.

		Collection<Instance> allInstances = InstanceHelpers.getAllInstances( this.app );
		for( Instance inst : allInstances )
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.processor.processMessage( newMessage( "loaded" ));

		Collection<Instance> newAllInstances = InstanceHelpers.getAllInstances( this.app );
		Assert.assertEquals( allInstances.size() + 3, newAllInstances.size());

		for( Instance inst : newAllInstances )
			inst.setStatus( InstanceStatus.DEPLOYED_STARTED );

		// For instance creation, delay verifications rely on stored time when ACK are received
		for( Instance inst : newAllInstances ) {
			if( InstanceHelpers.isTarget( inst ))
				this.ma.acknowledgeHeartBeat( inst );
		}

		// Next creation should fail
		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( allInstances.size() + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Wait a second
		Thread.sleep( 1000 );

		// It should work now
		this.processor.processMessage( newMessage( "loaded" ));
		Assert.assertEquals( allInstances.size() + 6, InstanceHelpers.getAllInstances( this.app ).size());

		// Test removing
		// First time works
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( allInstances.size() + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Second won't
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( allInstances.size() + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Wait a second
		Thread.sleep( 1000 );

		// After the delay, it works
		this.processor.processMessage( newMessage( "peaceful" ));
		Assert.assertEquals( allInstances.size(), InstanceHelpers.getAllInstances( this.app ).size());
	}


	@Test
	public void testAutonomic_replicateWithCustomChildName() throws Exception {

		// Override the rules
		File f = new File( this.app.getDirectory(), Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES );
		Assert.assertTrue( f.exists());

		File sourceFile = TestUtils.findTestFile( "/autonomic/rules-with-custom-child-name.cfg" );
		Utils.copyStream( sourceFile, f );

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Replicate the Tomcat VM
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Make sure the new Tomcat does not have the same name than other Tomcats
		List<Instance> tomcatInstances = InstanceHelpers.findInstancesByComponentName( this.app, this.app.getTomcat().getComponent().getName());
		Assert.assertEquals( 2, tomcatInstances.size());

		String tomcat1 = tomcatInstances.get( 0 ).getName();
		String tomcat2 = tomcatInstances.get( 1 ).getName();
		Assert.assertFalse( tomcat1.equals( tomcat2 ));
		Assert.assertTrue( tomcat2.startsWith( tomcat1 ));
	}


	@Test
	public void testAutonomic_deleteAutonomicInstanceByHand() throws Exception {

		// Get some information about the application
		int instanceCount = InstanceHelpers.getAllInstances( this.app ).size();

		// Replicate the Tomcat VM
		this.processor.processMessage( newMessage( "replicated" ));
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.app ).size());

		// Since there is no real agent in this state, none of the new instances
		// will be deployed and started. Verify it.
		List<Instance> rootInstances = new ArrayList<>( this.app.getRootInstances());
		rootInstances.remove( this.app.getMySqlVm());
		rootInstances.remove( this.app.getTomcatVm());

		Assert.assertEquals( 1, rootInstances.size());
		Instance tomcatInstance = rootInstances.get( 0 );

		Assert.assertEquals( InstanceStatus.DEPLOYING, tomcatInstance.getStatus());
		Assert.assertEquals( 1, this.manager.getRuleBasedHandler().getAutonomicInstancesCount());

		// Kill the new Tomcat by hand
		this.manager.instancesMngr().undeployAll( this.ma, tomcatInstance );
		this.manager.instancesMngr().removeInstance( this.ma, tomcatInstance );

		// It should have been removed from the model
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.app ).size());

		// And the autonomic instances count should have been decreased
		Assert.assertEquals( 0, this.manager.getRuleBasedHandler().getAutonomicInstancesCount());
	}


	private Message newMessage( String eventId ) {
		return new MsgNotifAutonomic( this.app.getName(), this.app.getTomcatVm().getName(), eventId, "we do not care" );
	}
}
