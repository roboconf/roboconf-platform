/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.roboconf.core.dsl.ParsingConstants;

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
	P_INVALID_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Syntax error: a property was expected." ),
	P_INVALID_PROPERTY_OR_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Syntax error: a property or an instance was expected." ),
	P_INVALID_FILE_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING, "Invalid file type. It mixes facet, component and instance definitions." ),
	P_NO_FILE_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING, "No file type. No import, facet, component or instance definition was found." ),

	// Parsing Model Errors
	PM_INVALID_BLOCK_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Validation failed. Unknown block type." ),
	PM_UNKNOWN_PROPERTY_NAME( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "Unknown property name." ),
	PM_FORBIDDEN_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "This name is not allowed for a facet or a component." ),
	PM_EMPTY_PROPERTY_VALUE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The property value is missing." ),
	PM_EMPTY_IMPORT_LOCATION( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The import location must be specified." ),
	PM_EMPTY_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The facet name is missing." ),
	PM_EMPTY_REFERENCED_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "A name is missing in the list." ),
	PM_EMPTY_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The component name is missing." ),
	PM_EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "A variable name is missing." ),

	PM_INVALID_CHILD_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid child name. As a reminder, children names must be separated by a comma." ),
	PM_INVALID_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "An invalid name was found. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	PM_INVALID_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid component name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	PM_INVALID_EXPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid variable name. Exported variable names must be separated by a comma." ),
	PM_EXTERNAL_IS_KEYWORD_FOR_IMPORTS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, ParsingConstants.PROPERTY_COMPONENT_EXTERNAL_IMPORT + " is a keyword reserved for imports." ),
	PM_INVALID_IMPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid variable name. Imported variable names must be separated by a comma." ),
	PM_INVALID_INSTALLER_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid installer name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	PM_INVALID_INSTANCE_ELEMENT( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "An instance can only contain properties, other instances, blank lines or comments." ),
	PM_INVALID_INSTANCE_COUNT( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The 'count' property for instances must be a positive integer." ),
	PM_USELESS_INSTANCE_COUNT( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "The 'count' property is useless here, the default value is already 1." ),

	PM_PROPERTY_NOT_APPLIABLE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "This property does not apply to this element." ),
	PM_DUPLICATE_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "This property was already set for this element." ),
	PM_MISSING_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "The 'name' property is missing." ),
	PM_INVALID_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Invalid instance name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),

	PM_DOT_IS_NOT_ALLOWED( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Component and facet names cannot contain a dot." ),
	PM_INCOMPLETE_IMPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, "Incomplete variable name. Imported variable names must be prefixed by a component or facet name, followed by a dot." ),
	PM_MALFORMED_COMMENT( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "A comment delimiter is missing. It will be added at serialization time." ),
	PM_MALFORMED_BLANK( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL, "A blank section contains non-blank characters. It will be ignored at serialization time." ),

	// Conversion Errors
	CO_NOT_A_GRAPH( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A graph file imports a file which is not a graph file." ),
	CO_NOT_INSTANCES( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A instance definition file imports a file which is not an instance definition." ),
	CO_UNREACHABLE_FILE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A configuration file could not be read." ),
	CO_ALREADY_DEFINED_FACET( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "This facet was defined more than once." ),
	CO_ALREADY_DEFINED_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "This component was defined more than once." ),
	CO_CYCLE_IN_COMPONENTS_INHERITANCE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A component directly or indirectly extends itself. Such a cycle is forbidden." ),
	CO_ALREADY_DEFINED_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "This instance was defined more than once." ),
	CO_CONFLICTING_INFERRED_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "An inferred instance (count = ...) is in conflict with another instance. They both have the same name and path." ),
	CO_INEXISTING_CHILD( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "No child matching this name (component or facet) was found." ),
	CO_INEXISTING_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "No component with this name was found." ),
	CO_INEXISTING_FACET( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "No facet with this name was found." ),
	CO_CONFLICTING_NAME( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "A facet and a component have the same name. This can lead to ambiguities when resolving children." ),
	CO_GRAPH_COULD_NOT_BE_BUILT( ErrorLevel.SEVERE, ErrorCategory.CONVERSION, "The graph(s) could not be built." ),

	// Runtime Model Errors
	RM_MISSING_APPLICATION_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The application name is missing." ),
	RM_MISSING_APPLICATION_DSL_ID( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "The DSL identifier to read model files is missing. The default parser will be used." ),
	RM_MISSING_APPLICATION_QUALIFIER( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "The application qualifier is missing." ),
	RM_MISSING_APPLICATION_GEP( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The entry-point for graph(s) is missing." ),
	RM_MISSING_APPLICATION_GRAPHS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "An application must have a graph definition." ),
	RM_MISSING_APPLICATION_EXPORT_PREFIX( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The application exports external variables but did not specify the external prefix property." ),

	RM_DOT_IS_NOT_ALLOWED( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Component and facet names cannot contain a dot." ),
	RM_EMPTY_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The component name cannot be empty." ),
	RM_EMPTY_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The facet name cannot be empty." ),
	RM_INVALID_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid component name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_INVALID_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid facet name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_EMPTY_COMPONENT_INSTALLER( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The component's installer name is mandatory." ),
	RM_INVALID_COMPONENT_INSTALLER( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid installer name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_COMPONENT_IMPORTS_EXPORTS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The component imports a variable it exports (the import should be marked as optional)." ),
	RM_ROOT_INSTALLER_MUST_BE_TARGET( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The installer name for root components must be called 'target'." ),

	RM_EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A variable name cannot be empty or null." ),
	RM_INVALID_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid variable name. Expected pattern: " + ParsingConstants.PATTERN_ID ),
	RM_INVALID_APPLICATION_EXPORT_PREFIX( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid prefix for external exports. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_MAGIC_INSTANCE_VARIABLE( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "A variable is exported in the instance but was not defined in its component." ),
	RM_MISSING_VARIABLE_VALUE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "All the variables (except network ones, such as IP) must have a value." ),
	RM_AMBIGUOUS_OVERRIDING( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A variable is exported in the instance but is ambiguously resolved in its component." ),
	RM_INVALID_EXTERNAL_EXPORT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The application exports a variable that does not exist in the graph." ),
	RM_ALREADY_DEFINED_EXTERNAL_EXPORT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The application exports a variable more than once. This is probably a copy/paste error." ),
	RM_INVALID_RANDOM_KIND( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A component exports a variable whose value is generated randomly by Roboconf, but its type is invalid." ),
	RM_NO_VALUE_FOR_RANDOM( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A component exports a variable whose value is generated randomly by Roboconf, it cannot have a value set in the graph." ),

	RM_CYCLE_IN_COMPONENTS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A cycle was detected in the graph(s)." ),
	RM_CYCLE_IN_COMPONENTS_INHERITANCE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A component directly or indirectly extends itself. Such a cycle is forbidden." ),
	RM_CYCLE_IN_FACETS_INHERITANCE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A facet directly or indirectly extends itself. Such a cycle is forbidden." ),
	RM_NO_ROOT_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "No root component was found in the graph." ),
	RM_NOT_A_ROOT_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The graph(s) references a component as a root component while this component has ancestors." ),
	RM_UNRESOLVABLE_VARIABLE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A variable is imported but no component exports it." ),
	RM_UNRESOLVABLE_FACET_VARIABLE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "A facet variable is imported but no component inherits from this facet." ),

	RM_EMPTY_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The instance name cannot be empty." ),
	RM_INVALID_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "Invalid instance name. Expected pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	RM_EMPTY_INSTANCE_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "The instance is not associated with a component." ),
	RM_MISSING_INSTANCE_PARENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "According to the graph(s), this instance should have a parent instance." ),
	RM_INVALID_INSTANCE_PARENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "According to the graph(s), this instance cannot have this parent instance." ),
	RM_UNREACHABLE_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, "This component will never be instantiated. Only a facet references it as a child and no component uses this facet." ),
	RM_ORPHAN_FACET( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "This facet is not used by any component in the graph." ),
	RM_ORPHAN_FACET_WITH_CHILDREN( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL, "This facet has children and is not used by any component in the graph." ),

	// Projects Errors
	PROJ_NO_GRAPH_DIR( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "A Roboconf project must contain a 'graph' directory with the graph(s) definition(s)." ),
	PROJ_NO_DESC_DIR( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "A Roboconf project must contain a 'descriptor' directory with the application description." ),
	PROJ_NO_DESC_FILE( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The application's descriptor was not found (application.properties)." ),
	PROJ_MISSING_GRAPH_EP( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The entry-point for the graph(s) was not found." ),
	PROJ_MISSING_INSTANCE_EP( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The entry-point for the instance(s) was not found." ),
	PROJ_EXTRACT_TEMP( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "Roboconf failed to extract the ZIP archive (temporary directory)." ),
	PROJ_EXTRACT_ZIP( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "Roboconf failed to extract the ZIP archive (ZIP reading)." ),
	PROJ_DELETE_TEMP( ErrorLevel.WARNING, ErrorCategory.PROJECT, "Roboconf failed to delete a temporary directory." ),
	PROJ_READ_DESC_FILE( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The application's descriptor could not be read." ),
	PROJ_INVALID_EXTERNAL_EXPORTS( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "Invalid external exports were found in the application's descriptor." ),
	PROJ_NO_RESOURCE_DIRECTORY( ErrorLevel.WARNING, ErrorCategory.PROJECT, "A graph(s) component has no resource (recipe) directory." ),
	PROJ_APPLICATION_TEMPLATE_NOT_FOUND( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "The application template was not found." ),
	PROJ_INVALID_COMMAND_EXT( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "A command script file must use the " + Constants.FILE_EXT_COMMANDS + " extension." ),
	PROJ_INVALID_RULE_EXT( ErrorLevel.SEVERE, ErrorCategory.PROJECT, "An autonomic rule file must use the " + Constants.FILE_EXT_RULE + " extension." ),

	// Recipes Errors
	REC_PUPPET_DISLIKES_WILDCARD_IMPORTS( ErrorLevel.WARNING, ErrorCategory.RECIPES, "Puppet modules may encounter problems with Roboconf wildcard imports." ),
	REC_PUPPET_HAS_NO_RBCF_MODULE( ErrorLevel.SEVERE, ErrorCategory.RECIPES, "There must be a Puppet module whose name starts with 'roboconf_'." ),
	REC_PUPPET_HAS_TOO_MANY_RBCF_MODULES( ErrorLevel.SEVERE, ErrorCategory.RECIPES, "There must be only ONE Puppet module whose name starts with 'roboconf_'." ),
	REC_PUPPET_MISSING_PARAM_IMPORT_DIFF( ErrorLevel.SEVERE, ErrorCategory.RECIPES, "Puppet scripts that handle updates must have an 'importDiff' parameter." ),
	REC_PUPPET_MISSING_PARAM_RUNNING_STATE( ErrorLevel.SEVERE, ErrorCategory.RECIPES, "Puppet scripts must have a 'runningState' parameter." ),
	REC_PUPPET_MISSING_PARAM_FROM_IMPORT( ErrorLevel.WARNING, ErrorCategory.RECIPES, "Component imports mean the Puppet module must have matching parameters." ),
	REC_PUPPET_SYNTAX_ERROR( ErrorLevel.SEVERE, ErrorCategory.RECIPES, "The 'puppet parser validate' found errors in a PP file." ),
	REC_SCRIPT_NO_SCRIPTS_DIR( ErrorLevel.SEVERE, ErrorCategory.RECIPES, "Scripts must be placed under a 'scripts' directory." ),
	REC_ARTIFACT_ID_IN_LOWER_CASE( ErrorLevel.WARNING, ErrorCategory.RECIPES, "Recipe projects' artifact ID should be in lower case." ),
	REC_MISSING_README( ErrorLevel.WARNING, ErrorCategory.RECIPES, "Recipe projects should contain a readme file." ),
	REC_OFFICIAL_GROUP_ID( ErrorLevel.WARNING, ErrorCategory.RECIPES, "Official recipe projects should use " + Constants.OFFICIAL_RECIPES_GROUP_ID + " as their group ID." ),
	REC_NON_MATCHING_ARTIFACT_ID( ErrorLevel.WARNING, ErrorCategory.RECIPES, "Recipe projects' directories should have the same name than their artifact ID." ),
	REC_AVOID_INSTANCES( ErrorLevel.WARNING, ErrorCategory.RECIPES, "Recipe projects do not have to contain instances definitions." ),

	// Commands Errors
	CMD_INVALID_TARGET_ID( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "An invalid target ID was specified in the command." ),
	CMD_NO_MATCHING_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "No instance was found. A command was probably not written correctly." ),
	CMD_NOT_AN_ACCEPTABLE_PARENT( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "This component cannot be instantiated under this instance." ),
	CMD_CANNOT_HAVE_ANY_PARENT( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "This component cannot have any parent. It cannot be instantiated under this instance." ),
	CMD_NOT_A_SCOPED_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "An instance was supposed to be associated with the 'target' installer." ),
	CMD_NOT_A_ROOT_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "This instruction can only be applied to root instances (no parent)." ),
	CMD_UNRECOGNIZED_INSTRUCTION( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "This instruction was not recognized." ),
	CMD_INVALID_INSTANCE_STATUS( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "An instance status was expected." ),
	CMD_INSTABLE_INSTANCE_STATUS( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "Instance status changes are only valid with stable statuses." ),
	CMD_EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "Variables in Roboconf commands must have a name." ),
	CMD_MISSING_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "An instance name was expected." ),
	CMD_INVALID_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "Instance names must respect the following pattern: " + ParsingConstants.PATTERN_FLEX_ID ),
	CMD_MISSING_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "A component name was expected in the command." ),
	CMD_INEXISTING_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "The component referenced in the command does not exist in the graph." ),
	CMD_MISSING_PARENT_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "A parent instance path was expected in the command." ),
	CMD_CONFLICTING_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "A sibling instance with this name already exists." ),
	CMD_EMAIL_NO_MESSAGE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "The e-mail message cannot be empty." ),
	CMD_NO_INSTRUCTION( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "No valid instruction was found in the file." ),
	CMD_UNRESOLVED_VARIABLE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "A variable is used in a command but it could not be resolved." ),
	CMD_INVALID_SYNTAX( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "Invalid syntax for this command. Please, refer to the documentation." ),
	CMD_MISSING_TARGET_FILE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "The target file is missing." ),
	CMD_MISSING_COMMAND_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "The command name is missing." ),
	CMD_INEXISTING_COMMAND( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "There is no command file with this name." ),
	CMD_LOOPING_COMMAND( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "A command file cannot invoke itself recursively." ),
	CMD_NASTY_LOOPING_COMMAND( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, "An 'execute' instruction cannot invoke a *.commands file that contains this same instruction." ),

	// Rule errors
	RULE_IO_ERROR( ErrorLevel.SEVERE, ErrorCategory.RULES, "I/O error while reading a rule file." ),
	RULE_EMPTY_NAME( ErrorLevel.SEVERE, ErrorCategory.RULES, "The rule name cannot be empty." ),
	RULE_INVALID_SYNTAX( ErrorLevel.SEVERE, ErrorCategory.RULES, "Invalid syntax for this command. Please, refer to the documentation." ),
	RULE_EMPTY_WHEN( ErrorLevel.SEVERE, ErrorCategory.RULES, "The WHEN section cannot be empty, Roboconf events must be specified." ),
	RULE_EMPTY_THEN( ErrorLevel.SEVERE, ErrorCategory.RULES, "The THEN section cannot be empty, Roboconf commands must be listed." ),
	RULE_UNKNOWN_COMMAND( ErrorLevel.SEVERE, ErrorCategory.RULES, "Invalid syntax in rule file. This command could not be found." ),
	;


	/**
	 * RoboconfError categories.
	 */
	public enum ErrorCategory {
		PARSING, PARSING_MODEL, CONVERSION, RUNTIME_MODEL, PROJECT, RECIPES, COMMANDS, RULES, EXECUTION;
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
