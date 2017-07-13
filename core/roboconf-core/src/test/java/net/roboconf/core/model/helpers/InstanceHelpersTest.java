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

package net.roboconf.core.model.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;

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
	public void testFindInstanceName() {

		Assert.assertEquals( "vm", InstanceHelpers.findInstanceName( "/vm" ));
		Assert.assertEquals( "tomcat server", InstanceHelpers.findInstanceName( "/vm/tomcat server" ));
		Assert.assertEquals( "war", InstanceHelpers.findInstanceName( "/vm/tomcat server/war" ));
		Assert.assertEquals( "no slash", InstanceHelpers.findInstanceName( "no slash" ));
		Assert.assertEquals( "   ", InstanceHelpers.findInstanceName( "   " ));
		Assert.assertEquals( "", InstanceHelpers.findInstanceName( "" ));
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

		Instance instance = new Instance( "inst 1" ).component( new Component( "c" ));
		Assert.assertEquals( 0, InstanceHelpers.findAllExportedVariables( instance ).size());

		instance.overriddenExports.put( "var1", "value1" );
		Map<String,String> map = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertEquals( 1, map.size());
		Assert.assertEquals( "value1", map.get( "var1" ));

		Component component = new Component( "comp 1" );
		component.addExportedVariable( new ExportedVariable( "var1", "another value" ));
		component.addExportedVariable( new ExportedVariable( "var2", "value2" ));
		instance.setComponent( component );

		map = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertEquals( 2, map.size());
		Assert.assertEquals( "value1", map.get( "comp 1.var1" ));
		Assert.assertEquals( "value2", map.get( "comp 1.var2" ));

		instance.overriddenExports.clear();
		map = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertEquals( 2, map.size());
		Assert.assertEquals( "another value", map.get( "comp 1.var1" ));
		Assert.assertEquals( "value2", map.get( "comp 1.var2" ));
	}


	@Test
	public void testFindAllExportedVariables_withFacets() {

		Component component = new Component( "comp 1" );
		component.addExportedVariable( new ExportedVariable( "var1", "another value" ));
		component.addExportedVariable( new ExportedVariable( "var2", "var 2 value" ));
		component.addExportedVariable( new ExportedVariable( "ip", null ));

		Facet f1 = new Facet( "f1" );
		f1.addExportedVariable( new ExportedVariable( "param1", "value1" ));
		component.associateFacet( f1 );

		Facet f2 = new Facet( "f2" );
		f2.addExportedVariable( new ExportedVariable( "param2", "value2" ));
		component.associateFacet( f2 );

		Facet f3 = new Facet( "f3" );
		f3.addExportedVariable( new ExportedVariable( "param3", "value3" ));
		component.associateFacet( f3 );
		component.addExportedVariable( new ExportedVariable( "f3.param3", "component overrides facet" ));

		Facet f4 = new Facet( "f4" );
		f4.addExportedVariable( new ExportedVariable( "param4-1", "value4" ));
		f4.addExportedVariable( new ExportedVariable( "param4-2", "value4" ));
		f2.extendFacet( f4 );
		f2.addExportedVariable( new ExportedVariable( "f4.param4-1", "facet overrides facet" ));

		Instance instance = new Instance( "inst 1" );
		instance.setComponent( component );
		instance.overriddenExports.put( "var1", "some value" );
		instance.overriddenExports.put( "toto.ip", null );
		instance.overriddenExports.put( "f1.param1", "my-value" );
		instance.data.put( Instance.IP_ADDRESS, "192.168.1.18" );

		Component extendedComponent = new Component( "extended" );
		extendedComponent.addExportedVariable( new ExportedVariable( "v", "hop" ));
		component.extendComponent( extendedComponent );
		component.addExportedVariable( new ExportedVariable( "extended.v", "nop" ));

		Map<String,String> map = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertEquals( 16, map.size());

		Assert.assertEquals( "some value", map.get( "comp 1.var1" ));
		Assert.assertEquals( "var 2 value", map.get( "comp 1.var2" ));
		Assert.assertEquals( "192.168.1.18", map.get( "comp 1.ip" ));

		Assert.assertEquals( "192.168.1.18", map.get( "toto.ip" ));

		Assert.assertEquals( "nop", map.get( "extended.v" ));

		// The first one was specifically overridden in the instance
		Assert.assertEquals( "my-value", map.get( "f1.param1" ));
		Assert.assertEquals( "value1", map.get( "comp 1.param1" ));

		Assert.assertEquals( "value2", map.get( "f2.param2" ));
		Assert.assertEquals( "value2", map.get( "comp 1.param2" ));

		Assert.assertEquals( "component overrides facet", map.get( "f3.param3" ));
		Assert.assertEquals( "component overrides facet", map.get( "comp 1.param3" ));

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

		Application app = new Application( new ApplicationTemplate());
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

		ApplicationTemplate app = new ApplicationTemplate();
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

		File directory = TestUtils.findTestFile( "/applications/lamp-legacy-with-only-components" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.getApplicationTemplate());
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( result.getLoadErrors()));

		ApplicationTemplate app = result.getApplicationTemplate();
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
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, mySqlInstance_1 ));

		mySqlInstance_1.overriddenExports.put( "port", "3307" );
		Assert.assertTrue( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, mySqlInstance_1 ));
		Assert.assertEquals( 3, InstanceHelpers.getAllInstances( app ).size());

		// Invalid application => no insertion
		Instance instanceWithNoComponent = new Instance( "MySQL-2" );
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, instanceWithNoComponent ));
		Assert.assertEquals( 3, InstanceHelpers.getAllInstances( app ).size());

		Instance instWithInvalidName = new Instance( "inst!!!" ).component( ComponentHelpers.findComponent( app.getGraphs(), "Apache" ));
		Assert.assertFalse( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, instWithInvalidName ));
		Assert.assertEquals( 3, InstanceHelpers.getAllInstances( app ).size());

		instWithInvalidName.setName( "whatever" );
		Assert.assertTrue( InstanceHelpers.tryToInsertChildInstance( app, vmInstance, instWithInvalidName ));
		Assert.assertEquals( 4, InstanceHelpers.getAllInstances( app ).size());
	}


	@Test
	public void testDuplicateInstance_singleInstance() {

		Instance original = new Instance( "inst" ).channel( "chan" ).component( new Component( "comp" ));
		original.overriddenExports.put( "test", "test" );
		original.overriddenExports.put( "A.port", "8012" );
		original.data.put( "some", "data" );
		original.getImports().put( "facet-name", new ArrayList<Import> ());
		original.setStatus( InstanceStatus.DEPLOYED_STARTED );

		Instance copy = InstanceHelpers.replicateInstance( original );
		Assert.assertEquals( original.getName(), copy.getName());
		Assert.assertEquals( original.channels, copy.channels );
		Assert.assertEquals( original.overriddenExports.size(), copy.overriddenExports.size());
		Assert.assertEquals( "test", copy.overriddenExports.get( "test" ));
		Assert.assertEquals( "8012", copy.overriddenExports.get( "A.port" ));
		Assert.assertEquals( 0, copy.getImports().size());
		Assert.assertEquals( original.getComponent(), copy.getComponent());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, copy.getStatus());
	}


	@Test
	public void testDuplicateInstance_withChildren() {

		// The originals
		Instance original_1 = new Instance( "inst-1" ).channel( "chan" ).component( new Component( "comp-1" ));
		original_1.overriddenExports.put( "test", "test" );
		original_1.overriddenExports.put( "A.port", "8012" );

		Instance original_2 = new Instance( "inst-2" ).channel( "chan" ).component( new Component( "comp-2" ));
		original_2.overriddenExports.put( "port", "8012" );

		Instance original_22 = new Instance( "inst-22" ).channel( "chan" ).component( new Component( "comp-78" ));

		Instance original_3 = new Instance( "inst-3" ).channel( "chan" ).component( new Component( "comp-3" ));
		original_3.overriddenExports.put( "ip", "localhost" );

		InstanceHelpers.insertChild( original_1, original_2 );
		InstanceHelpers.insertChild( original_1, original_22 );
		InstanceHelpers.insertChild( original_2, original_3 );

		// Perform a copy of the root
		Instance copy = InstanceHelpers.replicateInstance( original_1 );
		Assert.assertEquals( original_1.getName(), copy.getName());
		Assert.assertEquals( original_1.channels, copy.channels );
		Assert.assertEquals( original_1.overriddenExports.size(), copy.overriddenExports.size());
		Assert.assertEquals( "test", copy.overriddenExports.get( "test" ));
		Assert.assertEquals( "8012", copy.overriddenExports.get( "A.port" ));
		Assert.assertEquals( original_1.getComponent(), copy.getComponent());
		Assert.assertEquals( 2, copy.getChildren().size());
		Assert.assertNull( copy.getParent());

		Instance[] children = copy.getChildren().toArray( new Instance[ 0 ]);
		Assert.assertEquals( original_2.getName(), children[ 0 ].getName());
		Assert.assertEquals( original_2.channels, children[ 0 ].channels );
		Assert.assertEquals( original_2.overriddenExports.size(), children[ 0 ].overriddenExports.size());
		Assert.assertEquals( "8012", children[ 0 ].overriddenExports.get( "port" ));
		Assert.assertEquals( original_2.getComponent(), children[ 0 ].getComponent());
		Assert.assertEquals( 1, children[ 0 ].getChildren().size());
		Assert.assertEquals( copy, children[ 0 ].getParent());

		Assert.assertEquals( original_22.getName(), children[ 1 ].getName());
		Assert.assertEquals( original_22.channels, children[ 1 ].channels );
		Assert.assertEquals( 0, children[ 1 ].overriddenExports.size());
		Assert.assertEquals( original_22.getComponent(), children[ 1 ].getComponent());
		Assert.assertEquals( 0, children[ 1 ].getChildren().size());
		Assert.assertEquals( copy, children[ 1 ].getParent());

		Instance lastChild = children[ 0 ].getChildren().iterator().next();
		Assert.assertEquals( original_3.getName(), lastChild.getName());
		Assert.assertEquals( original_3.channels, lastChild.channels );
		Assert.assertEquals( original_3.overriddenExports.size(), lastChild.overriddenExports.size());
		Assert.assertEquals( "localhost", lastChild.overriddenExports.get( "ip" ));
		Assert.assertEquals( original_3.getComponent(), lastChild.getComponent());
		Assert.assertEquals( 0, lastChild.getChildren().size());
		Assert.assertEquals( children[ 0 ], lastChild.getParent());

		// Perform a copy of the first child (the one which has a child)
		copy = InstanceHelpers.replicateInstance( original_2 );
		Assert.assertEquals( original_2.getName(), copy.getName());
		Assert.assertEquals( original_2.channels, copy.channels );
		Assert.assertEquals( original_2.overriddenExports.size(), copy.overriddenExports.size());
		Assert.assertEquals( "8012", copy.overriddenExports.get( "port" ));
		Assert.assertEquals( original_2.getComponent(), copy.getComponent());
		Assert.assertEquals( 1, copy.getChildren().size());
		Assert.assertNull( copy.getParent());
		Assert.assertNotNull( original_2.getParent());

		lastChild = copy.getChildren().iterator().next();
		Assert.assertEquals( original_3.getName(), lastChild.getName());
		Assert.assertEquals( original_3.channels, lastChild.channels );
		Assert.assertEquals( original_3.overriddenExports.size(), lastChild.overriddenExports.size());
		Assert.assertEquals( "localhost", lastChild.overriddenExports.get( "ip" ));
		Assert.assertEquals( original_3.getComponent(), lastChild.getComponent());
		Assert.assertEquals( 0, lastChild.getChildren().size());
		Assert.assertEquals( copy, lastChild.getParent());
	}


	@Test
	public void testIsTarget() {

		Instance inst = new Instance( "i" );
		Assert.assertFalse( InstanceHelpers.isTarget( inst ));

		inst.setComponent( new Component( "comp" ).installerName( "whatever" ));
		Assert.assertFalse( InstanceHelpers.isTarget( inst ));

		inst.getComponent().setInstallerName( Constants.TARGET_INSTALLER );
		Assert.assertTrue( InstanceHelpers.isTarget( inst ));
	}


	@Test
	public void testFindRootInstancePath() {

		Assert.assertEquals( "", InstanceHelpers.findRootInstancePath( null ));
		Assert.assertEquals( "", InstanceHelpers.findRootInstancePath( "  " ));
		Assert.assertEquals( "root", InstanceHelpers.findRootInstancePath( "root" ));
		Assert.assertEquals( "root", InstanceHelpers.findRootInstancePath( "/root" ));
		Assert.assertEquals( "root", InstanceHelpers.findRootInstancePath( "/root/" ));
		Assert.assertEquals( "root", InstanceHelpers.findRootInstancePath( "//root//" ));
		Assert.assertEquals( "", InstanceHelpers.findRootInstancePath( "/" ));
		Assert.assertEquals( "root", InstanceHelpers.findRootInstancePath( "/root/server/whatever" ));
		Assert.assertEquals( "root", InstanceHelpers.findRootInstancePath( "root/invalid" ));
	}


	@Test
	public void testRemoveOffScopeInstances_zero() {

		Instance root = new Instance( "root" ).component( new Component( "Root" ).installerName( Constants.TARGET_INSTALLER ));
		Instance server = new Instance( "server" ).component( new Component( "Server" ).installerName( "whatever" ));
		Instance app1 = new Instance( "app1" ).component( new Component( "Application" ).installerName( "whatever" ));
		Instance app2 = new Instance( "app2" ).component( new Component( "Application" ).installerName( "whatever" ));
		Instance server2 = new Instance( "server2" ).component( new Component( "Server" ).installerName( "whatever" ));

		InstanceHelpers.insertChild( root, server );
		InstanceHelpers.insertChild( root, server2 );
		InstanceHelpers.insertChild( server, app1 );
		InstanceHelpers.insertChild( server, app2 );

		Assert.assertEquals( 5, InstanceHelpers.buildHierarchicalList( root ).size());
		InstanceHelpers.removeOffScopeInstances( root );
		Assert.assertEquals( 5, InstanceHelpers.buildHierarchicalList( root ).size());
	}


	@Test
	public void testRemoveOffScopeInstances_oneTarget() {

		Instance root = new Instance( "root" ).component( new Component( "Root" ).installerName( Constants.TARGET_INSTALLER ));
		Instance server = new Instance( "server" ).component( new Component( "Server" ).installerName( Constants.TARGET_INSTALLER ));
		Instance app1 = new Instance( "app1" ).component( new Component( "Application" ).installerName( "whatever" ));
		Instance app2 = new Instance( "app2" ).component( new Component( "Application" ).installerName( "whatever" ));
		Instance server2 = new Instance( "server2" ).component( new Component( "Server" ).installerName( "whatever" ));

		InstanceHelpers.insertChild( root, server );
		InstanceHelpers.insertChild( root, server2 );
		InstanceHelpers.insertChild( server, app1 );
		InstanceHelpers.insertChild( server2, app2 );

		Assert.assertEquals( 5, InstanceHelpers.buildHierarchicalList( root ).size());
		InstanceHelpers.removeOffScopeInstances( root );

		List<Instance> instances = InstanceHelpers.buildHierarchicalList( root );
		Assert.assertEquals( 3, instances.size());
		Assert.assertTrue( instances.contains( root ));
		Assert.assertTrue( instances.contains( server2 ));
		Assert.assertTrue( instances.contains( app2 ));
	}


	@Test
	public void testRemoveOffScopeInstances_oneMiddleTarget() {

		Instance root = new Instance( "root" ).component( new Component( "Root" ).installerName( Constants.TARGET_INSTALLER ));
		Instance server = new Instance( "server" ).component( new Component( "Server" ).installerName( Constants.TARGET_INSTALLER ));
		Instance app1 = new Instance( "app1" ).component( new Component( "Application" ).installerName( Constants.TARGET_INSTALLER ));
		Instance app2 = new Instance( "app2" ).component( new Component( "Application" ).installerName( "whatever" ));
		Instance server2 = new Instance( "server2" ).component( new Component( "Server" ).installerName( "whatever" ));

		InstanceHelpers.insertChild( root, server );
		InstanceHelpers.insertChild( root, server2 );
		InstanceHelpers.insertChild( server, app1 );
		InstanceHelpers.insertChild( server2, app2 );

		Assert.assertEquals( 5, InstanceHelpers.buildHierarchicalList( root ).size());
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( server ).size());
		InstanceHelpers.removeOffScopeInstances( server );

		Assert.assertEquals( 4, InstanceHelpers.buildHierarchicalList( root ).size());

		List<Instance> instances = InstanceHelpers.buildHierarchicalList( server );
		Assert.assertEquals( 1, instances.size());
		Assert.assertTrue( instances.contains( server ));
	}


	@Test
	public void testFindScopedInstance() {

		Instance root = new Instance( "root" ).component( new Component( "Root" ).installerName( Constants.TARGET_INSTALLER ));
		Instance server = new Instance( "server" ).component( new Component( "Server" ).installerName( Constants.TARGET_INSTALLER ));
		Instance app1 = new Instance( "app1" ).component( new Component( "Application" ).installerName( "whatever" ));
		Instance app2 = new Instance( "app2" ).component( new Component( "Application" ).installerName( "whatever" ));
		Instance server2 = new Instance( "server2" ).component( new Component( "Server" ).installerName( "whatever" ));

		InstanceHelpers.insertChild( root, server );
		InstanceHelpers.insertChild( root, server2 );
		InstanceHelpers.insertChild( server, app1 );
		InstanceHelpers.insertChild( server2, app2 );

		Assert.assertEquals( root, InstanceHelpers.findScopedInstance( root ));
		Assert.assertEquals( server, InstanceHelpers.findScopedInstance( server ));
		Assert.assertEquals( server, InstanceHelpers.findScopedInstance( app1 ));
		Assert.assertEquals( root, InstanceHelpers.findScopedInstance( app2 ));
	}


	@Test
	public void testFindAllScopedInstances() {

		Instance root = new Instance( "root" ).component( new Component( "Root" ).installerName( Constants.TARGET_INSTALLER ));
		Instance server = new Instance( "server" ).component( new Component( "Server" ).installerName( Constants.TARGET_INSTALLER ));
		Instance app1 = new Instance( "app1" ).component( new Component( "Application" ).installerName( "whatever" ));
		Instance app2 = new Instance( "app2" ).component( new Component( "Application" ).installerName( Constants.TARGET_INSTALLER ));
		Instance server2 = new Instance( "server2" ).component( new Component( "Server" ).installerName( "whatever" ));

		InstanceHelpers.insertChild( root, server );
		InstanceHelpers.insertChild( root, server2 );
		InstanceHelpers.insertChild( server, app1 );
		InstanceHelpers.insertChild( server2, app2 );

		Application app = new Application( "test", new ApplicationTemplate());
		app.getRootInstances().add( root );

		List<Instance> instances = InstanceHelpers.findAllScopedInstances( app );
		Assert.assertEquals( 3, instances.size());
		Assert.assertTrue( instances.contains( root ));
		Assert.assertTrue( instances.contains( server ));
		Assert.assertTrue( instances.contains( app2 ));
	}


	@Test
	public void testFixOverriddenExports() {

		Component comp = new Component("comp");
		comp.addExportedVariable( new ExportedVariable( "comp.export1", "c1" ));
		comp.addExportedVariable( new ExportedVariable( "comp.export2", "c2" ));
		comp.addExportedVariable( new ExportedVariable( "comp.export3", "c3" ));

		Instance inst = new Instance("inst").component(comp);
		Assert.assertEquals( 0, inst.overriddenExports.size());
		InstanceHelpers.fixOverriddenExports(inst);
		Assert.assertEquals( 0, inst.overriddenExports.size());

		inst.overriddenExports.put( "inst.export1", "i1" ); // New (instance export)
		inst.overriddenExports.put( "comp.export2", "c2" ); // Unchanged component export
		inst.overriddenExports.put( "comp.export3", "i3" ); // Overridden component export
		InstanceHelpers.fixOverriddenExports(inst);

		// Check component exports (no change expected).
		Assert.assertEquals( 3, comp.exportedVariables.size());
		Assert.assertEquals( "c1", comp.exportedVariables.get("comp.export1").getValue());
		Assert.assertEquals( "c2", comp.exportedVariables.get("comp.export2").getValue());
		Assert.assertEquals( "c3", comp.exportedVariables.get("comp.export3").getValue());

		// Check instance overridden exports
		// exports 1 and 3 should remain unchanged, export 2 should have disappeared!
		Assert.assertEquals( 2, inst.overriddenExports.size());
		Assert.assertEquals( "i1", inst.overriddenExports.get("inst.export1"));
		Assert.assertEquals( "i3", inst.overriddenExports.get("comp.export3"));
	}
}
