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
public interface ParsingConstants {

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
	 * The pattern for a flexible ID (allows spaces).
	 */
	String PATTERN_FLEX_ID = "[a-zA-Z_](\\w|-| |\\.)*";

	/**
	 * The pattern for an ID (variable, plug-in, etc).
	 */
	String PATTERN_ID = "[a-zA-Z_](\\w|-|\\.)*";

	/**
	 * The pattern for an image file name.
	 */
	String PATTERN_IMAGE = "[^\\s]+\\.((png)|(gif)|(jpg)|(jpeg))$";
}
