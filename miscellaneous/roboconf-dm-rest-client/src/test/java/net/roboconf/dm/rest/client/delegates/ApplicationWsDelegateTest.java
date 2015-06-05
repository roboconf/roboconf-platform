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

package net.roboconf.dm.rest.client.delegates;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;

import javax.ws.rs.core.UriBuilder;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ApplicationException;
import net.roboconf.dm.rest.services.internal.RestApplication;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClientDm;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationWsDelegateTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private static final String REST_URI = "http://localhost:8090";

	private TestApplication app;
	private WsClient client;
	private ManagedApplication ma;
	private Manager manager;
	private HttpServer httpServer;
	private TestClientDm msgClient;
	private MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();


	@After
	public void after() {

		this.manager.stop();
		if( this.httpServer != null )
			this.httpServer.stop();

		if( this.client != null )
			this.client.destroy();
	}


	@Before
	public void before() throws Exception {
		this.registry.addMessagingClientFactory(new TestClientFactory());

		this.manager = new Manager();
		this.manager.setMessagingType(MessagingConstants.TEST_FACTORY_TYPE);
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setConfigurationDirectoryLocation( this.folder.newFolder().getAbsolutePath());
		this.manager.start();

		// Reconfigure with the messaging client factory registry set.
		this.manager.getMessagingClient().setRegistry(this.registry);
		this.manager.reconfigure();


		this.msgClient = TestUtils.getInternalField( this.manager.getMessagingClient(), "messagingClient", TestClientDm.class );
		this.msgClient.sentMessages.clear();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class).cancel();

		URI uri = UriBuilder.fromUri( REST_URI ).build();
		RestApplication restApp = new RestApplication( this.manager );
		this.httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );;

		// Load an application
		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );
		this.manager.getNameToManagedApplication().put( this.app.getName(), this.ma );

		this.client = new WsClient( REST_URI );
	}


	@Test( expected = ApplicationException.class )
	public void testChangeState_inexistingApplication() throws Exception {
		this.client.getApplicationDelegate().changeInstanceState( "inexisting", InstanceStatus.DEPLOYED_STARTED, null );
	}


	@Test( expected = ApplicationException.class )
	public void testChangeState_inexistingInstance_null() throws Exception {
		this.client.getApplicationDelegate().changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED, null );
	}


	@Test( expected = ApplicationException.class )
	public void testChangeState_inexistingInstance() throws Exception {
		this.client.getApplicationDelegate().changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED, "/bip/bip" );
	}


	@Test( expected = ApplicationException.class )
	public void testChangeState_invalidAction() throws Exception {
		this.client.getApplicationDelegate().changeInstanceState( this.app.getName(), null, null );
	}


	@Test
	public void testChangeState_deployRoot_success() throws Exception {

		TestTargetResolver iaasResolver = new TestTargetResolver();
		this.manager.setTargetResolver( iaasResolver );

		Assert.assertEquals( 0, iaasResolver.instanceToRunningStatus.size());
		this.client.getApplicationDelegate().changeInstanceState(
				this.app.getName(),
				InstanceStatus.DEPLOYED_STARTED,
				InstanceHelpers.computeInstancePath( this.app.getMySqlVm()));

		Assert.assertEquals( 1, iaasResolver.instanceToRunningStatus.size());
		Assert.assertTrue( iaasResolver.instanceToRunningStatus.get( this.app.getMySqlVm()));
	}


	@Test
	public void testChangeState_deploy_success() throws Exception {

		TestClientDm msgClient = getInternalClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STOPPED, instancePath );
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
	}


	@Test
	public void testStopAll() throws Exception {

		TestClientDm msgClient = getInternalClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().stopAll( this.app.getName(), instancePath );
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
	}


	@Test( expected = ApplicationException.class )
	public void testStopAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().stopAll( "oops", instancePath );
	}


	@Test
	public void testUndeployAll() throws Exception {

		TestClientDm msgClient = getInternalClient();
		Assert.assertEquals( 0, msgClient.sentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().undeployAll( this.app.getName(), instancePath );
		Assert.assertEquals( 0, msgClient.sentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
	}


	@Test( expected = ApplicationException.class )
	public void testUndeployAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		this.client.getApplicationDelegate().undeployAll( "oops", instancePath );
	}


	@Test
	public void testDeployAndStartAll() throws Exception {

		TestClientDm msgClient = getInternalClient();
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
	public void testFindComponentChildren() throws Exception {

		List<Component> components = this.client.getApplicationDelegate().findComponentChildren( "inexisting", "" );
		Assert.assertEquals( 0, components.size());

		components = this.client.getApplicationDelegate().findComponentChildren( this.app.getName(), "inexisting-component" );
		Assert.assertEquals( 0, components.size());

		components = this.client.getApplicationDelegate().findComponentChildren( this.app.getName(), null );
		Assert.assertEquals( 1, components.size());
		Assert.assertTrue( components.contains( this.app.getMySqlVm().getComponent()));

		components = this.client.getApplicationDelegate().findComponentChildren( this.app.getName(), this.app.getMySqlVm().getComponent().getName());
		Assert.assertEquals( 2, components.size());
		Assert.assertTrue( components.contains( this.app.getMySql().getComponent()));;
		Assert.assertTrue( components.contains( this.app.getTomcat().getComponent()));;
	}


	@Test
	public void testFindComponentAncestors() throws Exception {

		List<Component> components = this.client.getApplicationDelegate().findComponentAncestors( "inexisting", "my-comp" );
		Assert.assertEquals( 0, components.size());

		components = this.client.getApplicationDelegate().findComponentAncestors( this.app.getName(), "my-comp" );
		Assert.assertEquals( 0, components.size());

		components = this.client.getApplicationDelegate().findComponentAncestors( this.app.getName(), this.app.getTomcat().getComponent().getName());
		Assert.assertEquals( 1, components.size());
		Assert.assertTrue( components.contains( this.app.getMySqlVm().getComponent()));
		Assert.assertTrue( components.contains( this.app.getTomcatVm().getComponent()));
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


	@Test
	public void testRemoveInstance_success() {
		// Check the Tomcat instance is here.
		final String tomcatPath = InstanceHelpers.computeInstancePath( this.app.getTomcat() );
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, tomcatPath ) );

		// Delete the Tomcat instance.
		this.client.getApplicationDelegate().removeInstance( this.app.getName(), tomcatPath );

		// Check it is gone.
		Assert.assertNull( InstanceHelpers.findInstanceByPath( this.app, tomcatPath ) );
	}


	@Test
	public void testRemoveInstance_nonExistingInstance() {
		try {
			this.client.getApplicationDelegate().removeInstance( this.app.getName(), "/I-do-not-exist" );
			Assert.fail( "Expecting exception" );
		} catch ( UniformInterfaceException e ) {
			// Not found!
			Assert.assertEquals( 404, e.getResponse().getStatus() );
		}
	}


	@Test
	public void testRemoveInstance_nonExistingApplication() {
		try {
			this.client.getApplicationDelegate().removeInstance( "I-am-not-an-app", InstanceHelpers.computeInstancePath( this.app.getTomcat() ) );
			Assert.fail( "Expecting exception" );
		} catch ( UniformInterfaceException e ) {
			// Not found!
			Assert.assertEquals( 404, e.getResponse().getStatus() );
		}
	}


	@Test
	public void testResynchronize_success() throws ApplicationException {
		final Collection<Instance> rootInstances = this.app.getRootInstances();

		// Deploy & start everything.
		for(Instance i : rootInstances)
			i.setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Request an application resynchronization.
		this.client.getApplicationDelegate().resynchronize( this.app.getName() );

		// Check a MsgCmdResynchronize has been sent to each agent.
		final List<Message> sentMessages = this.msgClient.sentMessages;
		Assert.assertEquals( rootInstances.size(), sentMessages.size() );
		for( Message message : sentMessages )
			Assert.assertTrue( message instanceof MsgCmdResynchronize );

	}


	@Test
	public void testResynchronize_nonExistingApplication() {
		try {
			this.client.getApplicationDelegate().resynchronize( "I-am-not-an-app" );
			Assert.fail( "Expecting exception" );
		} catch ( UniformInterfaceException e ) {
			// Not found!
			Assert.assertEquals( 404, e.getResponse().getStatus() );
		}
	}


	private TestClientDm getInternalClient() throws IllegalAccessException {
		return TestUtils.getInternalField( this.manager.getMessagingClient(), "messagingClient", TestClientDm.class );
	}
}
