/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

/**
 * An application template groups an identifier, graph definitions and instances.
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTemplate extends AbstractApplication implements Serializable {

	private static final long serialVersionUID = -4753958407033243184L;

	private String qualifier, dslId;
	private Graphs graphs;


	/**
	 * Constructor.
	 */
	public ApplicationTemplate() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name
	 */
	public ApplicationTemplate( String name ) {
		this.name = name;
	}

	/**
	 * @return the qualifier
	 */
	public String getQualifier() {
		return this.qualifier;
	}

	/**
	 * @param qualifier the qualifier to set
	 */
	public void setQualifier( String qualifier ) {
		this.qualifier = qualifier;
	}

	/**
	 * @return the dslId
	 */
	public String getDslId() {
		return this.dslId;
	}

	/**
	 * @param dslId the dslId to set
	 */
	public void setDslId( String dslId ) {
		this.dslId = dslId;
	}

	/**
	 * @return the graphs
	 */
	public Graphs getGraphs() {
		return this.graphs;
	}

	/**
	 * @param graphs the graphs to set
	 */
	public void setGraphs( Graphs graphs ) {
		this.graphs = graphs;
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof ApplicationTemplate
				&& Objects.equals( this.name, ((ApplicationTemplate) obj ).getName())
				&& Objects.equals( this.qualifier, ((ApplicationTemplate) obj ).getQualifier());
	}

	@Override
	public int hashCode() {
		int i1 = this.name == null ? 29 : this.name.hashCode();
		int i2 = this.qualifier == null ? 11 : this.qualifier.hashCode();
		return i1 * i2;
	}

	/**
	 * Sets the name in a chain approach.
	 */
	public ApplicationTemplate name( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the qualifier in a chain approach.
	 */
	public ApplicationTemplate qualifier( String qualifier ) {
		this.qualifier = qualifier;
		return this;
	}

	/**
	 * Sets the description in a chain approach.
	 */
	public ApplicationTemplate description( String description ) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the DSL ID in a chain approach.
	 */
	public ApplicationTemplate dslId( String dslId ) {
		this.dslId = dslId;
		return this;
	}

	/**
	 * Sets the graphs in a chain approach.
	 */
	public ApplicationTemplate graphs( Graphs graphs ) {
		this.graphs = graphs;
		return this;
	}

	/**
	 * Sets the directory in a chain approach.
	 */
	public ApplicationTemplate directory( File directory ) {
		this.directory = directory;
		return this;
	}
}
