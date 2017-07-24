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

import java.io.File;
import java.util.logging.Logger;

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
import net.roboconf.dm.management.api.IPreferencesMngr;
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

		// Prepare to run a DM distribution
		Option[] options = super.config();
		ExamSystem system = PaxExamRuntime.createServerSystem( options );
		TestContainer container = PaxExamRuntime.createContainer( system );
		Assert.assertEquals( KarafTestContainer.class, container.getClass());

		try {
			// Start the DM's distribution... and wait... :(
			container.start();
			ItUtils.waitForDmRestServices( getCurrentPort());

			// Verify we get the default CSS, which is quite big
			Logger logger = Logger.getLogger( getClass().getName());
			String cssUrl = "http://localhost:" + getCurrentPort() + "/roboconf-web-administration/roboconf.min.css";
			String cssContent = Utils.readUrlContentQuietly( cssUrl, logger );
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
			cssUrl = "http://localhost:" + getCurrentPort() + "/roboconf-web-administration/roboconf.min.css";
			cssContent = Utils.readUrlContentQuietly( cssUrl, logger );
			Assert.assertEquals( "hi!", cssContent );

			// Verify that by default, there is no web extension
			String url = "http://localhost:" + getCurrentPort() + "/roboconf-dm/preferences";
			String received = Utils.readUrlContentQuietly( url, logger );
			String expected = "{\"name\":\"" + IPreferencesMngr.WEB_EXTENSIONS + "\",\"value\":\"\",";
			Assert.assertTrue( received.contains( expected ));

		} finally {
			container.stop();
		}
	}
}
