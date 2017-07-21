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

package net.roboconf.core.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails.ErrorDetailsKind;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 * @author Amadou Diarra - UGA
 */
public class RecipesValidatorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * @return {@code true} if and only if puppet is installed.
	 * @throws IOException when something bad happened.
	 */
	private boolean puppetIsInstalled() throws IOException {
		int exitCode;
		try {
			exitCode = ProgramUtils.executeCommand(
					this.logger,
					new String[]{"puppet", "-v"},
					null, null, null, null);
		} catch (InterruptedException | IOException e) {
			exitCode = 1;
		}
		return exitCode == 0;
	}


	@Test
	public void testScriptValidation_noScriptAtAll() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "script" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, RecipesValidator.SCRIPTS_DIR_NAME ).mkdirs());

		Assert.assertEquals( 0, RecipesValidator.validateComponentRecipes( appDir, comp ).size());
	}


	@Test
	public void testScriptValidation_success() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "script" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, RecipesValidator.SCRIPTS_DIR_NAME ).mkdirs());
		Utils.writeStringInto( "\ntest", new File( directory, RecipesValidator.SCRIPTS_DIR_NAME + "/test.sh" ));

		Assert.assertEquals( 0, RecipesValidator.validateComponentRecipes( appDir, comp ).size());
	}


	@Test
	public void testScriptValidation_noScriptsDirectory() throws Exception {

		File appDir = this.folder.newFolder();
		Component comp = new Component( "toto" ).installerName( "script" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( directory.mkdirs());
		Utils.writeStringInto( "", new File( directory, "test.sh" ));

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_SCRIPT_NO_SCRIPTS_DIR, errors.get( 0 ).getErrorCode());
	}

	@Test
	public void testEmptyRecipeDirectory() throws Exception {

		File appDir = this.folder.newFolder();
		Component comp = new Component("toto").installerName("script");
		File directory = ResourceUtils.findInstanceResourcesDirectory(appDir, comp);
		Assert.assertTrue(directory.mkdirs());

		List<ModelError> errors = RecipesValidator.validateComponentRecipes(appDir, comp);
		Assert.assertEquals(0, errors.size());
	}


	@Test
	public void testPuppetValidation_noModule() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( directory.mkdirs());

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_HAS_NO_RBCF_MODULE, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testPuppetValidation_twoModules() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto" ).mkdirs());
		Assert.assertTrue( new File( directory, "roBoconf_titi" ).mkdirs());

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_HAS_TOO_MANY_RBCF_MODULES, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testPuppetValidation_oneModuleButWildCardImports() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		comp.addImportedVariable( new ImportedVariable( "Other.*", true, false ));

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto" ).mkdirs());

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_DISLIKES_WILDCARD_IMPORTS, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testPuppetValidation_invalidUpdatePp() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/update.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/invalid-update.pp" );
		Utils.copyStream( inputFile, targetFile );

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_DIFF, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testPuppetValidation_validUpdatePp() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/update.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/update.pp" );
		Utils.copyStream( inputFile, targetFile );

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testPuppetValidation_invalidInitPp() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/init.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/invalid-update.pp" );
		Utils.copyStream( inputFile, targetFile );

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_DIFF, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testPuppetValidation_invalidUpdatePp_validInitPp() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/init.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/invalid-update.pp" );
		Utils.copyStream( inputFile, targetFile );

		targetFile = new File( directory, "roboconf_toto/manifests/update.pp" );
		Utils.copyStream( inputFile, targetFile );

		// The init.pp is not considered as being called during updates.
		// So, no error about imports.
		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_DIFF, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testPuppetValidation_onlyStartPp() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/invalid-update.pp" );
		Utils.copyStream( inputFile, targetFile );

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testPuppetValidation_missingRunningState() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/missing-running-state.pp" );
		Utils.copyStream( inputFile, targetFile );

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_RUNNING_STATE, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testPuppetValidation_missingParameterFromImports() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		comp.addImportedVariable( new ImportedVariable( "Test.ip", true, false ));
		comp.addImportedVariable( new ImportedVariable( "Test.port", true, false ));
		comp.addImportedVariable( new ImportedVariable( "Oops.ip", true, false ));

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/update.pp" );
		Utils.copyStream( inputFile, targetFile );

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 2, errors.size());

		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_FROM_IMPORT, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( 3, errors.get( 0 ).getDetails().length );

		Assert.assertEquals( ErrorDetailsKind.NAME, errors.get( 0 ).getDetails()[ 0 ].getErrorDetailsKind());
		Assert.assertEquals( "toto", errors.get( 0 ).getDetails()[ 0 ].getElementName());
		Assert.assertEquals( ErrorDetailsKind.FILE, errors.get( 0 ).getDetails()[ 1 ].getErrorDetailsKind());
		Assert.assertEquals( ErrorDetailsKind.EXPECTED, errors.get( 0 ).getDetails()[ 2 ].getErrorDetailsKind());
		Assert.assertEquals( "test", errors.get( 0 ).getDetails()[ 2 ].getElementName());

		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_FROM_IMPORT, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( 3, errors.get( 1 ).getDetails().length );

		Assert.assertEquals( ErrorDetailsKind.NAME, errors.get( 1 ).getDetails()[ 0 ].getErrorDetailsKind());
		Assert.assertEquals( "toto", errors.get( 1 ).getDetails()[ 0 ].getElementName());
		Assert.assertEquals( ErrorDetailsKind.FILE, errors.get( 1 ).getDetails()[ 1 ].getErrorDetailsKind());
		Assert.assertEquals( ErrorDetailsKind.EXPECTED, errors.get( 1 ).getDetails()[ 2 ].getErrorDetailsKind());
		Assert.assertEquals( "oops", errors.get( 1 ).getDetails()[ 2 ].getElementName());
	}


	@Test
	public void testPuppetValidation_invalidPuppetClass() throws Exception {
		Assume.assumeTrue(puppetIsInstalled());

		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		comp.addImportedVariable( new ImportedVariable( "Test.ip", true, false ));
		comp.addImportedVariable( new ImportedVariable( "Test.port", true, false ));
		comp.addImportedVariable( new ImportedVariable( "Oops.ip", true, false ));

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		Utils.writeStringInto( "Not a valid puppet class...", targetFile );

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_SYNTAX_ERROR, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testPuppetValidation_withMatchingImport() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		comp.addImportedVariable( new ImportedVariable( "WithInit.value", true, false ));

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/update.pp" );
		Utils.copyStream( inputFile, targetFile );

		// We only parse what we know...
		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testPuppetValidation_invalidSyntax() throws Exception {
		Assume.assumeTrue(puppetIsInstalled());

		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/invalid-syntax.pp" );
		Utils.copyStream( inputFile, targetFile );

		// We only parse what we know...
		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_SYNTAX_ERROR, errors.get( 0 ).getErrorCode());
	}
}
