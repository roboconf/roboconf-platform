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

package net.roboconf.dm.management.exceptions;

import java.util.Arrays;
import java.util.Collection;

import net.roboconf.core.RoboconfError;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InvalidApplicationException extends Exception {
	private static final long serialVersionUID = -648674838264717185L;


	/**
	 * Constructor.
	 */
	public InvalidApplicationException( Collection<RoboconfError> errors ) {
		super( convertErrorsListToString( errors ));
	}

	/**
	 * Constructor.
	 */
	public InvalidApplicationException( RoboconfError error ) {
		super( convertErrorsListToString( Arrays.asList( error )));
	}

	/**
	 * Constructor.
	 * @param e
	 */
	public InvalidApplicationException( Exception e ) {
		super( e );
	}

	/**
	 * @param errors a non-null list of errors
	 * @return a detailed string
	 */
	private static String convertErrorsListToString( Collection<RoboconfError> errors ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "The application contains errors." );

		for( RoboconfError error : errors ) {
			sb.append( "\n[ " );
			sb.append( error.getErrorCode().getLevel());
			sb.append( " ] " );
			sb.append( error.getErrorCode().getMsg());
			if( error.getDetails() != null ) {
				sb.append( "\nDetails: " );
				sb.append( error.getDetails());
			}
		}

		return sb.toString();
	}
}
