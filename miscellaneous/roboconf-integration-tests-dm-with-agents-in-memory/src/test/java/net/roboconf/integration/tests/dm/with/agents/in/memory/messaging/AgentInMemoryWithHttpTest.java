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

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;

import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.commons.internal.parameterized.HttpConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfITConfiguration;

/**
 * @author Vincent Zurczak - Linagora
 */
@RoboconfITConfiguration( withRabbitMq = false )
public class AgentInMemoryWithHttpTest extends AbstractAgentInMemoryTest {

	/**
	 * Constructor.
	 */
	public AgentInMemoryWithHttpTest() {
		super( new HttpConfiguration(), "HTTP" );
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		// Since the DM's port is dynamically changed during the test, we
		// need to update the HTTP messaging configuration too.
		List<Option> options = new ArrayList<>( Arrays.asList( super.config()));
		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.http.cfg",
				"net.roboconf.messaging.http.server.port",
				String.valueOf( getCurrentPort())));

		return ItUtils.asArray( options );
	}


	@Override
	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( HttpConfiguration.class );

		return probe;
	}
}
