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

package net.roboconf.dm.management.exceptions;

/**
 * @author Noël - LIG
 */
public class AlreadyExistingException extends Exception {
	private static final long serialVersionUID = -1632759138157527191L;


	/**
	 * Constructor.
	 * @param elementName an element name
	 */
	public AlreadyExistingException( String elementName ) {
		super( elementName + " already exists." );
	}
}
