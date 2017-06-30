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

package net.roboconf.core.model;

import static net.roboconf.core.errors.ErrorDetails.file;
import static net.roboconf.core.errors.ErrorDetails.line;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.errors.RoboconfError;

/**
 * A parsing error instantiates and localizes an {@link ErrorCode}.
 * @author Vincent Zurczak - Linagora
 */
public class ParsingError extends RoboconfError {

	private final int line;
	private final File file;


	/**
	 * Constructor.
	 * @param errorCode an error code
	 * @param file the file that contains the error
	 * @param line a line number
	 */
	public ParsingError( ErrorCode errorCode, File file, int line ) {
		this( errorCode, file, line, new ErrorDetails[ 0 ]);
	}

	/**
	 * Constructor.
	 * @param errorCode an error code
	 * @param file the file that contains the error
	 * @param line a line number
	 * @param details the error details
	 */
	public ParsingError( ErrorCode errorCode, File file, int line, ErrorDetails... details ) {
		super( errorCode, addLineAndFile( line, file, details ));
		this.line = line;
		this.file = file;
	}

	/**
	 * @return the line
	 */
	public int getLine() {
		return this.line;
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		return this.file;
	}

	/* (non-Javadoc)
	 * @see net.roboconf.core.RoboconfError
	 * #equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj ) {
		return super.equals( obj )
				&& Objects.equals( this.line, ((ParsingError) obj).line )
				&& Objects.equals( this.file, ((ParsingError) obj).file );
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
	 * Adds details about the line number and the file.
	 * @param line
	 * @param file
	 * @param details
	 * @return a non-null array
	 */
	private static ErrorDetails[] addLineAndFile( int line, File file, ErrorDetails... details ) {

		Set<ErrorDetails> updatedDetails = new LinkedHashSet<> ();
		if( details != null )
			updatedDetails.addAll( Arrays.asList( details ));

		if( file != null ) {
			updatedDetails.add( file( file ));
			updatedDetails.add( line( line ));
		}

		return updatedDetails.toArray( new ErrorDetails[ updatedDetails.size()]);
	}
}
