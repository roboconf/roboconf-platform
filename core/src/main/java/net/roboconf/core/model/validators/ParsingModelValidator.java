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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ErrorCode;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.parsing.AbstractInstruction;
import net.roboconf.core.model.parsing.AbstractPropertiesHolder;
import net.roboconf.core.model.parsing.Constants;
import net.roboconf.core.model.parsing.FileRelations;
import net.roboconf.core.model.parsing.RelationBlank;
import net.roboconf.core.model.parsing.RelationComment;
import net.roboconf.core.model.parsing.RelationComponent;
import net.roboconf.core.model.parsing.RelationFacet;
import net.roboconf.core.model.parsing.RelationImport;
import net.roboconf.core.model.parsing.RelationProperty;

/**
 * A set of methods to validate parsing model objects.
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelValidator {

	/**
	 * @param fileRelations
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( FileRelations fileRelations ) {
		Collection<ModelError> result = new ArrayList<ModelError> ();
		result.addAll( fileRelations.getParsingErrors());
		for( AbstractInstruction instr : fileRelations.getInstructions())
			result.addAll( validate( instr ));

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( AbstractInstruction instr ) {

		Collection<ModelError> result;
		switch( instr.getInstructionType()) {
		case AbstractInstruction.COMPONENT:
			result = validate((RelationComponent) instr);
			break;

		case AbstractInstruction.FACET:
			result = validate((RelationFacet) instr);
			break;

		case AbstractInstruction.IMPORT:
			result = validate((RelationImport) instr);
			break;

		case AbstractInstruction.COMMENT:
			result = validate((RelationComment) instr);
			break;

		case AbstractInstruction.PROPERTY:
			result = validate((RelationProperty) instr);
			break;

		case AbstractInstruction.BLANK:
			result = validate((RelationBlank) instr);
			break;

		default:
			result = new ArrayList<ModelError>( 1 );
			ModelError error = new ModelError( ErrorCode.INVALID_INSTRUCTION_TYPE, instr.getLine());
			error.setDetails( "Instruction type: " + instr.getInstructionType());
			result.add( error );
			break;
		}

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RelationImport instr ) {

		String uri = instr.getUri();
		Collection<ModelError> result = new ArrayList<ModelError> ();
		if( Utils.isEmptyOrWhitespaces( uri ))
			result.add( new ModelError( ErrorCode.EMPTY_IMPORT_LOCATION, instr.getLine()));

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RelationComponent instr ) {
		Collection<ModelError> result = validatePropertiesHolder( instr );
		if( ! instr.getPropertyNameToProperty().containsKey( Constants.COMPONENT_ALIAS ))
			result.add( new ModelError( ErrorCode.MISSING_ALIAS_PROPERTY, instr.getLine()));

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RelationFacet instr ) {
		return validatePropertiesHolder( instr );
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RelationProperty instr ) {

		Collection<ModelError> result = new ArrayList<ModelError> ();
		String value = instr.getValue();
		String name = instr.getName();
		int line = instr.getLine();

		if( Utils.isEmptyOrWhitespaces( value )) {
			result.add( new ModelError( ErrorCode.EMPTY_PROPERTY_VALUE, line ));

		} else if( Constants.CHILDREN.equals( name )) {
			for( String s : Utils.splitNicely( value, Constants.PROPERTY_SEPARATOR )) {
				if( s.matches( Constants.PATTERN_ID ))
					continue;

				ModelError error = new ModelError( ErrorCode.INVALID_CHILD_NAME, line );
				error.setDetails( "Child name: " + s );
				result.add( error );
			}

		} else if( Constants.COMPONENT_ALIAS.equals( name )) {
			// nothing

		} else if( Constants.COMPONENT_FACETS.equals( name )
				|| Constants.FACET_EXTENDS.equals( name )) {

			for( String s : Utils.splitNicely( value, Constants.PROPERTY_SEPARATOR )) {
				if( Utils.isEmptyOrWhitespaces( s )) {
					result.add( new ModelError( ErrorCode.EMPTY_REFERENCED_FACET_NAME, line ));

				} else if( ! s.matches( Constants.PATTERN_ID )) {
					ModelError error = new ModelError( ErrorCode.INVALID_FACET_NAME, line );
					error.setDetails( "Facet name: " + s );
					result.add( error );
				}
			}

		} else if( Constants.COMPONENT_IMPORTS.equals( name )) {
			for( String s : Utils.splitNicely( value, Constants.PROPERTY_SEPARATOR )) {
				if( Utils.isEmptyOrWhitespaces( s ))
					result.add( new ModelError( ErrorCode.EMPTY_VARIABLE_NAME, line ));
				else if( ! s.matches( Constants.PATTERN_ID ))
					result.add( new ModelError( ErrorCode.INVALID_IMPORTED_VAR_NAME, line ));
				else if( ! s.contains( "." )
						|| s.indexOf( '.' ) == s.length() -1 )
					result.add( new ModelError( ErrorCode.INCOMPLETE_IMPORTED_VAR_NAME, line ));
			}

		} else if( Constants.EXPORTS.equals( name )) {
			for( String s : Utils.splitNicely( value, Constants.PROPERTY_SEPARATOR )) {
				Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
				if( Utils.isEmptyOrWhitespaces( entry.getKey()))
					result.add( new ModelError( ErrorCode.EMPTY_VARIABLE_NAME, line ));
				else if( ! entry.getKey().matches( Constants.PATTERN_ID ))
					result.add( new ModelError( ErrorCode.INVALID_EXPORTED_VAR_NAME, line ));
			}

		} else if( Constants.ICON_LOCATION.equals( name )) {
			if( ! value.toLowerCase().matches( Constants.PATTERN_IMAGE ))
				result.add( new ModelError( ErrorCode.INVALID_ICON_LOCATION, line ));

		} else if( Constants.INSTALLER.equals( name )) {
			if( ! value.matches( Constants.PATTERN_ID ))
				result.add( new ModelError( ErrorCode.INVALID_INSTALLER_NAME, line ));

		} else {
			ModelError error = new ModelError( ErrorCode.UNKNWON_PROPERTY_NAME, line );
			error.setDetails( "Property name: " + name );
			result.add( error );
		}

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RelationComment instr ) {

		// Only makes sense when a comment section was created programmatically.
		Collection<ModelError> result = new ArrayList<ModelError> ();
		int cpt = 0;
		for( String s : instr.getContent().split( "\n" )) {
			if( ! s.trim().startsWith( Constants.COMMENT_DELIMITER ))
				result.add( new ModelError( ErrorCode.MALFORMED_COMMENT, instr.getLine() + cpt ));

			cpt ++;
		}

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RelationBlank instr ) {

		// Only makes sense when a RelationBlank section was created programmatically.
		Collection<ModelError> result = new ArrayList<ModelError> ();
		if( ! Utils.isEmptyOrWhitespaces( instr.getContent()))
			result.add( new ModelError( ErrorCode.MALFORMED_BLANK, instr.getLine()));

		return result;
	}


	/**
	 * @param holder
	 * @return a non-null list of {@link ModelError}s
	 */
	private static Collection<ModelError> validatePropertiesHolder( AbstractPropertiesHolder holder ) {

		// Check the name
		Collection<ModelError> result = new ArrayList<ModelError> ();
		String name = holder.getName();
		if( Utils.isEmptyOrWhitespaces( name ))
			result.add( new ModelError( holder.getInstructionType() == AbstractInstruction.FACET ? ErrorCode.EMPTY_FACET_NAME : ErrorCode.EMPTY_COMPONENT_NAME, holder.getLine()));
		else if( ! name.matches( Constants.PATTERN_ID ))
			result.add( new ModelError( holder.getInstructionType() == AbstractInstruction.FACET ? ErrorCode.INVALID_FACET_NAME : ErrorCode.INVALID_COMPONENT_NAME, holder.getLine()));
		else if( name.contains( "." ))
			result.add( new ModelError( ErrorCode.DOT_IS_NOT_ALLOWED, holder.getLine()));

		// Check all the properties have a value
		List<String> supportedProperties = Arrays.asList( holder.getSupportedPropertyNames());
		for( RelationProperty p : holder.getPropertyNameToProperty().values()) {
			if( supportedProperties.contains( p.getName())) {
				result.addAll( validate( p ));

			} else {
				ModelError error = new ModelError( ErrorCode.PROPERTY_NOT_APPLIABLE, p.getLine());
				error.setDetails( "Property name: " + p.getName());
				result.add( error );
			}
		}

		return result;
	}
}
