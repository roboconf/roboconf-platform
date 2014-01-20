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
import net.roboconf.core.model.parsing.FileRelations;
import net.roboconf.core.model.parsing.RelationBlank;
import net.roboconf.core.model.parsing.RelationComment;
import net.roboconf.core.model.parsing.RelationComponent;
import net.roboconf.core.model.parsing.RelationFacet;
import net.roboconf.core.model.parsing.RelationImport;
import net.roboconf.core.model.parsing.RelationProperty;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelValidatorTest {

	@Test
	public void testImport() {

		FileRelations file = new FileRelations( new File( "some-file" ));
		RelationImport instr = new RelationImport( file );

		instr.setUri( "another-file.txt" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setUri( "http://server/another-file.txt" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setUri( "" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_IMPORT_LOCATION, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testBlank() {

		FileRelations file = new FileRelations( new File( "some-file" ));
		RelationBlank instr = new RelationBlank( file, "" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RelationBlank( file, "\t\n" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RelationBlank( file, "\t  \n   \n  " );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RelationBlank( file, "\nInvalid blank line\n" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.MALFORMED_BLANK, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testComment() {

		FileRelations file = new FileRelations( new File( "some-file" ));
		RelationComment instr = new RelationComment( file, "# some comment" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RelationComment( file, "##### some comment" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RelationComment( file, "# comment 1\n#comment 2\n# Comment number blabla" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr = new RelationComment( file, "# Comment 1\nOops, I forgot the sharp symbol\n# Another comment" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.MALFORMED_COMMENT, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testProperty() {

		FileRelations file = new FileRelations( new File( "some-file" ));
		RelationProperty instr = new RelationProperty( file );

		// Component alias
		instr.setNameAndValue( Constants.COMPONENT_ALIAS, "" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_ALIAS, "Some alias" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_ALIAS, "Some weird alias 12, with special characters!:;*%$" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());


		// Icon
		instr.setNameAndValue( Constants.ICON_LOCATION, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.ICON_LOCATION, "icon.gif" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.ICON_LOCATION, "http://server/icon.png" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.ICON_LOCATION, "icon.bmp" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_ICON_LOCATION, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.ICON_LOCATION, "icon location.gif" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_ICON_LOCATION, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.ICON_LOCATION, "icon%20location.gif" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());


		// Installer
		instr.setNameAndValue( Constants.INSTALLER, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.INSTALLER, "installerName" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.INSTALLER, "installer-name" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.INSTALLER, "installer.name" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.INSTALLER, "installer#name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_INSTALLER_NAME, errors.iterator().next().getErrorCode());


		// Children
		instr.setNameAndValue( Constants.CHILDREN, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.CHILDREN, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.CHILDREN, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.CHILDREN, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.CHILDREN, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.CHILDREN, "-facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.CHILDREN, ".facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.CHILDREN, "facet#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.CHILDREN, "facet;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.CHILDREN, "facet with spaces" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_CHILD_NAME, errors.iterator().next().getErrorCode());


		// Component Facets
		instr.setNameAndValue( Constants.COMPONENT_FACETS, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "_facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "-facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, ".facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "54_facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "facet#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "facet;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_FACETS, "facet with spaces" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());


		// Extended Facets
		instr.setNameAndValue( Constants.FACET_EXTENDS, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "_facet1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "facet1, facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "facet1 , facet2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "facet1 , facet2, facet3453_, facet-, facet." );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "-facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.FACET_EXTENDS, ".facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "54_facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "facet#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "facet;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.FACET_EXTENDS, "facet with spaces" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());


		// Exported variables
		instr.setNameAndValue( Constants.EXPORTS, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.EXPORTS, "var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.EXPORTS, "_var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.EXPORTS, "var1 , var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.EXPORTS, "var1=value1, var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.EXPORTS, "var1 = value1 , var2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.EXPORTS, "var1, var2 = value2" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.EXPORTS, "var1, var2 = value2 , var3" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.EXPORTS, "-var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.EXPORTS, ".var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.EXPORTS, "2var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.EXPORTS, "var#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.EXPORTS, "var;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.EXPORTS, "var1, var2;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.EXPORTS, "var1; var2;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_EXPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.EXPORTS, "var#, var2;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.INVALID_EXPORTED_VAR_NAME, modelError.getErrorCode());


		// Imported variables
		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "c.var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "_c.var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "_c._var1" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "c.var1, c2.var" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "c.var1 , c2.var" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "var." );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "c.var, var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INCOMPLETE_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "var, var1" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.INCOMPLETE_IMPORTED_VAR_NAME, modelError.getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "var#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "-var" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, ".var" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "c.var, var;" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_IMPORTED_VAR_NAME, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( Constants.COMPONENT_IMPORTS, "-var, var!" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 2, errors.size());
		for( ModelError modelError : errors )
			Assert.assertEquals( ErrorCode.INVALID_IMPORTED_VAR_NAME, modelError.getErrorCode());


		// Invalid property
		instr.setNameAndValue( "An Invalid Property", "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_PROPERTY_VALUE, errors.iterator().next().getErrorCode());

		instr.setNameAndValue( "An Invalid Property", "some value" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.UNKNWON_PROPERTY_NAME, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testFacet() {

		FileRelations file = new FileRelations( new File( "some-file" ));
		RelationFacet instr = new RelationFacet( file );

		instr.setName( "facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "Facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "_facet" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "facet#" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "facet name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setName( ".facet" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "facet." );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		instr.setName( "facet.name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		instr.setName( "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_FACET_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "name" );
		instr.getPropertyNameToProperty().put( Constants.COMPONENT_ALIAS, new RelationProperty( file, Constants.COMPONENT_ALIAS, "some alias" ));
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROPERTY_NOT_APPLIABLE, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testComponent() {

		FileRelations file = new FileRelations( new File( "some-file" ));
		RelationComponent instr = new RelationComponent( file );

		instr.setName( "component" );
		Collection<ModelError> errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.MISSING_ALIAS_PROPERTY, errors.iterator().next().getErrorCode());

		instr.getPropertyNameToProperty().put( Constants.COMPONENT_ALIAS, new RelationProperty( file, Constants.COMPONENT_ALIAS, "An alias" ));
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "_component" );
		Assert.assertEquals( 0, ParsingModelValidator.validate( instr ).size());

		instr.setName( "component#" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "component name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		instr.setName( ".component" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.INVALID_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "component." );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		instr.setName( "component.name" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.DOT_IS_NOT_ALLOWED, errors.iterator().next().getErrorCode());

		instr.setName( "" );
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.EMPTY_COMPONENT_NAME, errors.iterator().next().getErrorCode());

		instr.setName( "name" );
		instr.getPropertyNameToProperty().put( Constants.FACET_EXTENDS, new RelationProperty( file, Constants.FACET_EXTENDS, "facet1" ));
		errors = ParsingModelValidator.validate( instr );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROPERTY_NOT_APPLIABLE, errors.iterator().next().getErrorCode());
	}
}
