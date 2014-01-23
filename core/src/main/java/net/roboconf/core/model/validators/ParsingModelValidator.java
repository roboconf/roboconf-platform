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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ErrorCode;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.parsing.AbstractPropertiesHolder;
import net.roboconf.core.model.parsing.AbstractRegion;
import net.roboconf.core.model.parsing.Constants;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.parsing.RegionBlank;
import net.roboconf.core.model.parsing.RegionComment;
import net.roboconf.core.model.parsing.RegionComponent;
import net.roboconf.core.model.parsing.RegionFacet;
import net.roboconf.core.model.parsing.RegionImport;
import net.roboconf.core.model.parsing.RegionInstanceOf;
import net.roboconf.core.model.parsing.RegionProperty;

/**
 * A set of methods to validate parsing model objects.
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelValidator {

	/**
	 * @param definitionFile
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( FileDefinition definitionFile ) {
		Collection<ModelError> result = new ArrayList<ModelError> ();
		result.addAll( definitionFile.getParsingErrors());
		for( AbstractRegion instr : definitionFile.getInstructions())
			result.addAll( validate( instr ));

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( AbstractRegion instr ) {

		Collection<ModelError> result;
		switch( instr.getInstructionType()) {
		case AbstractRegion.COMPONENT:
			result = validate((RegionComponent) instr);
			break;

		case AbstractRegion.FACET:
			result = validate((RegionFacet) instr);
			break;

		case AbstractRegion.IMPORT:
			result = validate((RegionImport) instr);
			break;

		case AbstractRegion.COMMENT:
			result = validate((RegionComment) instr);
			break;

		case AbstractRegion.PROPERTY:
			result = validate((RegionProperty) instr);
			break;

		case AbstractRegion.BLANK:
			result = validate((RegionBlank) instr);
			break;

		case AbstractRegion.INSTANCEOF:
			result = validate((RegionInstanceOf) instr);
			break;

		default:
			result = new ArrayList<ModelError>( 1 );
			ModelError error = new ModelError( ErrorCode.PM_INVALID_INSTRUCTION_TYPE, instr.getLine());
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
	public static Collection<ModelError> validate( RegionImport instr ) {

		String uri = instr.getUri();
		Collection<ModelError> result = new ArrayList<ModelError> ();
		if( Utils.isEmptyOrWhitespaces( uri ))
			result.add( new ModelError( ErrorCode.PM_EMPTY_IMPORT_LOCATION, instr.getLine()));

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RegionComponent instr ) {
		Collection<ModelError> result = validatePropertiesHolder( instr, true );
		if( instr.findPropertyByName( Constants.PROPERTY_COMPONENT_ALIAS ) == null )
			result.add( new ModelError( ErrorCode.PM_MISSING_ALIAS_PROPERTY, instr.getLine()));

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RegionInstanceOf instr ) {

		// Basic checks
		Collection<ModelError> result = validatePropertiesHolder( instr, false );
		RegionProperty prop = instr.findPropertyByName( Constants.PROPERTY_INSTANCE_NAME );
		if( prop == null ) {
			result.add( new ModelError( ErrorCode.PM_MISSING_INSTANCE_NAME, instr.getLine()));

		} else {
			String name = prop.getValue();
			if( instr.findPropertyByName( Constants.PROPERTY_INSTANCE_CARDINALITY ) != null ) {
				if( ! name.contains( Constants.INSTANCE_INDEX_MARKER ))
					result.add( new ModelError( ErrorCode.PM_MISSING_INDEX_REFERENCE, instr.getLine()));

			} else if( name.contains( Constants.INSTANCE_INDEX_MARKER )) {
				result.add( new ModelError( ErrorCode.PM_INVALID_INDEX_REFERENCE_USE, instr.getLine()));
			}
		}

		// Check internal regions are supported
		for( AbstractRegion region : instr.getInternalInstructions()) {
			if( region.getInstructionType() != AbstractRegion.BLANK
					&& region.getInstructionType() != AbstractRegion.COMMENT
					&& region.getInstructionType() != AbstractRegion.PROPERTY
					&& region.getInstructionType() != AbstractRegion.INSTANCEOF ) {

				ModelError error = new ModelError( ErrorCode.PM_INVALID_INSTANCE_ELEMENT, region.getLine());
				error.setDetails( "Invalid type found: " + region.getClass().getSimpleName());
				result.add( error );
			}
		}

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RegionFacet instr ) {
		return validatePropertiesHolder( instr, true );
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RegionProperty instr ) {

		Collection<ModelError> result = new ArrayList<ModelError> ();
		String value = instr.getValue();
		String name = instr.getName();
		int line = instr.getLine();

		if( Utils.isEmptyOrWhitespaces( value )) {
			result.add( new ModelError( ErrorCode.PM_EMPTY_PROPERTY_VALUE, line ));

		} else if( Constants.PROPERTY_GRAPH_CHILDREN.equals( name )) {
			for( String s : Utils.splitNicely( value, Constants.PROPERTY_SEPARATOR )) {
				if( s.matches( Constants.PATTERN_ID ))
					continue;

				ModelError error = new ModelError( ErrorCode.PM_INVALID_CHILD_NAME, line );
				error.setDetails( "Child name: " + s );
				result.add( error );
			}

		} else if( Constants.PROPERTY_COMPONENT_ALIAS.equals( name )) {
			// nothing

		} else if( Constants.PROPERTY_COMPONENT_FACETS.equals( name )
				|| Constants.PROPERTY_FACET_EXTENDS.equals( name )) {

			for( String s : Utils.splitNicely( value, Constants.PROPERTY_SEPARATOR )) {
				if( Utils.isEmptyOrWhitespaces( s )) {
					result.add( new ModelError( ErrorCode.PM_EMPTY_REFERENCED_FACET_NAME, line ));

				} else if( ! s.matches( Constants.PATTERN_ID )) {
					ModelError error = new ModelError( ErrorCode.PM_INVALID_FACET_NAME, line );
					error.setDetails( "Facet name: " + s );
					result.add( error );
				}
			}

		} else if( Constants.PROPERTY_COMPONENT_IMPORTS.equals( name )) {
			for( String s : Utils.splitNicely( value, Constants.PROPERTY_SEPARATOR )) {
				if( Utils.isEmptyOrWhitespaces( s ))
					result.add( new ModelError( ErrorCode.PM_EMPTY_VARIABLE_NAME, line ));
				else if( ! s.matches( Constants.PATTERN_ID ))
					result.add( new ModelError( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, line ));
				else if( ! s.contains( "." )
						|| s.indexOf( '.' ) == s.length() -1 )
					result.add( new ModelError( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, line ));
			}

		} else if( Constants.PROPERTY_GRAPH_EXPORTS.equals( name )) {
			for( String s : Utils.splitNicely( value, Constants.PROPERTY_SEPARATOR )) {
				Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
				if( Utils.isEmptyOrWhitespaces( entry.getKey()))
					result.add( new ModelError( ErrorCode.PM_EMPTY_VARIABLE_NAME, line ));
				else if( ! entry.getKey().matches( Constants.PATTERN_ID ))
					result.add( new ModelError( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, line ));
			}

		} else if( Constants.PROPERTY_GRAPH_ICON_LOCATION.equals( name )) {
			if( ! value.toLowerCase().matches( Constants.PATTERN_IMAGE ))
				result.add( new ModelError( ErrorCode.PM_INVALID_ICON_LOCATION, line ));

		} else if( Constants.PROPERTY_GRAPH_INSTALLER.equals( name )) {
			if( ! value.matches( Constants.PATTERN_ID ))
				result.add( new ModelError( ErrorCode.PM_INVALID_INSTALLER_NAME, line ));

		} else {
			ModelError error = new ModelError( ErrorCode.PM_UNKNWON_PROPERTY_NAME, line );
			error.setDetails( "Property name: " + name );
			result.add( error );
		}

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RegionComment instr ) {

		// Only makes sense when a comment section was created programmatically.
		Collection<ModelError> result = new ArrayList<ModelError> ();
		int cpt = 0;
		for( String s : instr.getContent().split( "\n" )) {
			if( ! s.trim().startsWith( Constants.COMMENT_DELIMITER ))
				result.add( new ModelError( ErrorCode.PM_MALFORMED_COMMENT, instr.getLine() + cpt ));

			cpt ++;
		}

		return result;
	}


	/**
	 * @param instr
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( RegionBlank instr ) {

		// Only makes sense when a RegionBlank section was created programmatically.
		Collection<ModelError> result = new ArrayList<ModelError> ();
		if( ! Utils.isEmptyOrWhitespaces( instr.getContent()))
			result.add( new ModelError( ErrorCode.PM_MALFORMED_BLANK, instr.getLine()));

		return result;
	}


	/**
	 * @param holder
	 * @param strict true if only supported properties are allowed, false otherwise
	 * @return a non-null list of {@link ModelError}s
	 */
	private static Collection<ModelError> validatePropertiesHolder( AbstractPropertiesHolder holder, boolean strict ) {

		// Check the name
		Collection<ModelError> result = new ArrayList<ModelError> ();
		String name = holder.getName();
		if( Utils.isEmptyOrWhitespaces( name ))
			result.add( new ModelError( holder.getInstructionType() == AbstractRegion.FACET ? ErrorCode.PM_EMPTY_FACET_NAME : ErrorCode.PM_EMPTY_COMPONENT_NAME, holder.getLine()));
		else if( ! name.matches( Constants.PATTERN_ID ))
			result.add( new ModelError( holder.getInstructionType() == AbstractRegion.FACET ? ErrorCode.PM_INVALID_FACET_NAME : ErrorCode.PM_INVALID_COMPONENT_NAME, holder.getLine()));
		else if( name.contains( "." ))
			result.add( new ModelError( ErrorCode.PM_DOT_IS_NOT_ALLOWED, holder.getLine()));

		// Check all the properties have a value
		List<String> supportedProperties = Arrays.asList( holder.getSupportedPropertyNames());
		Set<String> foundProperties = new HashSet<String> ();
		for( AbstractRegion region : holder.getInternalInstructions()) {

			if( region.getInstructionType() == AbstractRegion.PROPERTY ) {
				RegionProperty p = (RegionProperty) region;
				if( foundProperties.contains( p.getName())) {
					ModelError error = new ModelError( ErrorCode.PM_DUPLICATE_PROPERTY, p.getLine());
					error.setDetails( "Property name: " + p.getName());
					result.add( error );
				}

				foundProperties.add( p.getName());
				if( supportedProperties.contains( p.getName())) {
					result.addAll( validate( p ));

				} else if( strict ) {
					ModelError error = new ModelError( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, p.getLine());
					error.setDetails( "Property name: " + p.getName());
					result.add( error );
				}

			} else {
				result.addAll( validate( region ));
			}
		}

		return result;
	}
}
