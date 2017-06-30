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

package net.roboconf.core.dsl.converters;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromInstanceDefinitionTest {

	@Test
	public void testDuplicateInstance() throws Exception {

		Component vmComponent = new Component( "VM" ).installerName( "target" );
		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "tomcat.port", "8080" ));

		vmComponent.addChild( tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-instance.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		fromDef.buildInstances( graphs, f );

		Iterator<ParsingError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testInexistingComponent() throws Exception {

		Component vmComponent = new Component( "VM" ).installerName( "target" );
		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "tomcat.port", "8080" ));

		vmComponent.addChild( tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/invalid/inexisting-component.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		fromDef.buildInstances( graphs, f );

		Iterator<ParsingError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_INEXISTING_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testInstanceWithExtraData() throws Exception {

		Component vmComponent = new Component( "VM" ).installerName( "target" );
		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "tomcat.port", "8080" ));

		vmComponent.addChild( tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/valid/instance-with-extra-data.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( 1, rootInstances.size());

		Instance vmInstance = rootInstances.iterator().next();
		Assert.assertEquals( 1, vmInstance.getChildren().size());
		Assert.assertEquals( "VM1", vmInstance.getName());
		Assert.assertEquals( 1, vmInstance.data.size());
		Assert.assertEquals( "192.168.1.10", vmInstance.data.get( "ec2.elastic.ip" ));
	}


	@Test
	public void testComponentResolutionWhenSurroundingSpaces() throws Exception {

		Component vmComponent = new Component( "VM" ).installerName( "target" );
		Component aComponent = new Component( "A" ).installerName( "whatever" );

		vmComponent.addChild( aComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/valid/instance-with-space-after.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );

		Iterator<ParsingError> iterator = fromDef.getErrors().iterator();
		Assert.assertFalse( iterator.hasNext());

		Assert.assertEquals( 2, rootInstances.size());
		for( Instance rootInstance : rootInstances ) {
			Assert.assertEquals( 1, rootInstance.getChildren().size());
			Instance instance = rootInstance.getChildren().iterator().next();

			Assert.assertEquals( "A", instance.getComponent().getName());
			Assert.assertEquals( "A ", instance.getName());
		}
	}


	@Test
	public void testInstancesWithWrongImport() throws Exception {

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( new Component( "VM" ).installerName( "target" ));

		File f = TestUtils.findTestFile( "/configurations/invalid/instances-with-inexisting-import.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		fromDef.buildInstances( graphs, f );

		Iterator<ParsingError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_UNREACHABLE_FILE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testComplexInstances() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "VM" ).installerName( "target" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.port", "8080" ));
		vmComponent.addChild( tomcatComponent );

		Component warComponent = new Component( "WAR" ).installerName( "script" );
		tomcatComponent.addChild( warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/complex-instances.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Application app = new Application( new ApplicationTemplate());
		app.getRootInstances().addAll( rootInstances );

		Assert.assertEquals( 3, rootInstances.size());
		Assert.assertEquals( 8, InstanceHelpers.getAllInstances( app ).size());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1/i-tomcat" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1/i-tomcat/i-war" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-2" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-3" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-3/i-tomcat-1" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-3/i-tomcat-2" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-3/i-tomcat-2/i-war" ));
	}


	@Test
	public void test_N_Instantiations() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "VM" ).installerName( "target" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.port", "8080" ));
		vmComponent.addChild( tomcatComponent );

		Component warComponent = new Component( "WAR" ).installerName( "script" );
		tomcatComponent.addChild( warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/n-instances.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Application app = new Application( new ApplicationTemplate());
		app.getRootInstances().addAll( rootInstances );

		Assert.assertEquals( 14, rootInstances.size());
		Assert.assertEquals( 3688, InstanceHelpers.getAllInstances( app ).size());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1/i-tomcat" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1/i-tomcat/i-war" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-2" ));

		// The n-instantiated begin here
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-01" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-01/i-tomcat-1" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-01/i-tomcat-1/i-war001" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-12" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-12/i-tomcat-1" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-12/i-tomcat-1/i-war001" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-09" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-09/i-tomcat-3" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-09/i-tomcat-3/i-war001" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-09" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-09/i-tomcat-2" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-09/i-tomcat-2/i-war101" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-09/i-tomcat-2/i-war025" ));
	}


	@Test
	public void test_N_medium_Instantiations() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "VM" ).installerName( "target" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.port", "8080" ));
		vmComponent.addChild( tomcatComponent );

		Component warComponent = new Component( "WAR" ).installerName( "script" );
		tomcatComponent.addChild( warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/n-medium-instances.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Application app = new Application( new ApplicationTemplate());
		app.getRootInstances().addAll( rootInstances );

		Assert.assertEquals( 3, rootInstances.size());
		Assert.assertEquals( 2045, InstanceHelpers.getAllInstances( app ).size());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1/i-tomcat" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1/i-tomcat/i-war" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-2" ));

		// The n-instantiated begin here
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-/i-tomcat-0001" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-/i-tomcat-0001/i-war" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-/i-tomcat-1001" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-/i-tomcat-1001/i-war" ));

		Assert.assertNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-/i-tomcat-1001/i-war1" ));
		Assert.assertNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-/i-tomcat-1001/i-war01" ));
	}



	@Test
	public void test_N_InstantiationsWithConflict() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "VM" ).installerName( "target" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.port", "8080" ));
		vmComponent.addChild( tomcatComponent );

		Component warComponent = new Component( "WAR" ).installerName( "script" );
		tomcatComponent.addChild( warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/invalid/instanceof-name-conflict-with-count.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );
		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_CONFLICTING_INFERRED_INSTANCE, fromDef.getErrors().iterator().next().getErrorCode());

		// The assertions
		Application app = new Application( new ApplicationTemplate());
		app.getRootInstances().addAll( rootInstances );

		Assert.assertEquals( 5, rootInstances.size());
		// Should be 6, but there two have the same path.
		// So, one is overriding the other.
	}


	@Test
	public void testRuntimeData() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "vm" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( vmComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/single-runtime-instance.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Assert.assertEquals( 1, rootInstances.size());
		Instance instance = rootInstances.iterator().next();

		Assert.assertEquals( "vm 1", instance.getName());
		Assert.assertEquals( vmComponent, instance.getComponent());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, instance.getStatus());
		Assert.assertEquals( 3, instance.data.size());
		Assert.assertEquals( "127.0.0.1", instance.data.get( "ip" ));
		Assert.assertEquals( "mach-ID", instance.data.get( "machine-id" ));
		Assert.assertEquals( "something different", instance.data.get( "whatever" ));
	}


	@Test
	public void testInstanceWithComplexQuotedVariables() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "vm" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( vmComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/instance-with-complex-quoted-values.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Assert.assertEquals( 1, rootInstances.size());
		Instance instance = rootInstances.iterator().next();

		Assert.assertEquals( "vm 1", instance.getName());
		Assert.assertEquals( vmComponent, instance.getComponent());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, instance.getStatus());

		Assert.assertEquals( "38.195.27.7", instance.overriddenExports.get( "ip" ));
		Assert.assertEquals( "this is ;a; complex value with semicolons", instance.overriddenExports.get( "message" ));
		Assert.assertEquals( "", instance.overriddenExports.get( "bad3" ));
		Assert.assertEquals( "a long sentence for test", instance.overriddenExports.get( "bad4" ));

		Assert.assertEquals( 1, instance.channels.size());
		Assert.assertEquals( "demo", instance.channels.iterator().next());
	}
}
