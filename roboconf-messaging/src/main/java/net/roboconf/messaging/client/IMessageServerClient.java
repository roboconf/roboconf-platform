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

/**
 * An interface to abstract the message server.
 * <p>
 * It is associated with a single application.
 * </p>
 * <p>
 * Implementations are responsible of the connection with the
 * message server. They are also in charge of listening and processing
 * messages (subscriptions).
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
	 * Opens a connection with the message server.
	 */
	void openConnection() throws IOException;

	/**
	 * Closes the connection with the message server.
	 */
	void closeConnection() throws IOException;

	/**
	 * Subscribes to a queue or topic.
	 * @param sourceName the source name (e.g., DM or agent something, useful for debug)
	 * @param interactionType the interaction type (the queue or topic name should be deduced from it)
	 * @param routingKey the routing key, if the message server supports it
	 * <p>
	 * This feature allows to route a message to a given queue. It is supported
	 * by RabbitMQueue.
	 * </p>
	 * @param messageProcessor the message processor for the subscription
	 */
	void subscribeTo( String sourceName, InteractionType interactionType, String routingKey, IMessageProcessor messageProcessor )
	throws IOException;

	/**
	 * Unsubscribes from a queue or topic.
	 * @param interactionType the interaction type (the queue or topic name should be deduced from it)
	 * @param routingKey the filter name, if the message server supports it
	 */
	void unsubscribeTo( InteractionType interactionType, String routingKey ) throws IOException;

	/**
	 * Publishes a message a message on a queue or a topic.
	 * @param interactionType the interaction type (the queue or topic name should be deduced from it)
	 * @param routingKey the filter name, if the message server supports it
	 * @param message the message to publish
	 */
	void publish( InteractionType interactionType, String routingKey, Message message ) throws IOException;

	/**
	 * Deletes the queue or topic identified by these parameters.
	 * @param interactionType the interaction type (the queue or topic name should be deduced from it)
	 * @param routingKey the filter name, if the message server supports it
	 */
	void deleteQueueOrTopic( InteractionType interactionType, String routingKey ) throws IOException;
}
