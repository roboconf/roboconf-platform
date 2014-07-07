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

import java.io.File;

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
	 * Sets the execution level.
	 * <p>
	 * This method allows to define until which steps a plug-in must go.
	 * It allows, as an example, to only log an action.
	 * Or to only generate files (if it generates ones).
	 * </p>
	 * <p>
	 * This is particularly useful to debug or to test plug-ins and
	 * Roboconf deployments.
	 * </p>
	 *
	 * @param executionLevel the execution level
	 */
	void setExecutionLevel( ExecutionLevel executionLevel );


	/**
	 * Sets the directory to use when the execution level is {@link ExecutionLevel#GENERATE_FILES}.
	 * @param dumpDirectory the dump directory (not null)
	 */
	void setDumpDirectory( File dumpDirectory );


	/**
	 * Sets the agent name (useful for debug and analyzing logs).
	 * @param agentName the agent name
	 */
	void setAgentName( String agentName );


	/**
	 * @return the plug-in name
	 */
	String getPluginName();
}
