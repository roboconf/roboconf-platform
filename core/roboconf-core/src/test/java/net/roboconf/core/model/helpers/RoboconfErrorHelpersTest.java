/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfErrorHelpersTest {

	@Test
	public void testContainsCriticalErrors() {

		List<RoboconfError> errors = new ArrayList<RoboconfError> ();
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( errors ));

		errors.add( new RoboconfError( ErrorCode.PM_MALFORMED_COMMENT ));
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( errors ));

		errors.add( new RoboconfError( ErrorCode.PM_DUPLICATE_PROPERTY ));
		Assert.assertTrue( RoboconfErrorHelpers.containsCriticalErrors( errors ));
	}


	@Test
	public void testFindWarnings() {

		Collection<RoboconfError> errors = new ArrayList<RoboconfError> ();
		Assert.assertEquals( 0, RoboconfErrorHelpers.findWarnings( errors ).size());

		errors.add( new RoboconfError( ErrorCode.PM_DUPLICATE_PROPERTY ));
		Assert.assertEquals( 0, RoboconfErrorHelpers.findWarnings( errors ).size());

		errors.add( new RoboconfError( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY ));
		errors = RoboconfErrorHelpers.findWarnings( errors );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.iterator().next().getErrorCode());
	}
}
