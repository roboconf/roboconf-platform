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

package net.roboconf.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ValidateApplicationMojoTest extends AbstractTest {

	@Test( expected = MojoFailureException.class )
	public void testValidProjectButInvalidApp() throws Exception {

		findAndExecuteMojo( "project--invalid-app", true );
	}


	@Test
	public void testValidAppProjectWithWarnings() throws Exception {

		findAndExecuteMojo( "project--valid-with-warnings", true );
		// No exception thrown
	}


	@Test( expected = MojoExecutionException.class )
	public void testValidAppButNotInTarget() throws Exception {

		ValidateApplicationMojo mojo = (ValidateApplicationMojo) super.findMojo( "project--valid", "validate-application" );
		mojo.execute();
	}


	@Test
	public void testValidAppProject() throws Exception {

		findAndExecuteMojo( "project--valid", false );
		// No exception thrown
	}


	@Test
	public void testRecipe_nonOfficial_withWarnings_1() throws Exception {

		// Find and execute the mojo
		ValidateApplicationMojo mojo = retriveAndPrepareMojo( "recipe" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		this.rule.setVariableValueToObject( mojo, "recipe", true );

		mojo.execute();

		// We should have a result file
		File resultsFile = new File( project.getBasedir(), MavenPluginConstants.VALIDATION_RESULT_PATH );
		Assert.assertTrue( resultsFile.exists());
	}


	@Test
	public void testRecipe_nonOfficial_withWarnings_2() throws Exception {

		// Find and execute the mojo
		ValidateApplicationMojo mojo = retriveAndPrepareMojo( "recipe-with-instances" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		this.rule.setVariableValueToObject( mojo, "recipe", true );

		mojo.execute();

		// We should have a result file
		File resultsFile = new File( project.getBasedir(), MavenPluginConstants.VALIDATION_RESULT_PATH );
		Assert.assertTrue( resultsFile.exists());
	}


	@Test
	public void testRecipe_official_exactGroupId() throws Exception {

		// Find and execute the mojo
		ValidateApplicationMojo mojo = retriveAndPrepareMojo( "recipe" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		this.rule.setVariableValueToObject( mojo, "recipe", true );
		this.rule.setVariableValueToObject( mojo, "official", true );

		project.setGroupId( Constants.OFFICIAL_RECIPES_GROUP_ID );
		project.setArtifactId( project.getName());
		Assert.assertTrue( new File( project.getBasedir(), "readme" ).createNewFile());

		mojo.execute();

		// We should not have a result file
		File resultsFile = new File( project.getBasedir(), MavenPluginConstants.VALIDATION_RESULT_PATH );
		Assert.assertFalse( resultsFile.exists());
	}


	@Test
	public void testRecipe_official_anotherGroupId() throws Exception {

		// Find and execute the mojo
		ValidateApplicationMojo mojo = retriveAndPrepareMojo( "recipe" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		this.rule.setVariableValueToObject( mojo, "recipe", true );
		this.rule.setVariableValueToObject( mojo, "official", true );

		project.setGroupId( Constants.OFFICIAL_RECIPES_GROUP_ID + ".database" );
		project.setArtifactId( project.getName());
		Assert.assertTrue( new File( project.getBasedir(), "readme" ).createNewFile());

		mojo.execute();

		// We should not have a result file
		File resultsFile = new File( project.getBasedir(), MavenPluginConstants.VALIDATION_RESULT_PATH );
		Assert.assertFalse( resultsFile.exists());
	}


	@Test( expected = MojoFailureException.class )
	public void testRecipe_official_fail_1() throws Exception {

		ValidateApplicationMojo mojo = retriveAndPrepareMojo( "recipe" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		this.rule.setVariableValueToObject( mojo, "recipe", true );
		this.rule.setVariableValueToObject( mojo, "official", true );

		project.setName( "ReCipe" );
		mojo.execute();
	}


	@Test( expected = MojoFailureException.class )
	public void testRecipe_official_fail_2() throws Exception {

		ValidateApplicationMojo mojo = retriveAndPrepareMojo( "recipe-with-instances" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		this.rule.setVariableValueToObject( mojo, "recipe", true );
		this.rule.setVariableValueToObject( mojo, "official", true );

		project.setName( "ReCipe" );
		mojo.execute();
	}



	private void findAndExecuteMojo( String projectName, boolean expectTextFile ) throws Exception {

		ValidateApplicationMojo mojo = retriveAndPrepareMojo( projectName );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );

		// Execute the mojo
		mojo.execute();

		// Should we have a result file?
		File resultsFile = new File( project.getBasedir(), MavenPluginConstants.VALIDATION_RESULT_PATH );
		Assert.assertEquals( expectTextFile, resultsFile.exists());
	}



	private ValidateApplicationMojo retriveAndPrepareMojo( String projectName ) throws Exception {

		// Get the mojo
		ValidateApplicationMojo mojo = (ValidateApplicationMojo) super.findMojo( projectName, "validate-application" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		Assert.assertNotNull( project );

		// Copy the resources
		Utils.copyDirectory(
				new File( project.getBasedir(), MavenPluginConstants.SOURCE_MODEL_DIRECTORY ),
				new File( project.getBuild().getOutputDirectory()));

		return mojo;
	}
}
