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

import java.util.Map;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 * FIXME: is there a better way to transmit model files instead of a byte array?
 */
public class MsgCmdInstanceDeploy extends Message {

	private static final long serialVersionUID = 411037586577734609L;
	private final String instancePath;
	private final Map<String,byte[]> fileNameToFileContent;

	/**
	 * Constructor.
	 * @param instancePath
	 * @param fileNameToFileContent
	 */
	public MsgCmdInstanceDeploy( String instancePath, Map<String, byte[]> fileNameToFileContent ) {
		super();
		this.instancePath = instancePath;
		this.fileNameToFileContent = fileNameToFileContent;
	}

	/**
	 * Constructor.
	 * @param instance
	 * @param fileNameToFileContent
	 */
	public MsgCmdInstanceDeploy( Instance instance, Map<String, byte[]> fileNameToFileContent ) {
		this( InstanceHelpers.computeInstancePath( instance ), fileNameToFileContent );
	}

	/**
	 * @return the instance path
	 */
	public String getInstancePath() {
		return this.instancePath;
	}

	/**
	 * @return a map associating file names with their content
	 */
	public Map<String,byte[]> getFileNameToFileContent() {
		return this.fileNameToFileContent;
	}
}
