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

package net.roboconf.integration.tests.dm.probes;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.commons.internal.parameterized.RabbitMqConfiguration;

/**
 * A base class to run the DM's distribution.
 * @author Vincent Zurczak - Linagora
 */
public abstract class DmTest extends AbstractIntegrationTest {

	/**
	 * A random port for the DM's web server (starting from 8282 and incrementing for every test).
	 */
	private static final AtomicInteger RP = new AtomicInteger( 8282 );


	@Configuration
	public Option[] config() throws Exception {

		RP.incrementAndGet();

		List<Option> options = ItUtils.getBaseOptionsAsList( getConfigurationBean(), new RabbitMqConfiguration());
		options.add( editConfigurationFilePut(
				"etc/org.ops4j.pax.web.cfg",
				"org.osgi.service.http.port",
				String.valueOf( getCurrentPort())));

		return ItUtils.asArray( options );
	}

	@Override
	protected ItConfigurationBean getConfigurationBean() {
		return new ItConfigurationBean( "roboconf-karaf-dist-dm", "dm" ).hideLogs( hideLogs());
	}

	protected int getCurrentPort() {
		return RP.get();
	}

	protected boolean hideLogs() {
		return true;
	}
}
