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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.with.agents.in.memory.internal.MyHandler;
import net.roboconf.integration.tests.dm.with.agents.in.memory.internal.MyTargetResolver;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;

/**
 * Test bulk actions.
 * <p>
 * Deploy and start all instances, etc.
 * On two agents.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class BulkActionsTest extends DmWithAgentInMemoryTest {

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

		probe.addTest( MyHandler.class );
		probe.addTest( MyTargetResolver.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		File resourcesDirectory = TestUtils.findApplicationDirectory( "simple" );
		String appLocation = resourcesDirectory.getAbsolutePath();
		return OptionUtils.combine(
				super.config(),
				systemProperty( APP_LOCATION ).value( appLocation ));
	}


	@Test
	public void run() throws Exception {

		// Update the manager
		configureManagerForInMemoryUsage();

		// Load the application
		String appLocation = System.getProperty( APP_LOCATION );
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( new File( appLocation ));
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.applicationMngr().getManagedApplications().size());

		// Associate a default target for this application
		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nhandler: in-memory" );
		this.manager.targetsMngr().associateTargetWith( targetId, ma.getApplication(), null );

		// Deploy...
		Instance mysql = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM/MySQL" );
		Instance app = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/App VM/App" );
		Assert.assertNotNull( mysql );
		Assert.assertNotNull( app );

		this.manager.instancesMngr().deployAndStartAll( ma, null );

		// The deploy and start messages for 'app' and 'MySQL' were stored in the DM.
		// Wait for them to be picked up by the message checker thread.
		// 7s = 6s (Manager#TIMER_PERIOD) + 1s for security
		Thread.sleep( 7000 );

		this.manager.instancesMngr().changeInstanceState( ma, mysql.getParent(), InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().changeInstanceState( ma, app.getParent(), InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().changeInstanceState( ma, mysql, InstanceStatus.DEPLOYED_STARTED );
		this.manager.instancesMngr().changeInstanceState( ma, app, InstanceStatus.DEPLOYED_STARTED );

		this.manager.instancesMngr().stopAll( ma, null );
		Thread.sleep( 300 );

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, mysql.getParent().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, mysql.getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, app.getParent().getStatus());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STOPPED, app.getStatus());

		// Undeploy them all
		this.manager.instancesMngr().undeployAll( ma, null );
		Thread.sleep( 300 );
		for( Instance inst : InstanceHelpers.getAllInstances( ma.getApplication()))
			Assert.assertEquals( inst.getName(), InstanceStatus.NOT_DEPLOYED, inst.getStatus());
	}
}
