/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.karaf.commands.common;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import net.roboconf.core.Constants;

/**
 * @author Vincent Zurczak - Linagora
 */
@Command( scope = "roboconf", name = "info", description="Display information about Roboconf and its environment." )
@Service
public class RoboconfInfoCommand implements Action {

	static final String KARAF_INFO = "info";

	// Picked up from Karaf's "InfoAction" class
	static final int PAD = 25;

	@Reference
	Session session;

	@Reference
	BundleContext ctx;

	// Other fields
	PrintStream out = System.out;


	@Override
	public Object execute() throws Exception {

		// Display information about Roboconf
		List<String> versions = new ArrayList<> ();
		for( Bundle bundle : this.ctx.getBundles()) {
			if( Constants.RBCF_CORE_SYMBOLIC_NAME.equals( bundle.getSymbolicName()))
				versions.add( bundle.getVersion().toString());
		}

		this.out.println();
		this.out.println( "Roboconf" );

		// We format our stuff just like the "info" command.
		// See https://github.com/apache/karaf/blob/master/shell/commands/src/main/java/org/apache/karaf/shell/commands/impl/InfoAction.java
		if( versions.size() > 1 ) {
			this.out.println( "\n[ WARNING ] Several versions of Roboconf run in the server." );
			this.out.println( "[ WARNING ] Please, run bundle:list | grep roboconf-core for more details." );
			this.out.println();

			printValue( "Roboconf versions", versions.get( 0 ));
			for( int i=1; i<versions.size(); i++ )
				printValue( "", versions.get( i ));

		} else if( versions.size() == 1 ) {
			printValue( "Roboconf version", versions.get( 0 ));

		} else {
			printValue( "Roboconf version", "Undetermined" );
		}

		this.out.println();

		// Show Karaf info
		this.session.execute( KARAF_INFO );
		this.out.println();
		return null;
	}


	// Copied from Karaf's "InfoAction" class
	private void printValue(String name, String value) {

		this.out.println(
				"  " + SimpleAnsi.INTENSITY_BOLD + name
				+ SimpleAnsi.INTENSITY_NORMAL
				+ spaces( PAD - name.length())
				+ "   " + value );
	}


	// Copied from Karaf's "InfoAction" class
	private String spaces(int nb) {

		StringBuilder sb = new StringBuilder();
		for( int i=0; i<nb; i++ ) {
			sb.append( ' ' );
		}

		return sb.toString();
	}
}
