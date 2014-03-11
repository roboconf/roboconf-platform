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
	 * Sets the message processor.
	 * <p>
	 * It will be used by the implementing class when a message is received.
	 * </p>
	 * @param messageProcessor a message processor
	 */
	void setMessageProcessor( IMessageProcessor messageProcessor );

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
	 * @param interactionType the interaction type (the queue or topic name should be deduced from it)
	 * @param filterName the filter name, if the message server supports it
	 * <p>
	 * This feature allows to filter messages on a queue or a topic. It is supported
	 * on RabbitMQueue (it is then called routingKey).
	 * </p>
	 */
	void subscribeTo( InteractionType interactionType, String filterName ) throws IOException;

	/**
	 * Unsubscribes from a queue or topic.
	 * @param interactionType the interaction type (the queue or topic name should be deduced from it)
	 * @param filterName the filter name, if the message server supports it
	 */
	void unsubscribeTo( InteractionType interactionType, String filterName ) throws IOException;

	/**
	 * Publishes a message a message on a queue or a topic.
	 * @param interactionType the interaction type (the queue or topic name should be deduced from it)
	 * @param filterName the filter name, if the message server supports it
	 * @param message the message to publish
	 */
	void publish( InteractionType interactionType, String filterName, Message message ) throws IOException;
}
