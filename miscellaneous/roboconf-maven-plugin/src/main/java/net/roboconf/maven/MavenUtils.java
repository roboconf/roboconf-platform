/**
 * Copyright 2016 Linagora, Université Joseph Fourier, Floralis
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

import org.apache.maven.plugin.logging.Log;

import net.roboconf.core.ErrorCode.ErrorLevel;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class MavenUtils {

	/**
	 * Private empty constructor.
	 */
	private MavenUtils() {
		// nothing
	}


	/**
	 * Formats a Roboconf errors and outputs it in the logs.
	 * @param error an error
	 * @param log the Maven logger
	 * @return a string builder with the output
	 */
	public static StringBuilder formatError( RoboconfError error, Log log ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "[ " );
		sb.append( error.getErrorCode().getCategory().toString().toLowerCase());
		sb.append( " ] " );
		sb.append( error.getErrorCode().getMsg());
		if( ! Utils.isEmptyOrWhitespaces( error.getDetails()))
			sb.append( " " + error.getDetails());

		if( ! sb.toString().endsWith( "." ))
			sb.append( "." );

		if( error instanceof ParsingError ) {
			sb.append( " See " );
			sb.append(((ParsingError) error).getFile().getName());
			sb.append( ", line " );
			sb.append(((ParsingError) error).getLine());
			sb.append( "." );
		}

		if( error.getErrorCode().getLevel() == ErrorLevel.WARNING )
			log.warn( sb.toString());
		else
			log.error( sb.toString());

		return sb;
	}
}
