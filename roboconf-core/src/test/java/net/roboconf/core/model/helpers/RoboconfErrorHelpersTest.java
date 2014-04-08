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

package net.roboconf.core.model.helpers;

import java.util.ArrayList;
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

		errors.add( new RoboconfError( ErrorCode.PM_INVALID_INDEX_REFERENCE_USE ));
		Assert.assertTrue( RoboconfErrorHelpers.containsCriticalErrors( errors ));
	}
}
