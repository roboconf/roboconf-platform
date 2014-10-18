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

package net.roboconf.messaging.internal;

import junit.framework.Assert;
import net.roboconf.messaging.internal.RabbitMqTestUtils;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqTestUtilsTest {

	@Test
	public void testIsVersionGreaterThanThreeDotTwo() {

		Assert.assertTrue( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "3.2" ));
		Assert.assertTrue( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "3.2.1" ));
		Assert.assertTrue( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "3.3" ));
		Assert.assertTrue( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "4.2" ));

		Assert.assertFalse( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "3.1" ));
		Assert.assertFalse( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "3.1.3" ));
		Assert.assertFalse( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "3.0" ));
		Assert.assertFalse( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "2.1" ));

		Assert.assertFalse( RabbitMqTestUtils.isVersionGOEThreeDotTwo( "whatever" ));
	}
}
