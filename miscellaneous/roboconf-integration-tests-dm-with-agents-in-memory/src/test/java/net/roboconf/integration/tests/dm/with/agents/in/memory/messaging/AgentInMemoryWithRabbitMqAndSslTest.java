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

import org.junit.Ignore;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;

import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.parameterized.RabbitMqWithSslConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfITConfiguration;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;

/**
 * These tests should be run locally and are disabled by default.
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
@RoboconfITConfiguration( withRabbitMq = false )
public class AgentInMemoryWithRabbitMqAndSslTest extends AbstractAgentInMemoryTest {

	/**
	 * Constructor.
	 */
	public AgentInMemoryWithRabbitMqAndSslTest() {
		super( new RabbitMqWithSslConfiguration(), "Rabbit MQ and SSL" );
	}


	@Override
	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( RabbitMqWithSslConfiguration.class );
		probe.addTest( AbstractAgentInMemoryTest.class );
		probe.addTest( DmWithAgentInMemoryTest.class );
		probe.addTest( InMemoryTargetResolver.class );
		probe.addTest( AbstractIntegrationTest.class );
		probe.addTest( IMessagingConfiguration.class );
		probe.addTest( ItConfigurationBean.class );

		return probe;
	}
}
