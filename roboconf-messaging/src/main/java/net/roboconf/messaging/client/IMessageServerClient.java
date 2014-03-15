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

import java.io.IOException;

import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.utils.MessagingUtils;

/**
 * An interface to abstract the message server.
 * <p>
 * It is associated with a single application.
 * </p>
 * <p>
 * Implementations are responsible of the connection with the
 * message server. They are also in charge of listening and processing
 * messages.
 * </p>
 *
 * @author Noël - LIG
 * @author Vincent Zurczak - Linagora
 */
public interface IMessageServerClient {

	/**
	 * Sets the location of the message server.
	 */
	void setMessageServerIp( String messageServerIp );

	/**
	 * Sets the application name.
	 */
	void setApplicationName( String applicationName );

	/**
	 * Sets the source name.
	 * <p>
	 * For an agent, the root instance name is expected.<br />
	 * For the DM, use {@link MessagingUtils#SOURCE_DM}.
	 * </p>
	 */
	void setSourceName( String sourceName );

	/**
	 * Opens a connection with the message server.
	 * <p>
	 * This method also starts to listen for messages
	 * targeting the source (see {@link #setSourceName(String)}).
	 * </p>
	 */
	void openConnection( IMessageProcessor messageProcessor ) throws IOException;

	/**
	 * Closes the connection with the message server.
	 * <p>
	 * This method also deletes server artifacts associated with
	 * the source (see {@link #setSourceName(String)}).
	 * </p>
	 */
	void closeConnection() throws IOException;

	/**
	 * Cleans all the server artifacts related to this application.
	 * <p>
	 * This method must be called when ALL the agents and the DM
	 * have stopped sending and listening messages on the server.
	 * </p>
	 * <p>
	 * In theory, it should only be called by the DM when an application
	 * is deleted.
	 * </p>
	 */
	void cleanAllMessagingServerArtifacts() throws IOException;

	/**
	 * Publishes a message on the server.
	 * @param toDm true to indicate this message targets the DM, false if it is sent to an agent
	 * @param routingKey the routing key so that the server knows who must receive the message
	 * @param message the message to publish
	 */
	void publish( boolean toDm, String routingKey, Message message ) throws IOException;

	/**
	 * Binds a routing key to this source.
	 * <p>
	 * As an example, on RabbitMQ, it is used to define entry points to
	 * a queue.
	 * </p>
	 * @param routingKey the routing key
	 */
	void bind( String routingKey ) throws IOException;

	/**
	 * Un-binds a routing key from this source.
	 * <p>
	 * As an example, on RabbitMQ, it is used to delete an entry point to
	 * a queue.
	 * </p>
	 * @param routingKey the routing key
	 */
	void unbind( String routingKey ) throws IOException;
}
