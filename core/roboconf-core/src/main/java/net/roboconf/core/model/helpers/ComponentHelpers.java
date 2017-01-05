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

package net.roboconf.core.model.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.comparators.AbstractTypeComparator;

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
		for( Iterator<Component> it = findAllComponents( graphs ).iterator(); it.hasNext() && result == null; ) {
			Component c = it.next();
			if( name.equals( c.getName()))
					result = c;
		}

		return result;
	}


	/**
	 * Finds a component by name.
	 * @param app an application (not null)
	 * @param name the component name (not null)
	 * @return a component (can be null)
	 */
	public static Component findComponent( AbstractApplication app, String name ) {
		Graphs graphs = app.getGraphs();
		return findComponent( graphs, name );
	}


	/**
	 * Finds a component by name from another component.
	 * @param component the component used to build a partial graph (should not be null)
	 * @param componentName the component name (not null)
	 * @return a component (can be null)
	 */
	public static Component findComponentFrom( Component component, String componentName ) {

		Graphs partialGraph = new Graphs();
		if( component != null )
			partialGraph.getRootComponents().add( component );

		return findComponent( partialGraph, componentName );
	}


	/**
	 * Extracts the names of abstract types.
	 * @param types a non-null list of types
	 * @return a non-null list of string
	 */
	public static List<String> extractNames( Collection<? extends AbstractType> types ) {

		List<String> result = new ArrayList<> ();
		for( AbstractType t : types )
			result.add( t.getName());

		return result;
	}


	/**
	 * Determines whether a component is associated with the "target" installer or not.
	 * @param component a component (not null)
	 * @return true if it is associated with the "target" installer, false otherwise
	 */
	public static boolean isTarget( Component component ) {
		return Constants.TARGET_INSTALLER.equalsIgnoreCase( component.getInstallerName());
	}


	/**
	 * Finds all the possible children of a component.
	 * @param component a non-null component
	 * @return a non-null list of components
	 */
	public static Collection<Component> findAllChildren( Component component ) {
		return findAncestorsOrChildren( component, true );
	}


	/**
	 * Finds all the possible ancestors of a component.
	 * @param component a non-null component
	 * @return a non-null list of components
	 */
	public static Collection<Component> findAllAncestors( Component component ) {
		return findAncestorsOrChildren( component, false );
	}


	/**
	 * Finds the installer name for a given component.
	 * @param component a component
	 * @return the installer name (potentially retrieved from an extended component)
	 */
	public static String findComponentInstaller( Component component ) {

		String installer = null;
		for( Component c : findAllExtendedComponents( component )) {
			installer = c.getInstallerName();
			if( installer != null )
				break;
		}

		return installer;
	}


	/**
	 * Finds all the exported variables for a given component.
	 * <p>
	 * This method also fixes the name of the exported variables (set the right prefixes, if required).
	 * </p>
	 * <p>
	 * A component can override variables values of the components its extends.
	 * It can also override variable values from inherited and associated facets.
	 * </p>
	 * <p>
	 * To solve conflicts between facets and inherited components, facets variables are
	 * resolved first. Then, component values are injected and may thus override facet
	 * values.
	 * </p>
	 *
	 * @param component a non-null component
	 * @return a non-null map of exported variables (key = variable name, value = variable value).
	 */
	public static Map<String,String> findAllExportedVariables( Component component ) {
		return findAllExportedVariables( component, new HashSet<Component>( 0 ));
	}


	/**
	 * Finds all the exported variables for a given Roboconf type.
	 * <p>
	 * This method also fixes the name of the exported variables (set the right prefixes, if required).
	 * </p>
	 *
	 * @param type a type (facet or component)
	 * @return a non-null map of exported variables (key = variable name, value = variable value).
	 */
	public static Map<String,String> findAllExportedVariables( AbstractType type ) {

		Map<String,String> result;
		if( type instanceof Facet )
			result = findAllExportedVariables((Facet) type);
		else
			result = findAllExportedVariables((Component) type);

		return result;
	}


	/**
	 * Finds all the exported variables for a given application / graph(s).
	 * @param graphs a non-null graph(s)
	 * @return a non-null map of exported variables (key = variable name, value = variable value).
	 */
	public static Map<String,String> findAllExportedVariables( Graphs graphs ) {

		Map<String,String> result = new HashMap<> ();
		for( Component c : findAllComponents( graphs ))
			result.putAll( findAllExportedVariables( c ));

		return result;
	}


	/**
	 * Finds all the exported variables of a facet.
	 * <p>
	 * This method also fixes the name of the exported variables (set the right prefixes, if required).
	 * </p>
	 * <p>
	 * A facet can also override variable values from the facets its extends.
	 * </p>
	 *
	 * @param facet a facet
	 * @return a non-null map (key = variable name, value = variable value).
	 */
	public static Map<String,String> findAllExportedVariables( Facet facet ) {

		Map<Facet,Boolean> facetToResolved = new HashMap<> ();
		Map<Facet,Map<String,String>> facetToResolvedExports = new HashMap<> ();

		Collection<Facet> facets = new HashSet<>( findAllExtendedFacets( facet ));
		facets.add( facet );
		for( Facet f : facets ) {
			facetToResolvedExports.put( f, new HashMap<String,String> ());
			facetToResolved.put( f, Boolean.FALSE );
		}

		// The implementation prevents cycles from blocking the resolution.
		while( facetToResolved.containsValue( Boolean.FALSE )) {
			entries: for( Facet f : facetToResolvedExports.keySet()) {

				// A facet can be processed only if all the facets it extends were already processed.
				if( facetToResolved.get( f ))
					continue entries;

				// In case of cycle... A extends B which extends A...
				if( searchForInheritanceCycle( f ) != null ) {
					facetToResolved.put( f, Boolean.TRUE );
					continue;
				}

				// Get the resolved variables from all the extended facets.
				Map<String,String> localExportedVariables = new HashMap<> ();
				for( Facet ff : f.getExtendedFacets()) {
					if( ! facetToResolved.get( ff ))
						continue entries;

					localExportedVariables.putAll( facetToResolvedExports.get( ff ));
				}

				// If a variable name already exists from the inherited one, we can directly override it.
				// Otherwise, we may have to update the variable prefix.
				for( Map.Entry<String,ExportedVariable> var : f.exportedVariables.entrySet()) {
					String value = var.getValue().getValue();
					if( localExportedVariables.containsKey( var.getKey()))
						localExportedVariables.put( var.getKey(), value );
					else
						localExportedVariables.put( fixVariableName( f, var.getKey()), value );
				}

				facetToResolvedExports.put( f, localExportedVariables );
				facetToResolved.put( f, Boolean.TRUE );
			}
		}

		// Now, the result is easy to get.
		return facetToResolvedExports.get( facet );
	}


	/**
	 * Finds all the imported variables for a given component.
	 * @param component a non-null component
	 * @return a non-null map of imported variables (key = variable name, value = imported variable bean)
	 */
	public static Map<String,ImportedVariable> findAllImportedVariables( Component component ) {

		// Process components from the ancestors to the children... => override
		Map<String,ImportedVariable> result = new HashMap<> ();
		List<Component> extendedComponents = findAllExtendedComponents( component );
		Collections.reverse( extendedComponents );

		for( Component c : extendedComponents )
			result.putAll( c.importedVariables );

		return result;
	}


	/**
	 * Finds all the facets of a component.
	 * <p>
	 * Inheritance cycles are ignored.
	 * </p>
	 *
	 * @param component a non-null component
	 * @return a non-null list of facets
	 */
	public static Collection<Facet> findAllFacets( Component component ) {

		Set<Facet> result = new HashSet<> ();
		List<Facet> toProcess = new ArrayList<> ();
		for( Component c : findAllExtendedComponents( component ))
			toProcess.addAll( c.getFacets());

		while( ! toProcess.isEmpty()) {
			Facet f = toProcess.remove( 0 );
			result.add( f );
			toProcess.addAll( f.getExtendedFacets());

			// Prevent loops
			toProcess.removeAll( result );
		}

		return result;
	}


	/**
	 * Finds all the components that this component inherits from.
	 * <p>
	 * For commodity reasons, the result always contains the current component.
	 * </p>
	 * <p>
	 * Inheritance cycles are ignored.
	 * </p>
	 *
	 * @param component a non-null component
	 * @return a non-null list (it always contains the <code>component</code> parameter)
	 */
	public static List<Component> findAllExtendedComponents( Component component ) {

		List<Component> result = new ArrayList<> ();
		for( Component c = component; c != null; c = c.getExtendedComponent()) {
			if( result.contains( c ))
				break;

			result.add( c );
		}

		return result;
	}


	/**
	 * Finds all the facets that this facet inherits from.
	 * <p>
	 * Inheritance cycles are ignored.
	 * </p>
	 *
	 * @param facet a non-null facet
	 * @return a non-null collection
	 */
	public static Collection<Facet> findAllExtendedFacets( Facet facet ) {

		Set<Facet> result = new HashSet<> ();
		Set<Facet> toProcess = new HashSet<>( facet.getExtendedFacets());
		while( ! toProcess.isEmpty()) {

			Facet f = toProcess.iterator().next();
			result.add( f );
			toProcess.addAll( f.getExtendedFacets());
			toProcess.removeAll( result );
		}

		return result;
	}


	/**
	 * Finds all the facets that extend this facet.
	 * <p>
	 * Inheritance cycles are ignored.
	 * </p>
	 *
	 * @param facet a non-null facet
	 * @return a non-null collection
	 */
	public static Collection<Facet> findAllExtendingFacets( Facet facet ) {

		Set<Facet> result = new HashSet<> ();
		Set<Facet> toProcess = new HashSet<>( facet.getExtendingFacets());
		while( ! toProcess.isEmpty()) {

			Facet f = toProcess.iterator().next();
			result.add( f );
			toProcess.addAll( f.getExtendingFacets());
			toProcess.removeAll( result );
		}

		return result;
	}


	/**
	 * Finds all the components that extend a given component.
	 * <p>
	 * Inheritance cycles are ignored.
	 * </p>
	 *
	 * @param component a component
	 * @return a non-null collection
	 */
	public static Collection<Component> findAllExtendingComponents( Component component ) {

		Collection<Component> result = new HashSet<> ();
		Set<Component> toProcess = new HashSet<>( component.getExtendingComponents());
		while( ! toProcess.isEmpty()) {

			Component c = toProcess.iterator().next();
			result.add( c );
			toProcess.addAll( c.getExtendingComponents());
			toProcess.removeAll( result );
		}

		// In case of circular dependencies...
		result.remove( component );

		return result;
	}


	/**
	 * Finds all the components of a graph.
	 * <p>
	 * Inheritance cycles are ignored.
	 * </p>
	 *
	 * @param graphs a set of graphs
	 * @return a non-null list of components
	 */
	public static List<Component> findAllComponents( Graphs graphs ) {

		Set<Component> result = new HashSet<> ();
		Set<Component> toProcess = new HashSet<> ();

		toProcess.addAll( graphs.getRootComponents());
		while( ! toProcess.isEmpty()) {
			Component current = toProcess.iterator().next();
			result.add( current );

			toProcess.addAll( findAllExtendedComponents( current ));
			toProcess.addAll( findAllExtendingComponents( current ));
			toProcess.addAll( findAllChildren( current ));
			toProcess.addAll( findAllAncestors( current ));

			// Prevent loops
			toProcess.removeAll( result );
		}

		return new ArrayList<>( result );
	}


	/**
	 * Finds all the components of an application or a template.
	 * <p>
	 * Inheritance cycles are ignored.
	 * </p>
	 *
	 * @param app an application or a template (not null)
	 * @return a non-null list of components
	 */
	public static List<Component> findAllComponents( AbstractApplication app ) {

		List<Component> result = new ArrayList<> ();
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
	 * <p>
	 * We keep a list of all the ancestors. We then go through all the children.
	 * If a child appears to be also in the ancestors, then we have a cycle in
	 * the container-contained perspective.
	 * </p>
	 *
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
			for( Component childComponent : findAllChildren( component )) {

				List<Component> updatedAncestors = new ArrayList<>( ancestors );
				String s = searchForLoop( childComponent, updatedAncestors );

				if( s != null ) {
					result = s;
					break;
				}
			}
		}

		return result;
	}


	/**
	 * Searches for a cycle in the facets inheritance.
	 * @param facet the facet from which we introspect (not null)
	 * @return null if no cycle was found, a string describing the cycle otherwise
	 */
	public static String searchForInheritanceCycle( Facet facet ) {

		String result = null;
		if( findAllExtendedFacets( facet ).contains( facet ))
			result = facet + " -> ... -> " + facet;

		return result;
	}


	/**
	 * Searches for a cycle in the facets inheritance.
	 * @param component the component from which we introspect (not null)
	 * @return null if no cycle was found, a string describing the cycle otherwise
	 */
	public static String searchForInheritanceCycle( Component component ) {

		String result = null;
		for( Component c = component.getExtendedComponent(); c != null && result == null; c = c.getExtendedComponent()) {
			if( c.equals( component ))
				result = component + " -> ... -> " + component;
		}

		return result;
	}


	/**
	 * Finds the component dependencies for a given component.
	 * @param component a component
	 * @return a non-null map (key = component name, value = true if the dependency is optional, false otherwise)
	 */
	public static Map<String,Boolean> findComponentDependenciesFor( Component component ) {

		Map<String,Boolean> map = new HashMap<> ();
		for( ImportedVariable var : findAllImportedVariables( component ).values()) {
			String componentOrFacet = VariableHelpers.parseVariableName( var.getName()).getKey();
			Boolean b = map.get( componentOrFacet );
			if( b == null || b )
				map.put( componentOrFacet, var.isOptional());
		}

		return map;
	}


	/**
	 * Finds all the components a component depends on.
	 * @param component a component
	 * @param app the application
	 * @return a non-null map (key = component name, value = true if the dependency is optional, false otherwise)
	 */
	public static Map<Component,Boolean> findComponentDependenciesFor( Component component, ApplicationTemplate app ) {

		// Determine which components or facets are required.
		Map<String,Boolean> map = findComponentDependenciesFor( component );

		// Resolve names to components
		Map<Component,Boolean> result = new HashMap<> ();
		for( Component c : findAllComponents( app )) {

			// Check component names first.
			Boolean required = map.get( c.getName());
			if( required != null ) {
				result.put( c, required );
			}

			// Or maybe it is a facet name.
			else for( Facet f : findAllFacets( c )) {
				required = map.get( f.getName());

				// If this component owns the facet, stop here.
				if( required != null ) {
					result.put( c, required );
					break;
				}
			}
		}

		return result;
	}


	/**
	 * Finds all the components that depend on a given component.
	 * @param component a component
	 * @param app the application
	 * @return a non-null map (key = component name, value = true if the dependency is optional, false otherwise)
	 */
	public static Map<Component,Boolean> findComponentsThatDependOn( Component component, ApplicationTemplate app ) {

		// Determine the matching prefixes.
		Set<String> prefixes = new HashSet<> ();
		prefixes.add( component.getName());
		for( Facet f : findAllFacets( component ))
			prefixes.add( f.getName());

		// Resolve names to components
		Map<Component,Boolean> result = new HashMap<> ();
		for( Component c : findAllComponents( app )) {

			Map<String,Boolean> map = findComponentDependenciesFor( c );
			map.keySet().retainAll( prefixes );

			// Empty map => 'c' does not depend on 'component'
			if( map.isEmpty())
				continue;

			// Otherwise, determine the dependency
			boolean value = true;
			for( Boolean b : map.values())
				value = value && b;

			result.put( c, value );
		}

		return result;
	}


	/**
	 * Fixes the name of an exported variable name.
	 * <p>
	 * An exported variable must ALWAYS be prefixed with the name of the "type"
	 * that exports it.
	 * </p>
	 *
	 * @param type a facet or a component
	 * @param exportedVariableName the name of an exported variable
	 * @return a non-null string
	 */
	static String fixVariableName( AbstractType type, String exportedVariableName ) {

		String prefix = type.getName() + ".";
		String result = exportedVariableName;
		if( ! result.startsWith( prefix ))
			result = prefix + exportedVariableName;

		return result;
	}


	/**
	 * Finds the ancestors or the children of a given component.
	 * <p>
	 * The algorithm to find them is the same. The only difference
	 * lies in the list we go through.
	 * </p>
	 *
	 * @param component the component (not null)
	 * @param children true to search children, false for ancestors
	 * @return a non-null list
	 */
	private static Collection<Component> findAncestorsOrChildren( final Component component, final boolean children ) {

		// The algorithm of death...
		Set<Component> result = new TreeSet<>( new AbstractTypeComparator());
		for( Component c : findAllExtendedComponents( component )) {

			// A component may have zero child or ancestor.
			// But its facets may define ones.
			Collection<AbstractType> list = new HashSet<> ();
			list.addAll( children ? c.getChildren() : c.getAncestors());
			for( Facet facet : findAllFacets( c ))
				list.addAll( children ? facet.getChildren() : facet.getAncestors());

			// Now, take a look at the list's content.
			for( AbstractType type : list ) {

				if( type instanceof Component ) {
					Component cType = (Component) type;

					// Add the component but also those that extend it!
					result.add( cType );
					result.addAll( findAllExtendingComponents( cType ));
				}

				else {
					Facet fType = (Facet) type;

					// Find all the "super" facets
					Collection<Facet> allFacets = findAllExtendingFacets( fType );
					allFacets.add( fType );
					for( Facet facet : allFacets ) {

						// Add all the components associated with the facet.
						// Add their extending components too!
						for( Component cc : facet.getAssociatedComponents()) {
							result.add( cc );
							result.addAll( findAllExtendingComponents( cc ));
						}
					}
				}
			}
		}

		return result;
	}


	/**
	 * Finds all the exported variables for a given component.
	 * <p>
	 * This method also fixes the name of the exported variables (set the right prefixes, if required).
	 * </p>
	 * <p>
	 * A component can override variables values of the components its extends.
	 * It can also override variable values from inherited and associated facets.
	 * </p>
	 * <p>
	 * To solve conflicts between facets and inherited components, facets variables are
	 * resolved first. Then, component values are injected and may thus override facet
	 * values.
	 * </p>
	 *
	 * @param component a non-null component
	 * @param alreadyChecked a non-null list of already checked components (prevents cycles)
	 * @return a non-null map of exported variables (key = variable name, value = variable value).
	 */
	private static Map<String,String> findAllExportedVariables( Component component, Set<Component> alreadyChecked ) {
		Map<String,String> result = new HashMap<> ();

		// Get all the inherited variables from facets
		for( Facet f : component.getFacets())
			result.putAll( findAllExportedVariables( f ));

		// Now, get all the exported variables by super components.
		// Recursive call here...
		List<Component> extendedComponents = findAllExtendedComponents( component );
		Collections.reverse( extendedComponents );
		extendedComponents.remove( component );
		extendedComponents.removeAll( alreadyChecked );

		Set<Component> ac = new HashSet<>( alreadyChecked );
		ac.add( component );
		for( Component c : extendedComponents )
			result.putAll( findAllExportedVariables( c, ac ));

		// Handle current variables
		Map<String,String> newVariables = new HashMap<> ();
		for( Map.Entry<String,ExportedVariable> entry : component.exportedVariables.entrySet()) {

			// If a variable name already exists from the inherited one, we can directly override it.
			String variableValue = entry.getValue().getValue();
			if( result.containsKey( entry.getKey()))
				newVariables.put( entry.getKey(), variableValue );

			// Or it is a new value
			else
				newVariables.put( fixVariableName( component, entry.getKey()), variableValue );

			// Override inherited variables.
			// If the component exports a and that its inherits Alpha.a, then we should
			// override the value of Alpha.a too. In some way, we skip prefixes...
			for( String inheritedName : result.keySet()) {
				String nameSuffix = VariableHelpers.parseVariableName( inheritedName ).getValue();
				if( entry.getKey().equals( nameSuffix ))
					newVariables.put( inheritedName, variableValue );
			}
		}

		result.putAll( newVariables );

		// Now, we must replicate variables that do not have the right prefix.
		// Let's assume that A extends B, that A exports a1, a2, and that B exports b1.
		// Then...
		// A exports A.a1 and A.a2. So far, so good.
		// Now, B exports B.b1.
		//
		// Besides, B is also a "A". So, its exports A.a1 and A.a2 (if someone wants to know all the A instances, B should respond).
		// Now, if someone only wants to know B instances, B should propagate inherited information with its name prefix.
		// So, B will also export B.a1 and B.a2
		newVariables.clear();;
		for( Map.Entry<String,String> entry : result.entrySet()) {
			Map.Entry<String,String> var = VariableHelpers.parseVariableName( entry.getKey());
			String newKey = component.getName() + "." + var.getValue();
			if( ! component.getName().equals( var.getKey())
					&& ! result.containsKey( newKey ))
				newVariables.put( newKey, entry.getValue());
		}

		result.putAll( newVariables );
		return result;
	}
}
