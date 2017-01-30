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

import java.io.File;
import java.io.IOException;
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

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdUpdateProbeConfiguration;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagerLifeCycleTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private TestClient msgClient;
	private TestTargetResolver targetResolver;
	private TestApplication app;


	@Before
	public void resetManager() throws Exception {

		this.targetResolver = new TestTargetResolver();

		this.manager = new Manager();
		this.manager.setTargetResolver( this.targetResolver );
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
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

		// Create an application all the tests can use
		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testChangeInstanceState_root() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());

		// Switch a root instance only works if the state is DEPLOYED_STARTED...
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());

		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, this.app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertTrue( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, msg.getClass());
		Assert.assertEquals( this.app.getMySqlVm(), ((MsgCmdSetScopedInstance) msg).getScopedInstance());

		// ... or NOT_DEPLOYED (the current state is DEPLOYING)
		Assert.assertEquals( InstanceStatus.DEPLOYING, this.app.getMySqlVm().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.STARTING );
		Assert.assertEquals( InstanceStatus.DEPLOYING, this.app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());

		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.isRunning( this.app.getMySqlVm()));

		verifyAgentTerminationPropagation();
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		// ... Same thing if the current state is DEPLOYED_STARTED
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.STARTING );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		// Make sure data are cleared correctly
		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.IP_ADDRESS ));
		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.RUNNING_FROM ));
		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.TARGET_ACQUIRED ));
	}


	@Test
	public void testChangeInstanceState_rootWithProbeConfiguration() throws Exception {

		// Prepare the application
		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());

		// Add a probe file
		File probeDir = new File( ma.getDirectory(), Constants.PROJECT_DIR_PROBES );
		Assert.assertTrue( probeDir.mkdir());

		String filename = this.app.getMySqlVm().getComponent().getName() + Constants.FILE_EXT_MEASURES;
		File probeFile = new File( probeDir, filename );
		Utils.writeStringInto( "whatever", probeFile );

		// Deploy
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, this.app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertTrue( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 2, ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, msg.getClass());
		Assert.assertEquals( this.app.getMySqlVm(), ((MsgCmdSetScopedInstance) msg).getScopedInstance());

		msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).get( 1 );
		Assert.assertEquals( MsgCmdUpdateProbeConfiguration.class, msg.getClass());
		Assert.assertEquals( "/" + this.app.getMySqlVm(), ((MsgCmdUpdateProbeConfiguration) msg).getInstancePath());
		Assert.assertEquals( 1, ((MsgCmdUpdateProbeConfiguration) msg).getProbeResources().size());
		Assert.assertNotNull(((MsgCmdUpdateProbeConfiguration) msg).getProbeResources().get( filename ));
	}


	@Test
	public void testChangeInstanceState_childWithDeployedRoot() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());

		// The DM only propagates requests for child instances
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());

		Message msg = this.msgClient.allSentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Try other states
		this.msgClient.allSentMessages.clear();
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySql(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());

		msg = this.msgClient.allSentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Not_DEPLOYED
		this.msgClient.allSentMessages.clear();
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySql(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());

		msg = this.msgClient.allSentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());
	}


	@Test
	public void testChangeInstanceState_childWithDeployingRoot() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());

		// The DM only propagates requests for child instances.
		// But since the root is deploying, messages are stored.
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Try other states
		ma.getScopedInstanceToAwaitingMessages().clear();
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySql(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());

		msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Not_DEPLOYED
		ma.getScopedInstanceToAwaitingMessages().clear();
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySql(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());

		msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());
	}


	@Test( expected = IOException.class )
	public void testChangeInstanceState_invalidConfiguration() throws Exception {

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		ManagedApplication ma = new ManagedApplication( this.app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( ma );
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
	}


	@Test
	public void testDeployRoot() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertTrue( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		List<Message> messages = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, msg.getClass());
		Assert.assertEquals( this.app.getMySqlVm(), ((MsgCmdSetScopedInstance) msg).getScopedInstance());
	}


	@Test
	public void testDeployRootMoreThanOnce() throws Exception {

		// This is a unit test for #80.
		// More than concurrent accesses, it tests the fact the DM can filter
		// redundant requests about root instances deployment.
		final ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		final Instance instance = this.app.getMySqlVm();

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertNull( this.targetResolver.count( instance ));
		Assert.assertNull( instance.data.get( Instance.TARGET_ACQUIRED ));

		Thread[] threads = new Thread[ 3 ];
		for( int i=0; i<threads.length; i++ ) {
			threads[ i ] = new Thread() {
				@Override
				public void run() {
					try {
						ManagerLifeCycleTest.this.manager.instancesMngr().changeInstanceState( ma, instance, InstanceStatus.DEPLOYED_STARTED );

					} catch( Exception e ) {
						e.printStackTrace();
					}
				}
			};
		}

		for( Thread thread : threads )
			thread.start();

		for( Thread thread : threads )
			thread.join();

		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertNotNull( instance.data.get( Instance.TARGET_ACQUIRED ));
		Assert.assertTrue( this.targetResolver.isRunning( this.app.getMySqlVm()));

		// 2 requests to deploy a root instance, but one is filtered by the DM.
		Assert.assertEquals((Integer) 1, this.targetResolver.count( instance ));
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		List<Message> messages = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, msg.getClass());
		Assert.assertEquals( this.app.getMySqlVm(), ((MsgCmdSetScopedInstance) msg).getScopedInstance());
	}


	@Test
	public void testDeployRoot_alreadyDeployed() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		// A root instance is considered to be deployed if it has a machine ID.
		this.app.getMySqlVm().data.put( Instance.MACHINE_ID, "something" );
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
	}


	@Test( expected = TargetException.class )
	public void testDeployRoot_targetException() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		TestTargetResolver newResolver = new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties )
			throws TargetException {
				throw new TargetException( "For test purpose!" );
			}
		};

		this.manager.setTargetResolver( newResolver );

		// Nothing happens with children instances
		try {
			this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySql(), InstanceStatus.DEPLOYED_STARTED );

		} catch( Exception e ) {
			Assert.fail( "Nothing should have happened here." );
		}

		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
	}


	@Test( expected = IOException.class )
	public void testDeployRoot_invalidConfiguration() throws Exception {
		ManagedApplication ma = new ManagedApplication( this.app );

		String targetId = this.manager.targetsMngr().createTarget( "prop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( ma );
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
	}


	@Test
	public void testUndeployRoot() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.app.getMySqlVm().data.put( Instance.MACHINE_ID, InstanceHelpers.computeInstancePath( this.app.getMySqlVm()));
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		verifyAgentTerminationPropagation();
	}


	@Test
	public void testUndeployRoot_noMachineId() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}


	@Test
	public void testUndeployRoot_notDeployed() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		this.app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		// The state means nothing in fact, the machine ID does
		ma.getScopedInstanceToAwaitingMessages().clear();
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.app.getMySqlVm().data.put( Instance.MACHINE_ID, InstanceHelpers.computeInstancePath( this.app.getMySqlVm()));
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());


	}


	@Test( expected = TargetException.class )
	public void testUndeployRoot_targetException() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		TestTargetResolver newResolver = new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties )
			throws TargetException {
				throw new TargetException( "For test purpose!" );
			}
		};

		this.app.getMySqlVm().data.put( Instance.MACHINE_ID, "we need one" );
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.manager.setTargetResolver( newResolver );
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
	}


	@Test( expected = IOException.class )
	public void testUndeployRoot_invalidConfiguration() throws Exception {

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		ManagedApplication ma = new ManagedApplication( this.app );

		String targetId = this.manager.targetsMngr().createTarget( "prop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( ma );
		this.manager.instancesMngr().changeInstanceState( ma, this.app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
	}


	@Test
	public void testDeployAndStartAll_application() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().deployAndStartAll( ma, null );

		Assert.assertTrue( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertTrue( this.targetResolver.isRunning( this.app.getTomcatVm()));
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 2, ma.getScopedInstanceToAwaitingMessages().size());

		// MySQL
		List<Message> mySqlMessages = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm());
		Assert.assertEquals( 2, mySqlMessages.size());

		Assert.assertEquals( MsgCmdSetScopedInstance.class, mySqlMessages.get( 0 ).getClass());
		Assert.assertEquals( this.app.getMySqlVm(), ((MsgCmdSetScopedInstance) mySqlMessages.get( 0 )).getScopedInstance());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, mySqlMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getMySql()), ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getNewState());

		// Tomcat
		List<Message> tomcatMessages = ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm());
		Assert.assertEquals( 3, tomcatMessages.size());

		Assert.assertEquals( MsgCmdSetScopedInstance.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( this.app.getTomcatVm(), ((MsgCmdSetScopedInstance) tomcatMessages.get( 0 )).getScopedInstance());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getNewState());
	}


	@Test
	public void testDeployAndStartAll_rootInstance() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().deployAndStartAll( ma, this.app.getTomcatVm());

		Assert.assertNull( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertTrue( this.targetResolver.isRunning( this.app.getTomcatVm()));
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		// Tomcat
		List<Message> tomcatMessages = ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm());
		Assert.assertEquals( 3, tomcatMessages.size());

		Assert.assertEquals( MsgCmdSetScopedInstance.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( this.app.getTomcatVm(), ((MsgCmdSetScopedInstance) tomcatMessages.get( 0 )).getScopedInstance());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getNewState());
	}


	@Test
	public void testDeployAndStartAll_intermediateInstance_vmDeployed() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().deployAndStartAll( ma, this.app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 2, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, this.msgClient.allSentMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) this.msgClient.allSentMessages.get( 0 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) this.msgClient.allSentMessages.get( 0 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, this.msgClient.allSentMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getWar()), ((MsgCmdChangeInstanceState) this.msgClient.allSentMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) this.msgClient.allSentMessages.get( 1 )).getNewState());
	}


	@Test
	public void testDeployAndStartAll_intermediateInstance_vmDeploying() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nprop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYING );
		this.manager.instancesMngr().deployAndStartAll( ma, this.app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		// Tomcat
		List<Message> tomcatMessages = ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm());
		Assert.assertEquals( 2, tomcatMessages.size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 0 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 0 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getNewState());
	}


	@Test( expected = IOException.class )
	public void testDeployAndStartAll_invalidConfiguration() throws Exception {
		ManagedApplication ma = new ManagedApplication( this.app );

		String targetId = this.manager.targetsMngr().createTarget( "prop: ok\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( ma );
		this.manager.instancesMngr().deployAndStartAll( ma, this.app.getMySqlVm());
	}


	@Test
	public void testStopAll_application() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().stopAll( ma, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 2, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getMySql()),((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());

		msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testStopAll_rootInstance() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().stopAll( ma, this.app.getTomcatVm());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testStopAll_intermediateInstance() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().stopAll( ma, this.app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test( expected = IOException.class )
	public void testStopAll_invalidConfiguration() throws Exception {
		ManagedApplication ma = new ManagedApplication( this.app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( ma );
		this.manager.instancesMngr().stopAll( ma, this.app.getMySqlVm());
	}


	@Test
	public void testUndeployAll_application() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.app.getTomcatVm().data.put( Instance.MACHINE_ID, InstanceHelpers.computeInstancePath( this.app.getTomcatVm()));
		this.manager.instancesMngr().undeployAll( ma, null );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getTomcatVm().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());

		Assert.assertNull( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertFalse( this.targetResolver.isRunning( this.app.getTomcatVm()));

		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		verifyAgentTerminationPropagation();
	}


	@Test
	public void testUndeployAll_rootInstance() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.app.getTomcatVm().data.put( Instance.MACHINE_ID, InstanceHelpers.computeInstancePath( this.app.getTomcatVm()));
		this.app.getTomcatVm().setStatus( InstanceStatus.DEPLOYING );
		this.manager.instancesMngr().undeployAll( ma, this.app.getTomcatVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getTomcatVm().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getMySqlVm().getStatus());

		Assert.assertNull( this.targetResolver.isRunning( this.app.getMySqlVm()));
		Assert.assertFalse( this.targetResolver.isRunning( this.app.getTomcatVm()));
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		verifyAgentTerminationPropagation();
	}


	@Test
	public void testUndeployAll_intermediateInstance() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().undeployAll( ma, this.app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( this.app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test( expected = IOException.class )
	public void testUndeployAll_invalidConfiguration() throws Exception {
		ManagedApplication ma = new ManagedApplication( this.app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( ma );
		this.manager.instancesMngr().undeployAll( ma, this.app.getMySqlVm());
	}


	private void verifyAgentTerminationPropagation() {

		// The DM mimicked an agent sending a removed import.
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());
		Assert.assertEquals( MsgCmdRemoveImport.class, this.msgClient.allSentMessages.get( 0 ).getClass());
		this.msgClient.allSentMessages.clear();
	}
}
