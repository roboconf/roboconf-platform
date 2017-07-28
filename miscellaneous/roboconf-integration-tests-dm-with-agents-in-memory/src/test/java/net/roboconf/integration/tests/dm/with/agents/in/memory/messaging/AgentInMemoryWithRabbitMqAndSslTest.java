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

package net.roboconf.integration.tests.dm.with.agents.in.memory.messaging;

import java.io.File;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.parameterized.RabbitMqWithSslConfigurationWithoutUserData;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfITConfiguration;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

/**
 * Test messaging with RabbitMQ over SSL without sending SSL configuration as user data.
 * <p>
 * These tests should be run locally and are disabled by default.
 * </p>
 * <p>
 * Clone https://github.com/roboconf/rabbitmq-with-ssl-in-docker and
 * follow the instructions to launch a Docker container with RabbitMQ and
 * SSL configuration. These tests natively run with such a container.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@Ignore
// We consider it is not up to our runner to verify RMQ with SSL
// is running. A specific environment must be setup for this test to run.
// And since it uses Docker, we wrote the test based on a Linux file system.
@RoboconfITConfiguration( withRabbitMq = false )
public class AgentInMemoryWithRabbitMqAndSslTest extends AbstractAgentInMemoryTest {

	/**
	 * Constructor.
	 */
	public AgentInMemoryWithRabbitMqAndSslTest() {
		super( new RabbitMqWithSslConfigurationWithoutUserData(), "Rabbit MQ and SSL without user data" );
	}


	@Override
	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( RabbitMqWithSslConfigurationWithoutUserData.class );
		probe.addTest( AbstractAgentInMemoryTest.class );
		probe.addTest( DmWithAgentInMemoryTest.class );
		probe.addTest( InMemoryTargetResolver.class );
		probe.addTest( AbstractIntegrationTest.class );
		probe.addTest( IMessagingConfiguration.class );
		probe.addTest( ItConfigurationBean.class );

		return probe;
	}


	@Test
	@Override
	public void run() throws Exception {

		// Verify that messaging works.
		// This will create the agents in-memory.
		super.run();

		// Verify (after) that SSL files were NOT transmitted.
		// The messaging configuration was left untouched
		// (unlike what happens with SSL configuration being passed through user data with agent in-memory).
		File karafEtc = new File( System.getProperty( Constants.KARAF_ETC ));
		Assert.assertTrue( karafEtc.isDirectory());

		File agentConfigurationFile = new File( karafEtc, Constants.KARAF_CFG_FILE_AGENT );
		Assert.assertTrue( agentConfigurationFile.isFile());
		Properties props = Utils.readPropertiesFile( agentConfigurationFile );
		Assert.assertEquals( "rabbitmq", props.getProperty( Constants.MESSAGING_TYPE ));

		File messagingConfigurationFile = new File( karafEtc, "net.roboconf.messaging.rabbitmq.cfg" );
		Assert.assertTrue( messagingConfigurationFile.isFile());
		props = Utils.readPropertiesFile( messagingConfigurationFile );
		Assert.assertEquals( "false", props.get( RabbitMqConstants.RABBITMQ_SSL_AS_USER_DATA ));
		Assert.assertEquals( "/tmp/docker-test/trust-store.p12", props.get( RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PATH ));
		Assert.assertEquals( "/tmp/docker-test/key-store.p12", props.get( RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PATH ));
	}


	@Override
	protected String createTargetProperties() {
		return "id: tid\nhandler: in-memory\nin-memory.write-user-data = true";
	}
}
