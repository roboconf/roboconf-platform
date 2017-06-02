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

package net.roboconf.agent.jmx;

/**
 * Interface for plugin management MBean.
 * @author Pierre-Yves Gibello - Linagora
 */
public interface PluginStatsMBean {

	/**
	 * Resets all counters.
	 */
	void reset();

	/**
	 * Retrieves the number of "initialize" invocations.
	 * @return The requested number of invocations
	 */
	int getInitializeCount();

	/**
	 * Retrieves the number of "deploy" invocations.
	 * @return The requested number of invocations
	 */
	int getDeployCount();

	/**
	 * Retrieves the number of "undeploy" invocations.
	 * @return The requested number of invocations
	 */
	int getUndeployCount();

	/**
	 * Retrieves the number of "start" invocations.
	 * @return The requested number of invocations
	 */
	int getStartCount();

	/**
	 * Retrieves the number of "stop" invocations.
	 * @return The requested number of invocations
	 */
	int getStopCount();

	/**
	 * Retrieves the number of "update" invocations.
	 * @return The requested number of invocations
	 */
	int getUpdateCount();

	/**
	 * Retrieves the number of invocation errors.
	 * @return The number of invocation errors
	 */
	int getErrorCount();
}
