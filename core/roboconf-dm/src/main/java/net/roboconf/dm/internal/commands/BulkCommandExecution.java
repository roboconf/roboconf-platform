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

package net.roboconf.dm.internal.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.roboconf.core.commands.BulkCommandInstructions;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class BulkCommandExecution extends AbstractCommandExecution {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final BulkCommandInstructions instr;
	private final Manager manager;


	/**
	 * Constructor.
	 * @param instr
	 * @param manager
	 */
	public BulkCommandExecution( BulkCommandInstructions instr, Manager manager ) {
		this.instr = instr;
		this.manager = manager;
	}


	@Override
	public void execute() throws CommandException {

		// Resolve runtime structure
		ManagedApplication ma = resolveManagedApplication( this.manager, this.instr );
		List<Instance> instances = new ArrayList<> ();
		if( this.instr.getInstancePath() != null ) {
			Instance instance = resolveInstance( this.instr, this.instr.getInstancePath(), false );
			instances.add( instance );

		} else {
			instances.addAll( InstanceHelpers.findInstancesByComponentName( this.instr.getApplication(), this.instr.getComponentName()));
		}

		// Execute the command
		try {
			switch( this.instr.getChangeStateInstruction()) {
			case DEPLOY_AND_START_ALL:
				for( Instance inst : instances )
					this.manager.instancesMngr().deployAndStartAll( ma, inst );

				break;

			case STOP_ALL:
				for( Instance inst : instances )
					this.manager.instancesMngr().stopAll( ma, inst );

				break;

			case UNDEPLOY_ALL:
				for( Instance inst : instances )
					this.manager.instancesMngr().undeployAll( ma, inst );

				break;

			case DELETE:
				for( Instance inst : instances )
					this.manager.instancesMngr().removeInstance( ma, inst, false );

				break;
			}

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}
}
