/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.util.List;
import java.util.Timer;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.DebugWsException;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;
import net.roboconf.dm.rest.services.internal.RestApplication;
import net.roboconf.messaging.api.MessagingConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DebugWsDelegateTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private static final String REST_URI = "http://localhost:8090";

	private WsClient client;
	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private HttpServer httpServer;
	private TestApplication app;


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

		// Configure a single application to be used by the tests
		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());

		URI uri = UriBuilder.fromUri( REST_URI ).build();
		RestApplication restApp = new RestApplication( this.manager );
		this.httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );;

		this.client = new WsClient( REST_URI );
	}


	@Test
	public void testDiagnoseApplication() throws Exception {

		List<Diagnostic> diags = this.client.getDebugDelegate().diagnoseApplication( "invalid" );
		Assert.assertEquals( 0, diags.size());

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		diags = this.client.getDebugDelegate().diagnoseApplication( this.app.getName());
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

		Diagnostic diag = this.client.getDebugDelegate().diagnoseInstance( this.app.getName(), path );
		Assert.assertNotNull( diag );
		Assert.assertEquals( path, diag.getInstancePath());
		Assert.assertEquals( 1, diag.getDependenciesInformation().size());
	}


	@Test( expected = DebugWsException.class )
	public void testDiagnoseInstance_inexistingInstance() throws Exception {

		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		this.client.getDebugDelegate().diagnoseInstance( this.app.getName(), "/inexisting" );
	}


	@Test( expected = DebugWsException.class )
	public void testDiagnoseInstance_inexistingApplication() throws Exception {

		String path = InstanceHelpers.computeInstancePath( this.app.getWar());
		ManagedApplication ma = new ManagedApplication( this.app );
		this.managerWrapper.addManagedApplication( ma );

		this.client.getDebugDelegate().diagnoseInstance( "inexisting", path );
	}
}
