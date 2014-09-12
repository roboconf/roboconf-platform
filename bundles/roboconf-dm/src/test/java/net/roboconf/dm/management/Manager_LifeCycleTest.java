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
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdStartInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdStopInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdUndeployInstance;

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
	public void testPerformDeploy() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Nothing happens with root instances
		Manager.INSTANCE.deploy( ma, app.getMySqlVm());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// Let's try with a child instance
		Manager.INSTANCE.deploy( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySql());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) msg).getInstancePath());

		// There is no component directory, so 0 file to send during this test
		Assert.assertEquals( 0, ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent().size());
	}


	@Test
	public void testPerformDeploy_rootIsDeploying() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// The message will be stored
		Manager.INSTANCE.deploy( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());

		List<Message> messages = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdChangeInstanceState.class, msg.getClass());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySql());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) msg).getInstancePath());

		// There is no component directory, so 0 file to send during this test
		Assert.assertEquals( 0, ((MsgCmdChangeInstanceState) msg).getFileNameToFileContent().size());
	}


	@Test
	public void testPerformStart() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Nothing happens with root instances
		Manager.INSTANCE.start( ma, app.getMySqlVm());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Let's try with a child instance
		Manager.INSTANCE.start( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdStartInstance.class, msg.getClass());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySql());
		Assert.assertEquals( instancePath, ((MsgCmdStartInstance) msg).getInstancePath());
	}


	@Test
	public void testPerformStart_rootIsDeploying() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// The message will be stored
		Manager.INSTANCE.start( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());

		List<Message> messages = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdStartInstance.class, msg.getClass());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySql());
		Assert.assertEquals( instancePath, ((MsgCmdStartInstance) msg).getInstancePath());
	}


	@Test
	public void testPerformStop() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Nothing happens with root instances
		Manager.INSTANCE.stop( ma, app.getMySqlVm());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Let's try with a child instance
		Manager.INSTANCE.stop( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdStopInstance.class, msg.getClass());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySql());
		Assert.assertEquals( instancePath, ((MsgCmdStopInstance) msg).getInstancePath());
	}


	@Test
	public void testPerformStop_rootIsDeploying() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// The message will be stored
		Manager.INSTANCE.stop( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());

		List<Message> messages = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdStopInstance.class, msg.getClass());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySql());
		Assert.assertEquals( instancePath, ((MsgCmdStopInstance) msg).getInstancePath());
	}


	@Test
	public void testPerformUndeploy() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Nothing happens with root instances
		Manager.INSTANCE.undeploy( ma, app.getMySqlVm());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		// Let's try with a child instance
		Manager.INSTANCE.undeploy( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).size());

		Message msg = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm()).get( 0 );
		Assert.assertEquals( MsgCmdUndeployInstance.class, msg.getClass());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySql());
		Assert.assertEquals( instancePath, ((MsgCmdUndeployInstance) msg).getInstancePath());
	}


	@Test
	public void testPerformUndeploy_rootIsDeploying() throws Exception {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app, null );
		Manager.INSTANCE.getAppNameToManagedApplication().put( app.getName(), ma );

		TestIaasResolver iaasResolver = (TestIaasResolver) Manager.INSTANCE.iaasResolver;
		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.messagingClient;
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		// The message will be stored
		Manager.INSTANCE.undeploy( ma, app.getMySql());
		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 1, ma.rootInstanceToAwaitingMessages.size());

		List<Message> messages = ma.rootInstanceToAwaitingMessages.get( app.getMySqlVm());
		Assert.assertEquals( 1, messages.size());

		Message msg = messages.get( 0 );
		Assert.assertEquals( MsgCmdUndeployInstance.class, msg.getClass());

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySql());
		Assert.assertEquals( instancePath, ((MsgCmdUndeployInstance) msg).getInstancePath());
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
		Assert.assertEquals( MsgCmdAddInstance.class, msg.getClass());

		Assert.assertNull(((MsgCmdAddInstance) msg).getParentInstancePath());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdAddInstance) msg).getInstanceToAdd());
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
		Assert.assertEquals( 3, mySqlMessages.size());

		Assert.assertEquals( MsgCmdAddInstance.class, mySqlMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getMySqlVm(), ((MsgCmdAddInstance)mySqlMessages.get( 0 )).getInstanceToAdd());
		Assert.assertNull(((MsgCmdAddInstance) mySqlMessages.get( 0 )).getParentInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, mySqlMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdChangeInstanceState) mySqlMessages.get( 1 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, mySqlMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()), ((MsgCmdStartInstance) mySqlMessages.get( 2 )).getInstancePath());

		// Tomcat
		List<Message> tomcatMessages = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm());
		Assert.assertEquals( 5, tomcatMessages.size());

		Assert.assertEquals( MsgCmdAddInstance.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getTomcatVm(), ((MsgCmdAddInstance) tomcatMessages.get( 0 )).getInstanceToAdd());
		Assert.assertNull(((MsgCmdAddInstance) tomcatMessages.get( 0 )).getParentInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, tomcatMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdStartInstance) tomcatMessages.get( 2 )).getInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 3 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 3 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, tomcatMessages.get( 4 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdStartInstance) tomcatMessages.get( 4 )).getInstancePath());
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
		Assert.assertEquals( 5, tomcatMessages.size());

		Assert.assertEquals( MsgCmdAddInstance.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( app.getTomcatVm(), ((MsgCmdAddInstance) tomcatMessages.get( 0 )).getInstanceToAdd());
		Assert.assertNull(((MsgCmdAddInstance) tomcatMessages.get( 0 )).getParentInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 1 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, tomcatMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdStartInstance) tomcatMessages.get( 2 )).getInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 3 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 3 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, tomcatMessages.get( 4 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdStartInstance) tomcatMessages.get( 4 )).getInstancePath());
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
		Assert.assertEquals( 4, msgClient.sentMessages.size());
		Assert.assertEquals( 0, ma.rootInstanceToAwaitingMessages.size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, msgClient.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) msgClient.sentMessages.get( 0 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, msgClient.sentMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdStartInstance) msgClient.sentMessages.get( 1 )).getInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, msgClient.sentMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) msgClient.sentMessages.get( 2 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, msgClient.sentMessages.get( 3 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdStartInstance) msgClient.sentMessages.get( 3 )).getInstancePath());
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
		Assert.assertEquals( 4, tomcatMessages.size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 0 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, tomcatMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdStartInstance) tomcatMessages.get( 1 )).getInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, tomcatMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdChangeInstanceState) tomcatMessages.get( 2 )).getInstancePath());

		Assert.assertEquals( MsgCmdStartInstance.class, tomcatMessages.get( 3 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getWar()), ((MsgCmdStartInstance) tomcatMessages.get( 3 )).getInstancePath());
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
		Assert.assertEquals( MsgCmdStopInstance.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getMySql()),((MsgCmdStopInstance) msg).getInstancePath());

		msg = ma.rootInstanceToAwaitingMessages.get( app.getTomcatVm()).get( 0 );
		Assert.assertEquals( MsgCmdStopInstance.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdStopInstance) msg).getInstancePath());
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
		Assert.assertEquals( MsgCmdStopInstance.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdStopInstance) msg).getInstancePath());
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
		Assert.assertEquals( MsgCmdStopInstance.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdStopInstance) msg).getInstancePath());
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
		Assert.assertEquals( MsgCmdUndeployInstance.class, msg.getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( app.getTomcat()), ((MsgCmdUndeployInstance) msg).getInstancePath());
	}
}
