/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.tooling.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.ProjectUtils.CreationBean;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ProjectUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testSimpleProject() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0-SNAPSHOT" ).mavenProject( false );

		Assert.assertEquals( 0, dir.listFiles().length );
		ProjectUtils.createProjectSkeleton( dir, bean );
		Assert.assertEquals( 6, dir.listFiles().length );

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertEquals( 2, alr.getLoadErrors().size());
		for( RoboconfError roboconfError : alr.getLoadErrors())
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, roboconfError.getErrorCode());

		Assert.assertEquals( bean.getProjectDescription(), alr.getApplicationTemplate().getDescription());
		Assert.assertEquals( bean.getProjectName(), alr.getApplicationTemplate().getName());
		Assert.assertEquals( bean.getProjectVersion(), alr.getApplicationTemplate().getVersion());
	}


	@Test
	public void testSimpleProjectForReusableRecipe() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0-SNAPSHOT" )
							.mavenProject( false ).reusableRecipe( true );

		Assert.assertEquals( 0, dir.listFiles().length );
		ProjectUtils.createProjectSkeleton( dir, bean );
		Assert.assertEquals( 2, dir.listFiles().length );

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );
		List<RoboconfError> errors = new ArrayList<>( alr.getLoadErrors());

		// These are the raw errors (not filtered for recipes)
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_DESC_DIR, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_UNREACHABLE_FILE, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void testMavenProject() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0-SNAPSHOT" )
							.pluginVersion( "1.0.0" );

		Assert.assertEquals( 0, dir.listFiles().length );
		ProjectUtils.createProjectSkeleton( dir, bean );
		Assert.assertEquals( 2, dir.listFiles().length );
		Assert.assertTrue( new File( dir, "pom.xml" ).exists());

		File modelDir = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDir.exists());
		Assert.assertEquals( 6, modelDir.listFiles().length );

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( modelDir );
		List<RoboconfError> errors = new ArrayList<>( alr.getLoadErrors());

		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 2 ).getErrorCode());

		Assert.assertEquals( "${project.description}", alr.getApplicationTemplate().getDescription());
		Assert.assertEquals( bean.getProjectName(), alr.getApplicationTemplate().getName());
		Assert.assertEquals( "${project.version}", alr.getApplicationTemplate().getVersion());
	}


	@Test
	public void testMavenProjectForReusableRecipe() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my-project" )
							.groupId( "net.roboconf" ).projectVersion( "1.0-SNAPSHOT" )
							.pluginVersion( "1.0.0" ).reusableRecipe( true );

		Assert.assertEquals( 0, dir.listFiles().length );
		ProjectUtils.createProjectSkeleton( dir, bean );
		Assert.assertEquals( 2, dir.listFiles().length );
		Assert.assertTrue( new File( dir, "pom.xml" ).exists());

		File modelDir = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDir.exists());
		Assert.assertEquals( 2, modelDir.listFiles().length );

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( modelDir );
		List<RoboconfError> errors = new ArrayList<>( alr.getLoadErrors());

		// These are the raw errors (not filtered for recipes)
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_DESC_DIR, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_UNREACHABLE_FILE, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void testMavenProjectWithCustomPom() throws Exception {

		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
							.projectDescription( "some desc" ).projectName( "my project" )
							.projectVersion( "1.0-SNAPSHOT" ).pluginVersion( "1.0.0" )
							.groupId( "net.roboconf" ).artifactId( "roboconf-sample" );

		File pomSkeleton = TestUtils.findTestFile( "/test-pom-skeleton.xml" );
		Assert.assertTrue( pomSkeleton.exists());
		bean.customPomLocation( pomSkeleton.getAbsolutePath());

		Assert.assertEquals( 0, dir.listFiles().length );
		ProjectUtils.createProjectSkeleton( dir, bean );
		Assert.assertEquals( 2, dir.listFiles().length );

		File pomFile = new File( dir, "pom.xml" );
		File expectedPom = TestUtils.findTestFile( "/expected-pom.xml" );
		Assert.assertTrue( expectedPom.exists());
		Assert.assertTrue( pomFile.exists());

		String actual = Utils.readFileContent( pomFile );
		String expected = Utils.readFileContent( expectedPom );
		Assert.assertEquals( expected, actual );

		File modelDir = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		Assert.assertTrue( modelDir.exists());
		Assert.assertEquals( 6, modelDir.listFiles().length );

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( modelDir );
		List<RoboconfError> errors = new ArrayList<>( alr.getLoadErrors());

		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 2 ).getErrorCode());

		Assert.assertEquals( "${project.description}", alr.getApplicationTemplate().getDescription());
		Assert.assertEquals( bean.getProjectName(), alr.getApplicationTemplate().getName());
		Assert.assertEquals( "${project.version}", alr.getApplicationTemplate().getVersion());
	}


	@Test
	public void testListPluginVersions() {
		Assert.assertNotNull( ProjectUtils.listMavenPluginVersions());
	}


	@Test
	public void testGetNonNullString() {

		Assert.assertEquals( "", CreationBean.getNonNullString( null ));
		Assert.assertEquals( "", CreationBean.getNonNullString( "" ));
		Assert.assertEquals( "", CreationBean.getNonNullString( "  " ));
		Assert.assertEquals( "toto", CreationBean.getNonNullString( " toto " ));
	}


	@Test
	public void testCreateRecipeDirectories_simpleProject() throws Exception {

		// Create a simple project
		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
				.projectDescription( "some desc" ).projectName( "my-project" )
				.groupId( "net.roboconf" ).projectVersion( "1.0-SNAPSHOT" ).mavenProject( false );

		ProjectUtils.createProjectSkeleton( dir, bean );
		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );

		// Recipe directories are missing
		Assert.assertEquals( 2, alr.getLoadErrors().size());
		for( RoboconfError error : alr.getLoadErrors())
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		// Create them and verify
		ProjectUtils.createRecipeDirectories( dir );
		alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertEquals( 0, alr.getLoadErrors().size());

		// The method should be idem-potent
		ProjectUtils.createRecipeDirectories( dir );
		alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertEquals( 0, alr.getLoadErrors().size());
	}


	@Test
	public void testCreateRecipeDirectories_mavenProject() throws Exception {

		// Create a Maven project
		File dir = this.folder.newFolder();
		CreationBean bean = new CreationBean()
				.projectDescription( "some desc" ).projectName( "my project" )
				.projectVersion( "1.0-SNAPSHOT" ).pluginVersion( "1.0.0" )
				.groupId( "net.roboconf" ).artifactId( "roboconf-sample" );

		// Recipe directories are missing
		ProjectUtils.createProjectSkeleton( dir, bean );
		dir = new File( dir, Constants.MAVEN_SRC_MAIN_MODEL );
		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );

		// Create them and verify
		Assert.assertEquals( 3, alr.getLoadErrors().size());
		Iterator<RoboconfError> it = alr.getLoadErrors().iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, it.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, it.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, it.next().getErrorCode());
		Assert.assertFalse( it.hasNext());

		List<File> createdDirectories = ProjectUtils.createRecipeDirectories( dir );
		alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertEquals( 1, alr.getLoadErrors().size());
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, alr.getLoadErrors().iterator().next().getErrorCode());
		Assert.assertEquals( 2, createdDirectories.size());

		// The method should be idem-potent
		createdDirectories = ProjectUtils.createRecipeDirectories( dir );
		alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertEquals( 1, alr.getLoadErrors().size());
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, alr.getLoadErrors().iterator().next().getErrorCode());
		Assert.assertEquals( 0, createdDirectories.size());
	}
}
