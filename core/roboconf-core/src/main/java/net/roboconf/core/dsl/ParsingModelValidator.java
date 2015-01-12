/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.dsl.parsing.AbstractBlock;
import net.roboconf.core.dsl.parsing.AbstractBlockHolder;
import net.roboconf.core.dsl.parsing.BlockBlank;
import net.roboconf.core.dsl.parsing.BlockComment;
import net.roboconf.core.dsl.parsing.BlockComponent;
import net.roboconf.core.dsl.parsing.BlockFacet;
import net.roboconf.core.dsl.parsing.BlockImport;
import net.roboconf.core.dsl.parsing.BlockInstanceOf;
import net.roboconf.core.dsl.parsing.BlockProperty;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;

/**
 * A set of methods to validate parsing model objects.
 * @author Vincent Zurczak - Linagora
 */
public final class ParsingModelValidator {

	/**
	 * Constructor.
	 */
	private ParsingModelValidator() {
		// nothing
	}


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
		return validatePropertiesHolder( block, true );
	}


	/**
	 * @param block a block
	 * @return a non-null list of {@link ModelError}s
	 */
	public static Collection<ModelError> validate( BlockInstanceOf block ) {

		// Basic checks
		Collection<ModelError> result = validatePropertiesHolder( block, false );
		BlockProperty prop = block.findPropertyBlockByName( ParsingConstants.PROPERTY_INSTANCE_NAME );

		if( prop == null )
			result.add( new ModelError( ErrorCode.PM_MISSING_INSTANCE_NAME, block.getLine()));
		else if( ! prop.getValue().matches( ParsingConstants.PATTERN_FLEX_ID ))
			result.add( new ModelError( ErrorCode.PM_INVALID_INSTANCE_NAME, block.getLine()));

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

		} else if( ParsingConstants.PROPERTY_GRAPH_CHILDREN.equals( name )) {
			for( String s : Utils.splitNicely( value, ParsingConstants.PROPERTY_SEPARATOR )) {
				if( s.matches( ParsingConstants.PATTERN_FLEX_ID ))
					continue;

				ModelError error = new ModelError( ErrorCode.PM_INVALID_CHILD_NAME, line );
				error.setDetails( "Child name: " + s );
				result.add( error );
			}

		} else if( ParsingConstants.PROPERTY_COMPONENT_FACETS.equals( name )
				|| ParsingConstants.PROPERTY_GRAPH_EXTENDS.equals( name )) {

			for( String s : Utils.splitNicely( value, ParsingConstants.PROPERTY_SEPARATOR )) {
				if( Utils.isEmptyOrWhitespaces( s )) {
					result.add( new ModelError( ErrorCode.PM_EMPTY_REFERENCED_NAME, line ));

				} else if( ! s.matches( ParsingConstants.PATTERN_FLEX_ID )) {
					ModelError error = new ModelError( ErrorCode.PM_INVALID_NAME, line );
					error.setDetails( "Invalid name: " + s );
					result.add( error );
				}
			}

		} else if( ParsingConstants.PROPERTY_COMPONENT_IMPORTS.equals( name )) {
			for( String s : Utils.splitNicely( value, ParsingConstants.PROPERTY_SEPARATOR )) {

				if( s.toLowerCase().endsWith( ParsingConstants.PROPERTY_COMPONENT_OPTIONAL_IMPORT ))
					s = s.substring( 0, s.length() - ParsingConstants.PROPERTY_COMPONENT_OPTIONAL_IMPORT.length());

				String patternForImports = ParsingConstants.PATTERN_ID;
				patternForImports += "(\\.\\*)?";

				s = s.trim();
				if( Utils.isEmptyOrWhitespaces( s ))
					result.add( new ModelError( ErrorCode.PM_EMPTY_VARIABLE_NAME, line ));
				else if( ! s.matches( patternForImports ))
					result.add( new ModelError( ErrorCode.PM_INVALID_IMPORTED_VAR_NAME, line, s ));
				else if( ! s.contains( "." )
						|| s.indexOf( '.' ) == s.length() -1 )
					result.add( new ModelError( ErrorCode.PM_INCOMPLETE_IMPORTED_VAR_NAME, line, s ));
			}

		} else if( ParsingConstants.PROPERTY_GRAPH_EXPORTS.equals( name )) {
			for( String s : Utils.splitNicely( value, ParsingConstants.PROPERTY_SEPARATOR )) {
				Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
				if( Utils.isEmptyOrWhitespaces( entry.getKey()))
					result.add( new ModelError( ErrorCode.PM_EMPTY_VARIABLE_NAME, line ));
				else if( ! entry.getKey().matches( ParsingConstants.PATTERN_ID ))
					result.add( new ModelError( ErrorCode.PM_INVALID_EXPORTED_VAR_NAME, line, s ));
			}


		} else if( ParsingConstants.PROPERTY_COMPONENT_INSTALLER.equals( name )) {
			if( ! value.matches( ParsingConstants.PATTERN_FLEX_ID ))
				result.add( new ModelError( ErrorCode.PM_INVALID_INSTALLER_NAME, line, value ));

		} else if( ParsingConstants.PROPERTY_INSTANCE_NAME.equals( name )) {
			// nothing

		} else if( ParsingConstants.PROPERTY_INSTANCE_CHANNELS.equals( name )) {
			// TODO: what is expected?

		} else if( ParsingConstants.PROPERTY_INSTANCE_DATA.equals( name )) {
			// TODO: validate it?

		} else if( ParsingConstants.PROPERTY_INSTANCE_STATE.equals( name )) {
			// TODO: validate it?

		} else if( ParsingConstants.PROPERTY_INSTANCE_COUNT.equals( name )) {
			int count = -1;
			try {
				count = Integer.parseInt( value );

			} catch( NumberFormatException e ) {
				// nothing
			}

			if( count < 1 )
				result.add( new ModelError( ErrorCode.PM_INVALID_INSTANCE_COUNT, line ));
			else if( count == 1 )
				result.add( new ModelError( ErrorCode.PM_USELESS_INSTANCE_COUNT, line ));

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
		else if( ! name.matches( ParsingConstants.PATTERN_FLEX_ID ))
			result.add( new ModelError( holder.getInstructionType() == AbstractBlock.FACET ? ErrorCode.PM_INVALID_NAME : ErrorCode.PM_INVALID_COMPONENT_NAME, holder.getLine()));
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
