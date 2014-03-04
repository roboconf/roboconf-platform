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

package net.roboconf.messaging.messages.from_agent_to_agent;

import java.util.Map;

import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 * FIXME: instance paths...
 */
public class MsgCmdImportAdd extends Message {

	private static final long serialVersionUID = -374008319791927432L;

	private final String importedTypeName;
	private final String instanceExportingVarsName;
	private final Map<String,String> exportedVarsAndValues;


	/**
	 * Constructor.
	 * @param name
	 * @param instanceExportingVarsName
	 * @param exportedVarsAndValues
	 */
	public MsgCmdImportAdd( String name, String instanceExportingVarsName, Map<String, String> exportedVarsAndValues ) {
		super();
		this.importedTypeName = name;
		this.instanceExportingVarsName = instanceExportingVarsName;
		this.exportedVarsAndValues = exportedVarsAndValues;
	}

	/**
	 * @return the importedTypeName
	 */
	public String getImportedTypeName() {
		return this.importedTypeName;
	}

	/**
	 * @return the instanceExportingVarsName
	 */
	public String getInstanceExportingVarsName() {
		return this.instanceExportingVarsName;
	}

	/**
	 * @return the exportedVarsAndValues
	 */
	public Map<String, String> getExportedVarsAndValues() {
		return this.exportedVarsAndValues;
	}
}
