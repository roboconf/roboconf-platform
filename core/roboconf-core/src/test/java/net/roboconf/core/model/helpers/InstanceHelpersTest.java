/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;

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

		// Series 0
		List<Instance> instances = InstanceHelpers.buildHierarchicalList( null );
		Assert.assertEquals( 0, instances.size());

		// Series 1
		Instance instance_1 = new Instance( "inst 1" );
		instances = InstanceHelpers.buildHierarchicalList( instance_1 );
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
	public void testFindAllExportedVariables() {

		Instance instance = new Instance( "inst 1" );
		Assert.assertEquals( 0, InstanceHelpers.findAllExportedVariables( instance ).size());

		instance.overridenExports.put( "var1", "value1" );
		Map<String,String> map = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertEquals( 1, map.size());
		Assert.assertEquals( "value1", map.get( "var1" ));

		Component component = new Component( "comp 1" );
		component.exportedVariables.put( "var1", "another value" );
		component.exportedVariables.put( "var2", "value2" );
		instance.setComponent( component );

		map = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertEquals( 2, map.size());
		Assert.assertEquals( "value1", map.get( "comp 1.var1" ));
		Assert.assertEquals( "value2", map.get( "comp 1.var2" ));

		instance.overridenExports.clear();
		map = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertEquals( 2, map.size());
		Assert.assertEquals( "another value", map.get( "comp 1.var1" ));
		Assert.assertEquals( "value2", map.get( "comp 1.var2" ));
	}


	@Test
	public void testFindAllExportedVariables_withFacets() {

		Component component = new Component( "comp 1" );
		component.exportedVariables.put( "var1", "another value" );
		component.exportedVariables.put( "var2", "var 2 value" );
		component.exportedVariables.put( "ip", null );

		Facet f1 = new Facet( "f1" );
		f1.exportedVariables.put( "param1", "value1" );
		component.associateFacet( f1 );

		Facet f2 = new Facet( "f2" );
		f2.exportedVariables.put( "param2", "value2" );
		component.associateFacet( f2 );

		Facet f3 = new Facet( "f3" );
		f3.exportedVariables.put( "param3", "value3" );
		component.associateFacet( f3 );
		component.exportedVariables.put( "f3.param3", "component overrides facet" );

		Facet f4 = new Facet( "f4" );
		f4.exportedVariables.put( "param4-1", "value4" );
		f4.exportedVariables.put( "param4-2", "value4" );
		f2.extendFacet( f4 );
		f2.exportedVariables.put( "f4.param4-1", "facet overrides facet" );

		Instance instance = new Instance( "inst 1" );
		instance.setComponent( component );
		instance.overridenExports.put( "var1", "some value" );
		instance.overridenExports.put( "toto.ip", null );
		instance.overridenExports.put( "f1.param1", "my-value" );
		instance.data.put( Instance.IP_ADDRESS, "192.168.1.18" );

		Component extendedComponent = new Component( "extended" );
		extendedComponent.exportedVariables.put( "v", "hop" );
		component.extendComponent( extendedComponent );
		component.exportedVariables.put( "extended.v", "nop" );

		Map<String,String> map = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertEquals( 10, map.size());

		Assert.assertEquals( "some value", map.get( "comp 1.var1" ));
		Assert.assertEquals( "var 2 value", map.get( "comp 1.var2" ));
		Assert.assertEquals( "192.168.1.18", map.get( "comp 1.ip" ));

		Assert.assertEquals( "192.168.1.18", map.get( "toto.ip" ));

		Assert.assertEquals( "nop", map.get( "extended.v" ));

		Assert.assertEquals( "my-value", map.get( "f1.param1" ));
		Assert.assertEquals( "value2", map.get( "f2.param2" ));
		Assert.assertEquals( "component overrides facet", map.get( "f3.param3" ));

		Assert.assertEquals( "facet overrides facet", map.get( "f4.param4-1" ));
		Assert.assertEquals( "value4", map.get( "f4.param4-2" ));
	}


	@Test
	public void testCountInstances() {

		Assert.assertEquals( 0, InstanceHelpers.countInstances( "" ));
		Assert.assertEquals( 0, InstanceHelpers.countInstances( "toto" ));
		Assert.assertEquals( 1, InstanceHelpers.countInstances( "/root-instance" ));
		Assert.assertEquals( 2, InstanceHelpers.countInstances( "/root-instance/apache" ));
		Assert.assertEquals( 3, InstanceHelpers.countInstances( "/root-instance/apache/war" ));
	}


	@Test
	public void testFindInstanceDirectoryOnAgent() {

		File f = InstanceHelpers.findInstanceDirectoryOnAgent( new Instance( "inst" )
				.component( new Component( "c" ).installerName( "my-plugin" )));

		File tempDir = new File( System.getProperty( "java.io.tmpdir" ));
		Assert.assertTrue( f.getAbsolutePath().startsWith( tempDir.getAbsolutePath()));
		Assert.assertTrue( f.getAbsolutePath().contains( "inst" ));
		Assert.assertTrue( f.getAbsolutePath().contains( "my-plugin" ));
	}


	@Test
	public void testGetAllInstances() {

		Application app = new Application();
		Instance[] rootInstances = new Instance[ 8 ];
		for( int i=0; i<rootInstances.length; i++ ) {
			rootInstances[ i ] = new Instance( "i-" + i );
			InstanceHelpers.insertChild( rootInstances[ i ], new Instance( "child-" + i ));
		}

		app.getRootInstances().addAll( Arrays.asList( rootInstances ));
		List<Instance> allInstances = InstanceHelpers.getAllInstances( app );
		Assert.assertEquals( rootInstances.length * 2, allInstances.size());
		for( Instance rootInstance : rootInstances )
			Assert.assertTrue( rootInstance.getName(), allInstances.contains( rootInstance ));
	}


	@Test
	public void testFindRootInstance() {

		Instance inst = new Instance( "inst" );
		Assert.assertEquals( inst, InstanceHelpers.findRootInstance( inst ));

		Instance childInstance = new Instance( "child-instance" );
		InstanceHelpers.insertChild( inst, childInstance );
		Assert.assertEquals( inst, InstanceHelpers.findRootInstance( inst ));
		Assert.assertEquals( inst, InstanceHelpers.findRootInstance( childInstance ));

		Instance lastChild = childInstance;
		for( int i=0; i<8; i++ ) {
			Instance tempInstance = new Instance( "child-" + i );
			InstanceHelpers.insertChild( lastChild, tempInstance );
			lastChild = tempInstance;
		}

		Assert.assertEquals( inst, InstanceHelpers.findRootInstance( lastChild ));
	}


	@Test
	public void testFindInstancesByComponentName() {

		Application app = new Application();
		Component tomcat = new Component( "tomcat" ).installerName( "puppet" );
		Component other = new Component( "other" ).installerName( "chef" );

		Instance i1 = new Instance( "i1" ).component( tomcat );
		Instance i2 = new Instance( "i2" ).component( tomcat );
		Instance i3 = new Instance( "i3" ).component( other );
		Instance i4 = new Instance( "i4" ).component( other );

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( other );
		graphs.getRootComponents().add( tomcat );
		app.setGraphs( graphs );

		InstanceHelpers.insertChild( i3, i1 );
		app.getRootInstances().add( i2 );
		app.getRootInstances().add( i3 );
		app.getRootInstances().add( i4 );

		List<Instance> tomcatInstances = InstanceHelpers.findInstancesByComponentName( app, tomcat.getName());
		Assert.assertEquals( 2, tomcatInstances.size());
		Assert.assertTrue( tomcatInstances.contains( i1 ));
		Assert.assertTrue( tomcatInstances.contains( i2 ));

		List<Instance> otherInstances = InstanceHelpers.findInstancesByComponentName( app, other.getName());
		Assert.assertEquals( 2, otherInstances.size());
		Assert.assertTrue( otherInstances.contains( i3 ));
		Assert.assertTrue( otherInstances.contains( i4 ));

		Assert.assertEquals( 0, InstanceHelpers.findInstancesByComponentName( app, "whatever" ).size());
	}


	@Test
	public void testFindInstanceByPath() {

		Instance rootInstance = new Instance( "root" );
		Instance current = rootInstance;
		for( int i=1; i<8; i++ ) {
			Instance tempInstance = new Instance( "i-" + i );
			InstanceHelpers.insertChild( current, tempInstance );
			current = tempInstance;
		}

		Assert.assertEquals( "root", InstanceHelpers.findInstanceByPath( rootInstance, "/root" ).getName());
		Assert.assertEquals( "i-4", InstanceHelpers.findInstanceByPath( rootInstance, "/root/i-1/i-2/i-3/i-4" ).getName());
		Assert.assertNull( InstanceHelpers.findInstanceByPath( rootInstance, "whatever" ));
		Assert.assertNull( InstanceHelpers.findInstanceByPath( rootInstance, "/root/whatever" ));
		Assert.assertNull( InstanceHelpers.findInstanceByPath( rootInstance, "/root/i-1/i-3" ));

		Assert.assertNull( InstanceHelpers.findInstanceByPath((Instance) null, "/root/i-1/i-3" ));
		Assert.assertNull( InstanceHelpers.findInstanceByPath( rootInstance, null ));
		Assert.assertNull( InstanceHelpers.findInstanceByPath((Application) null, "/root" ));

	}


	@Test
	public void testTryToInsertChildInstance() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/valid/lamp-legacy-with-only-components" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.getApplication());
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( result.getLoadErrors()));

		Application app = result.getApplication();
		app.getRootInstances().clear();
		Assert.assertEquals( 0, InstanceHelpers.getAllInstances( app ).size());

		Instance vmInstance = new Instance( "vm-1" ).component( ComponentHelpers.findComponent( app.getGraphs(), "VM" ));
		Assert.assertTrue( InstanceHelpers.tryToInsertChildInstance( app, null, vmInstance ));
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, null, vmInstance ));
		Assert.assertEquals( 1, InstanceHelpers.getAllInstances( app ).size());

		Instance tomcatInstance_1 = new Instance( "tomcat-1" ).component( ComponentHelpers.findComponent( app.getGraphs(), "Tomcat" ));
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, null, tomcatInstance_1 ));
		Assert.assertTrue( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, tomcatInstance_1 ));
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, tomcatInstance_1 ));
		Assert.assertEquals( 2, InstanceHelpers.getAllInstances( app ).size());

		Instance mySqlInstance_1 = new Instance( "MySQL-1" ).component( ComponentHelpers.findComponent( app.getGraphs(), "MySQL" ));
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, tomcatInstance_1, mySqlInstance_1 ));
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, mySqlInstance_1, tomcatInstance_1 ));
		Assert.assertEquals( 2, InstanceHelpers.getAllInstances( app ).size());

		Assert.assertTrue( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, mySqlInstance_1 ));
		Assert.assertEquals( 3, InstanceHelpers.getAllInstances( app ).size());

		// Invalid application => no insertion
		Instance instanceWithNoComponent = new Instance( "MySQL-2" );
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, instanceWithNoComponent ));
		Assert.assertEquals( 3, InstanceHelpers.getAllInstances( app ).size());

		Instance instWithInvalidName = new Instance( "inst!!!" ).component( ComponentHelpers.findComponent( app.getGraphs(), "MySQL" ));
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, instWithInvalidName ));
		Assert.assertEquals( 3, InstanceHelpers.getAllInstances( app ).size());

		instWithInvalidName.setName( "whatever" );
		Assert.assertTrue( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, instWithInvalidName ));
		Assert.assertEquals( 4, InstanceHelpers.getAllInstances( app ).size());
	}


	@Test
	public void testDuplicateInstance_singleInstance() {

		Instance original = new Instance( "inst" ).channel( "chan" ).component( new Component( "comp" ));
		original.overridenExports.put( "test", "test" );
		original.overridenExports.put( "A.port", "8012" );
		original.data.put( "some", "data" );
		original.getImports().put( "facet-name", new ArrayList<Import> ());

		Instance copy = InstanceHelpers.replicateInstance( original );
		Assert.assertEquals( original.getName(), copy.getName());
		Assert.assertEquals( original.channels, copy.channels );
		Assert.assertEquals( original.overridenExports.size(), copy.overridenExports.size());
		Assert.assertEquals( "test", copy.overridenExports.get( "test" ));
		Assert.assertEquals( "8012", copy.overridenExports.get( "A.port" ));
		Assert.assertEquals( 0, copy.getImports().size());
		Assert.assertEquals( original.getComponent(), copy.getComponent());
	}


	@Test
	public void testDuplicateInstance_withChildren() {

		// The originals
		Instance original_1 = new Instance( "inst-1" ).channel( "chan" ).component( new Component( "comp-1" ));
		original_1.overridenExports.put( "test", "test" );
		original_1.overridenExports.put( "A.port", "8012" );

		Instance original_2 = new Instance( "inst-2" ).channel( "chan" ).component( new Component( "comp-2" ));
		original_2.overridenExports.put( "port", "8012" );

		Instance original_22 = new Instance( "inst-22" ).channel( "chan" ).component( new Component( "comp-78" ));

		Instance original_3 = new Instance( "inst-3" ).channel( "chan" ).component( new Component( "comp-3" ));
		original_3.overridenExports.put( "ip", "localhost" );

		InstanceHelpers.insertChild( original_1, original_2 );
		InstanceHelpers.insertChild( original_1, original_22 );
		InstanceHelpers.insertChild( original_2, original_3 );

		// Perform a copy of the root
		Instance copy = InstanceHelpers.replicateInstance( original_1 );
		Assert.assertEquals( original_1.getName(), copy.getName());
		Assert.assertEquals( original_1.channels, copy.channels );
		Assert.assertEquals( original_1.overridenExports.size(), copy.overridenExports.size());
		Assert.assertEquals( "test", copy.overridenExports.get( "test" ));
		Assert.assertEquals( "8012", copy.overridenExports.get( "A.port" ));
		Assert.assertEquals( original_1.getComponent(), copy.getComponent());
		Assert.assertEquals( 2, copy.getChildren().size());
		Assert.assertNull( copy.getParent());

		Instance[] children = copy.getChildren().toArray( new Instance[ 0 ]);
		Assert.assertEquals( original_2.getName(), children[ 0 ].getName());
		Assert.assertEquals( original_2.channels, children[ 0 ].channels );
		Assert.assertEquals( original_2.overridenExports.size(), children[ 0 ].overridenExports.size());
		Assert.assertEquals( "8012", children[ 0 ].overridenExports.get( "port" ));
		Assert.assertEquals( original_2.getComponent(), children[ 0 ].getComponent());
		Assert.assertEquals( 1, children[ 0 ].getChildren().size());
		Assert.assertEquals( copy, children[ 0 ].getParent());

		Assert.assertEquals( original_22.getName(), children[ 1 ].getName());
		Assert.assertEquals( original_22.channels, children[ 1 ].channels );
		Assert.assertEquals( 0, children[ 1 ].overridenExports.size());
		Assert.assertEquals( original_22.getComponent(), children[ 1 ].getComponent());
		Assert.assertEquals( 0, children[ 1 ].getChildren().size());
		Assert.assertEquals( copy, children[ 1 ].getParent());

		Instance lastChild = children[ 0 ].getChildren().iterator().next();
		Assert.assertEquals( original_3.getName(), lastChild.getName());
		Assert.assertEquals( original_3.channels, lastChild.channels );
		Assert.assertEquals( original_3.overridenExports.size(), lastChild.overridenExports.size());
		Assert.assertEquals( "localhost", lastChild.overridenExports.get( "ip" ));
		Assert.assertEquals( original_3.getComponent(), lastChild.getComponent());
		Assert.assertEquals( 0, lastChild.getChildren().size());
		Assert.assertEquals( children[ 0 ], lastChild.getParent());

		// Perform a copy of the first child (the one which has a child)
		copy = InstanceHelpers.replicateInstance( original_2 );
		Assert.assertEquals( original_2.getName(), copy.getName());
		Assert.assertEquals( original_2.channels, copy.channels );
		Assert.assertEquals( original_2.overridenExports.size(), copy.overridenExports.size());
		Assert.assertEquals( "8012", copy.overridenExports.get( "port" ));
		Assert.assertEquals( original_2.getComponent(), copy.getComponent());
		Assert.assertEquals( 1, copy.getChildren().size());
		Assert.assertNull( copy.getParent());
		Assert.assertNotNull( original_2.getParent());

		lastChild = copy.getChildren().iterator().next();
		Assert.assertEquals( original_3.getName(), lastChild.getName());
		Assert.assertEquals( original_3.channels, lastChild.channels );
		Assert.assertEquals( original_3.overridenExports.size(), lastChild.overridenExports.size());
		Assert.assertEquals( "localhost", lastChild.overridenExports.get( "ip" ));
		Assert.assertEquals( original_3.getComponent(), lastChild.getComponent());
		Assert.assertEquals( 0, lastChild.getChildren().size());
		Assert.assertEquals( copy, lastChild.getParent());
	}
}
