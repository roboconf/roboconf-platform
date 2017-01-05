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

/**
 * A facet defines properties a component may inherit.
 * @author Vincent Zurczak - Linagora
 */
public class Facet extends AbstractType implements Serializable {

	private static final long serialVersionUID = 1368377145979352404L;

	private final Collection<Facet> extendedFacets = new HashSet<Facet>( 0 );
	private final Collection<Facet> extendingFacets = new HashSet<Facet>( 0 );
	final Collection<Component> associatedComponents = new HashSet<Component>( 0 );


	/**
	 * Constructor.
	 */
	public Facet() {
		// nothing
	}

	/**
	 * Constructor.
	 * @param name the component name
	 */
	public Facet( String name ) {
		this.name = name;
	}

	/**
	 * @return the associated components (never null and NOT modifiable)
	 */
	public Collection<Component> getAssociatedComponents() {
		return Collections.unmodifiableCollection( this.associatedComponents );
	}

	/**
	 * @return the extended facets (never null and NOT modifiable)
	 */
	public Collection<Facet> getExtendedFacets() {
		return Collections.unmodifiableCollection( this.extendedFacets );
	}

	/**
	 * @return the extending facets (never null and NOT modifiable)
	 */
	public Collection<Facet> getExtendingFacets() {
		return Collections.unmodifiableCollection( this.extendingFacets );
	}

	/**
	 * Creates a bi-directional relation between this facet and another one.
	 * @param facet a facet
	 */
	public void extendFacet( Facet facet ) {
		this.extendedFacets.add( facet );
		facet.extendingFacets.add( this );
	}
}
