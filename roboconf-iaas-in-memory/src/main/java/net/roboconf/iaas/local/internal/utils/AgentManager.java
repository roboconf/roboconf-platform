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

package net.roboconf.iaas.local.internal.utils;

import java.util.HashMap;
import java.util.Map;

import net.roboconf.agent.AgentLauncher;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentManager {

	public static final AgentManager INSTANCE = new AgentManager();
	private final Map<String,AgentLauncher> machineIdToAgentLauncher = new HashMap<String,AgentLauncher> ();


	/**
	 * Constructor.
	 */
	private AgentManager() {
		// nothing
	}


	/**
	 * Registers a machine ID and the associated agent launcher.
	 * @param machineId the machine ID
	 * @param agentLauncher
	 */
	public void registerMachine( String machineId, AgentLauncher agentLauncher ) {
		this.machineIdToAgentLauncher.put( machineId, agentLauncher );
	}


	/**
	 * Un-registers a machine ID and the associated agent launcher.
	 * @param machineId the machine ID
	 * @return the associated agent's launcher
	 */
	public AgentLauncher unregisterMachine( String machineId ) {
		return this.machineIdToAgentLauncher.remove( machineId );
	}
}
