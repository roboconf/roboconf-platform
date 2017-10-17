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
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.validation.ProjectValidator;
import net.roboconf.tooling.core.validation.ProjectValidator.ProjectValidationResult;

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

	@Parameter( defaultValue = "false" )
	private boolean recipe;

	@Parameter( defaultValue = "false" )
	private boolean official;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Find the target directory
		File completeAppDirectory = new File( this.project.getBuild().getOutputDirectory());
		if( ! completeAppDirectory.isDirectory())
			throw new MojoExecutionException( "The target model directory could not be found. " + completeAppDirectory );

		// Load and validate the application
		Collection<RoboconfError> reusableRecipeErrors = Collections.emptyList();
		ProjectValidationResult pvr = ProjectValidator.validateProject( completeAppDirectory, this.recipe );
		if( this.recipe ) {
			reusableRecipeErrors = validateRecipesSpecifics(
					this.project,
					pvr.getRawParsingResult().getApplicationTemplate(),
					this.official );

			pvr.getErrors().addAll( reusableRecipeErrors );
		}

		// Analyze the result
		try {
			if( pvr.getErrors().size() > 0 ) {
				reportErrors( pvr.getErrors());
				if( RoboconfErrorHelpers.containsCriticalErrors( pvr.getErrors()))
					throw new MojoFailureException( "Errors were found in the application." );

				if( this.official && ! reusableRecipeErrors.isEmpty())
					throw new MojoFailureException( "Warnings were found in official recipes. Please, fix them." );
			}

		} catch( IOException e ) {
			throw new MojoExecutionException( "A problem occurred during the validation.", e );
		}
	}


	/**
	 * Reports errors (in the logger and in a file).
	 * @param errors
	 * @throws IOException
	 */
	private void reportErrors( Collection<RoboconfError> errors ) throws IOException {

		// Add a log entry
		getLog().info( "Generating a report for validation errors under " + MavenPluginConstants.VALIDATION_RESULT_PATH );

		// Generate the report (file and console too)
		StringBuilder sb = MavenPluginUtils.formatErrors( errors, getLog());

		// Write the report.
		// Reporting only makes sense when there is an error or a warning.
		File targetFile = new File( this.project.getBasedir(), MavenPluginConstants.VALIDATION_RESULT_PATH );
		Utils.createDirectory( targetFile.getParentFile());
		Utils.writeStringInto( sb.toString(), targetFile );
	}


	/**
	 * Validate aspects that are specific to recipes (i.e. partial Roboconf applications).
	 * <p>
	 * Most of this validation could have been handled through enforcer rules. However,
	 * they are all warnings and we do not want to create hundreds of projects. We can
	 * see these rules as good practices that will be shared amongst all the Roboonf users.
	 * </p>
	 * <p>
	 * At worst, users can ignore these warnings.
	 * Or they can submit a feature request to add or remove validation rules.
	 * </p>
	 *
	 * @param project a Maven project
	 * @param tpl an application template
	 * @param official true if this recipe is maintained by the Roboconf team, false otherwise
	 * @return a non-null list of errors
	 */
	static Collection<RoboconfError> validateRecipesSpecifics( MavenProject project, ApplicationTemplate tpl, boolean official ) {

		Collection<RoboconfError> result = new ArrayList<> ();
		if( ! project.getArtifactId().equals( project.getArtifactId().toLowerCase()))
			result.add( new RoboconfError( ErrorCode.REC_ARTIFACT_ID_IN_LOWER_CASE ));

		if( ! tpl.getRootInstances().isEmpty())
			result.add( new RoboconfError( ErrorCode.REC_AVOID_INSTANCES ));

		if( official && ! project.getGroupId().startsWith( Constants.OFFICIAL_RECIPES_GROUP_ID ))
			result.add( new RoboconfError( ErrorCode.REC_OFFICIAL_GROUP_ID ));

		if( ! project.getArtifactId().equals( project.getArtifactId()))
			result.add( new RoboconfError( ErrorCode.REC_NON_MATCHING_ARTIFACT_ID ));

		File[] files = project.getBasedir().listFiles();
		boolean found = false;
		if( files != null ) {
			for( int i=0; i<files.length && ! found; i++ )
				found = files[ i ].getName().matches( "(?i)readme(\\..*)?" );
		}

		if( ! found )
			result.add( new RoboconfError( ErrorCode.REC_MISSING_README ));

		return result;
	}
}
