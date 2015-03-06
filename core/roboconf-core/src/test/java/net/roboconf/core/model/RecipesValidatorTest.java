/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RecipesValidatorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testBashValidation_noScriptAtAll() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "bash" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, RecipesValidator.SCRIPTS_DIR_NAME ).mkdirs());

		Assert.assertEquals( 0, RecipesValidator.validateComponentRecipes( appDir, comp ).size());
	}


	@Test
	public void testBashValidation_success() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "bash" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, RecipesValidator.SCRIPTS_DIR_NAME ).mkdirs());
		Utils.writeStringInto( RecipesValidator.BASH_DIRECTIVE + "\ntest", new File( directory, RecipesValidator.SCRIPTS_DIR_NAME + "/test.sh" ));

		Assert.assertEquals( 0, RecipesValidator.validateComponentRecipes( appDir, comp ).size());
	}


	@Test
	public void testBashValidation_noScriptsDirectory() throws Exception {

		File appDir = this.folder.newFolder();
		Component comp = new Component( "toto" ).installerName( "bash" );
		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( directory.mkdirs());
		Utils.writeStringInto( RecipesValidator.BASH_DIRECTIVE + "\n", new File( directory, "test.sh" ));

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_BASH_NO_SCRIPTS_DIR, errors.get( 0 ).getErrorCode());
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
		comp.importedVariables.put( "Other.*", Boolean.TRUE );

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
		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_ADDED, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_REMOVED, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_COMP, errors.get( 2 ).getErrorCode());
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
		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_ADDED, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_REMOVED, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_COMP, errors.get( 2 ).getErrorCode());
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
		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_ADDED, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_REMOVED, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_IMPORT_COMP, errors.get( 2 ).getErrorCode());
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
		comp.importedVariables.put( "Test.ip", Boolean.TRUE );
		comp.importedVariables.put( "Test.port", Boolean.TRUE );
		comp.importedVariables.put( "Oops.ip", Boolean.TRUE );

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/update.pp" );
		Utils.copyStream( inputFile, targetFile );

		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_FROM_IMPORT, errors.get( 0 ).getErrorCode());
		Assert.assertTrue( errors.get( 0 ).getDetails().contains( "Parameter: test" ));
		Assert.assertEquals( ErrorCode.REC_PUPPET_MISSING_PARAM_FROM_IMPORT, errors.get( 1 ).getErrorCode());
		Assert.assertTrue( errors.get( 1 ).getDetails().contains( "Parameter: oops" ));
	}


	@Test
	public void testPuppetValidation_invalidPuppetClass() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		comp.importedVariables.put( "Test.ip", Boolean.TRUE );
		comp.importedVariables.put( "Test.port", Boolean.TRUE );
		comp.importedVariables.put( "Oops.ip", Boolean.TRUE );

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		Utils.writeStringInto( "Not a valid puppet class...", targetFile );

		// We only parse what we know...
		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testPuppetValidation_withMatchingImport() throws Exception {
		File appDir = this.folder.newFolder();

		Component comp = new Component( "toto" ).installerName( "puppet" );
		comp.importedVariables.put( "WithInit.value", Boolean.TRUE );

		File directory = ResourceUtils.findInstanceResourcesDirectory( appDir, comp );
		Assert.assertTrue( new File( directory, "roboconf_toto/manifests" ).mkdirs());

		File targetFile = new File( directory, "roboconf_toto/manifests/start.pp" );
		File inputFile = TestUtils.findTestFile( "/recipes/update.pp" );
		Utils.copyStream( inputFile, targetFile );

		// We only parse what we know...
		List<ModelError> errors = RecipesValidator.validateComponentRecipes( appDir, comp );
		Assert.assertEquals( 0, errors.size());
	}
}
