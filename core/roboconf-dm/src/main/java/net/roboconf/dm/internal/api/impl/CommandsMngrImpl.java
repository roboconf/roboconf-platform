/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

import net.roboconf.core.Constants;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.commands.CommandsExecutor;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ICommandsMngr;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 */
public class CommandsMngrImpl implements ICommandsMngr {

	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager
	 */
	public CommandsMngrImpl( Manager manager ) {
		this.manager = manager;
	}


	@Override
	public void createOrUpdateCommand( Application app, String commandName, String commandText ) throws IOException {

		File cmdFile = findCommandFile( app, commandName );
		Utils.createDirectory( cmdFile.getParentFile());
		Utils.writeStringInto( commandText, cmdFile );
	}


	@Override
	public void deleteCommand( Application app, String commandName ) throws IOException {

		File cmdFile = findCommandFile( app, commandName );
		Utils.deleteFilesRecursively( cmdFile );
	}


	@Override
	public List<String> listCommands( Application app ) {

		List<String> result = new ArrayList<> ();
		File cmdDir = new File( app.getDirectory(), Constants.PROJECT_DIR_COMMANDS );

		if( cmdDir.isDirectory()) {
			for( File f : Utils.listAllFiles( cmdDir )) {
				if( f.getName().endsWith( Constants.FILE_EXT_COMMANDS )) {
					String cmdName = f.getName().replace( Constants.FILE_EXT_COMMANDS, "" );
					result.add( cmdName );
				}
			}
		}

		return result;
	}


	@Override
	public String getCommandInstructions( Application app, String commandName ) throws IOException {

		File cmdFile = findCommandFile( app, commandName );
		String result = cmdFile.exists() ? result = Utils.readFileContent( cmdFile ) : "";
		return result;
	}


	@Override
	public List<ParsingError> validate( Application app, String commandText ){

		CommandsParser parser = new CommandsParser( app, commandText );
		return parser.getParsingErrors();
	}


	@Override
	public void execute( Application app, String commandName )
	throws CommandException, NoSuchFileException {
		execute( app, commandName, null );
	}


	@Override
	public void execute( Application app, String commandName, CommandExecutionContext executionContext )
	throws CommandException, NoSuchFileException {

		File cmdFile = findCommandFile( app, commandName );
		if( ! cmdFile.isFile())
			throw new NoSuchFileException( cmdFile.getAbsolutePath());

		CommandsExecutor executor = new CommandsExecutor( this.manager, app, cmdFile, executionContext );
		executor.execute();
	}


	private File findCommandFile( Application app, String commandName ) {

		File cmdDir = new File( app.getDirectory(), Constants.PROJECT_DIR_COMMANDS );
		return new File( cmdDir, commandName + Constants.FILE_EXT_COMMANDS );
	}
}
