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

package net.roboconf.integration.tests.servermode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.integration.probes.AgentTest;
import net.roboconf.integration.tests.internal.ItUtils;

import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer;
import org.ops4j.pax.exam.spi.PaxExamRuntime;

/**
 * @author Vincent Zurczak - Linagora
 */
public class KarafClient_AgentTest extends AgentTest {

	@Test
	public void run() throws Exception {

		// Prepare to run an agent distribution
		Option[] options = super.config();
		ExamSystem system = PaxExamRuntime.createServerSystem( options );
		TestContainer container = PaxExamRuntime.createContainer( system );
		Assert.assertEquals( KarafTestContainer.class, container.getClass());

		try {
			// Start the agent's distribution... and wait... :(
			container.start();
			Thread.sleep( ItUtils.getTimeout() / 3 );

			// Since this test runs outside Karaf, we cannot rely on System.getProperty( "karaf.base" );
			// So, we need to extract the Karaf directory by Java reflection.
			File karafDirectory = TestUtils.getInternalField( container, "targetFolder", File.class );
			Assert.assertNotNull( karafDirectory );

			File binDirectory = new File( karafDirectory, "bin" );
			Assert.assertTrue( binDirectory.exists());

			final StringBuilder sb = new StringBuilder();
			Logger logger = Logger.getLogger( getClass().getName());
			logger.setLevel( Level.ALL );
			logger.addHandler( new Handler() {

				@Override
				public void close() throws SecurityException {
					// nothing
				}

				@Override
				public void flush() {
					// nothing
				}

				@Override
				public void publish( LogRecord record ) {
					sb.append( record.getMessage() + "\n" );
				}
			});

			List<String> command = new ArrayList<> ();
			command.add( "/bin/sh" );
			command.add( "client" );
			command.add( "feature:list" );

			int code = ProgramUtils.executeCommand( logger, command, binDirectory, null );
			Assert.assertEquals( 0, code );
			Assert.assertTrue( sb.toString().contains( "ipojo-all" ));

		} finally {
			container.stop();
		}
	}
}
