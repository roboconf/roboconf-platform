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

package net.roboconf.integration.tests.dm;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.internal.MyHandler;
import net.roboconf.integration.tests.dm.internal.MyTargetResolver;
import net.roboconf.integration.tests.dm.probes.DmTest;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

/**
 * A test that verifies a full chain involving the autonomic.
 * <p>
 * This test uses a real agent, deployed locally, with the "embedded" target.
 * RabbitMQ is the messaging server.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class AutonomicTest extends DmTest {

	private static final String APP_LOCATION = "my.app.location";
	private static final String TMP_DIR = System.getProperty( "java.io.tmpdir" );

	private static final File PROBE_TRIGGER_FILE = new File( TMP_DIR, "probe-trigger" );
	private static final File AUTONOMIC_RULE_RESULT = new File( TMP_DIR, "autonomic-result" );


	@Inject
	protected Manager manager;

	@Inject
	protected AgentMessagingInterface agent;



	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( DmTest.class );
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

		// Store the application's location
		File targetDirectory = File.createTempFile( "roboconf-it-temp", "", null );
		Assert.assertTrue( targetDirectory.delete());
		Assert.assertTrue( targetDirectory.mkdir());

		File resourcesDirectory = TestUtils.findApplicationDirectory( "lamp" );
		Utils.copyDirectory( resourcesDirectory, targetDirectory );


		// Update the application's content (autonomic part)
		File cmdFile = new File( targetDirectory, Constants.PROJECT_DIR_COMMANDS + "/scale.commands" );
		Assert.assertTrue( cmdFile.delete());

		cmdFile = new File( targetDirectory, Constants.PROJECT_DIR_COMMANDS + "/it-cmd.commands" );
		Utils.writeStringInto( "write this into " + AUTONOMIC_RULE_RESULT.getAbsolutePath(), cmdFile );

		File ruleFile = new File( targetDirectory, Constants.PROJECT_DIR_RULES_AUTONOMIC + "/sample.drl" );
		Assert.assertTrue( ruleFile.exists());
		Utils.writeStringInto( "rule \"it\" when it-event then it-cmd end", ruleFile );

		File probesDir = new File( targetDirectory, Constants.PROJECT_DIR_PROBES );
		Assert.assertTrue( probesDir.mkdir());

		File probeFile = new File( probesDir, "/VM.measures" );
		Utils.writeStringInto( "[EVENT file it-event]\nDelete if exists " + PROBE_TRIGGER_FILE.getAbsolutePath(), probeFile );

		File vmDir = new File( targetDirectory, Constants.PROJECT_DIR_GRAPH + "/VM" );
		Assert.assertTrue( vmDir.mkdir());


		// Deploy the agent's bundles
		List<Option> options = new ArrayList<> ();
		options.addAll( Arrays.asList( super.config()));
		options.add( systemProperty( APP_LOCATION ).value( targetDirectory.getAbsolutePath()));

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
				.artifactId( "roboconf-agent-default" )
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

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-target-embedded" )
				.version( roboconfVersion )
				.start());

		// Agent configuration (embedded)
		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				Constants.MESSAGING_TYPE,
				RabbitMqConstants.FACTORY_RABBITMQ ));

		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				"application-name", "test" ));

		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				"scoped-instance-path", "/MySQL VM" ));

		return options.toArray( new Option[ options.size()]);
	}


	@Test
	public void run() throws Exception {

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
		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nhandler: embedded" );
		this.manager.targetsMngr().associateTargetWith( targetId, ma.getApplication(), null );

		// Instantiate a new root instance
		Instance rootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( rootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		this.manager.instancesMngr().changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );

		// At this step, the DM is waiting the agent to send a heart beat.
		// Except that in this test, the agent may have started (and tried to send a heart beat)
		// before the DM was started. So, we here force the agent to send a new heart beat.
		// What we want to test is that the autonomic works. Not the heart beats...
		this.agent.getMessagingClient().sendMessageToTheDm( new MsgNotifHeartbeat(
				this.agent.getApplicationName(),
				this.agent.getScopedInstancePath(),
				"127.0.0.1" ));

		// Wait a little bit
		Thread.sleep( 800 );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());

		// Wait a little bit, the autonomic should do its job.
		// The polling period for probes is Constants.PROBES_POLLING_PERIOD
		Thread.sleep( Constants.PROBES_POLLING_PERIOD + 100 );

		// A file exists => delete it and send a notification.
		// Notification received => create another file.
		// So, we just need to verify the file was created.
		Assert.assertTrue( AUTONOMIC_RULE_RESULT.exists());
	}


	@After
	public void cleanMess() throws Exception {

		Utils.deleteFilesRecursively( PROBE_TRIGGER_FILE );
		Utils.deleteFilesRecursively( AUTONOMIC_RULE_RESULT );

		String appLocation = System.getProperty( APP_LOCATION );
		if( ! Utils.isEmptyOrWhitespaces( appLocation ))
			Utils.deleteFilesRecursively( new File( appLocation ));
	}
}
