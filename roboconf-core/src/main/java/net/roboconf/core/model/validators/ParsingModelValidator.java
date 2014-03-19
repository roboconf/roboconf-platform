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

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.parsing.AbstractBlock;
import net.roboconf.core.model.parsing.AbstractBlockHolder;
import net.roboconf.core.model.parsing.BlockBlank;
import net.roboconf.core.model.parsing.BlockComment;
import net.roboconf.core.model.parsing.BlockComponent;
import net.roboconf.core.model.parsing.BlockFacet;
import net.roboconf.core.model.parsing.BlockImport;
import net.roboconf.core.model.parsing.BlockInstanceOf;
import net.roboconf.core.model.parsing.BlockProperty;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.parsing.ParsingConstants;

/**
 * A set of methods to validate parsing model objects.
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelValidator {

	/**
	 * @param definitionFile a definition file
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( FileDefinition definitionFile ) {
		Collection<ModelError> result = new ArrayList<ModelError> ();
		result.addAll( definitionFile.getParsingErrors());
		for( AbstractBlock block : definitionFile.getBlocks())
			result.addAll( validate( block ));

		return result;
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( AbstractBlock block ) {

		Collection<ModelError> result;
		switch( block.getInstructionType()) {
		case AbstractBlock.COMPONENT:
			result = validate((BlockComponent) block);
			break;

		case AbstractBlock.FACET:
			result = validate((BlockFacet) block);
			break;

		case AbstractBlock.IMPORT:
			result = validate((BlockImport) block);
			break;

		case AbstractBlock.COMMENT:
			result = validate((BlockComment) block);
			break;

		case AbstractBlock.PROPERTY:
			result = validate((BlockProperty) block);
			break;

		case AbstractBlock.BLANK:
			result = validate((BlockBlank) block);
			break;

		case AbstractBlock.INSTANCEOF:
			result = validate((BlockInstanceOf) block);
			break;

		default:
			result = new ArrayList<ModelError>( 1 );
			ModelError error = new ModelError( ErrorCode.PM_INVALID_BLOCK_TYPE, block.getLine());
			error.setDetails( "Instruction type: " + block.getInstructionType());
			result.add( error );
			break;
		}

		return result;
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( BlockImport block ) {

		String uri = block.getUri();
		Collection<ModelError> result = new ArrayList<ModelError> ();
		if( Utils.isEmptyOrWhitespaces( uri ))
			result.add( new ModelError( ErrorCode.PM_EMPTY_IMPORT_LOCATION, block.getLine()));

		return result;
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( BlockComponent block ) {
		Collection<ModelError> result = validatePropertiesHolder( block, true );
		if( block.findPropertyBlockByName( Constants.PROPERTY_COMPONENT_ALIAS ) == null )
			result.add( new ModelError( ErrorCode.PM_MISSING_ALIAS_PROPERTY, block.getLine()));

		return result;
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( BlockInstanceOf block ) {

		// Basic checks
		Collection<ModelError> result = validatePropertiesHolder( block, false );
		BlockProperty prop = block.findPropertyBlockByName( Constants.PROPERTY_INSTANCE_NAME );
		if( prop == null ) {
			result.add( new ModelError( ErrorCode.PM_MISSING_INSTANCE_NAME, block.getLine()));

		} else {
//			String name = prop.getValue();
//			if( block.findPropertyBlockByName( Constants.PROPERTY_INSTANCE_CARDINALITY ) != null ) {
//				if( ! name.contains( ParsingConstants.INSTANCE_INDEX_MARKER ))
//					result.add( new ModelError( ErrorCode.PM_MISSING_INDEX_REFERENCE, block.getLine()));
//
//			} else if( name.contains( ParsingConstants.INSTANCE_INDEX_MARKER )) {
//				result.add( new ModelError( ErrorCode.PM_INVALID_INDEX_REFERENCE_USE, block.getLine()));
//			}
		}

		// Check internal regions are supported
		for( AbstractBlock region : block.getInnerBlocks()) {
			if( region.getInstructionType() != AbstractBlock.BLANK
					&& region.getInstructionType() != AbstractBlock.COMMENT
					&& region.getInstructionType() != AbstractBlock.PROPERTY
					&& region.getInstructionType() != AbstractBlock.INSTANCEOF ) {

				ModelError error = new ModelError( ErrorCode.PM_INVALID_INSTANCE_ELEMENT, region.getLine());
				error.setDetails( "Invalid type found: " + region.getClass().getSimpleName());
				result.add( error );
			}
		}

		return result;
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( BlockFacet block ) {
		return validatePropertiesHolder( block, true );
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( BlockProperty block ) {

		Collection<ModelError> result = new ArrayList<ModelError> ();
		String value = block.getValue();
		String name = block.getName();
		int line = block.getLine();

		if( Utils.isEmptyOrWhitespaces( value )) {
			result.add( new ModelError( ErrorCode.PM_EMPTY_PROPERTY_VALUE, line ));

		} else if( Constants.PROPERTY_GRAPH_CHILDREN.equals( name )) {
			for( String s : Utils.splitNicely( value, ParsingConstants.PROPERTY_SEPARATOR )) {
				if( s.matches( ParsingConstants.PATTERN_ID ))
					continue;

				ModelError error = new ModelError( ErrorCode.PM_INVALID_CHILD_NAME, line );
				error.setDetails( "Child name: " + s );
				result.add( error );
			}

		} else if( Constants.PROPERTY_COMPONENT_ALIAS.equals( name )) {
			// nothing

		} else if( Constants.PROPERTY_COMPONENT_FACETS.equals( name )
				|| Constants.PROPERTY_FACET_EXTENDS.equals( name )) {

			for( String s : Utils.splitNicely( value, ParsingConstants.PROPERTY_SEPARATOR )) {
				if( Utils.isEmptyOrWhitespaces( s )) {
					result.add( new ModelError( ErrorCode.PM_EMPTY_REFERENCED_FACET_NAME, line ));

				} else if( ! s.matches( ParsingConstants.PATTERN_ID )) {
					ModelError error = new ModelError( ErrorCode.PM_INVALID_FACET_NAME, line );
					error.setDetails( "Facet name: " + s );
					result.add( error );
				}
			}

		} else if( Constants.PROPERTY_COMPONENT_IMPORTS.equals( name )) {
			for( String s : Utils.splitNicely( value, ParsingConstants.PROPERTY_SEPARATOR )) {

				if( s.toLowerCase().endsWith( Constants.PROPERTY_COMPONENT_OPTIONAL_IMPORT ))
					s = s.substring( 0, s.length() - Constants.PROPERTY_COMPONENT_OPTIONAL_IMPORT.length());

				s = s.trim();
				if( Utils.isEmptyOrWhitespaces( s ))
					result.add( new ModelError( ErrorCode.PM_EMPTY_VARIABLE_NAME, line ));
				else if( ! s.matches( ParsingConstants.PATTERN_ID ))
					result.add( new ModelError( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, line ));
				else if( ! s.contains( "." )
						|| s.indexOf( '.' ) == s.length() -1 )
					result.add( new ModelError( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, line ));
			}

		} else if( Constants.PROPERTY_GRAPH_EXPORTS.equals( name )) {
			for( String s : Utils.splitNicely( value, ParsingConstants.PROPERTY_SEPARATOR )) {
				Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
				if( Utils.isEmptyOrWhitespaces( entry.getKey()))
					result.add( new ModelError( ErrorCode.PM_EMPTY_VARIABLE_NAME, line ));
				else if( ! entry.getKey().matches( ParsingConstants.PATTERN_ID ))
					result.add( new ModelError( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, line ));
			}

		} else if( Constants.PROPERTY_GRAPH_ICON_LOCATION.equals( name )) {
			if( ! value.toLowerCase().matches( ParsingConstants.PATTERN_IMAGE ))
				result.add( new ModelError( ErrorCode.PM_INVALID_ICON_LOCATION, line ));

		} else if( Constants.PROPERTY_GRAPH_INSTALLER.equals( name )) {
			if( ! value.matches( ParsingConstants.PATTERN_ID ))
				result.add( new ModelError( ErrorCode.PM_INVALID_INSTALLER_NAME, line ));

		} else if( Constants.PROPERTY_INSTANCE_NAME.equals( name )) {
			// nothing

		} else if( Constants.PROPERTY_INSTANCE_CHANNEL.equals( name )) {
			// TODO: what is expected?

		} else {
			ModelError error = new ModelError( ErrorCode.PM_UNKNOWN_PROPERTY_NAME, line );
			error.setDetails( "Property name: " + name );
			result.add( error );
		}

		return result;
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( BlockComment block ) {

		// Only makes sense when a comment section was created programmatically.
		Collection<ModelError> result = new ArrayList<ModelError> ();
		int cpt = 0;
		for( String s : block.getContent().split( "\n" )) {
			if( ! s.trim().startsWith( ParsingConstants.COMMENT_DELIMITER ))
				result.add( new ModelError( ErrorCode.PM_MALFORMED_COMMENT, block.getLine() + cpt ));

			cpt ++;
		}

		return result;
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( BlockBlank block ) {

		// Only makes sense when a BlockBlank section was created programmatically.
		Collection<ModelError> result = new ArrayList<ModelError> ();
		if( ! Utils.isEmptyOrWhitespaces( block.getContent()))
			result.add( new ModelError( ErrorCode.PM_MALFORMED_BLANK, block.getLine()));

		return result;
	}


	/**
	 * @param holder
	 * @param strict true if only supported properties are allowed, false otherwise
	 * @return a non-null list of {@link ModelError}s
	 */
	private static Collection<ModelError> validatePropertiesHolder( AbstractBlockHolder holder, boolean strict ) {

		// Check the name
		Collection<ModelError> result = new ArrayList<ModelError> ();
		String name = holder.getName();
		if( Utils.isEmptyOrWhitespaces( name ))
			result.add( new ModelError( holder.getInstructionType() == AbstractBlock.FACET ? ErrorCode.PM_EMPTY_FACET_NAME : ErrorCode.PM_EMPTY_COMPONENT_NAME, holder.getLine()));
		else if( ! name.matches( ParsingConstants.PATTERN_ID ))
			result.add( new ModelError( holder.getInstructionType() == AbstractBlock.FACET ? ErrorCode.PM_INVALID_FACET_NAME : ErrorCode.PM_INVALID_COMPONENT_NAME, holder.getLine()));
		else if( name.contains( "." ))
			result.add( new ModelError( ErrorCode.PM_DOT_IS_NOT_ALLOWED, holder.getLine()));

		// Check all the properties have a value
		List<String> supportedProperties = Arrays.asList( holder.getSupportedPropertyNames());
		Set<String> foundProperties = new HashSet<String> ();
		for( AbstractBlock region : holder.getInnerBlocks()) {

			if( region.getInstructionType() == AbstractBlock.PROPERTY ) {
				BlockProperty p = (BlockProperty) region;
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
