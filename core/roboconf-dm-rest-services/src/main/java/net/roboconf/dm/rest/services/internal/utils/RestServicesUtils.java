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

import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.utils.Utils;

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
	 * Handles an exception.
	 * @param logger the logger
	 * @param status the response's status
	 * @param msg the message (can be null)
	 * @param e the exception (can be null)
	 * @return a response builder
	 */
	public static ResponseBuilder handleException( Logger logger, int status, String msg, Exception e ) {

		StringBuilder sb = new StringBuilder();
		if( msg != null )
			sb.append( formatEnd( msg ));

		if( e != null && ! Utils.isEmptyOrWhitespaces( e.getMessage()))
			sb.append( e.getMessage());

		logger.severe( sb.toString());
		if( e != null )
			Utils.logException( logger, e );

		// Errors should return a JSon object.
		// Otherwise, Restangular cannot parse error messages.
		String details = msg == null ? "Not specified." : msg.replaceAll( "\"", "\\\"" );
		return Response.status( status ).entity( "{\"reason\":\"" + details + "\"}" );
	}


	/**
	 * Handles an exception.
	 * @param logger the logger
	 * @param status the response's status
	 * @param msg the message (can be null)
	 * @param e the exception (can be null)
	 * @return a response builder
	 */
	public static ResponseBuilder handleException( Logger logger, Status status, String msg, Exception e ) {
		return handleException( logger, status.getStatusCode(), msg, e );
	}


	/**
	 * Handles an exception.
	 * @param logger the logger
	 * @param status the response's status
	 * @param msg the message (can be null)
	 * @return a response builder
	 */
	public static ResponseBuilder handleException( Logger logger, Status status, String msg ) {
		return handleException( logger, status.getStatusCode(), msg, null );
	}


	/**
	 * Formats the end of a message for logging.
	 * @param s a string (can be null)
	 * @return a formatted string, or null if the input was null
	 */
	static String formatEnd( String s ) {
		return Utils.isEmptyOrWhitespaces( s ) ? null : s.replaceFirst( "\\s*$", " " );
	}
}
