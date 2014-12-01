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

package net.roboconf.core.model.converters;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.io.ParsingModelIo;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.validators.ParsingModelValidator;
import net.roboconf.core.model.validators.RuntimeModelValidator;

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

		Component cA = new Component( "A" ).alias( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		cA.getExportedVariables().put( "A.port", "9000" );
		cA.getExportedVariables().put( "A.ip", null );
		cA.getImportedVariables().put( "B.port", Boolean.FALSE );
		cA.getImportedVariables().put( "B.ip", Boolean.TRUE );

		Component cB = new Component( "B" ).alias( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		cB.getExportedVariables().put( "B.port", "9000" );
		cB.getExportedVariables().put( "B.ip", null );

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_oneFacet() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).alias( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );
		cA.getImportedVariables().put( "facetF.props", Boolean.FALSE );

		cA.getExportedVariables().put( "A.port", "9000" );
		cA.getExportedVariables().put( "A.ip", null );
		cA.getImportedVariables().put( "B.port", Boolean.TRUE );
		cA.getImportedVariables().put( "B.ip", Boolean.TRUE );

		Component cB = new Component( "B" ).alias( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		cB.getFacetNames().add( "facetF" );
		cB.getExportedVariables().put( "B.port", "9000" );
		cB.getExportedVariables().put( "B.ip", null );
		cB.getExportedVariables().put( "facetF.props", null );

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_threeFacets() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).alias( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		cA.getFacetNames().add( "my-facet-1" );

		cA.getExportedVariables().put( "A.port", "9000" );
		cA.getExportedVariables().put( "A.ip", null );
		cA.getExportedVariables().put( "my-facet-1.data", "coucou" );

		cA.getImportedVariables().put( "B.port", Boolean.TRUE );
		cA.getImportedVariables().put( "B.ip", Boolean.TRUE );
		cA.getImportedVariables().put( "facetF.props", Boolean.FALSE );

		Component cB = new Component( "B" ).alias( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		cB.getFacetNames().add( "facetF" );
		cA.getFacetNames().add( "my-facet-2" );

		cA.getExportedVariables().put( "my-facet-2.woo", "woo" );
		cB.getExportedVariables().put( "B.port", "9000" );
		cB.getExportedVariables().put( "B.ip", null );
		cB.getExportedVariables().put( "facetF.props", null );

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_withComments() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).alias( "A" ).installerName( Constants.TARGET_INSTALLER ).iconLocation( "some-location.jpg" );
		graphs.getRootComponents().add( cA );

		cA.getFacetNames().add( "my-facet-1" );

		cA.getExportedVariables().put( "A.port", "9000" );
		cA.getExportedVariables().put( "A.ip", null );
		cA.getExportedVariables().put( "my-facet-1.data", "coucou" );

		cA.getImportedVariables().put( "B.ip", Boolean.TRUE );
		cA.getImportedVariables().put( "facetF.props", Boolean.FALSE );

		Component cB = new Component( "B" ).alias( "B" ).installerName( "installer B" );
		cB.getFacetNames().add( "facetF" );
		cA.getFacetNames().add( "my-facet-2" );

		cA.getExportedVariables().put( "my-facet-2.woo", "woo" );
		cB.getExportedVariables().put( "B.port", "9000" );
		cB.getExportedVariables().put( "B.ip", null );
		cB.getExportedVariables().put( "facetF.props", null );

		Component cC = new Component( "C" ).alias( "C" ).installerName( "installer C" );
		cC.getFacetNames().add( "facetF" );
		cC.getFacetNames().add( "my-facet-2" );

		cC.getExportedVariables().put( "my-facet-2.woo", "woo" );
		cC.getExportedVariables().put( "C.port", "9000" );
		cC.getExportedVariables().put( "facetF.props", null );

		ComponentHelpers.insertChild( cA, cB );
		ComponentHelpers.insertChild( cA, cC );
		compareGraphs( graphs, true );
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

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		Graphs readGraphs = fromDef.buildGraphs();
		Assert.assertEquals(  0, fromDef.getErrors().size());
		Assert.assertEquals(  0, RuntimeModelValidator.validate( readGraphs ).size());

		// Compare the graphs
		List<Component> readComponents = ComponentHelpers.findAllComponents( readGraphs );
		Assert.assertEquals( ComponentHelpers.findAllComponents( graphs ).size(), readComponents.size());
		for( Component readComponent : readComponents ) {
			Component originalComponent = ComponentHelpers.findComponent( graphs, readComponent.getName());
			Assert.assertNotNull( readComponent.getName(), originalComponent );
			Assert.assertEquals( readComponent.getAlias(), originalComponent.getAlias());
			Assert.assertEquals( readComponent.getInstallerName(), originalComponent.getInstallerName());
			Assert.assertEquals( readComponent.getIconLocation(), originalComponent.getIconLocation());
			Assert.assertEquals( readComponent.getExportedVariables().size(), originalComponent.getExportedVariables().size());
			Assert.assertEquals( readComponent.getImportedVariables().size(), originalComponent.getImportedVariables().size());

			for( Map.Entry<String,String> entry : readComponent.getExportedVariables().entrySet()) {
				Assert.assertTrue( readComponent.getName(), originalComponent.getExportedVariables().containsKey( entry.getKey()));
				String value = originalComponent.getExportedVariables().get( entry.getKey());
				Assert.assertEquals( readComponent.getName(), entry.getValue(), value );
			}

			for( Map.Entry<String,Boolean> entry : readComponent.getImportedVariables().entrySet()) {
				Assert.assertTrue( originalComponent.getImportedVariables().containsKey( entry.getKey()));
				Boolean value = originalComponent.getImportedVariables().get( entry.getKey());
				Assert.assertEquals( readComponent.getName(), entry.getValue(), value );
			}
		}
	}
}
