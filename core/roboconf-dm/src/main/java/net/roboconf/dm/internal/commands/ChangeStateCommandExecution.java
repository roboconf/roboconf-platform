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

import net.roboconf.core.commands.ChangeStateCommandInstruction;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class ChangeStateCommandExecution extends AbstractCommandExecution {

	private final ChangeStateCommandInstruction instr;
	private final Manager manager;


	/**
	 * Constructor.
	 * @param instr
	 * @param manager
	 */
	public ChangeStateCommandExecution( ChangeStateCommandInstruction instr, Manager manager ) {
		this.instr = instr;
		this.manager = manager;
	}


	@Override
	public void execute() throws CommandException {

		// Resolve runtime structure
		Instance instance = resolveInstance( this.instr, this.instr.getInstancePath(), false );
		ManagedApplication ma = resolveManagedApplication( this.manager, this.instr );

		// Execute the command
		try {
			this.manager.instancesMngr().changeInstanceState( ma, instance, this.instr.getTargetStatus());

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}
}
