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

package net.roboconf.integration.tests.paxrunner.miscellaneous;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import net.roboconf.agent.internal.Agent;
import net.roboconf.agent.internal.AgentMessageProcessor;
import net.roboconf.agent.internal.misc.HeartbeatTask;
import net.roboconf.agent.internal.misc.PluginMock;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.probes.DmTest;
import net.roboconf.integration.tests.internal.ItUtils;
import net.roboconf.integration.tests.internal.MyHandler;
import net.roboconf.integration.tests.internal.MyTargetResolver;
import net.roboconf.integration.tests.internal.runners.RoboconfPaxRunner;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * A test that verifies a full chain involving the autonomic.
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class AutonomicTest extends DmTest {

	private static final String APP_LOCATION = "my.app.location";
	private static final String TMP_DIR = System.getProperty( "java.io.tmpdir" );

	private static final File PROBE_TRIGGER_FILE = new File( TMP_DIR, "probe-trigger" );
	private static final File AUTONOMIC_RULE_RESULT = new File( TMP_DIR, "autonomic-result" );


	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Inject
	protected Manager manager;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( DmTest.class );
		probe.addTest( TestUtils.class );

		probe.addTest( MyHandler.class );
		probe.addTest( MyTargetResolver.class );

		// Classes from the agent
		probe.addTest( Agent.class );
		probe.addTest( PluginMock.class );
		probe.addTest( HeartbeatTask.class );
		probe.addTest( AgentMessageProcessor.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = new ArrayList<> ();
		options.addAll( Arrays.asList( super.config()));

		// Store the application's location
		File resourcesDirectory = TestUtils.findApplicationDirectory( "lamp" );
		File targetDirectory = this.folder.newFolder();
		Utils.copyDirectory( resourcesDirectory, targetDirectory );

		options.add( systemProperty( APP_LOCATION ).value( targetDirectory.getAbsolutePath()));

		// Update the application's content (autonomic part)
		File cmdFile = new File( targetDirectory, Constants.PROJECT_DIR_COMMANDS + "/scale.commands" );
		Assert.assertTrue( cmdFile.exists());
		Utils.writeStringInto( "[EVENT file it-event]\nDelete if exists " + PROBE_TRIGGER_FILE.getAbsolutePath(), cmdFile );

		File ruleFile = new File( targetDirectory, Constants.PROJECT_DIR_RULES_AUTONOMIC + "/sample.drl" );
		Assert.assertTrue( ruleFile.exists());
		Utils.writeStringInto( "rule \"it\" when it-event then it-cmd end", ruleFile );

		File probeFile = new File( targetDirectory, Constants.PROJECT_DIR_PROBES + "/" + "/VM.measures" );
		Utils.writeStringInto( "write this into " + AUTONOMIC_RULE_RESULT.getAbsolutePath(), probeFile );

		// Deploy the agent's bundles
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

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent-monitoring-api" )
				.version( roboconfVersion )
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent-monitoring" )
				.version( roboconfVersion )
				.start());

		return options.toArray( new Option[ options.size()]);
	}


	@Test
	public void run() throws Exception {

		// Update the manager
		MyTargetResolver myResolver = new MyTargetResolver();
		this.manager.setTargetResolver( myResolver );
		this.manager.reconfigure();

		// Sleep for a while, to let the RabbitMQ client factory arrive.
		Thread.sleep( 2000 );

		// Prepare the files for verifications
		if( ! PROBE_TRIGGER_FILE.exists())
			Assert.assertTrue( PROBE_TRIGGER_FILE.createNewFile());

		Utils.deleteFilesRecursively( AUTONOMIC_RULE_RESULT );
		Assert.assertFalse( AUTONOMIC_RULE_RESULT.exists());

		// Load the application
		String appLocation = System.getProperty( APP_LOCATION );
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( new File( appLocation ));
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.applicationMngr().getManagedApplications().size());

		// Associate a default target for this application
		String targetId = this.manager.targetsMngr().createTarget( "handler: whatever" );
		this.manager.targetsMngr().associateTargetWithScopedInstance( targetId, ma.getApplication(), null );

		// There is no agent yet (no root instance was deployed)
		Assert.assertEquals( 0, myResolver.handler.agentIdToAgent.size());

		// Instantiate a new root instance
		Instance rootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( rootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		this.manager.instancesMngr().changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );
		Thread.sleep( 800 );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());

		// Wait a little bit, the autonomic should do its job.
		Thread.sleep( 2000 );

		// A file does not exist => send a notification.
		// Notification received => create another file.
		// So, we just need to verify the file was created.
		Assert.assertTrue( AUTONOMIC_RULE_RESULT.exists());
	}
}
