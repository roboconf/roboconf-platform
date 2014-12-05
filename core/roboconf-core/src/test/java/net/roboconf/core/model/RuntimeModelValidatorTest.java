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

package net.roboconf.core.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.ParsingModelValidator;
import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
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

		comp.getMetadata().getFacetNames().add( "" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_FACET_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.getMetadata().getFacetNames().clear();
		comp.getMetadata().getFacetNames().add( "!nvalid-facet-n@me" );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_FACET_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.getMetadata().getFacetNames().clear();
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		comp.getExportedVariables().put( "comp.ip", null );
		comp.getExportedVariables().put( "comp.port", "9000" );
		comp.getImportedVariables().put( "comp.ip", Boolean.FALSE );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.getImportedVariables().put( "comp.ip", Boolean.TRUE );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		comp.getImportedVariables().put( "", Boolean.FALSE );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		comp.getImportedVariables().remove( "" );

		comp.getImportedVariables().put( "comp.inva!id", Boolean.FALSE );
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testCycleInComponentInheritance() {

		Component c1 = new Component( "c1" ).installerName( Constants.TARGET_INSTALLER );
		Component c2 = new Component( "c2" ).installerName( Constants.TARGET_INSTALLER );
		Component c3 = new Component( "c3" ).installerName( Constants.TARGET_INSTALLER );
		Component c4 = new Component( "c4" ).installerName( Constants.TARGET_INSTALLER );

		c1.getMetadata().setExtendedComponent( c2 );
		c2.getMetadata().setExtendedComponent( c3 );
		c3.getMetadata().setExtendedComponent( c4 );

		Assert.assertEquals( 0, RuntimeModelValidator.validate( c1 ).size());
		Assert.assertEquals( 0, RuntimeModelValidator.validate( c2 ).size());
		Assert.assertEquals( 0, RuntimeModelValidator.validate( c3 ).size());
		Assert.assertEquals( 0, RuntimeModelValidator.validate( c4 ).size());

		c4.getMetadata().setExtendedComponent( c1 );
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

		c1.getMetadata().setExtendedComponent( null );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( c1 ).size());

		c1.getMetadata().setExtendedComponent( c1 );
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
			Assert.assertEquals( ErrorCode.RM_DUPLICATE_COMPONENT, iterator.next().getErrorCode());
			Assert.assertFalse( iterator.hasNext());
		}

		// Unresolvable variable
		graphs.getRootComponents().clear();
		graphs.getRootComponents().add( comp1 );
		comp1.getImportedVariables().put( "tomcat.port", Boolean.FALSE );
		iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp1.getImportedVariables().put( "tomcat.port", Boolean.TRUE );
		iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		comp1.getImportedVariables().clear();

		// Test for loops
		Component comp2 = new Component( "comp2" ).installerName( "installer-2" );
		ComponentHelpers.insertChild( comp1, comp2 );
		ComponentHelpers.insertChild( comp2, comp1 );

		iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_CYCLE_IN_COMPONENTS, iterator.next().getErrorCode());
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

		inst.getOverriddenExports().put( "inst.value", "whatever" );
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

		component.getExportedVariables().put( "ip", null );
		Iterator<RoboconfError> iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.getExportedVariables().clear();
		component.getExportedVariables().put( "my-component.ip", null );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( component ).size());

		component.getExportedVariables().put( "ip", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.getExportedVariables().clear();
		component.getExportedVariables().put( "my-component.ip", null );
		component.getExportedVariables().put( "my-facet.ip", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.getMetadata().getFacetNames().add( "my-facet" );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( component ).size());

		component.getExportedVariables().clear();
		component.getExportedVariables().put( "my-component.", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_EXPORT_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.getExportedVariables().clear();
		component.getExportedVariables().put( "my-component.inva!id", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.getExportedVariables().clear();
		component.getExportedVariables().put( "", null );
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testInvalidChildInstance_1() throws Exception {

		Component vmComponent1 = new Component( "VM_type1" ).installerName( Constants.TARGET_INSTALLER );
		Component vmComponent2 = new Component( "VM_type2" ).installerName( Constants.TARGET_INSTALLER );
		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		ComponentHelpers.insertChild( vmComponent1, tomcatComponent );
		ComponentHelpers.insertChild( vmComponent2, tomcatComponent );

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
}
