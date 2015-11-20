/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

/**
 * A set of constants.
 * @author Vincent Zurczak - Linagora
 */
public interface Constants {

	/**
	 * The wild card symbol (can be used in a component to reference any variable for a given group).
	 */
	String WILDCARD = "*";

	/**
	 * The name of the target installer (which is somehow specific).
	 */
	String TARGET_INSTALLER = "target";

	/**
	 * The name of the file that contain the target properties.
	 */
	String TARGET_PROPERTIES_FILE_NAME = "target.properties";

	/**
	 * The {@value #TARGET_PROPERTY_HANDLER} property in {@value #TARGET_PROPERTIES_FILE_NAME} files.
	 */
	String TARGET_PROPERTY_HANDLER = "handler";

	/**
	 * The {@value #TARGET_PROPERTY_NAME} property in {@value #TARGET_PROPERTIES_FILE_NAME} files.
	 */
	String TARGET_PROPERTY_NAME = "name";

	/**
	 * The {@value #TARGET_PROPERTY_DESCRIPTION} property in {@value #TARGET_PROPERTIES_FILE_NAME} files.
	 */
	String TARGET_PROPERTY_DESCRIPTION = "description";

	/**
	 * The heart beat period (in milliseconds).
	 */
	long HEARTBEAT_PERIOD = 60000;


	/**
	 * The <strong>graph</strong> directory.
	 */
	String PROJECT_DIR_GRAPH = "graph";

	/**
	 * The <strong>descriptor</strong> directory.
	 */
	String PROJECT_DIR_DESC = "descriptor";

	/**
	 * The <strong>instances</strong> directory.
	 */
	String PROJECT_DIR_INSTANCES = "instances";

	/**
	 * The <strong>autonomic</strong> directory.
	 */
	String PROJECT_DIR_AUTONOMIC = "autonomic";

	/**
	 * The <strong>commands</strong> directory.
	 */
	String PROJECT_DIR_COMMANDS = "commands";

	/**
	 * The <strong>application.properties</strong> file name.
	 */
	String PROJECT_FILE_DESCRIPTOR = "application.properties";

	/**
	 * The <strong>measures</strong> file extension.
	 */
	String FILE_EXT_MEASURES = ".measures";

	/**
	 * The <strong>rules</strong> file extension.
	 */
	String FILE_RULES = "rules.cfg";


	/**
	 * A specific variable whose value is set at runtime.
	 */
	String SPECIFIC_VARIABLE_IP = "ip";

	/**
	 * The default group ID for official Roboconf recipes.
	 */
	String OFFICIAL_RECIPES_GROUP_ID = "net.roboconf.recipes";

	/**
	 * A constant used for generated applications (used with recipes).
	 */
	String GENERATED = "generated";
}
