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

package net.roboconf.integration.tests;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.probes.AbstractTest;
import net.roboconf.integration.probes.DmTest;
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
public class ScopedInstanceShouldBeAbleToExportVariablesTest extends DmTest {

	private static final String APP_LOCATION = "my.app.location";

	@Inject
	protected Manager manager;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( AbstractTest.class );
		probe.addTest( DmTest.class );
		probe.addTest( TestUtils.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {
		List<Option> options = getBaseOptions();

		// Store the application's location
		File resourcesDirectory = TestUtils.findApplicationDirectory( "root-exporting-variables" );
		String appLocation = resourcesDirectory.getAbsolutePath();
		options.add( systemProperty( APP_LOCATION ).value( appLocation ));

		// Deploy the agent's bundles
		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-plugin-api" )
				.version( getRoboconfVersion())
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent" )
				.version( getRoboconfVersion())
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-target-in-memory" )
				.version( getRoboconfVersion())
				.start());

		return options.toArray( new Option[ options.size()]);
	}


	@Override
	@Test
	public void run() throws Exception {

		// Wait for the in-memory target to be available
		for( int i=0; i<5; i++ ) {
			if( this.manager.getTargetHandlers().isEmpty())
				Thread.sleep(300);
			else
				break;
		}

		// Load the application template
		String appLocation = System.getProperty( APP_LOCATION );
		ApplicationTemplate tpl = this.manager.loadApplicationTemplate( new File( appLocation ));

		// Create an application
		ManagedApplication ma = this.manager.createApplication( "test", null, tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.getNameToManagedApplication().size());

		// Instantiate a new scoped instance
		Instance scopedInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/vm1" );
		Assert.assertNotNull( scopedInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, scopedInstance.getStatus());

		Instance childInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/vm1/app" );
		Assert.assertNotNull( childInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, childInstance.getStatus());

		this.manager.changeInstanceState( ma, scopedInstance, InstanceStatus.DEPLOYED_STARTED );
		Thread.sleep( 800 );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, scopedInstance.getStatus());

		// Verify that the child instance has resolved its dependencies and that it is started
		this.manager.changeInstanceState( ma, childInstance, InstanceStatus.DEPLOYED_STARTED );
		Thread.sleep( 800 );

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, childInstance.getStatus());
		Collection<Import> resolvedImports = childInstance.getImports().get( "VM" );
		Assert.assertEquals( 1, resolvedImports.size());

		Import imp = resolvedImports.iterator().next();
		Assert.assertEquals( "/vm1", imp.getInstancePath());
		Assert.assertEquals( "VM", imp.getComponentName());
		Assert.assertEquals( "test", imp.getExportedVars().get( "VM.config" ));
	}
}
