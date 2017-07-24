/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.tests.commons.internal;

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
import java.util.logging.Logger;

import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

import net.roboconf.core.utils.UriUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;

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
	public static List<Option> getBaseOptionsAsList( ItConfigurationBean bean, IMessagingConfiguration messagingConfiguration ) {

		MavenArtifactUrlReference karafUrl = maven()
				.groupId( bean.getGroupId())
				.artifactId( bean.getArtifactId())
				.version( bean.getVersion())
				.type( "tar.gz" );

		// Configure the platform
		List<Option> options = new ArrayList<> ();
		options.add( karafDistributionConfiguration()
				.frameworkUrl( karafUrl )
				.unpackDirectory( new File( "target/exam-" + bean.getDirectoryName()))
				.useDeployFolder( false ));

		options.add( cleanCaches( true ));
		options.add( keepRuntimeFolder());
		options.add( systemTimeout( getTimeout()));

		// Which messaging configuration?
		options.addAll( messagingConfiguration.options());

		// Logs management
		if( bean.getRoboconfLogsLevel() != null ) {

			// Override the log configuration in Karaf
			for( String loggerName : LOGGERS ) {
				options.add( editConfigurationFilePut(
						"etc/org.ops4j.pax.logging.cfg",
						"log4j.logger." + loggerName,
						bean.getRoboconfLogsLevel() + ", roboconf" ));
			}

		} else if( bean.areLogsHidden()) {

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
	public static Option[] getBaseOptions( ItConfigurationBean bean, IMessagingConfiguration messagingConfiguration ) {
		return asArray( getBaseOptionsAsList( bean, messagingConfiguration ));
	}


	/**
	 * Waits for the DM's REST services to be online.
	 * @throws Exception
	 */
	public static void waitForDmRestServices( int serverPort ) throws Exception {

		// By default, PAX (runner) tests only wait for the manager to be available.
		// For some tests however, we must also be sure that the REST services are online.
		// The most simple solution is to wait for the applications listing to work.

		URI targetUri = UriUtils.urlToUri( "http://localhost:" + serverPort + "/roboconf-dm/applications" );
		for( int i=0; i<20; i++ ) {
			Thread.sleep( 1000 );
			String s = "";
			try {
				s = Utils.readUrlContent( targetUri.toString());

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


	/**
	 * @return the maximum delay to find OSGi services.
	 * <p>
	 * If the ROBCONF_IT_TIMEOUT environment variable is set, we return its
	 * values. Otherwise, the default timeout is returned (30s).
	 * </p>
	 */
	public static long getTimeout() {

		long result = 30000;
		Logger logger = Logger.getLogger( ItUtils.class.getName());
		String envValue = System.getenv( "ROBOCONF_IT_TIMEOUT" );
		try {
			if( envValue != null ) {
				logger.info( "Env variable ROBOCONF_IT_TIMEOUT is defined and will be used." );
				result = Long.parseLong( envValue );
			}

		} catch( NumberFormatException e ) {
			logger.warning( "The timeout for integration tests could not be read from ENV variables. " + e.getMessage());
		}

		return result;
	}
}
