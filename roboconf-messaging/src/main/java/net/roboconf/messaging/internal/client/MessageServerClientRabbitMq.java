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
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.internal.utils.SerializationUtils;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.utils.MessagingUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * @author Noël - LIG
 * @author Vincent Zurczak - Linagora
 */
public final class MessageServerClientRabbitMq implements IMessageServerClient {

	private static final String TOPIC = "topic";
	private final String loggerName = getClass().getName();

	Connection connection;
	Channel	channel;
	boolean connected = false;
	String queueName, consumerTag;

	private String messageServerIp, applicationName;
	private String sourceName = MessagingUtils.SOURCE_DM;



	@Override
	public void setMessageServerIp( String messageServerIp ) {
		this.messageServerIp = messageServerIp;
	}


	@Override
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	@Override
	public void setSourceName( String sourceName ) {
		this.sourceName = sourceName;
	}


	@Override
	public void openConnection( final IMessageProcessor messageProcessor ) throws IOException {

		// Already connected? Do nothing
		if( this.connected )
			return;

		// Initialize the connection
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost( this.messageServerIp );
		this.connection = factory.newConnection();
		this.channel = this.connection.createChannel();
		this.connected = true;

		// 1 agent or 1 dm <=> 1 queue
		String exchangeName = getExchangeName();

		// Exchange declaration is idem-potent
		this.channel.exchangeDeclare( exchangeName, TOPIC );

		// Queue declaration is idem-potent
		this.queueName = this.applicationName + "." + this.sourceName;
		this.channel.queueDeclare( this.queueName, true, false, true, null );

		// Start to listen to the queue
		final QueueingConsumer consumer = new QueueingConsumer( this.channel );
		this.consumerTag = this.channel.basicConsume( this.queueName, true, consumer );

		new Thread( "Roboconf - Queue listener for " + this.queueName ) {
			@Override
			public void run() {

				final Logger logger = Logger.getLogger( MessageServerClientRabbitMq.this.loggerName );
				logger.fine( getName() + " starts listening to new messages." );

				// We listen to messages until the consumer is cancelled
				for( ;; ) {

					try {
						QueueingConsumer.Delivery delivery = consumer.nextDelivery();
						Message message = SerializationUtils.deserializeObject( delivery.getBody());

						StringBuilder sb = new StringBuilder();
						sb.append( MessageServerClientRabbitMq.this.sourceName );
						sb.append( " received a message " );
						sb.append( message.getClass().getSimpleName());
						sb.append( " on routing key '" );
						sb.append( delivery.getEnvelope().getRoutingKey());
						sb.append( "'." );
						// FIXME: should be logged in finer
						logger.info( sb.toString());

						messageProcessor.processMessage( message );

					} catch( ShutdownSignalException e ) {
						logger.finest( MessageServerClientRabbitMq.this.sourceName + ": the message server is shutting down." );
						break;

					} catch( ConsumerCancelledException e ) {
						logger.fine( getName() + " stops listening to new messages." );
						break;

					} catch( InterruptedException e ) {
						logger.finest( Utils.writeException( e ));
						break;

					} catch( ClassNotFoundException e ) {
						logger.severe( MessageServerClientRabbitMq.this.sourceName + ": a message could not be deserialized. Class cast exception." );
						logger.finest( Utils.writeException( e ));

					}  catch( IOException e ) {
						logger.severe( MessageServerClientRabbitMq.this.sourceName + ": a message could not be deserialized. I/O exception." );
						logger.finest( Utils.writeException( e ));
					}
				}
			};

		}.start();
	}


	@Override
	public void closeConnection() throws IOException {

		if( this.channel != null
				&& this.channel.isOpen()) {
			this.channel.basicCancel( this.consumerTag );
			this.channel.queueDelete( this.queueName );
			this.channel.close();
		}

		if( this.connection != null
				&& this.connection.isOpen())
			this.connection.close();

		this.channel = null;
		this.connection = null;
		this.consumerTag = null;

		this.connected = false;
	}


	@Override
	public void bind( String routingKey ) throws IOException {

		// Bind routing key to the queue.
		// queueBind is idem-potent
		this.channel.queueBind( this.queueName, getExchangeName(), routingKey );
	}


	@Override
	public void unbind( String routingKey ) throws IOException {

		// Unbind the routing key and the queue.
		// queueUnbind is idem-potent
		this.channel.queueUnbind( this.queueName, getExchangeName(), routingKey );
	}


	@Override
	public void publish( boolean toDm, String routingKey, Message message )
	throws IOException {

		if( this.connected ) {
			this.channel.basicPublish(
					getExchangeName( toDm ), routingKey, null,
					SerializationUtils.serializeObject( message ));
		}
	}


	@Override
	public void cleanAllMessagingServerArtifacts() throws IOException {

		if( this.connected )
			throw new IOException( "This instance is already connected to the messaging server." );

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost( this.messageServerIp );
		Connection connection = factory.newConnection();
		Channel channel = this.connection.createChannel();

		channel.exchangeDelete( getExchangeName( true ));
		channel.exchangeDelete( getExchangeName( false ));

		channel.close();
		connection.close();
	}



	private String getExchangeName( boolean dm ) {
		return this.applicationName + "." + (dm ? "admin" : "agents");
	}


	private String getExchangeName() {
		return getExchangeName( MessagingUtils.SOURCE_DM.equalsIgnoreCase( this.sourceName ));
	}
}
