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

package net.roboconf.dm.management.api;

import java.io.IOException;
import java.util.List;

import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * An API to manipulate commands in the DM.
 * <p>
 * Commands are only used by applications.
 * Application templates can come with predefined commands, but they will
 * be copied in applications. Commands are stored in application directories, under the
 * "commands" directory.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface ICommandsMngr {

	/**
	 * Creates or updates a command from its instructions.
	 * @param app the associated application
	 * @param commandName the command name (must be unique)
	 * @param commandText the instructions contained in the command (must be valid)
	 * @throws IOException if something went wrong
	 * @see #validate(String)
	 */
	void createOrUpdateCommand( Application app, String commandName, String commandText ) throws IOException;


	/**
	 * Deletes a command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @throws IOException if something went wrong
	 */
	void deleteCommand( Application app, String commandName ) throws IOException;


	/**
	 * Gets the instructions contained by a command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @return the commands content (never null)
	 * @throws IOException if something went wrong
	 */
	String getCommandInstructions( Application app, String commandName ) throws IOException;


	/**
	 * Validates the syntax of command instructions.
	 * @param app the associated application
	 * @param commandText a set of command instructions
	 * @return a non-null map of errors
	 */
	List<ParsingError> validate( Application app, String commandText );


	/**
	 * Executes a command.
	 * @param app the associated application
	 * @param commandName a command name
	 * @throws CommandException if execution failed
	 */
	void execute( Application app, String commandName ) throws CommandException;
}
