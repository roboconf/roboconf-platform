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

import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IClient {

	/**
	 * Sets the message queue where the client can store the messages to process.
	 * @param messageQueue the message queue
	 */
	void setMessageQueue( RoboconfMessageQueue messageQueue );

	/**
	 * Sets the domain.
	 * @param domain
	 */
	void setDomain( String domain );

	/**
	 * @return true if the client is connected, false otherwise
	 */
	boolean isConnected();

	/**
	 * Opens a connection with the message server.
	 */
	void openConnection() throws IOException;

	/**
	 * Closes the connection with the message server.
	 * <p>
	 * There is no need to check {@link #isConnected()} before invoking this method.
	 * </p>
	 */
	void closeConnection() throws IOException;

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

	/**
	 * @return the type of messaging currently used by this client.
	 */
	String getMessagingType();

	/**
	 * @return the domain
	 */
	String getDomain();

	/**
	 * Gets the provider-specific messaging configuration of this client.
	 * <p>
	 * Messaging configuration is needed in order to configure a Roboconf VM, for instance when it is replicated.
	 * </p>
	 * @return the provider-specific messaging configuration of this client. The returned map is unmodifiable.
	 */
	// TODO: /!\ they may be differences between DM & agents messaging configurations (i.e HTTP server/client certificate, passwords, ...).
	// Exposing everything in the same configuration may raise serious security issues.
	Map<String, String> getConfiguration();

}
