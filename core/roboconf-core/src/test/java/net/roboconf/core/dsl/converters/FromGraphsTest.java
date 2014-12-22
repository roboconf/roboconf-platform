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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.ParsingModelValidator;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.internal.tests.ComplexApplicationFactory1;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.RuntimeModelValidator;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.helpers.ComponentHelpers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphsTest {

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


	@Test
	public void testFromGraphs_noFacet() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		cA.exportedVariables.put( "A.port", "9000" );
		cA.exportedVariables.put( "A.ip", null );
		cA.importedVariables.put( "B.port", Boolean.FALSE );
		cA.importedVariables.put( "B.ip", Boolean.TRUE );

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		cB.exportedVariables.put( "B.port", "9000" );
		cB.exportedVariables.put( "B.ip", null );

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_oneFacet() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );
		cA.importedVariables.put( "facetF.props", Boolean.FALSE );

		cA.exportedVariables.put( "A.port", "9000" );
		cA.exportedVariables.put( "A.ip", null );
		cA.importedVariables.put( "B.port", Boolean.TRUE );
		cA.importedVariables.put( "B.ip", Boolean.TRUE );

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		Facet facetF = new Facet( "facetF" );
		facetF.exportedVariables.put( "facetF.props", "something" );

		cB.associateFacet( facetF );
		cB.exportedVariables.put( "B.port", "9000" );
		cB.exportedVariables.put( "B.ip", null );

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_threeFacets() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		Facet facet = new Facet( "my-facet-1" );
		facet.exportedVariables.put( "data", "coucou" );
		cA.associateFacet( facet );

		cA.exportedVariables.put( "A.port", "9000" );
		cA.exportedVariables.put( "A.ip", null );

		cA.importedVariables.put( "B.port", Boolean.TRUE );
		cA.importedVariables.put( "B.ip", Boolean.TRUE );
		cA.importedVariables.put( "facetF.props", Boolean.FALSE );

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		facet = new Facet( "facetF" );
		facet.exportedVariables.put( "facetF.props", "some value" );
		cB.associateFacet( facet );

		facet = new Facet( "my-facet-2" );
		facet.exportedVariables.put( "my-facet-2.woo", "woo" );
		cB.associateFacet( facet );

		cB.exportedVariables.put( "B.port", "9000" );
		cB.exportedVariables.put( "B.ip", null );

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_complexApplication() throws Exception {

		Application app = ComplexApplicationFactory1.newApplication();
		compareGraphs( app.getGraphs(), false );
	}


	@Test
	public void testFromGraphs_withComments() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		Facet facet = new Facet( "my-facet-1" );
		facet.exportedVariables.put( "my-facet-1.data", "coucou" );
		cA.associateFacet( facet );

		cA.exportedVariables.put( "A.port", "9000" );
		cA.exportedVariables.put( "A.ip", null );

		cA.importedVariables.put( "B.ip", Boolean.TRUE );
		cA.importedVariables.put( "facetF.props", Boolean.FALSE );

		Component cB = new Component( "B" ).installerName( "installer B" );
		cB.exportedVariables.put( "B.port", "9000" );
		cB.exportedVariables.put( "B.ip", null );

		facet = new Facet( "facetF" );
		facet.exportedVariables.put( "facetF.props", "some value" );
		cB.associateFacet( facet );

		facet = new Facet( "my-facet-2" );
		facet.exportedVariables.put( "my-facet-2.woo", "woo" );
		cB.associateFacet( facet );

		Component cC = new Component( "C" ).installerName( "installer C" );
		for( Facet f : cB.getFacets())
			cC.associateFacet( f );

		cC.exportedVariables.put( "my-facet-2.woo", "woo" );
		cC.exportedVariables.put( "C.port", "9000" );
		cC.exportedVariables.put( "facetF.props", "something else" );

		cA.addChild( cB );
		cA.addChild( cC );
		compareGraphs( graphs, true );
	}


	@Test
	public void testFromGraphs_withInheritance() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		cA.exportedVariables.put( "A.port", "9000" );
		cA.exportedVariables.put( "A.ip", null );

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		cB.extendComponent( cA );
		graphs.getRootComponents().add( cB );

		cB.exportedVariables.put( "A.port", "9000" );
		cB.exportedVariables.put( "A.ip", null );

		compareGraphs( graphs, false );
	}


	/**
	 * Compares an in-memory graphs with its written/read version.
	 * @param graphs a graphs
	 * @param writeComments true to write the comments, false otherwise
	 * @throws Exception
	 */
	private void compareGraphs( Graphs graphs, boolean writeComments ) throws Exception {

		Assert.assertEquals(  0, RuntimeModelValidator.validate( graphs ).size());
		File targetFile = this.testFolder.newFile( "roboconf_test.graph" );
		FileDefinition defToWrite = new FromGraphs().buildFileDefinition( graphs, targetFile, writeComments );
		ParsingModelIo.saveRelationsFile( defToWrite, writeComments, System.getProperty( "line.separator" ));

		// Load the saved file
		FileDefinition def = ParsingModelIo.readConfigurationFile( targetFile, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());
		Assert.assertEquals( FileDefinition.GRAPH, def.getFileType());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( null );
		Graphs readGraphs = fromDef.buildGraphs( targetFile );
		Assert.assertEquals(  0, fromDef.getErrors().size());
		Assert.assertEquals(  0, RuntimeModelValidator.validate( readGraphs ).size());

		// Compare the graphs
		List<Component> readComponents = ComponentHelpers.findAllComponents( readGraphs );
		Assert.assertEquals( ComponentHelpers.findAllComponents( graphs ).size(), readComponents.size());
		for( Component readComponent : readComponents ) {
			Component originalComponent = ComponentHelpers.findComponent( graphs, readComponent.getName());
			Assert.assertNotNull( readComponent.getName(), originalComponent );
			Assert.assertEquals( readComponent.getExtendedComponent(), originalComponent.getExtendedComponent());
			Assert.assertEquals( readComponent.getInstallerName(), originalComponent.getInstallerName());
			Assert.assertEquals( readComponent.exportedVariables.size(), originalComponent.exportedVariables.size());
			Assert.assertEquals( readComponent.importedVariables.size(), originalComponent.importedVariables.size());

			for( Map.Entry<String,String> entry : readComponent.exportedVariables.entrySet()) {
				Assert.assertTrue( readComponent.getName(), originalComponent.exportedVariables.containsKey( entry.getKey()));
				String value = originalComponent.exportedVariables.get( entry.getKey());
				Assert.assertEquals( readComponent.getName(), entry.getValue(), value );
			}

			for( Map.Entry<String,Boolean> entry : readComponent.importedVariables.entrySet()) {
				Assert.assertTrue( originalComponent.importedVariables.containsKey( entry.getKey()));
				Boolean value = originalComponent.importedVariables.get( entry.getKey());
				Assert.assertEquals( readComponent.getName(), entry.getValue(), value );
			}
		}
	}
}
