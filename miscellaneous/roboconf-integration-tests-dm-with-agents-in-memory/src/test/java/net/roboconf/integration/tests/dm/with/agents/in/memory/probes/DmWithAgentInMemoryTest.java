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

package net.roboconf.integration.tests.dm.with.agents.in.memory.probes;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.After;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.parameterized.RabbitMqConfiguration;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * A base test for PAX-runner tests with the DM's distribution and agents in-memory.
 * @author Vincent Zurczak - Linagora
 */
public abstract class DmWithAgentInMemoryTest extends AbstractIntegrationTest {

	/**
	 * A random port for the DM's web server (starting from 8382 and incrementing for every test).
	 */
	private static final AtomicInteger RP = new AtomicInteger( 8382 );


	@Inject
	protected Manager manager;

	@Inject
	@Filter( "(factory.name=roboconf-target-in-memory)" )
	private TargetHandler inMemoryIaas;


	@Configuration
	public Option[] config() throws Exception {
		return ItUtils.asArray( getOptionsForInMemoryAsList());
	}


	@After
	public void clearAgentsWorkingDirectory() {
		File agentdirectory = new File( Constants.WORK_DIRECTORY_AGENT );
		Utils.deleteFilesRecursivelyAndQuietly( agentdirectory );
	}


	protected List<Option> getOptionsForInMemoryAsList() {

		List<Option> options = ItUtils.getBaseOptionsAsList( getConfigurationBean(), getMessagingConfiguration());
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
				.artifactId( "roboconf-target-in-memory" )
				.version( roboconfVersion )
				.start());

		RP.incrementAndGet();
		options.add( editConfigurationFilePut(
				"etc/org.ops4j.pax.web.cfg",
				"org.osgi.service.http.port",
				String.valueOf( getCurrentPort())));

		return options;
	}


	protected int getCurrentPort() {
		return RP.get();
	}


	@Override
	protected ItConfigurationBean getConfigurationBean() {
		return new ItConfigurationBean( "roboconf-karaf-dist-dm", "dm-with-agent-in-memory" );
	}


	protected IMessagingConfiguration getMessagingConfiguration() {
		return new RabbitMqConfiguration();
	}


	// TODO: do we need this?


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
