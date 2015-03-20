/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.monitoring.internal;

import java.io.File;
import java.io.IOException;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Graphs;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static net.roboconf.dm.monitoring.MonitoringService.MONITORING_TARGET_DIRECTORY;
import static net.roboconf.dm.monitoring.MonitoringService.MONITORING_TEMPLATE_DIRECTORY;
import static net.roboconf.dm.monitoring.internal.MonitoringTestUtils.addStringTemplate;
import static net.roboconf.dm.monitoring.internal.MonitoringTestUtils.hasContent;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Test the {@link net.roboconf.dm.monitoring.internal.MonitoringManager} component.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class MonitoringManagerTest {

	/**
	 * The template watcher poll interval.
	 * <p>
	 * If some tests fail erratically, this interval should be increased.
	 * </p>
	 */
	private static long POLL_INTERVAL = 250L;

	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	/**
	 * The tested {@code MonitoringManager} component.
	 */
	private MonitoringManager manager;

	/**
	 * The Monitoring manager template directory.
	 */
	private File templateDir;

	/**
	 * The Monitoring manager target directory.
	 */
	private File targetDir;

	/**
	 * A sample application.
	 */
	private final Application app1 = new Application()
			.name( "test-app-1" )
			.description( "An application being tested" )
			.qualifier( "test" )
			.graphs( new Graphs() );

	/**
	 * Another sample application.
	 */
	private final Application app2 = new Application()
			.name( "test-app-2" )
			.description( "Another application being tested" )
			.qualifier( "test" )
			.graphs( new Graphs() );

	@Before
	public void before() throws IOException {
		// Create the configuration directory.
		final File configDir = tmpDir.newFolder();
		templateDir = new File( configDir, MONITORING_TEMPLATE_DIRECTORY );
		targetDir = new File( configDir, MONITORING_TARGET_DIRECTORY );

		// Create & configure the monitoring manager component.
		manager = new MonitoringManager();
		manager.setPollInterval( POLL_INTERVAL );
		manager.start();
		manager.startMonitoring( configDir );

		// Add the applications
		manager.addApplication( app1 );
		manager.addApplication( app2 );
	}

	@After
	public void after() throws IOException {
		// Remove the applications
		manager.removeApplication( app1 );
		manager.removeApplication( app2 );

		manager.stopMonitoring();
		manager.stop();
	}

	@Test
	public void testRootDirectoriesAreCreated() {
		assertThat( templateDir ).exists().isDirectory();
		assertThat( targetDir ).exists().isDirectory();
	}

	@Test
	public void testGlobalAndSpecificTemplates() throws IOException, InterruptedException {
		// Add global & local (app-specific) templates.
		addStringTemplate( manager, null, "global.test", "global:{{name}}" );
		addStringTemplate( manager, app1, "local1.test", "app1:{{name}}" );
		addStringTemplate( manager, app2, "local2.test", "app2:{{name}}" );

		// Check the templates are here.
		assertThat( manager.listTemplates( null ) ).containsOnly( "global.test" );
		assertThat( manager.listTemplates( app1 ) ).containsOnly( "local1.test" );
		assertThat( manager.listTemplates( app2 ) ).containsOnly( "local2.test" );

		// Wait for a while, so the reports are generated.
		Thread.sleep( 2 * POLL_INTERVAL );

		// Check the presence and the content of the global monitoring reports.
		assertThat( new File( targetDir, "test-app-1.global.test" ) )
				.exists()
				.isFile()
				.satisfies( hasContent( "global:test-app-1" ) );
		assertThat( new File( targetDir, "test-app-2.global.test" ) )
				.exists()
				.isFile()
				.satisfies( hasContent( "global:test-app-2" ) );

		// Check the presence and the content of the local monitoring reports.
		assertThat( new File( targetDir, "test-app-1" + File.separatorChar + "local1.test" ) )
				.exists()
				.isFile()
				.satisfies( hasContent( "app1:test-app-1" ) );
		assertThat( new File( targetDir, "test-app-2" + File.separatorChar + "local2.test" ) )
				.exists()
				.isFile()
				.satisfies( hasContent( "app2:test-app-2" ) );

		// Remove the applications
		manager.removeApplication( app1 );
		manager.removeApplication( app2 );
	}

	@Test
	public void testUpdateApplication() throws IOException, InterruptedException {
		// Add global & local (app-specific) templates.
		addStringTemplate( manager, null, "global.test", "global:{{description}}" );
		addStringTemplate( manager, app1, "local1.test", "app1:{{description}}" );

		final File global = new File( targetDir, "test-app-1.global.test" );
		final File local = new File( targetDir, "test-app-1" + File.separatorChar + "local1.test" );

		// Wait for a while, so the reports are generated.
		Thread.sleep( 2 * POLL_INTERVAL );

		// Check the presence and the content of the global monitoring reports.
		assertThat( global ).exists().isFile().satisfies( hasContent( "global:An application being tested" ) );

		// Check the presence and the content of the local monitoring reports.
		assertThat( local ).exists().isFile().satisfies( hasContent( "app1:An application being tested" ) );

		// Change the application, check, update, and recheck!
		app1.setDescription( "CHANGED!" );

		// Wait for a while... just in case an (unwanted) update occurs.
		Thread.sleep( 2 * POLL_INTERVAL );

		// Should not have changed!
		assertThat( global ).satisfies( hasContent( "global:An application being tested" ) );
		assertThat( local ).satisfies( hasContent( "app1:An application being tested" ) );

		// Update the application, the reports should be updated synchronously.
		manager.updateApplication( app1 );
		assertThat( global ).satisfies( hasContent( "global:CHANGED!" ) );
		assertThat( local ).satisfies( hasContent( "app1:CHANGED!" ) );
	}

	@Test
	public void testRemoveApplication() throws IOException, InterruptedException {
		// Add global & local (app-specific) templates.
		addStringTemplate( manager, null, "global.test", "global:{{description}}" );
		addStringTemplate( manager, app1, "local.test", "local:{{description}}" );

		final File global = new File( targetDir, "test-app-1.global.test" );
		final File local = new File( targetDir, "test-app-1" + File.separatorChar + "local.test" );

		// Wait for a while, so the reports are generated.
		Thread.sleep( 2 * POLL_INTERVAL );

		// Check the presence and the content monitoring reports.
		assertThat( global ).exists().isFile().satisfies( hasContent( "global:An application being tested" ) );
		assertThat( local ).exists().isFile().satisfies( hasContent( "local:An application being tested" ) );

		// Remove the application.
		manager.removeApplication( app1 );

		// Update the app which has just been removed, so nothing should happen.
		app1.setDescription( "CHANGED!" );
		manager.updateApplication( app1 );

		// Wait for a while... just in case an (unwanted) update occurs.
		Thread.sleep( 2 * POLL_INTERVAL );

		// Generated reports should not have changed!
		assertThat( global ).satisfies( hasContent( "global:An application being tested" ) );
		assertThat( local ).satisfies( hasContent( "local:An application being tested" ) );

		// Add additional templates.
		addStringTemplate( manager, null, "global2.test", "global2:{{description}}" );
		addStringTemplate( manager, app1, "local2.test", "local2:{{description}}" );

		// Wait for a while... just in case an (unwanted) monitoring report generation occurs.
		Thread.sleep( 2 * POLL_INTERVAL );

		// Update the application, the reports should be updated synchronously.
		manager.updateApplication( app1 );
		assertThat( new File( targetDir, "test-app-1.global2.test" ) ).doesNotExist();
		assertThat( new File( targetDir, "test-app-1" + File.separatorChar + "local2.test" ) ).doesNotExist();
	}

}
