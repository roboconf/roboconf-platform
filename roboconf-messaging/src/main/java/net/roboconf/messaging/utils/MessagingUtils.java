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

package net.roboconf.messaging.utils;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class MessagingUtils {

	public static final long HEARTBEAT_PERIOD = 60000;
	public static final String SOURCE_DM = "dm";


	/**
	 * @param instance an instance deployed on the same machine than the target agent
	 * @return the routing key to use to send messages to this agent
	 */
	public static String buildRoutingKeyToAgent( Instance instance ) {
		Instance rootInstance = InstanceHelpers.findRootInstance( instance );
		return buildRoutingKeyToAgent( rootInstance.getName());
	}


	/**
	 * @param rootInstanceName the name of the root instance name associated with the agent
	 * @return the routing key to use to send messages to this agent
	 */
	public static String buildRoutingKeyToAgent( String rootInstanceName ) {
		return "machine." + rootInstanceName;
	}


	/**
	 * @return the routing key to use to send messages to the DM
	 */
	public static String buildRoutingKeyToDm() {
		return "dm";
	}
}
