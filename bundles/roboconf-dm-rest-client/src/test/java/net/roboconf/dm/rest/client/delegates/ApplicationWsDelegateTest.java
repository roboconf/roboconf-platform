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

package net.roboconf.dm.rest.client.delegates;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.internal.management.ManagementHelpers;
import net.roboconf.dm.internal.test.TestIaasResolver;
import net.roboconf.dm.internal.test.TestMessageServerClient;
import net.roboconf.dm.internal.test.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ApplicationException;
import net.roboconf.dm.rest.client.test.RestTestUtils;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdChangeInstanceState;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationWsDelegateTest extends JerseyTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TestApplication app;
	private WsClient client;
	private ManagedApplication ma;
	private Manager manager;


	@Before
	public void resetManager() throws Exception {

		// Reset and configure the fields
		File directory = this.folder.newFolder();

		this.manager = ManagementHelpers.createConfiguredManager();
		this.manager.getConfiguration().setMessgingFactory( new DmMessageServerClientFactory());
		this.manager.getConfiguration().setConfigurationDirectoryLocation( directory.getAbsolutePath());
		this.manager.setIaasResolver( new TestIaasResolver());
		this.manager.getConfiguration().update();

		// Load an application
		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app, null );
		this.manager.getAppNameToManagedApplication().put( this.app.getName(), this.ma );
		this.client = RestTestUtils.buildWsClient();
	}


	@After
	public void destroyClient() {
		if( this.client != null )
			this.client.destroy();
	}


	@Override
	protected AppDescriptor configure() {
		return RestTestUtils.buildTestDescriptor();
	}


	@Override
    public TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }


	@Test( expected = ApplicationException.class )
	public void testChangeInstanceState_inexistingApplication() throws Exception {
		this.client.getApplicationDelegate().changeInstanceState( "inexisting", InstanceStatus.DEPLOYED_STARTED, null );
	}


	@Test( expected = ApplicationException.class )
	public void testChangeInstanceState_inexistingInstance_null() throws Exception {
		this.client.getApplicationDelegate().changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED, null );
	}


	@Test( expected = ApplicationException.class )
	public void testChangeInstanceState_inexistingInstance() throws Exception {
		this.client.getApplicationDelegate().changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED, "/bip/bip" );
	}


	@Test( expected = ApplicationException.class )
	public void testChangeInstanceState_invalidState() throws Exception {
		this.client.getApplicationDelegate().changeInstanceState( this.app.getName(), null, null );
	}


	@Test
	public void testChangeInstanceState_deployRoot_success() throws Exception {

		TestIaasResolver iaasResolver = new TestIaasResolver();
		this.manager.setIaasResolver( iaasResolver );

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		this.client.getApplicationDelegate().changeInstanceState(
				this.app.getName(),
				InstanceStatus.DEPLOYED_STARTED,
				InstanceHelpers.computeInstancePath( this.app.getMySqlVm()));

		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( this.app.getMySqlVm()));
	}


	@Test
	public void testChangeInstanceState_deploy_success() throws Exception {

		TestMessageServerClient msgClient = (TestMessageServerClient) this.manager.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED, instancePath );
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
	}


	@Test
	public void testStopAll() throws Exception {

		TestMessageServerClient msgClient = (TestMessageServerClient) this.manager.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().stopAll( this.app.getName(), instancePath );
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, ((MsgCmdChangeInstanceState) messages.get( 0 )).getNewState());
	}


	@Test( expected = ApplicationException.class )
	public void testStopAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().stopAll( "oops", instancePath );
	}


	@Test
	public void testUndeployAll() throws Exception {

		TestMessageServerClient msgClient = (TestMessageServerClient) this.manager.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().undeployAll( this.app.getName(), instancePath );
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, ((MsgCmdChangeInstanceState) messages.get( 0 )).getNewState());
	}


	@Test( expected = ApplicationException.class )
	public void testUndeployAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().undeployAll( "oops", instancePath );
	}


	@Test
	public void testDeployAndStartAll() throws Exception {

		TestMessageServerClient msgClient = (TestMessageServerClient) this.manager.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().deployAndStartAll( this.app.getName(), instancePath );
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 2, messages.size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getWar()), ((MsgCmdChangeInstanceState) messages.get( 1 )).getInstancePath());
	}


	@Test( expected = ApplicationException.class )
	public void testDeployAndStartAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().deployAndStartAll( "oops", instancePath );
	}


	@Test
	public void testListChildrenInstances() throws Exception {

		List<Instance> instances = this.client.getApplicationDelegate().listChildrenInstances( this.app.getName(), "/bip/bip", false );
		Assert.assertEquals( 0, instances.size());

		instances = this.client.getApplicationDelegate().listChildrenInstances( this.app.getName(), null, false );
		Assert.assertEquals( this.app.getRootInstances().size(), instances.size());

		instances = this.client.getApplicationDelegate().listChildrenInstances( this.app.getName(), null, true );
		Assert.assertEquals( InstanceHelpers.getAllInstances( this.app ).size(), instances.size());

		instances = this.client.getApplicationDelegate().listChildrenInstances( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), false );
		Assert.assertEquals( 1, instances.size());

		instances = this.client.getApplicationDelegate().listChildrenInstances( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), true );
		Assert.assertEquals( 2, instances.size());
	}


	@Test
	public void testListAllComponents() throws Exception {

		List<Component> components = this.client.getApplicationDelegate().listAllComponents( "inexisting" );
		Assert.assertEquals( 0, components.size());

		components = this.client.getApplicationDelegate().listAllComponents( this.app.getName());
		Assert.assertEquals( ComponentHelpers.findAllComponents( this.app ).size(), components.size());
	}


	@Test
	public void testFindPossibleComponentChildren() throws Exception {

		List<Component> components = this.client.getApplicationDelegate().findPossibleComponentChildren( "inexisting", "" );
		Assert.assertEquals( 0, components.size());

		components = this.client.getApplicationDelegate().findPossibleComponentChildren( this.app.getName(), "inexisting-component" );
		Assert.assertEquals( 0, components.size());

		components = this.client.getApplicationDelegate().findPossibleComponentChildren( this.app.getName(), null );
		Assert.assertEquals( 1, components.size());
		Assert.assertTrue( components.contains( this.app.getMySqlVm().getComponent()));

		components = this.client.getApplicationDelegate().findPossibleComponentChildren( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getMySqlVm()));
		Assert.assertEquals( 2, components.size());
		Assert.assertTrue( components.contains( this.app.getMySql().getComponent()));
		Assert.assertTrue( components.contains( this.app.getTomcat().getComponent()));
	}


	@Test
	public void testFindPossibleParentInstances() throws Exception {

		List<String> instancePaths = this.client.getApplicationDelegate().findPossibleParentInstances( "inexisting", "my-comp" );
		Assert.assertEquals( 0, instancePaths.size());

		instancePaths = this.client.getApplicationDelegate().findPossibleParentInstances( this.app.getName(), "my-comp" );
		Assert.assertEquals( 0, instancePaths.size());

		instancePaths = this.client.getApplicationDelegate().findPossibleParentInstances( this.app.getName(), this.app.getTomcat().getComponent().getName());
		Assert.assertEquals( 2, instancePaths.size());
		Assert.assertTrue( instancePaths.contains( InstanceHelpers.computeInstancePath( this.app.getMySqlVm())));
		Assert.assertTrue( instancePaths.contains( InstanceHelpers.computeInstancePath( this.app.getTomcatVm())));
	}


	@Test
	public void testCreateInstanceFromComponent() throws Exception {

		Instance newInstance = this.client.getApplicationDelegate().createInstanceFromComponent( "inexisting", "my-comp" );
		Assert.assertNull( newInstance );

		String componentName = this.app.getMySqlVm().getComponent().getName();
		newInstance = this.client.getApplicationDelegate().createInstanceFromComponent( this.app.getName(), componentName );
		Assert.assertNotNull( newInstance );
		Assert.assertEquals( componentName, newInstance.getComponent().getName());
	}


	@Test
	public void testAddInstance_root_success() throws Exception {

		Assert.assertEquals( 2, this.app.getRootInstances().size());
		Instance newInstance = new Instance( "vm-mail" ).component( this.app.getMySqlVm().getComponent());
		this.client.getApplicationDelegate().addInstance( this.app.getName(), null, newInstance );
		Assert.assertEquals( 3, this.app.getRootInstances().size());
	}


	@Test( expected = ApplicationException.class )
	public void testAddInstance_root_failure() throws Exception {

		Assert.assertEquals( 2, this.app.getRootInstances().size());
		Instance existingInstance = new Instance( this.app.getMySqlVm().getName());
		this.client.getApplicationDelegate().addInstance( this.app.getName(), null, existingInstance );
	}


	@Test
	public void testAddInstance_child_success() throws Exception {

		Instance newMysql = new Instance( "mysql-2" ).component( this.app.getMySql().getComponent());

		Assert.assertEquals( 1, this.app.getTomcatVm().getChildren().size());
		Assert.assertFalse( this.app.getTomcatVm().getChildren().contains( newMysql ));

		this.client.getApplicationDelegate().addInstance( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newMysql );
		Assert.assertEquals( 2, this.app.getTomcatVm().getChildren().size());

		List<String> paths = new ArrayList<String> ();
		for( Instance inst : this.app.getTomcatVm().getChildren())
			paths.add( InstanceHelpers.computeInstancePath( inst ));

		String rootPath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		Assert.assertTrue( paths.contains( rootPath + "/" + newMysql.getName()));
		Assert.assertTrue( paths.contains( rootPath + "/" + this.app.getTomcat().getName()));
	}


	@Test
	public void testAddInstance_child_incompleteComponent() throws Exception {

		// Pass an incomplete component object to the REST API
		String mySqlComponentName = this.app.getMySql().getComponent().getName();
		Instance newMysql = new Instance( "mysql-2" ).component( new Component( mySqlComponentName ));

		Assert.assertEquals( 1, this.app.getTomcatVm().getChildren().size());
		Assert.assertFalse( this.app.getTomcatVm().getChildren().contains( newMysql ));

		this.client.getApplicationDelegate().addInstance( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newMysql );
		Assert.assertEquals( 2, this.app.getTomcatVm().getChildren().size());

		List<String> paths = new ArrayList<String> ();
		for( Instance inst : this.app.getTomcatVm().getChildren())
			paths.add( InstanceHelpers.computeInstancePath( inst ));

		String rootPath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		Assert.assertTrue( paths.contains( rootPath + "/" + newMysql.getName()));
		Assert.assertTrue( paths.contains( rootPath + "/" + this.app.getTomcat().getName()));
	}


	@Test( expected = ApplicationException.class )
	public void testAddInstance_child_failure() throws Exception {

		// We cannot deploy a WAR directly on a VM!
		// At least, this what the graph says.
		Instance newWar = new Instance( "war-2" ).component( this.app.getWar().getComponent());

		Assert.assertEquals( 1, this.app.getTomcatVm().getChildren().size());
		Assert.assertFalse( this.app.getTomcatVm().getChildren().contains( newWar ));
		this.client.getApplicationDelegate().addInstance( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newWar );
	}


	@Test( expected = ApplicationException.class )
	public void testAddInstance_inexstingApplication() throws Exception {

		Instance newMysql = new Instance( "mysql-2" ).component( this.app.getMySql().getComponent());
		this.client.getApplicationDelegate().addInstance( "inexisting", InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newMysql );
	}


	@Test( expected = ApplicationException.class )
	public void testAddInstance_inexstingParentInstance() throws Exception {

		Instance newMysql = new Instance( "mysql-2" ).component( this.app.getMySql().getComponent());
		this.client.getApplicationDelegate().addInstance( "inexisting", "/bip/bip", newMysql );
	}
}