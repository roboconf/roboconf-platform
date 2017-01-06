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

package net.roboconf.messaging.api.messages.from_agent_to_dm;

import net.roboconf.core.model.beans.Instance;

/**
 * @author Noël - LIG
 */
public class MsgNotifHeartbeat extends AbstractMsgNotif {

	private static final long serialVersionUID = -5112964737944073719L;

	private final String ipAddress;
	private boolean modelRequired = false;


	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param scopedInstancePath the scoped instance's path
	 * @param ipAddress the IP address
	 */
	public MsgNotifHeartbeat( String applicationName, String scopedInstancePath, String ipAddress ) {
		super( applicationName, scopedInstancePath );
		this.ipAddress = ipAddress;
	}

	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param scopedInstance the scoped instance (associated with the agent)
	 * @param ipAddress the IP address
	 */
	public MsgNotifHeartbeat( String applicationName, Instance scopedInstance, String ipAddress ) {
		super( applicationName, scopedInstance );
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the IP address
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}

	/**
	 * @return the modelRequired
	 */
	public boolean isModelRequired() {
		return this.modelRequired;
	}

	/**
	 * @param modelRequired the modelRequired to set
	 */
	public void setModelRequired( boolean modelRequired ) {
		this.modelRequired = modelRequired;
	}
}
