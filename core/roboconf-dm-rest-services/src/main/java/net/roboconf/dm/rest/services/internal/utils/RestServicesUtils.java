/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.utils;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.rest.services.internal.errors.RestError;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class RestServicesUtils {

	/**
	 * Private constructor.
	 */
	private RestServicesUtils() {
		// nothing
	}


	/**
	 * Handles an error and makes the magic stuff so that users can understand it.
	 * @param status the response's status
	 * @param restError a REST error
	 * @param lang the user language
	 * @return a response builder
	 */
	public static ResponseBuilder handleError( int status, RestError restError, String lang ) {

		List<RoboconfError> errors = new ArrayList<> ();
		errors.add( restError );
		if( restError.getException() instanceof InvalidApplicationException ) {
			errors.addAll(((InvalidApplicationException) restError.getException()).getErrors());
		}

		StringBuilder sb = new StringBuilder();
		String sep = "\n\n";
		for( String s : RoboconfErrorHelpers.formatErrors( errors, null, false ).values()) {
			sb.append( s );
			sb.append( sep );
			sep = "\n";
		}

		// Errors should return a JSon object.
		// Otherwise, Restangular cannot parse error messages.
		// See https://stackoverflow.com/questions/42068/how-do-i-handle-newlines-in-json
		String msg = sb.toString().trim()
						.replaceAll( "\"", "\\\"" )
						.replaceAll( "\n", "\\\\n" )
						.replaceAll( "\r", "\\\\r" )
						.replaceAll( "\t", "\\\\t" );

		return Response.status( status ).entity( "{\"reason\":\"" + msg + "\"}" );
	}


	/**
	 * Handles an error and makes the magic stuff so that users can understand it.
	 * @param status the response's status
	 * @param restError a REST error
	 * @param lang the user language
	 * @return a response builder
	 */
	public static ResponseBuilder handleError( Status status, RestError restError, String lang ) {
		return handleError( status.getStatusCode(), restError, lang );
	}


	/**
	 * @param manager a non-null manager
	 * @return the user language, as specified in the preferences
	 */
	public static String lang( Manager manager ) {
		return manager.preferencesMngr().get( IPreferencesMngr.USER_LANGUAGE );
	}
}
