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
import java.util.List;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 */
public class CommandsMngrImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Application app;
	private CommandsMngrImpl cmdMngr;

	private Manager manager;


	@Before
	public void createMockObject() throws IOException {

		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());
		this.manager = Mockito.mock( Manager.class );

		this.cmdMngr = new CommandsMngrImpl( this.manager );
		Assert.assertEquals( "", this.cmdMngr.getCommandInstructions( this.app, "" ));
	}


	@Test
	public void testBasics() throws IOException {

		this.cmdMngr.createOrUpdateCommand(this.app, "toto","This is a command");
		Assert.assertEquals("This is a command", this.cmdMngr.getCommandInstructions(this.app, "toto"));

		this.cmdMngr.createOrUpdateCommand( this.app, "toto", "Good command");
		Assert.assertEquals( "Good command", this.cmdMngr.getCommandInstructions(this.app, "toto"));
		this.cmdMngr.deleteCommand( this.app, "tata");
		Assert.assertEquals( "", this.cmdMngr.getCommandInstructions(this.app, "tata"));
	}


	@Test
	public void testValidate() {

		List<ParsingError> errors = this.cmdMngr.validate( this.app, "deploy and start all /tomcat-vm" );
		Assert.assertEquals( 0, errors.size());

		errors = this.cmdMngr.validate( this.app, "This is not a command..." );
		Assert.assertEquals( 2, errors.size());
	}


	@Test
	public void testListCommands() throws Exception {

		Application inexisting = new Application( "inexisting", this.app.getTemplate());
		Assert.assertEquals( 0, this.cmdMngr.listCommands( inexisting ).size());
		Assert.assertEquals( 0, this.cmdMngr.listCommands( this.app ).size());

		this.cmdMngr.createOrUpdateCommand( this.app, "toto", "Good command");
		List<String> list = this.cmdMngr.listCommands( this.app );

		Assert.assertEquals( 1, list.size());
		Assert.assertEquals( "toto", list.get( 0 ));

		this.cmdMngr.createOrUpdateCommand( this.app, "before", "Good command");
		this.cmdMngr.createOrUpdateCommand( this.app, "toto2", "Good command");
		list = this.cmdMngr.listCommands( this.app );

		Assert.assertEquals( 3, list.size());
		Assert.assertEquals( "before", list.get( 0 ));
		Assert.assertEquals( "toto", list.get( 1 ));
		Assert.assertEquals( "toto2", list.get( 2 ));

		this.cmdMngr.deleteCommand( this.app, "toto" );
		list = this.cmdMngr.listCommands( this.app );

		Assert.assertEquals( 2, list.size());
		Assert.assertEquals( "before", list.get( 0 ));
		Assert.assertEquals( "toto2", list.get( 1 ));
	}


	@Test
	public void testExecute() throws Exception {

		String line = "rename /tomcat-vm as tomcat-vm-copy";
		Assert.assertEquals( 0, this.cmdMngr.validate( this.app, line ).size());

		String cmdName = "my-command";
		this.cmdMngr.createOrUpdateCommand( this.app, cmdName, line );

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, "/tomcat-vm" ));
		Assert.assertNull( InstanceHelpers.findInstanceByPath( this.app, "/tomcat-vm-copy" ));
		this.cmdMngr.execute( this.app, cmdName, CommandHistoryItem.ORIGIN_REST_API, "some source" );
		Assert.assertNull( InstanceHelpers.findInstanceByPath( this.app, "/tomcat-vm" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( this.app, "/tomcat-vm-copy" ));
	}


	@Test( expected = NoSuchFileException.class )
	public void testExecute_noSuchCommand() throws Exception {

		this.cmdMngr.execute( this.app, "my-command", CommandHistoryItem.ORIGIN_REST_API, "some source" );
	}


	@Test
	public void testWithHistory_realDataSource() throws Exception {

		// Create a H2 data source
		JdbcDataSource ds = new JdbcDataSource();
		File dataFile = this.folder.newFile();
		ds.setURL( "jdbc:h2:" + dataFile.getAbsolutePath());
		ds.setUser( "roboconf" );
		ds.setPassword( "roboconf" );

		// Prepare the mock
		Mockito.when( this.manager.getDataSource()).thenReturn( ds );

		// Execute a command once
		String cmdName = "my-command";
		String line = "rename /tomcat-vm as tomcat-vm-copy";
		this.cmdMngr.createOrUpdateCommand( this.app, cmdName, line );

		Assert.assertEquals( 0, this.cmdMngr.getHistoryNumberOfPages( 10, null ));
		Assert.assertEquals( 0, this.cmdMngr.getHistory( 0, 10, null, null, null ).size());
		this.cmdMngr.execute( this.app, cmdName, CommandHistoryItem.ORIGIN_REST_API, "some source" );

		// Verify things were updated
		Assert.assertEquals( 1, this.cmdMngr.getHistoryNumberOfPages( 10, null ));
		Assert.assertEquals( 1, this.cmdMngr.getHistory( 0, 10, null, null, null ).size());

		// Execute it again
		final int repeatCount = 15;
		for( int i=0; i<repeatCount; i++ ) {
			try {
				this.cmdMngr.execute( this.app, cmdName, CommandHistoryItem.ORIGIN_SCHEDULER, "some source 2" );
				Assert.fail( "An error was expected, the instance to rename does not exist anymore." );

			} catch( CommandException e ) {
				// nothing
			}
		}

		Assert.assertEquals( 2, this.cmdMngr.getHistoryNumberOfPages( 10, null ));

		List<CommandHistoryItem> items = this.cmdMngr.getHistory( 0, 10, null, null, null );
		Assert.assertEquals( 10, items.size());

		items = this.cmdMngr.getHistory( 0, 20, null, null, null );
		Assert.assertEquals( repeatCount + 1, items.size());
		CommandHistoryItem item = items.get( 0 );

		Assert.assertEquals( this.app.getName(), item.getApplicationName());
		Assert.assertEquals( "my-command", item.getCommandName());
		Assert.assertEquals( "some source", item.getOriginDetails());
		Assert.assertEquals( CommandHistoryItem.ORIGIN_REST_API, item.getOrigin());
		Assert.assertEquals( CommandHistoryItem.EXECUTION_OK, item.getExecutionResult());
		Assert.assertTrue( item.getDuration() > 0L );
		Assert.assertTrue( item.getStart() > 0L );

		for( int i=0; i<repeatCount; i++ ) {
			item = items.get( i + 1 );

			Assert.assertEquals( this.app.getName(), item.getApplicationName());
			Assert.assertEquals( "my-command", item.getCommandName());
			Assert.assertEquals( "some source 2", item.getOriginDetails());
			Assert.assertEquals( CommandHistoryItem.ORIGIN_SCHEDULER, item.getOrigin());
			Assert.assertEquals( CommandHistoryItem.EXECUTION_ERROR, item.getExecutionResult());
			Assert.assertTrue( item.getDuration() > 0L );
			Assert.assertTrue( item.getStart() > 0L );
		}

		// Filter by application name
		Assert.assertEquals( 0, this.cmdMngr.getHistoryNumberOfPages( 10, "inexisting app" ));
		Assert.assertEquals( 0, this.cmdMngr.getHistory( 0, 10, null, null, "inexisting app" ).size());

		Assert.assertEquals( 2, this.cmdMngr.getHistoryNumberOfPages( 10, this.app.getName()));
		Assert.assertEquals( 1, this.cmdMngr.getHistoryNumberOfPages( -1, this.app.getName()));

		Assert.assertEquals( repeatCount + 1, this.cmdMngr.getHistory( 0, 20, "result", null, this.app.getName()).size());
		Assert.assertEquals( repeatCount + 1, this.cmdMngr.getHistory( -1, -1, "result", null, this.app.getName()).size());

		// Verify sorting: the first in history was the only successful one
		items = this.cmdMngr.getHistory( -1, -1, "start", "asc", this.app.getName());
		Assert.assertEquals( CommandHistoryItem.EXECUTION_OK, items.get( 0 ).getExecutionResult());
		Assert.assertEquals( "some source", items.get( 0 ).getOriginDetails());
		Assert.assertEquals( CommandHistoryItem.ORIGIN_REST_API, items.get( 0 ).getOrigin());

		items = this.cmdMngr.getHistory( -1, -1, null, "asc", this.app.getName());
		Assert.assertEquals( CommandHistoryItem.EXECUTION_OK, items.get( 0 ).getExecutionResult());
		Assert.assertEquals( "some source", items.get( 0 ).getOriginDetails());
		Assert.assertEquals( CommandHistoryItem.ORIGIN_REST_API, items.get( 0 ).getOrigin());

		items = this.cmdMngr.getHistory( -1, -1, "result", "desc", this.app.getName());
		Assert.assertEquals( CommandHistoryItem.EXECUTION_ERROR, items.get( 0 ).getExecutionResult());
		Assert.assertEquals( CommandHistoryItem.ORIGIN_SCHEDULER, items.get( 0 ).getOrigin());

		Assert.assertEquals( CommandHistoryItem.EXECUTION_OK, items.get( items.size() - 1 ).getExecutionResult());
		Assert.assertEquals( "some source", items.get( items.size() - 1 ).getOriginDetails());
		Assert.assertEquals( CommandHistoryItem.ORIGIN_REST_API, items.get( items.size() - 1 ).getOrigin());

		// Verify extra-pagination (start from item n° 20 and get at most 20 items)
		items = this.cmdMngr.getHistory( 20, 20, null, null, this.app.getName());
		Assert.assertEquals( 0, items.size());
	}


	@Test
	public void testWithHistory_noSource() throws Exception {

		Assert.assertEquals( 0, this.cmdMngr.getHistoryNumberOfPages( 10, null ));
		Assert.assertEquals( 0, this.cmdMngr.getHistory( 0, 10, null, null, null ).size());
	}
}
