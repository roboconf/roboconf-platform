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

/**
 * A set of constants related to parsing of configuration files.
 * @author Vincent Zurczak - Linagora
 */
public interface Constants {

	/**
	 * The symbols that indicates the beginning of a comment.
	 */
	String COMMENT_DELIMITER = "#";

	/**
	 * The symbols that indicates the beginning of a comment.
	 */
	String PROPERTY_SEPARATOR = ",";

	/**
	 * The symbols that indicates the beginning of a comment.
	 */
	String EQUAL_OPERATOR = "=";

	/**
	 * The marker indicating a generated index must be appended to an instance name.
	 */
	String INSTANCE_INDEX_MARKER = "${index}";


	/**
	 * Keyword to declare an import.
	 */
	String KEYWORD_IMPORT = "import";

	/**
	 * Keyword to declare a facet.
	 */
	String KEYWORD_FACET = "facet";

	/**
	 * Keyword to declare an instance.
	 */
	String KEYWORD_INSTANCE_OF = "instanceof";

	/**
	 * Keyword to declare a joker instance.
	 */
	String KEYWORD_INSTANCE_GROUP = "<group>";


	/**
	 * The pattern for an ID (variable, plug-in, etc).
	 */
	String PATTERN_ID = "[a-zA-Z_](\\w|-|\\.)*";

	/**
	 * The pattern for an image file name.
	 */
	String PATTERN_IMAGE = "[^\\s]+\\.((png)|(gif)|(jpg)|(jpeg))$";




	/**
	 * Keyword for facet property.
	 */
	String PROPERTY_FACET_EXTENDS = "extends";


	/**
	 * Keyword for component property.
	 */
	String PROPERTY_COMPONENT_ALIAS = "alias";

	/**
	 * Keyword for component property.
	 */
	String PROPERTY_COMPONENT_FACETS = "facets";

	/**
	 * Keyword for component property.
	 */
	String PROPERTY_COMPONENT_IMPORTS = "imports";


	/**
	 * Keyword for a facet or a component property.
	 */
	String PROPERTY_GRAPH_EXPORTS = "exports";

	/**
	 * Keyword for a facet or a component property.
	 */
	String PROPERTY_GRAPH_CHILDREN = "children";

	/**
	 * Keyword for a facet or a component property.
	 */
	String PROPERTY_GRAPH_ICON_LOCATION = "icon";

	/**
	 * Keyword for a facet or a component property.
	 */
	String PROPERTY_GRAPH_INSTALLER = "installer";


	/**
	 * Keyword for an instance property.
	 */
	String PROPERTY_INSTANCE_NAME = "name";

	/**
	 * Keyword for an instance property.
	 */
	String PROPERTY_INSTANCE_CARDINALITY = "cardinality";

	/**
	 * Keyword for an instance property.
	 */
	String PROPERTY_INSTANCE_CHANNEL = "channel";
}
