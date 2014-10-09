/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.agent.internal.lifecycle;

import java.io.IOException;
import java.util.Map;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
class DeployedStopped extends AbstractLifeCycleManager {

	/**
	 * Constructor.
	 * @param appName
	 * @param messagingClient
	 */
	public DeployedStopped( String appName, IAgentClient messagingClient ) {
		super( appName, messagingClient );
	}


	@Override
	public void changeInstanceState(
			Instance instance,
			PluginInterface plugin,
			InstanceStatus newStatus,
			Map<String,byte[]> fileNameToFileContent )
	throws IOException, PluginException {

		if( newStatus == InstanceStatus.DEPLOYED_STARTED )
			start( instance, plugin );
		else if( newStatus == InstanceStatus.NOT_DEPLOYED )
			undeploy( instance, plugin );
	}
}