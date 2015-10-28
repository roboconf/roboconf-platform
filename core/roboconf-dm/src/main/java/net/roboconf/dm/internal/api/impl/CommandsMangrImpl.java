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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.api.ICommandsMangr;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 */

public class CommandsMangrImpl implements ICommandsMangr{

	/**
	 * Creates a command from its instructions.
	 * @param app the associated application
	 * @param commandName the command name (must be unique)
	 * @param commandText the instructions contained in the command (must be valid)
	 * @throws IOException if something went wrong
	 * @see #validate(String)
	 */

	@Override
	public void createCommand( Application app, String commandName, String commandText ) throws IOException{

		File appDir = app.getDirectory();
		File cmdDir = new File(appDir.getAbsolutePath()+"/"+"commands");
		if(!cmdDir.isDirectory()){
			Utils.createDirectory(cmdDir);
		}
		File cmd = new File(cmdDir.getAbsolutePath()+"/"+commandName+".command");
		Utils.writeStringInto(commandText, cmd);
	}


	/**
	 * Creates a command from a command file.
	 * @param app the associated application
	 * @param commandName the command name (must be unique)
	 * @param commandFile a file with command instructions (that must be valid)
	 * @throws IOException if something went wrong
	 * @see #validate(String)
	 */
	@Override
	public void createCommand( Application app, String commandName, File commandFile ) throws IOException{

		File appDir = app.getDirectory();
		File cmdDir = new File(appDir.getAbsolutePath()+"/"+"commands");
		if(!cmdDir.isDirectory()){
			Utils.createDirectory(cmdDir);
		}
		File cmd = new File(cmdDir.getAbsolutePath()+"/"+commandName+".command");
		Utils.copyStream(commandFile, cmd);
	}


	/**
	 * Deletes a command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @throws IOException if something went wrong
	 */
	@Override
	public void deleteCommand( Application app, String commandName ) throws IOException{
		File appDir = app.getDirectory();
		File cmdDel = new File(appDir.getAbsolutePath()+"/"+"commands"+"/"+commandName+".command");
		Logger logger = Logger.getLogger( CommandsMangrImpl.class.getName());
		if(cmdDel.delete()){
			logger.finer("The command "+commandName+" was deleted");
		}else{
			logger.fine(commandName+": No such command");
		}
	}


	/**
	 * Updates the instructions of a command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @param commandText the new command instructions (must be valid)
	 * @throws IOException if something went wrong
	 */
	@Override
	public void updateCommand( Application app, String commandName, String commandText ) throws IOException{
		createCommand(app,commandName,commandText);
	}


	/**
	 * Gets the instructions contained by a command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @return the commands content (never null)
	 * @throws IOException
	 */
	@Override
	public String getCommandInstructions( Application app, String commandName ) throws IOException{
		File appDir = app.getDirectory();
		File cmd = new File(appDir.getAbsolutePath()+"/"+"commands"+"/"+commandName+".command");
		Logger logger = Logger.getLogger( CommandsMangrImpl.class.getName());
		String result = "";
		if(cmd.exists()){
			result = Utils.readFileContent(cmd);
		}
		if(result.isEmpty()){
			logger.fine(commandName+" contains no instructions");
		}
		return result;
	}


	/**
	 * Validates the syntax of command instructions.
	 * @param commandText a set of command instructions
	 * @return true if it is valid, false otherwise
	 */
	@Override
	public boolean validate( String commandText ){
		return false;
	}


	/**
	 * Executes a command.
	 * @param app the associated application
	 * @param commandName a command name
	 * @throws IOException if something went wrong
	 */
	@Override
	public void execute( Application app, String commandName ) throws IOException{

	}

}