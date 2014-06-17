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
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.internal.TestMessageServerClient;
import net.roboconf.dm.internal.TestMessageServerClient.DmMessageServerClientFactory;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.ManagerConfiguration;
import net.roboconf.dm.rest.api.ApplicationAction;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ApplicationException;
import net.roboconf.dm.rest.client.test.RestTestUtils;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceDeploy;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStart;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceStop;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdInstanceUndeploy;

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


	@Before
	public void resetManager() throws Exception {

		// Shutdown used with a temporary folder will cause "IO Exceptions" (failed to save instances)
		Manager.INSTANCE.shutdown();
		Manager.INSTANCE.setIaasResolver( new TestIaasResolver());
		Manager.INSTANCE.setMessagingClientFactory( new DmMessageServerClientFactory());

		File dir = this.folder.newFolder();
		ManagerConfiguration conf = ManagerConfiguration.createConfiguration( dir );
		Manager.INSTANCE.initialize( conf );

		// Load an application
		this.app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put( this.app.getName(), new ManagedApplication( this.app, null ));
		this.client = RestTestUtils.buildWsClient();
	}


	@Test( expected = ApplicationException.class )
	public void testPerform_inexistingApplication() throws Exception {
		this.client.getApplicationDelegate().perform( "inexisting", ApplicationAction.deploy, null );
	}


	@Test( expected = ApplicationException.class )
	public void testPerform_inexistingInstance_null() throws Exception {
		this.client.getApplicationDelegate().perform( this.app.getName(), ApplicationAction.deploy, null );
	}


	@Test( expected = ApplicationException.class )
	public void testPerform_inexistingInstance() throws Exception {
		this.client.getApplicationDelegate().perform( this.app.getName(), ApplicationAction.deploy, "/bip/bip" );
	}


	@Test( expected = ApplicationException.class )
	public void testPerform_invalidAction() throws Exception {
		this.client.getApplicationDelegate().perform( this.app.getName(), null, null );
	}


	@Test
	public void testPerform_deployRoot_success() throws Exception {

		TestIaasResolver iaasResolver = new TestIaasResolver();
		Manager.INSTANCE.setIaasResolver( iaasResolver );

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		this.client.getApplicationDelegate().perform(
				this.app.getName(),
				ApplicationAction.deploy,
				InstanceHelpers.computeInstancePath( this.app.getMySqlVm()));

		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( this.app.getMySqlVm()));
	}


	@Test
	public void testPerform_deploy_success() throws Exception {

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();

		Assert.assertEquals( 0, msgClient.sentMessages.size());
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().perform( this.app.getName(), ApplicationAction.deploy, instancePath );

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals( MsgCmdInstanceDeploy.class, msgClient.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdInstanceDeploy) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test
	public void testStopAll() throws Exception {

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().stopAll( this.app.getName(), instancePath );

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals( MsgCmdInstanceStop.class, msgClient.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdInstanceStop) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test( expected = ApplicationException.class )
	public void testStopAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().stopAll( "oops", instancePath );
	}


	@Test
	public void testUndeployAll() throws Exception {

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().undeployAll( this.app.getName(), instancePath );

		Assert.assertEquals( 1, msgClient.sentMessages.size());
		Assert.assertEquals( MsgCmdInstanceUndeploy.class, msgClient.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdInstanceUndeploy) msgClient.sentMessages.get( 0 )).getInstancePath());
	}


	@Test( expected = ApplicationException.class )
	public void testUndeployAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().undeployAll( "oops", instancePath );
	}


	@Test
	public void testDeployAndStartAll() throws Exception {

		TestMessageServerClient msgClient = (TestMessageServerClient) Manager.INSTANCE.getMessagingClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().deployAndStartAll( this.app.getName(), instancePath );
		Assert.assertEquals( 4, msgClient.sentMessages.size());

		Assert.assertEquals( MsgCmdInstanceDeploy.class, msgClient.sentMessages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdInstanceDeploy) msgClient.sentMessages.get( 0 )).getInstancePath());

		Assert.assertEquals( MsgCmdInstanceStart.class, msgClient.sentMessages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdInstanceStart) msgClient.sentMessages.get( 1 )).getInstancePath());

		Assert.assertEquals( MsgCmdInstanceDeploy.class, msgClient.sentMessages.get( 2 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getWar()), ((MsgCmdInstanceDeploy) msgClient.sentMessages.get( 2 )).getInstancePath());

		Assert.assertEquals( MsgCmdInstanceStart.class, msgClient.sentMessages.get( 3 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getWar()), ((MsgCmdInstanceStart) msgClient.sentMessages.get( 3 )).getInstancePath());
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
