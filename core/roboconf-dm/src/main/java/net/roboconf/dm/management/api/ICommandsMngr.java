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

package net.roboconf.dm.management.api;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.runtime.CommandHistoryItem;
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
	 * Lists available commands.
	 * @param app an application name
	 * @return a non-null list
	 */
	List<String> listCommands( Application app );


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
	 * <p>
	 * Equivalent to <code>execute( app, commandName, null )</code>
	 * </p>
	 *
	 * @param app the associated application
	 * @param commandName a command name (the file extension is optional)
	 * @param origin who is executing the command (see {@link CommandHistoryItem} constants)
	 * <p>
	 * It is possible to use a different number. It is up to the client to interpret
	 * the meaning of the origin anyway.
	 * </p>
	 *
	 * @param originDetails execution context (e.g. job name for the scheduler)
	 * @throws CommandException if execution failed
	 * @throws NoSuchFileException if there is no such command
	 */
	void execute( Application app, String commandName, int origin, String originDetails )
	throws CommandException, NoSuchFileException;


	/**
	 * Executes a command.
	 * @param app the associated application
	 * @param commandName a command name (the file extension is optional)
	 * @param executionContext an execution context to define some constraints on commands
	 * @param origin who is executing the command (see {@link CommandHistoryItem} constants)
	 * <p>
	 * It is possible to use a different number. It is up to the client to interpret
	 * the meaning of the origin anyway.
	 * </p>
	 *
	 * @param originDetails execution context (e.g. job name for the scheduler)
	 * @throws CommandException if execution failed
	 * @throws NoSuchFileException if there is no such command
	 */
	void execute(
			Application app,
			String commandName,
			CommandExecutionContext executionContext,
			int origin,
			String originDetails )
	throws CommandException, NoSuchFileException;


	/**
	 * Gets the history of the executed commands.
	 * @param start where to start (for pagination, set to 0 if &lt; 0)
	 * @param maxEntry how many entries (for pagination, set to 20 if value &lt; 1)
	 * @param sortCriteria sort criteria (names: start / application / command / origin / result)
	 * @param sortingOrder the sorting order (asc / desc)
	 * @param applicationName the application name to filter the result (can be null for all applications)
	 * @return a non-null list
	 */
	List<CommandHistoryItem> getHistory( int start, int maxEntry, String sortCriteria, String sortingOrder, String applicationName );


	/**
	 * @param itemsPerPage the number of items per page (set to 20 if value &lt; 1)
	 * @param applicationName the application name to filter the result (can be null for all applications)
	 * @return the number of pages
	 */
	int getHistoryNumberOfPages( int itemsPerPage, String applicationName );


	/**
	 * A context to set constraints on commands.
	 * <p>
	 * The primary purpose is related to the autonomic, but it could be reused
	 * if necessary.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static final class CommandExecutionContext {

		private final AtomicInteger globalVmNumber, appVmNumber;
		private final int maxVm;
		private final boolean strictMaxVm;
		private final String newVmMarkerKey, newVmMarkerValue;


		/**
		 * Constructor.
		 * @param globalVmNumber
		 * @param appVmNumber
		 * @param strictMaxVm
		 * @param newVmMarkerKey
		 * @param newVmMarkerValue
		 * @param maxVm
		 */
		public CommandExecutionContext(
				AtomicInteger globalVmNumber,
				AtomicInteger appVmNumber,
				int maxVm,
				boolean strictMaxVm,
				String newVmMarkerKey,
				String newVmMarkerValue ) {

			this.maxVm = maxVm;
			this.strictMaxVm = strictMaxVm;
			this.newVmMarkerKey = newVmMarkerKey;
			this.newVmMarkerValue = newVmMarkerValue;
			this.globalVmNumber = globalVmNumber;
			this.appVmNumber = appVmNumber;
		}

		public int getMaxVm() {
			return maxVm;
		}

		public boolean isStrictMaxVm() {
			return strictMaxVm;
		}

		public String getNewVmMarkerKey() {
			return newVmMarkerKey;
		}

		public String getNewVmMarkerValue() {
			return newVmMarkerValue;
		}

		public AtomicInteger getGlobalVmNumber() {
			return globalVmNumber;
		}

		public AtomicInteger getAppVmNumber() {
			return appVmNumber;
		}
	}
}
