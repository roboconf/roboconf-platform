/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.karaf.commands.dm.history;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.dm.internal.api.impl.CommandsMngrImpl;
import net.roboconf.dm.management.Manager;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PruneHistoryCommandTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testExecute_withRealDataSource() throws Exception {

		// Create a H2 data source
		JdbcDataSource ds = new JdbcDataSource();
		File dataFile = this.folder.newFile();
		ds.setURL( "jdbc:h2:" + dataFile.getAbsolutePath());
		ds.setUser( "roboconf" );
		ds.setPassword( "roboconf" );

		// Prepare the mock
		ServiceReference<DataSource> sr = Mockito.mock( ServiceReference.class );

		PruneHistoryCommand pruneHistoryCmd = new PruneHistoryCommand();
		pruneHistoryCmd.bundleContext = Mockito.mock( BundleContext.class );
		Mockito.when( pruneHistoryCmd.bundleContext.getServiceReferences(
				DataSource.class,
				"(dataSourceName=roboconf-dm-db)" )).thenReturn( Arrays.asList( sr ));

		Mockito.when( pruneHistoryCmd.bundleContext.getService( sr )).thenReturn( ds );

		// Populate the database
		Manager manager = Mockito.mock( Manager.class );
		Mockito.when( manager.getDataSource()).thenReturn( ds );
		CommandsMngrImpl cmdMngr = new CommandsMngrImpl( manager ) ;

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		String cmdName = "my-command";
		String line = "rename /tomcat-vm as tomcat-vm-copy";
		cmdMngr.createOrUpdateCommand( app, cmdName, line );
		final int count = 5;
		for( int i=0; i<count; i++ ) {
			try {
				cmdMngr.execute( app, cmdName, CommandHistoryItem.ORIGIN_REST_API, "some source" );

			} catch( Exception e ) {
				// nothing
			}
		}

		List<CommandHistoryItem> historyItems = cmdMngr.getHistory( 0, 10, null, null, app.getName());
		Assert.assertEquals( count, historyItems.size());

		// Invalid argument? => Nothing is deleted.
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		pruneHistoryCmd.out = new PrintStream( os, true, "UTF-8" );

		pruneHistoryCmd.daysToKeep = -10;
		pruneHistoryCmd.execute();
		historyItems = cmdMngr.getHistory( 0, 10, null, null, app.getName());
		Assert.assertEquals( count, historyItems.size());
		Assert.assertEquals( "[ WARNING ] The daysToKeep argument must be equal or greater than 0. Operation cancelled.", os.toString( "UTF-8" ).trim());

		// Remove entries older than two days. => Here, nothing will be deleted.
		os = new ByteArrayOutputStream();
		pruneHistoryCmd.out = new PrintStream( os, true, "UTF-8" );

		pruneHistoryCmd.daysToKeep = 2;
		pruneHistoryCmd.execute();
		historyItems = cmdMngr.getHistory( 0, 10, null, null, app.getName());
		Assert.assertEquals( count, historyItems.size());
		Assert.assertEquals( "Pruning the commands history.\nOnly the last 2 days will be kept.\nPruning done.", os.toString( "UTF-8" ).trim());

		// Remove entries older than two days. => Here, nothing will be deleted.
		os = new ByteArrayOutputStream();
		pruneHistoryCmd.out = new PrintStream( os, true, "UTF-8" );

		pruneHistoryCmd.daysToKeep = 1;
		pruneHistoryCmd.execute();
		historyItems = cmdMngr.getHistory( 0, 10, null, null, app.getName());
		Assert.assertEquals( count, historyItems.size());
		Assert.assertEquals( "Pruning the commands history.\nOnly the last 1 day will be kept.\nPruning done.", os.toString( "UTF-8" ).trim());

		// Remove all the entries
		os = new ByteArrayOutputStream();
		pruneHistoryCmd.out = new PrintStream( os, true, "UTF-8" );

		pruneHistoryCmd.daysToKeep = 0;
		pruneHistoryCmd.execute();
		historyItems = cmdMngr.getHistory( 0, 10, null, null, app.getName());
		Assert.assertEquals( 0, historyItems.size());
		Assert.assertEquals( "Pruning the commands history.\nAll the entries will be deleted.\nPruning done.", os.toString( "UTF-8" ).trim());
	}


	@Test
	public void testExecute_withoutDataSource() throws Exception {

		// Prepare the mock
		PruneHistoryCommand pruneHistoryCmd = new PruneHistoryCommand();
		pruneHistoryCmd.bundleContext = Mockito.mock( BundleContext.class );
		Mockito.when( pruneHistoryCmd.bundleContext.getServiceReferences(
				DataSource.class,
				"(dataSourceName=roboconf-dm-db)" )).thenReturn( new ArrayList<ServiceReference<DataSource>>( 0 ));

		// Invalid argument? => Nothing is deleted.
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		pruneHistoryCmd.out = new PrintStream( os, true, "UTF-8" );

		pruneHistoryCmd.execute();
		Assert.assertEquals( os.toString( "UTF-8" ).trim(), "No data source was found to prune the commands history." );
	}
}
