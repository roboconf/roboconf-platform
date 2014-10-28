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

package net.roboconf.pax.probe;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.management.ITargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmWithAgentInMemoryTest extends AbstractTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Inject
	protected Manager manager;

	@Inject
	@Filter( "(factory.name=roboconf-target-in-memory)" )
	private TargetHandler inMemoryIaas;


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
				.artifactId( "roboconf-plugin-api" )
				.version( CURRENT_DEV_VERSION )
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent" )
				.version( CURRENT_DEV_VERSION )
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-target-in-memory" )
				.version( CURRENT_DEV_VERSION )
				.start());

		return options.toArray( new Option[ options.size()]);
	}


	@Override
	public void run() throws Exception {

		// Update the manager instance
		configureManagerForInMemoryUsage();

		// Run normally
		super.run();
	}


	/**
	 * Updates the IaaS to use in-memory messaging and IaaS resolution.
	 */
	protected void configureManagerForInMemoryUsage() throws IOException {

		this.manager.setMessagingFactoryType( MessagingConstants.FACTORY_TEST );
		this.manager.setConfigurationDirectoryLocation( this.folder.newFolder().getAbsolutePath());
		this.manager.setTargetResolver( new InMemoryTargetResolver( this.inMemoryIaas ));
		this.manager.update();
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static final class InMemoryTargetResolver implements ITargetResolver {
		private final TargetHandler inMemoryIaas;


		public InMemoryTargetResolver( TargetHandler inMemoryIaas ) {
			this.inMemoryIaas = inMemoryIaas;
		}

		@Override
		public TargetHandler findTargetHandler( List<TargetHandler> target, ManagedApplication ma, Instance instance )
		throws TargetException {
			return this.inMemoryIaas;
		}
	}
}