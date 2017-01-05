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

package net.roboconf.karaf.commands.dm.targets;

import java.io.PrintStream;
import java.util.logging.Logger;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;

import net.roboconf.core.utils.ManifestUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
@Command( scope = "roboconf", name = "target", description="Installs a target when necessary" )
@Service
public class InstallTargetCommand implements Action {

	@Argument( index = 0, name = "target", description = "The target's name.", required = true, multiValued = false )
	@Completion( TargetCompleter.class )
	String targetName = null;

	@Reference
	private Session session;

	// Other fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	String roboconfVersion;
	PrintStream out = System.out;


	/**
	 * Constructor.
	 */
	public InstallTargetCommand() {
		String bundleVersion = ManifestUtils.findBundleVersion();
		this.roboconfVersion = ManifestUtils.findMavenVersion( bundleVersion );
	}


	@Override
	public Object execute() throws Exception {

		SupportedTarget st = SupportedTarget.which( this.targetName );
		if( st == null ) {
			this.out.println( "Unknown target: " + this.targetName + ". Make sure it is correct or install it manually." );

		} else if( this.roboconfVersion == null ) {
			this.out.println( "Error: the Roboconf version could not be determined." );

		} else for( String cmd : st.findCommands( this.roboconfVersion )) {
			this.logger.fine( "Executing " + cmd + "..." );
			this.out.println( "Executing " + cmd + "..." );
			this.session.execute( cmd );
		}

		return null;
	}
}
