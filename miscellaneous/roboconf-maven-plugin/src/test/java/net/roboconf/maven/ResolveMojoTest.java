/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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
import java.util.HashSet;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.Utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ResolveMojoTest extends AbstractTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testNoDependency() throws Exception {
		final String projectName = "project--valid";

		File baseDir = this.resources.getBasedir( projectName );
		Assert.assertNotNull( baseDir );
		Assert.assertTrue( baseDir.isDirectory());

		File targetDir = new File( baseDir, MavenPluginConstants.TARGET_MODEL_DIRECTORY + "/" + Constants.PROJECT_DIR_GRAPH );
		Assert.assertFalse( targetDir.isDirectory());
		findMojo( projectName, "resolve" ).execute();

		// The mojo does not create the directory is there is no dependency
		Assert.assertFalse( targetDir.isDirectory());
	}


	@Test
	public void testWithInvalidRoboconfDependencies() throws Exception {

		// Prepare the project
		final String projectName = "project--valid";

		File baseDir = this.resources.getBasedir( projectName );
		Assert.assertNotNull( baseDir );
		Assert.assertTrue( baseDir.isDirectory());

		AbstractMojo mojo = findMojo( projectName, "resolve" );

		// Add dependencies
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		project.setDependencyArtifacts( new HashSet<Artifact> ());

		Artifact notRbcfArtifact1 = new DefaultArtifact( "net.roboconf", "roboconf-core", "0.2", "runtime", "jar", null, new DefaultArtifactHandler());
		project.getDependencyArtifacts().add( notRbcfArtifact1 );

		Artifact notRbcfArtifact2 = new DefaultArtifact( "net.roboconf", "roboconf-core", "0.2", "runtime", "jar", null, new DefaultArtifactHandler());
		notRbcfArtifact2.setFile( new File( "file that does not exist" ));
		project.getDependencyArtifacts().add( notRbcfArtifact2 );

		Artifact notRbcfArtifact3 = new DefaultArtifact( "net.roboconf", "roboconf-core", "0.2", "runtime", "jar", null, new DefaultArtifactHandler());
		File temp = this.folder.newFile( "toto.zip" );
		Assert.assertTrue( temp.exists());

		notRbcfArtifact3.setFile( temp );
		project.getDependencyArtifacts().add( notRbcfArtifact3 );

		// Execute it
		File targetDir = new File( baseDir, MavenPluginConstants.TARGET_MODEL_DIRECTORY + "/" + Constants.PROJECT_DIR_GRAPH );
		Assert.assertFalse( targetDir.isDirectory());
		mojo.execute();

		Assert.assertFalse( targetDir.isDirectory());
	}


	@Test
	public void testWithRoboconfDependency() throws Exception {

		// Prepare the project
		final String projectName = "project--valid";

		File baseDir = this.resources.getBasedir( projectName );
		Assert.assertNotNull( baseDir );
		Assert.assertTrue( baseDir.isDirectory());

		AbstractMojo mojo = findMojo( projectName, "resolve" );

		// Add dependencies
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		project.setDependencyArtifacts( new HashSet<Artifact> ());

		Artifact dep = new DefaultArtifact( "net.roboconf", "recipe", "0.2", "runtime", "jar", null, new DefaultArtifactHandler());
		dep.setFile( zipRecipe());
		project.getDependencyArtifacts().add( dep );

		// Execute it
		File targetDir = new File( baseDir, MavenPluginConstants.TARGET_MODEL_DIRECTORY + "/" + Constants.PROJECT_DIR_GRAPH );
		Assert.assertFalse( targetDir.isDirectory());
		mojo.execute();

		Assert.assertTrue( targetDir.isDirectory());
		Assert.assertEquals( 2, targetDir.listFiles().length );
		Assert.assertTrue( new File( targetDir, "recipe" ).isDirectory());
		Assert.assertTrue( new File( targetDir, "recipe/lamp.graph" ).isFile());
		Assert.assertTrue( new File( targetDir, "MySQL" ).isDirectory());
		Assert.assertTrue( new File( targetDir, "MySQL/readme.md" ).isFile());
	}


	private File zipRecipe() throws Exception {

		File baseDir = this.resources.getBasedir( "recipe" );
		Assert.assertNotNull( baseDir );
		Assert.assertTrue( baseDir.isDirectory());

		File targetZipFile = this.folder.newFile( "dep.zip" );
		Map<String,String> entryToContent = Utils.storeDirectoryResourcesAsString( new File( baseDir, MavenPluginConstants.SOURCE_MODEL_DIRECTORY ));
		TestUtils.createZipFile( entryToContent, targetZipFile );

		return targetZipFile;
	}
}
