/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.runtime.impl;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Component;

/**
 * A basic implementation of the {@link Component} interface.
 * @author Vincent Zurczak - Linagora
 */
public class ComponentImpl implements Component {

	private String name, alias, installerName, iconLocation;
	private File resourceFile;
	private final Collection<String> importedVariableNames = new HashSet<String> ();
	private final Collection<String> facetNames = new HashSet<String> ();
	private final Map<String,String> exportedVariables = new HashMap<String,String> ();

	private final Collection<Component> children = new HashSet<Component> ();
	private final Collection<Component> ancestors = new HashSet<Component> ();


	/**
	 * Constructor.
	 */
	public ComponentImpl() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name the component name
	 */
	public ComponentImpl( String name ) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	@Override
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
	@Override
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
	 * @return the resourceFile
	 */
	@Override
	public File getResourceFile() {
		return this.resourceFile;
	}

	/**
	 * @param resourceFile the resourceFile to set
	 */
	@Override
	public void setResourceFile( File resourceFile ) {
		this.resourceFile = resourceFile;
	}

	/**
	 * @return the iconLocation
	 */
	@Override
	public String getIconLocation() {
		return this.iconLocation;
	}

	/**
	 * @param iconLocation the iconLocation to set
	 */
	@Override
	public void setIconLocation( String iconLocation ) {
		this.iconLocation = iconLocation;
	}

	/**
	 * @return the installerName
	 */
	@Override
	public String getInstallerName() {
		return this.installerName;
	}

	/**
	 * @param installerName the installerName to set
	 */
	@Override
	public void setInstallerName( String installerName ) {
		this.installerName = installerName;
	}

	/**
	 * @return the importedVariableNames
	 */
	@Override
	public Collection<String> getImportedVariableNames() {
		return this.importedVariableNames;
	}

	/**
	 * @return the facetNames
	 */
	@Override
	public Collection<String> getFacetNames() {
		return this.facetNames;
	}

	/**
	 * @return the exportedVariables
	 */
	@Override
	public Map<String, String> getExportedVariables() {
		return this.exportedVariables;
	}

	/**
	 * @return the children
	 */
	@Override
	public Collection<Component> getChildren() {
		return this.children;
	}

	/**
	 * @return the ancestors
	 */
	@Override
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
}
