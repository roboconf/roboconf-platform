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

import static net.roboconf.core.Constants.LOCAL_RESOURCE_PREFIX;
import static net.roboconf.core.Constants.PROJECT_DIR_GRAPH;
import static net.roboconf.core.Constants.PROJECT_SUB_DIR_SCRIPTS;
import static net.roboconf.core.Constants.SCOPED_SCRIPT_AT_AGENT_SUFFIX;
import static net.roboconf.core.Constants.SCOPED_SCRIPT_AT_DM_CONFIGURE_SUFFIX;
import static net.roboconf.core.Constants.TARGET_PROPERTIES_FILE_NAME;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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
 * Test the execution of scripts provided by a target.
 * @author Amadou Diarra - UGA
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class ExecuteScriptResourcesTest extends DmWithAgentInMemoryTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
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

		File appDirectory = TestUtils.findApplicationDirectory( "simple" );
		String appLocation = appDirectory.getAbsolutePath();
		return OptionUtils.combine(
				super.config(),
				systemProperty( APP_LOCATION ).value( appLocation ));
	}


	@Test
	public void run() throws Exception {

		// Update the manager
		configureManagerForInMemoryUsage();

		// Copy the application...
		String appLocation = System.getProperty( APP_LOCATION );
		File originalDirectory = new File( appLocation );
		Assert.assertTrue( originalDirectory.exists());

		File directoryCopy = this.folder.newFolder();
		Utils.copyDirectory( originalDirectory, directoryCopy );

		// ...  and update it
		File targetDir = new File( directoryCopy, PROJECT_DIR_GRAPH + "/VM" );
		Assert.assertTrue( targetDir.exists());
		Assert.assertTrue( new File( targetDir, "target.properties" ).renameTo( new File( targetDir, "toto.properties" )));

		File outputFile = new File( System.getProperty( "java.io.tmpdir" ), "toto.txt" );
		Assert.assertTrue( ! outputFile.exists() || outputFile.delete());

		Assert.assertTrue( new File( targetDir, "toto/sub" ).mkdirs());
		Utils.writeStringInto(
				"#!/bin/bash\necho \"agent was here!\" >> " + outputFile.getAbsolutePath(),
				new File( targetDir, "toto/" + SCOPED_SCRIPT_AT_AGENT_SUFFIX + "sh" ));

		Utils.writeStringInto( "key = value", new File( targetDir, "toto/sub/script.properties" ));
		Utils.writeStringInto(
				"#!/bin/bash\necho \"DM was here!\" >> " + outputFile.getAbsolutePath(),
				new File( targetDir, "toto/" + LOCAL_RESOURCE_PREFIX + SCOPED_SCRIPT_AT_DM_CONFIGURE_SUFFIX + "sh" ));

		// Load the application
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( directoryCopy );
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.applicationMngr().getManagedApplications().size());
		Assert.assertEquals( 1, this.manager.targetsMngr().listAllTargets().size());

		// Verify the script files were copied correctly
		String targetId = "target-id";
		File dir = new File( this.manager.configurationMngr().getWorkingDirectory(), ConfigurationUtils.TARGETS + "/" + targetId );
		Assert.assertTrue( dir.exists());
		Assert.assertTrue( new File( dir, TARGET_PROPERTIES_FILE_NAME ).exists());
		Assert.assertTrue( new File( dir, PROJECT_SUB_DIR_SCRIPTS + "/" + SCOPED_SCRIPT_AT_AGENT_SUFFIX + "sh" ).exists());
		Assert.assertTrue( new File( dir, PROJECT_SUB_DIR_SCRIPTS + "/" + LOCAL_RESOURCE_PREFIX + SCOPED_SCRIPT_AT_DM_CONFIGURE_SUFFIX + "sh" ).exists());
		Assert.assertTrue( new File( dir, PROJECT_SUB_DIR_SCRIPTS + "/sub/script.properties" ).exists());

		// Deploy
		Instance scopedInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( scopedInstance );
		this.manager.instancesMngr().deployAndStartAll( ma, scopedInstance );

		// The deploy and start messages for 'app' and 'MySQL' were stored in the DM.
		// Wait for them to be picked up by the message checker thread.
		// 7s = 6s (Manager#TIMER_PERIOD) + 1s for security
		Thread.sleep( 7000 );

		this.manager.instancesMngr().changeInstanceState( ma, scopedInstance, InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, scopedInstance.getStatus());

		// Verify that the main scripts were executed.
		// Normally, the DM's script should have been executed before the agent's one.
		// But since the DM and the agent here lies in the same machine, the order cannot be guaranted.
		Assert.assertTrue( outputFile.exists());
		String s = Utils.readFileContent( outputFile ).trim();

		String s1 = "DM was here!\nagent was here!";
		String s2 = "agent was here!\nDM was here!";
		Assert.assertTrue( s, s.equals( s1 ) || s.equals( s2 ));
	}
}
