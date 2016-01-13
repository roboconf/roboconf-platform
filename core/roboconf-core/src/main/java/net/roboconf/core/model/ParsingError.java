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

package net.roboconf.core.model;

import java.io.File;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;

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
		super( errorCode );
		this.line = line;
		this.file = file;
	}

	/**
	 * Constructor.
	 * @param errorCode an error code
	 * @param file the file that contains the error
	 * @param line a line number
	 * @param details the error details
	 */
	public ParsingError( ErrorCode errorCode, File file, int line, String details ) {
		super( errorCode, details );
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
}
