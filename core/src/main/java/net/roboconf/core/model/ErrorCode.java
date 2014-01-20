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

package net.roboconf.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * All the error codes that can be encountered with a Roboconf model.
 * @author Vincent Zurczak - Linagora
 */
public enum ErrorCode {

	// Parsing Errors
	IO_ERROR( ErrorLevel.SEVERE, ErrorCategory.PARSING, "An I/O exception occurred, parsing was interrupted." ),
	ONE_INSTRUCTION_PER_LINE( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Extra-characters were found after the semi-colon. There must be only one instruction per line." ),
	PROPERTY_ENDS_WITH_SEMI_COLON( ErrorLevel.SEVERE, ErrorCategory.PARSING, "A property definition must end with a semi-colon." ),
	IMPORT_ENDS_WITH_SEMI_COLON( ErrorLevel.SEVERE, ErrorCategory.PARSING, "An import declaration must end with a semi-colon." ),
	O_C_BRACKET_EXTRA_CHARACTERS( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Extra-characters were found after the opening curly bracket." ),
	C_C_BRACKET_EXTRA_CHARACTERS( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Extra-characters were found after the closing curly bracket." ),
	O_C_BRACKET_MISSING( ErrorLevel.SEVERE, ErrorCategory.PARSING, "A facet or component name must be followed by an opening curly bracket." ),
	C_C_BRACKET_MISSING( ErrorLevel.SEVERE, ErrorCategory.PARSING, "A facet or component declaration must end with a closing curly bracket." ),
	UNRECOGNIZED_INSTRUCTION( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Unrecognized instruction. 'import', 'facet' or a component name were expected." ),
	INVALID_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Invalid content. A property was expected." ),

	// Parsing Model Errors
	INVALID_INSTRUCTION_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Validation failed. Unknown instruction type." ),
	UNKNWON_PROPERTY_NAME( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "Unknown property name for a facet or a component." ),
	FORBIDDEN_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "This name is not allowed for a facet or a component." ),
	EMPTY_PROPERTY_VALUE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The property value is missing." ),
	EMPTY_IMPORT_LOCATION( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The import location must be specified." ),
	EMPTY_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The facet name is missing." ),
	EMPTY_REFERENCED_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "A facet name is missing." ),
	EMPTY_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The component name is missing." ),
	EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "A variable name is missing." ),

	INVALID_CHILD_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid child name. As a reminder, children names must be separated by a comma." ),
	INVALID_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid facet name. Facet names must be separated by a comma." ),
	INVALID_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid component name. Component names must be separated by a comma." ),
	INVALID_EXPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid variable name. Exported variable names must be separated by a comma." ),
	INVALID_IMPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid variable name. Imported variable names must be separated by a comma." ),
	INVALID_INSTALLER_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid installer name." ),
	INVALID_ICON_LOCATION( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The icon location must end with an image extension (gif, jpg, jpeg, png)." ),
	PROPERTY_NOT_APPLIABLE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "This property does not apply to this element." ),

	MISSING_ALIAS_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The 'alias' property is missing." ),
	DOT_IS_NOT_ALLOWED( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Component and facet names cannot contain a dot." ),
	INCOMPLETE_IMPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Incomplete variable name. Imported variable names must be prefixed by a component or facet name, followed by a dot." ),
	MALFORMED_COMMENT( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "A comment delimiter is missing. It will be added at serialization time." ),
	MALFORMED_BLANK( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "A blank section contains non-blank characters. It will be ignored at serialization time." ),

	// Conversion Errors
	UNREACHABLE_FILE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A configuration file could not be read." ),
	UNRESOLVED_FACET( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "The facet could not be resolved. It was not declared anywhere." ),
	CYCLE_IN_FACETS( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A cycle was found in facet definitions." ),
	ALREADY_DEFINED_FACET( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "This facet was already defined." ),
	ALREADY_DEFINED_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "This component was already defined." ),

	// Graph Errors

	;


	/**
	 * Error categories.
	 */
	public enum ErrorCategory {
		PARSING, PARSING_MODEL, CONVERSION, RUNTIME_MODEL;
	}

	/**
	 * Error levels.
	 */
	public enum ErrorLevel {
		SEVERE, WARNING;
	}


	// These instructions are called after the enumeration items have been created.
	private static final Map<ErrorCategory,AtomicInteger> CAT_TO_ID = new HashMap<ErrorCategory,AtomicInteger> ();
	static {
		for( ErrorCategory cat : ErrorCategory.values())
			CAT_TO_ID.put( cat, new AtomicInteger( 0 ));

		for( ErrorCode code : values())
			code.errorId = CAT_TO_ID.get( code.category ).getAndIncrement();
	}

	private final String msg;
	private final ErrorCategory category;
	private final ErrorLevel level;
	private int errorId;


	/**
	 * @param level
	 * @param category
	 * @param msg
	 */
	private ErrorCode( ErrorLevel level, ErrorCategory category, String msg ) {
		this.msg = msg;
		this.category = category;
		this.level = level;
	}

	/**
	 * @return the category
	 */
	public ErrorCategory getCategory() {
		return this.category;
	}

	/**
	 * @return the level
	 */
	public ErrorLevel getLevel() {
		return this.level;
	}

	/**
	 * @return the msg
	 */
	public String getMsg() {
		return this.msg;
	}

	/**
	 * @return the errorId
	 */
	public int getErrorId() {
		return this.errorId;
	}
}
