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

package net.roboconf.core.dsl.converters;

import java.io.File;
import java.util.Iterator;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.helpers.ComponentHelpers;

import org.junit.Test;

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
	public void testGraphWithWrongImport() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/graph-with-invalid-import.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Iterator<ModelError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_UNREACHABLE_FILE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testDuplicateComponent() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-component.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 2, fromDef.getErrors().size());
		for( ModelError error : fromDef.getErrors())
			Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_COMPONENT, error.getErrorCode());
	}


	@Test
	public void testDuplicateFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 3, fromDef.getErrors().size());
		for( ModelError error : fromDef.getErrors())
			Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_FACET, error.getErrorCode());
	}


	@Test
	public void testUnresolvedFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/unresolved-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_UNRESOLVED_FACET, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testUnresolvedExtendedFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/unresolved-extended-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_UNRESOLVED_FACET, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testCycleInFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/cycle-in-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_CYCLE_IN_FACETS, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testUnresolvableFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/unresolvable-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_UNRESOLVED_FACET, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testSelfOptionalImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/component-optional-imports.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());

		Component componentA = ComponentHelpers.findComponent( graphs, "A" );
		Assert.assertTrue( componentA.getExportedVariables().containsKey( "A.port" ));
		Assert.assertTrue( componentA.getExportedVariables().containsKey( "A.ip" ));

		Assert.assertTrue( componentA.getImportedVariables().containsKey( "A.port" ));
		Assert.assertTrue( componentA.getImportedVariables().containsKey( "A.ip" ));

		Assert.assertTrue( componentA.getImportedVariables().get( "A.port" ));
		Assert.assertTrue( componentA.getImportedVariables().get( "A.ip" ));
	}


	@Test
	public void testIDsWithSpaces() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/real-lamp-all-in-one-flex.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		Graphs graphs = fromDef.buildGraphs( f );

		Assert.assertEquals( 0, fromDef.getErrors().size());

		Component component = ComponentHelpers.findComponent( graphs, "hello world" );
		Assert.assertNotNull( component );
		Assert.assertTrue( component.getMetadata().getFacetNames().contains( "war archive" ));

		component = ComponentHelpers.findComponent( graphs, "ecom" );
		Assert.assertNotNull( component );
		Assert.assertTrue( component.getMetadata().getFacetNames().contains( "war archive" ));
	}


	@Test
	public void testInexistingChildren_components() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-inexisting-children.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 2, fromDef.getErrors().size());

		ModelError[] errors = fromDef.getErrors().toArray( new ModelError[ 2 ]);
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 0 ].getErrorCode());
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 1 ].getErrorCode());
	}


	@Test
	public void testInexistingChildren_facets() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-inexisting-child-facet.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());

		ModelError[] errors = fromDef.getErrors().toArray( new ModelError[ 1 ]);
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 0 ].getErrorCode());
		Assert.assertTrue( errors[ 0 ].getDetails().contains( "Fa2" ));
	}


	@Test
	public void testInexistingExtendedComponent() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-extends-inexisting-component.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		Assert.assertEquals( 1, fromDef.getErrors().size());

		ModelError[] errors = fromDef.getErrors().toArray( new ModelError[ 1 ]);
		Assert.assertEquals( ErrorCode.CO_INEXISTING_COMPONENT, errors[ 0 ].getErrorCode());
	}


	@Test
	public void testCountParents() {
		FromGraphDefinition fromDef = new FromGraphDefinition( null );

		Component c1 = new Component( "c1" );
		Assert.assertEquals( 0, fromDef.countParents( c1 ));

		Component c2 = new Component( "c2" );
		Component c3 = new Component( "c3" );

		c2.getMetadata().setExtendedComponent( c3 );
		Assert.assertEquals( 1, fromDef.countParents( c2 ));

		c1.getMetadata().setExtendedComponent( c2 );
		Assert.assertEquals( 2, fromDef.countParents( c1 ));

		c3.getMetadata().setExtendedComponent( c1 );
		Assert.assertEquals( -1, fromDef.countParents( c1 ));
		Assert.assertEquals( -1, fromDef.countParents( c2 ));
	}
}
