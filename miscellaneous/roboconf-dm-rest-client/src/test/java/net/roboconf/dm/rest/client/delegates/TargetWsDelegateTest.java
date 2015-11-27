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

import java.io.IOException;
import java.net.URI;
import java.util.Timer;

import javax.ws.rs.core.UriBuilder;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.TargetWsException;
import net.roboconf.dm.rest.services.internal.RestApplication;
import net.roboconf.messaging.api.MessagingConstants;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetWsDelegateTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private static final String REST_URI = "http://localhost:8090";

	private TestApplication app;
	private ManagedApplication ma;
	private Manager manager;
	private TestManagerWrapper managerWrapper;

	private WsClient client;
	private HttpServer httpServer;


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

		// Disable the messages timer for predictability
		TestUtils.getInternalField( this.manager, "timer", Timer.class).cancel();

		URI uri = UriBuilder.fromUri( REST_URI ).build();
		RestApplication restApp = new RestApplication( this.manager );
		this.httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );

		// Load an application
		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app );
		this.managerWrapper.getNameToManagedApplication().put( this.app.getName(), this.ma );

		this.client = new WsClient( REST_URI );
	}


	@Test
	public void testListTargets_all() throws Exception {

		Assert.assertEquals( 0, this.client.getTargetWsDelegate().listAllTargets().size());
		this.client.getTargetWsDelegate().createTarget( "" );
		Assert.assertEquals( 1, this.client.getTargetWsDelegate().listAllTargets().size());
		String t2 = this.client.getTargetWsDelegate().createTarget( "" );
		Assert.assertNotNull( t2 );

		Assert.assertEquals( 2, this.client.getTargetWsDelegate().listAllTargets().size());
		this.client.getTargetWsDelegate().createTarget( "" );
		Assert.assertEquals( 3, this.client.getTargetWsDelegate().listAllTargets().size());

		this.client.getTargetWsDelegate().deleteTarget( t2 );
		Assert.assertEquals( 2, this.client.getTargetWsDelegate().listAllTargets().size());
	}


	@Test
	public void testCreateTarget() throws Exception {

		String content = "id: toto";
		String targetId = this.client.getTargetWsDelegate().createTarget( content );
		String read = this.manager.targetsMngr().findRawTargetProperties( targetId );

		Assert.assertEquals( content, read );
	}


	@Test( expected = TargetWsException.class )
	public void testAssociations_invalidApplication() throws Exception {

		this.client.getTargetWsDelegate().createTarget( "" );
		String t2 = this.client.getTargetWsDelegate().createTarget( "" );

		TestApplication app1 = new TestApplication();
		TestApplication app2 = new TestApplication();
		app2.name( "myApp2" );

		this.managerWrapper.getNameToManagedApplication().put( app1.getName(), new ManagedApplication( app1 ));
		this.managerWrapper.getNameToManagedApplication().put( app2.getName(), new ManagedApplication( app2 ));

		this.client.getTargetWsDelegate().associateTarget( new Application( "invalid", app1.getTemplate()), null, t2, true );
	}


	@Test
	public void testAssociations_success() throws Exception {

		String targetId = this.client.getTargetWsDelegate().createTarget( "" );
		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		try {
			this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
			Assert.fail( "No target should have been found." );

		} catch( IOException e ) {
			// nothing
		}

		this.client.getTargetWsDelegate().associateTarget( app, null, targetId, true );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
	}


	@Test
	public void testAssociations_doAndUndo() throws Exception {

		String targetId = this.client.getTargetWsDelegate().createTarget( "" );
		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.getNameToManagedApplication().put( app.getName(), ma );

		try {
			this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
			Assert.fail( "No target should have been found." );

		} catch( IOException e ) {
			// nothing
		}

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		this.client.getTargetWsDelegate().associateTarget( app, instancePath, targetId, true );

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());

		try {
			this.client.getTargetWsDelegate().deleteTarget( targetId );
			Assert.fail( "We should not be able to delete this target, it is under use." );

		} catch( Exception e ) {
			// nothing
		}

		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		this.client.getTargetWsDelegate().associateTarget( app, instancePath, targetId, false );

		try {
			this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
			Assert.fail( "No target should have been found." );

		} catch( IOException e ) {
			// nothing
		}
	}
}
