/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.test.framework;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.List;

import javax.inject.Inject;

import net.roboconf.dm.internal.environment.iaas.IaasResolver;
import net.roboconf.dm.management.Manager;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.in_memory.internal.IaasInMemory;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmWithAgentInMemoryTest extends AbstractTest {

	@Inject
	private Manager manager;

	@Inject
	private IaasInMemory inMemoryIaas;

	@Override
	protected String getArtifactId() {
		return "roboconf-karaf-dist-dm";
	}

	@Override
	protected String getDirectorySuffix() {
		return "dm-with-agent-in-memory";
	}

	@Configuration
	public Option[] config() {

		int debugPort = -1;
		List<Option> options = getBaseOptions( debugPort );
		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-iaas-in-memory" )
				.version( CURRENT_DEV_VERSION )
				.start());

		return options.toArray( new Option[ options.size()]);
	}


	@Override
	public void run() {

		// Update the manager instance to use another IaaS resolver
		this.manager.setIaasResolver( new IaasResolver() {
			@Override
			protected IaasInterface findIaasHandler( IaasInterface[] iaas, String iaasType ) {
				return DmWithAgentInMemoryTest.this.inMemoryIaas;
			}
		});

		// Run normally
		super.run();
	}
}
