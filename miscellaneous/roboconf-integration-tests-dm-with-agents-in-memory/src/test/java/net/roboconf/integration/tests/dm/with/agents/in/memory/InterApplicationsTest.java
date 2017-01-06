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

package net.roboconf.integration.tests.dm.with.agents.in.memory;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;

/**
 * A scoped instance MUST be able to export variables.
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class InterApplicationsTest extends DmWithAgentInMemoryTest {

	private static final String APP_LOCATION = "my.app.location";


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( DmWithAgentInMemoryTest.class );
		probe.addTest( TestUtils.class );

		probe.addTest( AbstractIntegrationTest.class );
		probe.addTest( IMessagingConfiguration.class );
		probe.addTest( ItConfigurationBean.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = super.getOptionsForInMemoryAsList();

		// Store the application's location
		File resourcesDirectory = TestUtils.findTestFile( "/app-with-external-exports" );
		String appLocation = resourcesDirectory.getAbsolutePath();
		options.add( systemProperty( APP_LOCATION ).value( appLocation ));

		return options.toArray( new Option[ options.size()]);
	}



	private ApplicationTemplate tplImporting, tplExporting;
	private ManagedApplication importing, exporting;


	/**
	 * Loads the templates and creates applications.
	 * @throws Exception
	 */
	private void prepare() throws Exception {

		// Load the application template
		String appLocation = System.getProperty( APP_LOCATION );
		File exportingDir = new File( appLocation );
		this.tplExporting = this.manager.applicationTemplateMngr().loadApplicationTemplate( exportingDir );

		File importingDir = new File( exportingDir, "../app-with-external-imports" );
		this.tplImporting = this.manager.applicationTemplateMngr().loadApplicationTemplate( importingDir );

		// Verify some assertions on the loaded templates
		Assert.assertEquals( 1, this.tplExporting.externalExports.size());
		Assert.assertEquals( "Lamp.lb-ip", this.tplExporting.externalExports.get( "Apache.ip" ));
		Assert.assertEquals( "Lamp", this.tplExporting.getExternalExportsPrefix());

		Component activitiComponent = ComponentHelpers.findComponent( this.tplImporting.getGraphs(), "SE-Activiti" );
		Assert.assertEquals( 3, activitiComponent.importedVariables.size());

		ImportedVariable var = activitiComponent.importedVariables.get( "Lamp.lb-ip" );
		Assert.assertNotNull( var );
		Assert.assertTrue( var.isExternal());
		Assert.assertFalse( var.isOptional());

		// Create applications
		this.exporting = this.manager.applicationMngr().createApplication( "exporting", null, this.tplExporting );
		Assert.assertNotNull( this.exporting );

		this.importing = this.manager.applicationMngr().createApplication( "importing", null, this.tplImporting );
		Assert.assertNotNull( this.importing );

		Assert.assertEquals( 2, this.manager.applicationMngr().getManagedApplications().size());

		// Associate a target with it
		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nhandler = in-memory" );
		this.manager.targetsMngr().associateTargetWith( targetId, this.importing.getApplication(), null );
		this.manager.targetsMngr().associateTargetWith( targetId, this.exporting.getApplication(), null );
	}


	/**
	 * Creates a binding between the two applications.
	 * @throws Exception
	 */
	private void bind() throws Exception {

		this.manager.applicationMngr().bindOrUnbindApplication(
				this.importing,
				this.tplExporting.getExternalExportsPrefix(),
				this.exporting.getName(),
				true );
	}


	/**
	 * Deploys the importing application.
	 * @throws Exception
	 */
	private void deployImporting() throws Exception {
		this.manager.instancesMngr().deployAndStartAll( this.importing, null );
	}


	/**
	 * Deploys the exporting application.
	 * @throws Exception
	 */
	private void deployExporting() throws Exception {

		this.manager.instancesMngr().deployAndStartAll( this.exporting, null );
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( this.exporting.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}
	}


	/**
	 * Verifies that the IMPORTING application is missing external dependencies.
	 * @throws Exception
	 */
	private void verifyImportingIsWaiting() throws Exception {

		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( this.importing.getApplication())) {
			if( "se".equals( inst.getName()))
				Assert.assertEquals( InstanceStatus.UNRESOLVED, inst.getStatus());
			else
				Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}
	}


	/**
	 * Verifies assertions on the applications once they were started and bound together.
	 * @throws Exception
	 */
	private void verifyAfter() throws Exception {

		Thread.sleep( 800 );

		// Verify the importing application is entirely started
		for( Instance inst : InstanceHelpers.getAllInstances( this.importing.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}

		// Verify the import of the external variable
		Instance seInstance = InstanceHelpers.findInstanceByPath( this.importing.getApplication(), "/VM/petals/se" );
		Assert.assertNotNull( seInstance );

		Collection<Import> imports = seInstance.getImports().get( "Lamp" );
		Assert.assertNotNull( imports );
		Assert.assertEquals( 1, imports.size());

		Import imp = imports.iterator().next();
		Assert.assertEquals( "Lamp", imp.getComponentName());
		Assert.assertEquals( "/Apache VM/Apache", imp.getInstancePath());
		Assert.assertEquals( 1, imp.getExportedVars().size());
		Assert.assertTrue( imp.getExportedVars().containsKey( "Lamp.lb-ip" ));

		// Undeploy the exporting application
		this.manager.instancesMngr().undeployAll( this.exporting, null );
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( this.exporting.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}

		// Verify the importing application
		for( Instance inst : InstanceHelpers.getAllInstances( this.importing.getApplication())) {
			if( "se".equals( inst.getName()))
				Assert.assertEquals( InstanceStatus.UNRESOLVED, inst.getStatus());
			else
				Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}

		imports = seInstance.getImports().get( "Lamp.lb-ip" );
		Assert.assertNull( imports );

		// Undeploy the importing application
		this.manager.instancesMngr().undeployAll( this.importing, null );
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( this.importing.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}
	}


	// These tests are about verifying permutations and various order in scenarios.
	// Depending on the order, we must verify that the states of the IMPORTING application
	// are relevant with respect to the dependencies resolution.


	// 1-2-3
	@Test
	public void bind_deployImporting_deployExporting() throws Exception {

		prepare();
		bind();

		deployImporting();
		verifyImportingIsWaiting();

		deployExporting();
		verifyAfter();
	}


	// 1-3-2
	@Test
	public void bind_deployExporting_deployImporting() throws Exception {

		prepare();
		bind();
		deployExporting();

		deployImporting();
		verifyAfter();
	}


	// 2-3-1
	@Test
	public void deployImporting_deployExporting_bind() throws Exception {

		prepare();
		deployImporting();
		verifyImportingIsWaiting();

		deployExporting();
		verifyImportingIsWaiting();

		bind();
		verifyAfter();
	}


	// 3-2-1
	@Test
	public void deployExporting_deployImporting_bind() throws Exception {

		prepare();
		deployExporting();

		deployImporting();
		verifyImportingIsWaiting();

		bind();
		verifyAfter();
	}


	// 3-1-2
	@Test
	public void deployExporting_bind_deployImporting() throws Exception {

		prepare();
		deployExporting();
		bind();
		deployImporting();
		verifyAfter();
	}


	// 2-1-3
	@Test
	public void deployImporting_bind_deployExporting() throws Exception {

		prepare();
		deployImporting();
		verifyImportingIsWaiting();

		bind();
		verifyImportingIsWaiting();

		deployExporting();
		verifyAfter();
	}


	@Test
	public void verifyApplicationBindingsAreSentAtTheBeginning() throws Exception {

		// Basic things
		prepare();
		deployImporting();
		deployExporting();
		bind();

		// Verify the importing application is entirely started
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( this.importing.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}

		// Now, stop all the instances in both applications
		this.manager.instancesMngr().undeployAll( this.exporting, null );
		this.manager.instancesMngr().undeployAll( this.importing, null );

		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( this.importing.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}

		for( Instance inst : InstanceHelpers.getAllInstances( this.exporting.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}

		// Deploy and start them all, again, but DO NOT bind.
		// Bindings should have been kept by the DM and sent to the new agents.
		this.manager.instancesMngr().deployAndStartAll( this.exporting, null );
		this.manager.instancesMngr().deployAndStartAll( this.importing, null );

		// All the instances should be started
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( this.importing.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}

		for( Instance inst : InstanceHelpers.getAllInstances( this.exporting.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}
	}
}
