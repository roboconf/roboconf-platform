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

import net.roboconf.core.Constants;
import net.roboconf.core.commands.CreateInstanceCommandInstruction;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ICommandsMngr.CommandExecutionContext;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class CreateInstanceCommandExecution extends AbstractCommandExecution {

	private final CreateInstanceCommandInstruction instr;
	private final Manager manager;


	/**
	 * Constructor.
	 * @param instr
	 * @param manager
	 */
	public CreateInstanceCommandExecution( CreateInstanceCommandInstruction instr, Manager manager ) {
		this.instr = instr;
		this.manager = manager;
	}


	@Override
	public void execute() throws CommandException {

		// Resolve runtime structures
		Instance parentInstance = resolveInstance( this.instr, this.instr.getParentInstancePath(), true );
		ManagedApplication ma = resolveManagedApplication( this.manager, this.instr );

		// Verify we can create new VMs in the model
		verify( this.executionContext, this.instr.getComponent());

		// Execute the command
		try {
			Instance instance = new Instance( this.instr.getInstanceName()).component( this.instr.getComponent());
			this.manager.instancesMngr().addInstance( ma, parentInstance, instance );
			update( this.executionContext, instance );

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}


	/**
	 * Verifies a new VM model can be created according to a given context.
	 * @param executionContext
	 * @param component
	 * @throws CommandException
	 */
	public static void verify( CommandExecutionContext executionContext, Component component )
	throws CommandException {

		if( executionContext != null
				&& Constants.TARGET_INSTALLER.equalsIgnoreCase( component.getInstallerName())
				&& executionContext.getMaxVm() > 0
				&& executionContext.getMaxVm() <= executionContext.getGlobalVmNumber().get()
				&& executionContext.isStrictMaxVm())
			throw new CommandException( "The maximum number of VM created by the autonomic has been reached." );
	}


	/**
	 * Updates an instance with context information.
	 * @param executionContext
	 * @param createdInstance
	 */
	public static void update( CommandExecutionContext executionContext, Instance createdInstance ) {

		if( executionContext != null ) {
			createdInstance.data.put( executionContext.getNewVmMarkerKey(), executionContext.getNewVmMarkerValue());
			executionContext.getGlobalVmNumber().incrementAndGet();
			executionContext.getAppVmNumber().incrementAndGet();
		}
	}
}
