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

package net.roboconf.messaging.api.messages.from_agent_to_agent;

import java.util.Map;

import net.roboconf.messaging.api.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgCmdAddImport extends Message {

	private static final long serialVersionUID = -374008319791927432L;

	private final String componentOrFacetName;
	private final String applicationOrContextName;
	private final String addedInstancePath;
	private final Map<String,String> exportedVariables;


	/**
	 * Constructor.
	 * @param applicationOrContextName
	 * @param componentOrFacetName
	 * @param addedInstancePath
	 * @param exportedVariables
	 */
	public MsgCmdAddImport( String applicationOrContextName, String componentOrFacetName, String addedInstancePath, Map<String,String> exportedVariables ) {
		super();
		this.componentOrFacetName = componentOrFacetName;
		this.addedInstancePath = addedInstancePath;
		this.exportedVariables = exportedVariables;
		this.applicationOrContextName = applicationOrContextName;
	}

	/**
	 * @return the componentOrFacetName
	 */
	public String getComponentOrFacetName() {
		return this.componentOrFacetName;
	}

	/**
	 * @return the addedInstancePath
	 */
	public String getAddedInstancePath() {
		return this.addedInstancePath;
	}

	/**
	 * @return the exportedVariables
	 */
	public Map<String, String> getExportedVariables() {
		return this.exportedVariables;
	}

	/**
	 * @return the applicationOrContextName
	 */
	public String getApplicationOrContextName() {
		return this.applicationOrContextName;
	}
}
