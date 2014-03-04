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

package net.roboconf.messaging.processing;

import java.util.logging.Logger;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.utils.SerializationUtils;

import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * A thread to run a message processor.
 * @author Noël - LIG
 */
public class MessageProcessorThread extends Thread {

	private boolean running = true;

	private final QueueingConsumer consumer;
	private final IMessageProcessor messageProcessor;
	private final Logger logger = Logger.getLogger( MessagingConstants.ROBOCONF_LOGGER_NAME );


	/**
	 * Constructor.
	 * @param consumer
	 * @param messageProcessor
	 */
	public MessageProcessorThread( QueueingConsumer consumer, IMessageProcessor messageProcessor ) {
		super( "Roboconf's Message Processor Thread" );
		this.consumer = consumer;
		this.messageProcessor = messageProcessor;
	}

	/**
	 * Stops the thread.
	 */
	public void halt() {
		this.running = false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		while( this.running ) {
			try {
				this.logger.finer( "Message processor thread: Waiting for messages." );
				QueueingConsumer.Delivery delivery = this.consumer.nextDelivery();

				this.logger.finer( "Message processor thread: Got a new message." );
				byte[] rawMessage = delivery.getBody();
				Message message = SerializationUtils.deserializeObject( rawMessage );
				this.messageProcessor.processMessage( message );

			} catch( ShutdownSignalException e ) {
				this.logger.severe( "The message server is shutting down." );
				halt();

			} catch( ConsumerCancelledException e ) {
				this.logger.finest( Utils.writeException( e ));

			} catch( InterruptedException e ) {
				this.logger.finest( Utils.writeException( e ));
			}
		}
	}
}
