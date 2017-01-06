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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * The <strong>package</strong> mojo.
 * @author Vincent Zurczak - Linagora
 */
@Mojo( name="package-application", defaultPhase = LifecyclePhase.PACKAGE )
public class PackageApplicationMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject project;

	@Component( role = Archiver.class, hint = "zip" )
	private ZipArchiver zipArchiver;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Check the target file
		File modelDir = new File( this.project.getBuild().getOutputDirectory());
		if( ! modelDir.exists())
			throw new MojoExecutionException( "The model could not be found. " + modelDir );

		File targetDir = new File( this.project.getBuild().getDirectory());
		String archiveName = this.project.getBuild().getFinalName() + ".zip";
		File archiveFile = new File( targetDir, archiveName );

		// Add the application directories
		try {
			this.zipArchiver.addDirectory( modelDir );
			this.zipArchiver.setCompress( true );
			this.zipArchiver.setDestFile( archiveFile );
			this.zipArchiver.createArchive();

			this.project.getArtifact().setFile( archiveFile );

		} catch( Exception e ) {
			throw new MojoExecutionException( "Exception while packaging.", e );
		}
	}
}
