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

import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.impl.InstanceImpl;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceHelpersTest {

	@Test
	public void testComputeInstancePath() {

		InstanceImpl instance1 = new InstanceImpl();
		Assert.assertEquals( "/", InstanceHelpers.computeInstancePath( instance1 ));

		instance1.setName( "inst1" );
		Assert.assertEquals( "/inst1", InstanceHelpers.computeInstancePath( instance1 ));

		InstanceImpl instance2 = new InstanceImpl( "inst2" );
		Assert.assertEquals( "/inst2", InstanceHelpers.computeInstancePath( instance2 ));

		instance1.getChildren().add( instance2 );
		instance2.setParent( instance1 );
		Assert.assertEquals( "/inst1", InstanceHelpers.computeInstancePath( instance1 ));
		Assert.assertEquals( "/inst1/inst2", InstanceHelpers.computeInstancePath( instance2 ));
	}


	@Test
	public void testHaveSamePath() {

		InstanceImpl instance1 = new InstanceImpl( "inst1" );
		Assert.assertTrue( InstanceHelpers.haveSamePath( instance1, instance1 ));

		InstanceImpl instance2 = new InstanceImpl( "inst2" );
		Assert.assertFalse( InstanceHelpers.haveSamePath( instance1, instance2 ));

		instance2.setName( "inst1" );
		Assert.assertTrue( InstanceHelpers.haveSamePath( instance1, instance2 ));
	}


	@Test
	public void testBuildHierarchicalList() {

		// Series 1
		InstanceImpl instance_1 = new InstanceImpl( "inst 1" );
		List<Instance> instances = InstanceHelpers.buildHierarchicalList( instance_1 );
		Assert.assertEquals( 1, instances.size());
		Assert.assertEquals( instance_1, instances.get( 0 ));

		// Series 2
		InstanceImpl instance_1_1 = new InstanceImpl( "inst 11" );
		InstanceHelpers.insertChild( instance_1, instance_1_1 );

		instances = InstanceHelpers.buildHierarchicalList( instance_1 );
		Assert.assertEquals( 2, instances.size());
		Assert.assertEquals( instance_1, instances.get( 0 ));
		Assert.assertEquals( instance_1_1, instances.get( 1 ));

		// Series 3
		InstanceImpl instance_1_2 = new InstanceImpl( "inst 12" );
		InstanceHelpers.insertChild( instance_1, instance_1_2 );

		InstanceImpl instance_1_1_1 = new InstanceImpl( "inst 111" );
		InstanceHelpers.insertChild( instance_1_1, instance_1_1_1 );

		InstanceImpl instance_1_2_1 = new InstanceImpl( "inst 121" );
		InstanceHelpers.insertChild( instance_1_2, instance_1_2_1 );

		instances = InstanceHelpers.buildHierarchicalList( instance_1 );
		Assert.assertEquals( 5, instances.size());
		Assert.assertEquals( instance_1, instances.get( 0 ));
		Assert.assertEquals( instance_1_1, instances.get( 1 ));
		Assert.assertEquals( instance_1_2, instances.get( 2 ));
		Assert.assertEquals( instance_1_1_1, instances.get( 3 ));
		Assert.assertEquals( instance_1_2_1, instances.get( 4 ));
	}


	@Test
	public void testInsertChild() {

		InstanceImpl instance_1 = new InstanceImpl( "inst 1" );
		InstanceImpl instance_1_1 = new InstanceImpl( "inst 11" );

		Assert.assertNull( instance_1.getParent());
		Assert.assertNull( instance_1_1.getParent());
		Assert.assertEquals( 0, instance_1.getChildren().size());
		InstanceHelpers.insertChild( instance_1, instance_1_1 );

		Assert.assertEquals( 1, instance_1.getChildren().size());
		Assert.assertEquals( instance_1_1, instance_1.getChildren().iterator().next());
		Assert.assertEquals( instance_1, instance_1_1.getParent());
		Assert.assertTrue( instance_1.getChildren().contains( instance_1_1 ));
		Assert.assertNull( instance_1.getParent());
		Assert.assertNotSame( instance_1, instance_1_1 );
	}
}
