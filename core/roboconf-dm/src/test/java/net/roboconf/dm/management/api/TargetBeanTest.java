/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.management.api;

import junit.framework.Assert;
import net.roboconf.dm.management.api.ITargetsMngr.TargetBean;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetBeanTest {

	@Test
	public void testEquals() {

		TargetBean b1 = new TargetBean();
		Assert.assertTrue( b1.hashCode() > 0 );

		b1.id = "test";
		TargetBean b2 = new TargetBean();
		b2.id = "test";

		TargetBean b3 = new TargetBean();
		b3.id = "other";

		Assert.assertEquals( b1, b2 );
		Assert.assertFalse( b1.equals( b3 ));
		Assert.assertFalse( b1.equals( new Object()));

		Assert.assertEquals( b1.hashCode(), b2.hashCode());
		Assert.assertTrue( b1.hashCode() != b3.hashCode());
	}
}
