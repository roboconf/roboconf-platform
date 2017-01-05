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

package net.roboconf.core.model.comparators;

import java.io.Serializable;
import java.util.Comparator;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceComparator implements Serializable, Comparator<Instance> {
	private static final long serialVersionUID = 3420271536889564843L;


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
		int result = -1;
		if( isBefore == null )
			result = p1.length - p2.length;
		else if( isBefore == p1 )
			result = 1;

		return result;
	}
}
