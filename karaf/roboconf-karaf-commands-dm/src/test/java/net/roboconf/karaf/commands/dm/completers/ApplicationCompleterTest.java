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

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IApplicationMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationCompleterTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testComplete() throws Exception {

		// Mock what is required
		Session session = Mockito.mock( Session.class );
		CommandLine commandLine = Mockito.mock( CommandLine.class );
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 0 );

		final List<ManagedApplication> mas = new ArrayList<> ();
		IApplicationMngr applicationMngr = Mockito.mock( IApplicationMngr.class );
		Mockito.when( applicationMngr.getManagedApplications()).thenReturn( mas );

		Manager manager = Mockito.mock( Manager.class );
		Mockito.when( manager.applicationMngr()).thenReturn( applicationMngr );

		ApplicationTemplate tpl = new ApplicationTemplate( "whatever" ).directory( this.folder.newFolder());

		// Nothing typed in yet
		ApplicationCompleter completer = new ApplicationCompleter();
		completer.manager = manager;
		List<String> candidates = new ArrayList<> ();

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 0, candidates.size());

		// Complete "I"
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "I" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 1 ); // "I".length()

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 0, candidates.size());

		mas.add( new ManagedApplication( new Application( "Ito", tpl )));
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( "Ito", candidates.get( 0 ).trim());

		// Complete "i" (case insensitive)
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "i" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 1 ); // "i".length()

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( "Ito", candidates.get( 0 ).trim());

		// Try with several apps - and still "i"
		mas.add( new ManagedApplication( new Application( "uto", tpl )));
		mas.add( new ManagedApplication( new Application( "ito2", tpl )));
		mas.add( new ManagedApplication( new Application( "as", tpl )));

		candidates = new ArrayList<> ();
		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 2, candidates.size());
		Assert.assertEquals( "Ito", candidates.get( 0 ).trim());
		Assert.assertEquals( "ito2", candidates.get( 1 ).trim());

		// Better search
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "ito2" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 4 ); // "ito2".length()

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -4, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( "ito2", candidates.get( 0 ).trim());

		// Unknown
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "unknown" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( "unknown".length());

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
	}
}
