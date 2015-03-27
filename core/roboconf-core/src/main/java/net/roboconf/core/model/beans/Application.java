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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;

import net.roboconf.core.utils.Utils;

/**
 * An application groups an identifier, graph definitions and instances.
 * @author Vincent Zurczak - Linagora
 */
public class Application implements Serializable {

	private static final long serialVersionUID = -4753958407033243184L;

	private String name, qualifier, description, dslId;
	private Graphs graphs;
	private final Collection<Instance> rootInstances = new LinkedHashSet<Instance> ();


	/**
	 * Constructor.
	 */
	public Application() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name
	 */
	public Application( String name ) {
		this.name = name;
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
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription( String description ) {
		this.description = description;
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

	/**
	 * @return the root instances
	 */
	public Collection<Instance> getRootInstances() {
		return this.rootInstances;
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Application
				&& Utils.areEqual( this.name, ((Application) obj ).getName())
				&& Utils.areEqual( this.qualifier, ((Application) obj ).getQualifier());
	}

	@Override
	public int hashCode() {
		int i1 = this.name == null ? 29 : this.name.hashCode();
		int i2 = this.qualifier == null ? 11 : this.qualifier.hashCode();
		return i1 * i2;
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * Sets the name in a chain approach.
	 */
	public Application name( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the qualifier in a chain approach.
	 */
	public Application qualifier( String qualifier ) {
		this.qualifier = qualifier;
		return this;
	}

	/**
	 * Sets the description in a chain approach.
	 */
	public Application description( String description ) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the DSL ID in a chain approach.
	 */
	public Application dslId( String dslId ) {
		this.dslId = dslId;
		return this;
	}

	/**
	 * Sets the graphs in a chain approach.
	 */
	public Application graphs( Graphs graphs ) {
		this.graphs = graphs;
		return this;
	}
}
