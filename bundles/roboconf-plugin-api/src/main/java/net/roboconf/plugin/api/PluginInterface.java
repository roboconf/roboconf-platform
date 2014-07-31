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

package net.roboconf.plugin.api;

import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

/**
 * @author Noël - LIG
 */
public interface PluginInterface {

	/**
	 * Initializes the plug-in for this instance.
	 * <p>
	 * As an example, in Puppet, this method install all the
	 * modules that will be used by the manifest(s).
	 * </p>
	 *
	 * @param instance
	 */
	void initialize( Instance instance ) throws PluginException;


	/**
	 * Deploys an instance.
	 * @param instance the instance to deploy
	 */
	void deploy( Instance instance ) throws PluginException;


	/**
	 * Starts an instance.
	 * @param instance the instance to start
	 */
	void start( Instance instance ) throws PluginException;


	/**
	 * Updates an instance.
	 * @param instance the instance to update
	 * @param importChanged the changed import, if any (null if no change)
	 * @param statusChanged the new status of the instance that triggered the update, when applicable (null if N/A)
	 */
	void update(Instance instance, Import importChanged, InstanceStatus statusChanged) throws PluginException;


	/**
	 * Stops an instance.
	 * @param instance the instance to stop
	 */
	void stop( Instance instance ) throws PluginException;


	/**
	 * Undeploys an instance.
	 * @param instance the instance to undeploy
	 */
	void undeploy( Instance instance ) throws PluginException;


	/**
	 * Sets the names (useful for debug and analyzing logs).
	 * @param applicationName the application name
	 * @param the root instance name the root instance name
	 */
	void setNames( String applicationName, String rootInstanceName );


	/**
	 * @return the plug-in name
	 */
	String getPluginName();
}
