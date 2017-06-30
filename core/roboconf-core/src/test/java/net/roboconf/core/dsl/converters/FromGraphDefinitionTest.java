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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails.ErrorDetailsKind;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.RuntimeModelValidator;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.ExportedVariable.RandomKind;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.helpers.ComponentHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphDefinitionTest {

	@Test
	public void test_simpleInstaller() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/simple-installer.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( "my-own-installer", graphs.getRootComponents().iterator().next().getInstallerName());
	}


	@Test
	public void test_WithSpecialNames() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/special-names.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( 4, graphs.getRootComponents().size());

		List<String> componentNames = new ArrayList<> ();
		for( Component component : graphs.getRootComponents() ) {
			componentNames.add( component.getName());
		}

		Assert.assertTrue( componentNames.contains( "ImportingComponent" ));
		Assert.assertTrue( componentNames.contains( "ExportingComponent" ));
		Assert.assertTrue( componentNames.contains( "FacetComponent" ));
		Assert.assertTrue( componentNames.contains( "InstanceOfComponent" ));
	}


	@Test
	public void test_complexVariablesValues() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/component-with-complex-variables-values.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( 1, graphs.getRootComponents().size());
		Component comp = graphs.getRootComponents().iterator().next();
		Assert.assertEquals( 6, comp.exportedVariables.size());

		ExportedVariable var = comp.exportedVariables.get( "key1" );
		Assert.assertEquals( "value1", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = comp.exportedVariables.get( "key2" );
		Assert.assertEquals( "value2", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = comp.exportedVariables.get( "key3" );
		Assert.assertEquals( "this is key number 3", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = comp.exportedVariables.get( "key4" );
		Assert.assertEquals( "key4", var.getValue());
		Assert.assertTrue( var.isRandom());
		Assert.assertEquals( RandomKind.PORT, var.getRandomKind());

		var = comp.exportedVariables.get( "key5" );
		Assert.assertEquals( "key5", var.getValue());
		Assert.assertFalse( var.isRandom());

		var = comp.exportedVariables.get( "key6" );
		Assert.assertEquals( " key; 6 ", var.getValue());
		Assert.assertTrue( var.isRandom());
		Assert.assertEquals( RandomKind.PORT, var.getRandomKind());
	}


	@Test
	public void test_duplicateInstaller() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-duplicate-property.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.PM_DUPLICATE_PROPERTY, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void test_conflictingNames() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/conflicting-names.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Iterator<ParsingError> it = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_CONFLICTING_NAME, it.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_CONFLICTING_NAME, it.next().getErrorCode());
		Assert.assertFalse( it.hasNext());

		Assert.assertEquals( "type", graphs.getRootComponents().iterator().next().getName());
	}


	@Test
	public void test_inexistingChildInComponent() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/inexisting-child-in-component.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Iterator<ParsingError> it = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, it.next().getErrorCode());
		Assert.assertFalse( it.hasNext());

		Assert.assertEquals( "root", graphs.getRootComponents().iterator().next().getName());
	}


	@Test
	public void test_inexistingChildInFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/inexisting-child-in-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Iterator<ParsingError> it = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, it.next().getErrorCode());
		Assert.assertFalse( it.hasNext());

		Assert.assertEquals( 0, graphs.getRootComponents().size());
	}


	@Test
	public void testGraphWithWrongImport() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/graph-with-invalid-import.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Iterator<ParsingError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_UNREACHABLE_FILE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testDuplicateComponent() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-component.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 2, fromDef.getErrors().size());
		for( ParsingError error : fromDef.getErrors())
			Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_COMPONENT, error.getErrorCode());
	}


	@Test
	public void testDuplicateFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 3, fromDef.getErrors().size());
		for( ParsingError error : fromDef.getErrors())
			Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_FACET, error.getErrorCode());
	}


	@Test
	public void testUnresolvedFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/unresolved-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_INEXISTING_FACET, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testUnresolvedExtendedFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/unresolved-extended-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_INEXISTING_FACET, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testUnresolvableFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/unresolvable-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_INEXISTING_FACET, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testSelfOptionalImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/component-optional-imports.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());

		Component componentA = ComponentHelpers.findComponent( graphs, "A" );
		Assert.assertTrue( componentA.exportedVariables.containsKey( "port" ));
		Assert.assertTrue( componentA.exportedVariables.containsKey( "ip" ));

		Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( componentA );
		Assert.assertTrue( exportedVariables.containsKey( "A.port" ));
		Assert.assertTrue( exportedVariables.containsKey( "A.ip" ));

		ImportedVariable var = componentA.importedVariables.get( "A.port" );
		Assert.assertNotNull( var );
		Assert.assertTrue( var.isOptional());

		var = componentA.importedVariables.get( "A.ip" );
		Assert.assertNotNull( var );
		Assert.assertTrue( var.isOptional());
	}


	@Test
	public void testComplexHierarchy() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/complex-hierarchy.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( 0, RuntimeModelValidator.validate( graphs ).size());

		Component root = ComponentHelpers.findComponent( graphs, "root" );
		Assert.assertNotNull( root );

		Collection<Component> ancestors = ComponentHelpers.findAllAncestors( root );
		Assert.assertEquals( 0, ancestors.size());

		Collection<Component> children = ComponentHelpers.findAllChildren( root );
		Assert.assertEquals( 1, children.size());

		Component tomcat = children.iterator().next();
		Assert.assertEquals( "Tomcat", tomcat.getName());

		ancestors = ComponentHelpers.findAllAncestors( tomcat );
		Assert.assertEquals( 1, ancestors.size());
		Assert.assertEquals( root, ancestors.iterator().next());

		children = ComponentHelpers.findAllChildren( tomcat );
		Assert.assertEquals( 1, children.size());

		Component app = children.iterator().next();
		Assert.assertEquals( "App", app.getName());
	}


	@Test
	public void testIDsWithSpaces() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/real-lamp-all-in-one-flex.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 7, fromDef.getErrors().size());
		for( ParsingError error : fromDef.getErrors()) {
			boolean a1 = ErrorCode.PM_INVALID_NAME == error.getErrorCode();
			boolean a2 = ErrorCode.PM_INVALID_CHILD_NAME == error.getErrorCode();
			Assert.assertTrue( error.getErrorCode().name(), a1 || a2 );
		}

		Assert.assertEquals( 0, graphs.getRootComponents().size());
		Assert.assertEquals( 0, graphs.getFacetNameToFacet().size());
	}


	@Test
	public void testInexistingChildren_components() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-inexisting-children.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 2, fromDef.getErrors().size());

		ParsingError[] errors = fromDef.getErrors().toArray( new ParsingError[ 2 ]);
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 0 ].getErrorCode());
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 1 ].getErrorCode());
	}


	@Test
	public void testInexistingChildren_facets() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-inexisting-child-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());

		ParsingError[] errors = fromDef.getErrors().toArray( new ParsingError[ 1 ]);
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 0 ].getErrorCode());
		Assert.assertEquals( 3, errors[ 0 ].getDetails().length );

		Assert.assertEquals( "Fa3", errors[ 0 ].getDetails()[ 0 ].getElementName());
		Assert.assertEquals( ErrorDetailsKind.NAME, errors[ 0 ].getDetails()[ 0 ].getErrorDetailsKind());
		Assert.assertEquals( ErrorDetailsKind.FILE, errors[ 0 ].getDetails()[ 1 ].getErrorDetailsKind());
		Assert.assertEquals( ErrorDetailsKind.LINE, errors[ 0 ].getDetails()[ 2 ].getErrorDetailsKind());
	}


	@Test
	public void testInexistingExtendedComponent() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-extends-inexisting-component.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());

		ParsingError[] errors = fromDef.getErrors().toArray( new ParsingError[ 1 ]);
		Assert.assertEquals( ErrorCode.CO_INEXISTING_COMPONENT, errors[ 0 ].getErrorCode());
	}


	@Test
	public void testExternalImport() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/component-external-imports.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());

		Component componentA = ComponentHelpers.findComponent( graphs, "A" );
		Assert.assertTrue( componentA.exportedVariables.containsKey( "port" ));
		Assert.assertTrue( componentA.exportedVariables.containsKey( "ip" ));

		Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( componentA );
		Assert.assertTrue( exportedVariables.containsKey( "A.port" ));
		Assert.assertTrue( exportedVariables.containsKey( "A.ip" ));

		ImportedVariable var = componentA.importedVariables.get( "A.port" );
		Assert.assertNotNull( var );
		Assert.assertTrue( var.isOptional());
		Assert.assertFalse( var.isExternal());

		var = componentA.importedVariables.get( "A.ip" );
		Assert.assertNotNull( var );
		Assert.assertTrue( var.isOptional());
		Assert.assertFalse( var.isExternal());

		var = componentA.importedVariables.get( "App.toto" );
		Assert.assertNotNull( var );
		Assert.assertFalse( var.isOptional());
		Assert.assertTrue( var.isExternal());

		var = componentA.importedVariables.get( "App2.ip" );
		Assert.assertNotNull( var );
		Assert.assertTrue( var.isOptional());
		Assert.assertTrue( var.isExternal());
	}


	@Test
	public void testExplodedExportsAndImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/only-component-4.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs g = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());

		Component tomcat = ComponentHelpers.findComponent( g, "tomcat" );
		Assert.assertNotNull( tomcat );

		Assert.assertEquals( 2, tomcat.exportedVariables.size());
		Assert.assertNotNull( tomcat.exportedVariables.get( "db.port" ));
		Assert.assertEquals( "8080", tomcat.exportedVariables.get( "db.port" ).getValue());
		Assert.assertNotNull( tomcat.exportedVariables.get( "db.ip" ));
		Assert.assertNull( tomcat.exportedVariables.get( "db.ip" ).getValue());

		Component apache = ComponentHelpers.findComponent( g, "apache" );
		Assert.assertNotNull( apache );

		Assert.assertEquals( 2, apache.importedVariables.size());
		Assert.assertNotNull( apache.importedVariables.get( "tomcat.port" ));
		Assert.assertTrue( apache.importedVariables.get( "tomcat.port" ).isOptional());

		Assert.assertNotNull( apache.importedVariables.get( "tomcat.ip" ));
		Assert.assertFalse( apache.importedVariables.get( "tomcat.ip" ).isOptional());
	}


	@Test
	public void testComponentsWithRandomPorts() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/component-with-random-ports.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs g = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());

		// Comp1
		Component comp1 = ComponentHelpers.findComponent( g, "comp1" );
		Assert.assertNotNull( comp1 );

		Assert.assertEquals( 2, comp1.exportedVariables.size());
		Assert.assertNotNull( comp1.exportedVariables.get( "ip" ));

		Assert.assertNotNull( comp1.exportedVariables.get( "port" ));
		Assert.assertNull( comp1.exportedVariables.get( "port" ).getValue());
		Assert.assertTrue( comp1.exportedVariables.get( "port" ).isRandom());
		Assert.assertEquals( RandomKind.PORT, comp1.exportedVariables.get( "port" ).getRandomKind());

		// Comp2
		Component comp2 = ComponentHelpers.findComponent( g, "comp2" );
		Assert.assertNotNull( comp2 );

		Assert.assertEquals( 3, comp2.exportedVariables.size());
		Assert.assertNotNull( comp2.exportedVariables.get( "ip" ));

		Assert.assertNotNull( comp2.exportedVariables.get( "httpPort" ));
		Assert.assertNull( comp2.exportedVariables.get( "httpPort" ).getValue());
		Assert.assertTrue( comp2.exportedVariables.get( "httpPort" ).isRandom());
		Assert.assertEquals( RandomKind.PORT, comp2.exportedVariables.get( "httpPort" ).getRandomKind());

		Assert.assertNotNull( comp2.exportedVariables.get( "ajpPort" ));
		Assert.assertNull( comp2.exportedVariables.get( "ajpPort" ).getValue());
		Assert.assertTrue( comp2.exportedVariables.get( "ajpPort" ).isRandom());
		Assert.assertEquals( RandomKind.PORT, comp2.exportedVariables.get( "ajpPort" ).getRandomKind());

		// Comp3
		Component comp3 = ComponentHelpers.findComponent( g, "comp3" );
		Assert.assertNotNull( comp3 );

		Assert.assertEquals( 2, comp3.exportedVariables.size());
		Assert.assertNotNull( comp3.exportedVariables.get( "ip" ));

		Assert.assertNotNull( comp3.exportedVariables.get( "ajpPort" ));
		Assert.assertEquals( "8959", comp3.exportedVariables.get( "ajpPort" ).getValue());
		Assert.assertFalse( comp3.exportedVariables.get( "ajpPort" ).isRandom());
		Assert.assertNull( comp3.exportedVariables.get( "ajpPort" ).getRandomKind());
	}


	@Test
	public void testComponentsWithInvalidRandomPorts() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-with-invalid-random-port.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs g = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Component comp2 = ComponentHelpers.findComponent( g, "comp2" );
		Assert.assertNotNull( comp2 );

		Assert.assertEquals( 3, comp2.exportedVariables.size());
		Assert.assertNotNull( comp2.exportedVariables.get( "ip" ));

		Assert.assertNotNull( comp2.exportedVariables.get( "httpPort" ));
		Assert.assertNull( comp2.exportedVariables.get( "httpPort" ).getValue());
		Assert.assertTrue( comp2.exportedVariables.get( "httpPort" ).isRandom());
		Assert.assertEquals( RandomKind.PORT, comp2.exportedVariables.get( "httpPort" ).getRandomKind());

		Assert.assertNotNull( comp2.exportedVariables.get( "ajpPort" ));
		Assert.assertNull( comp2.exportedVariables.get( "ajpPort" ).getValue());
		Assert.assertTrue( comp2.exportedVariables.get( "ajpPort" ).isRandom());
		Assert.assertNull( comp2.exportedVariables.get( "ajpPort" ).getRandomKind());
	}


	@Test
	public void test_brokenGraph_1() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/broken-graph-1.graph" );

		// Normal loading: errors and no built graph
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertNotSame( 0, fromDef.getErrors().size());
		Assert.assertEquals( 0, graphs.getRootComponents().size());
		Assert.assertEquals( 0, graphs.getFacetNameToFacet().size());

		// Flexible loading
		fromDef = new FromGraphDefinition( f.getParentFile(), true );
		graphs = fromDef.buildGraphs( f );

		Assert.assertNotSame( 0, fromDef.getErrors().size());
		Assert.assertEquals( 4, ComponentHelpers.findAllComponents( graphs ).size());
		Assert.assertEquals( 2, graphs.getFacetNameToFacet().size());

		// Verify facets
		Facet f1 = graphs.getFacetNameToFacet().get( "f1" );
		Assert.assertNotNull( f1 );
		Assert.assertEquals( 2, f1.exportedVariables.size());
		Assert.assertEquals( 0, f1.getChildren().size());

		Facet f2 = graphs.getFacetNameToFacet().get( "f2" );
		Assert.assertNotNull( f2 );
		Assert.assertEquals( 0, f2.exportedVariables.size());
		Assert.assertEquals( 0, f2.getChildren().size());

		// Verify components
		Component c1 = ComponentHelpers.findComponent( graphs, "c1" );
		Assert.assertNotNull( c1 );
		Assert.assertEquals( 2, c1.exportedVariables.size());
		Assert.assertEquals( 0, c1.importedVariables.size());
		Assert.assertEquals( 1, c1.getFacets().size());
		Assert.assertEquals( 0, c1.getChildren().size());

		Component c2 = ComponentHelpers.findComponent( graphs, "c2" );
		Assert.assertNotNull( c2 );
		Assert.assertEquals( 1, c2.exportedVariables.size());
		Assert.assertEquals( 0, c2.importedVariables.size());
		Assert.assertEquals( 0, c2.getFacets().size());
		Assert.assertEquals( 1, c2.getChildren().size());

		Component comp1 = ComponentHelpers.findComponent( graphs, "comp1" );
		Assert.assertNotNull( comp1 );
		Assert.assertEquals( 0, comp1.exportedVariables.size());
		Assert.assertEquals( 0, comp1.importedVariables.size());
		Assert.assertEquals( 0, comp1.getFacets().size());
		Assert.assertEquals( 0, comp1.getChildren().size());

		Component comp2 = ComponentHelpers.findComponent( graphs, "comp2" );
		Assert.assertNotNull( comp2 );
		Assert.assertEquals( 1, comp2.exportedVariables.size());
		Assert.assertEquals( 0, comp2.importedVariables.size());
		Assert.assertEquals( 0, comp2.getFacets().size());
		Assert.assertEquals( 0, comp2.getChildren().size());
	}


	@Test
	public void test_brokenGraph_2() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/broken-graph-2.graph" );

		// Normal loading: errors and no built graph
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertNotSame( 0, fromDef.getErrors().size());
		Assert.assertEquals( 0, graphs.getRootComponents().size());
		Assert.assertEquals( 0, graphs.getFacetNameToFacet().size());

		// Flexible loading
		fromDef = new FromGraphDefinition( f.getParentFile(), true );
		graphs = fromDef.buildGraphs( f );

		Assert.assertNotSame( 0, fromDef.getErrors().size());
		Assert.assertEquals( 2, ComponentHelpers.findAllComponents( graphs ).size());
		Assert.assertEquals( 2, graphs.getFacetNameToFacet().size());

		// Verify facets
		Facet f1 = graphs.getFacetNameToFacet().get( "f1" );
		Assert.assertNotNull( f1 );
		Assert.assertEquals( 2, f1.exportedVariables.size());
		Assert.assertEquals( 0, f1.getChildren().size());

		Facet f2 = graphs.getFacetNameToFacet().get( "f2" );
		Assert.assertNotNull( f2 );
		Assert.assertEquals( 0, f2.exportedVariables.size());
		Assert.assertEquals( 0, f2.getChildren().size());

		// Verify components.
		// "c2" and "comp1" are not found because located after the broken section.
		Component c1 = ComponentHelpers.findComponent( graphs, "c1" );
		Assert.assertNotNull( c1 );
		Assert.assertEquals( 2, c1.exportedVariables.size());
		Assert.assertEquals( 0, c1.importedVariables.size());
		Assert.assertEquals( 1, c1.getFacets().size());
		Assert.assertEquals( 0, c1.getChildren().size());

		Component comp2 = ComponentHelpers.findComponent( graphs, "comp2" );
		Assert.assertNotNull( comp2 );
		Assert.assertEquals( 1, comp2.exportedVariables.size());
		Assert.assertEquals( 0, comp2.importedVariables.size());
		Assert.assertEquals( 0, comp2.getFacets().size());
		Assert.assertEquals( 0, comp2.getChildren().size());
	}


	@Test
	public void testTypeAnnotations() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/broken-graph-1.graph" );

		// Flexible loading
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile(), true );
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertNotSame( 0, fromDef.getErrors().size());
		Assert.assertEquals( 4, ComponentHelpers.findAllComponents( graphs ).size());
		Assert.assertEquals( 2, graphs.getFacetNameToFacet().size());
		Assert.assertEquals( 3, fromDef.getTypeAnnotations().size());

		// Verify facets
		Facet f1 = graphs.getFacetNameToFacet().get( "f1" );
		Assert.assertNotNull( f1 );
		Assert.assertEquals( "This is facet f1.\nAnd the desc spans over two lines.", fromDef.getTypeAnnotations().get( f1.getName()));

		Facet f2 = graphs.getFacetNameToFacet().get( "f2" );
		Assert.assertNotNull( f2 );
		Assert.assertEquals( "Simple comment.", fromDef.getTypeAnnotations().get( f2.getName()));

		// Verify components
		Component c1 = ComponentHelpers.findComponent( graphs, "c1" );
		Assert.assertNotNull( c1 );
		Assert.assertEquals( "A comment about c1", fromDef.getTypeAnnotations().get( c1.getName()));

		Component c2 = ComponentHelpers.findComponent( graphs, "c2" );
		Assert.assertNotNull( c2 );
		Assert.assertNull( fromDef.getTypeAnnotations().get( c2.getName()));

		Component comp1 = ComponentHelpers.findComponent( graphs, "comp1" );
		Assert.assertNotNull( comp1 );
		Assert.assertNull( fromDef.getTypeAnnotations().get( comp1.getName()));

		Component comp2 = ComponentHelpers.findComponent( graphs, "comp2" );
		Assert.assertNotNull( comp2 );
		Assert.assertNull( fromDef.getTypeAnnotations().get( comp2.getName()));
	}


	@Test
	public void testQuotedProperties() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-with-quoted-values.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile(), false );
		fromDef.buildGraphs( f );

		Assert.assertEquals( 4, fromDef.getErrors().size());
		Iterator<ParsingError> iterator = fromDef.getErrors().iterator();

		ParsingError error = iterator.next();
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTALLER_NAME, error.getErrorCode());

		error = iterator.next();
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, error.getErrorCode());
		Assert.assertEquals( 1, error.getDetails().length );
		Assert.assertEquals( "\"toto\"", error.getDetails()[ 0 ].getElementName());
		Assert.assertEquals( ErrorDetailsKind.NAME, error.getDetails()[ 0 ].getErrorDetailsKind());

		error = iterator.next();
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, error.getErrorCode());
		Assert.assertEquals( 1, error.getDetails().length );
		Assert.assertEquals( "\"A\"", error.getDetails()[ 0 ].getElementName());
		Assert.assertEquals( ErrorDetailsKind.NAME, error.getDetails()[ 0 ].getErrorDetailsKind());

		error = iterator.next();
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, error.getErrorCode());
		Assert.assertEquals( 1, error.getDetails().length );
		Assert.assertEquals( "\"component\"", error.getDetails()[ 0 ].getElementName());
		Assert.assertEquals( ErrorDetailsKind.NAME, error.getDetails()[ 0 ].getErrorDetailsKind());
	}
}
