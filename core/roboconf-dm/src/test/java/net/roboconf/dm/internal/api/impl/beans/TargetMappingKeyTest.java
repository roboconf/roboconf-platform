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

package net.roboconf.dm.internal.api.impl.beans;

import junit.framework.Assert;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetMappingKeyTest {

	@Test
	public void testConstructors() {

		ApplicationTemplate tpl = new ApplicationTemplate( "hop" );

		TargetMappingKey key = new TargetMappingKey( tpl );
		Assert.assertEquals( "hop", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePath());

		key = new TargetMappingKey( tpl, "oops" );
		Assert.assertEquals( "hop", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertEquals( "oops", key.getInstancePath());

		tpl.setQualifier( "v1" );
		key = new TargetMappingKey( tpl, "oops" );
		Assert.assertEquals( "hop", key.getName());
		Assert.assertEquals( "v1", key.getQualifier());
		Assert.assertEquals( "oops", key.getInstancePath());

		Application app = new Application( tpl );
		key = new TargetMappingKey( app );
		Assert.assertNull( key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePath());

		app.setName( "ok" );
		key = new TargetMappingKey( app );
		Assert.assertEquals( "ok", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePath());

		key = new TargetMappingKey( app, new Instance( "inst" ));
		Assert.assertEquals( "ok", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertEquals( "/inst", key.getInstancePath());

		key = new TargetMappingKey( "1", "2", "3" );
		Assert.assertEquals( "1", key.getName());
		Assert.assertEquals( "2", key.getQualifier());
		Assert.assertEquals( "3", key.getInstancePath());
	}


	@Test
	public void testToStringAndParseAndEquals() {

		TargetMappingKey key = TargetMappingKey.parse( "" );
		Assert.assertNull( key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePath());

		key = TargetMappingKey.parse( null );
		Assert.assertNull( key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePath());

		TargetMappingKey newKey = new TargetMappingKey( "name", "qualifier", "inst" );
		key = TargetMappingKey.parse( newKey.toString());
		Assert.assertEquals( "name", key.getName());
		Assert.assertEquals( "qualifier", key.getQualifier());
		Assert.assertEquals( "inst", key.getInstancePath());
		Assert.assertEquals( newKey, key );
		Assert.assertEquals( newKey.hashCode(), key.hashCode());

		newKey = new TargetMappingKey( "name2", null, null );
		key = TargetMappingKey.parse( newKey.toString());
		Assert.assertEquals( "name2", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePath());
		Assert.assertEquals( newKey, key );
		Assert.assertEquals( newKey.hashCode(), key.hashCode());
	}
}
