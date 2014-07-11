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

package net.roboconf.dm;

import java.util.Collection;

import junit.framework.Assert;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.validators.RuntimeModelValidator;
import net.roboconf.dm.internal.TestApplication;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class SetupTest {

	@Test
	public void testTestApplication() {

		Application app = new TestApplication();
		Collection<RoboconfError> errors = RuntimeModelValidator.validate( app );
		Assert.assertEquals( 0, errors.size());
	}
}
