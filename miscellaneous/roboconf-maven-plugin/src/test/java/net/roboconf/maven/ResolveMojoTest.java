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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.tooling.core.ProjectUtils;
import net.roboconf.tooling.core.ProjectUtils.CreationBean;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ResolveMojoTest extends AbstractTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final Map<String,Artifact> artifactIdToArtifact = new HashMap<> ();


	@Before
	public void resetArtifactCache() {
		this.artifactIdToArtifact.clear();
	}


	@Test
	public void testNoDependency() throws Exception {
		final String projectName = "project--valid";

		File baseDir = this.resources.getBasedir( projectName );
		Assert.assertNotNull( baseDir );
		Assert.assertTrue( baseDir.isDirectory());

		File targetDir = new File( baseDir, MavenPluginConstants.TARGET_MODEL_DIRECTORY + "/" + Constants.PROJECT_DIR_GRAPH );
		Assert.assertFalse( targetDir.isDirectory());
		findMojo( projectName, "resolve" ).execute();

		// The mojo does not create the directory if there is no dependency
		Assert.assertFalse( targetDir.isDirectory());
	}


	@Test( expected = MojoExecutionException.class )
	public void testWithInvalidRoboconfDependencies() throws Exception {

		// Prepare the project
		final String projectName = "project--valid";

		File baseDir = this.resources.getBasedir( projectName );
		Assert.assertNotNull( baseDir );
		Assert.assertTrue( baseDir.isDirectory());

		AbstractMojo mojo = findMojo( projectName, "resolve" );
		this.rule.setVariableValueToObject( mojo, "repoSystem", newRepositorySystem());
		this.rule.setVariableValueToObject( mojo, "repositories", new ArrayList<RemoteRepository>( 0 ));

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
	}


	@Test
	public void testWithValidRoboconfDependencies() throws Exception {

		// Prepare the project
		final String projectName = "project--valid";

		File baseDir = this.resources.getBasedir( projectName );
		Assert.assertNotNull( baseDir );
		Assert.assertTrue( baseDir.isDirectory());

		AbstractMojo mojo = findMojo( projectName, "resolve" );
		this.rule.setVariableValueToObject( mojo, "repoSystem", newRepositorySystem());
		this.rule.setVariableValueToObject( mojo, "repositories", new ArrayList<RemoteRepository>( 0 ));

		// Create a Roboconf application
		File dir = this.folder.newFolder();
		File targetZipFile = this.folder.newFile();
		Assert.assertTrue( targetZipFile.delete());

		CreationBean bean = new CreationBean()
				.projectDescription( "some desc" ).projectName( "my-project" )
				.groupId( "net.roboconf" ).projectVersion( "1.0-SNAPSHOT" ).mavenProject( false );

		ProjectUtils.createProjectSkeleton( dir, bean );
		ZipArchiver zipArchiver = new ZipArchiver();
		zipArchiver.addDirectory( dir );
		zipArchiver.setCompress( true );
		zipArchiver.setDestFile( targetZipFile );
		zipArchiver.createArchive();

		Assert.assertTrue( targetZipFile.isFile());

		// Add dependencies
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( mojo, "project" );
		project.setDependencyArtifacts( new HashSet<Artifact> ());

		Artifact rbcfArtifact3 = new DefaultArtifact( "net.roboconf", "roboconf-core", "0.2", "runtime", "jar", null, new DefaultArtifactHandler());
		rbcfArtifact3.setFile( targetZipFile );
		project.getDependencyArtifacts().add( rbcfArtifact3 );

		// Add it to our "local" repository
		this.artifactIdToArtifact.put( rbcfArtifact3.getArtifactId(), rbcfArtifact3 );

		// Execute it
		File targetDir = new File( baseDir, MavenPluginConstants.TARGET_MODEL_DIRECTORY + "/" + Constants.PROJECT_DIR_GRAPH );
		Assert.assertFalse( targetDir.isDirectory());
		mojo.execute();

		// Verify the import was copied in the right location
		File importDir = new File( targetDir, "net.roboconf/roboconf-core" );
		Assert.assertTrue( importDir.isDirectory());
		Assert.assertTrue( new File( importDir, "main.graph" ).isFile());
	}


	private RepositorySystem newRepositorySystem() {

		RepositorySystem repoSystem = new DefaultRepositorySystem() {
			@Override
			public ArtifactResult resolveArtifact( RepositorySystemSession session, ArtifactRequest request )
			throws ArtifactResolutionException {

				ArtifactResult res = new ArtifactResult( request );
				Artifact mavenArtifact = ResolveMojoTest.this.artifactIdToArtifact.get( request.getArtifact().getArtifactId());
				if( mavenArtifact == null )
					throw new ArtifactResolutionException( new ArrayList<ArtifactResult>( 0 ), "Error in test wrapper and settings." );

				org.eclipse.aether.artifact.DefaultArtifact art =
						new org.eclipse.aether.artifact.DefaultArtifact(
								"groupId", "artifactId", "classifier", "extension", "version",
								null, mavenArtifact.getFile());

				res.setArtifact( art );
				return res;
			}
		};

		return repoSystem;
	}
}
