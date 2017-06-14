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

package net.roboconf.core.model.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.ErrorCode.ErrorLevel;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.SourceReference;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class RoboconfErrorHelpers {

	/**
	 * Private empty constructor.
	 */
	private RoboconfErrorHelpers() {
		// nothing
	}


	/**
	 * Determines whether a collection of errors contains at least one critical error.
	 * @param errors a non-null collection of errors
	 * @return true if at least one critical error, false otherwise
	 */
	public static boolean containsCriticalErrors( Collection<? extends RoboconfError> errors ) {

		boolean result = false;
		for( RoboconfError error : errors ) {
			if(( result = error.getErrorCode().getLevel() == ErrorLevel.SEVERE ))
				break;
		}

		return result;
	}


	/**
	 * Finds all the warnings among a collection of Roboconf errors.
	 * @param errors a non-null collection of errors
	 * @return a non-null collection of warnings
	 */
	public static Collection<RoboconfError> findWarnings( Collection<? extends RoboconfError> errors ) {

		Collection<RoboconfError> result = new ArrayList<> ();
		for( RoboconfError error : errors ) {
			if( error.getErrorCode().getLevel() == ErrorLevel.WARNING )
				result.add( error );
		}

		return result;
	}


	/**
	 * Extracts and formats warnings so that they can be displayed by a logger.
	 * @param errors a non-null list of errors
	 * @return a list of string, each one being readable information about a warning
	 */
	public static List<String> extractAndFormatWarnings( Collection<? extends RoboconfError> errors ) {

		List<String> result = new ArrayList<> ();
		for( RoboconfError warning : RoboconfErrorHelpers.findWarnings( errors )) {
			StringBuilder sb = new StringBuilder();
			sb.append( warning.getErrorCode().getMsg());
			if( ! Utils.isEmptyOrWhitespaces( warning.getDetails()))
				sb.append( " " + warning.getDetails());

			result.add( sb.toString());
		}

		return result;
	}


	/**
	 * Resolves errors with their location when it is possible.
	 * <p>
	 * Parsing and conversion errors already have location information (file and line
	 * number). This is not the case of runtime errors (validation of the runtime model).
	 * However, these errors keep a reference to the object that contains the error.
	 * </p>
	 * <p>
	 * When we load an application from a file, we keep an association between a runtime model
	 * object and its location (file and line number). Therefore, we can resolve the location
	 * of runtime errors too (provided the model was loaded from a file).
	 * </p>
	 * <p>
	 * So, this method replaces (runtime) model errors by errors that contain location data.
	 * </p>
	 *
	 * @param alr the result of an application load operation
	 * @return a non-null list of errors
	 */
	public static List<RoboconfError> resolveErrorsWithLocation( ApplicationLoadResult alr ) {

		List<RoboconfError> result = new ArrayList<> ();
		for( RoboconfError error : alr.getLoadErrors()) {

			RoboconfError errorToAdd = error;
			if( error instanceof ModelError ) {
				Object modelObject = ((ModelError) error).getModelObject();
				SourceReference sr = alr.getObjectToSource().get( modelObject );
				if( sr != null )
					errorToAdd = new ParsingError( error.getErrorCode(), sr.getSourceFile(), sr.getLine(), error.getDetails());
			}

			result.add( errorToAdd );
		}

		return result;
	}


	/**
	 * Filters errors for recipes.
	 * <p>
	 * Indeed, some errors only make sense for complete applications, not for
	 * reusable recipes. This method removes them from the input list of errors.
	 * </p>
	 *
	 * @param alr an {@link ApplicationLoadResult}
	 */
	public static void filterErrorsForRecipes( ApplicationLoadResult alr ) {
		filterErrorsForRecipes( alr.getLoadErrors());
	}


	/**
	 * Filters errors for recipes.
	 * <p>
	 * Indeed, some errors only make sense for complete applications, not for
	 * reusable recipes. This method removes them from the input list of errors.
	 * </p>
	 *
	 * @param errors a non-null list of errors
	 */
	public static void filterErrorsForRecipes( Collection<? extends RoboconfError> errors ) {

		filterErrors(
				errors,
				ErrorCode.RM_ROOT_INSTALLER_MUST_BE_TARGET,
				ErrorCode.RM_UNRESOLVABLE_FACET_VARIABLE,
				ErrorCode.RM_UNREACHABLE_COMPONENT,
				ErrorCode.RM_ORPHAN_FACET,
				ErrorCode.RM_ORPHAN_FACET_WITH_CHILDREN );
	}


	/**
	 * Filters errors by removing those associated with specific error codes.
	 * @param errors a non-null list of errors
	 * @param errorCodes error codes
	 */
	public static void filterErrors( Collection<? extends RoboconfError> errors, ErrorCode... errorCodes ) {

		List<ErrorCode> codesToSkip = new ArrayList<> ();
		if( errorCodes != null )
			codesToSkip.addAll( Arrays.asList( errorCodes ));

		Collection<RoboconfError> toRemove = new ArrayList<> ();
		for( RoboconfError error : errors ) {
			if( codesToSkip.contains( error.getErrorCode()))
				toRemove.add( error );
		}

		errors.removeAll( toRemove );
	}


	/**
	 * Formats a Roboconf error as a string.
	 * @param error an error
	 * @return a non-null string
	 */
	public static String formatError( RoboconfError error ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "[ " );
		sb.append( error.getErrorCode().getCategory().toString().toLowerCase());
		sb.append( " ] " );
		sb.append( error.getErrorCode().getMsg());
		if( ! Utils.isEmptyOrWhitespaces( error.getDetails()))
			sb.append( " " + error.getDetails());

		if( sb.charAt( sb.length() -1 ) != '.' )
			sb.append( "." );

		if( error instanceof ParsingError ) {
			sb.append( " See " );
			sb.append(((ParsingError) error).getFile().getName());
			sb.append( ", line " );
			sb.append(((ParsingError) error).getLine());
			sb.append( "." );
		}

		return sb.toString();
	}
}
