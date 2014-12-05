/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.messages.from_agent_to_dm;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgNotifHeartbeat extends Message {

	private static final long serialVersionUID = -5112964737944073719L;

	private final String rootInstanceName, applicationName, ipAddress;
	private boolean modelRequired = false;


	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param rootInstanceName the root instance (machine) name
	 * @param ipAddress the IP address
	 */
	public MsgNotifHeartbeat( String applicationName, String rootInstanceName, String ipAddress ) {
		super();
		this.rootInstanceName = rootInstanceName;
		this.ipAddress = ipAddress;
		this.applicationName = applicationName;
	}

	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param rootInstance the root instance
	 * @param ipAddress the IP address
	 */
	public MsgNotifHeartbeat( String applicationName, Instance rootInstance, String ipAddress ) {
		this( applicationName, rootInstance.getName(), ipAddress );
	}

	/**
	 * @return the rootInstanceName
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
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
