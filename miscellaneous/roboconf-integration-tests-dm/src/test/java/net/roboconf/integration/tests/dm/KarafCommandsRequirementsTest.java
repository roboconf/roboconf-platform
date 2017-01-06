/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.tests.dm;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.ManifestUtils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.scheduler.IScheduler;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.probes.DmTest;

/**
 * This test verifies some requirements are met for our own Karaf commands.
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class KarafCommandsRequirementsTest extends DmTest {

	@Inject
	protected Manager manager;

	@Inject
	protected IScheduler scheduler;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( DmTest.class );
		probe.addTest( TestUtils.class );

		probe.addTest( AbstractIntegrationTest.class );
		probe.addTest( ItConfigurationBean.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = new ArrayList<> ();
		options.addAll( Arrays.asList( super.config()));

		// Deploy the agent's bundles (we need them because of this stupid probe).
		// If we'd run this test alone (e.g. "mvn clean test -Dtest=*Requi*"), then
		// we would not need these bundles. But when we run ALL the tests here,
		// we need them. Stupid PAX probe.
		String roboconfVersion = ItUtils.findRoboconfVersion();
		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-plugin-api" )
				.version( roboconfVersion )
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent" )
				.version( roboconfVersion )
				.start());

		return options.toArray( new Option[ options.size()]);
	}


	@Test
	public void verifySymbolicNameForRoboconfCore_1() throws Exception {

		Bundle bundle = FrameworkUtil.getBundle( ManifestUtils.class );
		Assert.assertNotNull( bundle );
		Assert.assertEquals( Constants.RBCF_CORE_SYMBOLIC_NAME, bundle.getSymbolicName());
	}


	@Test
	public void verifySymbolicNameForRoboconfCore_2() throws Exception {

		String symbolicName = ManifestUtils.findManifestProperty( org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME );
		Assert.assertEquals( Constants.RBCF_CORE_SYMBOLIC_NAME, symbolicName );
	}


	@Test
	public void verifyManifestUtilsInOsgiEnvironment() throws Exception {

		Assert.assertNotNull( ManifestUtils.findBundleVersion());
	}
}
