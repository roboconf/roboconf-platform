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

import org.junit.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ComponentTest {

	@Test
	public void testChain() {

		Component c = new Component( "c" );
		Component comp = new Component().name( "ins" ).installerName( "my-installer" );
		comp.extendComponent( c );

		Assert.assertEquals( "ins", comp.getName());
		Assert.assertEquals( "my-installer", comp.getInstallerName());
		Assert.assertEquals( c, comp.getExtendedComponent());
	}


	@Test
	public void testHashCode() {

		Component comp = new Component();
		Assert.assertTrue( comp.hashCode() > 0 );

		comp.setName( "comp" );
		Assert.assertTrue( comp.hashCode() > 0 );
	}


	@Test
	public void testEquals() {

		Component comp = new Component( "comp" );
		Assert.assertFalse( comp.equals( null ));
		Assert.assertFalse( comp.equals( new Component( "comp2" )));
		Assert.assertFalse( comp.equals( new Facet( "comp" )));

		Assert.assertEquals( comp, comp );
		Assert.assertEquals( comp, new Component( "comp" ));
	}


	@Test
	public void testInsertChild() {

		Component component_1 = new Component( "comp 1" );
		Component component_1_1 = new Component( "comp 11" );

		Assert.assertEquals( 0, component_1.getAncestors().size());
		Assert.assertEquals( 0, component_1.getChildren().size());

		Assert.assertEquals( 0, component_1_1.getAncestors().size());
		Assert.assertEquals( 0, component_1_1.getChildren().size());

		component_1.addChild( component_1_1 );
		Assert.assertEquals( 0, component_1.getAncestors().size());
		Assert.assertEquals( 1, component_1.getChildren().size());

		Assert.assertEquals( 1, component_1_1.getAncestors().size());
		Assert.assertEquals( 0, component_1_1.getChildren().size());

		Assert.assertEquals( component_1_1, component_1.getChildren().iterator().next());
		Assert.assertEquals( component_1, component_1_1.getAncestors().iterator().next());
		Assert.assertNotSame( component_1, component_1_1 );
	}


	@Test
	public void testAssociateFacet() {

		Component component = new Component( "comp" );
		Facet facet = new Facet( "facet" );

		Assert.assertEquals( 0, component.getFacets().size());
		Assert.assertEquals( 0, facet.getAssociatedComponents().size());

		component.associateFacet( facet );
		Assert.assertEquals( 1, component.getFacets().size());
		Assert.assertEquals( 1, facet.getAssociatedComponents().size());

		Assert.assertEquals( facet, component.getFacets().iterator().next());
		Assert.assertEquals( component, facet.getAssociatedComponents().iterator().next());
	}
}
