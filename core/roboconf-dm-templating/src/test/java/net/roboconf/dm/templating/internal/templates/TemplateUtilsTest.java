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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.internal.helpers.GenerationTest;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TemplateUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testFindApplicationName_valid() throws Exception {

		File templatesDir = new File( "whatever" );
		File globalTemplate = new File( templatesDir, "root.tpl" );
		File specificTemplate = new File( templatesDir, "specific/specific.tpl" );

		Assert.assertEquals( "specific", TemplateUtils.findApplicationName( templatesDir, specificTemplate ));
		Assert.assertNull( TemplateUtils.findApplicationName( templatesDir, globalTemplate ));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testFindApplicationName_invalid() throws Exception {

		File templatesDir = new File( "whatever" );
		File specificTemplate = new File( templatesDir, "specific/sub/dir/specific.tpl" );

		TemplateUtils.findApplicationName( templatesDir, specificTemplate );
	}


	@Test
	public void testDeleteGeneratedFiles() throws Exception {

		File dir = this.folder.newFolder();
		File appDir = new File( dir, "my-app" );
		Utils.createDirectory( appDir );

		Assert.assertTrue( appDir.exists());
		TemplateUtils.deleteGeneratedFiles( new Application( "my-app", null ), dir );
		Assert.assertFalse( appDir.exists());
		Assert.assertTrue( dir.exists());
	}


	@Test
	public void testFindTemplatesForApplication() {

		List<TemplateEntry> templates = new ArrayList<> ();
		templates.add( new TemplateEntry( new File( "whatever1.tpl" ), null, null, null ));
		templates.add( new TemplateEntry( new File( "whatever2.tpl" ), null, null, null ));
		templates.add( new TemplateEntry( new File( "whatever3.tpl" ), null, null, "app1" ));
		templates.add( new TemplateEntry( new File( "whatever4.tpl" ), null, null, "app1" ));
		templates.add( new TemplateEntry( new File( "whatever5.tpl" ), null, null, "app2" ));

		Collection<TemplateEntry> filteredTemplates = TemplateUtils.findTemplatesForApplication( null, templates );
		Assert.assertEquals( 2, filteredTemplates.size());
		Assert.assertTrue( filteredTemplates.contains( templates.get( 0 )));
		Assert.assertTrue( filteredTemplates.contains( templates.get( 1 )));

		filteredTemplates = TemplateUtils.findTemplatesForApplication( "app2", templates );
		Assert.assertEquals( 3, filteredTemplates.size());
		Assert.assertTrue( filteredTemplates.contains( templates.get( 0 )));
		Assert.assertTrue( filteredTemplates.contains( templates.get( 1 )));
		Assert.assertTrue( filteredTemplates.contains( templates.get( 4 )));

		filteredTemplates = TemplateUtils.findTemplatesForApplication( "app1", templates );
		Assert.assertEquals( 4, filteredTemplates.size());
		Assert.assertTrue( filteredTemplates.contains( templates.get( 0 )));
		Assert.assertTrue( filteredTemplates.contains( templates.get( 1 )));
		Assert.assertTrue( filteredTemplates.contains( templates.get( 2 )));
		Assert.assertTrue( filteredTemplates.contains( templates.get( 3 )));

		filteredTemplates = TemplateUtils.findTemplatesForApplication( "app3", templates );
		Assert.assertEquals( 2, filteredTemplates.size());
		Assert.assertTrue( filteredTemplates.contains( templates.get( 0 )));
		Assert.assertTrue( filteredTemplates.contains( templates.get( 1 )));
	}


	@Test
	public void testGenerate_defaultDirectory() throws Exception {

		// Copy a template
		File dir = this.folder.newFolder();
		File tplFile = new File( dir, "basic.txt.tpl" );
		InputStream in = getClass().getResourceAsStream( "/templates/basic.txt.tpl" );
		try {
			Utils.copyStream( in, tplFile );

		} finally {
			Utils.closeQuietly( in );
		}

		// Compile and verify it
		TemplateEntry te = new TemplateWatcher( null, dir, 100 ).compileTemplate( tplFile );
		Assert.assertNotNull( te );
		Assert.assertNull( te.getAppName());
		Assert.assertEquals( tplFile, te.getTemplateFile());
		Assert.assertNotNull( te.getTemplate());

		// Apply it to a given application
		Logger logger = Logger.getLogger( getClass().getName());
		File outputDir = this.folder.newFolder();
		Application app = GenerationTest.testApplicationForTemplates();

		Assert.assertEquals( 0, outputDir.listFiles().length );
		TemplateUtils.generate( app, outputDir, Collections.singleton( te ), logger );
		Assert.assertEquals( 1, outputDir.listFiles().length );

		File targetFile = new File( outputDir, app.getName() + "/basic.txt" );
		Assert.assertTrue( targetFile.exists());
		String currentContent = Utils.readFileContent( targetFile );

		File expectedFile =  TestUtils.findTestFile( "/output/basic.txt" );
		String expectedContent = Utils.readFileContent( expectedFile );

		Assert.assertEquals( expectedContent, currentContent );
	}


	@Test
	public void testGenerate_customDirectory() throws Exception {

		File targetFile = this.folder.newFile();
		Utils.deleteFilesRecursively( targetFile );

		// Copy a template
		String templateContent;
		InputStream in = getClass().getResourceAsStream( "/templates/basic-with-custom-output.txt.tpl" );
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Utils.copyStreamUnsafelyUseWithCaution( in, os );
			templateContent = os.toString( "UTF-8" ).replace( "%DIR%", targetFile.getAbsolutePath());

		} finally {
			Utils.closeQuietly( in );
		}

		File dir = this.folder.newFolder();
		File tplFile = new File( dir, "basic-with-custom-output.txt.tpl" );
		Utils.writeStringInto( templateContent, tplFile );

		// Compile and verify it
		TemplateEntry te = new TemplateWatcher( null, dir, 100 ).compileTemplate( tplFile );
		Assert.assertNotNull( te );
		Assert.assertNull( te.getAppName());
		Assert.assertEquals( tplFile, te.getTemplateFile());
		Assert.assertEquals( targetFile.getAbsolutePath(), te.getTargetFilePath());
		Assert.assertNotNull( te.getTemplate());

		// The file does not exist (yet)
		Assert.assertFalse( targetFile.exists());

		// Apply it to a given application
		Logger logger = Logger.getLogger( getClass().getName());
		File outputDir = this.folder.newFolder();

		Assert.assertEquals( 0, outputDir.listFiles().length );
		TemplateUtils.generate( GenerationTest.testApplicationForTemplates(), outputDir, Collections.singleton( te ), logger );

		// Nothing was added in the out put directory
		Assert.assertEquals( 0, outputDir.listFiles().length );

		// It was directly written in the target file
		Assert.assertTrue( targetFile.exists());
		String currentContent = Utils.readFileContent( targetFile );

		File expectedFile =  TestUtils.findTestFile( "/output/basic-with-custom-output.txt" );
		String expectedContent = Utils.readFileContent( expectedFile );

		Assert.assertEquals( expectedContent, currentContent );
	}


	@Test
	public void testGenerate_customDirectory_byApp() throws Exception {

		File targetFilePattern = new File( this.folder.newFolder(), "${app}/${app}.paf" );

		// Copy a template
		String templateContent;
		InputStream in = getClass().getResourceAsStream( "/templates/basic-with-custom-output.txt.tpl" );
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Utils.copyStreamUnsafelyUseWithCaution( in, os );
			templateContent = os.toString( "UTF-8" ).replace( "%DIR%", targetFilePattern.getAbsolutePath());

		} finally {
			Utils.closeQuietly( in );
		}

		File dir = this.folder.newFolder();
		File tplFile = new File( dir, "basic-with-custom-output.txt.tpl" );
		Utils.writeStringInto( templateContent, tplFile );

		// Compile and verify it
		TemplateEntry te = new TemplateWatcher( null, dir, 100 ).compileTemplate( tplFile );
		Assert.assertNotNull( te );
		Assert.assertNull( te.getAppName());
		Assert.assertEquals( tplFile, te.getTemplateFile());
		Assert.assertEquals( targetFilePattern.getAbsolutePath(), te.getTargetFilePath());
		Assert.assertNotNull( te.getTemplate());

		// The file does not exist (yet)
		Application app =  GenerationTest.testApplicationForTemplates();
		File targetFile = new File( targetFilePattern.getParentFile().getParentFile(), app.getName() + "/" + app.getName() + ".paf" );
		Assert.assertFalse( targetFilePattern.exists());
		Assert.assertFalse( targetFile.exists());

		// Apply it to a given application
		Logger logger = Logger.getLogger( getClass().getName());
		File outputDir = this.folder.newFolder();

		Assert.assertEquals( 0, outputDir.listFiles().length );
		TemplateUtils.generate( app, outputDir, Collections.singleton( te ), logger );

		// Nothing was added in the out put directory
		Assert.assertEquals( 0, outputDir.listFiles().length );

		// It was directly written in the target file
		Assert.assertFalse( targetFilePattern.exists());
		Assert.assertTrue( targetFile.exists());
		String currentContent = Utils.readFileContent( targetFile );

		File expectedFile =  TestUtils.findTestFile( "/output/basic-with-custom-output.txt" );
		String expectedContent = Utils.readFileContent( expectedFile );

		Assert.assertEquals( expectedContent, currentContent );
	}


	@Test( expected = IOException.class )
	public void testGenerate_io_exception() throws Exception {

		File inexistingTemplate = new File( "inexisting.tpl" );
		TemplateEntry te = new TemplateEntry( inexistingTemplate, null, null, null );

		Application app = new Application( "test", null );
		Logger logger = Logger.getLogger( getClass().getName());

		File outputDir = this.folder.newFile();
		TemplateUtils.generate( app, outputDir, Collections.singleton( te ), logger );
	}
}
