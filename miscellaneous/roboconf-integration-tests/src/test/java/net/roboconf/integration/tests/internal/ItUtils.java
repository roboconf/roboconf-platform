/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.tests.internal;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.UriUtils;
import net.roboconf.integration.probes.ItConfigurationBean;

import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ItUtils {

	/**
	 * Private empty constructor.
	 */
	private ItUtils() {
		// nothing
	}


	public static final long PLATFORM_TIMEOUT = 30000;
	private static final String[] LOGGERS = {
		// Loggers configured in our custom distributions
		"net.roboconf",
		"net.roboconf.dm.internal.tasks.CheckerMessagesTask",
		"net.roboconf.dm.rest.services.internal.resources.impl.ApplicationResource",
		"net.roboconf.target.api.AbstractThreadedTargetHandler$CheckingRunnable",
		"net.roboconf.dm.internal.environment.messaging.DmMessageProcessor"
	};


	/**
	 * @return a non-null list of options to run Karaf from this test
	 */
	public static List<Option> getBaseOptionsAsList( ItConfigurationBean bean ) {

		MavenArtifactUrlReference karafUrl = maven()
				.groupId( bean.getGroupId())
				.artifactId( bean.getArtifactId())
				.version( bean.getVersion())
				.type( "tar.gz" );

		// Configure the platform
		List<Option> options = new ArrayList<Option> ();
		options.add( karafDistributionConfiguration()
				.frameworkUrl( karafUrl )
				.unpackDirectory( new File( "target/exam-" + bean.getDirectoryName()))
				.useDeployFolder( false ));

		options.add( cleanCaches( true ));
		options.add( keepRuntimeFolder());
		options.add( systemTimeout( PLATFORM_TIMEOUT ));

		// Logs management
		if( bean.areLogsHidden()) {
			// Override the log configuration in Karaf
			options.add( logLevel( LogLevel.ERROR ));
			for( String loggerName : LOGGERS ) {
				options.add( editConfigurationFilePut(
						  "etc/org.ops4j.pax.logging.cfg",
						  "log4j.logger." + loggerName,
						  "ERROR, roboconf" ));
			}

			// Do not show the Karaf console in the logs
			options.add( configureConsole().ignoreLocalConsole());
		}

		return options;
	}


	/**
	 * @return a non-null array of options to run Karaf from this test
	 */
	public static Option[] getBaseOptions( ItConfigurationBean bean ) {
		return asArray( getBaseOptionsAsList( bean ));
	}


	/**
	 * Waits for the DM's REST services to be online.
	 * @throws Exception
	 */
	public static void waitForDmRestServices() throws Exception {

		// By default, PAX (runner) tests only wait for the manager to be available.
		// For some tests however, we must also be sure that the REST services are online.
		// The most simple solution is to wait for the applications listing to work.

		URI targetUri = UriUtils.urlToUri( "http://localhost:8181/applications" );
		for( int i=0; i<20; i++ ) {
			Thread.sleep( 1000 );
			String s = "";
			try {
				s = TestUtils.readUriContent( targetUri );
			} catch( Exception e ) {
				// nothing
			}

			if( "[]".equals( s ))
				break;
		}
	}


	/**
	 * Converts a list of options to an array of options.
	 * @param options a non-null list of options
	 * @return a non-null array of options
	 */
	public static Option[] asArray( List<Option> options ) {
		Option[] result = new Option[ options.size()];
		return options.toArray( result );
	}


	/**
	 * @return the Roboconf version (found in the manifest of roboconf-core)
	 */
	public static String findRoboconfVersion() {
		return MavenUtils.getArtifactVersion( "net.roboconf", "roboconf-core" );
	}
}
