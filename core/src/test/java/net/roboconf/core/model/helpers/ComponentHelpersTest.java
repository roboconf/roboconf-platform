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

package net.roboconf.core.model.helpers;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.impl.ComponentImpl;
import net.roboconf.core.model.runtime.impl.GraphsImpl;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ComponentHelpersTest {

	@Test
	public void testFindComponent() {

		GraphsImpl g = new GraphsImpl();
		Assert.assertNull( ComponentHelpers.findComponent( g, "c" ));

		ComponentImpl c1 = new ComponentImpl( "c1" );
		g.getRootComponents().add( c1 );
		Assert.assertEquals( c1, ComponentHelpers.findComponent( g, "c1" ));

		ComponentImpl c2 = new ComponentImpl( "c2" );
		g.getRootComponents().add( c2 );
		Assert.assertEquals( c2, ComponentHelpers.findComponent( g, "c2" ));

		ComponentImpl c21 = new ComponentImpl( "c21" );
		ComponentHelpers.insertChild( c2, c21 );
		Assert.assertEquals( c21, ComponentHelpers.findComponent( g, "c21" ));

		ComponentImpl duplicateC1 = new ComponentImpl( "c1" );
		g.getRootComponents().add( duplicateC1 );
		Assert.assertNotNull( ComponentHelpers.findComponent( g, "c1" ));
	}


	@Test
	public void testInsertChild() {

		ComponentImpl component_1 = new ComponentImpl( "comp 1" );
		ComponentImpl component_1_1 = new ComponentImpl( "comp 11" );

		Assert.assertEquals( 0, component_1.getAncestors().size());
		Assert.assertEquals( 0, component_1.getChildren().size());
		Assert.assertEquals( 0, component_1_1.getAncestors().size());
		Assert.assertEquals( 0, component_1_1.getChildren().size());
		ComponentHelpers.insertChild( component_1, component_1_1 );

		Assert.assertEquals( 0, component_1.getAncestors().size());
		Assert.assertEquals( 1, component_1.getChildren().size());
		Assert.assertEquals( 1, component_1_1.getAncestors().size());
		Assert.assertEquals( 0, component_1_1.getChildren().size());

		Assert.assertEquals( component_1_1, component_1.getChildren().iterator().next());
		Assert.assertEquals( component_1, component_1_1.getAncestors().iterator().next());
		Assert.assertTrue( component_1.getChildren().contains( component_1_1 ));
		Assert.assertTrue( component_1_1.getAncestors().contains( component_1 ));
		Assert.assertNotSame( component_1, component_1_1 );
	}


	@Test
	public void testSearchForLoop() {

		ComponentImpl c1 = new ComponentImpl( "c1" );
		Assert.assertNull( ComponentHelpers.searchForLoop( c1 ));

		ComponentImpl c11 = new ComponentImpl( "c11" );
		ComponentHelpers.insertChild( c1, c11 );
		Assert.assertNull( ComponentHelpers.searchForLoop( c1 ));

		ComponentImpl c12 = new ComponentImpl( "c1" );
		ComponentHelpers.insertChild( c1, c12 );
		Assert.assertEquals( "c1 -> c1", ComponentHelpers.searchForLoop( c1 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c11 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c12 ));

		c12.setName( "c12" );
		ComponentImpl c121 = new ComponentImpl( "c1" );
		ComponentHelpers.insertChild( c12, c121 );
		Assert.assertEquals( "c1 -> c12 -> c1", ComponentHelpers.searchForLoop( c1 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c11 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c12 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c121 ));

		c121.setName( "c121" );
		ComponentHelpers.insertChild( c121, c1 );
		Assert.assertEquals( "c1 -> c12 -> c121 -> c1", ComponentHelpers.searchForLoop( c1 ));
		Assert.assertEquals( "c12 -> c121 -> c1 -> c12", ComponentHelpers.searchForLoop( c12 ));
		Assert.assertEquals( "c121 -> c1 -> c12 -> c121", ComponentHelpers.searchForLoop( c121 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c11 ));
	}
}
