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

package net.roboconf.messaging.api.business;

import java.io.IOException;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;

/**
 * A client for the agents.
 * <p>
 * Each agent must have its own and unique client.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IAgentClient extends IClient {

	/**
	 * Sets the application name.
	 * @param applicationName
	 */
	void setApplicationName( String applicationName );


	/**
	 * Sets the path of the (scoped) instance associated with the agent.
	 * @param scopedInstancePath
	 */
	void setScopedInstancePath( String scopedInstancePath );


	/**
	 * Sets the external mapping parameters.
	 * <p>
	 * It is used to deal with inter-application dependencies.
	 * The messaging implementation will use the map parameter to intercept
	 * and find which variables must be exported to other applications.
	 * </p>
	 *
	 * @param externalExports a non-null map
	 * <p>
	 * Key = internal (graph) variable.<br />
	 * Value = name of the variable, seen from outside.
	 * </p>
	 * <p>
	 * Example: <code>exports: Toto.ip as test</code> and that the application's prefix
	 * is <b>APP</b> will result in an entry whose key is <code>Toto.ip</code>
	 * and whose value is<code>APP.test</code>.
	 * </p>
	 */
	void setExternalMapping( Map<String,String> externalExports );


	// Resolve exports

	/**
	 * Publishes the exports for a given instance.
	 * <p>
	 * This method indicates to other instances that they can use
	 * the variables exported by THIS instance.
	 * </p>
	 * <p>
	 * This method must also handles external exports, i.e. variables that are
	 * made visible by other Roboconf applications. The overall idea is that if
	 * a variable must be visible outside the current scope, then its alias will be
	 * given by the map parameter in {@link #setExternalMapping(Map)}.
	 * </p>
	 *
	 * @param instance the instance whose exports must be published
	 * @throws IOException if something went wrong
	 */
	void publishExports( Instance instance ) throws IOException;

	/**
	 * Publishes specific exports for a given instance.
	 * <p>
	 * This method must also handles external exports, i.e. variables that are
	 * made visible by other Roboconf applications. The overall idea is that if
	 * a variable must be visible outside the current scope, then its alias will be
	 * given by the map parameter in {@link #setExternalMapping(Map)}.
	 * </p>
	 *
	 * @param instance the instance whose exports must be published
	 * @param facetOrComponentName the prefix of the variables to publish
	 * @throws IOException if something went wrong
	 */
	void publishExports( Instance instance, String facetOrComponentName ) throws IOException;

	/**
	 * Un-publishes the exports for a given instance.
	 * <p>
	 * This method indicates to other instances that the variables exported
	 * by THIS instance cannot be used anymore.
	 * </p>
	 * <p>
	 * This method must also handles external exports, i.e. variables that are
	 * made visible by other Roboconf applications. The overall idea is that if
	 * a variable must be visible outside the current scope, then its alias will be
	 * given by the map parameter in {@link #setExternalMapping(Map)}.
	 * </p>
	 *
	 * @param instance the instance whose exports must be published
	 * @throws IOException if something went wrong
	 */
	void unpublishExports( Instance instance ) throws IOException;

	/**
	 * Configures the listener for requests from other agents.
	 * <p>
	 * Such requests aim at asking an agent to publish its exports.
	 * The agent will do so only if it exports a variable prefixed that
	 * is required by THIS instance.
	 * </p>
	 * <p>
	 * This method must also handles external exports, i.e. variables that are
	 * made visible by other Roboconf applications.
	 * </p>
	 *
	 * @param command {@link ListenerCommand#START} to start listening, {@link ListenerCommand#STOP} to stop listening
	 * @param instance the instance that need exports from other agents
	 * @throws IOException if something went wrong
	 */
	void listenToRequestsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException;


	// Resolve imports


	/**
	 * Requests other agents to export their variables on the messaging server.
	 * <p>
	 * This should be called when a new instance is registered on the agent. It guarantees
	 * that any new instance can be notified about the instances located on other agents.
	 * </p>
	 * <p>
	 * This method must also handles external exports, i.e. variables that are
	 * made visible by other Roboconf applications.
	 * </p>
	 *
	 * @param instance the instance that need exports from other agents
	 * @throws IOException if something went wrong
	 */
	void requestExportsFromOtherAgents( Instance instance ) throws IOException;

	/**
	 * Configures the listener for the exports from other agents.
	 * <p>
	 * This method must also handles external exports, i.e. variables that are
	 * made visible by other Roboconf applications.
	 * </p>
	 *
	 * @param command {@link ListenerCommand#START} to start listening, {@link ListenerCommand#STOP} to stop listening
	 * @param instance the instance that determine which exports must be listened to
	 * @throws IOException if something went wrong
	 */
	void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException;
}
