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
import java.util.List;

import net.roboconf.core.utils.Utils;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InitializeMojoTest {

	@Rule
	public MojoRule rule = new MojoRule();

	@Rule
	public TestResources resources = new TestResources();


	@Test
	public void checkSourcesDirectoriesAreUpdated() throws Exception {

		// Find the project
		File baseDir = this.resources.getBasedir( "project--valid" );
		Assert.assertNotNull( baseDir );
		Assert.assertTrue( baseDir.exists());
		Assert.assertTrue( baseDir.isDirectory());

		File pom = new File( baseDir, "pom.xml" );
		InitializeMojo mojo = (InitializeMojo) this.rule.lookupMojo( "initialize", pom );
		Assert.assertNotNull( mojo );

		// Create the Maven project by hand (...)
		final MavenProject mvnProject = new MavenProject() ;
		mvnProject.setFile( pom ) ;
		this.rule.setVariableValueToObject( mojo, "project", mvnProject );
		Assert.assertNotNull( this.rule.getVariableValueFromObject( mojo, "project" ));

		// Execute the mojo
		List<Resource> list = mvnProject.getResources();
		Assert.assertEquals( 0, list.size());
		Assert.assertTrue( Utils.isEmptyOrWhitespaces( mvnProject.getBuild().getOutputDirectory()));

		mojo.execute();

		list = mvnProject.getResources();
		Assert.assertEquals( 2, list.size());

		Resource res = list.get( 0 );
		Assert.assertTrue( res.isFiltering());
		Assert.assertEquals( new File( baseDir, MavenPluginConstants.SOURCE_MODEL_DIRECTORY ).getAbsolutePath(), res.getDirectory());

		res = list.get( 1 );
		Assert.assertFalse( res.isFiltering());
		Assert.assertEquals( new File( baseDir, MavenPluginConstants.SOURCE_MODEL_DIRECTORY ).getAbsolutePath(), res.getDirectory());

		File expectedFile = new File( mvnProject.getBasedir(), MavenPluginConstants.TARGET_MODEL_DIRECTORY );
		Assert.assertEquals( expectedFile.getAbsolutePath(), mvnProject.getBuild().getOutputDirectory());
	}
}
