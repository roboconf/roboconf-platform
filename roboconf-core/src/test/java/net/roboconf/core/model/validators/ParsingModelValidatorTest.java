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

package net.roboconf.core.model.validators;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.parsing.AbstractBlock;
import net.roboconf.core.model.parsing.BlockBlank;
import net.roboconf.core.model.parsing.BlockComment;
import net.roboconf.core.model.parsing.BlockComponent;
import net.roboconf.core.model.parsing.BlockFacet;
import net.roboconf.core.model.parsing.BlockImport;
import net.roboconf.core.model.parsing.BlockInstanceOf;
import net.roboconf.core.model.parsing.BlockProperty;
import net.roboconf.core.model.parsing.FileDefinition;

import org.junit.Test;

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

		Collection<ModelError> errors = ParsingModelValidator.validate( block );
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
		Collection<ModelError> errors = ParsingModelValidator.validate( block );
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
		Collection<ModelError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MALFORMED_BLANK, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testComment() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockComment block = new BlockComment( file, "# some comment" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block = new BlockComment( file, "##### some comment" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block = new BlockComment( file, "# comment 1\n#comment 2\n# Comment number blabla" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block = new BlockComment( file, "# Comment 1\nOops, I forgot the sharp symbol\n# Another comment" );
		Collection<ModelError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MALFORMED_COMMENT, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testProperty() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockProperty block = new BlockProperty( file );

		// Component alias
		block.setNameAndValue( Constants.PROPERTY_COMPONENT_ALIAS, "" );
		Collection<ModelError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_ALIAS, "Some alias" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_ALIAS, "Some weird alias 12, with special characters!:;*%$" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());


		// Icon
		block.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "icon.gif" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "http://server/icon.png" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "icon.bmp" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_ICON_LOCATION, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "icon location.gif" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_ICON_LOCATION, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "icon%20location.gif" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());


		// Installer
		block.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "installerName" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "installer-name" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "installer.name" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "installer#name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTALLER_NAME, errors.iterator().next().getErrorCode());


		// Children
		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "-facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, ".facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet with spaces" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet with	tabs" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());


		// Component Facets
		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "_facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "-facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, ".facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "54_facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet with spaces" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet with special ch@r@cters" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());


		// Extended Facets
		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "_facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "-facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, ".facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "54_facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet with spaces" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet with	tabs and speci@l" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());


		// Exported variables
		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "_var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1 , var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1=value1, var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1 = value1 , var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1, var2 = value2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1, var2 = value2 , var3" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "-var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, ".var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "2var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1, var2;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1; var2;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var#, var2;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, modelError.getErrorCode());


		// Imported variables
		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "_c.var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "_c._var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var1, c2.var" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var1 , c2.var" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "var." );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var, var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "var, var1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, modelError.getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "var#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "-var" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, ".var" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var, var;" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "-var, var!" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, modelError.getErrorCode());


		// Count property
		block.setNameAndValue( Constants.PROPERTY_INSTANCE_COUNT, "" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_INSTANCE_COUNT, "a" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_INSTANCE_COUNT, "woo" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_INSTANCE_COUNT, "-5" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_INSTANCE_COUNT, "2.3" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_INSTANCE_COUNT, "1" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_USELESS_INSTANCE_COUNT, errors.iterator().next().getErrorCode());

		block.setNameAndValue( Constants.PROPERTY_INSTANCE_COUNT, "3" );
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
		Collection<ModelError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setName( "facet name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "facet n@me" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		block.setName( ".facet" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

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

		block.setName( "name" );
		block.getInnerBlocks().add( new BlockProperty( file, Constants.PROPERTY_COMPONENT_ALIAS, "some alias" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testComponent() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockComponent block = new BlockComponent( file );

		block.setName( "component" );
		Collection<ModelError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MISSING_ALIAS_PROPERTY, errors.iterator().next().getErrorCode());

		block.getInnerBlocks().add( new BlockProperty( file, Constants.PROPERTY_COMPONENT_ALIAS, "An alias" ));
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "_component" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "component#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "component n*me" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		block.setName( ".component" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

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

		block.setName( "name" );
		block.getInnerBlocks().add( new BlockProperty( file, Constants.PROPERTY_FACET_EXTENDS, "facet1" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testInstanceOf() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		BlockInstanceOf block = new BlockInstanceOf( file );

		block.setName( "component" );
		Collection<ModelError> errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MISSING_INSTANCE_NAME, errors.iterator().next().getErrorCode());

		block.getInnerBlocks().add( new BlockProperty( file, Constants.PROPERTY_INSTANCE_NAME, "Any name" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.getInnerBlocks().clear();
		block.getInnerBlocks().add( new BlockProperty( file, Constants.PROPERTY_INSTANCE_NAME, "Any name?" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_NAME, errors.iterator().next().getErrorCode());

		block.getInnerBlocks().clear();
		block.getInnerBlocks().add( new BlockProperty( file, Constants.PROPERTY_INSTANCE_NAME, "AnyName" ));
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "_component" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( block ).size());

		block.setName( "component#" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		block.setName( "component name" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		block.setName( "component n@me" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		block.setName( ".component" );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

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

		block.setName( "name" );
		block.getInnerBlocks().add( new BlockProperty( file, Constants.PROPERTY_FACET_EXTENDS, "anyPropertyToOverride" ));
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 0, errors.size());

		AbstractBlock childBlock = new BlockProperty( file, Constants.PROPERTY_FACET_EXTENDS, "anyPropertyToOverride" );
		block.getInnerBlocks().add( childBlock );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DUPLICATE_PROPERTY, errors.iterator().next().getErrorCode());
		block.getInnerBlocks().remove( childBlock );

		childBlock = new BlockProperty( file, Constants.PROPERTY_FACET_EXTENDS, "anyPropertyToOverride" ) {
			@Override
			public int getInstructionType() {
				return 502;
			}
		};

		block.getInnerBlocks().add( childBlock );
		errors = ParsingModelValidator.validate( block );
		Assert.assertEquals( 2, errors.size());
		Iterator<ModelError> iterator = errors.iterator();
		Assert.assertEquals( ErrorCode.PM_INVALID_BLOCK_TYPE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTANCE_ELEMENT, iterator.next().getErrorCode());
	}
}
