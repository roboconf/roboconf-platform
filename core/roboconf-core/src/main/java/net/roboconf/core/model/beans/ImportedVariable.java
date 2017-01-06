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
public class ImportedVariable implements Serializable {

	private static final long serialVersionUID = 4270817159034940619L;
	private String name;
	private boolean optional = false, external = false;


	/**
	 * Constructor.
	 */
	public ImportedVariable() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name
	 * @param optional
	 * @param external
	 */
	public ImportedVariable( String name, boolean optional, boolean external ) {
		this.name = name;
		this.optional = optional;
		this.external = external;
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
	 * @return the optional
	 */
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * @param optional the optional to set
	 */
	public void setOptional( boolean optional ) {
		this.optional = optional;
	}

	/**
	 * @return the external
	 */
	public boolean isExternal() {
		return this.external;
	}

	/**
	 * @param external the external to set
	 */
	public void setExternal( boolean external ) {
		this.external = external;
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof ImportedVariable
				&& Objects.equals( this.name, ((ImportedVariable ) obj).name );
	}

	@Override
	public int hashCode() {
		return this.name == null ? 41 : this.name.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}
}
