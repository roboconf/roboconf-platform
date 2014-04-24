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

package net.roboconf.messaging.messages.from_agent_to_dm;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgNotifMachineUp extends Message {

	private static final long serialVersionUID = -9029162898048800254L;

	private final String ipAddress;
	private final String rootInstanceName;


	/**
	 * Constructor.
	 * @param rootInstanceName the root instance (machine) name
	 * @param ipAddress the IP address
	 */
	public MsgNotifMachineUp( String rootInstanceName, String ipAddress ) {
		super();
		this.rootInstanceName = rootInstanceName;
		this.ipAddress = ipAddress;
	}

	/**
	 * Constructor.
	 * @param rootInstance the root instance
	 * @param ipAddress the IP address
	 */
	public MsgNotifMachineUp( Instance rootInstance, String ipAddress ) {
		this( rootInstance.getName(), ipAddress );
	}

	/**
	 * @return the rootInstanceName
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}

	/**
	 * @return the IP address
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}
}
