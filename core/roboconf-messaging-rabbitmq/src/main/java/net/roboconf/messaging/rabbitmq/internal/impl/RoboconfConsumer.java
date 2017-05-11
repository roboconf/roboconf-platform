/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.rabbitmq.internal.impl;

import java.io.IOException;
import java.util.logging.Logger;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.utils.SerializationUtils;

/**
 * Notice: QueueingConsumer is deprecated, hence this implementation that allows recovery.
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfConsumer extends DefaultConsumer implements Consumer {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final RoboconfMessageQueue messageQueue;
	private final String sourceName;


	/**
	 * Constructor.
	 * @param channel
	 * @param sourceName
	 * @param messageQueue
	 */
	public RoboconfConsumer( String sourceName, Channel channel, RoboconfMessageQueue messageQueue ) {
		super( channel );
		this.messageQueue = messageQueue;
		this.sourceName = sourceName;
	}


	@Override
	public void handleDelivery( String consumerTag, Envelope envelope, BasicProperties properties, byte[] body )
	throws IOException {

		try {
			Message message = SerializationUtils.deserializeObject( body );
			this.logger.finer( this.sourceName + " received a message " + message.getClass().getSimpleName()
					+ " on routing key '" + envelope.getRoutingKey() + "'.");

			this.messageQueue.add( message );

		} catch( ClassNotFoundException | IOException e ) {
			this.logger.severe( this.sourceName + ": a message could not be deserialized. => " + e.getClass().getSimpleName());
			Utils.logException( this.logger, e );
			this.messageQueue.errorWhileReceivingMessage();
		}
	}


	@Override
	public void handleShutdownSignal( String consumerTag, ShutdownSignalException sig ) {

		if( sig.isInitiatedByApplication()) {
			this.logger.fine( this.sourceName + ": the connection to the messaging server was shut down." + id( consumerTag ));

		} else if( sig.getReference() instanceof Channel ) {
			int nb = ((Channel) sig.getReference()).getChannelNumber();
			this.logger.fine( "A RabbitMQ consumer was shut down. Channel #" + nb + ", " + id( consumerTag ));

		} else {
			this.logger.fine( "A RabbitMQ consumer was shut down." + id( consumerTag ));
		}
	}


	@Override
	public void handleCancelOk( String consumerTag ) {
		this.logger.fine( "A RabbitMQ consumer stops listening to new messages." + id( consumerTag ));
	}


	@Override
	public void handleCancel( String consumerTag ) throws IOException {
		this.logger.fine( "A RabbitMQ consumer UNEXPECTABLY stops listening to new messages." + id( consumerTag ));
	}


	/**
	 * @param consumerTag a consumer tag
	 * @return a readable ID of this consumer
	 */
	private String id( String consumerTag ) {

		StringBuilder sb = new StringBuilder();
		sb.append( " Consumer tag = " );
		sb.append( consumerTag );
		sb.append( " (@ " );
		sb.append( this.sourceName );
		sb.append( ")" );

		return sb.toString();
	}
}
