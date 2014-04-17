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

package net.roboconf.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.roboconf.core.model.parsing.ParsingConstants;

/**
 * All the error codes that can be encountered with a Roboconf model.
 * @author Vincent Zurczak - Linagora
 */
public enum ErrorCode {

	// Parsing Errors
	P_IO_ERROR( ErrorLevel.SEVERE, ErrorCategory.PARSING, "An I/O exception occurred, parsing was interrupted." ),
	P_ONE_BLOCK_PER_LINE( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Extra-characters were found after the semi-colon. You cannot have more than one block per line." ),
	P_PROPERTY_ENDS_WITH_SEMI_COLON( ErrorLevel.SEVERE, ErrorCategory.PARSING, "A property definition must end with a semi-colon." ),
	P_IMPORT_ENDS_WITH_SEMI_COLON( ErrorLevel.SEVERE, ErrorCategory.PARSING, "An import declaration must end with a semi-colon." ),
	P_O_C_BRACKET_EXTRA_CHARACTERS( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Extra-characters were found after the opening curly bracket." ),
	P_C_C_BRACKET_EXTRA_CHARACTERS( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Extra-characters were found after the closing curly bracket." ),
	P_O_C_BRACKET_MISSING( ErrorLevel.SEVERE, ErrorCategory.PARSING, "A facet, component or instance name must be followed by an opening curly bracket." ),
	P_C_C_BRACKET_MISSING( ErrorLevel.SEVERE, ErrorCategory.PARSING, "A facet, component or instance declaration must end with a closing curly bracket." ),
	P_UNRECOGNIZED_BLOCK( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Unrecognized block. 'import', 'facet', 'instanceof' or a component name were expected." ),
	P_INVALID_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Invalid content. A property was expected." ),
	P_INVALID_PROPERTY_OR_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Invalid content. A property or an instance was expected." ),
	P_INVALID_FILE_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Invalid file type. It mixes facet, component and instance definitions." ),
	P_NO_FILE_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING, "No file type. No import, facet, component or instance definition was found." ),

	// Parsing Model Errors
	PM_INVALID_BLOCK_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Validation failed. Unknown block type." ),
	PM_UNKNOWN_PROPERTY_NAME( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "Unknown property name for a facet or a component." ),
	PM_FORBIDDEN_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "This name is not allowed for a facet or a component." ),
	PM_EMPTY_PROPERTY_VALUE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The property value is missing." ),
	PM_EMPTY_IMPORT_LOCATION( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The import location must be specified." ),
	PM_EMPTY_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The facet name is missing." ),
	PM_EMPTY_REFERENCED_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "A facet name is missing." ),
	PM_EMPTY_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The component name is missing." ),
	PM_EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "A variable name is missing." ),

	PM_INVALID_CHILD_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid child name. As a reminder, children names must be separated by a comma." ),
	PM_INVALID_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid facet name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	PM_INVALID_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid component name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	PM_INVALID_EXPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid variable name. Exported variable names must be separated by a comma." ),
	PM_INVALID_IMPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid variable name. Imported variable names must be separated by a comma." ),
	PM_INVALID_INSTALLER_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid installer name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	PM_INVALID_ICON_LOCATION( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The icon location must end with an image extension (gif, jpg, jpeg, png)." ),
	PM_INVALID_INSTANCE_ELEMENT( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "An instance can only contain properties, other instances, blank lines or comments." ),

	PM_PROPERTY_NOT_APPLIABLE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "This property does not apply to this element." ),
	PM_DUPLICATE_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "This property was already set for this element." ),
	PM_MISSING_ALIAS_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The 'alias' property is missing." ),
	PM_MISSING_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The 'name' property is missing." ),
	PM_INVALID_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid instance name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),

	PM_DOT_IS_NOT_ALLOWED( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Component and facet names cannot contain a dot." ),
	PM_INCOMPLETE_IMPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Incomplete variable name. Imported variable names must be prefixed by a component or facet name, followed by a dot." ),
	PM_MALFORMED_COMMENT( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "A comment delimiter is missing. It will be added at serialization time." ),
	PM_MALFORMED_BLANK( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "A blank section contains non-blank characters. It will be ignored at serialization time." ),

	// Conversion Errors
	CO_NOT_A_GRAPH( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A graph file imports a file which is not a graph file." ),
	CO_UNREACHABLE_FILE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A configuration file could not be read." ),
	CO_UNRESOLVED_FACET( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "The facet could not be resolved. It was not declared anywhere." ),
	CO_CYCLE_IN_FACETS( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A cycle was found in facet definitions." ),
	CO_ALREADY_DEFINED_FACET( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "This facet was defined more than once." ),
	CO_ALREADY_DEFINED_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "This component was defined more than once." ),
	CO_ALREADY_DEFINED_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "This instance was defined more than once." ),
	CO_NOT_OVERRIDING( ErrorLevel.WARNING, ErrorCategory.CONVERSION, "A variable is exported in the instance but was not defined in its component." ),
	CO_AMBIGUOUS_OVERRIDING( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A variable is exported in the instance but could not be resolved in its component." ),
	CO_AMBIGUOUS_INSTALLER( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A component does not specify its installer but inherits several ones from its facets." ),

	// Runtime Model Errors
	RM_MISSING_APPLICATION_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The application name is missing." ),
	RM_MISSING_APPLICATION_QUALIFIER( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "The application qualifier is missing." ),
	RM_MISSING_APPLICATION_GEP( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The entry-point for graph(s) is missing." ),
	RM_MISSING_APPLICATION_GRAPHS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "An application must have a graph definition." ),

	RM_DOT_IS_NOT_ALLOWED( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Component names cannot contain a dot." ),
	RM_EMPTY_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The component name cannot be empty." ),
	RM_INVALID_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid component name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_EMPTY_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "This component references a facet whose name is null or empty." ),
	RM_INVALID_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid facet name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_EMPTY_COMPONENT_ALIAS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The component alias is mandatory." ),
	RM_EMPTY_COMPONENT_INSTALLER( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The component's installer name is mandatory." ),
	RM_INVALID_COMPONENT_INSTALLER( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid installer name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_COMPONENT_IMPORTS_EXPORTS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The component imports a variable it exports (the import should be marked as optional)." ),

	RM_INVALID_EXPORT_PREFIX( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "An exported variable must be prefixed by the component name or by a facet name." ),
	RM_INVALID_EXPORT_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "An exported variable cannot be empty." ),
	RM_EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A variable name cannot be empty or null." ),
	RM_INVALID_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid variable name. Expected pattern: " + ParsingConstants.PATTERN_ID ),

	RM_DUPLICATE_COMPONENT( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "This component was already defined." ),
	RM_CYCLE_IN_COMPONENTS( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "This component was already defined." ),
	RM_NO_ROOT_COMPONENT( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "No root component was found in the graph." ),
	RM_UNRESOLVABLE_VARIABLE( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "A variable is imported but no component exports it." ),

	RM_EMPTY_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The instance name cannot be empty." ),
	RM_INVALID_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid instance name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_EMPTY_INSTANCE_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The instance is not associated with a component." ),
	RM_MAGIC_INSTANCE_VARIABLE( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "An instance exports a variable which is not defined in its component (no override)." ),
	RM_MISSING_INSTANCE_PARENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "According to the graph(s), this instance should have a parent instance." ),
	RM_INVALID_INSTANCE_PARENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "According to the graph(s), this instance cannot have this parent instance." ),

	// Projects Errors
	PROJ_NO_GRAPH_DIR( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "A Roboconf project must contain a 'graph' directory with the graph(s) definition(s)." ),
	PROJ_NO_DESC_DIR( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "A Roboconf project must contain a 'descriptor' directory with the application description." ),
	PROJ_NO_DESC_FILE( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The application's descriptor was not found (application.properties)." ),
	PROJ_MISSING_GRAPH_EP( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The entry-point for the graph(s) was not found." ),
	PROJ_MISSING_INSTANCE_EP( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The entry-point for the instance(s) was not found." ),
	PROJ_NOT_A_GRAPH( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "A graph(s) file was expected." ),
	PROJ_NOT_AN_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "An instance(s) file was expected." ),
	PROJ_EXTRACT_TEMP( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "Roboconf failed to extract the ZIP archive (temporary directory)." ),
	PROJ_EXTRACT_ZIP( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "Roboconf failed to extract the ZIP archive (ZIP reading)." ),
	PROJ_DELETE_TEMP( ErrorLevel.WARNING, ErrorCategory.PROJECT, "Roboconf failed to delete a temporary directory." ),
	PROJ_READ_DESC_FILE( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The application's descriptor could not be read." ),


	// Execution Errors
	;


	/**
	 * RoboconfError categories.
	 */
	public enum ErrorCategory {
		PARSING, PARSING_MODEL, CONVERSION, RUNTIME_MODEL, PROJECT, EXECUTION;
	}

	/**
	 * RoboconfError levels.
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
