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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.target.TargetHelpers;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;
import net.roboconf.dm.rest.services.internal.resources.IDebugResource;
import net.roboconf.messaging.MessagingConstants;

import net.roboconf.messaging.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.internal.client.test.TestClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DebugResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private DebugResource resource;
	private MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();


	@Before
	public void initializeDm() throws IOException {
		this.registry.addMessagingClientFactory(new TestClientFactory());

		this.manager = new Manager();
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setConfigurationDirectoryLocation( this.folder.newFolder().getAbsolutePath());
		this.manager.start();

		// Reconfigure with the messaging client factory registry set.
		this.manager.getMessagingClient().setRegistry(this.registry);
		this.manager.reconfigure();

		this.resource = new DebugResource( this.manager );
	}


	@After
	public void stopDm() {
		if( this.manager != null )
			this.manager.stop();
	}


	@Test
	public void testCreateApplication() throws Exception {

		final String fileContent = "Hello!";
		File dir = this.folder.newFolder();

		Assert.assertEquals( 0, dir.listFiles().length );
		this.resource.createApplication( dir, fileContent );
		Assert.assertEquals( 3, dir.listFiles().length );

		File f = new File( dir, Constants.PROJECT_DIR_GRAPH + "/" + DebugResource.ROOT_COMPONENT_NAME + "/" + Constants.TARGET_PROPERTIES_FILE_NAME );
		Assert.assertTrue( f.exists());

		String readContent = Utils.readFileContent( f );
		Assert.assertEquals( fileContent, readContent );
	}


	@Test
	public void testCreateTestForTargetProperties_successfulCreation() throws Exception {

		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
		InputStream in = new ByteArrayInputStream( "target.id = whatever".getBytes( "UTF-8" ));
		Response resp = this.resource.createTestForTargetProperties( in, null );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());
		ManagedApplication ma = this.manager.getNameToManagedApplication().get( IDebugResource.FAKE_APP_NAME );
		Assert.assertNotNull( ma );

		Assert.assertEquals( 1, ma.getApplication().getTemplate().getGraphs().getRootComponents().size());
		Component rootComponent = ma.getApplication().getTemplate().getGraphs().getRootComponents().iterator().next();
		Assert.assertEquals( DebugResource.ROOT_COMPONENT_NAME, rootComponent.getName());
		Assert.assertEquals( Constants.TARGET_INSTALLER, rootComponent.getInstallerName());

		Assert.assertEquals( 1, ma.getApplication().getRootInstances().size());
		Instance rootInstance = ma.getApplication().getRootInstances().iterator().next();
		Assert.assertEquals( "root", rootInstance.getName());
		Assert.assertEquals( rootComponent, rootInstance.getComponent());

		Map<String,String> targetProperties = TargetHelpers.loadTargetProperties( ma.getTemplateDirectory(), rootInstance );
		Assert.assertNotNull( targetProperties );
		Assert.assertEquals( 1, targetProperties.size());
		Assert.assertEquals( "whatever", targetProperties.get( "target.id" ));

		this.manager.deleteApplication( ma );
		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
	}


	@Test
	public void testCreateTestForTargetProperties_successfulUpdate() throws Exception {

		// Load an application once
		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
		InputStream in = new ByteArrayInputStream( "target.id = whatever".getBytes( "UTF-8" ));
		Response resp = this.resource.createTestForTargetProperties( in, null );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		ManagedApplication ma = this.manager.getNameToManagedApplication().get( IDebugResource.FAKE_APP_NAME );
		Instance rootInstance = ma.getApplication().getRootInstances().iterator().next();
		Map<String,String> targetProperties = TargetHelpers.loadTargetProperties( ma.getTemplateDirectory(), rootInstance );

		Assert.assertNotNull( targetProperties );
		Assert.assertEquals( 1, targetProperties.size());
		Assert.assertEquals( "whatever", targetProperties.get( "target.id" ));

		// Update the application
		in = new ByteArrayInputStream( "target.id = something else".getBytes( "UTF-8" ));
		resp = this.resource.createTestForTargetProperties( in, null );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		targetProperties = TargetHelpers.loadTargetProperties( ma.getTemplateDirectory(), rootInstance );
		Assert.assertNotNull( targetProperties );
		Assert.assertEquals( 1, targetProperties.size());
		Assert.assertEquals( "something else", targetProperties.get( "target.id" ));

		this.manager.deleteApplication( ma );
		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
	}


	@Test
	public void testCreateTestForTargetProperties_conflict() throws Exception {

		// Load a fake application
		ApplicationTemplate tpl = new ApplicationTemplate();
		tpl.setGraphs( new Graphs());

		Application app = new Application( IDebugResource.FAKE_APP_NAME, tpl );
		this.manager.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		InputStream in = new ByteArrayInputStream( "target.id = whatever".getBytes( "UTF-8" ));
		Response resp = this.resource.createTestForTargetProperties( in, null );
		Assert.assertEquals( Status.CONFLICT.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		ManagedApplication ma = this.manager.getNameToManagedApplication().get( IDebugResource.FAKE_APP_NAME );
		Assert.assertEquals( 0, ma.getApplication().getRootInstances().size());
	}


	@Test
	public void testCreateTestForTargetProperties_exception() throws Exception {

		// Load a fake application - without a graph => NPE.
		// Not supposed to happen at runtime because the validation runs first.
		Application app = new Application( IDebugResource.FAKE_APP_NAME, new ApplicationTemplate());
		this.manager.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		InputStream in = new ByteArrayInputStream( "target.id = whatever".getBytes( "UTF-8" ));
		Response resp = this.resource.createTestForTargetProperties( in, null );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		ManagedApplication ma = this.manager.getNameToManagedApplication().get( IDebugResource.FAKE_APP_NAME );
		Assert.assertEquals( 0, ma.getApplication().getRootInstances().size());
	}


	@Test
	public void testCreateTestForTargetProperties_machineStillRunning() throws Exception {

		// Load an application once
		Assert.assertEquals( 0, this.manager.getNameToManagedApplication().size());
		InputStream in = new ByteArrayInputStream( "target.id = whatever".getBytes( "UTF-8" ));
		Response resp = this.resource.createTestForTargetProperties( in, null );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		ManagedApplication ma = this.manager.getNameToManagedApplication().get( IDebugResource.FAKE_APP_NAME );
		Instance rootInstance = ma.getApplication().getRootInstances().iterator().next();

		Map<String,String> targetProperties = TargetHelpers.loadTargetProperties(
				ma.getTemplateDirectory(),
				rootInstance );

		Assert.assertNotNull( targetProperties );
		Assert.assertEquals( 1, targetProperties.size());
		Assert.assertEquals( "whatever", targetProperties.get( "target.id" ));

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());
		this.manager.changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, rootInstance.getStatus());

		// Try to update the application - it should fail
		in = new ByteArrayInputStream( "target.id = something else".getBytes( "UTF-8" ));
		resp = this.resource.createTestForTargetProperties( in, null );
		Assert.assertEquals( Status.CONFLICT.getStatusCode(), resp.getStatus());

		targetProperties = TargetHelpers.loadTargetProperties( ma.getTemplateDirectory(), rootInstance );
		Assert.assertNotNull( targetProperties );
		Assert.assertEquals( 1, targetProperties.size());
		Assert.assertEquals( "whatever", targetProperties.get( "target.id" ));

		// Stop the root instance
		this.manager.changeInstanceState( ma, rootInstance, InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		// Retry, it should work
		in = new ByteArrayInputStream( "target.id = something else".getBytes( "UTF-8" ));
		resp = this.resource.createTestForTargetProperties( in, null );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		targetProperties = TargetHelpers.loadTargetProperties( ma.getTemplateDirectory(), rootInstance );
		Assert.assertNotNull( targetProperties );
		Assert.assertEquals( 1, targetProperties.size());
		Assert.assertEquals( "something else", targetProperties.get( "target.id" ));
	}


	@Test
	public void testDiagnoseApplication() {

		TestApplication app = new TestApplication();
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		List<Diagnostic> diags = this.resource.diagnoseApplication( "inexisting" );
		Assert.assertEquals( 0, diags.size());

		diags = this.resource.diagnoseApplication( app.getName());
		Assert.assertEquals( InstanceHelpers.getAllInstances( app ).size(), diags.size());

		for( Diagnostic diag : diags ) {
			Instance inst = InstanceHelpers.findInstanceByPath( app, diag.getInstancePath());
			Assert.assertNotNull( inst );

			for( DependencyInformation info : diag.getDependenciesInformation()) {
				Assert.assertFalse( info.isResolved());
			}
		}
	}


	@Test
	public void testDiagnoseInstance() {

		TestApplication app = new TestApplication();
		String path = InstanceHelpers.computeInstancePath( app.getWar());
		ManagedApplication ma = new ManagedApplication( app );
		this.manager.getNameToManagedApplication().put( app.getName(), ma );

		Response resp = this.resource.diagnoseInstance( "inexisting", path );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());

		resp = this.resource.diagnoseInstance( app.getName(), "/inexisting" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());

		resp = this.resource.diagnoseInstance( app.getName(), path );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		Diagnostic diag = (Diagnostic) resp.getEntity();
		Assert.assertNotNull( diag );
		Assert.assertEquals( path, diag.getInstancePath());
		Assert.assertEquals( 1, diag.getDependenciesInformation().size());
	}
}
