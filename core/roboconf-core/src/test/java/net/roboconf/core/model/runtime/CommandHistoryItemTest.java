/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandHistoryItemTest {

	@Test
	public void testConstantsAreAllDifferent() throws Exception {

		// Origins
		Set<Integer> origins = new HashSet<> ();
		int originsCpt = 0;
		for( Field f : CommandHistoryItem.class.getDeclaredFields()) {
			if( ! Modifier.isStatic( f.getModifiers())
					|| ! f.getName().startsWith( "ORIGIN_" ))
				continue;

			originsCpt ++;
			origins.add((Integer) f.get( null ));
		}

		Assert.assertNotEquals( 0, originsCpt );
		Assert.assertEquals( originsCpt, origins.size());

		// Execution results
		Set<Integer> execResults = new HashSet<> ();
		int execResultsCpt = 0;
		for( Field f : CommandHistoryItem.class.getDeclaredFields()) {
			if( ! Modifier.isStatic( f.getModifiers())
					|| ! f.getName().startsWith( "EXECUTION_" ))
				continue;

			execResultsCpt ++;
			execResults.add((Integer) f.get( null ));
		}

		Assert.assertNotEquals( 0, execResultsCpt );
		Assert.assertEquals( execResultsCpt, execResults.size());
	}
}
