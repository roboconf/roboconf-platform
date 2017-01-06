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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ValidateTargetMojoTest extends AbstractTest {

	@Test
	public void testValidTargetProject_single() throws Exception {

		retrivePrepareAndExecuteMojo( "target-ok-single" );
		// No exception thrown
	}


	@Test
	public void testValidTargetProject_singleWithScripts() throws Exception {

		retrivePrepareAndExecuteMojo( "target-ok-with-scripts" );
		// No exception thrown
	}


	@Test
	public void testValidTargetProject_multi() throws Exception {

		retrivePrepareAndExecuteMojo( "target-ok-multi" );
		// No exception thrown
	}


	@Test( expected = MojoFailureException.class )
	public void testInvalidTarget_missingId() throws Exception {
		retrivePrepareAndExecuteMojo( "target-ko-missing-id" );
	}


	@Test( expected = MojoFailureException.class )
	public void testInvalidTarget_missingHandler() throws Exception {
		retrivePrepareAndExecuteMojo( "target-ko-missing-handler" );
	}


	@Test( expected = MojoFailureException.class )
	public void testInvalidTarget_conflictingIds() throws Exception {
		retrivePrepareAndExecuteMojo( "target-ko-conflicting-ids" );
	}


	@Test( expected = MojoFailureException.class )
	public void testEmptyTargetProject() throws Exception {

		ValidateTargetMojo mojo = (ValidateTargetMojo) super.findMojo( "target-ok-single", "validate-target" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		Assert.assertNotNull( project );

		// Do NOT copy the resources
		Utils.createDirectory( new File( project.getBuild().getOutputDirectory()));

		// Execute
		mojo.execute();
	}


	private void retrivePrepareAndExecuteMojo( String projectName ) throws Exception {

		// Get the mojo
		ValidateTargetMojo mojo = (ValidateTargetMojo) super.findMojo( projectName, "validate-target" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		Assert.assertNotNull( project );

		// Copy the resources
		Utils.copyDirectory(
				new File( project.getBasedir(), MavenPluginConstants.SOURCE_MAIN_RESOURCES ),
				new File( project.getBuild().getOutputDirectory()));

		// Execute
		mojo.execute();
	}
}
