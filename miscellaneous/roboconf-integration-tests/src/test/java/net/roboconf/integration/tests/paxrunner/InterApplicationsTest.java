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

package net.roboconf.integration.tests.paxrunner;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import net.roboconf.integration.probes.DmWithAgentInMemoryTest;
import net.roboconf.integration.tests.internal.ItUtils;
import net.roboconf.integration.tests.internal.RoboconfPaxRunner;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

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

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = new ArrayList<> ();
		options.addAll( Arrays.asList( ItUtils.getOptionsForInMemory( true )));

		// Store the application's location
		File resourcesDirectory = TestUtils.findTestFile( "/app-with-external-exports" );
		String appLocation = resourcesDirectory.getAbsolutePath();
		options.add( systemProperty( APP_LOCATION ).value( appLocation ));

		return options.toArray( new Option[ options.size()]);
	}


	@Test
	public void run() throws Exception {

		// Load the application template
		String appLocation = System.getProperty( APP_LOCATION );
		File exportingDir = new File( appLocation );
		ApplicationTemplate tplExporting = this.manager.applicationTemplateMngr().loadApplicationTemplate( exportingDir );

		File importingDir = new File( exportingDir, "../app-with-external-imports" );
		ApplicationTemplate tplImporting = this.manager.applicationTemplateMngr().loadApplicationTemplate( importingDir );

		// Verify some assertions on the loaded templates
		Assert.assertEquals( 1, tplExporting.externalExports.size());
		Assert.assertEquals( "Lamp.lb-ip", tplExporting.externalExports.get( "Apache.ip" ));
		Assert.assertEquals( "Lamp", tplExporting.getExternalExportsPrefix());

		Component activitiComponent = ComponentHelpers.findComponent( tplImporting.getGraphs(), "SE-Activiti" );
		Assert.assertEquals( 3, activitiComponent.importedVariables.size());

		ImportedVariable var = activitiComponent.importedVariables.get( "Lamp.lb-ip" );
		Assert.assertNotNull( var );
		Assert.assertTrue( var.isExternal());
		Assert.assertFalse( var.isOptional());

		// Create applications
		ManagedApplication exporting = this.manager.applicationMngr().createApplication( "exporting", null, tplExporting );
		Assert.assertNotNull( exporting );

		ManagedApplication importing = this.manager.applicationMngr().createApplication( "importing", null, tplImporting );
		Assert.assertNotNull( importing );

		Assert.assertEquals( 2, this.manager.applicationMngr().getManagedApplications().size());

		// Associate them together
		this.manager.applicationMngr().bindApplication( importing, tplExporting.getExternalExportsPrefix(), exporting.getName());

		// Associate a target with it
		String targetId = this.manager.targetsMngr().createTarget( "handler = in-memory" );
		this.manager.targetsMngr().associateTargetWithScopedInstance( targetId, importing.getApplication(), null );
		this.manager.targetsMngr().associateTargetWithScopedInstance( targetId, exporting.getApplication(), null );

		// Deploy the importing application
		this.manager.instancesMngr().deployAndStartAll( importing, null );
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( importing.getApplication())) {
			if( "se".equals( inst.getName()))
				Assert.assertEquals( InstanceStatus.UNRESOLVED, inst.getStatus());
			else
				Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}

		// Deploy the exporting one
		this.manager.instancesMngr().deployAndStartAll( exporting, null );
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( exporting.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}

		// Verify the importing application is entirely started
		for( Instance inst : InstanceHelpers.getAllInstances( importing.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}

		// Verify the import of the external variable
		Instance seInstance = InstanceHelpers.findInstanceByPath( importing.getApplication(), "/VM/petals/se" );
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
		this.manager.instancesMngr().undeployAll( exporting, null );
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( exporting.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}

		// Verify the importing application
		for( Instance inst : InstanceHelpers.getAllInstances( importing.getApplication())) {
			if( "se".equals( inst.getName()))
				Assert.assertEquals( InstanceStatus.UNRESOLVED, inst.getStatus());
			else
				Assert.assertEquals( inst.getName(), InstanceStatus.DEPLOYED_STARTED, inst.getStatus());
		}

		imports = seInstance.getImports().get( "Lamp.lb-ip" );
		Assert.assertNull( imports );

		// Undeploy the importing application
		this.manager.instancesMngr().undeployAll( importing, null );
		Thread.sleep( 800 );
		for( Instance inst : InstanceHelpers.getAllInstances( importing.getApplication())) {
			Assert.assertEquals( inst.getName(), InstanceStatus.NOT_DEPLOYED, inst.getStatus());
		}
	}
}
