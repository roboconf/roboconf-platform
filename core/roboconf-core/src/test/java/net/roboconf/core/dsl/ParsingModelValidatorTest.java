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

package net.roboconf.core.dsl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.dsl.parsing.AbstractBlock;
import net.roboconf.core.dsl.parsing.BlockBlank;
import net.roboconf.core.dsl.parsing.BlockComment;
import net.roboconf.core.dsl.parsing.BlockComponent;
import net.roboconf.core.dsl.parsing.BlockFacet;
import net.roboconf.core.dsl.parsing.BlockImport;
import net.roboconf.core.dsl.parsing.BlockInstanceOf;
import net.roboconf.core.dsl.parsing.BlockProperty;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ParsingError;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelValidatorTest {

	@Test
	public void testUknown() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		AbstractBlock block = new AbstractBlock( file ) {
			@Override
			public int getInstructionType() {
				return 501;
			}
		};

		Collection<ParsingError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_BLOCK_TYPE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testImport() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockImport block = new BlockImport( file );

		block.setUri( "another-file.txt" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setUri( "http://server/another-file.txt" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setUri( "" );
		Collection<ParsingError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_IMPORT_LOCATION, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testBlank() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockBlank block = new BlockBlank( file, "" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block = new BlockBlank( file, "\t\n" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block = new BlockBlank( file, "\t  \n   \n  " );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block = new BlockBlank( file, "\nInvalid blank line\n" );
		Collection<ParsingError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MALFORMED_BLANK, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testComment() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockComment block = new BlockComment( file, "# some comment" );
		Assert.assertEquals( 0, ParsingModelValidator.validate((AbstractBlock) block ).size());

		block = new BlockComment( file, "##### some comment" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block = new BlockComment( file, "# comment 1\n#comment 2\n# Comment number blabla" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block = new BlockComment( file, "# Comment 1\nOops, I forgot the sharp symbol\n# Another comment" );
		Collection<ParsingError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MALFORMED_COMMENT, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testProperty() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockProperty block = new BlockProperty( file );

		// Installer
		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_INSTALLER, "" );
		Collection<ParsingError> errors = ParsingModelValidator.validate((AbstractBlock) block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_INSTALLER, "installerName" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_INSTALLER, "installer-name" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_INSTALLER, "installer.name" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_INSTALLER, "installer#name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTALLER_NAME, errors.iterator().next().getErrorCode());


		// Children
		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "-facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, ".facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet with spaces" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet with	tabs" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet-with-minus-symbols" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_CHILDREN, "facet_with_underscores" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());


		// Component Facets
		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "_facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "-facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, ".facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "54_facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet with spaces" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet with special ch@r@cters" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_FACETS, "facet1, , facet2" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_REFERENCED_NAME, errors.iterator().next().getErrorCode());


		// Extended Facets
		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "_facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "-facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, ".facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "54_facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet with spaces" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet with	tabs and speci@l" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet1,,facet2" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_REFERENCED_NAME, errors.iterator().next().getErrorCode());


		// Exported variables
		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "_var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1,var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1 , var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1=value1, var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1 = value1 , var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1, var2 = value2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1, var2 = value2 , var3" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1, var2 = \"value2\" , var3" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "random[port] var1 , var2 = \"value2\" , var3" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "-var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, ".var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "2var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1, var2;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var1; var2;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var2, , var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_VARIABLE_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var2, var1 = \"value1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, errors.iterator().next().getErrorCode());
		Assert.assertEquals( "var1", errors.iterator().next().getDetails()[ 0 ].getElementName());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var2, var1 =value1\"" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, errors.iterator().next().getErrorCode());
		Assert.assertEquals( "var1", errors.iterator().next().getDetails()[ 0 ].getElementName());

		block.setNameAndValue( ParsingConstants.PROPERTY_GRAPH_EXPORTS, "var#, var2;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 2, errors.size());
		for( ParsingError parsingError : errors )
			Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, parsingError.getErrorCode());


		// Imported variables
		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "c.var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "_c.var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "_c._var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "c.var1, c2.var" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "c.var1 , c2.var" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "var." );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "c.var, var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "var, var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 2, errors.size());
		for( ParsingError parsingError : errors )
			Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, parsingError.getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "var#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "-var" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, ".var" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "c.var, var;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, "-var, var!" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 2, errors.size());
		for( ParsingError parsingError : errors )
			Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, parsingError.getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_COMPONENT_IMPORTS, ", comp.var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_VARIABLE_NAME, errors.iterator().next().getErrorCode());


		// Count property
		block.setNameAndValue( ParsingConstants.PROPERTY_INSTANCE_COUNT, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_INSTANCE_COUNT, "a" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_INSTANCE_COUNT, "woo" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_INSTANCE_COUNT, "-5" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_INSTANCE_COUNT, "2.3" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_INSTANCE_COUNT, "1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_USELESS_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( ParsingConstants.PROPERTY_INSTANCE_COUNT, "3" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());


		// Instance channel
		block.setNameAndValue( ParsingConstants.PROPERTY_INSTANCE_CHANNELS, "whatever, there is no validation yet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());


		// Invalid property
		block.setNameAndValue( "An Invalid Property", "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( "An Invalid Property", "some value" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_UNKNOWN_PROPERTY_NAME, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testFacet() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockFacet block = new BlockFacet( file );

		block.setName( "facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "Facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "_facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "facet#" );
		Collection<ParsingError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "facet name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "facet n@me" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( ".facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "facet." );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		block.setName( "facet.name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		block.setName( "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setName( "facet-name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "facet_name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "name" );
		block.getInnerBlocks().add( new BlockProperty( file, "whatever", "whatever" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testComponent() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockComponent block = new BlockComponent( file );

		block.setName( "component" );
		Collection<ParsingError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "_component" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "component#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component n*me" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( ".component" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component." );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		block.setName( "component.name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		block.setName( "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component-name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "component_name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "name" );
		block.getInnerBlocks().add( new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_EXTENDS, "facet1" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.getInnerBlocks().add( new BlockProperty( file, "whatever", "whatever" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testInstanceOf() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockInstanceOf block = new BlockInstanceOf( file );

		block.setName( "component" );
		Collection<ParsingError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MISSING_INSTANCE_NAME, errors.iterator().next().getErrorCode());

		block.getInnerBlocks().add( new BlockProperty( file, ParsingConstants.PROPERTY_INSTANCE_NAME, "Any name" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.getInnerBlocks().clear();
		block.getInnerBlocks().add( new BlockProperty( file, ParsingConstants.PROPERTY_INSTANCE_NAME, "Any name?" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_NAME, errors.iterator().next().getErrorCode());

		block.getInnerBlocks().clear();
		block.getInnerBlocks().add( new BlockProperty( file, ParsingConstants.PROPERTY_INSTANCE_NAME, "AnyName" ));
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "_component" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "component#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component n@me" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( ".component" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component." );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		block.setName( "component.name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		block.setName( "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component-name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "component_name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "name" );
		block.getInnerBlocks().add( new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_EXTENDS, "anyPropertyToOverride" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		AbstractBlock childBlock = new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_EXTENDS, "anyPropertyToOverride" );
		block.getInnerBlocks().add( childBlock );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DUPLICATE_PROPERTY, errors.iterator().next().getErrorCode());
		block.getInnerBlocks().remove( childBlock );

		childBlock = new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_EXTENDS, "anyPropertyToOverride" ) {
			@Override
			public int getInstructionType() {
				return 502;
			}
		};

		block.getInnerBlocks().add( childBlock );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 2, errors.size());
		Iterator<ParsingError> iterator = errors.iterator();
		Assert.assertEquals( ErrorCode.PM_INVALID_BLOCK_TYPE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_ELEMENT, iterator.next().getErrorCode());
	}


	@Test
	public void testExternalIsAKeywordForImports_file() throws Exception {

		File f = TestUtils.findTestFile( "/configurations/invalid/external-is-keyword-for-imports.graph" );
		FromGraphDefinition fromDef = new FromGraphDefinition( f.getParentFile());
		fromDef.buildGraphs( f );

		List<ParsingError> errors = new ArrayList<>( fromDef.getErrors());
		Assert.assertEquals( 2, errors.size());

		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( 4, errors.get( 0 ).getLine());

		Assert.assertEquals( ErrorCode.PM_EXTERNAL_IS_KEYWORD_FOR_IMPORTS, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( 4, errors.get( 1 ).getLine());
	}
}
