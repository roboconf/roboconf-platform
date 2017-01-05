/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.beans;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ImportTest {

	@Test
	public void testToString() {

		Map<String,String> exportedVars = null;
		Import imp = new Import( "/root/node", "comp1", exportedVars );
		Assert.assertNotNull( imp.toString());

		exportedVars = new HashMap<String,String> ();
		imp = new Import( "/root/node", "comp1", exportedVars );
		Assert.assertNotNull( imp.toString());

		exportedVars.put( "comp.ip", "127.0.0.1" );
		exportedVars.put( "comp.data", null );
		imp = new Import( "/root/node", "comp1", exportedVars );
		Assert.assertNotNull( imp.toString());

		imp = new Import( new Instance( "my VM" ));
		Assert.assertEquals( "/my VM", imp.getInstancePath());
	}


	@Test
	public void testHashCode() {

		Assert.assertTrue( new Import( "inst", "comp" ).hashCode() > 0 );
		Assert.assertTrue( new Import( new Instance()).hashCode() > 0 );
	}


	@Test
	public void testEquals() {

		Import imp = new Import( "inst", "comp" );
		Assert.assertFalse( imp.equals( null ));
		Assert.assertFalse( imp.equals( new Import( "inst2", "comp" )));

		Assert.assertEquals( imp, imp );
		Assert.assertEquals( imp, new Import( "inst", "comp" ));
	}
}
