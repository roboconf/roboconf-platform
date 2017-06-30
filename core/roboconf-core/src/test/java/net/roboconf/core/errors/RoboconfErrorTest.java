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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfErrorTest {

	@Test
	public void testEquals() {

		RoboconfError def1 = new RoboconfError( ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT );
		RoboconfError def2 = new RoboconfError( ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT, component( "details" ));
		RoboconfError def3 = new RoboconfError( ErrorCode.CMD_CONFLICTING_INSTANCE_NAME, component( "details" ));

		Assert.assertTrue( def1.equals( def1 ));
		Assert.assertTrue( def1.equals( new RoboconfError( ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT )));
		Assert.assertFalse( def1.equals( null ));
		Assert.assertFalse( def1.equals( new Object()));
		Assert.assertFalse( def1.equals( def2 ));
		Assert.assertFalse( def1.equals( def3 ));

		Assert.assertTrue( def2.equals( def2 ));
		Assert.assertTrue( def2.equals( new RoboconfError( ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT, component( "details" ))));
		Assert.assertFalse( def2.equals( def3 ));
		Assert.assertFalse( def2.equals( def1 ));

		RoboconfError def4 = new RoboconfError( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, component( "vm" ));
		RoboconfError def5 = new RoboconfError( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, component( "server" ));
		Assert.assertFalse( def4.equals( def5 ));
		Assert.assertFalse( def5.equals( def4 ));
	}


	@Test
	public void testHashCode() {

		Assert.assertTrue( new RoboconfError( null ).hashCode() > 0 );
		Assert.assertTrue( new RoboconfError( ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT ).hashCode() > 0 );
	}
}
