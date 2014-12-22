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

import net.roboconf.messaging.messages.Message;

/**
 * Monitoring event (e.g. crossed threshold).
 * <p>
 * May be used for elasticity
 * (like adding a new VM when heavy load is detected), or any monitoring purpose.
 * </p>
 *
 * @author Pierre-Yves Gibello - Linagora
 */
public class MsgNotifAutonomic extends Message {

	private static final long serialVersionUID = -8930645802175790064L;

	private final String eventId;
	private final String eventInfo;
	private final String rootInstanceName;
	private final String applicationName;


	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param rootInstanceName the root instance's name
	 * @param eventId the event ID
	 * @param eventInfo info about the event (eg. result of Nagios Livestatus query)
	 */
	public MsgNotifAutonomic( String applicationName, String rootInstanceName, String eventId, String eventInfo ) {
		super();
		this.rootInstanceName = rootInstanceName;
		this.applicationName = applicationName;
		this.eventInfo = eventInfo;
		this.eventId = eventId;
	}

	/**
	 * @return the event name
	 */
	public String getEventId() {
		return this.eventId;
	}

	/**
	 * @return the event info (eg. result of Nagios Livestatus query)
	 */
	public String getEventInfo() {
		return this.eventInfo;
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
}
