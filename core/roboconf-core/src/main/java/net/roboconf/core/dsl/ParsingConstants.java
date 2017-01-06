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

package net.roboconf.core.dsl;

/**
 * A set of constants related to parsing of configuration files.
 * @author Vincent Zurczak - Linagora
 */
public interface ParsingConstants {

	/**
	 * The DSL version.
	 */
	String DSL_VERSION = "roboconf-1.0";


	/**
	 * The symbols that indicates the beginning of a comment.
	 */
	String COMMENT_DELIMITER = "#";

	/**
	 * The symbols that separates properties.
	 */
	String PROPERTY_SEPARATOR = ",";


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
	String KEYWORD_INSTANCE_OF = "instance of";


	/**
	 * The pattern for a flexible ID (allows spaces).
	 */
	String PATTERN_FLEX_ID = "[a-zA-Z_](\\w|-| |\\.)*";

	/**
	 * The pattern for an ID (variable, plug-in, etc).
	 */
	String PATTERN_ID = "[a-zA-Z_](\\w|-|\\.)*";

	/**
	 * The pattern for an application name (allow brackets).
	 */
	String PATTERN_APP_NAME = "[a-zA-Z_](\\w|[-.() ])*";


	/**
	 * Keyword for component property.
	 */
	String PROPERTY_COMPONENT_FACETS = "facets";

	/**
	 * Keyword for component property.
	 */
	String PROPERTY_COMPONENT_IMPORTS = "imports";

	/**
	 * Keyword for component property.
	 */
	String PROPERTY_COMPONENT_OPTIONAL_IMPORT = "(optional)";

	/**
	 * Keyword for component property.
	 */
	String PROPERTY_COMPONENT_EXTERNAL_IMPORT = "external";

	/**
	 * Keyword for component property.
	 */
	String PROPERTY_COMPONENT_INSTALLER = "installer";


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
	String PROPERTY_GRAPH_EXTENDS = "extends";

	/**
	 * A pattern to recognize an exported variable whose value is generated randomly by Roboconf.
	 */
	String PROPERTY_GRAPH_RANDOM_PATTERN = "random\\[([^]]*)\\](.*)";



	/**
	 * Keyword for an instance property.
	 */
	String PROPERTY_INSTANCE_NAME = "name";

	/**
	 * Keyword for an instance property (runtime, not meant to be set manually).
	 */
	String PROPERTY_INSTANCE_DATA = "instance-data";

	/**
	 * Keyword for an instance property (runtime, not meant to be set manually).
	 */
	String PROPERTY_INSTANCE_STATE = "instance-state";

	/**
	 * Keyword for an instance property.
	 */
	String PROPERTY_INSTANCE_CHANNELS = "channels";

	/**
	 * Keyword to create several instances at once.
	 */
	String PROPERTY_INSTANCE_COUNT = "count";

	/**
	 * Prefix to register private data in instances.
	 */
	String PROPERTY_INSTANCE_DATA_PREFIX = "data.";
}
