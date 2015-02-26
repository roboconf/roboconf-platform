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

import net.roboconf.core.ErrorCode.ErrorLevel;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.Utils;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * The mojo in charge of checking the application.
 * <p>
 * It must be invoked only once dependencies have been resolved and
 * imported in the project. And after the original model files in this
 * project have been filtered by the maven-resources-plugin.
 * </p>
 * <p>
 * This mojo works on a directory under the "target" build directory.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@Mojo( name="validate-application", defaultPhase = LifecyclePhase.COMPILE )
public class ValidateApplicationMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject project;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Find the target directory
		File completeAppDirectory = new File( this.project.getBuild().getOutputDirectory());
		if( ! completeAppDirectory.isDirectory())
			throw new MojoExecutionException( "The target model directory could not be found. " + completeAppDirectory );

		// Validate the application
		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( completeAppDirectory );

		// Analyze the result
		try {
			if( alr.getLoadErrors().size() > 0 ) {
				reportErrors( alr );
				if( RoboconfErrorHelpers.containsCriticalErrors( alr.getLoadErrors()))
					throw new MojoFailureException( "Errors were found in the application." );
			}

		} catch( IOException e ) {
			throw new MojoExecutionException( "A problem occurred during the validation.", e );
		}
	}


	private void reportErrors( ApplicationLoadResult alr ) throws IOException {

		// Add a log entry
		getLog().info( "Generating a report for validation errors under " + MavenPluginConstants.VALIDATION_RESULT_PATH );

		// Generate the report (file and console too)
		StringBuilder globalSb = new StringBuilder();
		for( RoboconfError error : alr.getLoadErrors()) {
			StringBuilder sb = new StringBuilder();
			sb.append( "[ " );
			sb.append( error.getErrorCode().getCategory().toString().toLowerCase());
			sb.append( " ] " );
			sb.append( error.getErrorCode().getMsg());
			if( ! Utils.isEmptyOrWhitespaces( error.getDetails()))
				sb.append( " " + error.getDetails());

			if( error.getErrorCode().getLevel() == ErrorLevel.WARNING )
				getLog().warn( sb.toString());
			else
				getLog().error( sb.toString());

			globalSb.append( sb );
			globalSb.append( "\n" );
		}

		// TODO: add line and source file name

		// Write the report.
		// Reporting only makes sense when there is an error or a warning.
		File targetFile = new File( this.project.getBasedir(), MavenPluginConstants.VALIDATION_RESULT_PATH );
		Utils.createDirectory( targetFile.getParentFile());
		Utils.writeStringInto( globalSb.toString(), targetFile );
	}
}
