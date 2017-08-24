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

package net.roboconf.messaging.api.extensions;

import java.io.IOException;
import java.util.Map;

import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IMessagingClient {

	/**
	 * Sets the message queue where the client can store the messages to process.
	 * @param messageQueue the message queue
	 */
	void setMessageQueue( RoboconfMessageQueue messageQueue );

	/**
	 * @return true if the client is connected, false otherwise
	 */
	boolean isConnected();

	/**
	 * Sets information about the Roboconf part that uses this client.
	 * <p>
	 * This information can be changed dynamically.
	 * Said differently, this information is not final, it can change with time.
	 * </p>
	 *
	 * @param ownerKind {@link RecipientKind#DM} or {@link RecipientKind#AGENTS}
	 * @param domain the domain
	 * @param applicationName the application name (only makes sense for agents)
	 * @param scopedInstancePath the scoped instance path  (only makes sense for agents)
	 */
	void setOwnerProperties( RecipientKind ownerKind, String domain, String applicationName, String scopedInstancePath );

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
	 * @return the type of messaging currently used by this client.
	 */
	String getMessagingType();

	/**
	 * Gets the provider-specific messaging configuration of this client.
	 * <p>
	 * Messaging configuration is needed in order to configure a Roboconf VM, for instance when it is replicated.
	 * This map <strong>must contain</strong> the {@link MessagingConstants#MESSAGING_TYPE_PROPERTY} key
	 * that tells which kind of messaging client is used.
	 * </p>
	 *
	 * @return the provider-specific messaging configuration of this client. The returned map is unmodifiable.
	 */
	// TODO: /!\ they may be differences between DM & agents messaging configurations (i.e HTTP server/client certificate, passwords, ...).
	// Exposing everything in the same configuration may raise serious security issues.
	Map<String, String> getConfiguration();

	/**
	 * Subscribes to a given context.
	 * @param ctx a messaging context (not null)
	 * @throws IOException if something went wrong
	 */
	void subscribe( MessagingContext ctx ) throws IOException;

	/**
	 * Unsubscribes to a given context.
	 * @param ctx a messaging context (not null)
	 * @throws IOException if something went wrong
	 */
	void unsubscribe( MessagingContext ctx ) throws IOException;

	/**
	 * Publishes a message.
	 * @param ctx a messaging context (not null)
	 * @param msg the message to publish (not null)
	 * @throws IOException if something went wrong
	 */
	void publish( MessagingContext ctx, Message msg ) throws IOException;

	/**
	 * Clear artifacts on the messaging server.
	 * <p>
	 * This method is invoked when an application was deleted.
	 * It allows to remove topics or whatever that were related to the deleted application.
	 * </p>
	 *
	 * @param application a non-null application
	 * @throws IOException if something went wrong
	 */
	void deleteMessagingServerArtifacts( Application application ) throws IOException;
}
