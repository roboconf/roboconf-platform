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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;

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
		this.rule.setVariableValueToObject( mojo, "repoSession", newRepositorySession());
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


	private RepositorySystemSession newRepositorySession() throws IOException {

		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		RepositorySystem repositorySystem = locator.getService( RepositorySystem.class );

		LocalRepository localRepo = new LocalRepository( this.folder.newFolder());

		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		LocalRepositoryManager lrm = repositorySystem.newLocalRepositoryManager( session, localRepo );
		session.setLocalRepositoryManager( lrm );

		return session;
	}
}
