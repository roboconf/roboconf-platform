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

package net.roboconf.integration.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.integration.probes.AbstractTest;
import net.roboconf.integration.probes.AgentTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * @author Vincent Zurczak - Linagora
 */
@RunWith( PaxExam.class )
@ExamReactorStrategy( PerMethod.class )
public class KarafClient_AgentTest extends AgentTest {

	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( AbstractTest.class );
		probe.addTest( AgentTest.class );
		probe.addTest( TestUtils.class );

		return probe;
	}


	@Override
	@Test
	public void run() throws Exception {

		// Try to connect with Karaf's client.
		String karafDirectory = System.getProperty( "karaf.base" );
		Assert.assertNotNull( karafDirectory );
		Assert.assertFalse( Utils.isEmptyOrWhitespaces( karafDirectory ));

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
	}
}
