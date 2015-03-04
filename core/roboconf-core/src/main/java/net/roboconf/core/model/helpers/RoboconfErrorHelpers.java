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

package net.roboconf.core.model.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.roboconf.core.ErrorCode.ErrorLevel;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.SourceReference;

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

		Collection<RoboconfError> result = new ArrayList<RoboconfError> ();
		for( RoboconfError error : errors ) {
			if( error.getErrorCode().getLevel() == ErrorLevel.WARNING )
				result.add( error );
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

		List<RoboconfError> result = new ArrayList<RoboconfError> ();
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
}
