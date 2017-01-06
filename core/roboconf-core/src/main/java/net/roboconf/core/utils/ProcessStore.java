/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Global process store.
 * @author Pierre-Yves Gibello - Linagora
 */
public class ProcessStore {

	private static final Map<String, Process> PROCESS_MAP = new HashMap<> ();


	/**
	 * Stores a process (eg. a running script), so that the
	 * process can be reached later (eg. to cancel it when blocked).
	 * @param process The process to be stored
	 */
	public static synchronized void setProcess(String applicationName, String scopedInstancePath, Process process) {
		PROCESS_MAP.put(toAgentId(applicationName, scopedInstancePath), process);
	}


	/**
	 * Retrieves a stored process, when found.
	 * @return The process
	 */
	public static synchronized Process getProcess(String applicationName, String scopedInstancePath) {
		return PROCESS_MAP.get(toAgentId(applicationName, scopedInstancePath));
	}


	/**
	 * Removes a stored process, if found.
	 */
	public static synchronized Process clearProcess(String applicationName, String scopedInstancePath) {
		return PROCESS_MAP.remove(toAgentId(applicationName, scopedInstancePath));
	}


	private static String toAgentId(String applicationName, String scopedInstancePath) {
		return applicationName + "|" + scopedInstancePath;
	}
}
