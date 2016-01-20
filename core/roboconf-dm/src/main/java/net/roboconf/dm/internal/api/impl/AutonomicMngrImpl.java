/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import net.roboconf.core.autonomic.Rule;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.impl.beans.AutonomicApplicationContext;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IAutonomicMngr;
import net.roboconf.dm.management.api.ICommandsMngr;
import net.roboconf.dm.management.api.ICommandsMngr.CommandExecutionContext;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class AutonomicMngrImpl implements IAutonomicMngr {

	static final String AUTONOMIC_MARKER = "autonomic";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,AutonomicApplicationContext> appNameToContext = new ConcurrentHashMap<> ();
	private final AtomicInteger autonomicVmCount = new AtomicInteger( 0 );

	private final ICommandsMngr commandsMngr;
	private final IPreferencesMngr preferencesMngr;


	/**
	 * Constructor.
	 * @param commandsMngr the commands manager
	 */
	public AutonomicMngrImpl( ICommandsMngr commandsMngr, IPreferencesMngr preferencesMngr ) {
		this.commandsMngr = commandsMngr;
		this.preferencesMngr = preferencesMngr;

		// FIXME: how to restore the number of "autonomic" VM when the DM restarts?
	}


	@Override
	public void loadApplicationRules( Application app ) {
		// TODO Auto-generated method stub

	}


	@Override
	public void refreshApplicationRules( Application app, String ruleName ) {
		// TODO Auto-generated method stub

	}


	@Override
	public void unloadApplicationRules( Application app ) {
		// TODO Auto-generated method stub

	}


	@Override
	public void notifyVmWasDeletedByHand( Instance rootInstance ) {

		// For the record, the instance was already removed from the model.
		if( rootInstance.data.remove( AUTONOMIC_MARKER ) != null )
			this.autonomicVmCount.decrementAndGet();
	}


	@Override
	public int getAutonomicInstancesCount() {
		return this.autonomicVmCount.get();
	}


	@Override
	public void handleEvent( ManagedApplication ma, MsgNotifAutonomic event ) {

		try {
			// Register the event
			this.logger.fine( "Autonomic event '" + event.getEventName() + "' is about to be recorded." );

			// Find the rules that are impacted by the current event registration
			AutonomicApplicationContext ctx = this.appNameToContext.get( ma.getName());
			if( ctx == null ) {
				this.logger.fine( "No autonomic context was found for application " + ma.getApplication() + "." );

			} else {
				ctx.eventNameToLastRecordTime.put( event.getEventName(), new Date().getTime());
				List<Rule> rulesToExecute = ctx.findRulesToExecute();
				if( rulesToExecute.isEmpty()) {
					this.logger.fine( "No rule was found after the event '" + event.getEventName() + "' occurred." );

				} else {
					// Prepare the (shared and read-only) execution context
					String strictMaxVmAS = this.preferencesMngr.get( IPreferencesMngr.AUTONOMIC_STRICT_MAX_VM_NUMBER, "true" );
					boolean strictMaxVm = Boolean.parseBoolean( strictMaxVmAS );

					String maxVmCountAS = this.preferencesMngr.get( IPreferencesMngr.AUTONOMIC_MAX_VM_NUMBER, "" + Integer.MAX_VALUE );
					int maxVmCount = Integer.parseInt( maxVmCountAS );
					if( maxVmCount < 0 )
						maxVmCount = Integer.MAX_VALUE;

					// If the upper-limit of created VM was reached, override the "strict" property.
					// This way, if a command tries to create a new VM, it will fail.
					// At the moment, we do not know whether VMs will be created if we go on.
					// If so, this check guarantees no additional one will be created.
					if( maxVmCount <= this.autonomicVmCount.get())
						strictMaxVm = true;

					// Complete the execution context
					CommandExecutionContext execCtx = new CommandExecutionContext(
							this.autonomicVmCount, maxVmCount, strictMaxVm,
							AUTONOMIC_MARKER, "" );

					// Process the rules
					for( Rule rule : rulesToExecute ) {
						this.logger.fine( "Applying rule '" + rule.getRuleName() + "' for event '" + event.getEventName() + "'." );
						ctx.recordPreExecution( rule.getRuleName());
						for( String commandName : rule.getCommandsToInvoke())
							this.commandsMngr.execute( ma.getApplication(), commandName, execCtx );
					}
				}
			}

		} catch( Exception e ) {
			this.logger.warning( "An autonomic event could not be handled. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}
}
