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
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ComponentHelpersTest {

	@Test
	public void testFindComponent() {

		Graphs g = new Graphs();
		Assert.assertNull( ComponentHelpers.findComponent( g, "c" ));

		Component c1 = new Component( "c1" );
		g.getRootComponents().add( c1 );
		Assert.assertEquals( c1, ComponentHelpers.findComponent( g, "c1" ));

		Component c2 = new Component( "c2" );
		g.getRootComponents().add( c2 );
		Assert.assertEquals( c2, ComponentHelpers.findComponent( g, "c2" ));

		Component c21 = new Component( "c21" );
		ComponentHelpers.insertChild( c2, c21 );
		Assert.assertEquals( c21, ComponentHelpers.findComponent( g, "c21" ));

		Component duplicateC1 = new Component( "c1" );
		g.getRootComponents().add( duplicateC1 );
		Assert.assertNotNull( ComponentHelpers.findComponent( g, "c1" ));
	}


	@Test
	public void testFindSubComponent() {

		TestApplication app = new TestApplication();
		Component comp = app.getTomcatVm().getComponent();

		Assert.assertNull( ComponentHelpers.findSubComponent( comp, "inexisting" ));
		Assert.assertNull( ComponentHelpers.findSubComponent( null, "inexisting" ));
		Assert.assertEquals( comp, ComponentHelpers.findSubComponent( comp, comp.getName()));

		Component targetComp = app.getTomcat().getComponent();
		Assert.assertEquals( targetComp, ComponentHelpers.findSubComponent( comp, targetComp.getName()));

		targetComp = app.getWar().getComponent();
		Assert.assertEquals( targetComp, ComponentHelpers.findSubComponent( comp, targetComp.getName()));
	}


	@Test
	public void testInsertChild() {

		Component component_1 = new Component( "comp 1" );
		Component component_1_1 = new Component( "comp 11" );

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

		Component c1 = new Component( "c1" );
		Assert.assertNull( ComponentHelpers.searchForLoop( c1 ));

		Component c11 = new Component( "c11" );
		ComponentHelpers.insertChild( c1, c11 );
		Assert.assertNull( ComponentHelpers.searchForLoop( c1 ));

		Component c12 = new Component( "c1" );
		ComponentHelpers.insertChild( c1, c12 );
		Assert.assertEquals( "c1 -> c1", ComponentHelpers.searchForLoop( c1 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c11 ));
		Assert.assertNull( ComponentHelpers.searchForLoop( c12 ));

		c12.setName( "c12" );
		Component c121 = new Component( "c1" );
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


	@Test
	public void testFindAllComponents() {

		Application app = new Application();
		Assert.assertEquals( 0, ComponentHelpers.findAllComponents( app ).size());

		Graphs graphs = new Graphs();
		app.setGraphs( graphs );
		Component comp1 = new Component( "comp1" );
		graphs.getRootComponents().add( comp1 );
		Assert.assertEquals( 1, ComponentHelpers.findAllComponents( app ).size());

		ComponentHelpers.insertChild( comp1, new Component( "comp-2" ));
		Assert.assertEquals( 2, ComponentHelpers.findAllComponents( app ).size());

		Component comp3 = new Component( "comp_3" );
		graphs.getRootComponents().add( comp3 );
		ComponentHelpers.insertChild( comp3, new Component( "comp-2" ));
		Assert.assertEquals( 3, ComponentHelpers.findAllComponents( app ).size());
	}
}
