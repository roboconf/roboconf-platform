/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.helpers.InstanceHelpers;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTest {

	@Test
	public void testEqualsAndHashCode_1() {

		Application app1 = new Application( new TestApplicationTemplate());
		app1.setName( "app" );

		Application app2 = new Application( new ApplicationTemplate());
		app2.setName( "app" );

		HashSet<Application> set = new HashSet<Application>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 1, set.size());
	}


	@Test
	public void testEqualsAndHashCode_2() {

		Application app1 = new Application( new TestApplicationTemplate());
		app1.setName( "app" );

		Application app2 = new Application( app1.getTemplate());
		app2.setName( "app" );

		HashSet<Application> set = new HashSet<Application>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 1, set.size());
	}


	@Test
	public void testEqualsAndHashCode_3() {

		Application app1 = new Application(  new TestApplicationTemplate());
		Application app2 = new Application( "app", app1.getTemplate());

		HashSet<Application> set = new HashSet<Application>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 2, set.size());
	}


	@Test
	public void testEquals() {

		Application app = new Application( "app", new ApplicationTemplate());
		Assert.assertFalse( app.equals( null ));
		Assert.assertFalse( app.equals( new Application( app.getTemplate())));
		Assert.assertFalse( app.equals( new Object()));

		Assert.assertEquals( app, app );
		Assert.assertEquals( app, new Application( "app", app.getTemplate()));
		Assert.assertEquals( app, new Application( "app", new ApplicationTemplate( "whatever" )));
	}


	@Test
	public void testChain() {

		Application app = new Application( new ApplicationTemplate()).name( "ins" ).description( "desc" );
		Assert.assertEquals( "ins", app.getName());
		Assert.assertEquals( "desc", app.getDescription());
	}


	@Test
	public void checkInstanceReplication() {

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		Application app = new Application( tpl );

		Assert.assertEquals( 5, InstanceHelpers.getAllInstances( app ).size());
		Assert.assertEquals( 5, InstanceHelpers.getAllInstances( tpl ).size());

		for( Instance inst : InstanceHelpers.getAllInstances( tpl )) {

			String instancePath = InstanceHelpers.computeInstancePath( inst );
			Instance copiedInstance = InstanceHelpers.findInstanceByPath( app, instancePath );
			Assert.assertNotNull( copiedInstance );

			Assert.assertEquals( inst.getName(), copiedInstance.getName());
			Assert.assertEquals( inst.getComponent(), copiedInstance.getComponent());
			Assert.assertEquals( inst.getImports(), copiedInstance.getImports());
			Assert.assertEquals( inst.getParent(), copiedInstance.getParent());
			Assert.assertEquals( inst.getChildren().size(), copiedInstance.getChildren().size());

			// Paths are the same, so the children are equal (even if they are not the same object)
			Assert.assertEquals( inst.getChildren(), copiedInstance.getChildren());
			Assert.assertEquals( inst.channels, copiedInstance.channels );
			Assert.assertEquals( inst.overriddenExports, copiedInstance.overriddenExports );
			Assert.assertEquals( inst.data, copiedInstance.data );

			Assert.assertFalse( inst == copiedInstance );
		}
	}
}
