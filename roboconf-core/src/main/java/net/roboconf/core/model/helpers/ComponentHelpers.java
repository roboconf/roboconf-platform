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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;

/**
 * Helpers related to components.
 * @author Vincent Zurczak - Linagora
 */
public final class ComponentHelpers {

	/**
	 * Private empty constructor.
	 */
	private ComponentHelpers() {
		// nothing
	}


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
	 * Finds all the components of a graph.
	 * @param graphs a set of graphs
	 * @return a non-null list of components
	 */
	public static List<Component> findAllComponents( Graphs graphs ) {

		List<Component> result = new ArrayList<Component> ();
		Set<Component> alreadyVisisted = new HashSet<Component> ();
		List<Component> toProcess = new ArrayList<Component> ();

		toProcess.addAll( graphs.getRootComponents());
		while( ! toProcess.isEmpty()) {

			Component current = toProcess.remove( 0 );
			if( alreadyVisisted.contains( current ))
				continue;

			alreadyVisisted.add( current );
			result.add( current );
			toProcess.addAll( current.getChildren());
		}

		return result;
	}


	/**
	 * Finds all the components of an application.
	 * @param app an application (not null)
	 * @return a non-null list of components
	 */
	public static List<Component> findAllComponents( Application app ) {

		List<Component> result = new ArrayList<Component> ();
		if( app.getGraphs() != null )
			result.addAll( findAllComponents( app.getGraphs()));

		return result;
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
