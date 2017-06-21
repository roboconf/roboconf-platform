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

import static net.roboconf.core.errors.ErrorDetails.component;
import static net.roboconf.core.errors.ErrorDetails.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.model.ModelError;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfErrorComparatorTest {

	@Test
	public void testCompareTo() {

		// Build a list of unsorted errors
		List<RoboconfError> errors = new ArrayList<> ();
		errors.add( new RoboconfError( ErrorCode.PROJ_APPLICATION_TEMPLATE_NOT_FOUND ));
		errors.add( new RoboconfError( ErrorCode.PM_DOT_IS_NOT_ALLOWED, instance( "inst1" )));
		errors.add( new RoboconfError( ErrorCode.PM_DOT_IS_NOT_ALLOWED ));
		errors.add( new RoboconfError( ErrorCode.RM_ALREADY_DEFINED_EXTERNAL_EXPORT ));
		errors.add( new RoboconfError( ErrorCode.PM_DOT_IS_NOT_ALLOWED, instance( "inst2" )));
		errors.add( new RoboconfError( ErrorCode.PM_DOT_IS_NOT_ALLOWED, instance( "inst1" ), instance( "inst2" )));
		errors.add( new RoboconfError( ErrorCode.PM_DOT_IS_NOT_ALLOWED, component( "comp" )));
		errors.add( new ModelError( ErrorCode.PM_DOT_IS_NOT_ALLOWED, null, component( "comp" )));

		// sort them
		Set<RoboconfError> sortedErrors = new TreeSet<>( new RoboconfErrorComparator());
		sortedErrors.addAll( errors );

		// Verify we did not lost any of them
		List<RoboconfError> sortedErrorsAsList = new ArrayList<>( sortedErrors );
		Assert.assertEquals( errors.size(), sortedErrors.size());
		Assert.assertEquals( errors.size(), sortedErrorsAsList.size());

		// Verify they are sorted correctly:
		// First criteria: error code
		// Second criteria: the error details
		// Third criteria: based on class name
		Assert.assertEquals( errors.get( 2 ), sortedErrorsAsList.get( 0 ));
		Assert.assertEquals( errors.get( 6 ), sortedErrorsAsList.get( 1 ));

		// ModelError comes RoboconfError (package name) but same details and same error code
		Assert.assertEquals( errors.get( 7 ), sortedErrorsAsList.get( 2 ));

		// Same error code than previously but different details
		Assert.assertEquals( errors.get( 1 ), sortedErrorsAsList.get( 3 ));
		Assert.assertEquals( errors.get( 5 ), sortedErrorsAsList.get( 4 ));
		Assert.assertEquals( errors.get( 4 ), sortedErrorsAsList.get( 5 ));

		// Last error codes
		Assert.assertEquals( errors.get( 3 ), sortedErrorsAsList.get( 6 ));
		Assert.assertEquals( errors.get( 0 ), sortedErrorsAsList.get( 7 ));
	}
}
