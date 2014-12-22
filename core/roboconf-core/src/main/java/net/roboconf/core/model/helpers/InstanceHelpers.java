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

package net.roboconf.core.model.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.RuntimeModelValidator;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;

/**
 * Helpers related to instances.
 * @author Vincent Zurczak - Linagora
 */
public final class InstanceHelpers {

	/**
	 * Private empty constructor.
	 */
	private InstanceHelpers() {
		// nothing
	}


	/**
	 * Builds a string representing the path from a root instance to this instance.
	 * <p>
	 * This string can be considered as a computed ID (or signature) of the instance.
	 * It only makes sense within a given application.
	 * </p>
	 *
	 * @param inst an instance (not null)
	 * @return a string (not null)
	 */
	public static String computeInstancePath( Instance inst ) {

		StringBuilder sb = new StringBuilder();
		for( Instance current = inst; current != null; current = current.getParent()) {
			StringBuilder currentSb = new StringBuilder( "/" );
			if( ! Utils.isEmptyOrWhitespaces( current.getName()))
				currentSb.append( current.getName());

			sb.insert( 0, currentSb.toString());
		}

		return sb.toString();
	}


	/**
	 * @param i1 an instance (not null)
	 * @param i2 an instance (not null)
	 * @return true if they have the same path, false otherwise
	 */
	public static boolean haveSamePath( Instance i1, Instance i2 ) {
		return computeInstancePath( i1 ).equals( computeInstancePath( i2 ));
	}


	/**
	 * Builds a list of instances ordered hierarchically.
	 * <p>
	 * Basically, we give it a root instance (level 0).<br />
	 * This instance is added in the list head.<br />
	 * Then, it adds child instances at level 1.<br />
	 * Then, at level 2, etc.
	 * </p>
	 * <p>
	 * Last elements in the list are leaves instances.
	 * </p>
	 *
	 * @param inst the instance from which we introspect
	 * <p>
	 * It will be the first item of the resulting list.
	 * </p>
	 *
	 * @return a non-null list
	 */
	public static List<Instance> buildHierarchicalList( Instance inst ) {

		List<Instance> instanceList = new ArrayList<Instance> ();
		List<Instance> todo = new ArrayList<Instance> ();
		if( inst != null )
			todo.add( inst );

		while( ! todo.isEmpty()) {
			Instance current = todo.remove( 0 );
			instanceList.add( current );
			todo.addAll( current.getChildren());
		}

		return instanceList;
	}


	/**
	 * Inserts a child instance.
	 * <p>
	 * This method does not check anything.
	 * In real implementations, such as in the DM, one should
	 * use {@link #tryToInsertChildInstance(Application, Instance, Instance)}.
	 * </p>
	 *
	 * @param child a child instance (not null)
	 * @param parent a parent instance (not null)
	 */
	public static void insertChild( Instance parent, Instance child ) {
		child.setParent( parent );
		parent.getChildren().add( child );
	}


	/**
	 * Gets the exported variables for an instance.
	 * <p>
	 * It includes the component variables, and the variables
	 * overridden by the instance.
	 * </p>
	 *
	 * @param instance an instance (not null)
	 * @return a non-null map (key = variable name, value = default variable value - can be null).
	 */
	public static Map<String,String> findAllExportedVariables( Instance instance ) {

		// Find the variables
		Map<String,String> result = new HashMap<String,String> ();
		if( instance.getComponent() != null )
			result.putAll( ComponentHelpers.findAllExportedVariables( instance.getComponent()));

		// Overridden variables may not contain the facet or component prefix.
		// To remain as flexible as possible, we will try to resolve them as component or facet variables.
		Map<String,Set<String>> localNameToFullNames = new HashMap<String,Set<String>> ();
		for( String inheritedVarName : result.keySet()) {
			String localName = VariableHelpers.parseVariableName( inheritedVarName ).getValue();
			Set<String> fullNames = localNameToFullNames.get( localName );
			if( fullNames == null )
				fullNames = new HashSet<String> ();

			fullNames.add( inheritedVarName );
			localNameToFullNames.put( localName, fullNames );
		}

		for( Map.Entry<String,String> entry : instance.overridenExports.entrySet()) {
			Set<String> fullNames = localNameToFullNames.get( entry.getKey());

			// No inherited variable or too many inherited ones? => Put it in raw mode.
			if( fullNames == null || fullNames.size() > 1 )
				result.put( entry.getKey(), entry.getValue());
			// Otherwise, override the inherited variable
			else
				result.put( fullNames.iterator().next(), entry.getValue());
		}

		// Update some values
		String ip = findRootInstance( instance ).data.get( Instance.IP_ADDRESS );
		if( ip != null )
			VariableHelpers.updateNetworkVariables( result, ip );

		return result;
	}


	/**
	 * Finds an instance by name.
	 * @param application the application
	 * @param instancePath the instance path
	 * @return an instance, or null if it was not found
	 */
	public static Instance findInstanceByPath( Application application, String instancePath ) {

		Collection<Instance> currentList = new ArrayList<Instance> ();
		if( application != null )
			currentList.addAll( application.getRootInstances());

		List<String> instanceNames = new ArrayList<String> ();
		if( instancePath != null )
			instanceNames.addAll( Arrays.asList( instancePath.split( "/" )));

		if( instanceNames.size() > 0
				&& Utils.isEmptyOrWhitespaces( instanceNames.get( 0 )))
			instanceNames.remove( 0 );

		// Every path segment points to an instance
		Instance result = null;
		for( String instanceName : instanceNames ) {

			result = null;
			for( Instance instance : currentList ) {
				if( instanceName.equals( instance.getName())) {
					result = instance;
					break;
				}
			}

			// The segment does not match any instance
			if( result == null )
				break;

			// Otherwise, prepare the next iteration
			currentList = result.getChildren();
		}

		return result;
	}


	/**
	 * Finds an instance by name.
	 * @param rootInstance a root instance
	 * @param instancePath the instance path
	 * @return an instance, or null if it was not found
	 */
	public static Instance findInstanceByPath( Instance rootInstance, String instancePath ) {

		Application tempApplication = new Application();
		if( rootInstance != null )
			tempApplication.getRootInstances().add( rootInstance );

		return findInstanceByPath( tempApplication, instancePath );
	}


	/**
	 * Finds instances by component name.
	 * @param application an application (not null)
	 * @param componentName a component name (not null)
	 * @return a non-null list of instances
	 */
	public static List<Instance> findInstancesByComponentName( Application application, String componentName ) {

		List<Instance> result = new ArrayList<Instance> ();
		for( Instance inst : getAllInstances( application )) {
			if( componentName.equals( inst.getComponent().getName()))
				result.add( inst );
		}

		return result;
	}


	/**
	 * Finds the root instance for an instance.
	 * @param instance an instance (not null)
	 * @return a non-null instance, the root instance
	 */
	public static Instance findRootInstance( Instance instance ) {

		Instance rootInstance = instance;
		while( rootInstance.getParent() != null )
			rootInstance = rootInstance.getParent();

		return rootInstance;
	}


	/**
	 * Gets all the instances of an application.
	 * @param application an application (not null)
	 * @return a non-null list of instances
	 * <p>
	 * The result is a list made up of ordered lists.<br />
	 * root-0, child-0-1, child-0-2, child-0-1-1, etc.<br />
	 * root-1, child-1-1, child-1-2, child-1-1-1, etc.<br />
	 * </p>
	 * <p>
	 * It means the resulting list can be considered as valid for starting instances
	 * from the root instances to the bottom leaves.
	 * </p>
	 */
	public static List<Instance> getAllInstances( Application application ) {

		List<Instance> result = new ArrayList<Instance> ();
		for( Instance instance : application.getRootInstances())
			result.addAll( InstanceHelpers.buildHierarchicalList( instance ));

		return result;
	}


	/**
	 * Tries to insert a child instance.
	 * <ol>
	 * 		<li>Check if there is no child instance with this name.</li>
	 * 		<li>Check that the graph(s) allow it (coherence with respect to the components).</li>
	 * 		<li>Insert the instance.</li>
	 * 		<li>Validate the application after insertion.</li>
	 * 		<li>Critical error => revert the insertion.</li>
	 * </ol>
	 * <p>
	 * This method assumes the application is already valid before the insertion.
	 * Which makes sense.
	 * </p>
	 *
	 * @param application the application (not null)
	 * @param parentInstance the parent instance (can be null)
	 * @param childInstance the child instance (not null)
	 * @return true if the child instance could be inserted, false otherwise
	 */
	public static boolean tryToInsertChildInstance( Application application, Instance parentInstance, Instance childInstance ) {

		boolean success = false;
		Collection<Instance> list = parentInstance == null ? application.getRootInstances() : parentInstance.getChildren();

		// First, make sure there is no child instance with this name before inserting.
		// Otherwise, removing the child instance may result randomly.
		boolean hasAlreadyAChildWithThisName = false;
		for( Instance inst : list ) {
			if(( hasAlreadyAChildWithThisName = childInstance.getName().equals( inst.getName())))
				break;
		}

		// We insert a "root instance"
		if( parentInstance == null ) {
			if( ! hasAlreadyAChildWithThisName
					&& ComponentHelpers.findAllAncestors( childInstance.getComponent()).isEmpty()) {

				application.getRootInstances().add( childInstance );
				success = true;
				// No validation here, but maybe we should...
			}
		}

		// We insert a child instance
		else {
			if( ! hasAlreadyAChildWithThisName
					&& ComponentHelpers.findAllChildren( parentInstance.getComponent()).contains( childInstance.getComponent())) {

				InstanceHelpers.insertChild( parentInstance, childInstance );
				Collection<Instance> allInstances = InstanceHelpers.getAllInstances( application );
				Collection<RoboconfError> errors = RuntimeModelValidator.validate( allInstances );
				if( RoboconfErrorHelpers.containsCriticalErrors( errors )) {
					parentInstance.getChildren().remove( childInstance );
					childInstance.setParent( null );

				} else {
					success = true;
				}
			}
		}

		return success;
	}


	/**
	 * Finds the directory where an agent stores the files for a given instance.
	 * @param instance an instance (not null)
	 * @return a file (not null, but may not exist)
	 */
	public static File findInstanceDirectoryOnAgent( Instance instance ) {

		String path = InstanceHelpers.computeInstancePath( instance );
		path = path.substring( 1 ).replace( '/', '_' ).replace( ' ', '_' );
		String installerName = ComponentHelpers.findComponentInstaller( instance.getComponent());

		return new File( System.getProperty( "java.io.tmpdir" ), "roboconf_agent/" + installerName + "/" + path );
	}


	/**
	 * Count the number of instance names into a given path.
	 * @param instancePath an instance path (not null)
	 * @return an integer
	 */
	public static int countInstances( String instancePath ) {
		return instancePath.split( "/" ).length - 1;
	}


	/**
	 * Replicates an instance and its children.
	 * <p>
	 * The result does not have any parent. It does not have any
	 * data, nor imports or real exports. In fact, only the name,
	 * the component association, the channel and the overridden exports
	 * are copied. The children are not "referenced" but replicated.
	 * </p>
	 *
	 * @param instance a non-null instance to replicate
	 * @return a non-null instance
	 */
	public static Instance replicateInstance( Instance instance ) {

		Map<Instance,Instance> instanceToDuplicate = new HashMap<Instance,Instance> ();
		List<Instance> toProcess = new ArrayList<Instance> ();
		toProcess.add( instance );

		while( ! toProcess.isEmpty()) {
			Instance current = toProcess.remove( 0 );

			Instance copy = new Instance();
			copy.name( current.getName());
			copy.component( current.getComponent());
			copy.channels.addAll( current.channels );
			copy.overridenExports.putAll( current.overridenExports );
			instanceToDuplicate.put( current, copy );

			Instance parent = instanceToDuplicate.get( current.getParent());
			if( parent != null )
				insertChild( parent, copy );

			toProcess.addAll( current.getChildren());
		}

		return instanceToDuplicate.get( instance );
	}
}
