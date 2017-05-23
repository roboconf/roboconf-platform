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

package net.roboconf.agent.internal;

import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * Proxy for plugin lifecycle and monitoring.
 * @author Pierre-Yves Gibello - Linagora
 *
 */
public class PluginProxy implements PluginInterface {

	static int initializeCount = 0;
	static int deployCount = 0;
	static int undeployCount = 0;
	static int startCount = 0;
	static int stopCount = 0;
	static int updateCount = 0;

	PluginInterface plugin;

	/**
	 * Constructor for plugin proxy.
	 * @param plugin The proxied plugin
	 */
	public PluginProxy(PluginInterface plugin) {
		this.plugin = plugin;
	}

	/**
	 * Resets all counters.
	 */
	public static synchronized void resetAllCounters() {
		initializeCount = 0;
		deployCount = 0;
		undeployCount = 0;
		startCount = 0;
		stopCount = 0;
		updateCount = 0;
	}

	@Override
	public void initialize(Instance instance) throws PluginException {
		this.plugin.initialize(instance);
		PluginProxy.incrementInitializeCount();
	}

	@Override
	public void deploy(Instance instance) throws PluginException {
		this.plugin.deploy(instance);
		PluginProxy.incrementDeployCount();
	}

	@Override
	public void start(Instance instance) throws PluginException {
		this.plugin.start(instance);
		PluginProxy.incrementStartCount();
	}

	@Override
	public void update(Instance instance, Import importChanged,
			InstanceStatus statusChanged) throws PluginException {
		this.plugin.update(instance, importChanged, statusChanged);
		PluginProxy.incrementUpdateCount();
	}

	@Override
	public void stop(Instance instance) throws PluginException {
		this.plugin.stop(instance);
		PluginProxy.incrementStopCount();
	}

	@Override
	public void undeploy(Instance instance) throws PluginException {
		this.plugin.undeploy(instance);
		PluginProxy.incrementUndeployCount();
	}

	@Override
	public void setNames(String applicationName, String rootInstanceName) {
		this.plugin.setNames(applicationName, rootInstanceName);
	}

	@Override
	public String getPluginName() {
		return this.plugin.getPluginName();
	}

	/**
	 * Retrieves the number of "initialize" invocations.
	 * @return The requested number of invocations
	 */
	public static synchronized int getInitializeCount() {
		return initializeCount;
	}

	private static synchronized void incrementInitializeCount() {
		PluginProxy.initializeCount ++;
	}

	/**
	 * Retrieves the number of "deploy" invocations.
	 * @return The requested number of invocations
	 */
	public static synchronized int getDeployCount() {
		return deployCount;
	}

	private static synchronized void incrementDeployCount() {
		PluginProxy.deployCount ++;
	}

	/**
	 * Retrieves the number of "undeploy" invocations.
	 * @return The requested number of invocations
	 */
	public static synchronized int getUndeployCount() {
		return undeployCount;
	}

	private static synchronized void incrementUndeployCount() {
		PluginProxy.undeployCount ++;
	}

	/**
	 * Retrieves the number of "start" invocations.
	 * @return The requested number of invocations
	 */
	public static synchronized int getStartCount() {
		return startCount;
	}

	private static synchronized void incrementStartCount() {
		PluginProxy.startCount ++;
	}

	/**
	 * Retrieves the number of "stop" invocations.
	 * @return The requested number of invocations
	 */
	public static synchronized int getStopCount() {
		return stopCount;
	}

	private static synchronized void incrementStopCount() {
		PluginProxy.stopCount ++;
	}

	/**
	 * Retrieves the number of "update" invocations.
	 * @return The requested number of invocations
	 */
	public static synchronized int getUpdateCount() {
		return updateCount;
	}

	private static synchronized void incrementUpdateCount() {
		PluginProxy.updateCount ++;
	}

}
