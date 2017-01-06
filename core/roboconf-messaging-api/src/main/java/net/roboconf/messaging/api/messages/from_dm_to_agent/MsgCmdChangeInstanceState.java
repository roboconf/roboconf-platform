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

package net.roboconf.messaging.api.messages.from_dm_to_agent;

import java.util.Map;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Noël - LIG
 */
public class MsgCmdChangeInstanceState extends Message {

	private static final long serialVersionUID = 411037586577734609L;
	private final String instancePath;
	private final InstanceStatus newState;
	private final Map<String,byte[]> fileNameToFileContent;


	/**
	 * Constructor.
	 * @param instancePath
	 * @param newState
	 * @param fileNameToFileContent
	 */
	public MsgCmdChangeInstanceState( String instancePath, InstanceStatus newState, Map<String, byte[]> fileNameToFileContent ) {
		super();
		this.instancePath = instancePath;
		this.newState = newState;
		this.fileNameToFileContent = fileNameToFileContent;
	}

	/**
	 * Constructor.
	 * @param instance
	 * @param newState
	 * @param fileNameToFileContent
	 */
	public MsgCmdChangeInstanceState( Instance instance, InstanceStatus newState, Map<String, byte[]> fileNameToFileContent ) {
		this( InstanceHelpers.computeInstancePath( instance ), newState, fileNameToFileContent );
	}

	/**
	 * Constructor.
	 * @param instance
	 * @param newState
	 */
	public MsgCmdChangeInstanceState( Instance instance, InstanceStatus newState ) {
		this( InstanceHelpers.computeInstancePath( instance ), newState, null );
	}

	/**
	 * Constructor.
	 * @param instancePath
	 * @param newState
	 */
	public MsgCmdChangeInstanceState( String instancePath, InstanceStatus newState ) {
		this( instancePath, newState, null );
	}

	/**
	 * @return the instance path
	 */
	public String getInstancePath() {
		return this.instancePath;
	}

	/**
	 * @return a map associating file names with their content (for deployment)
	 */
	public Map<String,byte[]> getFileNameToFileContent() {
		return this.fileNameToFileContent;
	}

	/**
	 * @return the newState
	 */
	public InstanceStatus getNewState() {
		return this.newState;
	}
}
