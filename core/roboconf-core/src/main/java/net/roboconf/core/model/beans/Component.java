/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A component represents a Software item (hardware, software, whatever).
 * @author Vincent Zurczak - Linagora
 */
public class Component extends AbstractType implements Serializable {

	private static final long serialVersionUID = 5163458185512982868L;

	private String installerName;
	private Component extendedComponent;

	public final Map<String,ImportedVariable> importedVariables = new HashMap<>( 0 );
	private final Collection<Component> extendingComponents = new HashSet<Component>( 0 );
	private final Collection<Facet> facets = new HashSet<Facet>( 0 );


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
	 * @return the extendedComponent
	 */
	public Component getExtendedComponent() {
		return this.extendedComponent;
	}

	/**
	 * @return the extending components (not null and not modifiable)
	 */
	public Collection<Component> getExtendingComponents() {
		return Collections.unmodifiableCollection( this.extendingComponents );
	}

	/**
	 * @return the facets (not null and not modifiable)
	 */
	public Collection<Facet> getFacets() {
		return Collections.unmodifiableCollection( this.facets );
	}

	/**
	 * Sets the name in a chain approach.
	 */
	public Component name( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the installer name in a chain approach.
	 */
	public Component installerName( String installerName ) {
		this.installerName = installerName;
		return this;
	}

	/**
	 * Creates a bi-directional relation between this component and an extended one.
	 * @param component a component
	 */
	public void extendComponent( Component component ) {

		if( this.extendedComponent != null )
			this.extendedComponent.extendingComponents.remove( this );

		component.extendingComponents.add( this );
		this.extendedComponent = component;
	}

	/**
	 * Creates a bi-directional relation between this component and a facet.
	 * @param facet a facet
	 */
	public void associateFacet( Facet facet ) {
		this.facets.add( facet );
		facet.associatedComponents.add( this );
	}

	/**
	 * Deletes a bi-directional relation between this component and a facet.
	 * @param facet a facet
	 */
	public void disassociateFacet( Facet facet ) {
		this.facets.remove( facet );
		facet.associatedComponents.remove( this );
	}

	/**
	 * A convenience method to add an imported variable.
	 * @param var a non-null variable
	 */
	public void addImportedVariable( ImportedVariable var ) {
		this.importedVariables.put( var.getName(), var );
	}
}
