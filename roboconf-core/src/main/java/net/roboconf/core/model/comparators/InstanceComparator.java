/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.comparators;

import java.util.Comparator;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceComparator implements Comparator<Instance> {

	@Override
	public int compare( Instance o1, Instance o2 ) {

		// Find the instance paths...
		String[] p1 = InstanceHelpers.computeInstancePath( o1 ).substring( 1 ).split( "/" );
		String[] p2 = InstanceHelpers.computeInstancePath( o2 ).substring( 1 ).split( "/" );
		int segmentsToCompare = Math.min( p1.length, p2.length );

		// ...  and compare their segments
		String[] isBefore = null;
		for( int i=0; i<segmentsToCompare && isBefore == null; i++ ) {
			int comparison = p1[ i ].compareTo( p2[ i ]);
			isBefore = comparison > 0 ? p1 : comparison < 0 ? p2 : null;
		}

		// If the compared segments are equal, compare the remains
		if( isBefore == null ) {
			isBefore = p1.length > p2.length ? p1 : p1.length < p2.length ? p2 : null;
		}

		// Return a result
		return isBefore == null ? 0 : isBefore == p1 ? 1 : -1;
	}
}
