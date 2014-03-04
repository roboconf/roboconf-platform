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

import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgNotifMachineDown extends Message {

	private static final long serialVersionUID = 2204000792853175646L;
	private final String rootInstanceName;


	/**
	 * Constructor.
	 * @param rootInstanceName the root instance (machine) name
	 */
	public MsgNotifMachineDown( String rootInstanceName ) {
		super();
		this.rootInstanceName = rootInstanceName;
	}

	/**
	 * @return the rootInstanceName
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}
}
