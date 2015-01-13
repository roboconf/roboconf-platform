/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.ParsingModelValidator;
import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.dsl.converters.FromInstanceDefinition;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelValidatorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testComponent() {

		Component comp = new Component();
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_COMPONENT_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_EMPTY_COMPONENT_INSTALLER, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.setName( "my # component" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_COMPONENT_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_EMPTY_COMPONENT_INSTALLER, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.setName( "my.component" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_DOT_IS_NOT_ALLOWED, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_EMPTY_COMPONENT_INSTALLER, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.setName( "comp" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_COMPONENT_INSTALLER, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.setInstallerName( "my installer !!" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_COMPONENT_INSTALLER, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.setInstallerName( "my installer" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_ROOT_INSTALLER_MUST_BE_TARGET, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.setInstallerName( Constants.TARGET_INSTALLER );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		comp.associateFacet( new Facet( "" ));
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		iterator = RuntimeModelValidator.validate( comp.getFacets().iterator().next()).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_FACET_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.disassociateFacet( comp.getFacets().iterator().next());
		comp.associateFacet( new Facet( "!nvalid-facet-n@me" ));
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		iterator = RuntimeModelValidator.validate( comp.getFacets().iterator().next()).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_FACET_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.disassociateFacet( comp.getFacets().iterator().next());
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		comp.exportedVariables.put( "comp.ip", null );
		comp.exportedVariables.put( "comp.port", "9000" );
		comp.importedVariables.put( "comp.ip", Boolean.FALSE );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.importedVariables.put( "comp.ip", Boolean.TRUE );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		comp.importedVariables.put( "", Boolean.FALSE );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		comp.importedVariables.clear();

		comp.importedVariables.put( "comp.inva!id", Boolean.FALSE );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		comp.importedVariables.clear();

		comp.exportedVariables.put( "toto", "" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_VARIABLE_VALUE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		comp.exportedVariables.clear();

		comp.exportedVariables.put( "toto.ip", "" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertFalse( iterator.hasNext());
		comp.exportedVariables.clear();
	}


	@Test
	public void testFacet() {

		Facet facet = new Facet();
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_FACET_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		facet.setName( "my # facet" );
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_FACET_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		facet.setName( "my.facet" );
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertEquals( ErrorCode.RM_DOT_IS_NOT_ALLOWED, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		facet.setName( "facet" );
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertFalse( iterator.hasNext());

		facet.exportedVariables.put( "", "value" );
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		facet.exportedVariables.clear();

		facet.exportedVariables.put( "facet.inva!id", "value" );
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		facet.exportedVariables.clear();

		facet.exportedVariables.put( "toto", "" );
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_VARIABLE_VALUE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		facet.exportedVariables.clear();

		facet.exportedVariables.put( "toto.ip", "" );
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertFalse( iterator.hasNext());
		facet.exportedVariables.clear();
	}


	@Test
	public void testCycleInComponentInheritance() {

		Component c1 = new Component( "c1" ).installerName( Constants.TARGET_INSTALLER );
		Component c2 = new Component( "c2" ).installerName( Constants.TARGET_INSTALLER );
		Component c3 = new Component( "c3" ).installerName( Constants.TARGET_INSTALLER );
		Component c4 = new Component( "c4" ).installerName( Constants.TARGET_INSTALLER );

		c1.extendComponent( c2 );
		c2.extendComponent( c3 );
		c3.extendComponent( c4 );

		Assert.assertEquals( 0, RuntimeModelValidator.validate( c1 ).size());
		Assert.assertEquals( 0, RuntimeModelValidator.validate( c2 ).size());
		Assert.assertEquals( 0, RuntimeModelValidator.validate( c3 ).size());
		Assert.assertEquals( 0, RuntimeModelValidator.validate( c4 ).size());

		c4.extendComponent( c1 );
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( c1 ).iterator();
		Assert.assertEquals( ErrorCode.RM_CYCLE_IN_COMPONENTS_INHERITANCE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		iterator = RuntimeModelValidator.validate( c1 ).iterator();
		Assert.assertEquals( ErrorCode.RM_CYCLE_IN_COMPONENTS_INHERITANCE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		iterator = RuntimeModelValidator.validate( c2 ).iterator();
		Assert.assertEquals( ErrorCode.RM_CYCLE_IN_COMPONENTS_INHERITANCE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		iterator = RuntimeModelValidator.validate( c4 ).iterator();
		Assert.assertEquals( ErrorCode.RM_CYCLE_IN_COMPONENTS_INHERITANCE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		c1.extendComponent( c1 );
		iterator = RuntimeModelValidator.validate( c1 ).iterator();
		Assert.assertEquals( ErrorCode.RM_CYCLE_IN_COMPONENTS_INHERITANCE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testGraphs() {

		Graphs graphs = new Graphs();
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_NO_ROOT_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Component comp1 = new Component( "comp1" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( comp1 );

		Component duplicateComp1 = new Component( "comp1" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( duplicateComp1 );

		// The validator checks something that cannot happen for the moment.
		// But we must keep it to prevent regressions.
		iterator = RuntimeModelValidator.validate( graphs ).iterator();
		if( iterator.hasNext()) {
			//Assert.assertEquals( ErrorCode.RM_DUPLICATE_COMPONENT, iterator.next().getErrorCode());
			Assert.assertFalse( iterator.hasNext());
		}

		// Unresolvable variable
		graphs.getRootComponents().clear();
		graphs.getRootComponents().add( comp1 );
		comp1.importedVariables.put( "tomcat.port", Boolean.FALSE );
		iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp1.importedVariables.put( "tomcat.port", Boolean.TRUE );
		iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		comp1.importedVariables.clear();

		// Test for loops: comp2 -> comp3 -> comp2
		Component comp2 = new Component( "comp2" ).installerName( "installer-2" );
		Component comp3 = new Component( "comp3" ).installerName( "installer-3" );
		comp1.addChild( comp2 );
		comp2.addChild( comp3 );
		comp3.addChild( comp2 );

		Collection<RoboconfError> errors = RuntimeModelValidator.validate( graphs );
		Assert.assertEquals( 2, errors.size());
		for( RoboconfError error: errors )
			Assert.assertEquals( ErrorCode.RM_CYCLE_IN_COMPONENTS, error.getErrorCode());
	}


	@Test
	public void testGraphs_notRoot() {

		Component comp1 = new Component( "comp1" ).installerName( Constants.TARGET_INSTALLER );
		Component comp2 = new Component( "comp2" ).installerName( Constants.TARGET_INSTALLER );
		comp1.addChild( comp2 );

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( comp2 );
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_NOT_A_ROOT_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testInstance() {

		Instance inst = new Instance();
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( inst ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_INSTANCE_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_EMPTY_INSTANCE_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		inst.setName( "?my instance?" );
		iterator = RuntimeModelValidator.validate( inst ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_INSTANCE_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_EMPTY_INSTANCE_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		inst.setName( "my-instance" );
		iterator = RuntimeModelValidator.validate( inst ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_INSTANCE_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		inst.setComponent( new Component( "comp" ));
		Assert.assertEquals( 0, RuntimeModelValidator.validate( inst ).size());

		inst.setName( "my instance" );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( inst ).size());

		inst.overriddenExports.put( "inst.value", "whatever" );
		iterator = RuntimeModelValidator.validate( inst ).iterator();
		Assert.assertEquals( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testInstances() {

		List<Instance> instances = new ArrayList<Instance> ();
		for( int i=0; i<10; i++ ) {
			Instance inst = new Instance( "inst-" + i ).component( new Component( "comp" ));
			instances.add( inst );
		}

		Assert.assertEquals( 0, RuntimeModelValidator.validate( instances ).size());
	}


	@Test
	public void testApplication() {

		Application app = new Application();
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_QUALIFIER, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setName( "My Application #!" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_QUALIFIER, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setQualifier( "Snapshot Build #2401" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setGraphs( new Graphs());
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_NO_ROOT_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Component comp = new Component( "root" ).installerName( Constants.TARGET_INSTALLER );
		app.getGraphs().getRootComponents().add( comp );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( app ).size());
	}


	@Test
	public void testApplicationDescriptor() {

		ApplicationDescriptor desc = new ApplicationDescriptor();
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_QUALIFIER, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_NAMESPACE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setName( "My Application #!" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_QUALIFIER, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_NAMESPACE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setQualifier( "Snapshot Build #2401" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_NAMESPACE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setNamespace( "net.roboconf" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setDslId( "roboconf-1.0" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setGraphEntryPoint( "graph.graph" );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( desc ).size());
	}


	@Test
	public void testSelfImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-self-imports.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testExportedVariableNames() throws Exception {

		Component component = new Component( "my-component" ).installerName( Constants.TARGET_INSTALLER );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( component ).size());

		component.exportedVariables.put( "ip", null );
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( component ).iterator();
		//Assert.assertEquals( ErrorCode.RM_INVALID_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.exportedVariables.clear();
		component.exportedVariables.put( "my-component.ip", null );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( component ).size());

		component.exportedVariables.put( "ip", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		//Assert.assertEquals( ErrorCode.RM_INVALID_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.exportedVariables.clear();
		component.exportedVariables.put( "my-component.ip", null );
		component.exportedVariables.put( "another-prefix.ip", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertFalse( iterator.hasNext());

		component.associateFacet( new Facet( "my-facet" ));
		Assert.assertEquals( 0, RuntimeModelValidator.validate( component ).size());

		component.exportedVariables.clear();
		component.exportedVariables.put( "my-component.@", "yo" );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.exportedVariables.clear();
		component.exportedVariables.put( "my-component.inva!id", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.exportedVariables.clear();
		component.exportedVariables.put( "", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testInvalidChildInstance_1() throws Exception {

		Component vmComponent1 = new Component( "VM_type1" ).installerName( Constants.TARGET_INSTALLER );
		Component vmComponent2 = new Component( "VM_type2" ).installerName( Constants.TARGET_INSTALLER );
		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		vmComponent1.addChild( tomcatComponent );
		vmComponent2.addChild( tomcatComponent );

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent1 );
		graphs.getRootComponents().add( vmComponent2 );

		// We cannot instantiate a VM under a VM
		Instance vmInstance1 = new Instance("vm1" ).component( vmComponent1 );
		Instance vmInstance2 = new Instance("vm2" ).component( vmComponent1 );
		InstanceHelpers.insertChild( vmInstance1, vmInstance2 );

		Application app = new Application( "app" ).qualifier( "snapshot" ).graphs( graphs );
		app.getRootInstances().add( vmInstance1 );

		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_INSTANCE_PARENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		// We cannot have a Tomcat as a root instance
		Instance tomcatInstance = new Instance("tomcat" ).component( tomcatComponent );
		app.getRootInstances().clear();
		app.getRootInstances().add( tomcatInstance );

		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_INSTANCE_PARENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		// We can insert a Tomcat under a VM
		vmInstance1.getChildren().clear();
		InstanceHelpers.insertChild( vmInstance1, tomcatInstance );
		app.getRootInstances().clear();
		app.getRootInstances().add( vmInstance1 );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( app ).size());
	}


	@Test
	public void testInvalidChildInstance_2() throws Exception {

		Component vmComponent = new Component( "VM" ).installerName( "target" );
		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		// We cannot instantiate a VM under a VM
		Instance vmInstance = new Instance("vm" ).component( vmComponent );
		Instance tomcatInstance = new Instance("tomcat" ).component( tomcatComponent );
		InstanceHelpers.insertChild( vmInstance, tomcatInstance );

		Application app = new Application( "app" ).qualifier( "snapshot" ).graphs( graphs );
		app.getRootInstances().add( vmInstance );

		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_INSTANCE_PARENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testTargetInstaller() throws Exception {

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( new Component( "VM" ).installerName( "target" ));

		File appDir = this.folder.newFolder();
		Collection<RoboconfError> errors = RuntimeModelValidator.validate( graphs, appDir );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.iterator().next().getErrorCode());

		File componentDir = new File( appDir, Constants.PROJECT_DIR_GRAPH + "/VM" );
		Assert.assertTrue( componentDir.mkdirs());

		errors = RuntimeModelValidator.validate( graphs, appDir );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_TARGET_PROPERTIES, errors.iterator().next().getErrorCode());

		File targetPropertiesFile = new File( componentDir, Constants.TARGET_PROPERTIES_FILE_NAME );
		Assert.assertTrue( targetPropertiesFile.createNewFile());
		errors = RuntimeModelValidator.validate( graphs, appDir );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testAnalyzeOverriddenExport() {

		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.exportedVariables.put( "Tomcat.ip", null );
		tomcatComponent.exportedVariables.put( "Tomcat.port", "8080" );

		Instance tomcatInstance = new Instance( "tomcat" ).component( tomcatComponent );
		Collection<RoboconfError> errors = RuntimeModelValidator.validate( tomcatInstance );
		Assert.assertEquals( 0, errors.size());

		tomcatInstance.overriddenExports.put( "Tomcat.port", "whatever" );
		errors = RuntimeModelValidator.validate( tomcatInstance );
		Assert.assertEquals( 0, errors.size());

		tomcatInstance.overriddenExports.put( "oops", "whatever" );
		errors = RuntimeModelValidator.validate( tomcatInstance );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, errors.iterator().next().getErrorCode());

		tomcatInstance.overriddenExports.remove( "oops" );
		tomcatInstance.overriddenExports.put( "port", "whatever" );
		errors = RuntimeModelValidator.validate( tomcatInstance );
		Assert.assertEquals( 0, errors.size());

		tomcatInstance.overriddenExports.remove( "Tomcat.port" );
		errors = RuntimeModelValidator.validate( tomcatInstance );
		Assert.assertEquals( 0, errors.size());

		Facet facet = new Facet( "facet" );
		facet.exportedVariables.put( "ip", null );
		tomcatComponent.associateFacet( facet );

		errors = RuntimeModelValidator.validate( tomcatInstance );
		Assert.assertEquals( 0, errors.size());

		tomcatInstance.overriddenExports.put( "ip", "localhost" );
		errors = RuntimeModelValidator.validate( tomcatInstance );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.RM_AMBIGUOUS_OVERRIDING, errors.iterator().next().getErrorCode());

		tomcatInstance.overriddenExports.remove( "ip" );
		tomcatInstance.overriddenExports.put( "facet.ip", "localhost" );
		errors = RuntimeModelValidator.validate( tomcatInstance );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testOverriddenExports() throws Exception {

		Component vmComponent = new Component( "VM" ).installerName( "target" );
		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.exportedVariables.put( "Tomcat.ip", null );
		tomcatComponent.exportedVariables.put( "Tomcat.port", "8080" );

		vmComponent.addChild( tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/valid/instance-overridden-exports.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Assert.assertEquals( 1, rootInstances.size());
		Instance rootInstance = rootInstances.iterator().next();
		Collection<Instance> instances = InstanceHelpers.buildHierarchicalList( rootInstance );
		Assert.assertEquals( 2, instances.size());

		Collection<RoboconfError> errors = RuntimeModelValidator.validate( instances );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testCycleInFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/cycle-in-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Collection<RoboconfError> errors = RuntimeModelValidator.validate( graphs );
		Assert.assertEquals( 3, errors.size());
		Set<String> messages = new HashSet<String> ();
		for( RoboconfError error : errors ) {
			Assert.assertEquals( ErrorCode.RM_CYCLE_IN_FACETS_INHERITANCE, error.getErrorCode());
			messages.add( error.getDetails());
		}

		Assert.assertEquals( 3, messages.size());
	}


	@Test
	public void testWildcardImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/graph-with-wildcards.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Collection<RoboconfError> errors = RuntimeModelValidator.validate( graphs );
		Assert.assertEquals( 0, errors.size());

		Component component = ComponentHelpers.findComponent( graphs, "app" );
		Assert.assertNotNull( component );

		Map<String,String> exports = ComponentHelpers.findAllExportedVariables( component );
		Assert.assertEquals( 2, exports.size());
		Assert.assertNull( exports.get( "app.ip" ));
		Assert.assertEquals( "toto", exports.get( "app.port" ));

		Map<String,Boolean> imports = ComponentHelpers.findAllImportedVariables( component );
		Assert.assertEquals( 2, imports.size());
		Assert.assertEquals( Boolean.FALSE, imports.get( "database.*" ));
		Assert.assertEquals( Boolean.FALSE, imports.get( "f-messaging-2.*" ));
	}


	@Test
	public void testWildcardImports_withErrors() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/graph-with-wildcards-and-errors.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Collection<RoboconfError> errors = RuntimeModelValidator.validate( graphs );
		Assert.assertEquals( 1, errors.size());

		RoboconfError error = errors.iterator().next();
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, error.getErrorCode());
		Assert.assertTrue( error.getDetails().contains( "messaging.*" ));
	}
}
