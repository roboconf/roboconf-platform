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

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MsgNotifMachineReadyToBeDeleted extends Message {

	private static final long serialVersionUID = -9029162898048800254L;
	private final String rootInstanceName;
	private final String applicationName;


	/**
	 * Constructor.
	 * @param applicationName the application name
	 * @param rootInstanceName the root instance (machine) name
	 */
	public MsgNotifMachineReadyToBeDeleted( String applicationName, String rootInstanceName ) {
		super();
		this.rootInstanceName = rootInstanceName;
		this.applicationName = applicationName;
	}

	/**
	 * Constructor.
	 * @param rootInstance the root instance
	 */
	public MsgNotifMachineReadyToBeDeleted( String applicationName, Instance rootInstance ) {
		this( applicationName, InstanceHelpers.findRootInstance( rootInstance ).getName());
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
