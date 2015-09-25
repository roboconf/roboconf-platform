/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.probes;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.integration.tests.internal.ItUtils;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;

/**
 * A base test for PAX-runner tests with the DM's distribution and agents in-memory.
 * @author Vincent Zurczak - Linagora
 */
public abstract class DmWithAgentInMemoryTest {

	@Inject
	protected Manager manager;

	@Inject
	@Filter( "(factory.name=roboconf-target-in-memory)" )
	private TargetHandler inMemoryIaas;


	@Configuration
	public Option[] config() throws Exception {
		return ItUtils.getOptionsForInMemory( true );
	}


	/**
	 * Updates the IaaS to use in-memory messaging and IaaS resolution.
	 */
	protected void configureManagerForInMemoryUsage() throws IOException, InterruptedException {

		this.manager.setTargetResolver( new InMemoryTargetResolver( this.inMemoryIaas ));
		this.manager.reconfigure();

		// Sleep for a while, to let the RabbitMQ client factory arrive.
		Thread.sleep(1000);
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static final class InMemoryTargetResolver implements ITargetHandlerResolver {
		private final TargetHandler inMemoryIaas;


		public InMemoryTargetResolver( TargetHandler inMemoryIaas ) {
			this.inMemoryIaas = inMemoryIaas;
		}

		@Override
		public TargetHandler findTargetHandler( Map<String,String> targetProperties )
		throws TargetException {
			return this.inMemoryIaas;
		}
	}
}
