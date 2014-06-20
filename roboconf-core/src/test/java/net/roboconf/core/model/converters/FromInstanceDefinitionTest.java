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

package net.roboconf.core.model.converters;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.ParsingModelIo;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromInstanceDefinitionTest {

	@Test( expected = IllegalArgumentException.class )
	public void testIllegalArgument() {

		FileDefinition def = new FileDefinition( new File( "whatever.txt" ));
		def.setFileType( FileDefinition.GRAPH );
		new FromInstanceDefinition( def );
	}


	@Test
	public void testAnalyzeOverriddenExport() {

		Component tomcatComponent = new Component( "Tomcat" ).alias( "App Server" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "tomcat.port", "8080" );

		Instance tomcatInstance = new Instance( "tomcat" ).component( tomcatComponent );
		List<ModelError> errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "unknown", "whatever" );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CO_NOT_OVERRIDING, errors.get( 0 ).getErrorCode());

		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "tomcat.port", "whatever" );
		Assert.assertEquals( 0, errors.size());

		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "port", "whatever" );
		Assert.assertEquals( 0, errors.size());

		tomcatComponent.getExportedVariables().put( "some-facet.port", "8081" );
		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "port", "whatever" );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CO_AMBIGUOUS_OVERRIDING, errors.get( 0 ).getErrorCode());

		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "tomcat.port", "whatever" );
		Assert.assertEquals( 0, errors.size());

		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "some-facet.port", "whatever" );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testOverriddenExports() throws Exception {

		Component vmComponent = new Component( "VM" ).alias( "a VM" ).installerName( "iaas" );
		Component tomcatComponent = new Component( "Tomcat" ).alias( "App Server" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "tomcat.port", "8080" );

		ComponentHelpers.insertChild( vmComponent, tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/valid/instance-overridden-exports.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs );

		Iterator<ModelError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_NOT_OVERRIDING, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Assert.assertEquals( 1, rootInstances.size());
		Instance rootInstance = rootInstances.iterator().next();
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( rootInstance ).size());
	}


	@Test
	public void testDuplicateInstance() throws Exception {

		Component vmComponent = new Component( "VM" ).alias( "a VM" ).installerName( "iaas" );
		Component tomcatComponent = new Component( "Tomcat" ).alias( "App Server" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "tomcat.port", "8080" );

		ComponentHelpers.insertChild( vmComponent, tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-instance.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		fromDef.buildInstances( graphs );
		Iterator<ModelError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testComplexInstances() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "VM" ).alias( "VM" ).installerName( "iaas" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).alias( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "Tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "Tomcat.port", "8080" );
		ComponentHelpers.insertChild( vmComponent, tomcatComponent );

		Component warComponent = new Component( "WAR" ).alias( "A simple web application" ).installerName( "bash" );
		ComponentHelpers.insertChild( tomcatComponent, warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/complex-instances.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Application app = new Application();
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
		Component vmComponent = new Component( "VM" ).alias( "VM" ).installerName( "iaas" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).alias( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "Tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "Tomcat.port", "8080" );
		ComponentHelpers.insertChild( vmComponent, tomcatComponent );

		Component warComponent = new Component( "WAR" ).alias( "A simple web application" ).installerName( "bash" );
		ComponentHelpers.insertChild( tomcatComponent, warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/n-instances.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Application app = new Application();
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
		Component vmComponent = new Component( "VM" ).alias( "VM" ).installerName( "iaas" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).alias( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "Tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "Tomcat.port", "8080" );
		ComponentHelpers.insertChild( vmComponent, tomcatComponent );

		Component warComponent = new Component( "WAR" ).alias( "A simple web application" ).installerName( "bash" );
		ComponentHelpers.insertChild( tomcatComponent, warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/n-medium-instances.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Application app = new Application();
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
		Component vmComponent = new Component( "VM" ).alias( "VM" ).installerName( "iaas" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).alias( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "Tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "Tomcat.port", "8080" );
		ComponentHelpers.insertChild( vmComponent, tomcatComponent );

		Component warComponent = new Component( "WAR" ).alias( "A simple web application" ).installerName( "bash" );
		ComponentHelpers.insertChild( tomcatComponent, warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/invalid/instanceof-name-conflict-with-count.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs );
		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_CONFLICTING_INFERRED_INSTANCE, fromDef.getErrors().iterator().next().getErrorCode());

		// The assertions
		Application app = new Application();
		app.getRootInstances().addAll( rootInstances );

		Assert.assertEquals( 5, rootInstances.size());
		// Should be 6, but there two have the same path.
		// So, one is overriding the other.
	}


	@Test
	public void testRuntimeData() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "vm" ).alias( "VM" ).installerName( "iaas" );
		graphs.getRootComponents().add( vmComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/single-runtime-instance.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		// The assertions
		Assert.assertEquals( 1, rootInstances.size());
		Instance instance = rootInstances.iterator().next();

		Assert.assertEquals( "vm 1", instance.getName());
		Assert.assertEquals( vmComponent, instance.getComponent());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, instance.getStatus());
		Assert.assertEquals( 3, instance.getData().size());
		Assert.assertEquals( "127.0.0.1", instance.getData().get( "ip" ));
		Assert.assertEquals( "mach-ID", instance.getData().get( "machine-id" ));
		Assert.assertEquals( "something different", instance.getData().get( "whatever" ));
	}
}
