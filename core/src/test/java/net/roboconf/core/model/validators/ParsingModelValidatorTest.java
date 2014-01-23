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

import junit.framework.Assert;
import net.roboconf.core.model.ErrorCode;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.parsing.Constants;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.parsing.RegionBlank;
import net.roboconf.core.model.parsing.RegionComment;
import net.roboconf.core.model.parsing.RegionComponent;
import net.roboconf.core.model.parsing.RegionFacet;
import net.roboconf.core.model.parsing.RegionImport;
import net.roboconf.core.model.parsing.RegionProperty;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelValidatorTest {

	@Test
	public void testImport() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		RegionImport instr = new RegionImport( file );

		instr.setUri( "another-file.txt" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setUri( "http://server/another-file.txt" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setUri( "" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_IMPORT_LOCATION, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testBlank() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		RegionBlank instr = new RegionBlank( file, "" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RegionBlank( file, "\t\n" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RegionBlank( file, "\t  \n   \n  " );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RegionBlank( file, "\nInvalid blank line\n" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MALFORMED_BLANK, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testComment() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		RegionComment instr = new RegionComment( file, "# some comment" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RegionComment( file, "##### some comment" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RegionComment( file, "# comment 1\n#comment 2\n# Comment number blabla" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RegionComment( file, "# Comment 1\nOops, I forgot the sharp symbol\n# Another comment" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MALFORMED_COMMENT, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testProperty() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		RegionProperty instr = new RegionProperty( file );

		// Component alias
		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_ALIAS, "" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_ALIAS, "Some alias" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_ALIAS, "Some weird alias 12, with special characters!:;*%$" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());


		// Icon
		instr.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "icon.gif" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "http://server/icon.png" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "icon.bmp" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_ICON_LOCATION, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "icon location.gif" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_ICON_LOCATION, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_ICON_LOCATION, "icon%20location.gif" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());


		// Installer
		instr.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "installerName" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "installer-name" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "installer.name" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_INSTALLER, "installer#name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_INSTALLER_NAME, errors.iterator().next().getErrorCode());


		// Children
		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "-facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, ".facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_CHILDREN, "facet with spaces" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());


		// Component Facets
		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "_facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "-facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, ".facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "54_facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_FACETS, "facet with spaces" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());


		// Extended Facets
		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "_facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "-facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, ".facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "54_facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_FACET_EXTENDS, "facet with spaces" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());


		// Exported variables
		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "_var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1 , var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1=value1, var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1 = value1 , var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1, var2 = value2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1, var2 = value2 , var3" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "-var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, ".var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "2var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1, var2;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var1; var2;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_GRAPH_EXPORTS, "var#, var2;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, modelError.getErrorCode());


		// Imported variables
		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "_c.var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "_c._var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var1, c2.var" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var1 , c2.var" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "var." );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var, var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "var, var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, modelError.getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "var#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "-var" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, ".var" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "c.var, var;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.PROPERTY_COMPONENT_IMPORTS, "-var, var!" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, modelError.getErrorCode());


		// Invalid property
		instr.setNameAndValue( "An Invalid Property", "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( "An Invalid Property", "some value" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_UNKNWON_PROPERTY_NAME, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testFacet() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		RegionFacet instr = new RegionFacet( file );

		instr.setName( "facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "Facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "_facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "facet#" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "facet name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setName( ".facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "facet." );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		instr.setName( "facet.name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		instr.setName( "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "name" );
		instr.getInternalInstructions().add( new RegionProperty( file, Constants.PROPERTY_COMPONENT_ALIAS, "some alias" ));
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testComponent() {

		FileDefinition file = new FileDefinition( new File( "some-file" ));
		RegionComponent instr = new RegionComponent( file );

		instr.setName( "component" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_MISSING_ALIAS_PROPERTY, errors.iterator().next().getErrorCode());

		instr.getInternalInstructions().add( new RegionProperty( file, Constants.PROPERTY_COMPONENT_ALIAS, "An alias" ));
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "_component" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "component#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "component name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		instr.setName( ".component" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "component." );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		instr.setName( "component.name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		instr.setName( "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_EMPTY_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "name" );
		instr.getInternalInstructions().add( new RegionProperty( file, Constants.PROPERTY_FACET_EXTENDS, "facet1" ));
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, errors.iterator().next().getErrorCode());
	}
}
