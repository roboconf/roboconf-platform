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

package net.roboconf.karaf.commands.dm.targets;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

import junit.framework.Assert;

import org.apache.felix.service.command.CommandSession;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstallTargetCommandTest {

	@Test
	public void testDoExecute_noTarget() throws Exception {

		InstallTargetCommand itc = new InstallTargetCommand();
		OutputStream os = new ByteArrayOutputStream();
		itc.out = new PrintStream( os );

		itc.doExecute();
		Assert.assertTrue( os.toString().contains( "Unknown target" ));
	}


	@Test
	public void testDoExecute_noRoboconfVersion() throws Exception {

		InstallTargetCommand itc = new InstallTargetCommand();
		OutputStream os = new ByteArrayOutputStream();
		itc.out = new PrintStream( os );
		itc.targetName = "openstack";

		itc.doExecute();
		Assert.assertTrue( os.toString().contains( "the Roboconf version" ));
	}


	@Test
	public void testDoExecute_valid() throws Exception {

		// Prepare the command
		InstallTargetCommand itc = new InstallTargetCommand();
		OutputStream os = new ByteArrayOutputStream();
		itc.out = new PrintStream( os );
		itc.targetName = "docker";
		itc.roboconfVersion = "0.5";

		// Mock the session
		final String expected = "bundle:install --start mvn:net.roboconf/roboconf-target-docker/0.5";
		CommandSession session = Mockito.mock( CommandSession.class );
		Mockito.when( session.execute( expected )).thenReturn( null );

		// Inject the session
		for( Class<?> c = itc.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Field field = c.getDeclaredField( "session" );
				field.setAccessible( true );
				field.set( itc, session );

			} catch( NoSuchFieldException e ) {
				// nothing
			}
		}

		// Verify the execution
		itc.doExecute();
		Assert.assertFalse( os.toString().contains( "the Roboconf version" ));
		Assert.assertFalse( os.toString().contains( "Unknown target" ));
		Mockito.verify( session ).execute( expected );
	}
}
