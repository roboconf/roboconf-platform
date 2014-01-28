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

package net.roboconf.core.model;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;

/**
 * A model error instantiates and localizes an {@link ErrorCode}.
 * @author Vincent Zurczak - Linagora
 */
public class ModelError extends RoboconfError {
	private int line;


	/**
	 * Constructor.
	 * @param errorCode an error code
	 * @param line a line number
	 */
	public ModelError( ErrorCode errorCode, int line ) {
		super( errorCode );
		this.line = line;
	}

	/**
	 * @return the line
	 */
	public int getLine() {
		return this.line;
	}

	/**
	 * @param line the line to set
	 */
	public void setLine( int line ) {
		this.line = line;
	}
}
