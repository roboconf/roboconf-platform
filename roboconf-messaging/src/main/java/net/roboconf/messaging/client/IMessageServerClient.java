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

package net.roboconf.messaging.client;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 */
public interface IMessageServerClient {

	/**
	 * @return the message server IP address
	 */
	String getMessageServerIp();

	/**
	 * Opens a connection with the message server.
	 */
	void openConnection();

	/**
	 * Closes the connection with the message server.
	 */
	void closeConnection();

	/**
	 * Declares a channel on the message server where instances will interact with each other.
	 * <p>
	 * Note that there is one channel per machine (root instance).
	 * </p>
	 *
	 * @param rootInstanceName the root instance name
	 * <p>
	 * A root instance name designates a machine (VM, device, etc).
	 * </p>
	 *
	 * @see
	 */
	void declareChannel( String rootInstanceName );

	/**
	 * Send a message to a machine designated by its name.
	 * @param message the message to send
	 * @param rootInstance the root instance (associated with a machine) the message must be sent to
	 */
	void sendMessage( Message message, Instance rootInstance );
}
