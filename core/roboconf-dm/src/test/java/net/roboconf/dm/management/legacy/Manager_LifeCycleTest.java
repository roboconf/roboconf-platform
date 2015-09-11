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

package net.roboconf.dm.management.legacy;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.internal.client.test.TestClientDm;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Manager_LifeCycleTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private TestClientDm msgClient;
	private TestTargetResolver targetResolver;


	@Before
	public void resetManager() throws Exception {

		this.targetResolver = new TestTargetResolver();

		this.manager = new Manager();
		this.manager.setTargetResolver( this.targetResolver );
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.setMessagingType(MessagingConstants.TEST_FACTORY_TYPE);
		this.manager.start();

		// Register mocked listeners - mainly for code coverage reasons
		this.manager.listenerAppears( Mockito.mock( IDmListener.class ));

		// Create the wrapper
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		this.msgClient = (TestClientDm) this.managerWrapper.getInternalMessagingClient();
		this.msgClient.sentMessages.clear();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class ).cancel();
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testChangeInstanceState_root() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		// Switch a root instance only works if the state is DEPLOYED_STARTED...
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertTrue( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, msg.getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetScopedInstance) msg).getScopedInstance());

		// ... or NOT_DEPLOYED (the current state is DEPLOYING)
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.STARTING );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		// ... Same thing if the current state is DEPLOYED_STARTED
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.STARTING );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
	}


	@Test
	public void testChangeInstanceState_childWithDeployedRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());

		// The DM only propagates requests for child instances
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());

		Message msg = this.msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Try other states
		this.msgClient.sentMessages.clear();
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());

		msg = this.msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Not_DEPLOYED
		this.msgClient.sentMessages.clear();
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySql(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());

		msg = this.msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());
	}


	@Test
	public void testChangeInstanceState_childWithDeployingRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());

		// The DM only propagates requests for child instances.
		// But since the root is deploying, messages are stored.
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Try other states
		ma.getScopedInstanceToAwaitingMessages().clear();
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		msg = ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Not_DEPLOYED
		ma.getScopedInstanceToAwaitingMessages().clear();
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySql(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		msg = ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());
	}


	@Test( expected = IOException.class )
	public void testChangeInstanceState_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		ManagedApplication ma = new ManagedApplication( app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
	}


	@Test
	public void testDeployRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertTrue( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		List<Message> messages = ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, msg.getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetScopedInstance) msg).getScopedInstance());
	}


	@Test
	public void testDeployRootMoreThanOnce() throws Exception {

		// This is a unit test for #80.
		// More than concurrent accesses, it tests the fact the DM can filter
		// redundant requests about root instances deployment.
		TestApplication app = new TestApplication();
		final ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		final Instance instance = app.getMySqlVm();

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertNull( this.targetResolver.count( instance ));
		Assert.assertNull( instance.data.get( Instance.TARGET_ACQUIRED ));

		Thread[] threads = new Thread[ 3 ];
		for( int i=0; i<threads.length; i++ ) {
			threads[ i ] = new Thread() {
				@Override
				public void run() {
					try {
						Manager_LifeCycleTest.this.manager.instancesMngr().changeInstanceState( ma, instance, InstanceStatus.DEPLOYED_STARTED );

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
		Assert.assertTrue( this.targetResolver.isRunning( app.getMySqlVm()));

		// 2 requests to deploy a root instance, but one is filtered by the DM.
		Assert.assertEquals((Integer) 1, this.targetResolver.count( instance ));
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		List<Message> messages = ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdSetScopedInstance.class, msg.getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetScopedInstance) msg).getScopedInstance());
	}


	@Test
	public void testDeployRoot_alreadyDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		// A root instance is considered to be deployed if it has a machine ID.
		app.getMySqlVm().data.put( Instance.MACHINE_ID, "something" );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
	}


	@Test( expected = TargetException.class )
	public void testDeployRoot_targetException() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		TestTargetResolver newResolver = new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties )
			throws TargetException {
				throw new TargetException( "For test purpose!" );
			}

			@Override
			public TargetHandler findTargetHandlerById( String targetId ) {
				return null;
			}
		};

		this.manager.setTargetResolver( newResolver );

		// Nothing happens with children instances
		try {
			this.manager.instancesMngr().changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STARTED );

		} catch( Exception e ) {
			Assert.fail( "Nothing should have happened here." );
		}

		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
	}


	@Test( expected = IOException.class )
	public void testDeployRoot_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
	}


	@Test
	public void testUndeployRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().data.put( Instance.MACHINE_ID, InstanceHelpers.computeInstancePath( app.getMySqlVm()));
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
	}


	@Test
	public void testUndeployRoot_noMachineId() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
	}


	@Test
	public void testUndeployRoot_notDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		// The state means nothing in fact, the machine ID does
		ma.getScopedInstanceToAwaitingMessages().clear();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().data.put( Instance.MACHINE_ID, InstanceHelpers.computeInstancePath( app.getMySqlVm()));
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().data.get( Instance.MACHINE_ID ));
		Assert.assertEquals( 1, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
	}


	@Test( expected = TargetException.class )
	public void testUndeployRoot_targetException() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		TestTargetResolver newResolver = new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties )
			throws TargetException {
				throw new TargetException( "For test purpose!" );
			}

			@Override
			public TargetHandler findTargetHandlerById( String targetId ) {
				return null;
			}
		};

		app.getMySqlVm().data.put( Instance.MACHINE_ID, "we need one" );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.manager.setTargetResolver( newResolver );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
	}


	@Test( expected = IOException.class )
	public void testUndeployRoot_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		ManagedApplication ma = new ManagedApplication( app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
	}


	@Test
	public void testDeployAndStartAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().deployAndStartAll( ma, null );

		Assert.assertTrue( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertTrue( this.targetResolver.isRunning( app.getTomcatVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 2, ma.getScopedInstanceToAwaitingMessages().size());

		// MySQL
		List<Message> mySqlMessages = ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm());
		Assert.assertEquals( 2, mySqlMessages.size());

		Assert.assertEquals( MsgCmdSetScopedInstance.class, mySqlMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetScopedInstance) mySqlMessages.get( 0 )).getScopedInstance());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, mySqlMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getNewState());

		// Tomcat
		List<Message> tomcatMessages = ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm());
		Assert.assertEquals( 3, tomcatMessages.size());

		Assert.assertEquals( MsgCmdSetScopedInstance.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getTomcatVm(), ((MsgCmdSetScopedInstance) tomcatMessages.get( 0 )).getScopedInstance());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getNewState());
	}


	@Test
	public void testDeployAndStartAll_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().deployAndStartAll( ma, app.getTomcatVm());

		Assert.assertNull( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertTrue( this.targetResolver.isRunning( app.getTomcatVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		// Tomcat
		List<Message> tomcatMessages = ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm());
		Assert.assertEquals( 3, tomcatMessages.size());

		Assert.assertEquals( MsgCmdSetScopedInstance.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getTomcatVm(), ((MsgCmdSetScopedInstance) tomcatMessages.get( 0 )).getScopedInstance());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getNewState());
	}


	@Test
	public void testDeployAndStartAll_intermediateInstance_vmDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().deployAndStartAll( ma, app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 2, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, this.msgClient.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) this.msgClient.sentMessages.get( 0 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) this.msgClient.sentMessages.get( 0 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, this.msgClient.sentMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) this.msgClient.sentMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) this.msgClient.sentMessages.get( 1 )).getNewState());
	}


	@Test
	public void testDeployAndStartAll_intermediateInstance_vmDeploying() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYING );
		this.manager.instancesMngr().deployAndStartAll( ma, app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());

		// Tomcat
		List<Message> tomcatMessages = ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm());
		Assert.assertEquals( 2, tomcatMessages.size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 0 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 0 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getNewState());
	}


	@Test( expected = IOException.class )
	public void testDeployAndStartAll_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.instancesMngr().deployAndStartAll( ma, app.getMySqlVm());
	}


	@Test
	public void testStopAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().stopAll( ma, null );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 2, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()),((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());

		msg = ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testStopAll_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().stopAll( ma, app.getTomcatVm());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testStopAll_intermediateInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().stopAll( ma, app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test( expected = IOException.class )
	public void testStopAll_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.instancesMngr().stopAll( ma, app.getMySqlVm());
	}


	@Test
	public void testUndeployAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getTomcatVm().data.put( Instance.MACHINE_ID, InstanceHelpers.computeInstancePath( app.getTomcatVm()));
		this.manager.instancesMngr().undeployAll( ma, null );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcatVm().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		Assert.assertNull( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertFalse( this.targetResolver.isRunning( app.getTomcatVm()));

		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
	}


	@Test
	public void testUndeployAll_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		app.getTomcatVm().data.put( Instance.MACHINE_ID, InstanceHelpers.computeInstancePath( app.getTomcatVm()));
		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYING );
		this.manager.instancesMngr().undeployAll( ma, app.getTomcatVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getTomcatVm().getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		Assert.assertNull( this.targetResolver.isRunning( app.getMySqlVm()));
		Assert.assertFalse( this.targetResolver.isRunning( app.getTomcatVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());
	}


	@Test
	public void testUndeployAll_intermediateInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, ma.getScopedInstanceToAwaitingMessages().size());

		this.manager.instancesMngr().undeployAll( ma, app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instancePathToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm()).size());

		Message msg = ma.getScopedInstanceToAwaitingMessages().get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test( expected = IOException.class )
	public void testUndeployAll_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );
		this.manager.instancesMngr().undeployAll( ma, app.getMySqlVm());
	}
}
