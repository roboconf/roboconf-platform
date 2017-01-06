/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.messages.from_agent_to_dm;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractMsgNotif extends Message {

	private static final long serialVersionUID = 8098680203487017313L;
	private String applicationName, scopedInstancePath;


	/**
	 * Constructor.
	 * @param applicationName
	 * @param scopedInstancePath
	 */
	public AbstractMsgNotif( String applicationName, String scopedInstancePath ) {
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
	}

	/**
	 * Constructor.
	 * @param applicationName
	 * @param scopedInstance
	 */
	public AbstractMsgNotif( String applicationName, Instance scopedInstance ) {
		this.applicationName = applicationName;
		this.scopedInstancePath = InstanceHelpers.computeInstancePath( scopedInstance );
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
	}

	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}

	/**
	 * @return the scopedInstancePath
	 */
	public String getScopedInstancePath() {
		return this.scopedInstancePath;
	}

	/**
	 * @param scopedInstancePath the scopedInstancePath to set
	 */
	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
	}
}
