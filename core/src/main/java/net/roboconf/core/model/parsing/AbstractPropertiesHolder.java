/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Group common code for facet and component instructions.
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractPropertiesHolder extends AbstractInstruction {

	/**
	 * The element's name.
	 */
	private String name;

	/**
	 * The in-line comment after a closing bracket.
	 */
	private String closingInlineComment;

	/**
	 * The instructions within this element.
	 */
	private final Collection<AbstractInstruction> internalInstructions = new ArrayList<AbstractInstruction> ();

	/**
	 * Facet and component properties.
	 * <p>
	 * Map key = property names.<br />
	 * Map values = properties with value and comments.<br />
	 * Insertion order is preserved.
	 * </p>
	 */
	private final Map<String,RelationProperty> propertyNameToProperty =
			new LinkedHashMap<String,RelationProperty>( Constants.DEFAULT_PROPERTIES_COUNT );


	/**
	 * Constructor.
	 * @param declaringFile not null
	 */
	public AbstractPropertiesHolder( AbstractFile declaringFile ) {
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
	 * @return the properties
	 * <p>
	 * Map key = property names.<br />
	 * Map values = properties with value and comments.<br />
	 * Insertion order is preserved.
	 * </p>
	 */
	public Map<String,RelationProperty> getPropertyNameToProperty() {
		return this.propertyNameToProperty;
	}

	/**
	 * @return the internalInstructions
	 */
	public Collection<AbstractInstruction> getInternalInstructions() {
		return this.internalInstructions;
	}

	/**
	 * @return the supported properties (not null)
	 * @see Constants
	 */
	public abstract String[] getSupportedPropertyNames();
}
