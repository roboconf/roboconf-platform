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

package net.roboconf.messaging.internal.client;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.InteractionType;
import net.roboconf.messaging.internal.utils.SerializationUtils;
import net.roboconf.messaging.messages.Message;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * @author Noël - LIG
 * @author Vincent Zurczak - Linagora
 * FIXME: we should delete the exchanges too
 */
public final class MessageServerClientRabbitMq implements IMessageServerClient {

	private static final String TOPIC = "topic";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Map<String,String> queueNameToConsumerTag = new ConcurrentHashMap<String,String> ();

	private Connection connection;
	private Channel	channel;
	private boolean connected;
	private String messageServerIp, applicationName;




	@Override
	public void openConnection() throws IOException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost( this.messageServerIp );
		this.connection = factory.newConnection();
		this.channel = this.connection.createChannel();
		this.connected = true;
	}


	public boolean isConnected() {
		return this.connected;
	}


	@Override
	public void closeConnection() throws IOException {

		if( this.channel != null
				&& this.channel.isOpen())
			this.channel.close();

		if( this.connection != null
				&& this.connection.isOpen())
			this.connection.close();

		this.channel = null;
		this.connection = null;
		this.connected = false;
	}


	@Override
	public void setMessageServerIp( String messageServerIp ) {
		this.messageServerIp = messageServerIp;
	}


	@Override
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	@Override
	public void subscribeTo(
			final String sourceName,
			InteractionType interactionType,
			String routingKey,
			final IMessageProcessor messageProcessor )
	throws IOException {

		// Initialize the exchange, the queue and the routing key
		declareRabbitArtifacts( interactionType, routingKey );
		final String queueName = getQueueName( interactionType, routingKey );

		// From now, start a thread that will pick and process messages
		final QueueingConsumer consumer = new QueueingConsumer( this.channel );
		String consumerTag = this.channel.basicConsume( queueName, true, consumer );
		this.queueNameToConsumerTag.put( queueName, consumerTag );

		new Thread( "Roboconf - " + sourceName + " @ " + queueName ) {
			@Override
			public void run() {

				// We listen to messages until the consumer is cancelled
				for( ;; ) {

					try {
						QueueingConsumer.Delivery delivery = consumer.nextDelivery();
						Message message = SerializationUtils.deserializeObject( delivery.getBody());

						StringBuilder sb = new StringBuilder();
						sb.append( sourceName );
						sb.append( " received a message " );
						sb.append( message.getClass().getSimpleName());
						sb.append( " on routing key " );
						sb.append( delivery.getEnvelope().getRoutingKey());
						sb.append( "." );
						// FIXME: should be logged in finer
						MessageServerClientRabbitMq.this.logger.info( sb.toString());

						messageProcessor.processMessage( message );

					} catch( ShutdownSignalException e ) {
						MessageServerClientRabbitMq.this.logger.finest( sourceName + ": the message server is shutting down." );
						break;

					} catch( ConsumerCancelledException e ) {
						MessageServerClientRabbitMq.this.logger.finest( Utils.writeException( e ));

					} catch( InterruptedException e ) {
						MessageServerClientRabbitMq.this.logger.finest( Utils.writeException( e ));

					} catch( ClassNotFoundException e ) {
						MessageServerClientRabbitMq.this.logger.severe( sourceName + ": a message could not be deserialized. Class cast exception." );
						MessageServerClientRabbitMq.this.logger.finest( Utils.writeException( e ));

					}  catch( IOException e ) {
						MessageServerClientRabbitMq.this.logger.severe( sourceName + ": a message could not be deserialized. I/O exception." );
						MessageServerClientRabbitMq.this.logger.finest( Utils.writeException( e ));
					}
				}
			};

		}.start();
	}


	@Override
	public void unsubscribeTo( InteractionType interactionType, String routingKey ) throws IOException {

		String queueName = getQueueName( interactionType, routingKey );
		String consumerTag = this.queueNameToConsumerTag.get( queueName );
		if( consumerTag != null ) {
			this.channel.basicCancel( consumerTag );
			this.queueNameToConsumerTag.remove( queueName );
		}
	}


	@Override
	public void publish( InteractionType interactionType, String routingKey, Message message )
	throws IOException {

		// Check this - useful for asynchronous invocation (as in a timer)
		if( this.channel == null )
			return;

		declareRabbitArtifacts( interactionType, routingKey );
		this.channel.basicPublish(
				getExchangeName( interactionType ),
				routingKey,
				null,
				SerializationUtils.serializeObject( message ));
	}


	@Override
	public void deleteQueueOrTopic( InteractionType interactionType, String routingKey ) throws IOException {

		unsubscribeTo( interactionType, routingKey );
		String queueName = getQueueName( interactionType, routingKey );
		this.channel.queueDelete( queueName );
	}



	private void declareRabbitArtifacts( InteractionType interactionType, String routingKey ) throws IOException {

		// For us, 1 exchange <=> 1 Queue
		String exchangeName = getExchangeName( interactionType );

		// Exchange declaration is idem-potent
		this.channel.exchangeDeclare( exchangeName, TOPIC );

		// Queue declaration is idem-potent
		String queueName = getQueueName( interactionType, routingKey );
		this.channel.queueDeclare( queueName, true, false, true, null );

		// Bind routing key to the queue.
		// queueBind is idem-potent
		this.channel.queueBind( queueName, exchangeName, routingKey );
	}



	private String getQueueName( InteractionType interactionType, String routingKey ) {
		return this.applicationName + "." + routingKey;
	}



	private String getExchangeName( InteractionType interactionType ) {

		StringBuilder sb = new StringBuilder();
		sb.append( this.applicationName );
		sb.append( "." );
		sb.append( interactionType == InteractionType.AGENT_TO_AGENT ? "agents" : "admin" );

		return sb.toString();
	}



	Set<String> getQueueNames() {
		return this.queueNameToConsumerTag.keySet();
	}



	Connection getConnection() {
		return this.connection;
	}



	Channel getChannel() {
		return this.channel;
	}
}
