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

package net.roboconf.core.model.beans;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ExportedVariable implements Serializable {

	private static final long serialVersionUID = -7928449439804391083L;
	private String name, value, rawKind;
	private boolean random;


	/**
	 * Constructor.
	 */
	public ExportedVariable() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name
	 * @param value
	 */
	public ExportedVariable( String name, String value ) {
		this.name = name;
		this.value = value;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue( String value ) {
		this.value = value;
	}

	/**
	 * @return the random
	 */
	public boolean isRandom() {
		return this.random;
	}

	/**
	 * @param random the random to set
	 */
	public void setRandom( boolean random ) {
		this.random = random;
	}

	/**
	 * @return the rawKind
	 */
	public String getRawKind() {
		return this.rawKind;
	}

	/**
	 * @param rawKind the rawKind to set
	 */
	public void setRawKind( String rawKind ) {
		this.rawKind = rawKind;
	}

	/**
	 * @return the randomKind
	 */
	public RandomKind getRandomKind() {
		return RandomKind.whichValue( this.rawKind );
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof ExportedVariable
				&& Objects.equals( this.name, ((ExportedVariable ) obj).name );
	}

	@Override
	public int hashCode() {
		return this.name == null ? 41 : this.name.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum RandomKind {
		PORT;


		/**
		 * A flexible alternative to determine a RandomKind from a string.
		 * @param s the string to read (can be null)
		 * @return null if it matched an unknown kind, or a RandomKind otherwise
		 */
		public static RandomKind whichValue( String s ) {

			RandomKind result = null;
			for( RandomKind rk : RandomKind.values()) {
				if( rk.toString().equalsIgnoreCase( s )) {
					result = rk;
					break;
				}
			}

			return result;
		}
	}
}
