/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.tooling.core.validation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.ProjectUtils;
import net.roboconf.tooling.core.ProjectUtils.CreationBean;
import net.roboconf.tooling.core.validation.ProjectValidator.ProjectValidationResult;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ProjectValidatorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void validateProject() throws Exception {

		// Basic validation from a new project
		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0-SNAPSHOT" ).mavenProject( false );

		ProjectUtils.createProjectSkeleton( dir, bean );

		ProjectValidationResult result = ProjectValidator.validateProject( dir );
		Assert.assertNotNull( result.getRawParsingResult());

		List<RoboconfError> errors = result.getErrors();
		Assert.assertEquals( 2, errors.size());
		for( RoboconfError roboconfError : errors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, roboconfError.getErrorCode());

		// Now, add an invalid graph file
		File invalidGraphFile = new File( dir, Constants.PROJECT_DIR_GRAPH + "/invalid" + Constants.FILE_EXT_GRAPH );
		Utils.writeStringInto( "comp {\n\tinst: script;\n}\n", invalidGraphFile );

		File invalidInstanceFile = new File( dir, Constants.PROJECT_DIR_INSTANCES + "/invalid" + Constants.FILE_EXT_INSTANCES );
		Utils.writeStringInto( "instance of inexisting {\n\tname: toto;\n}\n", invalidInstanceFile );

		errors = ProjectValidator.validateProject( dir ).getErrors();
		Assert.assertEquals( 6, errors.size());

		Assert.assertEquals( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.CO_INEXISTING_COMPONENT, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 2 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 3 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_UNREACHABLE_FILE, errors.get( 4 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_UNREACHABLE_FILE, errors.get( 5 ).getErrorCode());

		List<File> unreachableFiles = new ArrayList<> ();
		unreachableFiles.add(((ParsingError) errors.get( 4 )).getFile());
		unreachableFiles.add(((ParsingError) errors.get( 5 )).getFile());

		Assert.assertTrue( unreachableFiles.contains( invalidInstanceFile ));
		Assert.assertTrue( unreachableFiles.contains( invalidGraphFile ));

		// Verify these errors are not ALL found when we load the application
		errors = new ArrayList<>( RuntimeModelIo.loadApplication( dir ).getLoadErrors());
		Assert.assertEquals( 4, errors.size());

		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_UNREACHABLE_FILE, errors.get( 2 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_UNREACHABLE_FILE, errors.get( 3 ).getErrorCode());
	}


	@Test
	public void validateProject_withMavenVersion_butNotMavenProject() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "${project.version}" ).mavenProject( false );

		ProjectUtils.createProjectSkeleton( dir, bean );

		List<RoboconfError> errors = ProjectValidator.validateProject( dir ).getErrors();
		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 2 ).getErrorCode());
	}


	@Test
	public void validateProject_withMavenVersion() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "${project.version}" ).mavenProject( true );

		ProjectUtils.createProjectSkeleton( dir, bean );
		File modelDirectory = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDirectory.isDirectory());

		List<RoboconfError> errors = ProjectValidator.validateProject( modelDirectory ).getErrors();
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void validateProject_withMavenProperty_noPrefixIsAccepted() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "kikou ${my-property}" ).mavenProject( true );

		ProjectUtils.createProjectSkeleton( dir, bean );
		File modelDirectory = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDirectory.isDirectory());

		List<RoboconfError> errors = ProjectValidator.validateProject( modelDirectory ).getErrors();
		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 2 ).getErrorCode());
	}


	@Test
	public void validateProject_withMavenProperty_suffixCannotContainSpaces() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "${my-property} kikou" ).mavenProject( true );

		ProjectUtils.createProjectSkeleton( dir, bean );
		File modelDirectory = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDirectory.isDirectory());

		List<RoboconfError> errors = ProjectValidator.validateProject( modelDirectory ).getErrors();
		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 2 ).getErrorCode());
	}


	@Test
	public void validateProject_withMavenProperty_validSuffix() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "${my-property}-now" ).mavenProject( true );

		ProjectUtils.createProjectSkeleton( dir, bean );
		File modelDirectory = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDirectory.isDirectory());

		List<RoboconfError> errors = ProjectValidator.validateProject( modelDirectory ).getErrors();
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void validateReusableRecipeProject_noDescriptor_noInstances_withoutMaven() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0.0" ).mavenProject( false );

		ProjectUtils.createProjectSkeleton( dir, bean );
		Utils.deleteFilesRecursively( new File( dir, Constants.PROJECT_DIR_DESC ));
		Utils.deleteFilesRecursively( new File( dir, Constants.PROJECT_DIR_INSTANCES ));

		ProjectValidationResult pvr = ProjectValidator.validateProject( dir );
		Assert.assertTrue( pvr.isRecipe());
	}


	@Test
	public void validateReusableRecipeProject_withDescriptor_noInstancesDir_withoutMaven() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0.0" ).mavenProject( false );

		ProjectUtils.createProjectSkeleton( dir, bean );
		Utils.deleteFilesRecursively( new File( dir, Constants.PROJECT_DIR_INSTANCES ));

		ProjectValidationResult pvr = ProjectValidator.validateProject( dir );
		Assert.assertFalse( pvr.isRecipe());
	}


	@Test
	public void validateReusableRecipeProject_withDescriptor_noInstancesFile_withoutMaven() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0.0" ).mavenProject( false );

		ProjectUtils.createProjectSkeleton( dir, bean );
		Utils.deleteFilesRecursively( new File( dir, Constants.PROJECT_DIR_INSTANCES ));
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_INSTANCES ).mkdir());

		ProjectValidationResult pvr = ProjectValidator.validateProject( dir );
		Assert.assertFalse( pvr.isRecipe());
	}


	@Test
	public void validateReusableRecipeProject_noDescriptor_withInstances_withoutMaven() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0.0" ).mavenProject( false );

		ProjectUtils.createProjectSkeleton( dir, bean );
		Utils.deleteFilesRecursively( new File( dir, Constants.PROJECT_DIR_DESC ));

		ProjectValidationResult pvr = ProjectValidator.validateProject( dir );
		Assert.assertFalse( pvr.isRecipe());
	}


	@Test
	public void validateReusableRecipeProject_noDescriptor_noInstances_withMaven() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0.0" ).mavenProject( true );

		ProjectUtils.createProjectSkeleton( dir, bean );
		File modelDirectory = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDirectory.isDirectory());

		Utils.deleteFilesRecursively( new File( modelDirectory, Constants.PROJECT_DIR_DESC ));
		Utils.deleteFilesRecursively( new File( modelDirectory, Constants.PROJECT_DIR_INSTANCES ));

		ProjectValidationResult pvr = ProjectValidator.validateProject( modelDirectory );
		Assert.assertTrue( pvr.isRecipe());
	}


	@Test
	public void validateReusableRecipeProject_realRecipe_withMaven() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0.0" ).mavenProject( true );

		ProjectUtils.createProjectSkeleton( dir, bean );
		File modelDirectory = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDirectory.isDirectory());

		Utils.deleteFilesRecursively( new File( modelDirectory, Constants.PROJECT_DIR_DESC ));
		Utils.deleteFilesRecursively( new File( modelDirectory, Constants.PROJECT_DIR_INSTANCES ));
		Utils.deleteFilesRecursively( new File( modelDirectory, Constants.PROJECT_DIR_GRAPH ));

		String recipeContent = "toto {\n\tinstaller: script;\n}\n";
		File graphFile = new File( modelDirectory, Constants.PROJECT_DIR_GRAPH + "/recipe.graph" );
		Assert.assertTrue( graphFile.getParentFile().mkdir());
		Utils.writeStringInto( recipeContent, graphFile );

		ProjectValidationResult pvr = ProjectValidator.validateProject( modelDirectory );
		Assert.assertTrue( pvr.isRecipe());
		Assert.assertEquals( 1, pvr.getErrors().size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, pvr.getErrors().get( 0 ).getErrorCode());
	}
}
