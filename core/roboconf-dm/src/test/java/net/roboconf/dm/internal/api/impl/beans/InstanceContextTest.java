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

package net.roboconf.dm.internal.api.impl.beans;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceContextTest {

	@Test
	public void testConstructors() {

		ApplicationTemplate tpl = new ApplicationTemplate( "hop" );

		InstanceContext key = new InstanceContext( tpl );
		Assert.assertEquals( "hop", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePathOrComponentName());

		key = new InstanceContext( tpl, "oops" );
		Assert.assertEquals( "hop", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertEquals( "oops", key.getInstancePathOrComponentName());

		tpl.setVersion( "v1" );
		key = new InstanceContext( tpl, "oops" );
		Assert.assertEquals( "hop", key.getName());
		Assert.assertEquals( "v1", key.getQualifier());
		Assert.assertEquals( "oops", key.getInstancePathOrComponentName());

		Application app = new Application( tpl );
		key = new InstanceContext( app );
		Assert.assertNull( key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePathOrComponentName());

		app.setName( "ok" );
		key = new InstanceContext( app );
		Assert.assertEquals( "ok", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePathOrComponentName());

		key = new InstanceContext( app, new Instance( "inst" ));
		Assert.assertEquals( "ok", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertEquals( "/inst", key.getInstancePathOrComponentName());

		key = new InstanceContext( "1", "2", "3" );
		Assert.assertEquals( "1", key.getName());
		Assert.assertEquals( "2", key.getQualifier());
		Assert.assertEquals( "3", key.getInstancePathOrComponentName());
	}


	@Test
	public void testToStringAndParseAndEquals() {

		InstanceContext key = InstanceContext.parse( "" );
		Assert.assertNull( key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePathOrComponentName());

		key = InstanceContext.parse( null );
		Assert.assertNull( key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePathOrComponentName());

		InstanceContext newKey = new InstanceContext( "name", "qualifier", "inst" );
		key = InstanceContext.parse( newKey.toString());
		Assert.assertEquals( "name", key.getName());
		Assert.assertEquals( "qualifier", key.getQualifier());
		Assert.assertEquals( "inst", key.getInstancePathOrComponentName());
		Assert.assertEquals( newKey, key );
		Assert.assertEquals( newKey.hashCode(), key.hashCode());

		newKey = new InstanceContext( "name2", null, null );
		key = InstanceContext.parse( newKey.toString());
		Assert.assertEquals( "name2", key.getName());
		Assert.assertNull( key.getQualifier());
		Assert.assertNull( key.getInstancePathOrComponentName());
		Assert.assertEquals( newKey, key );
		Assert.assertEquals( newKey.hashCode(), key.hashCode());
	}
}
