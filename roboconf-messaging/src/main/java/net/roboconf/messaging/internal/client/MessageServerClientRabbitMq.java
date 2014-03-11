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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
 */
public final class MessageServerClientRabbitMq implements IMessageServerClient {

	private static final String FANOUT = "fanout";
	private static final String TOPIC = "topic";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Set<String> alreadyDeclared = new HashSet<String> ();
	private final Map<String,Set<String>> queueNameToRoutingKeys = new HashMap<String,Set<String>> ();

	private Connection connection;
	private Channel	channel;
	private String messageServerIp, applicationName;
	private IMessageProcessor messageProcessor;




	@Override
	public void openConnection() {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost( this.messageServerIp );
			this.connection = factory.newConnection();
			this.channel = this.connection.createChannel();

		} catch( IOException e ) {
			this.logger.severe( "A communication could not be open for the application " + this.applicationName + "." );
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void closeConnection() {
		try {
			this.channel.close();
			this.connection.close();

		} catch( IOException e ) {
			this.logger.severe( "A communication could not be closed for the application " + this.applicationName + "." );
			this.logger.finest( Utils.writeException( e ));
		}
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
	public void setMessageProcessor( IMessageProcessor messageProcessor ) {
		this.messageProcessor = messageProcessor;
	}


	@Override
	public void subscribeTo( InteractionType interactionType, String filterName )
	throws IOException {

		// Initialize the exchange and the queue if necessary
		declareExchangeIfNecessary( interactionType );

		// This implementation does two things.
		// It binds routing keys to a queue. And it consumes a queue.
		// We use queueNameToRoutingKeys to track what actions this method has to do.

		String queueName = getQueueName( interactionType );
		Set<String> routingKeys = this.queueNameToRoutingKeys.get( queueName );

		// Did we already subscribe to this?
		if( routingKeys != null
				&& routingKeys.contains( filterName ))
			return;

		// Bind the routing key to the queue
		this.channel.queueBind( queueName, getExchangeName( interactionType ), filterName );
		boolean createConsumer = true;
		if( routingKeys == null )
			routingKeys = new HashSet<String> ();
		else
			createConsumer = false;

		routingKeys.add( filterName );
		this.queueNameToRoutingKeys.put( queueName, routingKeys );

		if( ! createConsumer )
			return;

		// Create the consumer and starts picking messages
		QueueingConsumer consumer = new QueueingConsumer( this.channel );
		this.channel.basicConsume( queueName, true, consumer );

		while( true ) {
			try {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				byte[] rawMessage = delivery.getBody();
				Message message = SerializationUtils.deserializeObject( rawMessage );
				this.messageProcessor.processMessage( message );

			} catch( ShutdownSignalException e ) {
				this.logger.severe( "The message server is shutting down." );
				break;

			} catch( ConsumerCancelledException e ) {
				this.logger.finest( Utils.writeException( e ));

			} catch( InterruptedException e ) {
				this.logger.finest( Utils.writeException( e ));

			} catch( ClassNotFoundException e ) {
				this.logger.severe( "A message could not be deserialized. Class cast exception. " + e.getMessage());
				this.logger.finest( Utils.writeException( e ));

			}  catch( IOException e ) {
				this.logger.severe( "A message could not be deserialized. I/O exception. " + e.getMessage());
				this.logger.finest( Utils.writeException( e ));
			}
		}
	}


	@Override
	public void unsubscribeTo( InteractionType interactionType, String filterName ) throws IOException {

		String queueName = getQueueName( interactionType );
		Set<String> routingKeys = this.queueNameToRoutingKeys.get( queueName );
		if( routingKeys != null
				&& routingKeys.contains( filterName ))
			this.channel.queueUnbind( queueName, getExchangeName( interactionType ), filterName );
	}


	@Override
	public void publish( InteractionType interactionType, String filterName, Message message )
	throws IOException {

		declareExchangeIfNecessary( interactionType );
		this.channel.basicPublish(
				getExchangeName( interactionType ),
				filterName,
				null,
				SerializationUtils.serializeObject( message ));
	}



	private void declareExchangeIfNecessary( InteractionType interactionType ) throws IOException {

		String name = getExchangeName( interactionType );
		String type = getExchangeType( interactionType );
		String key = name + type;
		if( this.alreadyDeclared.contains( key ))
			return;

		this.alreadyDeclared.add( key );
		this.channel.exchangeDeclare( name, type );
		this.channel.queueDeclare( getQueueName( interactionType ), true, false, true, null );
	}



	private String getExchangeName( InteractionType interactionType ) {

		String result;
		if( interactionType == InteractionType.AGENT_TO_AGENT )
			result = this.applicationName + ".agent";
		else
			result = this.applicationName + ".admin";

		return result;
	}



	private String getQueueName( InteractionType interactionType ) {

		String result;
		if( interactionType == InteractionType.AGENT_TO_AGENT )
			result = "agent";
		else
			result = "admin";

		return result;
	}



	private String getExchangeType( InteractionType interactionType ) {

		// The result is an exchange type.
		// The values mean something for RabbitMQ.
		String result;
		if( interactionType == InteractionType.AGENT_TO_AGENT )
			result = TOPIC;
		else
			result = FANOUT;

		return result;
	}
}
