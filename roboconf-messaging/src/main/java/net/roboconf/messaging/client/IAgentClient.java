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

package net.roboconf.messaging.client;

import java.io.IOException;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

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
	 */
	void setApplicationName( String applicationName );


	/**
	 * Sets the name of the root instance associated with the agent.
	 */
	void setRootInstanceName( String rootInstanceName );


	// Resolve exports

	/**
	 * Publishes the exports for a given instance.
	 * <p>
	 * This method indicates to other instances that they can use
	 * the variables exported by THIS instance.
	 * </p>
	 *
	 * @param instance the instance whose exports must be published
	 * @throws IOException if something went wrong
	 */
	void publishExports( Instance instance ) throws IOException;

	/**
	 * Un-publishes the exports for a given instance.
	 * <p>
	 * This method indicates to other instances that the variables exported
	 * by THIS instance cannot be used anymore.
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
	 *
	 * @param command {@link ListenerCommand#START} to stop listening, {@link ListenerCommand#STOP} to stop listening
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
	 *
	 * @param instance the instance that need exports from other agents
	 * @throws IOException if something went wrong
	 */
	void requestExportsFromOtherAgents( Instance instance ) throws IOException;

	/**
	 * Configures the listener for the exports from other agents.
	 * @param command {@link ListenerCommand#START} to stop listening, {@link ListenerCommand#STOP} to stop listening
	 * @param instance the instance that determine which exports must be listened to
	 * @throws IOException if something went wrong
	 */
	void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException;


	// Communication with the DM

	/**
	 * Sends a message to the DM.
	 * @param message the message to send
	 * @throws IOException if something went wrong
	 */
	void sendMessageToTheDm( Message message ) throws IOException;

	/**
	 * Configures the listener for messages from the DM.
	 * @param command {@link ListenerCommand#START} to stop listening, {@link ListenerCommand#STOP} to stop listening
	 * @throws IOException if something went wrong
	 */
	void listenToTheDm( ListenerCommand command ) throws IOException;
}
