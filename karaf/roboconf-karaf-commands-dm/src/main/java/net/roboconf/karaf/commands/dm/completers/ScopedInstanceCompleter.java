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

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;

/**
 * @author Vincent Zurczak - Linagora
 */
@Service
public class ScopedInstanceCompleter implements Completer {

	@Reference
	Manager manager;


	@Override
	public int complete( Session session, CommandLine commandLine, List<String> candidates ) {

		// Scoped instance path should be the third argument, preceded by an application name.
		String applicationName = null;
		int position = commandLine.getCursorArgumentIndex();
		if( position > 0 )
			applicationName = commandLine.getArguments()[ position - 1 ];

		// Find the instances...
		StringsCompleter delegate = new StringsCompleter( false );
		ManagedApplication ma;
		if( ! Utils.isEmptyOrWhitespaces( applicationName )
				&& ( ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName )) != null ) {

			for( Instance inst : InstanceHelpers.findAllScopedInstances( ma.getApplication()))
				delegate.getStrings().add( InstanceHelpers.computeInstancePath( inst ));
		}

		return delegate.complete( session, commandLine, candidates );
	}
}
