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

package net.roboconf.core.model.helpers;

import java.util.ArrayList;
import java.util.List;

import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;

/**
 * Helpers related to components.
 * @author Vincent Zurczak - Linagora
 */
public class ComponentHelpers {

	/**
	 * Finds a component by name.
	 * @param graphs the graph(s) (can be null)
	 * @param name the component name (not null)
	 * @return a component (can be null)
	 */
	public static Component findComponent( Graphs graphs, String name ) {

		Component result = null;
		List<Component> components = new ArrayList<Component> ();
		if( graphs != null )
			components.addAll( graphs.getRootComponents());

		while( result == null
				&& ! components.isEmpty()) {

			Component current = components.remove( 0 );
			if( name.equals( current.getName()))
				result = current;
			else
				components.addAll( current.getChildren());
		}

		return result;
	}


	/**
	 * Inserts (if necessary) a child component.
	 * @param child a child component (not null)
	 * @param ancestor an ancestor component (not null)
	 */
	public static void insertChild( Component ancestor, Component child ) {
		if( ! child.getAncestors().contains( ancestor ))
			child.getAncestors().add( ancestor );

		if( ! ancestor.getChildren().contains( child ))
			ancestor.getChildren().add( child );
	}


	/**
	 * Searches for a loop in the graph starting from rootComponent.
	 * @param component the component from which we introspect (not null)
	 * @return null if no cycle was found, a string describing the cycle otherwise
	 */
	public static String searchForLoop( Component component ) {
		return searchForLoop( component, new ArrayList<Component> ());
	}


	/**
	 * Searches for a loop in the graph starting from rootComponent.
	 * @param component the component from which we introspect (not null)
	 * @param ancestors the list of ancestor components (not null)
	 * @return null if no cycle was found, a string describing the cycle otherwise
	 */
	private static String searchForLoop( Component component, List<Component> ancestors ) {

		String result = null;
		int index = ancestors.indexOf( component );
		if( index >= 0 ) {
			StringBuilder sb = new StringBuilder();
			for( int i=index; i<ancestors.size(); i++ ) {
				sb.append( ancestors.get( i ).getName());
				sb.append( " -> " );
			}

			sb.append( component.getName());
			result = sb.toString();

		} else {
			ancestors.add( component );
			for( Component childComponent : component.getChildren()) {

				List<Component> updatedAncestors = new ArrayList<Component>( ancestors );
				String s = searchForLoop( childComponent, updatedAncestors );

				if( s != null ) {
					result = s;
					break;
				}
			}
		}

		return result;
	}
}
