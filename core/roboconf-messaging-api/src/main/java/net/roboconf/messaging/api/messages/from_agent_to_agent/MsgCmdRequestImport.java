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

import net.roboconf.messaging.api.messages.Message;

/**
 * A message to indicate we need an import.
 * @author Noël - LIG
 */
public class MsgCmdRequestImport extends Message {

	private static final long serialVersionUID = 5366599037551758208L;
	private final String componentOrFacetName;
	private final String applicationOrContextName;


	/**
	 * Constructor.
	 * @param componentOrFacetName
	 * @param applicationOrContextName
	 */
	public MsgCmdRequestImport( String applicationOrContextName, String componentOrFacetName ) {
		super();
		this.componentOrFacetName = componentOrFacetName;
		this.applicationOrContextName = applicationOrContextName;
	}

	/**
	 * @return the component or facet name
	 */
	public String getComponentOrFacetName() {
		return this.componentOrFacetName;
	}

	/**
	 * @return the applicationOrContextName
	 */
	public String getApplicationOrContextName() {
		return this.applicationOrContextName;
	}
}
