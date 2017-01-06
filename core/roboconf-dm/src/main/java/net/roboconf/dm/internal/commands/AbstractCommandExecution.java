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

import net.roboconf.core.commands.AbstractCommandInstruction;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ICommandsMngr.CommandExecutionContext;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractCommandExecution {

	protected CommandExecutionContext executionContext;


	/**
	 * Executes a command.
	 * @throws CommandException if something went wrong
	 */
	abstract void execute() throws CommandException;


	protected ManagedApplication resolveManagedApplication( Manager manager, AbstractCommandInstruction instr )
	throws CommandException {

		String appName = instr.getApplication().getName();
		ManagedApplication ma = manager.applicationMngr().findManagedApplicationByName( appName );
		if( ma == null )
			throw new CommandException( "Application " + appName + " could not be found." );

		return ma;
	}


	protected Instance resolveInstance( AbstractCommandInstruction instr, String instancePath, boolean nullIsAllowed )
	throws CommandException {

		Instance instance = InstanceHelpers.findInstanceByPath( instr.getApplication(), instancePath );
		if( instance == null ) {
			if( ! nullIsAllowed || instancePath != null )
				throw new CommandException( "Instance " + instancePath + " could not be found." );
		}

		return instance;
	}


	public void setExecutionContext( CommandExecutionContext executionContext ) {
		this.executionContext = executionContext;
	}
}
