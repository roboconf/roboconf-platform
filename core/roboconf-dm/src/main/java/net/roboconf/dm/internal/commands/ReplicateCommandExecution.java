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

import net.roboconf.core.commands.ReplicateCommandInstruction;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class ReplicateCommandExecution extends AbstractCommandExecution {

	private final ReplicateCommandInstruction instr;
	private final Manager manager;


	/**
	 * Constructor.
	 * @param instr
	 * @param manager
	 */
	public ReplicateCommandExecution( ReplicateCommandInstruction instr, Manager manager ) {
		this.instr = instr;
		this.manager = manager;
	}


	@Override
	public void execute() throws CommandException {

		// Resolve runtime structure
		Instance rootInstance = resolveInstance( this.instr, this.instr.getReplicatedInstancePath(), true );
		ManagedApplication ma = resolveManagedApplication( this.manager, this.instr );

		// Verify we can create new VMs in the model
		CreateInstanceCommandExecution.verify( this.executionContext, rootInstance.getComponent());

		try {
			// Copy the instance
			Instance copy = InstanceHelpers.replicateInstance( rootInstance );
			copy.setName( this.instr.getNewInstanceName());
			this.manager.instancesMngr().addInstance( ma, null, copy );

			// Associate this new instance with the same target, if it has one
			String targetId = this.manager.targetsMngr().findTargetId( ma.getApplication(), "/" + rootInstance.getName(), true );
			if( targetId != null )
				this.manager.targetsMngr().associateTargetWith( targetId, ma.getApplication(), "/" + copy.getName());

			// Register meta-data
			CreateInstanceCommandExecution.update( this.executionContext, copy );

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}
}
