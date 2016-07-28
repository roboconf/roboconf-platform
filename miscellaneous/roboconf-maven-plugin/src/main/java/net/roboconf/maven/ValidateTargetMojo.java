/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;

/**
 * The mojo in charge of checking the target properties.
 * @author Vincent Zurczak - Linagora
 */
@Mojo( name="validate-target", defaultPhase = LifecyclePhase.COMPILE )
public class ValidateTargetMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject project;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Find the target directory
		File outputDirectory = new File( this.project.getBuild().getOutputDirectory());
		if( ! outputDirectory.isDirectory())
			throw new MojoExecutionException( "The target directory could not be found. " + outputDirectory );

		// Load and validate the target properties
		Set<String> targetIds = new HashSet<> ();
		try {
			for( File f: Utils.listAllFiles( outputDirectory, Constants.FILE_EXT_PROPERTIES )) {

				Properties props = Utils.readPropertiesFile( f );
				String id = props.getProperty( Constants.TARGET_PROERTY_ID );
				if( Utils.isEmptyOrWhitespaces( id ))
					throw new MojoFailureException( "The target ID is missing or invalid in " + f.getName());

				if( targetIds.contains( id ))
					throw new MojoFailureException( "Target ID '" + id + "' is used more than once in this archive." );

				targetIds.add( id );
				String handler = props.getProperty( Constants.TARGET_PROPERTY_HANDLER );
				if( Utils.isEmptyOrWhitespaces( handler ))
					throw new MojoFailureException( "The target handler is missing or invalid in " + f.getName());

				String name = props.getProperty( Constants.TARGET_PROPERTY_NAME );
				if( Utils.isEmptyOrWhitespaces( name ))
					getLog().warn( "[ warning ] The 'name' property is missing or invalid in " + f.getName());
			}

		} catch( IOException e ) {
			throw new MojoExecutionException( "A target properties file could not be read.", e );
		}

		// There should be properties files
		if( targetIds.isEmpty())
			throw new MojoFailureException( "No properties file was found in the project." );
	}
}
