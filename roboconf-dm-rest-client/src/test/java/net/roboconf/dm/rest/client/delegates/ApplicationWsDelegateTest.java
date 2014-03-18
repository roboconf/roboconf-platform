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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.internal.TestApplication;
import net.roboconf.dm.internal.TestIaasResolver;
import net.roboconf.dm.internal.TestMessageServerClient;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ApplicationException;
import net.roboconf.dm.rest.client.test.RestTestUtils;
import net.roboconf.dm.utils.ResourceUtils;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationWsDelegateTest extends JerseyTest {

	@Override
	protected AppDescriptor configure() {
		return RestTestUtils.buildTestDescriptor();
	}


	@Override
    public TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }


	@Before
	public void resetManager() {
		Manager.INSTANCE.cleanUpAll();
		Manager.INSTANCE.getAppNameToManagedApplication().clear();
		Manager.INSTANCE.setIaasResolver( new TestIaasResolver());
	}


	@Test( expected = IllegalArgumentException.class )
	public void testPerform_illegalArgument() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		client.getApplicationDelegate().perform( app.getName(), ApplicationAction.deploy, null, false );
	}


	@Test( expected = ApplicationException.class )
	public void testPerform_inexistingApplication() throws Exception {

		WsClient client = RestTestUtils.buildWsClient();
		client.getApplicationDelegate().perform( "inexisting", ApplicationAction.deploy, null, true );
	}


	@Test( expected = ApplicationException.class )
	public void testPerform_inexistingInstance() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		client.getApplicationDelegate().perform( app.getName(), ApplicationAction.deploy, "/bip/bip", false );
	}


	@Test( expected = ApplicationException.class )
	public void testPerform_invalidAction() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		client.getApplicationDelegate().perform( app.getName(), null, null, true );
	}


	@Test
	public void testPerform_deploy_success() throws Exception {

		// The interest of this method is to check that URLs
		// and instance paths are correctly handled by the DM.
		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		client.getApplicationDelegate().perform(
				app.getName(),
				ApplicationAction.deploy,
				InstanceHelpers.computeInstancePath( app.getMySqlVm()),
				false );
	}


	@Test
	public void testPerform_deployRoots_success() throws Exception {

		// Create temporary directories
		TestApplication app = new TestApplication();
		final File rootDir = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_app" );
		if( ! rootDir.exists()
				&& ! rootDir.mkdir())
			throw new IOException( "Failed to create a root directory for tests." );

		for( Instance inst : InstanceHelpers.getAllInstances( app )) {
			File f = ResourceUtils.findInstanceResourcesDirectory( rootDir, inst );
			if( ! f.exists()
					&& ! f.mkdirs())
				throw new IOException( "Failed to create a directory for tests. " + f.getAbsolutePath());
		}

		// The interest of this method is to check that URLs
		// and instance paths are correctly handled by the DM.
		TestMessageServerClient msgClient = new TestMessageServerClient();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, rootDir, msgClient ));

		try {
			WsClient client = RestTestUtils.buildWsClient();
			Assert.assertEquals( 0, msgClient.messageToRoutingKey.size());
			client.getApplicationDelegate().perform( app.getName(), ApplicationAction.deploy, null, true );

			int expected = InstanceHelpers.getAllInstances( app ).size() - app.getRootInstances().size();
			Assert.assertEquals( expected, msgClient.messageToRoutingKey.size());

		} finally {
			Utils.deleteFilesRecursively( rootDir );
		}
	}


	@Test
	public void testListChildrenInstances() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		List<Instance> instances = client.getApplicationDelegate().listChildrenInstances( app.getName(), "/bip/bip", false );
		Assert.assertEquals( 0, instances.size());

		instances = client.getApplicationDelegate().listChildrenInstances( app.getName(), null, false );
		Assert.assertEquals( app.getRootInstances().size(), instances.size());

		instances = client.getApplicationDelegate().listChildrenInstances( app.getName(), null, true );
		Assert.assertEquals( InstanceHelpers.getAllInstances( app ).size(), instances.size());

		instances = client.getApplicationDelegate().listChildrenInstances( app.getName(), InstanceHelpers.computeInstancePath( app.getTomcatVm()), true );
		Assert.assertEquals( 2, instances.size());
	}


	@Test
	public void testListAllComponents() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		List<Component> components = client.getApplicationDelegate().listAllComponents( "inexisting" );
		Assert.assertEquals( 0, components.size());

		components = client.getApplicationDelegate().listAllComponents( app.getName());
		Assert.assertEquals( ComponentHelpers.findAllComponents( app ).size(), components.size());
	}


	@Test
	public void testFindPossibleComponentChildren() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		List<Component> components = client.getApplicationDelegate().findPossibleComponentChildren( "inexisting", "" );
		Assert.assertEquals( 0, components.size());

		components = client.getApplicationDelegate().findPossibleComponentChildren( app.getName(), "inexisting-component" );
		Assert.assertEquals( 0, components.size());

		components = client.getApplicationDelegate().findPossibleComponentChildren( app.getName(), null );
		Assert.assertEquals( 1, components.size());
		Assert.assertTrue( components.contains( app.getMySqlVm().getComponent()));

		components = client.getApplicationDelegate().findPossibleComponentChildren( app.getName(), InstanceHelpers.computeInstancePath( app.getMySqlVm()));
		Assert.assertEquals( 2, components.size());
		Assert.assertTrue( components.contains( app.getMySql().getComponent()));
		Assert.assertTrue( components.contains( app.getTomcat().getComponent()));
	}


	@Test
	public void testFindPossibleParentInstances() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		List<String> instancePaths = client.getApplicationDelegate().findPossibleParentInstances( "inexisting", "my-comp" );
		Assert.assertEquals( 0, instancePaths.size());

		instancePaths = client.getApplicationDelegate().findPossibleParentInstances( app.getName(), "my-comp" );
		Assert.assertEquals( 0, instancePaths.size());

		instancePaths = client.getApplicationDelegate().findPossibleParentInstances( app.getName(), app.getTomcat().getComponent().getName());
		Assert.assertEquals( 2, instancePaths.size());
		Assert.assertTrue( instancePaths.contains( InstanceHelpers.computeInstancePath( app.getMySqlVm())));
		Assert.assertTrue( instancePaths.contains( InstanceHelpers.computeInstancePath( app.getTomcatVm())));
	}


	@Test
	public void testCreateInstanceFromComponent() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		Instance newInstance = client.getApplicationDelegate().createInstanceFromComponent( "inexisting", "my-comp" );
		Assert.assertNull( newInstance );

		String componentName = app.getMySqlVm().getComponent().getName();
		newInstance = client.getApplicationDelegate().createInstanceFromComponent( app.getName(), componentName );
		Assert.assertNotNull( newInstance );
		Assert.assertEquals( componentName, newInstance.getComponent().getName());
	}


	@Test
	public void testAddInstance_root_success() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 2, app.getRootInstances().size());

		Instance newInstance = new Instance( "vm-mail" );
		newInstance.setComponent( app.getMySqlVm().getComponent());

		client.getApplicationDelegate().addInstance( app.getName(), null, newInstance );
		Assert.assertEquals( 3, app.getRootInstances().size());
	}


	@Test( expected = ApplicationException.class )
	public void testAddInstance_root_failure() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		Assert.assertEquals( 2, app.getRootInstances().size());

		Instance existingInstance = new Instance( app.getMySqlVm().getName());
		client.getApplicationDelegate().addInstance( app.getName(), null, existingInstance );
	}


	@Test
	public void testAddInstance_child_success() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		Instance newMysql = new Instance( "mysql-2" );
		newMysql.setComponent( app.getMySql().getComponent());

		Assert.assertEquals( 1, app.getTomcatVm().getChildren().size());
		Assert.assertFalse( app.getTomcatVm().getChildren().contains( newMysql ));

		client.getApplicationDelegate().addInstance( app.getName(), InstanceHelpers.computeInstancePath( app.getTomcatVm()), newMysql );
		Assert.assertEquals( 2, app.getTomcatVm().getChildren().size());

		List<String> paths = new ArrayList<String> ();
		for( Instance inst : app.getTomcatVm().getChildren())
			paths.add( InstanceHelpers.computeInstancePath( inst ));

		String rootPath = InstanceHelpers.computeInstancePath( app.getTomcatVm());
		Assert.assertTrue( paths.contains( rootPath + "/" + newMysql.getName()));
		Assert.assertTrue( paths.contains( rootPath + "/" + app.getTomcat().getName()));
	}


	@Test( expected = ApplicationException.class )
	public void testAddInstance_child_failure() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		// We cannot deploy a WAR directly on a VM!
		// At least, this what the graph says.
		WsClient client = RestTestUtils.buildWsClient();
		Instance newWar = new Instance( "war-2" );
		newWar.setComponent( app.getWar().getComponent());

		Assert.assertEquals( 1, app.getTomcatVm().getChildren().size());
		Assert.assertFalse( app.getTomcatVm().getChildren().contains( newWar ));
		client.getApplicationDelegate().addInstance( app.getName(), InstanceHelpers.computeInstancePath( app.getTomcatVm()), newWar );
	}


	@Test( expected = ApplicationException.class )
	public void testAddInstance_inexstingApplication() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		Instance newMysql = new Instance( "mysql-2" );
		newMysql.setComponent( app.getMySql().getComponent());
		client.getApplicationDelegate().addInstance( "inexisting", InstanceHelpers.computeInstancePath( app.getTomcatVm()), newMysql );
	}


	@Test( expected = ApplicationException.class )
	public void testAddInstance_inexstingParentInstance() throws Exception {

		TestApplication app = new TestApplication();
		Manager.INSTANCE.getAppNameToManagedApplication().put(
				app.getName(),
				new ManagedApplication( app, null, new TestMessageServerClient()));

		WsClient client = RestTestUtils.buildWsClient();
		Instance newMysql = new Instance( "mysql-2" );
		newMysql.setComponent( app.getMySql().getComponent());
		client.getApplicationDelegate().addInstance( "inexisting", "/bip/bip", newMysql );
	}
}
