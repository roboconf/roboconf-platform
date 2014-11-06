/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.pax.probe;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

/**
 * @author Vincent Zurczak - Linagora
 */
@RunWith( PaxExam.class )
public abstract class AbstractTest {

	public static final String CURRENT_DEV_VERSION = "0.2-SNAPSHOT";
	public static final long PLATFORM_TIMEOUT = 30000;


	/**
	 * @return a non-null list of options to run Karaf from this test
	 */
	public List<Option> getBaseOptions() {

		MavenArtifactUrlReference karafUrl = maven()
				.groupId( getGroupId())
				.artifactId( getArtifactId())
				.version( CURRENT_DEV_VERSION )
				.type( "tar.gz" );

		List<Option> options = new ArrayList<Option> ();
		options.add( karafDistributionConfiguration()
				.frameworkUrl( karafUrl )
				.unpackDirectory( new File( "target/exam-" + getDirectorySuffix()))
				.useDeployFolder( false ));

		options.add( cleanCaches( true ));
		options.add( keepRuntimeFolder());
		options.add( systemTimeout( PLATFORM_TIMEOUT ));
		options.add( logLevel( LogLevel.INFO ));

		return options;
	}


	protected abstract String getArtifactId();
	protected abstract String getDirectorySuffix();


	protected String getGroupId() {
		return "net.roboconf";
	}


	@Test
	public void run() throws Exception {

		for( ;; ) {
			try {
				Thread.sleep( 10000 );

			} catch( InterruptedException e ) {
				e.printStackTrace();
			}
		}
	}
}
