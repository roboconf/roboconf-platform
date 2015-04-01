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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * The <strong>resolve</strong> mojo.
 * @author Vincent Zurczak - Linagora
 */
@Mojo( name="resolve", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class ResolveMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject project;

	@Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
	private ArtifactRepository local;



	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Find the target directory
		File completeAppDirectory = new File( this.project.getBuild().getOutputDirectory());

		// Copy the resources in the target directory
		Set<Artifact> artifacts = new HashSet<Artifact> ();
		if( this.project.getDependencyArtifacts() != null )
			artifacts.addAll( this.project.getDependencyArtifacts());

		for( Artifact unresolvedArtifact : artifacts ) {

			// Find the artifact in the local repository.
			Artifact art = this.local.find( unresolvedArtifact );

			// If necessary, resolve JAR as ZIP files.
			File file = art.getFile();
			if( art.getFile() == null ) {
				getLog().warn( "Artifact " + art.getArtifactId() + " has no attached file. Its content will not be copied in the target model directory." );
				continue;
			}

			String fixedFileName = file.getName().replaceFirst( "(?i)\\.jar$", ".zip" );
			file = new File( file.getParentFile(), fixedFileName );

			// Only accept ZIP files
			if( ! file.exists()) {
				getLog().warn( "Artifact " + art.getArtifactId() + " is not a ZIP file. Its content will not be copied in the target model directory." );
				continue;
			}

			// Prepare the extraction
			File temporaryDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf-temp" );
			File targetDirectory = new File( completeAppDirectory, Constants.PROJECT_DIR_GRAPH + "/" + art.getArtifactId());
			getLog().debug( "Copying the content of artifact " + art.getArtifactId() + " under " + targetDirectory );
			try {

				// Extract graph files - assumed to be at the root of the graph directory
				Utils.extractZipArchive( file, targetDirectory, "graph/[^/]*\\.graph", "graph/" );

				// Extract component files - directories
				Utils.extractZipArchive( file, targetDirectory.getParentFile(), "graph/.*/.*", "graph/" );

			} catch( IOException e ) {
				throw new MojoExecutionException( "The ZIP archive for artifact " + art.getArtifactId() + " could not be extracted.", e );

			} finally {
				Utils.deleteFilesRecursivelyAndQuitely( temporaryDirectory );
			}
		}
	}
}
