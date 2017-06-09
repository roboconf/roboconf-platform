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

package net.roboconf.dm.templating.internal.helpers;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.internal.templates.TemplateEntry;
import net.roboconf.dm.templating.internal.templates.TemplateUtils;
import net.roboconf.dm.templating.internal.templates.TemplateWatcher;

/**
 * Helpers do not have unit tests.
 * <p>
 * Instead, we have integration tests that verify
 * generation from templates works correctly. These
 * tests should cover all the features made available in the helpers.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class GenerationTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	/**
	 * @return an application to use with templating tests
	 */
	public static Application testApplicationForTemplates() throws Exception {

		// Load the application from the test resources.
		File dir = TestUtils.findApplicationDirectory( "app-for-templates" );
		Assert.assertTrue( dir.exists());

		final ApplicationLoadResult result = RuntimeModelIo.loadApplication( dir );
		assertThat( result.getApplicationTemplate()).isNotNull();

		// Create and patch an application to verify contexts are correctly generated
		Application app = new Application( "test-app", result.getApplicationTemplate()).description( "An example application" );
		Instance apacheVm = InstanceHelpers.findInstanceByPath( app, "/ApacheVm" );
		assertThat( apacheVm ).isNotNull();

		apacheVm.overriddenExports.put( "apacheVm.extra", "bonus" );

		for( Instance rootInstance : app.getRootInstances()) {
			if( rootInstance.equals( apacheVm ))
				continue;

			rootInstance.data.put( Instance.APPLICATION_NAME, app.getName());
			rootInstance.data.put( Instance.MACHINE_ID, "ds4sd14sdsfkdf" );
			ImportHelpers.addImport( rootInstance, "test", new Import( apacheVm ));
		}

		return app;
	}


	@Test
	public void testGenerationWithValidTemplates() throws Exception {

		// Generate files for this application from different templates
		Logger logger = Logger.getLogger( getClass().getName());
		File outputDir = this.folder.newFolder();

		List<File> templateFiles = TestUtils.findTestFiles( "/templates" );
		Assert.assertFalse( templateFiles.isEmpty());

		// The watcher is not started, it just in charge of compiling the templates...
		File templatesDir = templateFiles.get( 0 ).getParentFile();
		TemplateWatcher watcher = new TemplateWatcher( null, templatesDir, 100 );

		// Compare output files with what was expected
		for( File tplFile : templateFiles ) {

			// The template is used somewhere else
			if( "basic-with-custom-output.txt.tpl".equals( tplFile.getName()))
				continue;

			TemplateEntry te = watcher.compileTemplate( tplFile );
			Assert.assertNotNull( tplFile.getName(), te );
			TemplateUtils.generate( testApplicationForTemplates(), outputDir, Collections.singleton( te ), logger );

			File outputFile = new File( outputDir, "test-app/" + tplFile.getName().replace( ".tpl", "" ));
			Assert.assertTrue( tplFile.getName(), outputFile.exists());

			File expectedFile =  TestUtils.findTestFile( "/output/" + outputFile.getName());
			Assert.assertTrue( expectedFile.getName(), expectedFile.exists());

			String expectedFileContent = Utils.readFileContent( expectedFile );
			String fileContent = Utils.readFileContent( outputFile );
			Assert.assertEquals( tplFile.getName(), expectedFileContent, fileContent );
		}
	}


	@Test
	public void testGenerationWithInvalidTemplates() throws Exception {

		// Generate files for this application from different templates
		Logger logger = Logger.getLogger( getClass().getName());
		File outputDir = this.folder.newFolder();

		List<File> templateFiles = TestUtils.findTestFiles( "/invalid-templates" );
		Assert.assertFalse( templateFiles.isEmpty());

		// The watcher is not started, it just in charge of compiling the templates...
		File templatesDir = templateFiles.get( 0 ).getParentFile();
		TemplateWatcher watcher = new TemplateWatcher( null, templatesDir, 100 );

		// Compare output files with what was expected
		for( File tplFile : templateFiles ) {

			TemplateEntry te = watcher.compileTemplate( tplFile );
			Assert.assertNotNull( tplFile.getName(), te );
			TemplateUtils.generate( testApplicationForTemplates(), outputDir, Collections.singleton( te ), logger );

			File outputFile = new File( outputDir, "test-app/" + tplFile.getName().replace( ".tpl", "" ));
			Assert.assertTrue( tplFile.getName(), outputFile.exists());

			String fileContent = Utils.readFileContent( outputFile );
			Assert.assertEquals( tplFile.getName(), "", fileContent.trim());
		}
	}
}
