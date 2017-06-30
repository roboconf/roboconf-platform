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

import static net.roboconf.core.errors.ErrorDetails.name;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ExecuteCommandInstruction extends AbstractCommandInstruction {

	static final String PREFIX = "execute";
	private String commandName;


	/**
	 * Constructor.
	 * @param context
	 * @param instruction
	 * @param line
	 */
	ExecuteCommandInstruction( Context context, String instruction, int line ) {
		super( context, instruction, line );

		Pattern p = Pattern.compile( PREFIX + "\\s+(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.commandName = m.group( 1 ).trim();
			if( this.commandName.endsWith( Constants.FILE_EXT_COMMANDS ))
				this.commandName = this.commandName.substring( 0, this.commandName.lastIndexOf( '.' ));
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#doValidate()
	 */
	@Override
	public List<ParsingError> doValidate() {

		String fileName = this.commandName + Constants.FILE_EXT_COMMANDS;
		File commandsDirectory = new File( this.context.getApp().getDirectory(), Constants.PROJECT_DIR_COMMANDS );
		File commandFileToExecute;

		List<ParsingError> result = new ArrayList<> ();
		if( Utils.isEmptyOrWhitespaces( this.commandName )) {
			result.add( error( ErrorCode.CMD_MISSING_COMMAND_NAME ));
		}

		// Prevent a commands file from invoking itself recursively
		// If the command was loaded from a file...
		else if( this.context.getCommandFile() != null
				&& fileName.equals( this.context.getCommandFile().getName())) {

			result.add( error( ErrorCode.CMD_LOOPING_COMMAND, name( this.commandName )));
		}

		// The commands file to execute must exist.
		else if( ! ( commandFileToExecute = new File( commandsDirectory, fileName )).exists()) {
			result.add( error( ErrorCode.CMD_INEXISTING_COMMAND, name( this.commandName )));
		}

		// If it exists, we do not want it to contain the same instruction.
		// Can happen if we execute a commands (not loaded from a file) that executes
		// a commands file with the same instruction...
		else {
			try {
				String content = Utils.readFileContent( commandFileToExecute );
				Pattern p = Pattern.compile( PREFIX + "\\s+" + Pattern.quote( this.commandName ), Pattern.CASE_INSENSITIVE );
				if( p.matcher( content ).find())
					result.add( error( ErrorCode.CMD_NASTY_LOOPING_COMMAND, name( this.commandName )));

			} catch( IOException e ) {
				// If we cannot load the file's content, do not push the validation further...
				Utils.logException( Logger.getLogger( getClass().getName()), e );
			}
		}

		return result;
	}


	/**
	 * @return the commandName
	 */
	public String getCommandName() {
		return this.commandName;
	}
}
