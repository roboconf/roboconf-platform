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

import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgCmdImportRemove extends Message {

	private static final long serialVersionUID = -2597875984409385732L;

	private final String componentOrFacetName;
	private final String removedInstancePath;


	/**
	 * Constructor.
	 * @param componentOrFacetName
	 * @param removedInstancePath
	 */
	public MsgCmdImportRemove( String componentOrFacetName, String removedInstancePath ) {
		super();
		this.componentOrFacetName = componentOrFacetName;
		this.removedInstancePath = removedInstancePath;
	}

	/**
	 * @return the component or facet name
	 */
	public String getComponentOrFacetName() {
		return this.componentOrFacetName;
	}

	/**
	 * @return the removedInstancePath
	 */
	public String getRemovedInstancePath() {
		return this.removedInstancePath;
	}
}
