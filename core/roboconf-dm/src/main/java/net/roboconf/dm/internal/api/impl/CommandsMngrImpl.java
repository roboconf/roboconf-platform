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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import net.roboconf.core.Constants;
import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.commands.CommandsExecutor;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ICommandsMngr;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 * @author Vincent Zurczak - Linagora
 */
public class CommandsMngrImpl implements ICommandsMngr {

	static final int DEFAULT_ITEMS_PER_PAGE = 20;
	private final Logger logger = Logger.getLogger( getClass().getName());
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


	/*
	 * This class uses a data source to interact with a database.
	 * We use raw SQL queries, which urges us to correctly close
	 * database resources.
	 *
	 * We could have used JPA to reduce a little bit the size of the code.
	 * Here is an example of using JPA within OSGi:
	 *
	 * https://github.com/cschneider/Karaf-Tutorial/tree/master/db/examplejpa
	 *
	 * The main difference for us would be to use iPojo instead of Blueprint
	 * to create services. And we would not need Karaf commands. We do not
	 * use JPA as we have very limited interactions with a database and also
	 * because we would like to reduce the number of dependencies for the DM.
	 *
	 * Right now, the "roboconf-dm" bundle can be used as a usual Java library
	 * and only depends on JRE (and Roboconf) classes.
	 */


	@Override
	public int getHistoryNumberOfPages( int itemsPerPage, String applicationName ) {

		int result = 0;
		DataSource dataSource = this.manager.getDataSource();

		// Fix invalid values for the query
		if( itemsPerPage < 1 )
			itemsPerPage = DEFAULT_ITEMS_PER_PAGE;

		// The data source is optional.
		if( dataSource != null ) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet sqlRes = null;
			try {
				conn = dataSource.getConnection();

				StringBuilder sb = new StringBuilder();
				sb.append( "SELECT count( * ) FROM commands_history" );
				if( ! Utils.isEmptyOrWhitespaces( applicationName ))
					sb.append( " WHERE application = ?" );

				// Use PreparedStatement to prevent SQL injection
				ps = conn.prepareStatement( sb.toString());
				if( ! Utils.isEmptyOrWhitespaces( applicationName ))
					ps.setString( 1, applicationName );

				// Execute
				sqlRes = ps.executeQuery();
				if( sqlRes.next()) {
					int count = sqlRes.getInt( 1 );
					result = count == 0 ? 0 : count / itemsPerPage + 1;
				}

			} catch( SQLException e ) {
				this.logger.severe( "An error occurred while counting pages in commands history." );
				Utils.logException( this.logger, e );

			} finally {
				Utils.closeResultSet( sqlRes, this.logger );
				Utils.closeStatement( ps, this.logger );
				Utils.closeConnection( conn, this.logger );
			}
		}

		return result;
	}


	@Override
	public List<CommandHistoryItem> getHistory(
			int start,
			int maxEntry,
			String sortCriteria,
			String sortingOrder,
			String applicationName ) {

		List<CommandHistoryItem> result = new ArrayList<> ();
		DataSource dataSource = this.manager.getDataSource();

		// Fix invalid values for the query
		if( start < 0 )
			start = 0;

		if( maxEntry < 1 )
			maxEntry = DEFAULT_ITEMS_PER_PAGE;

		// Verify the sort criteria
		if( ! Arrays.asList( "start", "application", "command", "origin", "result" ).contains( sortCriteria ))
			sortCriteria = "start";

		if( ! Arrays.asList( "asc", "desc" ).contains( sortingOrder ))
			sortingOrder = "asc";

		// The data source is optional.
		if( dataSource != null ) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet sqlRes = null;
			try {
				conn = dataSource.getConnection();

				StringBuilder sb = new StringBuilder();
				sb.append( "SELECT * FROM commands_history " );
				if( ! Utils.isEmptyOrWhitespaces( applicationName ))
					sb.append( " WHERE application = ?" );

				sb.append( " ORDER BY " );
				sb.append( sortCriteria );
				sb.append( " " );
				sb.append( sortingOrder );
				sb.append( " LIMIT " );
				sb.append( maxEntry );
				sb.append( " OFFSET " );
				sb.append( start );

				// Use PreparedStatement to prevent SQL injection
				ps = conn.prepareStatement( sb.toString());
				if( ! Utils.isEmptyOrWhitespaces( applicationName ))
					ps.setString( 1, applicationName );

				// Build the result
				for( sqlRes = ps.executeQuery(); sqlRes.next(); ) {
					String appName = sqlRes.getString( "application" );
					String commandName = sqlRes.getString( "command" );
					int origin = sqlRes.getInt( "origin" );
					String originDetails = sqlRes.getString( "details" );
					int executionResult = sqlRes.getInt( "result" );
					long executionStart = sqlRes.getLong( "start" );
					long duration = sqlRes.getLong( "duration" );

					result.add( new CommandHistoryItem(
							appName, commandName, origin, originDetails,
							executionResult, executionStart, duration ));
				}

			} catch( SQLException e ) {
				this.logger.severe( "An error occurred while retrieving commands history from database." );
				Utils.logException( this.logger, e );

			} finally {
				Utils.closeResultSet( sqlRes, this.logger );
				Utils.closeStatement( ps, this.logger );
				Utils.closeConnection( conn, this.logger );
			}
		}

		return result;
	}


	@Override
	public void execute( Application app, String commandName, int origin, String originDetails )
	throws CommandException, NoSuchFileException {
		execute( app, commandName, null, origin, originDetails );
	}


	@Override
	public void execute(
			Application app,
			String commandName,
			CommandExecutionContext executionContext,
			int origin,
			String originDetails )
	throws CommandException, NoSuchFileException {

		File cmdFile = findCommandFile( app, commandName );
		if( ! cmdFile.isFile())
			throw new NoSuchFileException( cmdFile.getAbsolutePath());

		int result = CommandHistoryItem.EXECUTION_OK;
		long startInMilliSeconds = System.currentTimeMillis();
		long startInNanoSeconds = System.nanoTime();
		try {
			CommandsExecutor executor = new CommandsExecutor( this.manager, app, cmdFile, executionContext );
			executor.execute();
			if( executor.wereInstructionSkipped())
				result = CommandHistoryItem.EXECUTION_OK_WITH_SKIPPED;

		} catch( CommandException e ) {
			result = CommandHistoryItem.EXECUTION_ERROR;
			throw e;

		} finally {
			recordInHistory( startInMilliSeconds, startInNanoSeconds, result, app.getName(), commandName, origin, originDetails );
		}
	}


	/**
	 * @param startInMilliSeconds
	 * @param startInNanoSeconds
	 * @param result
	 * @param applicationName
	 * @param commandName
	 * @param origin
	 */
	private void recordInHistory(
			long startInMilliSeconds,
			long startInNanoSeconds,
			int result,
			String applicationName,
			String commandName,
			int origin,
			String originDetails ) {

		long duration = System.nanoTime() - startInNanoSeconds;
		DataSource dataSource = this.manager.getDataSource();

		// The data source is optional.
		if( dataSource != null ) {

			// We try to insert the record.
			boolean failed = false;
			Connection conn = null;
			try {
				conn = dataSource.getConnection();
				recordEntry( conn, startInMilliSeconds, duration, result, applicationName, commandName, origin, originDetails );

			} catch( SQLException e ) {
				failed = true;
			}

			// If it fails, we assume the table does not exist.
			// We thus try to create it and retry the insertion.
			// It avoids executing two SQL statements every time we try to insert it.

			// In Karaf, the data source can be configured / modified dynamically.
			// So, it is better to have a resilient implementation.
			try {
				if( failed ) {
					createTable( conn );
					recordEntry( conn, startInMilliSeconds, duration, result, applicationName, commandName, origin, originDetails );
				}

			} catch( SQLException e ) {
				this.logger.severe( "An error occurred while storing the result of a command execution in database." );
				Utils.logException( this.logger, e );

			} finally {
				Utils.closeConnection( conn, this.logger );
			}
		}
	}


	private void recordEntry(
			Connection conn,
			long start,
			long duration,
			int result,
			String applicationName,
			String commandName,
			int origin,
			String originDetails ) throws SQLException {

		String req = "INSERT INTO commands_history( application, command, start, duration, result, origin, details ) values( ?, ?, ?, ?, ?, ?, ? )";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement( req );
			ps.setString( 1, applicationName );
			ps.setString( 2, commandName );
			ps.setLong( 3, start );
			ps.setLong( 4, duration );
			ps.setInt( 5, result );
			ps.setInt( 6, origin );
			ps.setString( 7, originDetails );
			ps.execute();

		} finally {
			Utils.closeStatement( ps, this.logger );
		}
	}


	private void createTable( Connection conn ) throws SQLException {

		StringBuilder sb = new StringBuilder( "CREATE TABLE IF NOT EXISTS commands_history (" );
		sb.append( "id INT NOT NULL AUTO_INCREMENT," );
		sb.append( "application VARCHAR(255)," );
		sb.append( "command VARCHAR(255)," );
		sb.append( "start BIGINT," );
		sb.append( "duration BIGINT," );
		sb.append( "result SMALLINT," );
		sb.append( "origin SMALLINT," );
		sb.append( "details VARCHAR(255)," );
		sb.append( "PRIMARY KEY( id ))" );

		Statement st = null;
		try {
			st = conn.createStatement();
			st.execute( sb.toString());

		} finally {
			Utils.closeStatement( st, this.logger );
		}
	}


	private File findCommandFile( Application app, String commandName ) {

		String name = commandName;
		if( ! name.endsWith( Constants.FILE_EXT_COMMANDS ))
			name += Constants.FILE_EXT_COMMANDS;

		File cmdDir = new File( app.getDirectory(), Constants.PROJECT_DIR_COMMANDS );
		return new File( cmdDir, name );
	}
}
