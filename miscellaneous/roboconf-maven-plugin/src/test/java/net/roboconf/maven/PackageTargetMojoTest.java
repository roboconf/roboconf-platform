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

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PackageTargetMojoTest {

	@Rule
	public TestResources resources = new TestResources();

	@Rule
	public MojoRule rule = new MojoRule();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testValidTargetProject() throws Exception {

		final String finalName = "output";
		final String version = "1";

		// Copy the project
		File baseDirectory = this.resources.getBasedir( "target-ok-multi" );
		File compileDirectory = new File( baseDirectory, "target/test/classes" );
		File targetArchive = new File( baseDirectory, "target/" + finalName + ".zip" );
		Assert.assertFalse( targetArchive.exists());

		// Create the Maven project by hand
		File pom = new File( baseDirectory, "pom.xml" );
		final MavenProject mvnProject = new MavenProject() ;
		mvnProject.setFile( pom ) ;
		mvnProject.setVersion( version );
		mvnProject.getBuild().setFinalName( finalName );
		mvnProject.setArtifact( new ProjectArtifact( mvnProject ));
		mvnProject.getBuild().setDirectory( new File( baseDirectory, "target" ).getAbsolutePath());
		mvnProject.getBuild().setOutputDirectory( compileDirectory.getAbsolutePath());

		// Copy the resources - mimic what Maven would really do
		Utils.copyDirectory(
				new File( mvnProject.getBasedir(), "src/main/resources" ),
				new File( mvnProject.getBuild().getOutputDirectory()));

		// Package
		PackageTargetMojo packageTargetMojo = (PackageTargetMojo) this.rule.lookupMojo( "package-target", pom );
		this.rule.setVariableValueToObject( packageTargetMojo, "project", mvnProject );
		packageTargetMojo.execute();

		// Check assertions.
		// Unfortunately, no filtering here.
		Assert.assertTrue( targetArchive.exists());
		File unzipDirectory = this.folder.newFolder();
		Utils.extractZipArchive( targetArchive, unzipDirectory );

		List<File> files = Utils.listAllFiles( unzipDirectory );
		Assert.assertEquals( 2, files.size());
		Assert.assertTrue( files.contains( new File( unzipDirectory, "test1.properties" )));
		Assert.assertTrue( files.contains( new File( unzipDirectory, "test2.properties" )));
	}
}
