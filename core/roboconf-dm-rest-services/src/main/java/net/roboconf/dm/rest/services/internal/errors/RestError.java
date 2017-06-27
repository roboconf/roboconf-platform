/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.errors;

import static net.roboconf.core.errors.ErrorDetails.exceptionName;
import static net.roboconf.core.errors.ErrorDetails.logReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestError extends RoboconfError {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String uuid = UUID.randomUUID().toString();
	private final Exception exception;


	/**
	 * Constructor.
	 * @param errorCode
	 * @param details
	 */
	public RestError( ErrorCode errorCode, ErrorDetails... details ) {
		this( errorCode, null, details );
	}


	/**
	 * Constructor.
	 * @param errorCode
	 * @param e
	 * @param details
	 */
	public RestError( ErrorCode errorCode, Exception e, ErrorDetails... details ) {
		super( errorCode );
		this.exception = e;

		setDetails( upgradeDetails( e, this.uuid, details ));
		if( e != null )
			Utils.logException( this.logger, Level.FINEST, e, "Exception ID: " + this.uuid );
	}


	/**
	 * @return the exception
	 */
	public Exception getException() {
		return this.exception;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.core.RoboconfError
	 * #equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj ) {
		return super.equals( obj )
				&& Objects.equals( this.uuid, ((RestError) obj).uuid );
	}


	/* (non-Javadoc)
	 * @see net.roboconf.core.RoboconfError
	 * #hashCode()
	 */
	@Override
	public int hashCode() {
		// Keep for Findbugs.
		return super.hashCode();
	}


	/**
	 * @param e
	 * @param details
	 * @return a non-null array
	 */
	private static ErrorDetails[] upgradeDetails( Exception e, String uuid, ErrorDetails[] details ) {

		List<ErrorDetails> list = new ArrayList<> ();
		if( details != null )
			list.addAll( Arrays.asList( details ));

		if( e != null ) {
			// Write the exception's name, not the stack trace
			list.add( exceptionName( e ));
			list.add( logReference( uuid ));
		}

		return list.toArray( new ErrorDetails[ list.size()]);
	}
}
