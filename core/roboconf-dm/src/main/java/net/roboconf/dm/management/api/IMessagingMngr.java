/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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
import net.roboconf.messaging.api.business.IDmClient;
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
	void sendMessageToTheDm( Message message ) throws IOException;


	/**
	 * Sends a message to an agent in safe mode.
	 * <p>
	 * This method is the safest in terms of reliability.
	 * Indeed, if the instance's machine is not marked as available, the message is cached
	 * until the machine is online. Same thing is the messaging is not available at the moment.
	 * </p>
	 *
	 * @param ma the managed application
	 * @param message the message to send (or store)
	 * @param instance the target instance
	 * @throws IOException if an error occurred with the messaging
	 */
	void sendMessageSafely( ManagedApplication ma, Instance instance, Message message ) throws IOException;


	/**
	 * Sends a message directly to an agent.
	 * <p>
	 * Unlike {@link #sendMessageSafely(ManagedApplication, Instance, Message)},
	 * this method does not perform any check when sending a message. It directly
	 * uses the Roboconf messaging API to send a message.
	 * </p>
	 *
	 * @param ma the managed application
	 * @param message the message to send (or store)
	 * @param instance the target instance
	 * @throws IOException if an error occurred with the messaging
	 */
	void sendMessageDirectly( ManagedApplication ma, Instance scopedInstance, Message message ) throws IOException;


	/**
	 * A convenience method that sends/flushes messages that were stored for a given application/instance.
	 * <p>
	 * This method checks a messaging client is available. It also verifies the target scoped
	 * instance is deployed and started. Eventually, if a message fails to be sent, it is reinserted
	 * into the storage queue, which means a further invocation will try to send this message again.
	 * This guarantees no message is lost due to messaging issues on the DM side.
	 * </p>
	 * <p>
	 * This method is based upon the Roboconf messaging implementations, which means it relies on
	 * them to determiner if a message was correctly sent. Besides, the messaging API only verifies
	 * a message left correctly, not that the agent received it.
	 * </p>
	 *
	 * @param ma the managed application
	 * @param instance the target instance
	 */
	void sendStoredMessages( ManagedApplication ma, Instance instance );


	/**
	 * @return the messaging client (can be null or not connected)
	 */
	IDmClient getMessagingClient();


	/**
	 * Checks the messaging configuration.
	 * @throws IOException if the configuration is invalid
	 */
	void checkMessagingConfiguration() throws IOException;
}
