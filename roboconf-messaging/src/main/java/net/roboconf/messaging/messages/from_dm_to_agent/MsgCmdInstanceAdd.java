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

package net.roboconf.messaging.messages.from_dm_to_agent;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgCmdInstanceAdd extends Message {

	private static final long serialVersionUID = 411037586577734609L;
	private final Instance instanceToAdd;
	private final String parentInstancePath;

	/**
	 * Constructor.
	 * @param instanceToAdd
	 * @param parentInstancePath
	 */
	public MsgCmdInstanceAdd( String parentInstancePath, Instance instanceToAdd ) {
		super();
		this.instanceToAdd = instanceToAdd;
		this.parentInstancePath = parentInstancePath;
	}

	/**
	 * Constructor.
	 * @param parentInstance
	 * @param instanceToAdd
	 */
	public MsgCmdInstanceAdd( Instance parentInstance, Instance instanceToAdd ) {
		this( InstanceHelpers.computeInstancePath( parentInstance ), instanceToAdd );
	}

	/**
	 * @return the instance to add
	 */
	public Instance getInstanceToAdd() {
		return this.instanceToAdd;
	}

	/**
	 * @return the parentInstancePath
	 */
	public String getParentInstancePath() {
		return this.parentInstancePath;
	}
}
