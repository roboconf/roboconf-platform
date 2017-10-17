/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.tooling.core.validation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.errors.RoboconfErrorComparator;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.RuntimeModelIo.InstancesLoadResult;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ProjectValidator {

	/**
	 * Private empty constructor.
	 */
	private ProjectValidator() {
		// nothing
	}


	/**
	 * Validates a project.
	 * <p>
	 * Whereas {@link #validateProject(File, boolean)}, this method tries to
	 * guess whether the project is a recipe one or not. It assumes that
	 * if the project has neither a descriptor file, nor instances,
	 * then it can be considered as a recipe project
	 * </p>
	 *
	 * @param appDirectory the application's directory (must exist)
	 * @return a non-null list of errors, with the resolved location (sorted by error code)
	 */
	public static ProjectValidationResult validateProject( File appDirectory ) {

		// Determine whether the project is a recipe one.

		// Since a Roboconf project is not mandatory a Maven project,
		// we cannot rely on the POM. But we make the hypothesis that
		// if the project has neither a descriptor file, nor instances,
		// then it can be considered as a recipe project.

		// If there is a POM that indicates it is (or not) a recipe
		// project, then errors will appear during the Maven build.
		File instancesDir = new File( appDirectory, Constants.PROJECT_DIR_INSTANCES );
		boolean isRecipe =
				! new File( appDirectory, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR ).exists()
				&& (! instancesDir.isDirectory() || Utils.listAllFiles( instancesDir ).isEmpty());

		// Validate the project then
		return validateProject( appDirectory, isRecipe );
	}


	/**
	 * Validates a project.
	 * <p>
	 * It first tries to load it as a Roboconf project.
	 * Then, it validates all the files within the project, even those
	 * that are not reachable from the project descriptor.
	 * </p>
	 *
	 * @param appDirectory the application's directory (must exist)
	 * @param isRecipe true if the project is a reusable recipe
	 * @return a non-null list of errors, with the resolved location (sorted by error code)
	 */
	public static ProjectValidationResult validateProject( File appDirectory, boolean isRecipe ) {

		// Load and validate the application
		ApplicationLoadResult alr;
		if( isRecipe ) {
			alr = RuntimeModelIo.loadApplicationFlexibly( appDirectory );
		} else {
			alr = RuntimeModelIo.loadApplication( appDirectory );
		}

		// Then, search for all the graph files and validate them
		File dir = new File( appDirectory, Constants.PROJECT_DIR_GRAPH );
		if( dir.exists()) {
			for( File f : Utils.listAllFiles( dir, Constants.FILE_EXT_GRAPH )) {
				RuntimeModelIo.loadGraph( f, dir, alr );
			}
		}

		// Eventually, search for all the instance files and validate them
		dir = new File( appDirectory, Constants.PROJECT_DIR_INSTANCES );
		if( dir.exists()
				&& alr.getApplicationTemplate().getGraphs() != null ) {

			for( File f : Utils.listAllFiles( dir, Constants.FILE_EXT_INSTANCES )) {
				InstancesLoadResult ilr = RuntimeModelIo.loadInstances( f, dir, alr.getApplicationTemplate().getGraphs(), null );
				alr.getObjectToSource().putAll( ilr.getObjectToSource());
				alr.getLoadErrors().addAll( ilr.getLoadErrors());
			}
		}

		// Recipe? Filter some errors.
		if( isRecipe ) {
			RoboconfErrorHelpers.filterErrorsForRecipes( alr );
		}

		// Now, remove duplicate errors and resolve locations
		Collection<RoboconfError> errors = new HashSet<> ();
		errors.addAll( RoboconfErrorHelpers.resolveErrorsWithLocation( alr ));

		// Do not show errors in tooling when the project version looks like "${project.version}"
		// Only valid if the project is a Maven one.
		String path = appDirectory.getAbsolutePath().replaceAll( "\\\\", "/" );
		if( ! path.endsWith( "/" ) && Constants.MAVEN_SRC_MAIN_MODEL.endsWith( "/" ))
			path += "/";

		if( path.endsWith( Constants.MAVEN_SRC_MAIN_MODEL )
				&& new File( appDirectory, "../../../pom.xml" ).exists()) {

			List<RoboconfError> errorsToRemove = new ArrayList<> ();
			for( RoboconfError error : errors ) {

				// We ignore this error if it looks like a Maven property.
				if( error.getErrorCode() == ErrorCode.RM_INVALID_APPLICATION_VERSION
						&& error.getDetails().length > 0
						&& error.getDetails()[ 0 ].getElementName().matches( "^\\$\\{.*\\}\\S*" )) {

					errorsToRemove.add( error );
				}
			}

			errors.removeAll( errorsToRemove );
		}

		// Eventually, sort everything
		List<RoboconfError> errorsList = new ArrayList<>( errors );
		Collections.sort( errorsList, new RoboconfErrorComparator());

		return new ProjectValidationResult( errorsList, alr, isRecipe );
	}



	/**
	 * A wrapper class with all the errors and the validation context.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ProjectValidationResult {

		private final List<RoboconfError> errors;
		private final ApplicationLoadResult rawParsingResult;
		private final boolean isRecipe;


		/**
		 * Constructor.
		 * @param errors
		 * @param rawParsingResult
		 */
		public ProjectValidationResult( List<RoboconfError> errors, ApplicationLoadResult rawParsingResult, boolean isRecipe ) {
			this.errors = errors;
			this.isRecipe = isRecipe;
			this.rawParsingResult = rawParsingResult;
		}

		/**
		 * @return the errors
		 */
		public List<RoboconfError> getErrors() {
			return this.errors;
		}

		/**
		 * @return the rawParsingResult
		 */
		public ApplicationLoadResult getRawParsingResult() {
			return this.rawParsingResult;
		}

		/**
		 * @return the isRecipe
		 */
		public boolean isRecipe() {
			return this.isRecipe;
		}
	}
}
