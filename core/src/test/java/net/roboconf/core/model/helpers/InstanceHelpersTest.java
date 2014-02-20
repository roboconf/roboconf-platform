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
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceHelpersTest {

	@Test
	public void testComputeInstancePath() {

		Instance instance1 = new Instance();
		Assert.assertEquals( "/", InstanceHelpers.computeInstancePath( instance1 ));

		instance1.setName( "inst1" );
		Assert.assertEquals( "/inst1", InstanceHelpers.computeInstancePath( instance1 ));

		Instance instance2 = new Instance( "inst2" );
		Assert.assertEquals( "/inst2", InstanceHelpers.computeInstancePath( instance2 ));

		instance1.getChildren().add( instance2 );
		instance2.setParent( instance1 );
		Assert.assertEquals( "/inst1", InstanceHelpers.computeInstancePath( instance1 ));
		Assert.assertEquals( "/inst1/inst2", InstanceHelpers.computeInstancePath( instance2 ));
	}


	@Test
	public void testHaveSamePath() {

		Instance instance1 = new Instance( "inst1" );
		Assert.assertTrue( InstanceHelpers.haveSamePath( instance1, instance1 ));

		Instance instance2 = new Instance( "inst2" );
		Assert.assertFalse( InstanceHelpers.haveSamePath( instance1, instance2 ));

		instance2.setName( "inst1" );
		Assert.assertTrue( InstanceHelpers.haveSamePath( instance1, instance2 ));
	}


	@Test
	public void testBuildHierarchicalList() {

		// Series 1
		Instance instance_1 = new Instance( "inst 1" );
		List<Instance> instances = InstanceHelpers.buildHierarchicalList( instance_1 );
		Assert.assertEquals( 1, instances.size());
		Assert.assertEquals( instance_1, instances.get( 0 ));

		// Series 2
		Instance instance_1_1 = new Instance( "inst 11" );
		InstanceHelpers.insertChild( instance_1, instance_1_1 );

		instances = InstanceHelpers.buildHierarchicalList( instance_1 );
		Assert.assertEquals( 2, instances.size());
		Assert.assertEquals( instance_1, instances.get( 0 ));
		Assert.assertEquals( instance_1_1, instances.get( 1 ));

		// Series 3
		Instance instance_1_2 = new Instance( "inst 12" );
		InstanceHelpers.insertChild( instance_1, instance_1_2 );

		Instance instance_1_1_1 = new Instance( "inst 111" );
		InstanceHelpers.insertChild( instance_1_1, instance_1_1_1 );

		Instance instance_1_2_1 = new Instance( "inst 121" );
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

		Instance instance_1 = new Instance( "inst 1" );
		Instance instance_1_1 = new Instance( "inst 11" );

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


	@Test
	public void testGetExportedVariables() {

		Instance instance = new Instance( "inst 1" );
		Assert.assertEquals( 0, InstanceHelpers.getExportedVariables( instance ).size());

		instance.getOverriddenExports().put( "var1", "value1" );
		Map<String,String> map = InstanceHelpers.getExportedVariables( instance );
		Assert.assertEquals( 1, map.size());
		Assert.assertEquals( "value1", map.get( "var1" ));

		Component component = new Component( "comp 1" );
		component.getExportedVariables().put( "var1", "another value" );
		component.getExportedVariables().put( "var2", "value2" );
		instance.setComponent( component );

		map = InstanceHelpers.getExportedVariables( instance );
		Assert.assertEquals( 2, map.size());
		Assert.assertEquals( "value1", map.get( "var1" ));
		Assert.assertEquals( "value2", map.get( "var2" ));

		instance.getOverriddenExports().clear();
		map = InstanceHelpers.getExportedVariables( instance );
		Assert.assertEquals( 2, map.size());
		Assert.assertEquals( "another value", map.get( "var1" ));
		Assert.assertEquals( "value2", map.get( "var2" ));
	}
}
