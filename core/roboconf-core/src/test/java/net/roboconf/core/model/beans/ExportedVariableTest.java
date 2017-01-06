/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.util.HashSet;

import org.junit.Assert;
import net.roboconf.core.model.beans.ExportedVariable.RandomKind;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ExportedVariableTest {

	@Test
	public void testEqualsAndHashCode() {

		ExportedVariable var1 = new ExportedVariable( "test", "value 1" );
		ExportedVariable var2 = new ExportedVariable( "test", "value 2" );

		HashSet<ExportedVariable> set = new HashSet<>( 2 );
		set.add( var1 );
		set.add( var2 );
		Assert.assertEquals( 1, set.size());

		Assert.assertEquals( var1, var2 );
		Assert.assertFalse( var1.equals( new Object()));
		Assert.assertFalse( var1.equals( new ExportedVariable( "test2", "value 1" )));

		Assert.assertTrue( new ExportedVariable().hashCode() > 0 );
	}


	@Test
	public void testToString() {
		ExportedVariable var = new ExportedVariable( "test", null );
		Assert.assertEquals( "test", var.toString());
	}


	@Test
	public void tetstRandomKind_whichValue() {

		Assert.assertNull( RandomKind.whichValue( null ));
		Assert.assertNull( RandomKind.whichValue( "" ));

		Assert.assertEquals( RandomKind.PORT, RandomKind.whichValue( "port" ));
		Assert.assertEquals( RandomKind.PORT, RandomKind.whichValue( "pOrt" ));
	}
}
