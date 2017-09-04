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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
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
import net.roboconf.core.internal.tests.TestUtils.StringHandler;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.dm.probes.DmTest;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * @author Amadou Diarra - UGA
 */
public class TargetsDeploymentTest extends DmTest {

	private final Logger logger = Logger.getLogger( getClass().getName());


	@Test
	public void run() throws Exception {

		Assume.assumeTrue( RabbitMqTestUtils.checkRabbitMqIsRunning());

		// Prepare karaf container
		Option[] options = super.config();
		ExamSystem system = PaxExamRuntime.createServerSystem( options );
		TestContainer container = PaxExamRuntime.createContainer( system );
		Assert.assertEquals( KarafTestContainer.class, container.getClass());

		try {
			// Start the DM's distribution... and wait... :(
			container.start();
			ItUtils.waitForDmRestServices( getCurrentPort());

			// The console may take time to be ready
			Thread.sleep( 4000 );

			// Get Karaf directory by Java reflection.
			File karafDirectory = TestUtils.getInternalField( container, "targetFolder", File.class );
			Assert.assertNotNull( karafDirectory );

			File binDirectory = new File( karafDirectory, "bin" );
			Assert.assertTrue( binDirectory.exists());

			Logger execLogger = Logger.getLogger( getClass().getName());
			LogManager.getLogManager().addLogger( execLogger );
			execLogger.setLevel( Level.ALL );

			final StringHandler logHandler = new StringHandler();
			execLogger.addHandler( logHandler );

			// Targets list
			Map<String,String> targets = new HashMap<> ();
			targets.put( "roboconf:target in-memory", "Roboconf :: Target :: In-Memory" );
			targets.put( "roboconf:target openstack", "Roboconf :: Target :: Openstack IaaS" );
			targets.put( "roboconf:target aws", "Roboconf :: Target :: EC2 IaaS" );
			targets.put( "roboconf:target docker", "Roboconf :: Target :: Docker" );
			targets.put( "roboconf:target embedded", "Roboconf :: Target :: Embedded" );
			targets.put( "roboconf:target azure", "Roboconf :: Target :: Azure IaaS" );
			targets.put( "roboconf:target occi", "Roboconf :: Target :: OCCI" );

			// Verify if all targets are deployed
			for( String target : targets.keySet() ) {

				this.logger.info( "Installing " + target + "..." );
				List<String> command = new ArrayList<> ();
				command.add( "/bin/sh" );
				command.add( "client" );
				command.add( target );

				int code = ProgramUtils.executeCommand( execLogger, command, binDirectory, null, null, null );
				if( code != 0 ) {
					System.out.println( "\n\n\n" + logHandler.getLogs() + "\n\n\n" );
				}

				Assert.assertEquals( "Handler for " + target + " failed to be deployed.", 0, code );
				Assert.assertFalse(
						"Handler for " + target + " failed to be deployed (exec).",
						logHandler.getLogs().contains( "Error" ));
			}

			// Verify if all targets are in bundle list
			List<String> cmd = new ArrayList<> ();
			cmd.add( "/bin/sh" );
			cmd.add( "client" );
			cmd.add( "bundle:list" );

			int c = ProgramUtils.executeCommand( execLogger, cmd, binDirectory, null, null, null );
			Assert.assertEquals( 0, c );
			for( String value : targets.values() ) {
				Assert.assertTrue( logHandler.getLogs().contains( value ) );
			}

		} finally {
			container.stop();
		}
	}
}
