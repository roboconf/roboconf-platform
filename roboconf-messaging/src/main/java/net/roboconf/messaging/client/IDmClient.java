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

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * A client for the DM.
 * <p>
 * The DM has one client for ALL the applications.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IDmClient extends IClient {

	/**
	 * Sends a message to an agent.
	 * @param application the application associated with the given agent
	 * @param instance an instance managed on the agent (used to find the root instance name)
	 * @param message the message to send
	 * @throws IOException if something went wrong
	 */
	void sendMessageToAgent( Application application, Instance instance, Message message ) throws IOException;

	/**
	 * Configures the listener for messages sent by agents.
	 * @param application the application associated with the given agents
	 * @param command {@link ListenerCommand#START} to stop listening, {@link ListenerCommand#STOP} to stop listening
	 * @throws IOException if something went wrong
	 */
	void listenToAgentMessages( Application application, ListenerCommand command ) throws IOException;

	/**
	 * Deletes all the server artifacts related to this application.
	 * <p>
	 * This method must be called when ALL the agents have closed
	 * their connection and BEFORE the DM's connection is closed.
	 * </p>
	 *
	 * @param application the application whose messaging artifacts must be deleted
	 * @throws IOException if something went wrong
	 */
	void deleteMessagingServerArtifacts( Application application ) throws IOException;

	/**
	 * @return true if the client is connected, false otherwise
	 */
	boolean isConnected();
}
