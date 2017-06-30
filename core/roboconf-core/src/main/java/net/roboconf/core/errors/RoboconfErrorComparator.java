/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.errors;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * A comparator for Roboconf errors.
 * <p>
 * This implementation is not compliant with the use of Tree sets!<br />
 * To be clear, tree sets consider two objects are equal if their comparator return 0.
 * It does not verify the equal method (please, report to the javadoc of TreeSet). So, this
 * class guarantees that the comparison result is 0 if and only if they are equal.
 * </p>
 * <p>
 * Comparing errors should mostly be used within the tooling (for performance reasons).
 * Hence the externalization of the comparison in another class (we did not make Roboconf errors
 * implement the Comparable interface).
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfErrorComparator implements Serializable, Comparator<RoboconfError> {

	private static final long serialVersionUID = 6641916290820938883L;


	@Override
	public int compare( RoboconfError o1, RoboconfError o2 ) {

		// Be careful. When used with tree sets, objects whose comparison results in 0
		// are considered to be equal. First, check the error code as it is the most
		// efficient option.
		int result = o1.getErrorCode().compareTo( o2.getErrorCode());

		// Return 0 only if objects are equal
		if( result == 0
				&& ! Objects.equals( o1, o2 )) {

			// Otherwise, compare the details (alphabetically)
			for( int i=0; i<o1.getDetails().length && result == 0; i++ ) {
				for( int j=0; j<o2.getDetails().length && result == 0; j++ ) {
					result = o1.getDetails()[ i ].toString().compareTo( o2.getDetails()[ j ].toString());
				}
			}

			// If all the strings are the same, compare by array size
			if( result == 0 )
				result = o1.getDetails().length - o2.getDetails().length;

			// Since the objects are not equal, in no case we can accept to return 0.
			// So, let's just take another order. The class name is used.
			// So, it guarantees we respect the contract of this method.
			//
			// A = compare( o1, o2 ) <=> compare( o2, o1 ) = -A
			if( result == 0 )
				result = o1.getClass().getName().compareTo( o2.getClass().getName());
		}

		return result;
	}
}
