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

package net.roboconf.integration.tests.dm;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer;
import org.ops4j.pax.exam.spi.PaxExamRuntime;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.dm.probes.DmTest;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;
import net.roboconf.webextension.kibana.KibanaExtensionConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WebExtensionsTest extends DmTest {

	@Test
	public void run() throws Exception {

		Assume.assumeTrue( RabbitMqTestUtils.checkRabbitMqIsRunning());

		// Prepare to run a DM distribution
		String roboconfVersion = ItUtils.findRoboconfVersion();
		List<Option> options = new ArrayList<>( Arrays.asList( super.config()));
		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-web-extension-for-kibana" )
				.version( roboconfVersion )
				.start());

		ExamSystem system = PaxExamRuntime.createServerSystem( ItUtils.asArray( options ));
		TestContainer container = PaxExamRuntime.createContainer( system );
		Assert.assertEquals( KarafTestContainer.class, container.getClass());

		try {
			// Start the DM's distribution... and wait... :(
			container.start();
			ItUtils.waitForDmRestServices( getCurrentPort());

			// It may not work at the first time, since the extension needs to be loaded.
			// Verify that by default, there is a web extension
			boolean found = false;
			URL url = new URL( "http://localhost:" + getCurrentPort() + "/roboconf-dm/preferences" );
			for( int i=0; i<10; i++ ) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				InputStream in = url.openStream();
				try {
					Utils.copyStreamUnsafelyUseWithCaution( in, os );

				} finally {
					Utils.closeQuietly( in );
				}

				String received = os.toString( "UTF-8" );
				String expected = "{\"name\":\"" + IPreferencesMngr.WEB_EXTENSIONS + "\",\"value\":\"" + KibanaExtensionConstants.CONTEXT + "\",";
				found = received.contains( expected );
				if( found )
					break;
			}

			Assert.assertTrue( found );

		} finally {
			container.stop();
		}
	}
}
