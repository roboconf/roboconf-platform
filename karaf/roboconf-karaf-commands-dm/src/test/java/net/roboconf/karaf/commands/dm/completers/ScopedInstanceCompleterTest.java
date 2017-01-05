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

package net.roboconf.karaf.commands.dm.completers;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ScopedInstanceCompleterTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testComplete() throws Exception {

		TestApplication app = new TestApplication();
		app.directory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		// Mock what is required
		Session session = Mockito.mock( Session.class );
		CommandLine commandLine = Mockito.mock( CommandLine.class );
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "" );
		Mockito.when( commandLine.getCursorArgumentIndex()).thenReturn( 0 );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 0 );
		Mockito.when( commandLine.getArguments()).thenReturn( new String[ 0 ]);

		IApplicationMngr applicationMngr = Mockito.mock( IApplicationMngr.class );
		Mockito.when( applicationMngr.findManagedApplicationByName( app.getName())).thenReturn( ma );

		Manager manager = Mockito.mock( Manager.class );
		Mockito.when( manager.applicationMngr()).thenReturn( applicationMngr );

		// Nothing typed in yet
		ScopedInstanceCompleter completer = new ScopedInstanceCompleter();
		completer.manager = manager;
		List<String> candidates = new ArrayList<> ();

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 0, candidates.size());

		// Completion but no application
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "/my" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 3 );	// "/my".length()

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 0, candidates.size());

		// Completion with an invalid application
		Mockito.when( commandLine.getArguments()).thenReturn( new String[] { "inalid" });
		Mockito.when( commandLine.getCursorArgumentIndex()).thenReturn( 1 );

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 0, candidates.size());

		// Completion with an application
		Mockito.when( commandLine.getArguments()).thenReturn( new String[] { app.getName()});
		Mockito.when( commandLine.getCursorArgumentIndex()).thenReturn( 1 );

		Assert.assertEquals( -3, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( "/mysql-vm", candidates.get( 0 ).trim());

		// Complete "i" (case insensitive)
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "/MY" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 3 );	// "/MY".length()

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -3, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( "/mysql-vm", candidates.get( 0 ).trim());

		// Unknown
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "unknown" );

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
	}
}
