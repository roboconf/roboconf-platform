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

import net.roboconf.core.model.runtime.Instance;

/**
 * @author Noël - LIG
 */
public interface PluginInterface {

	/**
	 * Deploys an instance.
	 * @param instance the instance to deploy
	 */
	void deploy( Instance instance ) throws Exception;


	/**
	 * Starts an instance.
	 * @param instance the instance to start
	 */
	void start( Instance instance ) throws Exception;


	/**
	 * Updates an instance.
	 * @param instance the instance to update
	 */
	void update( Instance instance ) throws Exception;

	/**
	 * Stops an instance.
	 * @param instance the instance to stop
	 */
	void stop( Instance instance ) throws Exception;


	/**
	 * Undeploys an instance.
	 * @param instance the instance to undeploy
	 */
	void undeploy( Instance instance ) throws Exception;
}
