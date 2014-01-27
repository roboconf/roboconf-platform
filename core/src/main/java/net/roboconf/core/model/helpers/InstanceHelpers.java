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

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.impl.InstanceImpl;

/**
 * Helpers related to instances.
 * @author Vincent Zurczak - Linagora
 */
public class InstanceHelpers {

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
	 * @param child a child instance (not null)
	 * @param parent a parent instance (not null)
	 */
	public static void insertChild( Instance parent, InstanceImpl child ) {
		child.setParent( parent );
		parent.getChildren().add( child );
	}
}
