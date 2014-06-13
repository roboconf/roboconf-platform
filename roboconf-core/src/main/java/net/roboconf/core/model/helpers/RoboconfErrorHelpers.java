/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

import net.roboconf.core.ErrorCode.ErrorLevel;
import net.roboconf.core.RoboconfError;

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
	public static boolean containsCriticalErrors( Collection<RoboconfError> errors ) {

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
	public static Collection<RoboconfError> findWarnings( Collection<RoboconfError> errors ) {

		Collection<RoboconfError> result = new ArrayList<RoboconfError> ();
		for( RoboconfError error : errors ) {
			if( error.getErrorCode().getLevel() == ErrorLevel.WARNING )
				result.add( error );
		}

		return result;
	}
}
