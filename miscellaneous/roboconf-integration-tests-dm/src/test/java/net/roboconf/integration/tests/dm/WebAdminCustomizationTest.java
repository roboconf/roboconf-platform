/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer;
import org.ops4j.pax.exam.spi.PaxExamRuntime;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.dm.probes.DmTest;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WebAdminCustomizationTest extends DmTest {

	@Test
	public void run() throws Exception {

		Assume.assumeTrue( RabbitMqTestUtils.checkRabbitMqIsRunning());

		// Prepare to run an agent distribution
		Option[] options = super.config();
		ExamSystem system = PaxExamRuntime.createServerSystem( options );
		TestContainer container = PaxExamRuntime.createContainer( system );
		Assert.assertEquals( KarafTestContainer.class, container.getClass());

		try {
			// Start the DM's distribution... and wait... :(
			container.start();
			ItUtils.waitForDmRestServices();

			// Verify we get the default CSS, which is quite big
			URL url = new URL( "http://localhost:8181/roboconf-web-administration/roboconf.min.css" );
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			InputStream in = url.openStream();
			try {
				Utils.copyStreamUnsafelyUseWithCaution( in, os );

			} finally {
				Utils.closeQuietly( in );
			}

			String cssContent = os.toString( "UTF-8" );
			Assert.assertTrue( cssContent.length() > 100 );

			// Now, override it with our custom one and verify it is returned by our servlet
			//
			// Since this test runs outside Karaf, we cannot rely on System.getProperty( "karaf.base" );
			// So, we need to extract the Karaf directory by Java reflection.
			File karafDirectory = TestUtils.getInternalField( container, "targetFolder", File.class );
			Assert.assertNotNull( karafDirectory );

			File etcDirectory = new File( karafDirectory, "etc" );
			Utils.writeStringInto( "hi!", new File( etcDirectory, "roboconf.custom.css" ));

			// Now, verify what we get
			url = new URL( "http://localhost:8181/roboconf-web-administration/roboconf.min.css" );
			os = new ByteArrayOutputStream();
			in = url.openStream();
			try {
				Utils.copyStreamUnsafelyUseWithCaution( in, os );

			} finally {
				Utils.closeQuietly( in );
			}

			Assert.assertEquals( "hi!", os.toString( "UTF-8" ));

		} finally {
			container.stop();
		}
	}
}
