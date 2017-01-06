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

import static java.util.logging.Level.ALL;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.OFF;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * @author Vincent Zurczak - Linagora
 */
@Service
public class LogLevelCompleter implements Completer {

	static final Level[] LEVELS = new Level[] {
		ALL, CONFIG, FINE, FINER, FINEST, INFO, OFF, SEVERE, WARNING
	};


	@Override
	public int complete( Session session, CommandLine commandLine, List<String> candidates ) {

		StringsCompleter delegate = new StringsCompleter( false );
		for( Level level : LEVELS )
			delegate.getStrings().add( level.toString());

		return delegate.complete( session, commandLine, candidates );
	}


	/**
	 * @return a non-null string listing all the possible log levels
	 */
	public static String all() {

		StringBuilder sb = new StringBuilder();
		for( Iterator<Level> it = Arrays.asList( LEVELS ).iterator(); it.hasNext(); ) {
			sb.append( it.next());
			if( it.hasNext())
				sb.append( ", " );
		}

		return sb.toString();
	}
}
