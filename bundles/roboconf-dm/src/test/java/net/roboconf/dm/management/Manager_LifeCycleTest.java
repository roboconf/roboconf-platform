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
import java.util.List;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.internal.TestMessageServerClient;
import net.roboconf.dm.internal.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSetRootInstance;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class Manager_LifeCycleTest {

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

		// Disable the messages timer for predictability
		Manager.INSTANCE.timer.cancel();
	}


	@Test
	public void testChangeInstanceState_root() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		// Switch a root instance only works if the state is DEPLOYED_STARTED...
		Manager.INSTANCE.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		Manager.INSTANCE.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdSetRootInstance.class, msg.getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetRootInstance) msg).getRootInstance());

		// ... or NOT_DEPLOYED (the current state is DEPLOYING)
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
		Manager.INSTANCE.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.STARTING );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());

		Manager.INSTANCE.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertFalse( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		// ... Same thing if the current state is DEPLOYED_STARTED
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Manager.INSTANCE.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.STARTING );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getMySqlVm().getStatus());

		Manager.INSTANCE.changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertFalse( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());
	}


	@Test
	public void testChangeInstanceState_childWithDeployedRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// The DM only propagates requests for child instances
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Manager.INSTANCE.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, msgClient.sentMessages.size());

		Message msg = msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Try other states
		msgClient.sentMessages.clear();
		Manager.INSTANCE.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, msgClient.sentMessages.size());

		msg = msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Not_DEPLOYED
		msgClient.sentMessages.clear();
		Manager.INSTANCE.changeInstanceState( ma, app.getMySql(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, msgClient.sentMessages.size());

		msg = msgClient.sentMessages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());
	}


	@Test
	public void testChangeInstanceState_childWithDeployingRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// The DM only propagates requests for child instances.
		// But since the root is deploying, messages are stored.
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Manager.INSTANCE.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STOPPED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Try other states
		ma.rootInstanceToAwaitingMessages.clear();
		Manager.INSTANCE.changeInstanceState( ma, app.getMySql(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNotNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());

		// Not_DEPLOYED
		ma.rootInstanceToAwaitingMessages.clear();
		Manager.INSTANCE.changeInstanceState( ma, app.getMySql(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySql().getStatus());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
		Assert.assertNull( ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent());
	}


	@Test
	public void testPerformDeployRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Nothing happens with children instances
		Manager.INSTANCE.deployRoot( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// Let's try with a root instance
		Manager.INSTANCE.deployRoot( ma, app.getMySqlVm());
		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());

		List<Message> messages = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdSetRootInstance.class, msg.getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetRootInstance) msg).getRootInstance());
	}


	@Test
	public void testPerformDeployRoot_alreadyDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// A root instance is considered to be deployed if it has a machine ID.
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "something" );
		Manager.INSTANCE.deployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
	}


	@Test( expected = IaasException.class )
	public void testPerformDeployRoot_iaasException() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver newResolver = new TestIaasResolver() {
			@Override
			public IaasInterface findIaasInterface( IaasInterface[] iaas, ManagedApplication ma, Instance instance )
			throws IaasException {
				throw new IaasException( "For test purpose!" );
			}
		};

		Manager.INSTANCE.setIaasResolver( newResolver );

		// Nothing happens with children instances
		try {
			Manager.INSTANCE.deployRoot( ma, app.getMySql());

		} catch( Exception e ) {
			Assert.fail( "Nothing should have happened here." );
		}

		Manager.INSTANCE.deployRoot( ma, app.getMySqlVm());
	}


	@Test
	public void testPerformUndeployRoot() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Nothing happens with children instances
		Manager.INSTANCE.undeployRoot( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// Let's try with a root instance
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "something" );
		Manager.INSTANCE.undeployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertFalse( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
	}


	@Test
	public void testPerformUndeployRoot_noMachineId() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Nothing happens with children instances
		Manager.INSTANCE.undeployRoot( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// Let's try with a root instance
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Manager.INSTANCE.undeployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
	}


	@Test
	public void testPerformUndeployRoot_notDeployed() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Nothing happens with children instances
		Manager.INSTANCE.undeployRoot( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// Let's try with a root instance
		app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		Manager.INSTANCE.undeployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// The state means nothing in fact, the machine ID does
		ma.rootInstanceToAwaitingMessages.clear();
		app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "something" );
		Manager.INSTANCE.undeployRoot( ma, app.getMySqlVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		Assert.assertNull( app.getMySqlVm().getData().get( Instance.MACHINE_ID ));
		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertFalse( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
	}


	@Test( expected = IaasException.class )
	public void testPerformUndeployRoot_iaasException() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver newResolver = new TestIaasResolver() {
			@Override
			public IaasInterface findIaasInterface( IaasInterface[] iaas, ManagedApplication ma, Instance instance )
			throws IaasException {
				throw new IaasException( "For test purpose!" );
			}
		};

		Manager.INSTANCE.setIaasResolver( newResolver );

		// Nothing happens with children instances
		try {
			Manager.INSTANCE.undeployRoot( ma, app.getMySql());

		} catch( Exception e ) {
			Assert.fail( "Nothing should have happened here." );
		}

		Manager.INSTANCE.undeployRoot( ma, app.getMySqlVm());
	}


	@Test
	public void testDeployAndStartAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Manager.INSTANCE.deployAndStartAll( ma, null );

		Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( app.getTomcatVm()));
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 2, ma.rootInstanceToAwaitingMessages.size());

		// MySQL
		List<Message> mySqlMessages = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm());
		Assert.assertEquals( 2, mySqlMessages.size());

		Assert.assertEquals( MsgCmdSetRootInstance.class, mySqlMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdSetRootInstance) mySqlMessages.get( 0 )).getRootInstance());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, mySqlMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getNewState());

		// Tomcat
		List<Message> tomcatMessages = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm());
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
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Manager.INSTANCE.deployAndStartAll( ma, app.getTomcatVm());

		Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( app.getTomcatVm()));
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());

		// Tomcat
		List<Message> tomcatMessages = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm());
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
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		Manager.INSTANCE.deployAndStartAll( ma, app.getTomcat());

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 2, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, msgClient.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msgClient.sentMessages.get( 0 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msgClient.sentMessages.get( 0 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, msgClient.sentMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) msgClient.sentMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) msgClient.sentMessages.get( 1 )).getNewState());
	}


	@Test
	public void testDeployAndStartAll_intermediateInstance_vmDeploying() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		app.getTomcatVm().setStatus( InstanceStatus.DEPLOYING );
		Manager.INSTANCE.deployAndStartAll( ma, app.getTomcat());

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());

		// Tomcat
		List<Message> tomcatMessages = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm());
		Assert.assertEquals( 2, tomcatMessages.size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 0 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 0 )).getNewState());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getNewState());
	}


	@Test
	public void testStopAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Manager.INSTANCE.stopAll( ma, null );

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 2, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()),((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());

		msg = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testStopAll_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Manager.INSTANCE.stopAll( ma, app.getTomcatVm());

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testStopAll_intermediateInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Manager.INSTANCE.stopAll( ma, app.getTomcat());

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}


	@Test
	public void testUndeployAll_application() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		app.getTomcatVm().getData().put( Instance.MACHINE_ID, "some id..." );
		app.getMySqlVm().getData().put( Instance.MACHINE_ID, "another id..." );
		Manager.INSTANCE.undeployAll( ma, null );

		Assert.assertFalse( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertFalse( iaasResolver.instanceToRunningStatus.get( app.getTomcatVm()));

		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
	}


	@Test
	public void testUndeployAll_rootInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		app.getTomcatVm().getData().put( Instance.MACHINE_ID, "some id..." );
		Manager.INSTANCE.undeployAll( ma, app.getTomcatVm());

		Assert.assertNull( iaasResolver.instanceToRunningStatus.get( app.getMySqlVm()));
		Assert.assertFalse( iaasResolver.instanceToRunningStatus.get( app.getTomcatVm()));
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());
	}


	@Test
	public void testUndeployAll_intermediateInstance() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Manager.INSTANCE.undeployAll( ma, app.getTomcat());

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msg).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) msg).getNewState());
	}
}
