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

package net.roboconf.core;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class VersionTest {

	@Test
	public void testParseVersion() {

		Assert.assertNull( Version.parseVersion( "v1" ));
		Assert.assertNull( Version.parseVersion( "3.1." ));
		Assert.assertNull( Version.parseVersion( "3.t.4" ));
		Assert.assertNull( Version.parseVersion( "3-1-4" ));

		Version v = Version.parseVersion( "5.10.9" );
		Assert.assertEquals( 5, v.getMajor());
		Assert.assertEquals( 10, v.getMinor());
		Assert.assertEquals( 9, v.getPatch());
		Assert.assertNull( v.getQualifier());

		v = Version.parseVersion( "71.1.0.snapshot" );
		Assert.assertEquals( 71, v.getMajor());
		Assert.assertEquals( 1, v.getMinor());
		Assert.assertEquals( 0, v.getPatch());
		Assert.assertEquals( "snapshot", v.getQualifier());

		v = Version.parseVersion( "4.7.84-SNAPSHOT" );
		Assert.assertEquals( 4, v.getMajor());
		Assert.assertEquals( 7, v.getMinor());
		Assert.assertEquals( 84, v.getPatch());
		Assert.assertEquals( "SNAPSHOT", v.getQualifier());

		v = Version.parseVersion( "3.11" );
		Assert.assertEquals( 3, v.getMajor());
		Assert.assertEquals( 11, v.getMinor());
		Assert.assertEquals( 0, v.getPatch());
		Assert.assertNull( v.getQualifier());

		v = Version.parseVersion( "8.4-SNAPSHOT" );
		Assert.assertEquals( 8, v.getMajor());
		Assert.assertEquals( 4, v.getMinor());
		Assert.assertEquals( 0, v.getPatch());
		Assert.assertEquals( "SNAPSHOT", v.getQualifier());

		v = Version.parseVersion( "4.7.84.SNAP-SHOT" );
		Assert.assertEquals( 4, v.getMajor());
		Assert.assertEquals( 7, v.getMinor());
		Assert.assertEquals( 84, v.getPatch());
		Assert.assertEquals( "SNAP-SHOT", v.getQualifier());

		v = Version.parseVersion( "4.7.84.SNAP.SHOT" );
		Assert.assertEquals( 4, v.getMajor());
		Assert.assertEquals( 7, v.getMinor());
		Assert.assertEquals( 84, v.getPatch());
		Assert.assertEquals( "SNAP.SHOT", v.getQualifier());

		v = Version.parseVersion( "4.7.SNAP.SHOT" );
		Assert.assertEquals( 4, v.getMajor());
		Assert.assertEquals( 7, v.getMinor());
		Assert.assertEquals( 0, v.getPatch());
		Assert.assertEquals( "SNAP.SHOT", v.getQualifier());
	}
}
