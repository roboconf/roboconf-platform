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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationTemplateTest {

	@Test
	public void testEqualsAndHashCode_1() {

		ApplicationTemplate app1 = new ApplicationTemplate();
		app1.setName( "app" );
		app1.setVersion( "snapshot" );

		ApplicationTemplate app2 = new ApplicationTemplate();
		app2.setName( "app" );
		app2.setVersion( "snapshot" );

		HashSet<ApplicationTemplate> set = new HashSet<>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 1, set.size());
	}


	@Test
	public void testEqualsAndHashCode_2() {

		ApplicationTemplate app1 = new ApplicationTemplate();
		app1.setName( "app" );

		ApplicationTemplate app2 = new ApplicationTemplate();
		app2.setName( "app" );

		HashSet<ApplicationTemplate> set = new HashSet<>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 1, set.size());
	}


	@Test
	public void testEqualsAndHashCode_3() {

		ApplicationTemplate app1 = new ApplicationTemplate();
		ApplicationTemplate app2 = new ApplicationTemplate( "app" ).version( "whatever" );

		HashSet<ApplicationTemplate> set = new HashSet<>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 2, set.size());
	}


	@Test
	public void testEqualsAndHashCode_4() {

		ApplicationTemplate app1 = new ApplicationTemplate( "app" ).version( "v1" );
		ApplicationTemplate app2 = new ApplicationTemplate( "app" ).version( "v3" );

		HashSet<ApplicationTemplate> set = new HashSet<>( 2 );
		set.add( app1 );
		set.add( app2 );
		Assert.assertEquals( 2, set.size());
	}


	@Test
	public void testEquals() {

		ApplicationTemplate app = new ApplicationTemplate( "app" );
		Assert.assertFalse( app.equals( null ));
		Assert.assertFalse( app.equals( new ApplicationTemplate()));
		Assert.assertFalse( app.equals( new ApplicationTemplate( "app" ).version( "something" )));
		Assert.assertFalse( app.equals( new Object()));

		Assert.assertEquals( app, app );
		Assert.assertEquals( app, new ApplicationTemplate( "app" ));
	}


	@Test
	public void testChain() {

		ApplicationTemplate app = new ApplicationTemplate().name( "ins" ).description( "desc" ).version( "snapshot" ).graphs( new Graphs());
		Assert.assertEquals( "ins", app.getName());
		Assert.assertEquals( "desc", app.getDescription());
		Assert.assertEquals( "snapshot", app.getVersion());
		Assert.assertNotNull( app.getGraphs());
	}


	@Test
	public void testSetNameWithAccents() {

		ApplicationTemplate app = new ApplicationTemplate( "avé dés àcçents" );
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


	@Test
	public void testTags() {

		ApplicationTemplate app = new ApplicationTemplate();
		Assert.assertEquals( 0, app.getTags().size());

		app.addTag( "toto" );
		Assert.assertEquals( 1, app.getTags().size());
		Assert.assertEquals( "toto", app.getTags().iterator().next());

		Set<String> newTags = new HashSet<> ();
		newTags.add( "titi" );
		newTags.add( "tutu" );
		app.setTags( newTags );
		Assert.assertEquals( newTags, app.getTags());
	}
}
