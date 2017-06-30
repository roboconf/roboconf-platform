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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.beans.ApplicationBindings;
import net.roboconf.dm.rest.commons.beans.ApplicationBindings.ApplicationBindingItem;
import net.roboconf.dm.rest.commons.beans.TargetAssociation;
import net.roboconf.dm.rest.services.internal.resources.IApplicationResource;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeInstanceState;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdResynchronize;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private IApplicationResource resource;
	private TestApplication app;
	private ManagedApplication ma;
	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private TestClient msgClient;


	@After
	public void after() {
		this.manager.stop();
	}


	@Before
	public void before() throws Exception {

		// Create the manager
		this.manager = new Manager();
		this.manager.setMessagingType( MessagingConstants.FACTORY_TEST );
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		// Create the wrapper and complete configuration
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Get the messaging client
		this.msgClient = (TestClient) this.managerWrapper.getInternalMessagingClient();
		this.msgClient.clearMessages();

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class).cancel();

		// Create our resource
		this.resource = new ApplicationResource( this.manager );

		// Load an application
		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());

		this.ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( this.ma );
		this.managerWrapper.getApplicationTemplates().put( this.app.getTemplate(), Boolean.TRUE );
	}


	@Test
	public void testChangeState_inexistingApplication() throws Exception {

		Response resp = this.resource.changeInstanceState( "inexisting", InstanceStatus.DEPLOYED_STARTED.toString(), null );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testChangeState_inexistingInstance_null() throws Exception {

		Response resp = this.resource.changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED.toString(), null );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testChangeState_inexistingInstance() throws Exception {

		Response resp = this.resource.changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED.toString(), "/bip/bip" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testChangeState_invalidAction() throws Exception {

		Response resp = this.resource.changeInstanceState( this.app.getName(), null, null );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testChangeState_invalidActionAgain() throws Exception {

		Response resp = this.resource.changeInstanceState( this.app.getName(), "oops", null );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testChangeState_deployRoot_success() throws Exception {

		TestTargetResolver iaasResolver = new TestTargetResolver();
		this.manager.setTargetResolver( iaasResolver );

		String targetId = this.manager.targetsMngr().createTarget( "id: tid\nhandler: test" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		Assert.assertEquals( 0, iaasResolver.instancePathToRunningStatus.size());
		Response resp = this.resource.changeInstanceState(
				this.app.getName(),
				InstanceStatus.DEPLOYED_STARTED.toString(),
				InstanceHelpers.computeInstancePath( this.app.getMySqlVm()));

		String path = InstanceHelpers.computeInstancePath( this.app.getMySqlVm());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 1, iaasResolver.instancePathToRunningStatus.size());
		Assert.assertTrue( iaasResolver.instancePathToRunningStatus.get( path ));
	}


	@Test
	public void testChangeState_deploy_success() throws Exception {

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STOPPED.toString(), instancePath );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
	}


	@Test
	public void testChangeState_TargetException() throws Exception {

		TestTargetResolver newResolver = new TestTargetResolver() {
			@Override
			public TargetHandler findTargetHandler( Map<String,String> targetProperties )
			throws TargetException {
				throw new TargetException( "For test purpose!" );
			}
		};

		this.manager.setTargetResolver( newResolver );
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getTomcatVm().getStatus());
		Response resp = this.resource.changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED.toString(), instancePath );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, this.app.getTomcatVm().getStatus());
	}


	@Test
	public void testChangeState_IOException() throws Exception {

		this.msgClient.connected.set( false );
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.changeInstanceState( this.app.getName(), InstanceStatus.DEPLOYED_STARTED.toString(), instancePath );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 0, messages.size());
	}


	@Test
	public void testSetDescription_ok() throws Exception {

		String newDesc = "a new description";
		Assert.assertFalse( newDesc.equals( this.app.getDescription()));
		this.app.setDirectory( this.folder.newFolder());

		Response resp = this.resource.setDescription( this.app.getName(), newDesc );
		Assert.assertEquals( newDesc, this.app.getDescription());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testSetDescription_appNotFound() throws Exception {
		Response resp = this.resource.setDescription( "error", "new description" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testSetDescription_ioException() throws Exception {

		String newDesc = "a new description";
		Assert.assertFalse( newDesc.equals( this.app.getDescription()));
		this.app.setDirectory( this.folder.newFile());

		Response resp = this.resource.setDescription( this.app.getName(), newDesc );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testStopAll() throws Exception {

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.stopAll( this.app.getName(), instancePath );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
	}


	@Test
	public void testStopAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.stopAll( "oops", instancePath );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testStopAll_invalidInstancePath() throws Exception {

		Response resp = this.resource.stopAll( this.app.getName(), "/invalid" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testStopAll_IOException() throws Exception {

		this.msgClient.connected.set( false );
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.stopAll( this.app.getName(), instancePath );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testUndeployAll() throws Exception {

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.undeployAll( this.app.getName(), instancePath );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( instancePath, ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());
	}


	@Test
	public void testUndeployAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.undeployAll( "oops", instancePath );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testUndeployAll_invalidInstancePath() throws Exception {

		Response resp = this.resource.undeployAll( this.app.getName(), "/invalid" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testUndeployAll_IOException() throws Exception {

		this.msgClient.connected.set( false );
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.undeployAll( this.app.getName(), instancePath );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testDeployAndStartAll() throws Exception {

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.deployAndStartAll( this.app.getName(), instancePath );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 2, messages.size());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 0 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getTomcat()), ((MsgCmdChangeInstanceState) messages.get( 0 )).getInstancePath());

		Assert.assertEquals( MsgCmdChangeInstanceState.class, messages.get( 1 ).getClass());
		Assert.assertEquals( InstanceHelpers.computeInstancePath( this.app.getWar()), ((MsgCmdChangeInstanceState) messages.get( 1 )).getInstancePath());
	}


	@Test
	public void testDeployAndStartAll_invalidApp() throws Exception {

		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.deployAndStartAll( "oops", instancePath );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testDeployandStartAll_invalidInstancePath() throws Exception {

		Response resp = this.resource.deployAndStartAll( this.app.getName(), "/invalid" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testDeployAndStartAll_IOException() throws Exception {

		this.msgClient.connected.set( false );
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Response resp = this.resource.deployAndStartAll( this.app.getName(), instancePath );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testListChildrenInstances() throws Exception {

		List<Instance> instances = this.resource.listChildrenInstances( this.app.getName(), "/bip/bip", false );
		Assert.assertEquals( 0, instances.size());

		instances = this.resource.listChildrenInstances( this.app.getName(), "/bip/bip", true );
		Assert.assertEquals( 0, instances.size());

		instances = this.resource.listChildrenInstances( "inexisting", "/bip/bip", false );
		Assert.assertEquals( 0, instances.size());

		instances = this.resource.listChildrenInstances( this.app.getName(), null, false );
		Assert.assertEquals( this.app.getRootInstances().size(), instances.size());

		instances = this.resource.listChildrenInstances( this.app.getName(), null, true );
		Assert.assertEquals( InstanceHelpers.getAllInstances( this.app ).size(), instances.size());

		instances = this.resource.listChildrenInstances( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), false );
		Assert.assertEquals( 1, instances.size());

		instances = this.resource.listChildrenInstances( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), true );
		Assert.assertEquals( 2, instances.size());
	}


	@Test
	public void testListComponents() throws Exception {

		List<Component> components = this.resource.listComponents( "inexisting" );
		Assert.assertEquals( 0, components.size());

		components = this.resource.listComponents( this.app.getName());
		Assert.assertEquals( ComponentHelpers.findAllComponents( this.app ).size(), components.size());
	}


	@Test
	public void testComponentChildren() throws Exception {

		List<Component> components = this.resource.findComponentChildren( "inexisting", "" );
		Assert.assertEquals( 0, components.size());

		components = this.resource.findComponentChildren( this.app.getName(), "inexisting-component" );
		Assert.assertEquals( 0, components.size());

		components = this.resource.findComponentChildren( this.app.getName(), null );
		Assert.assertEquals( 1, components.size());
		Assert.assertTrue( components.contains( this.app.getMySqlVm().getComponent()));

		components = this.resource.findComponentChildren( this.app.getName(), this.app.getMySqlVm().getComponent().getName());
		Assert.assertEquals( 2, components.size());
		Assert.assertTrue( components.contains( this.app.getMySql().getComponent()));
		Assert.assertTrue( components.contains( this.app.getTomcat().getComponent()));
	}


	@Test
	public void testComponentAncestors() throws Exception {

		List<Component> components = this.resource.findComponentAncestors( "inexisting", "my-comp" );
		Assert.assertEquals( 0, components.size());

		components = this.resource.findComponentAncestors( this.app.getName(), "my-comp" );
		Assert.assertEquals( 0, components.size());

		components = this.resource.findComponentAncestors( this.app.getName(), this.app.getTomcat().getComponent().getName());
		Assert.assertEquals( 1, components.size());
		Assert.assertTrue( components.contains( this.app.getMySqlVm().getComponent()));
	}


	@Test
	public void testAddInstance_root_success() throws Exception {

		Assert.assertEquals( 2, this.app.getRootInstances().size());
		Instance newInstance = new Instance( "vm-mail" ).component( this.app.getMySqlVm().getComponent());
		Response resp = this.resource.addInstance( this.app.getName(), null, newInstance );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 3, this.app.getRootInstances().size());
	}


	@Test
	public void testAddInstance_root_byComponentName_success() throws Exception {

		Assert.assertEquals( 2, this.app.getRootInstances().size());
		String componentName = this.app.getMySqlVm().getComponent().getName();
		Instance newInstance = new Instance( "vm-mail" ).component( new Component( componentName ));
		Response resp = this.resource.addInstance( this.app.getName(), null, newInstance );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		Collection<Instance> rootInstances = this.app.getRootInstances();
		Assert.assertEquals( 3, rootInstances.size());
		for( Instance rootInstance : rootInstances ) {
			Assert.assertEquals( rootInstance.getName(), Constants.TARGET_INSTALLER, rootInstance.getComponent().getInstallerName());
		}
	}


	@Test
	public void testAddInstance_IOException() throws Exception {

		this.msgClient.connected.set( false );

		Assert.assertEquals( 2, this.app.getRootInstances().size());
		Instance newInstance = new Instance( "vm-mail" ).component( this.app.getMySqlVm().getComponent());
		Response resp = this.resource.addInstance( this.app.getName(), null, newInstance );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 2, this.app.getRootInstances().size());
	}


	@Test
	public void testAddInstance_root_nullComponent() throws Exception {

		Assert.assertEquals( 2, this.app.getRootInstances().size());
		Instance existingInstance = new Instance( this.app.getMySqlVm().getName());
		Response resp = this.resource.addInstance( this.app.getName(), null, existingInstance );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testAddInstance_root_invalidComponent() throws Exception {

		Assert.assertEquals( 2, this.app.getRootInstances().size());
		Instance existingInstance = new Instance( this.app.getMySqlVm().getName()).component( new Component( "invalid" ));
		Response resp = this.resource.addInstance( this.app.getName(), null, existingInstance );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testAddInstance_root_duplicate() throws Exception {

		Assert.assertEquals( 2, this.app.getRootInstances().size());
		Instance existingInstance = new Instance( this.app.getMySqlVm().getName()).component( this.app.getMySqlVm().getComponent());
		Response resp = this.resource.addInstance( this.app.getName(), null, existingInstance );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testAddInstance_child_success() throws Exception {

		Instance newMysql = new Instance( "mysql-2" ).component( this.app.getMySql().getComponent());

		Assert.assertEquals( 1, this.app.getTomcatVm().getChildren().size());
		Assert.assertFalse( this.app.getTomcatVm().getChildren().contains( newMysql ));

		Response resp = this.resource.addInstance( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newMysql );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 2, this.app.getTomcatVm().getChildren().size());

		List<String> paths = new ArrayList<> ();
		for( Instance inst : this.app.getTomcatVm().getChildren())
			paths.add( InstanceHelpers.computeInstancePath( inst ));

		String rootPath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		Assert.assertTrue( paths.contains( rootPath + "/" + newMysql.getName()));
		Assert.assertTrue( paths.contains( rootPath + "/" + this.app.getTomcat().getName()));
	}


	@Test
	public void testAddInstance_child_byComponentName_success() throws Exception {

		String componentName = this.app.getMySql().getComponent().getName();
		Instance newMysql = new Instance( "mysql-2" ).component( new Component( componentName ));

		Assert.assertEquals( 1, this.app.getTomcatVm().getChildren().size());
		Assert.assertFalse( this.app.getTomcatVm().getChildren().contains( newMysql ));

		Response resp = this.resource.addInstance( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newMysql );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 2, this.app.getTomcatVm().getChildren().size());

		List<String> paths = new ArrayList<> ();
		for( Instance inst : this.app.getTomcatVm().getChildren()) {
			Assert.assertEquals( inst.getName(), "puppet", inst.getComponent().getInstallerName());
			paths.add( InstanceHelpers.computeInstancePath( inst ));
		}

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

		Response resp = this.resource.addInstance( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newMysql );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 2, this.app.getTomcatVm().getChildren().size());

		List<String> paths = new ArrayList<> ();
		for( Instance inst : this.app.getTomcatVm().getChildren())
			paths.add( InstanceHelpers.computeInstancePath( inst ));

		String rootPath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		Assert.assertTrue( paths.contains( rootPath + "/" + newMysql.getName()));
		Assert.assertTrue( paths.contains( rootPath + "/" + this.app.getTomcat().getName()));
	}


	@Test
	public void testAddInstance_child_failure() throws Exception {

		// We cannot deploy a WAR directly on a VM!
		// At least, this what the graph says.
		Instance newWar = new Instance( "war-2" ).component( this.app.getWar().getComponent());

		Assert.assertEquals( 1, this.app.getTomcatVm().getChildren().size());
		Assert.assertFalse( this.app.getTomcatVm().getChildren().contains( newWar ));
		Response resp = this.resource.addInstance( this.app.getName(), InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newWar );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testAddInstance_inexstingApplication() throws Exception {

		Instance newMysql = new Instance( "mysql-2" ).component( this.app.getMySql().getComponent());
		Response resp = this.resource.addInstance( "inexisting", InstanceHelpers.computeInstancePath( this.app.getTomcatVm()), newMysql );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testAddInstance_inexstingParentInstance() throws Exception {

		Instance newMysql = new Instance( "mysql-2" ).component( this.app.getMySql().getComponent());
		Response resp = this.resource.addInstance( "inexisting", "/bip/bip", newMysql );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testFindTargetAssociations_noTarget_noAssociation() throws Exception {

		// Setup
		List<Instance> scopedInstances = InstanceHelpers.findAllScopedInstances( this.app );
		Assert.assertEquals( 2, scopedInstances.size());

		List<Component> scopedComponents = new ArrayList<> ();
		for( Instance inst : scopedInstances ) {
			if( ! scopedComponents.contains( inst.getComponent()))
				scopedComponents.add( inst.getComponent());
		}

		List<TargetAssociation> associations = this.resource.findTargetAssociations( this.app.getName());
		Assert.assertEquals( scopedInstances.size() + scopedComponents.size() + 1, associations.size());

		// Verify target associations
		for( int i=0; i<scopedInstances.size(); i++ ) {
			TargetAssociation ta = associations.get( i + 1 );
			String path = InstanceHelpers.computeInstancePath( scopedInstances.get( i ));
			Assert.assertEquals( scopedInstances.get( i ).getComponent().getName(), ta.getInstanceComponent());

			Assert.assertEquals( path, ta.getInstancePathOrComponentName());
			Assert.assertNull( ta.getTargetDescriptor());
		}

		for( int i=0; i<scopedComponents.size(); i++ ) {
			TargetAssociation ta = associations.get( i + 1 + scopedInstances.size());
			Assert.assertEquals( "@" + scopedComponents.get( i ).getName(), ta.getInstancePathOrComponentName());
			Assert.assertNull( ta.getTargetDescriptor());
		}

		TargetAssociation ta = associations.get( 0 );
		Assert.assertEquals( "", ta.getInstancePathOrComponentName());
		Assert.assertNull( ta.getTargetDescriptor());
	}


	@Test
	public void testFindTargetAssociations_withTarget_noAssociation() throws Exception {

		// Setup
		List<Instance> scopedInstances = InstanceHelpers.findAllScopedInstances( this.app );
		Assert.assertEquals( 2, scopedInstances.size());

		List<Component> scopedComponents = new ArrayList<> ();
		for( Instance inst : scopedInstances ) {
			if( ! scopedComponents.contains( inst.getComponent()))
				scopedComponents.add( inst.getComponent());
		}

		// Create a target but no association
		this.manager.targetsMngr().createTarget( "id: tid\nhandler: h" );
		List<TargetAssociation> associations = this.resource.findTargetAssociations( this.app.getName());
		Assert.assertEquals( scopedInstances.size() + scopedComponents.size() + 1, associations.size());

		for( int i=0; i<scopedInstances.size(); i++ ) {
			TargetAssociation ta = associations.get( i + 1 );
			String path = InstanceHelpers.computeInstancePath( scopedInstances.get( i ));
			Assert.assertEquals( scopedInstances.get( i ).getComponent().getName(), ta.getInstanceComponent());

			Assert.assertEquals( path, ta.getInstancePathOrComponentName());
			Assert.assertNull( ta.getTargetDescriptor());
		}

		for( int i=0; i<scopedComponents.size(); i++ ) {
			TargetAssociation ta = associations.get( i + 1 + scopedInstances.size());
			Assert.assertEquals( "@" + scopedComponents.get( i ).getName(), ta.getInstancePathOrComponentName());
			Assert.assertNull( ta.getTargetDescriptor());
		}

		TargetAssociation ta = associations.get( 0 );
		Assert.assertEquals( "", ta.getInstancePathOrComponentName());
		Assert.assertNull( ta.getTargetDescriptor());
	}


	@Test
	public void testFindTargetAssociations_withTarget_defaultAssociation() throws Exception {

		// Setup
		List<Instance> scopedInstances = InstanceHelpers.findAllScopedInstances( this.app );
		Assert.assertEquals( 2, scopedInstances.size());

		List<Component> scopedComponents = new ArrayList<> ();
		for( Instance inst : scopedInstances ) {
			if( ! scopedComponents.contains( inst.getComponent()))
				scopedComponents.add( inst.getComponent());
		}

		// Create a default target for the application
		String targetId = this.manager.targetsMngr().createTarget( "id: tid\nhandler: h" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );
		List<TargetAssociation> associations = this.resource.findTargetAssociations( this.app.getName());
		Assert.assertEquals( scopedInstances.size() + scopedComponents.size() + 1, associations.size());

		for( int i=0; i<scopedInstances.size(); i++ ) {
			TargetAssociation ta = associations.get( i + 1 );
			String path = InstanceHelpers.computeInstancePath( scopedInstances.get( i ));
			Assert.assertEquals( scopedInstances.get( i ).getComponent().getName(), ta.getInstanceComponent());

			Assert.assertEquals( path, ta.getInstancePathOrComponentName());
			Assert.assertNull( ta.getTargetDescriptor());
		}

		for( int i=0; i<scopedComponents.size(); i++ ) {
			TargetAssociation ta = associations.get( i + 1 + scopedInstances.size());
			Assert.assertEquals( "@" + scopedComponents.get( i ).getName(), ta.getInstancePathOrComponentName());
			Assert.assertNull( ta.getTargetDescriptor());
		}

		TargetAssociation ta = associations.get( 0 );
		Assert.assertEquals( "", ta.getInstancePathOrComponentName());
		Assert.assertEquals( targetId, ta.getTargetDescriptor().getId());
	}


	@Test
	public void testFindTargetAssociations_withTarget_withAssociations() throws Exception {

		// Setup
		List<Instance> scopedInstances = InstanceHelpers.findAllScopedInstances( this.app );
		Assert.assertEquals( 2, scopedInstances.size());

		List<Component> scopedComponents = new ArrayList<> ();
		for( Instance inst : scopedInstances ) {
			if( ! scopedComponents.contains( inst.getComponent()))
				scopedComponents.add( inst.getComponent());
		}

		// Default target
		String targetId = this.manager.targetsMngr().createTarget( "id: tid\nhandler: h" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.app, null );

		// Add a custom target for a given instance
		String newTargetId = this.manager.targetsMngr().createTarget( "id: new-tid\nhandler: h" );
		String instancePath = InstanceHelpers.computeInstancePath( this.app.getTomcatVm());
		this.manager.targetsMngr().associateTargetWith( newTargetId, this.app, instancePath );

		List<TargetAssociation> associations = this.resource.findTargetAssociations( this.app.getName());
		Assert.assertEquals( scopedInstances.size() + scopedComponents.size() + 1, associations.size());

		boolean foundCustomInstance = false;
		for( int i=0; i<scopedInstances.size(); i++ ) {
			TargetAssociation ta = associations.get( i + 1 );
			String path = InstanceHelpers.computeInstancePath( scopedInstances.get( i ));
			Assert.assertEquals( path, ta.getInstancePathOrComponentName());
			Assert.assertEquals( scopedInstances.get( i ).getComponent().getName(), ta.getInstanceComponent());

			if( instancePath.equals( path )) {
				Assert.assertEquals( newTargetId, ta.getTargetDescriptor().getId());
				foundCustomInstance = true;
			} else {
				Assert.assertNull( ta.getTargetDescriptor());
			}
		}

		for( int i=0; i<scopedComponents.size(); i++ ) {
			TargetAssociation ta = associations.get( i + 1 + scopedInstances.size());
			Assert.assertEquals( "@" + scopedComponents.get( i ).getName(), ta.getInstancePathOrComponentName());
			Assert.assertNull( ta.getTargetDescriptor());
		}

		Assert.assertTrue( foundCustomInstance );
		TargetAssociation ta = associations.get( 0 );
		Assert.assertEquals( "", ta.getInstancePathOrComponentName());
		Assert.assertEquals( targetId, ta.getTargetDescriptor().getId());

		// Change the default target
		this.manager.targetsMngr().associateTargetWith( newTargetId, this.app, null );
		associations = this.resource.findTargetAssociations( this.app.getName());

		foundCustomInstance = false;
		for( int i=0; i<scopedInstances.size(); i++ ) {
			ta = associations.get( i + 1 );
			String path = InstanceHelpers.computeInstancePath( scopedInstances.get( i ));
			Assert.assertEquals( path, ta.getInstancePathOrComponentName());

			if( instancePath.equals( path )) {
				Assert.assertEquals( newTargetId, ta.getTargetDescriptor().getId());
				foundCustomInstance = true;
			} else {
				Assert.assertNull( ta.getTargetDescriptor());
			}
		}

		for( int i=0; i<scopedComponents.size(); i++ ) {
			ta = associations.get( i + 1 + scopedInstances.size());
			Assert.assertEquals( "@" + scopedComponents.get( i ).getName(), ta.getInstancePathOrComponentName());
			Assert.assertNull( ta.getTargetDescriptor());
		}

		Assert.assertTrue( foundCustomInstance );
		ta = associations.get( 0 );
		Assert.assertEquals( "", ta.getInstancePathOrComponentName());
		Assert.assertEquals( newTargetId, ta.getTargetDescriptor().getId());
	}


	@Test
	public void testRemoveInstance_success() {

		// Check the Tomcat instance is here.
		final String tomcatPath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, tomcatPath ));

		// Delete the Tomcat instance.
		Response resp = this.resource.removeInstance( this.app.getName(), tomcatPath );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		// Check it is gone.
		Assert.assertNull( InstanceHelpers.findInstanceByPath( this.app, tomcatPath ));
	}


	@Test
	public void testRemoveInstance_stillRunning() {

		this.app.getTomcat().setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Check the Tomcat instance is here.
		final String tomcatPath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, tomcatPath ));

		// Delete the Tomcat instance.
		Response resp = this.resource.removeInstance( this.app.getName(), tomcatPath );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());

		// Check it is NOT gone.
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, tomcatPath ));
	}


	@Test
	public void testRemoveInstance_IOException() {

		this.msgClient.connected.set( false );

		// Check the Tomcat instance is here.
		final String tomcatPath = InstanceHelpers.computeInstancePath( this.app.getTomcat());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, tomcatPath ));

		// Delete the Tomcat instance.
		Response resp = this.resource.removeInstance( this.app.getName(), tomcatPath );
		Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), resp.getStatus());

		// Check it is NOT gone.
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, tomcatPath ));
	}


	@Test
	public void testRemoveInstance_nonExistingInstance() {
		Response resp = this.resource.removeInstance( this.app.getName(), "/I-do-not-exist" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testRemoveInstance_nonExistingApplication() {
		Response resp = this.resource.removeInstance( "I-am-not-an-app", InstanceHelpers.computeInstancePath( this.app.getTomcat()));
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testResynchronize_success() throws Exception {
		final Collection<Instance> rootInstances = this.app.getRootInstances();

		// Deploy & start everything.
		for(Instance i : rootInstances)
			i.setStatus( InstanceStatus.DEPLOYED_STARTED );

		// Request an application resynchronization.
		Response resp = this.resource.resynchronize( this.app.getName());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		// Check a MsgCmdResynchronize has been sent to each agent.
		final List<Message> sentMessages = this.msgClient.allSentMessages;
		Assert.assertEquals( rootInstances.size(), sentMessages.size());
		for( Message message : sentMessages )
			Assert.assertTrue( message instanceof MsgCmdResynchronize );
	}


	@Test
	public void testResynchronize_IOException() throws Exception {

		this.msgClient.connected.set( false );
		Response resp = this.resource.resynchronize( this.app.getName());
		Assert.assertEquals( Status.NOT_ACCEPTABLE.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testResynchronize_nonExistingApplication() {

		Response resp = this.resource.resynchronize( "I-am-not-an-app" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testBindApplication_inexistingApplication() throws Exception {

		Response resp = this.resource.bindApplication( "invalid", this.ma.getApplication().getTemplate().getName(), this.ma.getName());
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testBindApplication_invalidBoundApplication() throws Exception {

		Response resp = this.resource.bindApplication( this.ma.getName(), this.ma.getApplication().getTemplate().getName(), "invalid" );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testUnbindApplication_inexistingApplication() throws Exception {

		Response resp = this.resource.unbindApplication( "invalid", this.ma.getApplication().getTemplate().getName(), this.ma.getName());
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testBindApplication_invalidBoundTemplate() throws Exception {

		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.getTemplate().setName( "tpl-other" );
		app2.setName( "app-other" );

		this.managerWrapper.addManagedApplication( new ManagedApplication( app2 ));

		// ma and app2 do not have the same template name
		Response resp = this.resource.bindApplication( this.ma.getName(), this.ma.getApplication().getTemplate().getName(), app2.getName());
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}


	@Test
	public void testUnbindApplication_invalidBoundTemplate() throws Exception {

		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.getTemplate().setName( "tpl-other" );
		app2.setName( "app-other" );

		this.managerWrapper.addManagedApplication( new ManagedApplication( app2 ));

		// ma and app2 do not have the same template name
		Response resp = this.resource.unbindApplication( this.ma.getName(), this.ma.getApplication().getTemplate().getName(), app2.getName());
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}


	@Test
	public void testBindAndUnbindApplication_success() throws Exception {

		// Create a second application with a different template
		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.getTemplate().setName( "tpl-other" );
		app2.getTemplate().setExternalExportsPrefix( "eep" );
		app2.setName( "app-other" );

		this.managerWrapper.addManagedApplication( new ManagedApplication( app2 ));

		// Bind and check
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getMySqlVm()).size());

		Response resp = this.resource.bindApplication( this.ma.getName(), app2.getTemplate().getExternalExportsPrefix(), app2.getName());

		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		messages.addAll( this.ma.removeAwaitingMessages( this.app.getMySqlVm()));
		Assert.assertEquals( 2, messages.size());

		for( Message m : this.msgClient.allSentMessages ) {
			Assert.assertEquals( MsgCmdChangeBinding.class, m.getClass());

			MsgCmdChangeBinding msg = (MsgCmdChangeBinding) m;
			Assert.assertEquals( app2.getTemplate().getExternalExportsPrefix(), msg.getExternalExportsPrefix());
			Assert.assertNotNull( msg.getAppNames());
			Assert.assertEquals( 1, msg.getAppNames().size());
			Assert.assertTrue( msg.getAppNames().contains( app2.getName()));
		}

		// Unbind
		resp = this.resource.unbindApplication( this.ma.getName(), app2.getTemplate().getExternalExportsPrefix(), app2.getName());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		messages.addAll( this.ma.removeAwaitingMessages( this.app.getMySqlVm()));
		Assert.assertEquals( 2, messages.size());

		for( Message m : this.msgClient.allSentMessages ) {
			Assert.assertEquals( MsgCmdChangeBinding.class, m.getClass());

			MsgCmdChangeBinding msg = (MsgCmdChangeBinding) m;
			Assert.assertEquals( app2.getTemplate().getExternalExportsPrefix(), msg.getExternalExportsPrefix());
			Assert.assertNull( msg.getAppNames());
		}
	}


	@Test
	public void testReplaceApplicationBindings_success() throws Exception {

		// Create a second application with a different template
		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.getTemplate().setName( "tpl-other" );
		app2.getTemplate().setExternalExportsPrefix( "eep" );
		app2.setName( "app-other" );

		this.managerWrapper.addManagedApplication( new ManagedApplication( app2 ));

		// Bind and check
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getTomcatVm()).size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( this.app.getMySqlVm()).size());

		Response resp = this.resource.replaceApplicationBindings(
				this.ma.getName(),
				app2.getTemplate().getExternalExportsPrefix(),
				Arrays.asList( app2.getName()));

		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		List<Message> messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		messages.addAll( this.ma.removeAwaitingMessages( this.app.getMySqlVm()));
		Assert.assertEquals( 2, messages.size());

		for( Message m : this.msgClient.allSentMessages ) {
			Assert.assertEquals( MsgCmdChangeBinding.class, m.getClass());

			MsgCmdChangeBinding msg = (MsgCmdChangeBinding) m;
			Assert.assertEquals( app2.getTemplate().getExternalExportsPrefix(), msg.getExternalExportsPrefix());
			Assert.assertNotNull( msg.getAppNames());
			Assert.assertEquals( 1, msg.getAppNames().size());
			Assert.assertTrue( msg.getAppNames().contains( app2.getName()));
		}

		// Unbind
		resp = this.resource.replaceApplicationBindings(
				this.ma.getName(),
				app2.getTemplate().getExternalExportsPrefix(),
				new ArrayList<String>( 0 ));

		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 1, messages.size());
		messages.addAll( this.ma.removeAwaitingMessages( this.app.getMySqlVm()));
		Assert.assertEquals( 2, messages.size());

		for( Message m : this.msgClient.allSentMessages ) {
			Assert.assertEquals( MsgCmdChangeBinding.class, m.getClass());

			MsgCmdChangeBinding msg = (MsgCmdChangeBinding) m;
			Assert.assertEquals( app2.getTemplate().getExternalExportsPrefix(), msg.getExternalExportsPrefix());
			Assert.assertNull( msg.getAppNames());
		}

		// Unbind with something null
		resp = this.resource.replaceApplicationBindings(
				this.ma.getName(),
				app2.getTemplate().getExternalExportsPrefix(),
				null );

		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());

		messages = this.ma.removeAwaitingMessages( this.app.getTomcatVm());
		Assert.assertEquals( 0, messages.size());
		messages.addAll( this.ma.removeAwaitingMessages( this.app.getMySqlVm()));
		Assert.assertEquals( 0, messages.size());
	}


	@Test
	public void testReplaceApplicationBindings_inexistingApplication() throws Exception {

		Response resp = this.resource.replaceApplicationBindings(
				"invalid",
				this.ma.getApplication().getTemplate().getName(),
				Arrays.asList( this.ma.getName()));

		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testReplaceApplicationBindings_invalidBoundTemplate() throws Exception {

		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.getTemplate().setName( "tpl-other" );
		app2.setName( "app-other" );

		this.managerWrapper.addManagedApplication( new ManagedApplication( app2 ));

		// ma and app2 do not have the same template name
		Response resp = this.resource.replaceApplicationBindings(
				this.ma.getName(),
				this.ma.getApplication().getTemplate().getName(),
				Arrays.asList( app2.getName()));

		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}


	@Test
	public void testGetApplicationBindings_inexistingApplication() throws Exception {

		Response resp = this.resource.getApplicationBindings( "inexisting" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testGetApplicationBindings_success_noExternalImports() throws Exception {

		this.app.bindWithApplication( "some", "value" );
		this.app.bindWithApplication( "another", "value" );

		Response resp = this.resource.getApplicationBindings( this.app.getName());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		ApplicationBindings bindings = (ApplicationBindings) resp.getEntity();
		Assert.assertEquals( 0, bindings.prefixToItems.size());
	}


	@Test
	public void testGetApplicationBindings_success_withExternalImports() throws Exception {

		// Add external imports
		ImportedVariable newVar = new ImportedVariable( "prefix1.ip", false, true );
		this.app.getWar().getComponent().importedVariables.put( newVar.getName(), newVar );

		newVar = new ImportedVariable( "prefix2.something", false, true );
		this.app.getWar().getComponent().importedVariables.put( newVar.getName(), newVar );

		newVar = new ImportedVariable( "prefix3.else", false, true );
		this.app.getWar().getComponent().importedVariables.put( newVar.getName(), newVar );

		// Create some fake applications
		Map<String,String> nameToExternalExportPrefix = new LinkedHashMap<>( 4 );
		nameToExternalExportPrefix.put( "app1", "prefix1" );
		nameToExternalExportPrefix.put( "this_app", "prefix2" );
		nameToExternalExportPrefix.put( "app2", "prefix1" );
		nameToExternalExportPrefix.put( "app", null );

		for( Map.Entry<String,String> entry : nameToExternalExportPrefix.entrySet()) {

			// Register
			TestApplication ta = new TestApplication();
			ta.setDirectory( this.folder.newFolder());
			ta.setName( entry.getKey());
			ta.getTemplate().setExternalExportsPrefix( entry.getValue());
			this.managerWrapper.addManagedApplication( new ManagedApplication( ta ));

			// Bind
			if( entry.getValue() != null )
				this.app.bindWithApplication( entry.getValue(), ta.getName());
		}

		// Add an invalid binding.
		// It should not be listed farther.
		this.app.bindWithApplication( "invalid", "app" );

		// Verify the bindings
		Response resp = this.resource.getApplicationBindings( this.app.getName());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		ApplicationBindings bindings = (ApplicationBindings) resp.getEntity();
		Assert.assertEquals( 3, bindings.prefixToItems.size());

		List<ApplicationBindingItem> items = bindings.prefixToItems.get( "prefix1" );
		Assert.assertEquals( 2, items.size());
		Assert.assertTrue( items.get( 0 ).isBound());
		Assert.assertTrue( items.get( 1 ).isBound());

		List<String> boundAppNames = new ArrayList<> ();
		boundAppNames.add( items.get( 0 ).getApplicationName());
		boundAppNames.add( items.get( 1 ).getApplicationName());
		Collections.sort( boundAppNames );
		Assert.assertEquals( Arrays.asList( "app1", "app2" ), boundAppNames );

		items = bindings.prefixToItems.get( "prefix2" );
		Assert.assertEquals( 1, items.size());
		Assert.assertTrue( items.get( 0 ).isBound());
		Assert.assertEquals( "this_app", items.get( 0 ).getApplicationName());

		items = bindings.prefixToItems.get( "prefix3" );
		Assert.assertEquals( 0, items.size());

		// Unbind
		Assert.assertTrue( this.app.unbindFromApplication( "prefix1", "app2" ));
		resp = this.resource.getApplicationBindings( this.app.getName());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		bindings = (ApplicationBindings) resp.getEntity();
		Assert.assertEquals( 3, bindings.prefixToItems.size());

		items = bindings.prefixToItems.get( "prefix1" );
		Assert.assertEquals( 2, items.size());

		if( "app1".equals( items.get( 1 ).getApplicationName())) {
			ApplicationBindingItem itemToMove = items.remove( 1 );
			items.add( 0, itemToMove );
		}

		Assert.assertEquals( "app1", items.get( 0 ).getApplicationName());
		Assert.assertTrue( items.get( 0 ).isBound());
		Assert.assertEquals( "app2", items.get( 1 ).getApplicationName());
		Assert.assertFalse( items.get( 1 ).isBound());

		items = bindings.prefixToItems.get( "prefix2" );
		Assert.assertEquals( 1, items.size());
		Assert.assertTrue( items.get( 0 ).isBound());
		Assert.assertEquals( "this_app", items.get( 0 ).getApplicationName());

		items = bindings.prefixToItems.get( "prefix3" );
		Assert.assertEquals( 0, items.size());
	}


	@Test
	public void testCommands() throws IOException {

		// Inexisting application
		Application inexisting = new Application( "inexisting", this.app.getTemplate());
		Assert.assertEquals( 0, this.resource.listCommands( inexisting.getName()).size());

		// With a real application
		Response resp = this.resource.getCommandInstructions(this.app.getName(), "");
		Assert.assertEquals( Status.NO_CONTENT.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.resource.listCommands( this.app.getName()).size());

		// Add a command
		this.manager.commandsMngr().createOrUpdateCommand( this.app, "toto", "this is a command" );
		resp = this.resource.getCommandInstructions(this.app.getName(), "toto");
		Assert.assertEquals( "this is a command", resp.getEntity());

		List<String> commandsList = this.resource.listCommands( this.app.getName());
		Assert.assertEquals( 1, commandsList.size());
		Assert.assertEquals( "toto", commandsList.get( 0 ));

		// Update a command
		this.manager.commandsMngr().createOrUpdateCommand( this.app, "toto", "Good command" );
		resp = this.resource.getCommandInstructions(this.app.getName(), "toto");
		Assert.assertEquals( "Good command",resp.getEntity());

		// Delete it
		this.manager.commandsMngr().deleteCommand( this.app, "toto");
		resp = this.resource.getCommandInstructions( this.app.getName(), "toto" );
		Assert.assertEquals( Status.NO_CONTENT.getStatusCode(), resp.getStatus());
		Assert.assertEquals( null, resp.getEntity());
		Assert.assertEquals( 0, this.resource.listCommands( this.app.getName()).size());

		// Inexisting application
		resp = this.resource.getCommandInstructions( "inexisting-app", "toto" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testExecuteCommand_inexistingApplication() throws Exception {

		Response resp = this.resource.executeCommand( "inexisting", "cmd" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testExecuteCommand_inexistingCommand() throws Exception {

		Response resp = this.resource.executeCommand( this.app.getName(), "cmd" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testExecuteCommand_executionError() throws Exception {

		this.manager.commandsMngr().createOrUpdateCommand( this.app, "toto", "Good command");
		Response resp = this.resource.executeCommand( this.app.getName(), "toto" );
		Assert.assertEquals( Status.CONFLICT.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testExecuteCommand_success() throws Exception {

		File f = this.folder.newFile();
		Assert.assertTrue( f.delete());

		this.manager.commandsMngr().createOrUpdateCommand( this.app, "toto", "Write this into " + f.getAbsolutePath());
		Assert.assertFalse( f.exists());

		Response resp = this.resource.executeCommand( this.app.getName(), "toto" );
		Assert.assertTrue( f.exists());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testReplaceTags_success() throws Exception {

		Set<String> tags = new HashSet<> ();
		tags.add( "toto" );
		tags.add( "titi" );
		Response resp = this.resource.replaceTags(
				this.app.getTemplate().getName(),
				this.app.getTemplate().getVersion(),
				new ArrayList<>( tags ));

		Assert.assertEquals( tags, this.app.getTemplate().getTags());
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testReplaceTags_notFound() throws Exception {

		Set<String> tags = new HashSet<> ();
		tags.add( "toto" );
		tags.add( "titi" );
		Response resp = this.resource.replaceTags(
				"ao",
				"9.0",
				new ArrayList<>( tags ));

		Assert.assertEquals( 0, this.app.getTemplate().getTags().size());
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
	}
}
