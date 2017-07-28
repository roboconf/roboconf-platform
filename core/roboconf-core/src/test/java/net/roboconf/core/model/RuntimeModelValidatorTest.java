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

package net.roboconf.core.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.ParsingModelValidator;
import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.dsl.converters.FromInstanceDefinition;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails.ErrorDetailsKind;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelValidatorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testComponent() {

		Component comp = new Component();
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( comp ).iterator();
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

		comp.addExportedVariable( new ExportedVariable( "comp.ip", null ));
		comp.addExportedVariable( new ExportedVariable( "comp.port", "9000" ));
		comp.addImportedVariable( new ImportedVariable( "comp.ip", false, false ));
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp.addImportedVariable( new ImportedVariable( "comp.ip", true, false ));
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		comp.addImportedVariable( new ImportedVariable( "", false, false ));
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		comp.importedVariables.clear();

		comp.addImportedVariable( new ImportedVariable( "comp.inva!id", false, false ));
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		comp.importedVariables.clear();

		comp.addExportedVariable( new ExportedVariable( "toto", "" ));
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertFalse( iterator.hasNext());
		comp.exportedVariables.clear();

		comp.addExportedVariable( new ExportedVariable( "toto.ip", "" ));
		iterator = RuntimeModelValidator.validate( comp ).iterator();
		Assert.assertFalse( iterator.hasNext());
		comp.exportedVariables.clear();
	}


	@Test
	public void testFacet() {

		Facet facet = new Facet();
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( facet ).iterator();
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

		facet.addExportedVariable( new ExportedVariable( "", "value" ));
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		facet.exportedVariables.clear();

		facet.addExportedVariable( new ExportedVariable( "facet.inva!id", "value" ));
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		facet.exportedVariables.clear();

		facet.addExportedVariable( new ExportedVariable( "toto", "" ));
		iterator = RuntimeModelValidator.validate( facet ).iterator();
		Assert.assertFalse( iterator.hasNext());
		facet.exportedVariables.clear();

		facet.addExportedVariable( new ExportedVariable( "toto.ip", "" ));
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
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( c1 ).iterator();
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
	public void testGraphs_withCaseErrorInImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/graph-with-invalid-case-imports.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs g = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Collection<ModelError> errors = RuntimeModelValidator.validate( g );

		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testGraphs() {

		Graphs graphs = new Graphs();
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( graphs ).iterator();
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
		comp1.addImportedVariable( new ImportedVariable( "tomcat.port", false, false ));
		iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		comp1.addImportedVariable( new ImportedVariable( "tomcat.port", true, false ));
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

		Collection<ModelError> errors = RuntimeModelValidator.validate( graphs );
		Assert.assertEquals( 2, errors.size());
		for( ModelError error: errors )
			Assert.assertEquals( ErrorCode.RM_CYCLE_IN_COMPONENTS, error.getErrorCode());
	}


	@Test
	public void testGraphs_notRoot() {

		Component comp1 = new Component( "comp1" ).installerName( Constants.TARGET_INSTALLER );
		Component comp2 = new Component( "comp2" ).installerName( Constants.TARGET_INSTALLER );
		comp1.addChild( comp2 );

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( comp2 );
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_NOT_A_ROOT_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testInstance() {

		Instance inst = new Instance();
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( inst ).iterator();
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
	public void testInstance_noVariableValue() {

		// Create a valid instance and a valid component
		Instance inst = new Instance( "my instance" );
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( inst ).iterator();
		Assert.assertEquals( ErrorCode.RM_EMPTY_INSTANCE_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Component comp = new Component( "comp" ).installerName( "target" );
		comp.addExportedVariable( new ExportedVariable( "ip", "" ));
		comp.addExportedVariable( new ExportedVariable( "p1", "value1" ));
		comp.addExportedVariable( new ExportedVariable( "p2", "" ));
		Assert.assertEquals( 0, RuntimeModelValidator.validate( comp ).size());

		// Associate them together => variable without value
		inst.setComponent( comp );
		iterator = RuntimeModelValidator.validate( inst ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_VARIABLE_VALUE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		// Fix it and make sure validation succeeds
		inst.overriddenExports.put( "inst.value1", "whatever" );
		inst.overriddenExports.put( "comp.p2", "a default value" );
		iterator = RuntimeModelValidator.validate( inst ).iterator();
		Assert.assertEquals( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		// Define a new instance variable without value => error
		inst.overriddenExports.put( "something else", "" );
		iterator = RuntimeModelValidator.validate( inst ).iterator();
		Assert.assertEquals( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_VARIABLE_VALUE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testInstances() {

		List<Instance> instances = new ArrayList<> ();
		for( int i=0; i<10; i++ ) {
			Instance inst = new Instance( "inst-" + i ).component( new Component( "comp" ));
			instances.add( inst );
		}

		Assert.assertEquals( 0, RuntimeModelValidator.validate( instances ).size());
	}


	@Test
	public void testApplication() {

		ApplicationTemplate app = new ApplicationTemplate();
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setName( "My Application #!" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setName( "My Application (test)" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setName( "My Application" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setVersion( "Snapshot Build #2401" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setVersion( "3.2.4" );
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

		app.externalExports.put( "comp.!nvalid", "ko" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_INVALID_EXTERNAL_EXPORT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.externalExports.remove( "comp.!nvalid" );
		app.externalExports.put( "comp.valid", "!invalid" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_INVALID_EXTERNAL_EXPORT, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.externalExports.put( "comp.valid", "ok" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_INVALID_EXTERNAL_EXPORT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
		app.externalExports.remove( "comp.valid" );

		comp.addExportedVariable( new ExportedVariable( "test", "default" ));
		app.externalExports.put( "root.test", "alias" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setExternalExportsPrefix( "inval!d prefix" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		app.setExternalExportsPrefix( "prefix" );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( app ).size());

		comp.addExportedVariable( new ExportedVariable( "test-bis", "default" ));
		app.externalExports.put( "root.test-bis", "alias" );
		iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_ALREADY_DEFINED_EXTERNAL_EXPORT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testApplicationDescriptor() {

		ApplicationTemplateDescriptor desc = new ApplicationTemplateDescriptor();
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setName( "My Application #!" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setName( "My Àppliçation" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setName( "My Application" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setVersion( "Snapshot Build #2401" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setDslId( "roboconf-1.0" );
		desc.setVersion( "1.4" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setGraphEntryPoint( "graph.graph" );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( desc ).size());

		desc.invalidExternalExports.add( "oops" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.PROJ_INVALID_EXTERNAL_EXPORTS, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.invalidExternalExports.clear();
		desc.externalExports.put( "comp.valid", "ok" );
		desc.externalExports.put( "comp.!nvalid", "ko" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.externalExports.remove( "comp.!nvalid" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		desc.setExternalExportsPrefix( "prefix" );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( desc ).size());

		desc.externalExports.put( "comp.valid", "!nvalid" );
		iterator = RuntimeModelValidator.validate( desc ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testSelfImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-self-imports.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ParsingError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Iterator<ModelError> iterator = RuntimeModelValidator.validate( graphs ).iterator();
		Assert.assertEquals( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testExportedVariableNames() throws Exception {

		Component component = new Component( "my-component" ).installerName( Constants.TARGET_INSTALLER );
		Assert.assertEquals( 0, RuntimeModelValidator.validate( component ).size());

		component.addExportedVariable( new ExportedVariable( "ip", null ));
		Iterator<ModelError> iterator = RuntimeModelValidator.validate( component ).iterator();
		//Assert.assertEquals( ErrorCode.RM_INVALID_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.exportedVariables.clear();
		component.addExportedVariable( new ExportedVariable( "my-component.ip", null ));
		Assert.assertEquals( 0, RuntimeModelValidator.validate( component ).size());

		component.addExportedVariable( new ExportedVariable( "ip", null ));
		iterator = RuntimeModelValidator.validate( component ).iterator();
		//Assert.assertEquals( ErrorCode.RM_INVALID_EXPORT_PREFIX, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.exportedVariables.clear();
		component.addExportedVariable( new ExportedVariable( "my-component.ip", null ));
		component.addExportedVariable( new ExportedVariable( "another-prefix.ip", null ));
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertFalse( iterator.hasNext());

		component.associateFacet( new Facet( "my-facet" ));
		Assert.assertEquals( 0, RuntimeModelValidator.validate( component ).size());

		component.exportedVariables.clear();
		component.addExportedVariable( new ExportedVariable( "my-component.@", "yo" ));
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.exportedVariables.clear();
		component.addExportedVariable( new ExportedVariable( "my-component.inva!id", null ));
		iterator = RuntimeModelValidator.validate( component ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_VARIABLE_NAME, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		component.exportedVariables.clear();
		component.addExportedVariable( new ExportedVariable( "", null ));
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

		ApplicationTemplate app = new ApplicationTemplate( "app" ).version( "2.4" ).graphs( graphs );
		app.getRootInstances().add( vmInstance1 );

		Iterator<ModelError> iterator = RuntimeModelValidator.validate( app ).iterator();
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

		ApplicationTemplate app = new ApplicationTemplate( "app" ).version( "2.4" ).graphs( graphs );
		app.getRootInstances().add( vmInstance );

		Iterator<ModelError> iterator = RuntimeModelValidator.validate( app ).iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_INSTANCE_PARENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testTargetInstaller() throws Exception {

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( new Component( "VM" ).installerName( "target" ));

		File appDir = this.folder.newFolder();
		Collection<ModelError> errors = RuntimeModelValidator.validate( graphs, appDir );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.iterator().next().getErrorCode());

		File componentDir = new File( appDir, Constants.PROJECT_DIR_GRAPH + "/VM" );
		Assert.assertTrue( componentDir.mkdirs());

		// No target.properties => no error
		errors = RuntimeModelValidator.validate( graphs, appDir );
		Assert.assertEquals( 0, errors.size());

		// A target.properties is present => no error
		File targetPropertiesFile = new File( componentDir, Constants.TARGET_PROPERTIES_FILE_NAME );
		Assert.assertTrue( targetPropertiesFile.createNewFile());
		errors = RuntimeModelValidator.validate( graphs, appDir );
		Assert.assertEquals( 3, errors.size());

		Iterator<ModelError> it = errors.iterator();
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_ID, it.next().getErrorCode());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_HANDLER, it.next().getErrorCode());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_NAME, it.next().getErrorCode());

		// Add the required properties
		Utils.writeStringInto( "id: tid\nhandler: test\nname: n", targetPropertiesFile );
		errors = RuntimeModelValidator.validate( graphs, appDir );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testAnalyzeOverriddenExport() {

		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.port", "8080" ));

		Instance tomcatInstance = new Instance( "tomcat" ).component( tomcatComponent );
		Collection<ModelError> errors = RuntimeModelValidator.validate( tomcatInstance );
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
		facet.addExportedVariable( new ExportedVariable( "ip", null ));
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
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.port", "8080" ));

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

		Collection<ModelError> errors = RuntimeModelValidator.validate( instances );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testCycleInFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/cycle-in-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Collection<ModelError> errors = RuntimeModelValidator.validate( graphs );
		Assert.assertEquals( 5, errors.size());
		RoboconfErrorHelpers.filterErrorsForRecipes( errors );
		Assert.assertEquals( 3, errors.size());

		for( ModelError error : errors ) {
			Assert.assertEquals( ErrorCode.RM_CYCLE_IN_FACETS_INHERITANCE, error.getErrorCode());
			Assert.assertEquals( 1, error.getDetails().length );
		}
	}


	@Test
	public void testWildcardImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/graph-with-wildcards.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Collection<ModelError> errors = RuntimeModelValidator.validate( graphs );
		Assert.assertEquals( 0, errors.size());

		Component component = ComponentHelpers.findComponent( graphs, "app" );
		Assert.assertNotNull( component );

		Map<String,String> exports = ComponentHelpers.findAllExportedVariables( component );
		Assert.assertEquals( 2, exports.size());
		Assert.assertNull( exports.get( "app.ip" ));
		Assert.assertEquals( "toto", exports.get( "app.port" ));

		Map<String,ImportedVariable> imports = ComponentHelpers.findAllImportedVariables( component );
		Assert.assertEquals( 2, imports.size());
		Assert.assertNotNull( imports.get( "database.*" ));
		Assert.assertFalse( imports.get( "database.*" ).isOptional());
		Assert.assertNotNull( imports.get( "f-messaging-2.*" ));
		Assert.assertFalse( imports.get( "f-messaging-2.*" ).isOptional());
	}


	@Test
	public void testWildcardImports_withErrors() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/graph-with-wildcards-and-errors.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Collection<ModelError> errors = RuntimeModelValidator.validate( graphs );
		Assert.assertEquals( 1, errors.size());

		ModelError error = errors.iterator().next();
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, error.getErrorCode());
		Assert.assertEquals( new Component( "app" ), error.getModelObject());

		Assert.assertEquals( 1, error.getDetails().length );
		Assert.assertEquals( ErrorDetailsKind.VARIABLE, error.getDetails()[ 0 ].getErrorDetailsKind());
		Assert.assertTrue( error.getDetails()[ 0 ].getElementName().contains( "messaging.*" ));
	}


	@Test
	public void testInvalidRandomPort() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-with-invalid-random-port.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs g = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Collection<ModelError> errors = RuntimeModelValidator.validate( g );
		Assert.assertEquals( 1, errors.size());

		ModelError error = errors.iterator().next();
		Assert.assertEquals( ErrorCode.RM_INVALID_RANDOM_KIND, error.getErrorCode());
		Assert.assertEquals( new Component( "comp2" ), error.getModelObject());

		Assert.assertEquals( 1, error.getDetails().length );
		Assert.assertEquals( ErrorDetailsKind.UNRECOGNIZED, error.getDetails()[ 0 ].getErrorDetailsKind());
		Assert.assertTrue( error.getDetails()[ 0 ].getElementName().contains( "string" ));
	}


	@Test
	public void testInvalidRandomPortWithValue() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-with-invalid-random-port-value.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs g = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Collection<ModelError> errors = RuntimeModelValidator.validate( g );
		Assert.assertEquals( 1, errors.size());

		ModelError error = errors.iterator().next();
		Assert.assertEquals( ErrorCode.RM_NO_VALUE_FOR_RANDOM, error.getErrorCode());
		Assert.assertEquals( new Component( "comp2" ), error.getModelObject());

		Assert.assertEquals( 1, error.getDetails().length );
		Assert.assertEquals( ErrorDetailsKind.VARIABLE, error.getDetails()[ 0 ].getErrorDetailsKind());
		Assert.assertTrue( error.getDetails()[ 0 ].getElementName().contains( "httpPort" ));
	}


	@Test
	public void testValidRandomPort() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/component-with-random-ports.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs g = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Collection<ModelError> errors = RuntimeModelValidator.validate( g );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testUnreachableComponent() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/facet-without-component.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs g = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		List<ModelError> errors = new ArrayList<>( RuntimeModelValidator.validate( g ));
		Assert.assertEquals( 3, errors.size());

		Assert.assertEquals( ErrorCode.RM_ROOT_INSTALLER_MUST_BE_TARGET, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( new Component( "t" ), errors.get( 0 ).getModelObject());

		Assert.assertEquals( ErrorCode.RM_ORPHAN_FACET_WITH_CHILDREN, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( new Facet( "f" ), errors.get( 1 ).getModelObject());

		Assert.assertEquals( ErrorCode.RM_UNREACHABLE_COMPONENT, errors.get( 2 ).getErrorCode());
		Assert.assertEquals( new Component( "t" ), errors.get( 2 ).getModelObject());
	}
}
