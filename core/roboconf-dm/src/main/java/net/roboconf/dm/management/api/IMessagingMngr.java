/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.management.api;

import java.io.IOException;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IMessagingMngr {

	/**
	 * Sends a message to the DM.
	 * @param message the message to send
	 * @throws IOException if an error occurred with the messaging
	 */
	void sendMessage( Message message ) throws IOException;


	/**
	 * Sends a message to an agent, or stores it if the machine is not yet online.
	 * @param ma the managed application
	 * @param message the message to send (or store)
	 * @param instance the targetHandlers instance
	 * @throws IOException if an error occurred with the messaging
	 */
	void sendMessage( ManagedApplication ma, Instance instance, Message message ) throws IOException;


	/**
	 * @return the messaging client (cannot be null but may be not connected)
	 */
	IDmClient getMessagingClient();


	/**
	 * Checks the messaging configuration.
	 * @throws IOException if the configuration is invalid
	 */
	void checkMessagingConfiguration() throws IOException;
}
