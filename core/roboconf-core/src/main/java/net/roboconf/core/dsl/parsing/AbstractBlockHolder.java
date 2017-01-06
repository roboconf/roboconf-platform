/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.dsl.parsing;

import java.util.ArrayList;
import java.util.List;

/**
 * Blocks that can contain other blocks.
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractBlockHolder extends AbstractBlock {

	/**
	 * The element's name.
	 */
	private String name;

	/**
	 * The in-line comment after a closing bracket.
	 */
	private String closingInlineComment;

	/**
	 * The blocks within this element.
	 */
	private final List<AbstractBlock> innerBlocks = new ArrayList<AbstractBlock> ();


	/**
	 * Constructor.
	 * @param declaringFile the definition file (not null)
	 */
	public AbstractBlockHolder( FileDefinition declaringFile ) {
		super( declaringFile );
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * @return the closingInlineComment
	 */
	public String getClosingInlineComment() {
		return this.closingInlineComment;
	}

	/**
	 * @param closingInlineComment the closingInlineComment to set
	 */
	public void setClosingInlineComment( String closingInlineComment ) {
		this.closingInlineComment = closingInlineComment;
	}

	/**
	 * Finds a property by its name among the ones this instance owns.
	 * @param propertyName not null
	 * @return the property, or null if it was not found
	 */
	public BlockProperty findPropertyBlockByName( String propertyName ) {

		BlockProperty result = null;
		for( AbstractBlock region : this.innerBlocks ) {
			if( region.getInstructionType() == PROPERTY
					&& propertyName.equals(((BlockProperty) region).getName())) {

				result = (BlockProperty) region;
				break;
			}
		}

		return result;
	}

	/**
	 * Finds properties by name among the ones this instance owns.
	 * @param propertyName not null
	 * @return a non-null list of properties associated with this property name
	 */
	public List<BlockProperty> findPropertiesBlockByName( String propertyName ) {

		List<BlockProperty> result = new ArrayList<> ();
		for( AbstractBlock region : this.innerBlocks ) {
			if( region.getInstructionType() == PROPERTY
					&& propertyName.equals(((BlockProperty) region).getName())) {

				result.add((BlockProperty) region);
			}
		}

		return result;
	}

	/**
	 * @return the innerBlocks
	 */
	public List<AbstractBlock> getInnerBlocks() {
		return this.innerBlocks;
	}

	/**
	 * @return the supported properties (not null)
	 * @see net.roboconf.core.Constants
	 */
	public abstract String[] getSupportedPropertyNames();
}
