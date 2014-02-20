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

package net.roboconf.dm.rest;

import junit.framework.Assert;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestUtilsTest {

	@Test
	public void testToRestfulPath_1() throws Exception {

		Assert.assertEquals( "", RestUtils.toRestfulPath( "" ));
		Assert.assertEquals( "|vm", RestUtils.toRestfulPath( "/vm" ));
		Assert.assertEquals( "|vm|server", RestUtils.toRestfulPath( "/vm/server" ));
		Assert.assertEquals( "|vm|server 1", RestUtils.toRestfulPath( "/vm/server 1" ));
	}


	@Test
	public void testToRestfulPath_2() throws Exception {

		Instance i1 = new Instance( "vm 1" );
		Instance i2 = new Instance( "tomcat" );
		Instance i3 = new Instance( "hello world" );

		InstanceHelpers.insertChild( i1, i2 );
		InstanceHelpers.insertChild( i2, i3 );

		Assert.assertEquals( "|vm 1", RestUtils.toRestfulPath( i1 ));
		Assert.assertEquals( "|vm 1|tomcat", RestUtils.toRestfulPath( i2 ));
		Assert.assertEquals( "|vm 1|tomcat|hello world", RestUtils.toRestfulPath( i3 ));
	}


	@Test
	public void testFindInstanceFromRestfulPath() throws Exception {

		Instance i1 = new Instance( "vm 1" );
		Instance i2 = new Instance( "tomcat" );
		Instance i3 = new Instance( "hello world" );

		InstanceHelpers.insertChild( i1, i2 );
		InstanceHelpers.insertChild( i2, i3 );

		Application app = new Application();
		app.getRootInstances().add( i1 );

		Assert.assertEquals( i1, RestUtils.findInstanceFromRestfulPath( app, "|vm 1" ));
		Assert.assertEquals( i2, RestUtils.findInstanceFromRestfulPath( app, "|vm 1|tomcat" ));
		Assert.assertEquals( i3, RestUtils.findInstanceFromRestfulPath( app, "|vm 1|tomcat|hello world" ));
		Assert.assertNull( RestUtils.findInstanceFromRestfulPath( app, "anything" ));
	}
}
