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

package net.roboconf.dm.templating.internal.templates;

import java.io.File;
import java.io.InputStream;

import org.junit.Assert;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.internal.TemplatingManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TemplateWatcherWatchingTest {

	private static final long DELAY = 400;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private TemplateWatcher watcher;
	private File templatesDir;
	private TemplatingManager templatingMngr;


	@Before
	public void configureWatcher() throws Exception {

		this.templatesDir = this.folder.newFolder();
		this.templatingMngr = Mockito.mock( TemplatingManager.class );

		this.watcher = new TemplateWatcher( this.templatingMngr, this.templatesDir, DELAY );
		this.watcher.start();

		// Wait for the watcher to be started
		Thread.sleep( DELAY * 2 );
	}


	@After
	public void stopWatcher() {
		this.watcher.stop();
	}


	@Test
	public void testNoTemplate() throws Exception {

		Assert.assertEquals( 0, this.watcher.findTemplatesForApplication( null ).size());
		Assert.assertEquals( 0, this.watcher.findTemplatesForApplication( "whatever" ).size());
	}


	@Test
	public void testOneTemplate_appears_and_disappears() throws Exception {

		File tplFile = new File( this.templatesDir, "basic.txt.tpl" );
		InputStream in = getClass().getResourceAsStream( "/templates/basic.txt.tpl" );
		try {
			Utils.copyStream( in, tplFile );

		} finally {
			Utils.closeQuietly( in );
		}

		Thread.sleep( DELAY * 2 );
		Assert.assertEquals( 1, this.watcher.findTemplatesForApplication( null ).size());
		Assert.assertEquals( 1, this.watcher.findTemplatesForApplication( "any-app-name" ).size());

		// 2 invocations: one at startup and one when the template appears.
		Mockito.verify( this.templatingMngr, Mockito.times( 2 )).processNewTemplates( Mockito.anyCollectionOf( TemplateEntry.class ));

		Utils.deleteFilesRecursively( tplFile );

		Thread.sleep( DELAY * 2 );
		Assert.assertEquals( 0, this.watcher.findTemplatesForApplication( null ).size());
		Assert.assertEquals( 0, this.watcher.findTemplatesForApplication( "whatever" ).size());
	}


	@Test
	public void testTwoTemplates_appears() throws Exception {

		// Create the templates
		File f = new File( this.templatesDir, "basic.txt.tpl" );
		InputStream in = getClass().getResourceAsStream( "/templates/basic.txt.tpl" );
		try {
			Utils.copyStream( in, f );

		} finally {
			Utils.closeQuietly( in );
		}

		File subDir = new File( this.templatesDir, "my-app" );
		Utils.createDirectory( subDir );
		Utils.copyStream( f, new File( subDir, "test.txt.tpl" ));

		// Twice the delay PER template!
		Thread.sleep( DELAY * 4 );

		Assert.assertEquals( 1, this.watcher.findTemplatesForApplication( null ).size());
		Assert.assertEquals( 1, this.watcher.findTemplatesForApplication( "any-app-name" ).size());

		// One global template, plus one specific
		Assert.assertEquals( 2, this.watcher.findTemplatesForApplication( "my-app" ).size());

		// Two templates => 2 or 3 notifications, one at startup and one for every template
		// (or one for both templates, if they were detected at once).
		Mockito.verify( this.templatingMngr, Mockito.atLeast( 2 )).processNewTemplates( Mockito.anyCollectionOf( TemplateEntry.class ));
	}


	@Test
	public void testInvalidTemplate_andRepairIt() throws Exception {

		// Create an invalid template
		File tplFile = new File( this.templatesDir, "basic.txt.tpl" );
		Utils.writeStringInto( "{{#instanceOf VM}}", tplFile );

		Thread.sleep( DELAY * 2 );
		Assert.assertEquals( 0, this.watcher.findTemplatesForApplication( null ).size());
		Assert.assertEquals( 0, this.watcher.findTemplatesForApplication( "whatever" ).size());

		// Update it
		InputStream in = getClass().getResourceAsStream( "/templates/basic.txt.tpl" );
		try {
			Utils.copyStream( in, tplFile );

		} finally {
			Utils.closeQuietly( in );
		}

		Thread.sleep( DELAY * 2 );
		Assert.assertEquals( 1, this.watcher.findTemplatesForApplication( null ).size());
		Assert.assertEquals( 1, this.watcher.findTemplatesForApplication( "any-app-name" ).size());
	}
}
