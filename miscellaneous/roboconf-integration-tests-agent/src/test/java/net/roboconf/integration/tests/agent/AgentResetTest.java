/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.tests.agent;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.integration.tests.agent.probes.AgentTest;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdSetScopedInstance;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;

/**
 * An integration test to verify resetting an agent actually works.
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class AgentResetTest extends AgentTest {

	@Inject
	protected AgentMessagingInterface agent;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( AgentTest.class );
		probe.addTest( AbstractIntegrationTest.class );
		probe.addTest( IMessagingConfiguration.class );
		probe.addTest( ItConfigurationBean.class );
		probe.addTest( TestApplication.class );
		probe.addTest( TestApplicationTemplate.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = new ArrayList<> ();
		options.addAll( Arrays.asList( super.config()));

		// Configure the agent's ID
		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				"application-name",
				"my app 014" ));

		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				"domain",
				"tsd" ));

		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				"scoped-instance-path",
				"/vm 45" ));

		return options.toArray( new Option[ options.size()]);
	}


	@Test
	public void run() throws Exception {

		// Verify the agent's ID
		Assert.assertEquals( "my app 014", this.agent.getApplicationName());
		Assert.assertEquals( "tsd", this.agent.getDomain());
		Assert.assertEquals( "/vm 45", this.agent.getScopedInstancePath());
		Assert.assertNull( this.agent.getScopedInstance());

		// Set a scoped instance
		TestApplication app = new TestApplication();
		MsgCmdSetScopedInstance msg = new MsgCmdSetScopedInstance( app.getTomcatVm());
		((ReconfigurableClientAgent) this.agent.getMessagingClient()).getMessageProcessor().getMessageQueue().add( msg );

		// Wait a little bit
		Thread.sleep( 1000 );

		// We should have a scoped instance now
		Assert.assertEquals( app.getTomcatVm(), this.agent.getScopedInstance());

		// Overwrite the parameters to force a reset
		File karafEtc = new File( System.getProperty( Constants.KARAF_ETC ));
		Assert.assertTrue( karafEtc.isDirectory());

		File agentConfigFile = new File( karafEtc, Constants.KARAF_CFG_FILE_AGENT );
		Assert.assertTrue( agentConfigFile.isFile());

		String content = Utils.readFileContent( agentConfigFile );
		content = content.replaceFirst( "(?mi)^parameters\\s*[:=].*$", "parameters = " + Constants.AGENT_RESET );
		Utils.writeStringInto( content, agentConfigFile );

		// Wait a little bit
		Thread.sleep( 5 * 1000 );

		// Everything should have been reset
		Assert.assertEquals( "", this.agent.getApplicationName());
		Assert.assertEquals( "", this.agent.getScopedInstancePath());
		Assert.assertEquals( Constants.DEFAULT_DOMAIN, this.agent.getDomain());
		Assert.assertEquals( 0, ((ReconfigurableClientAgent) this.agent.getMessagingClient()).getMessageProcessor().getMessageQueue().size());
		Assert.assertEquals( MessagingConstants.FACTORY_IDLE, this.agent.getMessagingClient().getMessagingType());
		Assert.assertNull( this.agent.getScopedInstance());

		// Verify the configuration file
		Properties props = Utils.readPropertiesFile( agentConfigFile );
		Assert.assertEquals( "", props.get( "parameters" ));
		Assert.assertEquals( "", props.get( "application-name" ));
		Assert.assertEquals( Constants.DEFAULT_DOMAIN, props.get( "domain" ));
		Assert.assertEquals( "", props.get( "scoped-instance-path" ));
	}
}
