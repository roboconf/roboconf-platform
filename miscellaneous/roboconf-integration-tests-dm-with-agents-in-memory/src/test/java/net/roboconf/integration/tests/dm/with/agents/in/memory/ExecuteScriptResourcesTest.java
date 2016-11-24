/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.List;
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
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.with.agents.in.memory.internal.MyHandler;
import net.roboconf.integration.tests.dm.with.agents.in.memory.internal.MyTargetResolver;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;

/**
 * Test a script execution by an agent.
 * <p>
 * Deploy and start all instances, etc.
 * On two agents.
 * </p>
 *
 * @author Amadou Diarra - UGA
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class ExecuteScriptResourcesTest extends DmWithAgentInMemoryTest {

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

		// Create script files
		String targetId = this.manager.targetsMngr().findTargetId(ma.getApplication(), "/MySQL VM");
		File dir = new File( this.manager.configurationMngr().getWorkingDirectory(), ConfigurationUtils.TARGETS + "/" + targetId );
		Utils.writeStringInto( "#!/bin/bash\necho toto > toto.txt", new File( dir, "totoscript-all.sh"));
		Utils.writeStringInto( "Bonjour le monde cruel", new File( dir, "titiscript.py"));

		// Deploy
		Instance mysql = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM/MySQL" );
		Assert.assertNotNull( mysql );

		this.manager.instancesMngr().deployAndStartAll(ma, null);

		// The deploy and start messages for 'app' and 'MySQL' were stored in the DM.
		// Wait for them to be picked up by the message checker thread.
		// 7s = 6s (Manager#TIMER_PERIOD) + 1s for security
		Thread.sleep( 7000 );

		this.manager.instancesMngr().changeInstanceState( ma, mysql.getParent(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, mysql.getParent().getStatus());

		// Verify that the main script is executed
		File vmDir = InstanceHelpers.findInstanceDirectoryOnAgent( mysql.getParent() );
		File toto = new File( vmDir, "toto.txt" );
		Assert.assertTrue( toto.exists() );

		String s = Utils.readFileContent( toto );
		List<File> files = Utils.listAllFiles(vmDir);

		Assert.assertEquals( "toto", s.trim() );
		Assert.assertEquals( 3, files.size() );
	}
}
