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

package net.roboconf.integration.test;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.integration.test.internal.IntegrationTestsUtils;
import net.roboconf.integration.test.internal.IntegrationTestsUtils.MyMessageProcessor;
import net.roboconf.integration.test.internal.RoboconfPaxRunner;
import net.roboconf.pax.probe.AbstractTest;
import net.roboconf.pax.probe.DmWithAgentInMemoryTest;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

/**
 * A set of tests for the "in-memory" target.
 * <p>
 * We launch a Karaf installation with an agent in-memory. We load
 * an application and instantiates a root instance. An iPojo component
 * is created (and associated with an in-memory agent).
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerClass.class )
public class AgentInMemoryTest extends DmWithAgentInMemoryTest {

	private static final String APP_LOCATION = "my.app.location";

	@Inject
	@Filter( "(factory.name=roboconf-agent-in-memory)" )
	private Factory agentFactory;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( AbstractTest.class );
		probe.addTest( DmWithAgentInMemoryTest.class );
		probe.addTest( InMemoryTargetResolver.class );
		probe.addTest( TestUtils.class );
		probe.addTest( TemporaryFolder.class );
		probe.addTest( IntegrationTestsUtils.class );
		probe.addTest( MyMessageProcessor.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() {

		String appLocation = null;
		try {
			File resourcesDirectory = TestUtils.findTestFile( "/lamp", getClass());
			appLocation = resourcesDirectory.getAbsolutePath();

		} catch( Exception e ) {
			// nothing
		}

		return OptionUtils.combine(
				super.config(),
				systemProperty( APP_LOCATION ).value( appLocation ));
	}


	@Override
	public void run() throws Exception {

		// Prepare everything
		configureManagerForInMemoryUsage();
		String appLocation = System.getProperty( APP_LOCATION );

		// Load the application
		ManagedApplication ma = this.manager.loadNewApplication( new File( appLocation ));
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.getAppNameToManagedApplication().size());

		// There is no agent yet (no root instance was deployed)
		Assert.assertEquals( 0, this.agentFactory.getInstances().size());

		// Instantiate a new root instance
		Instance rootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( rootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		this.manager.changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );
		Thread.sleep( 3000 );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());

		// A new agent must have been created
		List<ComponentInstance> instances = this.agentFactory.getInstances();
		Assert.assertEquals( 1, instances.size());

		ComponentInstance instance = instances.get( 0 );
		Assert.assertEquals( "MySQL VM @ Legacy LAMP", instance.getInstanceName());
		Assert.assertEquals( ComponentInstance.VALID, instance.getState());
		Assert.assertTrue( instance.isStarted());

		// Undeploy
		this.manager.changeInstanceState( ma, rootInstance, InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( 0, this.agentFactory.getInstances().size());
	}
}
