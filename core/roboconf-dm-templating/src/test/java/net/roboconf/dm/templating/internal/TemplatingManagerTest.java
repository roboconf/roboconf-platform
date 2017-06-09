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

package net.roboconf.dm.templating.internal;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.templating.internal.templates.TemplateEntry;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TemplatingManagerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testBasics() {

		TemplatingManager mngr = new TemplatingManager();
		Assert.assertEquals( TemplatingManager.ID, mngr.getId());

		// Empty methods
		mngr.enableNotifications();
		mngr.disableNotifications();
		mngr.raw( "whatever" );
		mngr.applicationTemplate( null, EventType.CHANGED );

		// Binding and unbinding the DM
		Assert.assertNull( mngr.dm );
		mngr.bindManager( new Manager());
		Assert.assertNotNull( mngr.dm );
		mngr.unbindManager( null );
		Assert.assertNull( mngr.dm );
	}


	@Test
	public void testWatchingScenario() throws Exception {

		// Configure before starting
		TemplatingManager mngr = new TemplatingManager();
		File templatesDirectory = this.folder.newFolder();
		File outputDirectory = this.folder.newFolder();

		mngr.setTemplatesDirectory( templatesDirectory.getAbsolutePath());
		mngr.setOutputDirectory( outputDirectory.getAbsolutePath());
		mngr.setPollInterval( 300 );

		Manager dm = new Manager();
		mngr.bindManager( dm );

		// Start
		Assert.assertNull( mngr.templateWatcher );
		mngr.start();
		Assert.assertNotNull( mngr.templateWatcher );

		// Copy a template
		File tplFile = new File( templatesDirectory, "basic.txt.tpl" );
		InputStream in = getClass().getResourceAsStream( "/templates/basic.txt.tpl" );
		try {
			Utils.copyStream( in, tplFile );

		} finally {
			Utils.closeQuietly( in );
		}

		// Wait few seconds
		Thread.sleep( 700 );
		Assert.assertEquals( 0, outputDirectory.listFiles().length );

		// Create a new application
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		TestManagerWrapper managerWrapper = new TestManagerWrapper( dm );
		managerWrapper.addManagedApplication( new ManagedApplication( app ));

		mngr.application( app, EventType.CREATED );
		Thread.sleep( 700 );

		File expectedFile = new File( outputDirectory, app.getName() + "/basic.txt" );
		Assert.assertTrue( expectedFile.exists());

		// Reconfigure the poll interval and templates directory
		templatesDirectory = this.folder.newFolder();
		mngr.setTemplatesDirectory( templatesDirectory.getAbsolutePath());
		mngr.setPollInterval( 200 );

		// Already output files are still there
		Thread.sleep( 500 );
		Assert.assertTrue( expectedFile.exists());

		// The application changes.
		// There is no template => no output should appear.
		Utils.deleteFilesRecursively( expectedFile.getParentFile());
		Assert.assertFalse( expectedFile.exists());

		mngr.instance( app.getMySql(), app, EventType.CHANGED );
		Thread.sleep( 500 );
		Assert.assertFalse( expectedFile.exists());

		// Change the output directory
		outputDirectory = this.folder.newFolder();
		mngr.setOutputDirectory( outputDirectory.getAbsolutePath());

		// Still no template => no output
		Thread.sleep( 500 );
		Assert.assertFalse( expectedFile.exists());

		// Copy the template and verify a new file is generated
		tplFile = new File( templatesDirectory, "basic.txt.tpl" );
		in = getClass().getResourceAsStream( "/templates/basic.txt.tpl" );
		try {
			Utils.copyStream( in, tplFile );

		} finally {
			Utils.closeQuietly( in );
		}

		Thread.sleep( 500 );
		expectedFile = new File( outputDirectory, app.getName() + "/basic.txt" );
		Assert.assertTrue( expectedFile.exists());

		// Delete the application => generated files are deleted.
		mngr.application( app, EventType.DELETED );
		Assert.assertFalse( expectedFile.getParentFile().exists());

		// Templates are still there.
		Assert.assertTrue( tplFile.exists());

		// Stop the templating manager.
		Assert.assertNotNull( mngr.templateWatcher );
		mngr.stop();
		Assert.assertNull( mngr.templateWatcher );
	}


	@Test
	public void test_invalidDirectories() throws Exception {

		// Both directories are invalid
		TemplatingManager mngr = new TemplatingManager();
		mngr.setOutputDirectory( null );
		mngr.setTemplatesDirectory( null );

		Assert.assertNull( mngr.templatesDIR );
		Assert.assertNull( mngr.outputDIR );

		mngr.start();
		Assert.assertNull( mngr.templateWatcher );

		// Set the templates directory
		mngr.setTemplatesDirectory( this.folder.newFolder().getAbsolutePath());
		Assert.assertNotNull( mngr.templatesDIR );
		Assert.assertNull( mngr.outputDIR );

		mngr.start();
		Assert.assertNull( mngr.templateWatcher );
		mngr.stop();

		// For code coverage...
		// No exception
		mngr.application( new TestApplication(), EventType.CHANGED );
		mngr.processNewTemplates( new ArrayList<TemplateEntry>( 0 ));
		mngr.generate( new TestApplication());
	}


	@Test
	public void testLoggingInGenerate_ioException() throws Exception {

		TemplatingManager mngr = new TemplatingManager();
		mngr.setOutputDirectory( this.folder.newFile().getAbsolutePath());

		TemplateEntry te = new TemplateEntry( new File( "inexisting.tpl" ), null, null, null );
		Application app = new Application( "test", null );

		mngr.generate( app, Collections.singleton( te ));
		// No exception thrown while we try to write a file under another file.
	}


	@Test
	public void testLoggingInGenerate_npe() throws Exception {

		TemplatingManager mngr = new TemplatingManager();

		Application app = new Application( "test", null );
		Collection<TemplateEntry> te = new ArrayList<> ();
		te.add( null );

		mngr.generate( app, te );
		// No exception thrown while we will get a NPE while iterating on the templates.
	}
}
