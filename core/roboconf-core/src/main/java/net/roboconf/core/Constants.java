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

package net.roboconf.core;

import java.io.File;

/**
 * A set of constants.
 * @author Vincent Zurczak - Linagora
 */
public interface Constants {

	/**
	 * The <strong>measures</strong> file extension.
	 */
	String FILE_EXT_MEASURES = ".measures";

	/**
	 * The <strong>commands</strong> file extension.
	 */
	String FILE_EXT_COMMANDS = ".commands";

	/**
	 * The <strong>properties</strong> file extension.
	 */
	String FILE_EXT_PROPERTIES = ".properties";

	/**
	 * The <strong>graph</strong> file extension.
	 */
	String FILE_EXT_GRAPH = ".graph";

	/**
	 * The <strong>instances</strong> file extension.
	 */
	String FILE_EXT_INSTANCES = ".instances";

	/**
	 * The <strong>drl</strong> file extension.
	 */
	String FILE_EXT_RULE = ".drl";


	/**
	 * The wild card symbol (can be used in a component to reference any variable for a given group).
	 */
	String WILDCARD = "*";

	/**
	 * A constant for agent parameters to indicate it should be reset.
	 */
	String AGENT_RESET = "@reset@";

	/**
	 * The suffix for the name of a script to be executed by an agent (machine configuration).
	 * */
	String SCOPED_SCRIPT_AT_AGENT_SUFFIX = "agent.script.";

	/**
	 * The suffix for the name of a script to be executed by the DM (configure a machine).
	 * */
	String SCOPED_SCRIPT_AT_DM_CONFIGURE_SUFFIX = "dm.configure.script.";

	/**
	 * The suffix for the name of a script to be executed by the DM (unconfigure a machine).
	 * FIXME: although it was defined, it is not used at the moment. Too many problems with asynchronous unconfiguration.
	 * */
	String SCOPED_SCRIPT_AT_DM_UNCONFIGURE_SUFFIX = "dm.unconfigure.script.";

	/**
	 * A prefix for resources that should never be sent to an agent.
	 */
	String LOCAL_RESOURCE_PREFIX = "local.";

	/**
	 * The name of the target installer (which is somehow specific).
	 */
	String TARGET_INSTALLER = "target";

	/**
	 * The name of the file that contain the target properties.
	 */
	String TARGET_PROPERTIES_FILE_NAME = "target" + FILE_EXT_PROPERTIES;

	/**
	 * The {@value #TARGET_PROPERTY_HANDLER} property in {@value #TARGET_PROPERTIES_FILE_NAME} files.
	 */
	String TARGET_PROPERTY_HANDLER = "handler";

	/**
	 * The {@value #TARGET_PROPERTY_ID} property in {@value #TARGET_PROPERTIES_FILE_NAME} files.
	 */
	String TARGET_PROPERTY_ID = "id";

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
	String PROJECT_DIR_RULES_AUTONOMIC = "rules.autonomic";

	/**
	 * The <strong>probes</strong> directory.
	 */
	String PROJECT_DIR_PROBES = "probes";

	/**
	 * The <strong>commands</strong> directory.
	 */
	String PROJECT_DIR_COMMANDS = "commands";

	/**
	 * The <strong>scripts</strong> sub-directory.
	 */
	String PROJECT_SUB_DIR_SCRIPTS = "scripts";

	/**
	 * The <strong>application.properties</strong> file name.
	 */
	String PROJECT_FILE_DESCRIPTOR = "application" + FILE_EXT_PROPERTIES;

	/**
	 * The location of the agent's working directory (under the OS' temporary directory).
	 */
	String WORK_DIRECTORY_AGENT = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_agent" ).getAbsolutePath();


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

	/**
	 * The Maven directory to store application files.
	 */
	String MAVEN_SRC_MAIN_MODEL = "src/main/model/";

	/**
	 * The symbolic name of the "roboconf-core" bundle.
	 */
	String RBCF_CORE_SYMBOLIC_NAME = "net.roboconf.core";


	/**
	 * The property to define the messaging type in the DM and agent configuration files.
	 */
	String MESSAGING_TYPE = "messaging-type";

	/**
	 * The default polling period for probes.
	 */
	Long PROBES_POLLING_PERIOD = 20000L;

	/**
	 * The default domain name.
	 */
	String DEFAULT_DOMAIN = "default";


	/**
	 * The system property that points to Karaf's etc directory.
	 */
	String KARAF_ETC = "karaf.etc";

	/**
	 * The system property that points to Karaf's data directory.
	 */
	String KARAF_DATA = "karaf.data";

	/**
	 * The name of the main configuration file for an agent.
	 */
	String KARAF_CFG_FILE_AGENT = "net.roboconf.agent.configuration.cfg";
}
