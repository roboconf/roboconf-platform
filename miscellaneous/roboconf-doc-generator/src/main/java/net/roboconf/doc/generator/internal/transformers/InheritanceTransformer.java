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

package net.roboconf.doc.generator.internal.transformers;

import java.util.ArrayList;
import java.util.Collection;

import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Component;

/**
 * A transformer to find vertex positions for Roboconf's inheritance relations.
 * @author Vincent Zurczak - Linagora
 */
public class InheritanceTransformer extends HierarchicalTransformer {

	/**
	 * Constructor.
	 * @param component the component whose inheritance must be displayed
	 * @param extendedComponent the component this one extends directly
	 * @param extendingComponents the components that extend it
	 * @param maxPerLine the maximum number of vertices per line
	 */
	public InheritanceTransformer(
			Component component,
			Component extendedComponent,
			Collection<Component> extendingComponents,
			int maxPerLine ) {

		super( component, asList( extendedComponent ), asList( extendingComponents ), maxPerLine );
	}


	/**
	 * Convenience method to reuse the super constructor.
	 * @param extendedComponent
	 * @return a non-null collection
	 */
	static Collection<AbstractType> asList( Component extendedComponent ) {

		Collection<AbstractType> result;
		if( extendedComponent == null ) {
			result = new ArrayList<AbstractType>( 0 );

		} else {
			result = new ArrayList<AbstractType>( 1 );
			result.add( extendedComponent );
		}

		return result;
	}


	/**
	 * Convenience method to reuse the super constructor.
	 * @param components
	 * @return a non-null collection
	 */
	static Collection<AbstractType> asList( Collection<Component> components ) {

		Collection<AbstractType> result = new ArrayList<AbstractType> ();
		result.addAll( components );

		return result;
	}
}
