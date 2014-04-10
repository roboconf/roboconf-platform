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
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.io.ParsingModelIo;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.validators.ParsingModelValidator;
import net.roboconf.core.model.validators.RuntimeModelValidator;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphsTest {

	@Test
	public void testFromGraphs() throws Exception {

		// Build a graphs and save it
		Graphs graphs = new Graphs();

		Component cA = new Component( "A" );
		cA.setAlias( "A" );
		cA.getExportedVariables().put( "A.port", "9000" );
		cA.getExportedVariables().put( "A.ip", null );
		cA.getImportedVariables().put( "B.port", Boolean.TRUE );
		cA.getImportedVariables().put( "B.ip", Boolean.TRUE );

		// FIXME: we must write facets (for the exported variables)
		// cA.getImportedVariables().put( "facetF.props", Boolean.FALSE );
		cA.setInstallerName( "installer A" );
		graphs.getRootComponents().add( cA );

		Component cB = new Component( "B" );
		cB.setAlias( "B" );
		cB.getExportedVariables().put( "B.port", "9000" );
		cB.getExportedVariables().put( "B.ip", null );
		// cB.getExportedVariables().put( "facetF.props", null );
		cB.setInstallerName( "installer B" );
		graphs.getRootComponents().add( cB );

		Assert.assertEquals(  0, RuntimeModelValidator.validate( graphs ).size());
		File targetFile = File.createTempFile( "roboconf_", "test" );
		targetFile.deleteOnExit();
		FileDefinition defToWrite = new FromGraphs().buildFileDefinition( graphs, targetFile, false );
		ParsingModelIo.saveRelationsFile( defToWrite, false, System.getProperty( "line.separator" ));

		// Load the saved file
		FileDefinition def = ParsingModelIo.readConfigurationFile( targetFile, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		Graphs readGraphs = fromDef.buildGraphs();
		Assert.assertEquals(  0, RuntimeModelValidator.validate( readGraphs ).size());

		// Compare the graphs
		List<Component> readComponents = ComponentHelpers.findAllComponents( readGraphs );
		Assert.assertEquals( ComponentHelpers.findAllComponents( graphs ).size(), readComponents.size());
		for( Component readComponent : readComponents ) {
			Component originalComponent = ComponentHelpers.findComponent( graphs, readComponent.getName());
			Assert.assertNotNull( readComponent.getName(), originalComponent );
			Assert.assertEquals( readComponent.getAlias(), originalComponent.getAlias());
			Assert.assertEquals( readComponent.getInstallerName(), originalComponent.getInstallerName());

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
