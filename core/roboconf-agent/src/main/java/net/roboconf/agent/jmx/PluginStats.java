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

import net.roboconf.agent.internal.PluginProxy;

/**
 * MBean to monitor plugin invocations.
 * @author Pierre-Yves Gibello - Linagora
 */
public class PluginStats implements PluginStatsMBean {

	@Override
	public void reset() {
		PluginProxy.resetAllCounters();
	}

	@Override
	public int getInitializeCount() {
		return PluginProxy.getInitializeCount();
	}

	@Override
	public int getDeployCount() {
		return PluginProxy.getDeployCount();
	}

	@Override
	public int getUndeployCount() {
		return PluginProxy.getUndeployCount();
	}

	@Override
	public int getStartCount() {
		return PluginProxy.getStartCount();
	}

	@Override
	public int getStopCount() {
		return PluginProxy.getStopCount();
	}

	@Override
	public int getUpdateCount() {
		return PluginProxy.getUpdateCount();
	}

	@Override
	public int getErrorCount() {
		return PluginProxy.getErrorCount();
	}
}
