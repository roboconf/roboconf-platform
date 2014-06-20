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

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.io.ParsingModelIo;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.validators.ParsingModelValidator;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphDefinitionTest {

	@Test
	public void testInstallerConflicts_failure() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/conflicting-installers.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		fromDef.buildGraphs();
		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_AMBIGUOUS_INSTALLER, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testInstallerConflicts_success() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/overridden-conflicting-installers.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		Graphs graphs = fromDef.buildGraphs();
		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( "my-own-installer", graphs.getRootComponents().iterator().next().getInstallerName());
	}


	@Test
	public void testInstallerConflicts_noAmbiguity() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/no-conflicting-installers.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		Graphs graphs = fromDef.buildGraphs();
		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( "f1", graphs.getRootComponents().iterator().next().getInstallerName());
	}


	@Test
	public void testInstallerConflicts_simpleInstaller() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/simple-installer.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		Graphs graphs = fromDef.buildGraphs();
		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( "my-own-installer", graphs.getRootComponents().iterator().next().getInstallerName());
	}


	@Test( expected = IllegalArgumentException.class )
	public void testInvalidFileType() {
		FileDefinition def = new FileDefinition( new File( "whatever.txt" ));
		new FromGraphDefinition( def );
	}


	@Test
	public void testDuplicateComponent() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-component.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		fromDef.buildGraphs();
		Assert.assertEquals( 2, fromDef.getErrors().size());
		for( ModelError error : fromDef.getErrors())
			Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_COMPONENT, error.getErrorCode());
	}


	@Test
	public void testDuplicateFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-facet.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		fromDef.buildGraphs();
		Assert.assertEquals( 3, fromDef.getErrors().size());
		for( ModelError error : fromDef.getErrors())
			Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_FACET, error.getErrorCode());
	}


	@Test
	public void testUnresolvedFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/unresolved-facet.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		fromDef.buildGraphs();
		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_UNRESOLVED_FACET, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testUnresolvedExtendedFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/unresolved-extended-facet.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		fromDef.buildGraphs();
		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_UNRESOLVED_FACET, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testCycleInFacet() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/cycle-in-facet.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		fromDef.buildGraphs();
		Assert.assertEquals( 1, fromDef.getErrors().size());
		Assert.assertEquals( ErrorCode.CO_CYCLE_IN_FACETS, fromDef.getErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testSelfOptionalImports() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/valid/component-optional-imports.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		Graphs graphs = fromDef.buildGraphs();
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
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		Graphs graphs = fromDef.buildGraphs();
		Assert.assertEquals( 0, fromDef.getErrors().size());

		Component component = ComponentHelpers.findComponent( graphs, "hello world" );
		Assert.assertNotNull( component );
		Assert.assertTrue( component.getFacetNames().contains( "war archive" ));

		component = ComponentHelpers.findComponent( graphs, "ecom" );
		Assert.assertNotNull( component );
		Assert.assertTrue( component.getFacetNames().contains( "war archive" ));
	}


	@Test
	public void testInexistingChildren_components() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-inexisting-children.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		fromDef.buildGraphs();
		Assert.assertEquals( 2, fromDef.getErrors().size());

		ModelError[] errors = fromDef.getErrors().toArray( new ModelError[ 2 ]);
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 0 ].getErrorCode());
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 1 ].getErrorCode());
	}


	@Test
	public void testInexistingChildren_facets() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/component-inexisting-child-facet.graph" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromGraphDefinition fromDef = new FromGraphDefinition( def );
		fromDef.buildGraphs();
		Assert.assertEquals( 1, fromDef.getErrors().size());

		ModelError[] errors = fromDef.getErrors().toArray( new ModelError[ 1 ]);
		Assert.assertEquals( ErrorCode.CO_INEXISTING_CHILD, errors[ 0 ].getErrorCode());
		Assert.assertTrue( errors[ 0 ].getDetails().contains( "Fa2" ));
	}
}
