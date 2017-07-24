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

package net.roboconf.core.commands;

import static net.roboconf.core.errors.ErrorDetails.instruction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandsParser {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final List<ParsingError> parsingErrors = new ArrayList<> ();
	private final Context context;

	final List<AbstractCommandInstruction> instructions = new ArrayList<> ();



	/**
	 * Constructor.
	 * @param app an application (not null)
	 * @param commandsFile a file containing commands (not null)
	 */
	public CommandsParser( AbstractApplication app, File commandsFile ) {
		this.context = new Context( app, commandsFile );
		parse();
	}


	/**
	 * Constructor.
	 * @param app an application (not null)
	 * @param instructionsText instructions text (not null)
	 */
	public CommandsParser( AbstractApplication app, String instructionsText ) {
		this.context = new Context( app, null );
		if( instructionsText != null )
			parse( instructionsText );
	}


	/**
	 * @return a non-null list of errors
	 */
	public List<ParsingError> getParsingErrors() {

		List<ParsingError> result = new ArrayList<>( this.parsingErrors );
		if( this.context.getCommandFile() != null && ! this.context.getCommandFile().exists())
			result.add( 0, new ParsingError( ErrorCode.CMD_INEXISTING_COMMAND_FILE, this.context.getCommandFile(), 1 ));
		else if( this.instructions.isEmpty())
			result.add( 0, new ParsingError( ErrorCode.CMD_NO_INSTRUCTION, this.context.getCommandFile(), 1 ));

		return result;
	}


	/**
	 * @return the instructions
	 */
	public List<AbstractCommandInstruction> getInstructions() {
		return this.instructions;
	}


	/**
	 * Injects (valid) context variables in commands text.
	 * @param line a non-null line
	 * @param context a non-null map considered as the context
	 * @return the line, after it was updated
	 * <p>
	 * All the <code>$(sth)</code> variables will have been replaced by the value
	 * associated with the <i>sth</i> key in <code>context</code>.
	 * </p>
	 */
	public static String injectContextVariables( String line, Map<String,String> context ) {

		String result = line;
		for( Map.Entry<String,String> entry : context.entrySet())
			result = result.replace( "$(" + entry.getKey() + ")", entry.getValue());

		return result;
	}


	/**
	 * Parses the whole file and extracts instructions.
	 */
	private void parse() {

		try {
			// We assume these files are not that big.
			String fileContent = Utils.readFileContent( this.context.getCommandFile());
			parse( fileContent );

		} catch( IOException e ) {
			this.logger.severe( "A commands file could not be read. File path: " + this.context.getName());
		}
	}


	/**
	 * Parses the whole file and extracts instructions.
	 * @param instructionsText a non-null string to parse
	 */
	private void parse( String instructionsText ) {

		// Allow line breaks in commands. But we must keep the lines count.
		// So, we replace escaped line breaks by a particular separator.
		// They will be used to count lines.
		final String sep = "!@!";
		instructionsText = instructionsText.replaceAll( "\\\\\n\\s*", sep );

		// Parse line by line.
		int lineNumber = 0;
		for( String string : Utils.splitNicely( instructionsText, "\n" ) ) {

			String line = string.trim();
			lineNumber ++;

			// Remove comments
			line = line.replaceFirst( "#.*", "" );

			// Skip empty lines
			if( line.isEmpty())
				continue;

			// Update lines count
			int lineLength = line.length();
			line = line.replace( sep, "" );
			int lineCountOffset = (lineLength - line.length()) / sep.length();

			// Verify disabled variables.
			boolean disabled = false;
			for( String disabledVariableName : this.context.disabledVariables ) {
				if( line.contains( "$(" + disabledVariableName + ")" )) {
					disabled = true;
					break;
				}
			}

			// Update the line with variables
			line = injectContextVariables( line, this.context.variables );

			// Find the instruction
			AbstractCommandInstruction instr = parse( line, lineNumber );
			if( instr != null ) {
				instr.setDisabled( disabled );

				// Being disabled should not make the validation fail.
				// Query variables that were not resolved were "mocked" for the validation.
				List<ParsingError> errors = instr.validate();
				if( errors.isEmpty())
					instr.updateContext();
				else
					this.parsingErrors.addAll( errors );

				this.instructions.add( instr );

			} else {
				this.logger.severe( "An invalid instruction was found in " + this.context.getName() + ": " + line );
				this.parsingErrors.add( new ParsingError(
						ErrorCode.CMD_UNRECOGNIZED_INSTRUCTION,
						this.context.getCommandFile(),
						lineNumber,
						instruction( line )));
			}

			// Update the line number
			lineNumber += lineCountOffset;
		}
	}


	/**
	 * Parses a single line and extracts an instructions when possible.
	 * @param line a text line
	 * @param lineNumber the line number
	 * @return an instruction, or null if none could be recognized
	 */
	private AbstractCommandInstruction parse( String line, int lineNumber ) {

		AbstractCommandInstruction result = null;
		String toLowerCase = line.toLowerCase();
		if( toLowerCase.startsWith( AssociateTargetCommandInstruction.PREFIX ))
			result = new AssociateTargetCommandInstruction( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( ChangeStateCommandInstruction.PREFIX ))
			result = new ChangeStateCommandInstruction( this.context, line, lineNumber );

		else if( BulkCommandInstructions.isBulkInstruction( toLowerCase ))
			result = new BulkCommandInstructions( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( EmailCommandInstruction.PREFIX ))
			result = new EmailCommandInstruction( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( DefineVariableCommandInstruction.PREFIX ))
			result = new DefineVariableCommandInstruction( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( CreateInstanceCommandInstruction.PREFIX ))
			result = new CreateInstanceCommandInstruction( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( ReplicateCommandInstruction.PREFIX ))
			result = new ReplicateCommandInstruction( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( RenameCommandInstruction.PREFIX ))
			result = new RenameCommandInstruction( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( WriteCommandInstruction.WRITE_PREFIX ))
			result = new WriteCommandInstruction( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( AppendCommandInstruction.APPEND_PREFIX ))
			result = new AppendCommandInstruction( this.context, line, lineNumber );

		else if( toLowerCase.startsWith( ExecuteCommandInstruction.PREFIX ))
			result = new ExecuteCommandInstruction( this.context, line, lineNumber );

		return result;
	}
}
