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

package net.roboconf.core.model.comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceComparatorTest {

	@Test
	public void testRootInstances() {

		Instance i1 = new Instance( "root1" );
		Instance i2 = new Instance( "root2" );
		Instance i3 = new Instance( "1" );
		Instance i4 = new Instance( "_1" );
		Instance i5 = new Instance( "root11" );

		List<Instance> instances = new ArrayList<Instance> ();
		instances.add( i1 );
		instances.add( i2 );
		instances.add( i3 );
		instances.add( i4 );
		instances.add( i5 );

		Collections.sort( instances, new InstanceComparator());
		Assert.assertEquals( i3, instances.get( 0 ));
		Assert.assertEquals( i4, instances.get( 1 ));
		Assert.assertEquals( i1, instances.get( 2 ));
		Assert.assertEquals( i5, instances.get( 3 ));
		Assert.assertEquals( i2, instances.get( 4 ));
	}


	@Test
	public void testOneRootInstanceWithChildren() {

		Instance i1 = new Instance( "root1" );
		Instance i11 = new Instance( "child" );
		Instance i12 = new Instance( "child2" );
		Instance i2 = new Instance( "1" );
		Instance i3 = new Instance( "root11" );

		InstanceHelpers.insertChild( i1, i12 );
		InstanceHelpers.insertChild( i1, i11 );

		List<Instance> instances = new ArrayList<Instance> ();
		instances.add( i1 );
		instances.add( i2 );
		instances.add( i12 );
		instances.add( i3 );
		instances.add( i11 );

		Collections.sort( instances, new InstanceComparator());
		Assert.assertEquals( i2, instances.get( 0 ));
		Assert.assertEquals( i1, instances.get( 1 ));
		Assert.assertEquals( i11, instances.get( 2 ));
		Assert.assertEquals( i12, instances.get( 3 ));
		Assert.assertEquals( i3, instances.get( 4 ));
	}


	@Test
	public void testOnlyVmHavedifferentNames() {

		Instance i1 = new Instance( "vmec2tomcatrubis1" );
		Instance i2 = new Instance( "vmec2tomcatrubis2" );
		Instance i10 = new Instance( "vmec2tomcatrubis10" );
		Instance i11 = new Instance( "vmec2tomcatrubis11" );

		Instance i1_1 = new Instance( "tomcat" );
		Instance i2_1 = new Instance( "tomcat" );
		Instance i10_1 = new Instance( "tomcat" );
		Instance i11_1 = new Instance( "tomcat" );
		Instance i11_2 = new Instance( "tomcat2" );
		Instance i11_2_1 = new Instance( "app" );

		InstanceHelpers.insertChild( i1, i1_1 );
		InstanceHelpers.insertChild( i2, i2_1 );
		InstanceHelpers.insertChild( i10, i10_1 );
		InstanceHelpers.insertChild( i11, i11_1 );
		InstanceHelpers.insertChild( i11, i11_2 );
		InstanceHelpers.insertChild( i11_2, i11_2_1 );

		List<Instance> instances = new ArrayList<Instance> ();
		instances.add( i1 );
		instances.add( i2 );
		instances.add( i10 );
		instances.add( i11 );
		instances.add( i1_1 );
		instances.add( i2_1 );
		instances.add( i10_1 );
		instances.add( i11_1 );
		instances.add( i11_2 );
		instances.add( i11_2_1 );

		Collections.sort( instances, new InstanceComparator());
		Assert.assertEquals( i1, instances.get( 0 ));
		Assert.assertEquals( i1_1, instances.get( 1 ));
		Assert.assertEquals( i10, instances.get( 2 ));
		Assert.assertEquals( i10_1, instances.get( 3 ));
		Assert.assertEquals( i11, instances.get( 4 ));
		Assert.assertEquals( i11_1, instances.get( 5 ));
		Assert.assertEquals( i11_2, instances.get( 6 ));
		Assert.assertEquals( i11_2_1, instances.get( 7 ));
		Assert.assertEquals( i2, instances.get( 8 ));
		Assert.assertEquals( i2_1, instances.get( 9 ));
	}


	@Test
	public void testWithInvalidInstancePath() {

		Instance inst1 = new Instance( "inst" );
		Instance inst2 = new Instance();
		Instance inst3 = new Instance();
		Instance inst4 = new Instance( "instance" );

		List<Instance> instances = new ArrayList<Instance> ();
		instances.add( inst1 );
		instances.add( inst2 );
		instances.add( inst3 );
		instances.add( inst4 );

		Collections.sort( instances, new InstanceComparator());
		Assert.assertEquals( inst2, instances.get( 0 ));
		Assert.assertEquals( inst3, instances.get( 1 ));
		Assert.assertEquals( inst1, instances.get( 2 ));
		Assert.assertEquals( inst4, instances.get( 3 ));

		// Try another insertion order
		instances.clear();
		instances.add( inst2 );
		instances.add( inst1 );
		instances.add( inst3 );
		instances.add( inst4 );

		Collections.sort( instances, new InstanceComparator());
		Assert.assertEquals( inst2, instances.get( 0 ));
		Assert.assertEquals( inst3, instances.get( 1 ));
		Assert.assertEquals( inst1, instances.get( 2 ));
		Assert.assertEquals( inst4, instances.get( 3 ));
	}
}
