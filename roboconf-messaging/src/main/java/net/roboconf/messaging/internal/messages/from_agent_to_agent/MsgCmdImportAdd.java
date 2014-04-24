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

package net.roboconf.messaging.internal.messages.from_agent_to_agent;

import java.util.Map;

import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgCmdImportAdd extends Message {

	private static final long serialVersionUID = -374008319791927432L;

	private final String componentOrFacetName;
	private final String addedInstancePath;
	private final Map<String,String> exportedVariables;


	/**
	 * Constructor.
	 * @param componentOrFacetName
	 * @param addedInstancePath
	 * @param exportedVariables
	 */
	public MsgCmdImportAdd( String componentOrFacetName, String addedInstancePath, Map<String,String> exportedVariables ) {
		super();
		this.componentOrFacetName = componentOrFacetName;
		this.addedInstancePath = addedInstancePath;
		this.exportedVariables = exportedVariables;
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
}
