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

package net.roboconf.core.model.beans;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractType implements Serializable {

	private static final long serialVersionUID = 6426303830149882558L;

	/**
	 * Variables, accessible by their names and sorted alphabetically in this map.
	 */
	public final Map<String,ExportedVariable> exportedVariables = new TreeMap<> ();

	protected String name;
	protected final Collection<AbstractType> children = new HashSet<>( 0 );
	protected final Collection<AbstractType> ancestors = new HashSet<>( 0 );


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

	@Override
	public boolean equals( Object obj ) {
		return obj != null
				&& obj.getClass().equals( getClass())
				&& Objects.equals( this.name, ((AbstractType) obj).getName());
	}

	@Override
	public int hashCode() {
		return this.name == null ? 31 : this.name.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}


	/**
	 * Creates a bi-directional relation between this element and another type.
	 * @param type an abstract type (facet or component)
	 */
	public void addChild( AbstractType type ) {
		this.children.add( type );
		type.ancestors.add( this );
	}

	/**
	 * @return the children (never null and not modifiable)
	 */
	public Collection<AbstractType> getChildren() {
		return Collections.unmodifiableCollection( this.children );
	}

	/**
	 * @return the ancestors (never null and not modifiable)
	 */
	public Collection<AbstractType> getAncestors() {
		return Collections.unmodifiableCollection( this.ancestors );
	}

	/**
	 * A shortcut method to add a new exported variable.
	 * @param var a non-null variable to add
	 */
	public void addExportedVariable( ExportedVariable var ) {
		this.exportedVariables.put( var.getName(), var );
	}
}
