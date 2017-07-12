/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.errors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingConstants;

/**
 * All the error codes that can be encountered with a Roboconf model.
 * @author Vincent Zurczak - Linagora
 */
public enum ErrorCode {

	// Parsing Errors
	P_IO_ERROR( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_ONE_BLOCK_PER_LINE( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_PROPERTY_ENDS_WITH_SEMI_COLON( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_IMPORT_ENDS_WITH_SEMI_COLON( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_O_C_BRACKET_EXTRA_CHARACTERS( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_C_C_BRACKET_EXTRA_CHARACTERS( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_O_C_BRACKET_MISSING( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_C_C_BRACKET_MISSING( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_UNRECOGNIZED_BLOCK( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_INVALID_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_INVALID_PROPERTY_OR_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_INVALID_FILE_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING ),
	P_EMPTY_FILE( ErrorLevel.WARNING, ErrorCategory.PARSING ),

	// Parsing Model Errors
	PM_INVALID_BLOCK_TYPE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_UNKNOWN_PROPERTY_NAME( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL ),
	PM_FORBIDDEN_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_EMPTY_PROPERTY_VALUE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_EMPTY_IMPORT_LOCATION( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_EMPTY_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_EMPTY_REFERENCED_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_EMPTY_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),

	PM_INVALID_CHILD_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, ParsingConstants.PATTERN_ID ),
	PM_INVALID_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, ParsingConstants.PATTERN_ID ),
	PM_INVALID_EXPORT_COMPLEX_VALUE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_INVALID_EXPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_EXTERNAL_IS_KEYWORD_FOR_IMPORTS( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, ParsingConstants.PROPERTY_COMPONENT_EXTERNAL_IMPORT ),
	PM_INVALID_IMPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_INVALID_INSTALLER_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, ParsingConstants.PATTERN_FLEX_ID ),
	PM_INVALID_INSTANCE_ELEMENT( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_INVALID_INSTANCE_COUNT( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_USELESS_INSTANCE_COUNT( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL ),

	PM_PROPERTY_NOT_APPLIABLE( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_DUPLICATE_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_MISSING_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_INVALID_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL, ParsingConstants.PATTERN_FLEX_ID ),

	PM_DOT_IS_NOT_ALLOWED( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_INCOMPLETE_IMPORTED_VAR_NAME( ErrorLevel.SEVERE, ErrorCategory.PARSING_MODEL ),
	PM_MALFORMED_COMMENT( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL ),
	PM_MALFORMED_BLANK( ErrorLevel.WARNING, ErrorCategory.PARSING_MODEL ),

	// Conversion Errors
	CO_NOT_A_GRAPH( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_NOT_INSTANCES( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_UNREACHABLE_FILE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_ALREADY_DEFINED_FACET( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_ALREADY_DEFINED_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_CYCLE_IN_COMPONENTS_INHERITANCE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_ALREADY_DEFINED_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_CONFLICTING_INFERRED_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_INEXISTING_CHILD( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_INEXISTING_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_INEXISTING_FACET( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_CONFLICTING_NAME( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),
	CO_GRAPH_COULD_NOT_BE_BUILT( ErrorLevel.SEVERE, ErrorCategory.CONVERSION ),

	// Runtime Model Errors
	RM_MISSING_APPLICATION_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_APPLICATION_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, ParsingConstants.PATTERN_APP_NAME ),
	RM_MISSING_APPLICATION_DSL_ID( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL ),
	RM_MISSING_APPLICATION_VERSION( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_MISSING_APPLICATION_GEP( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_MISSING_APPLICATION_GRAPHS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_MISSING_APPLICATION_EXPORT_PREFIX( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_APPLICATION_VERSION( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),

	RM_DOT_IS_NOT_ALLOWED( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_EMPTY_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_EMPTY_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, ParsingConstants.PATTERN_FLEX_ID ),
	RM_INVALID_FACET_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, ParsingConstants.PATTERN_FLEX_ID ),
	RM_EMPTY_COMPONENT_INSTALLER( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_COMPONENT_INSTALLER( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, ParsingConstants.PATTERN_FLEX_ID ),
	RM_COMPONENT_IMPORTS_EXPORTS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_ROOT_INSTALLER_MUST_BE_TARGET( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),

	RM_EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, ParsingConstants.PATTERN_ID ),
	RM_INVALID_APPLICATION_EXPORT_PREFIX( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, ParsingConstants.PATTERN_FLEX_ID ),
	RM_MAGIC_INSTANCE_VARIABLE( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL ),
	RM_MISSING_VARIABLE_VALUE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_AMBIGUOUS_OVERRIDING( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_EXTERNAL_EXPORT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_ALREADY_DEFINED_EXTERNAL_EXPORT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_RANDOM_KIND( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_NO_VALUE_FOR_RANDOM( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),

	RM_CYCLE_IN_COMPONENTS( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_CYCLE_IN_COMPONENTS_INHERITANCE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_CYCLE_IN_FACETS_INHERITANCE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_NO_ROOT_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_NOT_A_ROOT_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_UNRESOLVABLE_VARIABLE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_UNRESOLVABLE_FACET_VARIABLE( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),

	RM_EMPTY_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL, ParsingConstants.PATTERN_FLEX_ID ),
	RM_EMPTY_INSTANCE_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_MISSING_INSTANCE_PARENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_INVALID_INSTANCE_PARENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_UNREACHABLE_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.RUNTIME_MODEL ),
	RM_ORPHAN_FACET( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL ),
	RM_ORPHAN_FACET_WITH_CHILDREN( ErrorLevel.WARNING, ErrorCategory.RUNTIME_MODEL ),

	// Projects Errors
	PROJ_NO_GRAPH_DIR( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_NO_DESC_DIR( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_NO_DESC_FILE( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_MISSING_GRAPH_EP( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_MISSING_INSTANCE_EP( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_EXTRACT_TEMP( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_EXTRACT_ZIP( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_DELETE_TEMP( ErrorLevel.WARNING, ErrorCategory.PROJECT ),
	PROJ_READ_DESC_FILE( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_INVALID_EXTERNAL_EXPORTS( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_NO_RESOURCE_DIRECTORY( ErrorLevel.WARNING, ErrorCategory.PROJECT ),
	PROJ_APPLICATION_TEMPLATE_NOT_FOUND( ErrorLevel.SEVERE, ErrorCategory.PROJECT ),
	PROJ_INVALID_COMMAND_EXT( ErrorLevel.SEVERE, ErrorCategory.PROJECT, Constants.FILE_EXT_COMMANDS ),
	PROJ_INVALID_RULE_EXT( ErrorLevel.SEVERE, ErrorCategory.PROJECT, Constants.FILE_EXT_RULE ),
	PROJ_UNREACHABLE_FILE( ErrorLevel.WARNING, ErrorCategory.PROJECT ),
	PROJ_INVALID_FILE_LOCATION( ErrorLevel.WARNING, ErrorCategory.PROJECT ),

	// Recipes Errors
	REC_PUPPET_DISLIKES_WILDCARD_IMPORTS( ErrorLevel.WARNING, ErrorCategory.RECIPES ),
	REC_PUPPET_HAS_NO_RBCF_MODULE( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_PUPPET_HAS_TOO_MANY_RBCF_MODULES( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_PUPPET_MISSING_PARAM_IMPORT_DIFF( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_PUPPET_MISSING_PARAM_RUNNING_STATE( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_PUPPET_MISSING_PARAM_FROM_IMPORT( ErrorLevel.WARNING, ErrorCategory.RECIPES ),
	REC_PUPPET_SYNTAX_ERROR( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_SCRIPT_NO_SCRIPTS_DIR( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_ARTIFACT_ID_IN_LOWER_CASE( ErrorLevel.WARNING, ErrorCategory.RECIPES ),
	REC_MISSING_README( ErrorLevel.WARNING, ErrorCategory.RECIPES ),
	REC_OFFICIAL_GROUP_ID( ErrorLevel.WARNING, ErrorCategory.RECIPES, Constants.OFFICIAL_RECIPES_GROUP_ID ),
	REC_NON_MATCHING_ARTIFACT_ID( ErrorLevel.WARNING, ErrorCategory.RECIPES ),
	REC_AVOID_INSTANCES( ErrorLevel.WARNING, ErrorCategory.RECIPES ),
	REC_TARGET_NO_ID( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_TARGET_NO_HANDLER( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_TARGET_INVALID_FILE_OR_CONTENT( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_TARGET_CONFLICTING_ID( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),
	REC_TARGET_NO_NAME( ErrorLevel.WARNING, ErrorCategory.RECIPES ),
	REC_TARGET_NO_PROPERTIES( ErrorLevel.SEVERE, ErrorCategory.RECIPES ),

	// Commands Errors
	CMD_INVALID_TARGET_ID( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_NO_MATCHING_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_NOT_AN_ACCEPTABLE_PARENT( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_CANNOT_HAVE_ANY_PARENT( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_NOT_A_SCOPED_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_NOT_A_ROOT_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_UNRECOGNIZED_INSTRUCTION( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INVALID_INSTANCE_STATUS( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INSTABLE_INSTANCE_STATUS( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_EMPTY_VARIABLE_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_MISSING_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INVALID_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS, ParsingConstants.PATTERN_FLEX_ID ),
	CMD_MISSING_COMPONENT_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INEXISTING_COMPONENT( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_MISSING_PARENT_INSTANCE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_CONFLICTING_INSTANCE_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_EMAIL_NO_MESSAGE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_NO_INSTRUCTION( ErrorLevel.WARNING, ErrorCategory.COMMANDS ),
	CMD_UNRESOLVED_VARIABLE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INVALID_SYNTAX( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_MISSING_TARGET_FILE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_MISSING_COMMAND_NAME( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INEXISTING_COMMAND( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INEXISTING_COMMAND_FILE( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_LOOPING_COMMAND( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_NASTY_LOOPING_COMMAND( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INVALID_DATE_PATTERN( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_NO_MIX_FOR_PATTERNS( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),
	CMD_INVALID_INDEX_PATTERN( ErrorLevel.SEVERE, ErrorCategory.COMMANDS ),

	// Rule errors
	RULE_IO_ERROR( ErrorLevel.SEVERE, ErrorCategory.RULES ),
	RULE_EMPTY_NAME( ErrorLevel.SEVERE, ErrorCategory.RULES ),
	RULE_INVALID_SYNTAX( ErrorLevel.SEVERE, ErrorCategory.RULES ),
	RULE_EMPTY_WHEN( ErrorLevel.SEVERE, ErrorCategory.RULES ),
	RULE_EMPTY_THEN( ErrorLevel.SEVERE, ErrorCategory.RULES ),
	RULE_UNKNOWN_COMMAND( ErrorLevel.SEVERE, ErrorCategory.RULES ),

	// REST Errors
	REST_TARGET_CONTAINS_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_TARGET_ASSOCIATION_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_TARGET_HINT_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),

	REST_AUTH_NO_MNGR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_AUTH_FAILED( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_SCHEDULER_IS_UNAVAILABLE( ErrorLevel.SEVERE, ErrorCategory.REST ),

	REST_INEXISTING( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_MISSING_PROPERTY( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_DELETION_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_IO_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_UNDETAILED_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_MESSAGING_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_SAVE_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),

	REST_MNGMT_ZIP_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_MNGMT_INVALID_TPL( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_MNGMT_INVALID_URL( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_MNGMT_CONFLICT( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_MNGMT_APP_SHUTDOWN_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_MNGMT_INVALID_IMAGE( ErrorLevel.SEVERE, ErrorCategory.REST ),

	REST_APP_EXEC_ERROR( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_DEBUG_AGENT_KO( ErrorLevel.SEVERE, ErrorCategory.REST ),
	REST_DEBUG_MSG_SENT( ErrorLevel.SEVERE, ErrorCategory.REST ),
	;


	/**
	 * RoboconfError categories.
	 */
	public enum ErrorCategory {
		PARSING, PARSING_MODEL, CONVERSION, RUNTIME_MODEL, PROJECT, RECIPES, COMMANDS, RULES, EXECUTION, REST;
	}

	/**
	 * RoboconfError levels.
	 */
	public enum ErrorLevel {
		SEVERE, WARNING;
	}


	// These instructions are called after the enumeration items have been created.
	private static final Map<ErrorCategory,AtomicInteger> CAT_TO_ID = new HashMap<> ();
	static {
		for( ErrorCategory cat : ErrorCategory.values())
			CAT_TO_ID.put( cat, new AtomicInteger( 0 ));

		for( ErrorCode code : values())
			code.errorId = CAT_TO_ID.get( code.category ).getAndIncrement();
	}

	private final ErrorCategory category;
	private final ErrorLevel level;
	private int errorId;
	private String[] i18nProperties;


	/**
	 * @param level
	 * @param category
	 * @param i18nProperties
	 */
	private ErrorCode( ErrorLevel level, ErrorCategory category, String... i18nProperties ) {
		this.category = category;
		this.level = level;
		this.i18nProperties = i18nProperties;
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
	 * @return the errorId
	 */
	public int getErrorId() {
		return this.errorId;
	}

	/**
	 * @return the i18nProperties (never null)
	 */
	public String[] getI18nProperties() {
		// Return a copy of the properties
		return Arrays.asList( this.i18nProperties ).toArray( new String[ this.i18nProperties.length ]);
	}
}
