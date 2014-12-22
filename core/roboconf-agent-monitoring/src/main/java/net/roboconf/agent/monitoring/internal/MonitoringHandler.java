/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.monitoring.internal;

import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * Handler invoked to deal with a monitoring solution.
 * @author Pierre-Yves Gibello - Linagora
 */
public abstract class MonitoringHandler {

	protected String eventId;
	protected String applicationName;
	protected String vmInstanceName;
	protected String ipAddress;

	/**
	 * Create a new monitoring handler.
	 * @param eventName The event ID
	 * @param applicationName The application name
	 * @param vmInstanceName The instance name
	 * @param ipAddress 
	 */
	public MonitoringHandler( String eventName, String applicationName, String vmInstanceName, String ipAddress ) {
		this.eventId = eventName;
		this.applicationName = applicationName;
		this.vmInstanceName = vmInstanceName;
		this.ipAddress = ipAddress;
	}

	/**
	 * Get the event ID.
	 * @return The event ID
	 */
	public String getEventId() {
		return this.eventId;
	}

	/**
	 * Process and fire event if needed.
	 * @return A notification to be sent to the manager, or null if nothing to send.
	 */
	public abstract MsgNotifAutonomic process();
}
