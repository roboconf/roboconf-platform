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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.ParsingModelValidator;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.internal.tests.ComplexApplicationFactory1;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.RuntimeModelValidator;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.helpers.ComponentHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphsTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();


	@Test
	public void testFromGraphs_noFacet() throws Exception {

		// Build a model
		Graphs graphs = new Graphs();
		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		cA.addExportedVariable( new ExportedVariable( "A.port", "9000" ));
		cA.addExportedVariable( new ExportedVariable( "A.ip", null ));
		cA.addImportedVariable( new ImportedVariable( "B.port", false, false ));
		cA.addImportedVariable( new ImportedVariable( "B.ip", true, false ));

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		cB.addExportedVariable( new ExportedVariable( "port", "9000" ));
		cB.addExportedVariable( new ExportedVariable( "ip", null ));

		// Write it into a file, load a new graph from this file and compare it with the current one
		compareGraphs( graphs, false );

		// Verify exported variables are written correctly
		FileDefinition defToWrite = new FromGraphs().buildFileDefinition( graphs, null, false );
		String s = ParsingModelIo.writeConfigurationFile( defToWrite, false, System.getProperty( "line.separator" ));
		Assert.assertTrue( s.contains( "port=\"9000\"" ));
		Assert.assertTrue( s.contains( "exports: ip, port=\"9000\";" ));
	}


	@Test
	public void testFromGraphs_oneFacet() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );
		cA.addImportedVariable( new ImportedVariable( "facetF.props", false, false ));

		cA.addExportedVariable( new ExportedVariable( "A.port", "9000" ));
		cA.addExportedVariable( new ExportedVariable( "A.ip", null ));
		cA.addImportedVariable( new ImportedVariable( "B.port", true, false ));
		cA.addImportedVariable( new ImportedVariable( "B.ip", true, false ));

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		Facet facetF = new Facet( "facetF" );
		facetF.addExportedVariable( new ExportedVariable( "facetF.props", "something" ));

		cB.associateFacet( facetF );
		cB.addExportedVariable( new ExportedVariable( "B.port", "9000" ));
		cB.addExportedVariable( new ExportedVariable( "B.ip", null ));

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_threeFacets() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		Facet facet = new Facet( "my-facet-1" );
		facet.addExportedVariable( new ExportedVariable( "data", "coucou" ));
		cA.associateFacet( facet );

		cA.addExportedVariable( new ExportedVariable( "A.port", "9000" ));
		cA.addExportedVariable( new ExportedVariable( "A.ip", null ));

		cA.addImportedVariable( new ImportedVariable( "B.port", true, false ));
		cA.addImportedVariable( new ImportedVariable( "B.ip", true, false ));
		cA.addImportedVariable( new ImportedVariable( "facetF.props", false, false ));

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		facet = new Facet( "facetF" );
		facet.addExportedVariable( new ExportedVariable( "facetF.props", "some value" ));
		cB.associateFacet( facet );

		facet = new Facet( "my-facet-2" );
		facet.addExportedVariable( new ExportedVariable( "my-facet-2.woo", "woo" ));
		cB.associateFacet( facet );

		cB.addExportedVariable( new ExportedVariable( "B.port", "9000" ));
		cB.addExportedVariable( new ExportedVariable( "B.ip", null ));

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_complexApplication() throws Exception {

		ApplicationTemplate app = ComplexApplicationFactory1.newApplication();
		compareGraphs( app.getGraphs(), false );
	}


	@Test
	public void testFromGraphs_withComments() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		Facet facet = new Facet( "my-facet-1" );
		facet.addExportedVariable( new ExportedVariable( "my-facet-1.data", "coucou" ));
		cA.associateFacet( facet );

		cA.addExportedVariable( new ExportedVariable( "A.port", "9000" ));
		cA.addExportedVariable( new ExportedVariable( "A.ip", null ));

		cA.addImportedVariable( new ImportedVariable( "B.ip", true, false ));
		cA.addImportedVariable( new ImportedVariable( "facetF.props", false, false ));

		Component cB = new Component( "B" ).installerName( "installer B" );
		cB.addExportedVariable( new ExportedVariable( "B.port", "9000" ));
		cB.addExportedVariable( new ExportedVariable( "B.ip", null ));

		facet = new Facet( "facetF" );
		facet.addExportedVariable( new ExportedVariable( "facetF.props", "some value" ));
		cB.associateFacet( facet );

		facet = new Facet( "my-facet-2" );
		facet.addExportedVariable( new ExportedVariable( "my-facet-2.woo", "woo" ));
		cB.associateFacet( facet );

		Component cC = new Component( "C" ).installerName( "installer C" );
		for( Facet f : cB.getFacets())
			cC.associateFacet( f );

		cC.addExportedVariable( new ExportedVariable( "my-facet-2.woo", "woo" ));
		cC.addExportedVariable( new ExportedVariable( "C.port", "9000" ));
		cC.addExportedVariable( new ExportedVariable( "facetF.props", "something else" ));

		cA.addChild( cB );
		cA.addChild( cC );
		compareGraphs( graphs, true );
	}


	@Test
	public void testFromGraphs_withInheritance() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		cA.addExportedVariable( new ExportedVariable( "A.port", "9000" ));
		cA.addExportedVariable( new ExportedVariable( "A.ip", null ));

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		cB.extendComponent( cA );
		graphs.getRootComponents().add( cB );

		cB.addExportedVariable( new ExportedVariable( "A.port", "9000" ));
		cB.addExportedVariable( new ExportedVariable( "A.ip", null ));

		compareGraphs( graphs, false );
	}


	@Test
	public void testFromGraphs_withExternal() throws Exception {
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cA );

		cA.addExportedVariable( new ExportedVariable( "A.port", "9000" ));
		cA.addExportedVariable( new ExportedVariable( "A.ip", null ));
		cA.addImportedVariable( new ImportedVariable( "B.port", false, true ));
		cA.addImportedVariable( new ImportedVariable( "B.ip", true, false ));

		Component cB = new Component( "B" ).installerName( Constants.TARGET_INSTALLER );
		graphs.getRootComponents().add( cB );

		cB.addExportedVariable( new ExportedVariable( "B.port", "9000" ));
		cB.addExportedVariable( new ExportedVariable( "B.ip", null ));

		compareGraphs( graphs, false );
	}


	/**
	 * Compares an in-memory graphs with its written/read version.
	 * @param graphs a graphs
	 * @param writeComments true to write the comments, false otherwise
	 * @throws Exception
	 */
	private void compareGraphs( Graphs graphs, boolean writeComments ) throws Exception {

		// Ignore some errors, for convenience...
		Collection<ModelError> errors = RuntimeModelValidator.validate( graphs );
		RoboconfErrorHelpers.filterErrorsForRecipes( errors );
		Assert.assertEquals(  0, errors.size());

		File targetFile = this.testFolder.newFile( "roboconf_test.graph" );
		FileDefinition defToWrite = new FromGraphs().buildFileDefinition( graphs, targetFile, writeComments );
		ParsingModelIo.saveRelationsFile( defToWrite, writeComments, System.getProperty( "line.separator" ));

		// Load the saved file
		FileDefinition def = ParsingModelIo.readConfigurationFile( targetFile, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());
		Assert.assertEquals( FileDefinition.GRAPH, def.getFileType());

		Collection<ParsingError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( null );
		Graphs readGraphs = fromDef.buildGraphs( targetFile );
		Assert.assertEquals(  0, fromDef.getErrors().size());

		// Compare all the errors, without ignoring ones
		Set<ModelError> originalErrors = new HashSet<>( RuntimeModelValidator.validate( graphs ));
		Set<ModelError> readErrors = new HashSet<>( RuntimeModelValidator.validate( readGraphs ));
		Assert.assertEquals( originalErrors, readErrors );

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

			for( Map.Entry<String,ExportedVariable> entry : readComponent.exportedVariables.entrySet()) {
				Assert.assertTrue( readComponent.getName(), originalComponent.exportedVariables.containsKey( entry.getKey()));
				ExportedVariable originalVar = originalComponent.exportedVariables.get( entry.getKey());

				Assert.assertEquals( readComponent.getName(), entry.getValue().getName(), originalVar.getName());
				Assert.assertEquals( readComponent.getName(), entry.getValue().getValue(), originalVar.getValue());
				Assert.assertEquals( readComponent.getName(), entry.getValue().getRandomKind(), originalVar.getRandomKind());
				Assert.assertEquals( readComponent.getName(), entry.getValue().isRandom(), originalVar.isRandom());
			}

			for( ImportedVariable var : readComponent.importedVariables.values()) {
				String junitMsg = readComponent.getName() + " :: " + var;
				ImportedVariable originalVar = originalComponent.importedVariables.get( var.getName());
				Assert.assertNotNull( "Imports did not match for " + junitMsg, originalVar );
				Assert.assertEquals( junitMsg, originalVar.isOptional(), var.isOptional());
				Assert.assertEquals( junitMsg, originalVar.isExternal(), var.isExternal());
			}
		}
	}
}
