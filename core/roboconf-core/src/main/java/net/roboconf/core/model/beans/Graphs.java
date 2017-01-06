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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * This object contains sets of related components.
 * @author Vincent Zurczak - Linagora
 */
public class Graphs implements Serializable {

	private static final long serialVersionUID = 2918281424743945139L;
	private final Collection<Component> rootsComponents = new HashSet<Component> ();
	private final Map<String,Facet> facetNameToFacet = new HashMap<> ();

	/**
	 * @return a non-null list of root components
	 */
	public Collection<Component> getRootComponents() {
		return this.rootsComponents;
	}

	/**
	 * @return the facetNameToFacet
	 */
	public Map<String,Facet> getFacetNameToFacet() {
		return this.facetNameToFacet;
	}
}
