/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

import java.io.IOException;
import java.util.List;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.internal.client.test.TestClientDm;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Manager_LifeCycleTest {

	private Manager manager;
	private TestClientDm msgClient;
	private TestTargetResolver targetResolver;


	@Before
	public void resetManager() throws Exception {

		this.manager = new Manager();
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setMessagingFactoryType( MessagingConstants.FACTORY_TEST );
		this.manager.start();

		this.msgClient = (TestClientDm) this.manager.getMessagingClient().getInternalClient();
		this.msgClient.sentMessages.clear();
		this.targetResolver = (TestTargetResolver) this.manager.targetResolver;

		// Disable the messages timer for predictability
		this.manager.timer.cancel();
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testChangeInstanceState_root() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		// Switch a root instance only works if the state is DEPLOYED_STARTED...
		this.manager.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		this.manager.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertTrue( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		Message msg = ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdSetRootInstance.class, msg.getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetRootInstance) msg).getRootInstance());

		// ... or NOT_DEPLOYED (the current state is DEPLOYING)
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
		this.manager.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.STARTING );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());

		this.manager.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		// ... Same thing if the current state is DEPLOYED_STARTED
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.STARTING );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());

		this.manager.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
	}


	@Test
	public void testChangeInstanceState_childWithDeployedRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());

		// The DM only propagates requests for child instances
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		this.manager.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());

		Message msg = this.msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Try other states
		this.msgClient.sentMessages.clear();
		this.manager.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.msgClient.sentMessages.size());

		msg = this.msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Not_DEPLOYED
		this.msgClient.sentMessages.clear();
		this.manager.changeInstanceState( ma, app.getMySql(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
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
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());

		// The DM only propagates requests for child instances.
		// But since the root is deploying, messages are stored.
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		this.manager.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		Message msg = ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Try other states
		ma.getRootInstanceToAwaitingMessages().clear();
		this.manager.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		msg = ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Not_DEPLOYED
		ma.getRootInstanceToAwaitingMessages().clear();
		this.manager.changeInstanceState( ma, app.getMySql(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());

		msg = ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());
	}


	@Test( expected = IOException.class )
	public void testChangeInstanceState_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		ManagedApplication ma = new ManagedApplication( app, null );

		this.manager = new Manager();
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
	}


	@Test
	public void testDeployRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());

		// Nothing happens with children instances
		this.manager.deployRoot( ma, app.getMySql());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		// Let's try with a root instance
		this.manager.deployRoot( ma, app.getMySqlVm());
		Assert.assertEquals( 1, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertTrue( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());

		List<Message> messages = ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdSetRootInstance.class, msg.getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetRootInstance) msg).getRootInstance());
	}


	@Test
	public void testDeployRoot_alreadyDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		// A root instance is considered to be deployed if it has a machine ID.
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "something" );
		this.manager.deployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
	}


	@Test( expected = TargetException.class )
	public void testDeployRoot_targetException() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		TestTargetResolver newResolver = new TestTargetResolver() {
			@Override
			public Target findTargetHandler( List<TargetHandler> target, ManagedApplication ma, Instance instance )
			throws TargetException {
				throw new TargetException( "For test purpose!" );
			}
		};

		this.manager.setTargetResolver( newResolver );

		// Nothing happens with children instances
		try {
			this.manager.deployRoot( ma, app.getMySql());

		} catch( Exception e ) {
			Assert.fail( "Nothing should have happened here." );
		}

		this.manager.deployRoot( ma, app.getMySqlVm());
	}


	@Test( expected = IOException.class )
	public void testDeployRoot_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );

		this.manager = new Manager();
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.deployRoot( ma, app.getMySqlVm());
	}


	@Test
	public void testUndeployRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());

		// Nothing happens with children instances
		this.manager.undeployRoot( ma, app.getMySql());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		// Let's try with a root instance
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "something" );
		this.manager.undeployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( 1, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
	}


	@Test
	public void testUndeployRoot_noMachineId() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());

		// Nothing happens with children instances
		this.manager.undeployRoot( ma, app.getMySql());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		// Let's try with a root instance
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.undeployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
	}


	@Test
	public void testUndeployRoot_notDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());

		// Nothing happens with children instances
		this.manager.undeployRoot( ma, app.getMySql());
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		// Let's try with a root instance
		app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		this.manager.undeployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		// The state means nothing in fact, the machine ID does
		ma.getRootInstanceToAwaitingMessages().clear();
		app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "something" );
		this.manager.undeployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( 1, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertFalse( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
	}


	@Test( expected = TargetException.class )
	public void testUndeployRoot_targetException() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		TestTargetResolver newResolver = new TestTargetResolver() {
			@Override
			public Target findTargetHandler( List<TargetHandler> target, ManagedApplication ma, Instance instance )
			throws TargetException {
				throw new TargetException( "For test purpose!" );
			}
		};

		this.manager.setTargetResolver( newResolver );

		// Nothing happens with children instances
		try {
			this.manager.undeployRoot( ma, app.getMySql());

		} catch( Exception e ) {
			Assert.fail( "Nothing should have happened here." );
		}

		this.manager.undeployRoot( ma, app.getMySqlVm());
	}


	@Test( expected = IOException.class )
	public void testUndeployRoot_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		ManagedApplication ma = new ManagedApplication( app, null );

		this.manager = new Manager();
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.undeployRoot( ma, app.getMySqlVm());
	}


	@Test
	public void testDeployAndStartAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.deployAndStartAll( ma, null );

		Assert.assertTrue( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertTrue( this.targetResolver.instanceToRunningStatus.get( app.getTomcatVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 2, ma.getRootInstanceToAwaitingMessages().size());

		// MySQL
		List<Message> mySqlMessages = ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm());
		Assert.assertEquals( 2, mySqlMessages.size());

		Assert.assertEquals( MsgCmdSetRootInstance.class, mySqlMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetRootInstance) mySqlMessages.get( 0 )).getRootInstance());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, mySqlMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getNewState());

		// Tomcat
		List<Message> tomcatMessages = ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm());
		Assert.assertEquals( 3, tomcatMessages.size());

		Assert.assertEquals( MsgCmdSetRootInstance.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getTomcatVm(), ((MsgCmdSetRootInstance) tomcatMessages.get( 0 )).getRootInstance());

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
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.deployAndStartAll( ma, app.getTomcatVm());

		Assert.assertNull( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertTrue( this.targetResolver.instanceToRunningStatus.get( app.getTomcatVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());

		// Tomcat
		List<Message> tomcatMessages = ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm());
		Assert.assertEquals( 3, tomcatMessages.size());

		Assert.assertEquals( MsgCmdSetRootInstance.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getTomcatVm(), ((MsgCmdSetRootInstance) tomcatMessages.get( 0 )).getRootInstance());

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
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.manager.deployAndStartAll( ma, app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 2, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

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
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYING );
		this.manager.deployAndStartAll( ma, app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());

		// Tomcat
		List<Message> tomcatMessages = ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm());
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
		ManagedApplication ma = new ManagedApplication( app, null );

		this.manager = new Manager();
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.deployAndStartAll( ma, app.getMySqlVm());
	}


	@Test
	public void testStopAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.stopAll( ma, null );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 2, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm()).size());

		Message msg = ma.getRootInstanceToAwaitingMessages().get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()),((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());

		msg = ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testStopAll_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.stopAll( ma, app.getTomcatVm());

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm()).size());

		Message msg = ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testStopAll_intermediateInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.stopAll( ma, app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm()).size());

		Message msg = ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test( expected = IOException.class )
	public void testStopAll_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );

		this.manager = new Manager();
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.stopAll( ma, app.getMySqlVm());
	}


	@Test
	public void testUndeployAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		app.getTomcatVm().getData().put( Instance.MACHINE_ID, "some id..." );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "another id..." );
		this.manager.undeployAll( ma, null );

		Assert.assertFalse( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertFalse( this.targetResolver.instanceToRunningStatus.get( app.getTomcatVm()));

		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
	}


	@Test
	public void testUndeployAll_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		app.getTomcatVm().getData().put( Instance.MACHINE_ID, "some id..." );
		this.manager.undeployAll( ma, app.getTomcatVm());

		Assert.assertNull( this.targetResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertFalse( this.targetResolver.instanceToRunningStatus.get( app.getTomcatVm()));
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());
	}


	@Test
	public void testUndeployAll_intermediateInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.getRootInstanceToAwaitingMessages().size());

		this.manager.undeployAll( ma, app.getTomcat());

		Assert.assertEquals( 0, this.targetResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, this.msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm()).size());

		Message msg = ma.getRootInstanceToAwaitingMessages().get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test( expected = IOException.class )
	public void testUndeployAll_invalidConfiguration() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );

		this.manager = new Manager();
		this.manager.getAppNameToManagedApplication().put( app.getName(), ma );
		this.manager.undeployAll( ma, app.getMySqlVm());
	}
}
