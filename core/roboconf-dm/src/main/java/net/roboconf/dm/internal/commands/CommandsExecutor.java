/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.RoboconfError;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandsExecutor {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,String> context = new HashMap<> ();

	private final File commandsFile;
	private final ManagedApplication ma;
	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager the manager
	 * @param ma a managed application (not null)
	 * @param commandsFile a file containing commands (not null)
	 */
	public CommandsExecutor( Manager manager, ManagedApplication ma, File commandsFile ) {
		this.commandsFile = commandsFile;
		this.ma = ma;
		this.manager = manager;
	}


	public List<RoboconfError> validate() {

		List<RoboconfError> result = new ArrayList<> ();
		try {
			for( ICommandInstruction instr : parse()) {
				RoboconfError error = instr.validate();
				if( error != null )
					result.add( error );
			}

		} catch( IOException e ) {
			result.add( null );

		} catch( CommandException e ) {
			result.add( null );
		}

		return result;
	}


	public List<RoboconfError> execute() throws CommandException {

		List<RoboconfError> result = new ArrayList<> ();
		try {
			for( ICommandInstruction instr : parse()) {
				if( instr.validate() == null )
					instr.execute();
			}

		} catch( CommandException e ) {
			throw e;

		} catch( Exception e ) {
			throw new CommandException( e );
		}

		return result;
	}


	private List<ICommandInstruction> parse() throws IOException, CommandException {

		// We assume these files are not that big.
		String fileContent = Utils.readFileContent( this.commandsFile );

		// Allow line breaks in commands.
		fileContent = fileContent.replace( "\\\n\\s+", "\n " );

		// Parse line by line.
		List<ICommandInstruction> result = new ArrayList<ICommandInstruction> ();
		for( String line : fileContent.split( "\n" )) {

			// Skip empty lines
			if( Utils.isEmptyOrWhitespaces( line ))
				continue;

			// Update the line with variables


			// Find the instruction
			ICommandInstruction instr = parse( line );
			if( instr == null ) {
				this.logger.severe( "An invalid instruction was found in " + this.commandsFile.getName() + ": " + line );
				throw new CommandException( "Invalid instruction found: " + line );
			}

			result.add( instr );
		}

		return result;
	}


	private ICommandInstruction parse( String line ) {

		ICommandInstruction result = null;
		String toLowerCase = line.toLowerCase();
		if( toLowerCase.startsWith( AssociateTargetCommandInstruction.PREFIX ))
			result = new AssociateTargetCommandInstruction( this.ma, this.manager, line );

		else if( toLowerCase.startsWith( ChangeStateCommandInstruction.PREFIX ))
			result = new ChangeStateCommandInstruction( this.ma, this.manager, line );

		else if( BulkCommandInstructions.isBulkInstruction( toLowerCase ))
			result = new BulkCommandInstructions( this.ma, this.manager, line );

		else if( toLowerCase.startsWith( EmailCommandInstruction.PREFIX ))
			result = new EmailCommandInstruction( this.manager, line );

		else if( toLowerCase.startsWith( DefineVariableCommandInstruction.PREFIX ))
			result = new DefineVariableCommandInstruction( this.ma, line, this.context );

		else if( toLowerCase.startsWith( CreateInstanceCommandInstruction.PREFIX ))
			result = new CreateInstanceCommandInstruction( this.ma, this.manager, line );

		else if( toLowerCase.startsWith( ReplicateCommandInstruction.PREFIX ))
			result = new ReplicateCommandInstruction( this.ma, this.manager, line );

		else if( toLowerCase.startsWith( RenameCommandInstruction.PREFIX ))
			result = new RenameCommandInstruction( this.ma, line );

		return result;
	}
}
