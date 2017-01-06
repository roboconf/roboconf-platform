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

import java.util.Map;

/**
 * A message to send agent logs to the DM.
 * @author Vincent Zurczak - Linagora
 */
public class MsgNotifLogs extends AbstractMsgNotif {

	private static final long serialVersionUID = -8930645802175790064L;
	private final Map<String,byte[]> logFiles;


	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param scopedInstancePath the scoped instance's path
	 * @param logFiles a non-null (and not empty) map (key = file name, value = file content)
	 */
	public MsgNotifLogs( String applicationName, String scopedInstancePath, Map<String,byte[]> logFiles ) {
		super( applicationName, scopedInstancePath );
		this.logFiles = logFiles;
	}

	/**
	 * @return the logFiles
	 */
	public Map<String,byte[]> getLogFiles() {
		return this.logFiles;
	}
}
