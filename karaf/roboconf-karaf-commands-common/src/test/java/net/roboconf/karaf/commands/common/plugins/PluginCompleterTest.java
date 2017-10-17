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

package net.roboconf.karaf.commands.common.plugins;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginCompleterTest {

	@Test
	public void testComplete() {

		// Mock what is required
		Session session = Mockito.mock( Session.class );
		CommandLine commandLine = Mockito.mock( CommandLine.class );
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 0 );

		// Nothing typed in yet
		PluginCompleter completer = new PluginCompleter();
		List<String> candidates = new ArrayList<> ();

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( 0, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( SupportedPlugin.allString().size(), candidates.size());

		// Complete "f"
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "f" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 1 ); // "f".length()

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( SupportedPlugin.FILE.toString().toLowerCase(), candidates.get( 0 ).trim());

		// Complete "FI" (case insensitive)
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "FI" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( 2 ); // "FI".length()

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -2, completer.complete( session, commandLine, candidates ));
		Assert.assertEquals( 1, candidates.size());
		Assert.assertEquals( SupportedPlugin.FILE.toString().toLowerCase(), candidates.get( 0 ).trim());

		// Unknown
		candidates = new ArrayList<> ();
		Mockito.when( commandLine.getCursorArgument()).thenReturn( "unknown" );
		Mockito.when( commandLine.getArgumentPosition()).thenReturn( "unknown".length());

		Assert.assertEquals( 0, candidates.size());
		Assert.assertEquals( -1, completer.complete( session, commandLine, candidates ));
	}
}
