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

package net.roboconf.core.model.comparators;

import java.io.Serializable;
import java.util.Comparator;

import net.roboconf.core.model.beans.AbstractType;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractTypeComparator implements Serializable, Comparator<AbstractType> {
	private static final long serialVersionUID = 3420271546889564843L;

	@Override
	public int compare( AbstractType o1, AbstractType o2 ) {

		int result;
		if( o1 == o2 )
			result = 0;
		else if( o1 == null )
			result = 1;
		else if( o2 == null )
			result = -1;
		else
			result = o1.getName().compareTo( o2.getName());

		return result;
	}
}
