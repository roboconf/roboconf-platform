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

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

/**
 * @author Vincent Zurczak - Linagora
 */
@RunWith( PaxExam.class )
public abstract class AbstractTest {

	public static final String CURRENT_DEV_VERSION = "0.2-SNAPSHOT";
	public static final long PLATFORM_TIMEOUT = 30000;

	protected abstract String getArtifactId();
	protected abstract String getDirectorySuffix();

	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * @param debugPort a positive integer for debug
	 * @return a non-null list of options to run Karaf from this test
	 */
	public List<Option> getBaseOptions( int debugPort ) {

		MavenArtifactUrlReference karafUrl = maven()
				.groupId( "net.roboconf" )
				.artifactId( getArtifactId())
				.version( CURRENT_DEV_VERSION )
				.type( "tar.gz" );

		List<Option> options = new ArrayList<Option> ();
		options.add( karafDistributionConfiguration()
				.frameworkUrl( karafUrl )
				.unpackDirectory( new File( "target/exam-" + getDirectorySuffix()))
				.useDeployFolder( false ));

		options.add( keepRuntimeFolder());
		if( debugPort != -1 )
			appendDebugOption( options, debugPort );

		options.add( systemTimeout( PLATFORM_TIMEOUT ));
		// deployPaxProbeIfNecessary( options );

		return options;
	}


	@Test
	public void run() {

		for( ;; ) {
			try {
				Thread.sleep( 10000 );

			} catch( InterruptedException e ) {
				e.printStackTrace();
			}
		}
	}


	private void appendDebugOption( List<Option> options, int debugPort ) {
		options.add( KarafDistributionOption.debugConfiguration( String.valueOf( debugPort ), true));
	}


	private void deployPaxProbeIfNecessary( List<Option> options ) {

		// This is required for sub-classes that are located in another Maven project.
		// By convention, we assume sub-classes are located in another package.
		String package1 = getClass().getPackage().getName();
		String package2 = AbstractTest.class.getPackage().getName();

		if( ! package1.equals( package2 )) {
			this.logger.info( "Adding the Roboconf PAX probe to the targetHandlers platform." );
			options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-pax-probe" )
				.version( CURRENT_DEV_VERSION )
				.start());
		}
	}
}
