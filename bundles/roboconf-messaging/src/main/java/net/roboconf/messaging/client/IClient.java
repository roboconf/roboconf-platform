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

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IClient {

	/**
	 * Start or stop listening to events.
	 * @author Vincent Zurczak - Linagora
	 */
	public enum ListenerCommand {
		START, STOP
	};


	/**
	 * Sets the connection parameters.
	 * @param messageServerIp the IP address of the messaging server
	 * @param messageServerUsername the user name to connect to the server
	 * @param messageServerPassword the password to connect to the server
	 */
	void setParameters( String messageServerIp, String messageServerUsername, String messageServerPassword );

	/**
	 * @return true if the client is connected, false otherwise
	 */
	boolean isConnected();

	/**
	 * Opens a connection with the message server.
	 * <p>
	 * The message processor will be used for any subsequent subscription.
	 * </p>
	 */
	void openConnection( AbstractMessageProcessor messageProcessor ) throws IOException;

	/**
	 * Closes the connection with the message server.
	 */
	void closeConnection() throws IOException;
}
