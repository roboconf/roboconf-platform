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

import java.util.concurrent.atomic.AtomicInteger;

import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * Proxy for plugin lifecycle and monitoring.
 * @author Pierre-Yves Gibello - Linagora
 */
public class PluginProxy implements PluginInterface {

	static AtomicInteger initializeCount = new AtomicInteger(0);
	static AtomicInteger deployCount = new AtomicInteger(0);
	static AtomicInteger undeployCount = new AtomicInteger(0);
	static AtomicInteger startCount = new AtomicInteger(0);
	static AtomicInteger stopCount = new AtomicInteger(0);
	static AtomicInteger updateCount = new AtomicInteger(0);
	static AtomicInteger errorCount = new AtomicInteger(0);

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
		initializeCount.set(0);
		deployCount.set(0);
		undeployCount.set(0);
		startCount.set(0);
		stopCount.set(0);
		updateCount.set(0);
		errorCount.set( 0 );
	}

	@Override
	public void initialize(Instance instance) throws PluginException {
		try {
			this.plugin.initialize(instance);

		} catch(PluginException e) {
			PluginProxy.incrementErrorCount();
			throw e;
		}
		initializeCount.incrementAndGet();
	}

	@Override
	public void deploy(Instance instance) throws PluginException {
		try {
			this.plugin.deploy(instance);

		} catch(PluginException e) {
			PluginProxy.incrementErrorCount();
			throw e;
		}
		deployCount.incrementAndGet();
	}

	@Override
	public void start(Instance instance) throws PluginException {
		try {
			this.plugin.start(instance);

		} catch(PluginException e) {
			PluginProxy.incrementErrorCount();
			throw e;
		}
		startCount.incrementAndGet();
	}

	@Override
	public void update(Instance instance, Import importChanged, InstanceStatus statusChanged)
	throws PluginException {

		try {
			this.plugin.update(instance, importChanged, statusChanged);

		} catch(PluginException e) {
			PluginProxy.incrementErrorCount();
			throw e;
		}
		updateCount.incrementAndGet();
	}

	@Override
	public void stop(Instance instance) throws PluginException {
		try {
			this.plugin.stop(instance);
		} catch(PluginException e) {
			PluginProxy.incrementErrorCount();
			throw e;
		}
		stopCount.incrementAndGet();
	}

	@Override
	public void undeploy(Instance instance) throws PluginException {
		try {
			this.plugin.undeploy(instance);

		} catch(PluginException e) {
			PluginProxy.incrementErrorCount();
			throw e;
		}
		undeployCount.incrementAndGet();
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
	public static int getInitializeCount() {
		return initializeCount.intValue();
	}

	/**
	 * Retrieves the number of "deploy" invocations.
	 * @return The requested number of invocations
	 */
	public static int getDeployCount() {
		return deployCount.intValue();
	}

	/**
	 * Retrieves the number of "undeploy" invocations.
	 * @return The requested number of invocations
	 */
	public static int getUndeployCount() {
		return undeployCount.intValue();
	}

	/**
	 * Retrieves the number of "start" invocations.
	 * @return The requested number of invocations
	 */
	public static int getStartCount() {
		return startCount.intValue();
	}

	/**
	 * Retrieves the number of "stop" invocations.
	 * @return The requested number of invocations
	 */
	public static int getStopCount() {
		return stopCount.intValue();
	}

	/**
	 * Retrieves the number of "update" invocations.
	 * @return The requested number of invocations
	 */
	public static int getUpdateCount() {
		return updateCount.intValue();
	}

	/**
	 * Retrieves the number of invocation errors.
	 * @return The number of invocation errors
	 */
	public static int getErrorCount() {
		return errorCount.intValue();
	}

	/**
	 * @return the proxyfied plugin
	 */
	public PluginInterface getPlugin() {
		return this.plugin;
	}

	private static void incrementErrorCount() {
		errorCount.incrementAndGet();
	}
}
