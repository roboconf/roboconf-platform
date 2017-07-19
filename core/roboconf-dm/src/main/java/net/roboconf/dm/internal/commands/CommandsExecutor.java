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

import java.io.File;
import java.util.logging.Logger;

import net.roboconf.core.commands.AbstractCommandInstruction;
import net.roboconf.core.commands.AppendCommandInstruction;
import net.roboconf.core.commands.AssociateTargetCommandInstruction;
import net.roboconf.core.commands.BulkCommandInstructions;
import net.roboconf.core.commands.ChangeStateCommandInstruction;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.commands.CreateInstanceCommandInstruction;
import net.roboconf.core.commands.EmailCommandInstruction;
import net.roboconf.core.commands.ExecuteCommandInstruction;
import net.roboconf.core.commands.RenameCommandInstruction;
import net.roboconf.core.commands.ReplicateCommandInstruction;
import net.roboconf.core.commands.WriteCommandInstruction;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.beans.Application;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ICommandsMngr.CommandExecutionContext;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandsExecutor {

	private final Logger logger = Logger.getLogger( getClass().getName());

	private final File commandsFile;
	private final Application app;
	private final Manager manager;
	private final CommandExecutionContext executionContext;
	private boolean instructionSkipped = false;


	/**
	 * Constructor.
	 * @param manager the manager
	 * @param app an application (not null)
	 * @param commandsFile a file containing commands (not null)
	 */
	public CommandsExecutor( Manager manager, Application app, File commandsFile ) {
		this( manager, app, commandsFile, null );
	}


	/**
	 * Constructor.
	 * @param manager the manager
	 * @param app an application (not null)
	 * @param commandsFile a file containing commands (not null)
	 * @param executionContext an execution context
	 */
	public CommandsExecutor(
			Manager manager,
			Application app,
			File commandsFile,
			CommandExecutionContext executionContext ) {

		this.commandsFile = commandsFile;
		this.app = app;
		this.manager = manager;
		this.executionContext = executionContext;
	}


	/**
	 * Executes a set of commands.
	 * <p>
	 * It is assumed that {@link #validate()} was invoked first and was
	 * successful.
	 * </p>
	 *
	 * @throws CommandException if something went wrong
	 */
	public void execute() throws CommandException {

		try {
			CommandsParser parser = new CommandsParser( this.app, this.commandsFile );
			if( RoboconfErrorHelpers.containsCriticalErrors( parser.getParsingErrors()))
				throw new CommandException( "Invalid command file. " + this.commandsFile.getName() + " contains errors." );

			for( AbstractCommandInstruction instr : parser.getInstructions()) {
				if( instr.isDisabled()) {
					this.logger.fine( "Skipping disabled instruction: " + instr.getClass().getSimpleName());
					this.instructionSkipped = true;
					continue;
				}

				AbstractCommandExecution executor = findExecutor( instr );
				if( executor == null ) {
					this.logger.fine( "Skipping non-executable instruction: " + instr.getClass().getSimpleName());
					continue;
				}

				executor.setExecutionContext( this.executionContext );
				executor.execute();
			}

		} catch( CommandException e ) {
			throw e;

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}


	/**
	 * @return true if one or several instructions were skipped during the execution
	 */
	public boolean wereInstructionSkipped() {
		return this.instructionSkipped;
	}


	/**
	 * Finds the right executor for an instruction.
	 * @param instr a non-null instruction
	 * @return an executor, or null if the instruction is not executable
	 */
	AbstractCommandExecution findExecutor( AbstractCommandInstruction instr ) {

		AbstractCommandExecution result = null;

		if( RenameCommandInstruction.class.equals( instr.getClass()))
			result = new RenameCommandExecution((RenameCommandInstruction) instr);

		else if( ReplicateCommandInstruction.class.equals( instr.getClass()))
			result = new ReplicateCommandExecution((ReplicateCommandInstruction) instr, this.manager );

		else if( AssociateTargetCommandInstruction.class.equals( instr.getClass()))
			result = new AssociateTargetCommandExecution((AssociateTargetCommandInstruction) instr, this.manager );

		else if( BulkCommandInstructions.class.equals( instr.getClass()))
			result = new BulkCommandExecution((BulkCommandInstructions) instr, this.manager );

		else if( ChangeStateCommandInstruction.class.equals( instr.getClass()))
			result = new ChangeStateCommandExecution((ChangeStateCommandInstruction) instr, this.manager );

		else if( CreateInstanceCommandInstruction.class.equals( instr.getClass()))
			result = new CreateInstanceCommandExecution((CreateInstanceCommandInstruction) instr, this.manager );

		else if( EmailCommandInstruction.class.equals( instr.getClass()))
			result = new EmailCommandExecution((EmailCommandInstruction) instr, this.manager );

		else if( WriteCommandInstruction.class.equals( instr.getClass()))
			result = new WriteCommandExecution((WriteCommandInstruction) instr);

		else if( AppendCommandInstruction.class.equals( instr.getClass()))
			result = new AppendCommandExecution((AppendCommandInstruction) instr);

		else if( ExecuteCommandInstruction.class.equals( instr.getClass()))
			result = new ExecuteCommandExecution((ExecuteCommandInstruction) instr, this.manager );

		return result;
	}
}
