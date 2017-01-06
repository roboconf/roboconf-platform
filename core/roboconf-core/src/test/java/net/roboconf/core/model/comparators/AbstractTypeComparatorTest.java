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

package net.roboconf.core.model.comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractTypeComparatorTest {

	@Test
	public void testComponentsSorting() {

		List<Component> components = new ArrayList<Component> ();
		components.add(  new Component( "z" ));
		components.add(  new Component( "m" ));
		components.add(  new Component( "a" ));
		components.add(  new Component( "b" ));

		Assert.assertEquals( "z", components.get( 0 ).getName());
		Collections.sort( components, new AbstractTypeComparator());

		Assert.assertEquals( "a", components.get( 0 ).getName());
		Assert.assertEquals( "b", components.get( 1 ).getName());
		Assert.assertEquals( "m", components.get( 2 ).getName());
		Assert.assertEquals( "z", components.get( 3 ).getName());
	}


	@Test
	public void testFacetsSorting() {

		List<Facet> facets = new ArrayList<Facet> ();
		facets.add(  new Facet( "z" ));
		facets.add(  new Facet( "m" ));
		facets.add(  new Facet( "a" ));
		facets.add(  new Facet( "b" ));

		Assert.assertEquals( "z", facets.get( 0 ).getName());
		Collections.sort( facets, new AbstractTypeComparator());

		Assert.assertEquals( "a", facets.get( 0 ).getName());
		Assert.assertEquals( "b", facets.get( 1 ).getName());
		Assert.assertEquals( "m", facets.get( 2 ).getName());
		Assert.assertEquals( "z", facets.get( 3 ).getName());
	}


	@Test
	public void testMixedSorting() {

		List<AbstractType> types = new ArrayList<AbstractType> ();
		types.add(  new Facet( "z" ));
		types.add(  new Component( "m" ));
		types.add(  new Component( "a" ));
		types.add(  new Facet( "b" ));

		Assert.assertEquals( "z", types.get( 0 ).getName());
		Collections.sort( types, new AbstractTypeComparator());

		Assert.assertEquals( "a", types.get( 0 ).getName());
		Assert.assertEquals( "b", types.get( 1 ).getName());
		Assert.assertEquals( "m", types.get( 2 ).getName());
		Assert.assertEquals( "z", types.get( 3 ).getName());
	}


	@Test
	public void testSortingWithNull() {

		List<AbstractType> types = new ArrayList<AbstractType> ();
		types.add(  new Facet( "z" ));
		types.add(  null );
		types.add(  new Component( "m" ));

		Assert.assertEquals( "z", types.get( 0 ).getName());
		Collections.sort( types, new AbstractTypeComparator());

		Assert.assertEquals( "m", types.get( 0 ).getName());
		Assert.assertEquals( "z", types.get( 1 ).getName());
		Assert.assertNull( types.get( 2 ));
	}
}
