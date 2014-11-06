/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.runtime;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.roboconf.core.utils.Utils;

/**
 * A component represents a Software item (hardware, software, whatever).
 * @author Vincent Zurczak - Linagora
 */
public class Component implements Serializable {

	private static final long serialVersionUID = 5163458185512982868L;

	private String name, alias, installerName, iconLocation;
	private final Collection<String> facetNames = new HashSet<String> ();
	private final Map<String,String> exportedVariables = new HashMap<String,String> ();
	private final Map<String,Boolean> importedVariables = new HashMap<String,Boolean> ();

	private final Collection<Component> children = new HashSet<Component> ();
	private final Collection<Component> ancestors = new HashSet<Component> ();


	/**
	 * Constructor.
	 */
	public Component() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name the component name
	 */
	public Component( String name ) {
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
	 * @return the alias
	 */
	public String getAlias() {
		return this.alias;
	}

	/**
	 * @param alias the alias to set
	 */
	public void setAlias( String alias ) {
		this.alias = alias;
	}

	/**
	 * @return the iconLocation
	 */
	public String getIconLocation() {
		return this.iconLocation;
	}

	/**
	 * @param iconLocation the iconLocation to set
	 */
	public void setIconLocation( String iconLocation ) {
		this.iconLocation = iconLocation;
	}

	/**
	 * @return the installerName
	 */
	public String getInstallerName() {
		return this.installerName;
	}

	/**
	 * @param installerName the installerName to set
	 */
	public void setInstallerName( String installerName ) {
		this.installerName = installerName;
	}

	/**
	 * @return the facetNames
	 */
	public Collection<String> getFacetNames() {
		return this.facetNames;
	}

	/**
	 * @return the importedVariables
	 * <p>
	 * Key = imported variable name.
	 * Value = true if the import is optional, false if it is required
	 * </p>
	 */
	public Map<String,Boolean> getImportedVariables() {
		return this.importedVariables;
	}

	/**
	 * @return the exportedVariables
	 */
	public Map<String, String> getExportedVariables() {
		return this.exportedVariables;
	}

	/**
	 * @return the children
	 */
	public Collection<Component> getChildren() {
		return this.children;
	}

	/**
	 * @return the ancestors
	 */
	public Collection<Component> getAncestors() {
		return this.ancestors;
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Component
				&& Utils.areEqual( this.name, ((Component) obj ).getName());
	}

	@Override
	public int hashCode() {
		return this.name == null ? 17 : this.name.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * Sets the name in a chain approach.
	 */
	public Component name( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the name in a chain approach.
	 */
	public Component alias( String alias ) {
		this.alias = alias;
		return this;
	}

	/**
	 * Sets the installerName in a chain approach.
	 */
	public Component installerName( String installerName ) {
		this.installerName = installerName;
		return this;
	}

	/**
	 * Sets the iconLocation in a chain approach.
	 */
	public Component iconLocation( String iconLocation ) {
		this.iconLocation = iconLocation;
		return this;
	}
}
