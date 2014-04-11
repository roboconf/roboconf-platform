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

package net.roboconf.core.model.runtime;

import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTest {

	@Test
	public void testEqualsAndHashCode_1() {

		Application app1 = new Application();
		app1.setName( "app" );
		app1.setQualifier( "snapshot" );

		Application app2 = new Application();
		app2.setName( "app" );
		app2.setQualifier( "snapshot" );

		HashSet<Application> set = new HashSet<Application>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 1, set.size());
	}


	@Test
	public void testEqualsAndHashCode_2() {

		Application app1 = new Application();
		app1.setName( "app" );

		Application app2 = new Application();
		app2.setName( "app" );

		HashSet<Application> set = new HashSet<Application>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 1, set.size());
	}
}
