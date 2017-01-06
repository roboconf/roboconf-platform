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

package net.roboconf.karaf.commands.dm.diagnostics;

import java.io.PrintStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.karaf.commands.dm.KarafDmCommandsUtils;
import net.roboconf.karaf.commands.dm.KarafDmCommandsUtils.RbcfInfo;
import net.roboconf.karaf.commands.dm.completers.ApplicationCompleter;
import net.roboconf.karaf.commands.dm.completers.ScopedInstanceCompleter;

/**
 * @author Vincent Zurczak - Linagora
 */
@Command( scope = "roboconf", name = "diagnostic-messaging", description="Verifies messaging works" )
@Service
public class DiagnosticMessagingCommand implements Action {

	@Argument( index = 0, name = "application", description = "The application's name", required = true, multiValued = false )
	@Completion( ApplicationCompleter.class )
	String applicationName;

	@Argument( index = 1, name = "scoped instance's path", description = "The scoped instance's path", required = false, multiValued = false )
	@Completion( ScopedInstanceCompleter.class )
	String scopedInstancePath;

	@Reference
	Manager manager;

	// Other fields
	final Logger logger = Logger.getLogger( getClass().getName());
	final Map<String,String> uuidToTarget = new ConcurrentHashMap<> ();
	final Map<String,Boolean> uuidToFound = new ConcurrentHashMap<> ();

	PrintStream out = System.out;
	int waitingDelay = 5;


	@Override
	public Object execute() throws Exception {

		// Check...
		RbcfInfo info = KarafDmCommandsUtils.findInstances( this.manager, this.applicationName, this.scopedInstancePath, this.out );

		// Execute
		if( ! info.getScopedInstances().isEmpty()) {

			DiagnosticListener listener = new DiagnosticListener( this.uuidToFound );
			try {
				this.manager.listenerAppears( listener );

				// DM
				this.out.println( "Pinging the DM..." );
				String uuid = UUID.randomUUID().toString();
				this.uuidToTarget.put( uuid, "The DM" );
				this.manager.debugMngr().pingMessageQueue( uuid );

				// Agents
				for( Instance inst : info.getScopedInstances()) {

					String path = InstanceHelpers.computeInstancePath( inst );
					if( inst.getStatus() != InstanceStatus.DEPLOYED_STARTED ) {
						this.out.println( "Skipping agent " + path + " as it is not deployed and started." );
						continue;
					}

					this.out.println( "Pinging agent " + path );
					uuid = UUID.randomUUID().toString();
					this.uuidToTarget.put( uuid, "Agent " + path );
					this.manager.debugMngr().pingAgent( info.getManagedApplication(), inst, uuid );
				}

				// Wait few seconds maximum
				this.out.println( "" );
				this.out.println( "Waiting few seconds to get PING results..." );
				this.out.println( "" );
				Thread.sleep( this.waitingDelay * 1000 );

			} finally {
				this.manager.listenerDisappears( listener );
				listener = null;

				this.out.println( "" );
				this.out.print( "The " + this.waitingDelay + " second" );
				if( this.waitingDelay > 1 )
					this.out.print( "s" );

				this.out.print( " delay elapsed." );
				this.out.println( "" );

				// Show successes first
				for( Map.Entry<String,String> entry : this.uuidToTarget.entrySet()) {
					Boolean b = this.uuidToFound.get( entry.getKey());
					if( b != null && b )
						this.out.println( "[ SUCCESS ] " + entry.getValue() + " responded to the ping." );
				}

				// And failures then
				this.out.println( "" );
				for( Map.Entry<String,String> entry : this.uuidToTarget.entrySet()) {
					Boolean b = this.uuidToFound.get( entry.getKey());
					if( b == null || ! b )
						this.out.println( "[ FAILURE ] " + entry.getValue() + " did not respond to the ping." );
				}
			}
		}

		return null;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class DiagnosticListener implements IDmListener {

		Map<String,Boolean> uuidToFound;


		/**
		 * Constructor.
		 * @param uuidToFound
		 */
		public DiagnosticListener( Map<String,Boolean> uuidToFound ) {
			this.uuidToFound = uuidToFound;
		}

		@Override
		public String getId() {
			return "karaf-command-to-diagnostic-messaging :: " + UUID.randomUUID().toString();
		}

		@Override
		public void enableNotifications() {
			// nothing
		}

		@Override
		public void disableNotifications() {
			// nothing
		}

		@Override
		public void application( Application application, EventType eventType ) {
			// nothing
		}

		@Override
		public void applicationTemplate( ApplicationTemplate tpl, EventType eventType ) {
			// nothing
		}

		@Override
		public void instance( Instance instance, Application application, EventType eventType ) {
			// nothing
		}

		@Override
		public void raw( String message, Object... data ) {

			String uuid = message.replaceFirst( "^PONG:", "" );
			this.uuidToFound.put( uuid, Boolean.TRUE );
		}
	}
}
