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
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PackageApplicationMojoTest {

	@Rule
	public TestResources resources = new TestResources();

	@Rule
	public MojoRule rule = new MojoRule();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testValidAppProject() throws Exception {

		final String finalName = "output";
		final String version = "1";

		// Copy the project
		File targetDirectory = this.resources.getBasedir( "project--valid" );
		File targetArchive = new File( targetDirectory, "target/" + finalName + ".zip" );
		File modelDirectory = new File( targetDirectory, MavenPluginConstants.TARGET_MODEL_DIRECTORY );
		Assert.assertFalse( targetArchive.exists());
		Assert.assertFalse( modelDirectory.exists());

		// Create the Maven project by hand
		File pom = new File( targetDirectory, "pom.xml" );
		final MavenProject mvnProject = new MavenProject() ;
		mvnProject.setFile( pom ) ;
		mvnProject.setVersion( version );
		mvnProject.getBuild().setDirectory( modelDirectory.getParentFile().getAbsolutePath());
		mvnProject.getBuild().setOutputDirectory( modelDirectory.getAbsolutePath());
		mvnProject.getBuild().setFinalName( finalName );
		mvnProject.setArtifact( new ProjectArtifact( mvnProject ));

		// Copy the resources - mimic what Maven would really do
		Utils.copyDirectory(
				new File( mvnProject.getBasedir(), MavenPluginConstants.SOURCE_MODEL_DIRECTORY ),
				new File( mvnProject.getBuild().getOutputDirectory()));

		// Package
		PackageApplicationMojo packageApplicationMojo = (PackageApplicationMojo) this.rule.lookupMojo( "package-application", pom );
		this.rule.setVariableValueToObject( packageApplicationMojo, "project", mvnProject );
		packageApplicationMojo.execute();

		// Check assertions.
		// Unfortunately, no filtering here.
		Assert.assertTrue( targetArchive.exists());
		targetDirectory = this.folder.newFolder();
		Utils.extractZipArchive( targetArchive, targetDirectory );

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( targetDirectory );
		Assert.assertEquals( 0, alr.getLoadErrors().size());
		Assert.assertEquals( "1.0.0", alr.getApplicationTemplate().getVersion());

		File notFilteredFile = new File( targetDirectory, "graph/Tomcat/readme.md" );
		Assert.assertTrue( notFilteredFile.exists());

		String content = Utils.readFileContent( notFilteredFile );
		Assert.assertTrue( content.contains( "${project.version}" ));
		Assert.assertFalse( content.contains( "1.0-SNAPSHOT" ));
	}


	@Test( expected = MojoExecutionException.class )
	public void testInvalidAppProject() throws Exception {

		final String finalName = "output";
		final String version = "1";

		// Copy the project
		File targetDirectory = this.resources.getBasedir( "project--valid" );
		File targetArchive = new File( targetDirectory, "target/" + finalName + ".zip" );
		File modelDirectory = new File( targetDirectory, MavenPluginConstants.TARGET_MODEL_DIRECTORY );
		Assert.assertFalse( targetArchive.exists());
		Assert.assertFalse( modelDirectory.exists());

		// Create the Maven project by hand
		File pom = new File( targetDirectory, "pom.xml" );
		final MavenProject mvnProject = new MavenProject() ;
		mvnProject.setFile( pom ) ;
		mvnProject.setVersion( version );
		mvnProject.getBuild().setDirectory( modelDirectory.getAbsolutePath());
		mvnProject.getBuild().setOutputDirectory( modelDirectory.getParentFile().getAbsolutePath());
		mvnProject.getBuild().setFinalName( finalName );
		mvnProject.setArtifact( new ProjectArtifact( mvnProject ));

		// Do NOT copy the resources

		// Package
		PackageApplicationMojo packageApplicationMojo = (PackageApplicationMojo) this.rule.lookupMojo( "package-application", pom );
		this.rule.setVariableValueToObject( packageApplicationMojo, "project", mvnProject );
		packageApplicationMojo.execute();
	}
}
