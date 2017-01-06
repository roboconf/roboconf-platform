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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.helpers.InstanceHelpers;

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

		HashSet<Application> set = new HashSet<>( 2 );
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

		HashSet<Application> set = new HashSet<>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 1, set.size());
	}


	@Test
	public void testEqualsAndHashCode_3() {

		Application app1 = new Application(  new TestApplicationTemplate());
		Application app2 = new Application( "app", app1.getTemplate());

		HashSet<Application> set = new HashSet<>( 2 );
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
	public void testAssociations() {

		ApplicationTemplate tpl = new ApplicationTemplate();
		Assert.assertEquals( 0, tpl.getAssociatedApplications().size());

		Application app1 = new Application( "1", tpl );
		Assert.assertEquals( 1, tpl.getAssociatedApplications().size());

		Application app2 = new Application( "2", tpl );
		Assert.assertEquals( 2, tpl.getAssociatedApplications().size());

		app1.removeAssociationWithTemplate();
		Assert.assertEquals( 1, tpl.getAssociatedApplications().size());

		app2.removeAssociationWithTemplate();
		Assert.assertEquals( 0, tpl.getAssociatedApplications().size());

		// Limits
		app1.removeAssociationWithTemplate();

		Application app3 = new Application( null );
		app3.removeAssociationWithTemplate();
		// No exception
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


	@Test
	public void testExternalExports() {

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		Application app = new Application( tpl );
		Assert.assertEquals( app.getExternalExports(), tpl.externalExports );

		tpl.externalExports.put( "something", "here" );
		Assert.assertEquals( app.getExternalExports(), tpl.externalExports );
	}


	@Test
	public void testApplicationBindings() {

		TestApplication app = new TestApplication();
		Assert.assertEquals( 0, app.getApplicationBindings().size());

		app.bindWithApplication( "p1", "app1" );
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app1" ));

		app.bindWithApplication( "p2", "app1" );
		Assert.assertEquals( 2, app.getApplicationBindings().size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p2" ).size());

		app.bindWithApplication( "p1", "app1" );	// idempotent
		Assert.assertEquals( 2, app.getApplicationBindings().size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p2" ).size());

		app.bindWithApplication( "p1", "app2" );
		Assert.assertEquals( 2, app.getApplicationBindings().size());
		Assert.assertEquals( 2, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app1" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app2" ));
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p2" ).size());

		Assert.assertTrue( app.unbindFromApplication( "p1", "app1" ));
		Assert.assertEquals( 2, app.getApplicationBindings().size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app2" ));
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p2" ).size());

		Assert.assertFalse( app.unbindFromApplication( "p1", "app1" ));

		Assert.assertTrue( app.unbindFromApplication( "p1", "app2" ));
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p2" ).size());

		Assert.assertFalse( app.unbindFromApplication( "inexisting", "app2" ));
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p2" ).size());

		Assert.assertFalse( app.unbindFromApplication( "p2", "inexisting" ));
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p2" ).size());
	}


	@Test
	public void testReplaceApplicationBindings() {

		TestApplication app = new TestApplication();
		Assert.assertEquals( 0, app.getApplicationBindings().size());

		app.bindWithApplication( "p1", "app1" );
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertEquals( 1, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app1" ));

		Assert.assertTrue( app.replaceApplicationBindings( "p1", new HashSet<>( Arrays.asList( "app1", "app2", "app3" ))));
		Assert.assertEquals( 3, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertFalse( app.replaceApplicationBindings( "p1", new HashSet<>( Arrays.asList( "app1", "app2", "app3" ))));
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertEquals( 3, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app1" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app2" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app3" ));

		Assert.assertTrue( app.replaceApplicationBindings( "p1", new HashSet<>( Arrays.asList( "app2", "app3" ))));
		Assert.assertFalse( app.replaceApplicationBindings( "p1", new HashSet<>( Arrays.asList( "app2", "app3" ))));
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertEquals( 2, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertFalse( app.getApplicationBindings().get( "p1" ).contains( "app1" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app2" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app3" ));

		Assert.assertTrue( app.replaceApplicationBindings( "p2", new HashSet<>( Arrays.asList( "app1", "app2", "app3" ))));
		Assert.assertFalse( app.replaceApplicationBindings( "p2", new HashSet<>( Arrays.asList( "app1", "app2", "app3" ))));
		Assert.assertEquals( 2, app.getApplicationBindings().size());
		Assert.assertEquals( 2, app.getApplicationBindings().get( "p1" ).size());
		Assert.assertFalse( app.getApplicationBindings().get( "p1" ).contains( "app1" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app2" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p1" ).contains( "app3" ));
		Assert.assertEquals( 3, app.getApplicationBindings().get( "p2" ).size());
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app1" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app2" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app3" ));

		Assert.assertTrue( app.replaceApplicationBindings( "p1", new HashSet<String>( 0 )));
		Assert.assertFalse( app.replaceApplicationBindings( "p1", new HashSet<String>( 0 )));
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertNull( app.getApplicationBindings().get( "p1" ));
		Assert.assertEquals( 3, app.getApplicationBindings().get( "p2" ).size());
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app1" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app2" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app3" ));

		Assert.assertTrue( app.replaceApplicationBindings( "p2", new HashSet<>( Arrays.asList( "app4", "app5", "app6" ))));
		Assert.assertFalse( app.replaceApplicationBindings( "p2", new HashSet<>( Arrays.asList( "app4", "app5", "app6" ))));
		Assert.assertEquals( 1, app.getApplicationBindings().size());
		Assert.assertNull( app.getApplicationBindings().get( "p1" ));
		Assert.assertEquals( 3, app.getApplicationBindings().get( "p2" ).size());
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app4" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app5" ));
		Assert.assertTrue( app.getApplicationBindings().get( "p2" ).contains( "app6" ));
	}


	@Test
	public void testShortcuts() {

		Application app = new Application( null );
		Assert.assertNull( app.getGraphs());
		Assert.assertNotNull( app.getExternalExports());
		Assert.assertEquals( 0, app.getExternalExports().size());

		app = new TestApplication();
		Assert.assertNotNull( app.getGraphs());
		Assert.assertNotNull( app.getExternalExports());
	}


	@Test
	public void testSetNameWithAccents() {

		Application app = new Application( "avé dés àcçents", new TestApplicationTemplate());
		Assert.assertEquals( "ave des accents", app.getName());
		Assert.assertEquals( "avé dés àcçents", app.getDisplayName());

		app.setName( "   " );
		Assert.assertEquals( "", app.getName());
		Assert.assertEquals( "", app.getDisplayName());

		app.setName( null );
		Assert.assertNull( app.getName());
		Assert.assertNull( app.getDisplayName());

		app.setName( " âêû éèà " );
		Assert.assertEquals( "aeu eea", app.getName());
		Assert.assertEquals( "âêû éèà", app.getDisplayName());
	}
}
