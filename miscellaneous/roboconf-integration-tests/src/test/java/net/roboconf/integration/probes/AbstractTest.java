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

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractTest {

	public static final long PLATFORM_TIMEOUT = 30000;
	protected boolean showLogs = false;


	/**
	 * @return a non-null list of options to run Karaf from this test
	 */
	public final List<Option> getBaseOptions() {

		MavenArtifactUrlReference karafUrl = maven()
				.groupId( getGroupId())
				.artifactId( getArtifactId())
				.version( getRoboconfVersion())
				.type( "tar.gz" );

		// Configure the platform
		List<Option> options = new ArrayList<Option> ();
		options.add( karafDistributionConfiguration()
				.frameworkUrl( karafUrl )
				.unpackDirectory( new File( "target/exam-" + getDirectorySuffix()))
				.useDeployFolder( false ));

		options.add( cleanCaches( true ));
		options.add( keepRuntimeFolder());
		options.add( systemTimeout( PLATFORM_TIMEOUT ));

		if( ! this.showLogs ) {
			// Override the log configuration in Karaf
			options.add( logLevel( LogLevel.ERROR ));
			options.add( editConfigurationFilePut(
					  "etc/org.ops4j.pax.logging.cfg",
					  "log4j.logger.net.roboconf",
					  "ERROR, roboconf" ));

			// Do not show the Karaf console in the logs
			options.add( configureConsole().ignoreLocalConsole());
		}

		return options;
	}


	protected abstract String getArtifactId();
	protected abstract String getDirectorySuffix();

	/**
	 * The test run method.
	 * @throws Exception
	 */
	protected abstract void run() throws Exception;


	protected String getGroupId() {
		return "net.roboconf";
	}

	protected final String getRoboconfVersion() {
		return MavenUtils.getArtifactVersion( "net.roboconf", "roboconf-core" );
	}
}
