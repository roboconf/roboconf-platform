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

import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.internal.client.test.TestClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;


/**
 * @author Vincent Zurczak - Linagora
 */
public class DebugResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private TestClient msgClient;
	private DebugResource resource;
	private TestApplication app;


	@Before
	public void initializeDm() throws Exception {

		// Create the manager
		this.manager = new Manager();
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
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

		// Register the REST resource
		this.resource = new DebugResource( this.manager );

		// Create an application all the tests can use
		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());
	}


	@After
	public void stopDm() {
		if( this.manager != null )
			this.manager.stop();
	}


	@Test
	public void testDiagnoseApplication() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		List<Diagnostic> diags = this.resource.diagnoseApplication( "inexisting" );
		Assert.assertEquals( 0, diags.size());

		diags = this.resource.diagnoseApplication( this.app.getName());
		Assert.assertEquals( InstanceHelpers.getAllInstances( this.app ).size(), diags.size());

		for( Diagnostic diag : diags ) {
			Instance inst = InstanceHelpers.findInstanceByPath( this.app, diag.getInstancePath());
			Assert.assertNotNull( inst );

			for( DependencyInformation info : diag.getDependenciesInformation()) {
				Assert.assertFalse( info.isResolved());
			}
		}
	}


	@Test
	public void testDiagnoseInstance() throws Exception {

		String path = InstanceHelpers.computeInstancePath( this.app.getWar());
		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Response resp = this.resource.diagnoseInstance( "inexisting", path );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());

		resp = this.resource.diagnoseInstance( this.app.getName(), "/inexisting" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());

		resp = this.resource.diagnoseInstance( this.app.getName(), path );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		Diagnostic diag = (Diagnostic) resp.getEntity();
		Assert.assertNotNull( diag );
		Assert.assertEquals( path, diag.getInstancePath());
		Assert.assertEquals( 1, diag.getDependenciesInformation().size());
	}


	@Test
	public void testCheckMessagingConnectionForTheDm_success() {

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		UUID uuid = UUID.randomUUID();
		Response resp = this.resource.checkMessagingConnectionForTheDm( uuid.toString());

		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 1, this.msgClient.allSentMessages.size());

		Message msg = this.msgClient.allSentMessages.get( 0 );
		Assert.assertEquals( MsgEcho.class, msg.getClass());
		Assert.assertEquals( uuid.toString(), ((MsgEcho) msg).getContent());
	}


	@Test
	public void testCheckMessagingConnectionForTheDm_ioException() {

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		this.msgClient.failMessageSending.set( true );

		Response resp = this.resource.checkMessagingConnectionForTheDm( UUID.randomUUID().toString());

		Assert.assertEquals( Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}


	@Test
	public void testCheckMessagingConnectionWithAgent_success() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		UUID uuid = UUID.randomUUID();
		String path = "/" + this.app.getMySqlVm().getName();

		InstanceStatus[] statuses = new InstanceStatus[] {
				InstanceStatus.DEPLOYED_STARTED,
				InstanceStatus.DEPLOYED_STOPPED,
				InstanceStatus.PROBLEM,
				InstanceStatus.DEPLOYING
		};

		for( InstanceStatus status : statuses ) {
			this.app.getMySqlVm().setStatus( status );
			Response resp = this.resource.checkMessagingConnectionWithAgent( this.app.getName(), path, uuid.toString());

			Assert.assertEquals( status.toString(), Status.OK.getStatusCode(), resp.getStatus());
			Assert.assertEquals( status.toString(), 1, this.msgClient.allSentMessages.size());

			Message msg = this.msgClient.allSentMessages.get( 0 );
			Assert.assertEquals( status.toString(), MsgEcho.class, msg.getClass());
			Assert.assertEquals( status.toString(), "PING:" + uuid.toString(), ((MsgEcho) msg).getContent());
			this.msgClient.allSentMessages.clear();
		}
	}


	@Test
	public void testCheckMessagingConnectionWithAgent_agentNotStarted() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		UUID uuid = UUID.randomUUID();
		String path = "/" + this.app.getMySqlVm().getName();

		Response resp = this.resource.checkMessagingConnectionWithAgent( this.app.getName(), path, uuid.toString());
		Assert.assertEquals( Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}


	@Test
	public void testCheckMessagingConnectionWithAgent_noMessaging() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		this.msgClient.failMessageSending.set( true );
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		UUID uuid = UUID.randomUUID();
		String path = "/" + this.app.getMySqlVm().getName();

		Response resp = this.resource.checkMessagingConnectionWithAgent( this.app.getName(), path, uuid.toString());
		Assert.assertEquals( Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}


	@Test
	public void testCheckMessagingConnectionWithAgent_invalidApplication() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		UUID uuid = UUID.randomUUID();
		String path = "/" + this.app.getMySqlVm().getName();

		Response resp = this.resource.checkMessagingConnectionWithAgent( "whatever", path, uuid.toString());
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}


	@Test
	public void testCheckMessagingConnectionWithAgent_invalidInstance() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
		UUID uuid = UUID.randomUUID();

		Response resp = this.resource.checkMessagingConnectionWithAgent( this.app.getName(), "oops", uuid.toString());
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, this.msgClient.allSentMessages.size());
	}
}
