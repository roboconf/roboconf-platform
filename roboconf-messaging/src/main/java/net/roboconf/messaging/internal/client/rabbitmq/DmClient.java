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

package net.roboconf.messaging.internal.client.rabbitmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.internal.utils.RabbitMqUtils;
import net.roboconf.messaging.internal.utils.SerializationUtils;
import net.roboconf.messaging.messages.Message;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * The RabbitMQ client for the DM.
 * @author Vincent Zurczak - Linagora
 */
public class DmClient implements IDmClient {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String messageServerIp;

	final Map<String,String> applicationNameToConsumerTag = new HashMap<String,String> ();
	Channel channel;
	AbstractMessageProcessor messageProcessor;



	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient
	 * #setMessageServerIp(java.lang.String)
	 */
	@Override
	public void setMessageServerIp( String messageServerIp ) {
		this.messageServerIp = messageServerIp;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient
	 * #isConnected()
	 */
	@Override
	public boolean isConnected() {
		return this.channel != null;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient
	 * #openConnection(net.roboconf.messaging.client.AbstractMessageProcessor)
	 */
	@Override
	public void openConnection( AbstractMessageProcessor messageProcessor )
	throws IOException {

		// Already connected? Do nothing
		this.logger.fine( "The DM is opening a connection to RabbitMQ." );
		if( isConnected()) {
			this.logger.info( "The DM has already a connection to RabbitMQ." );
			return;
		}

		// Initialize the connection
		ConnectionFactory factory = new ConnectionFactory();
		RabbitMqUtils.configureFactory( factory, this.messageServerIp );
		this.channel = factory.newConnection().createChannel();

		// Store the message processor for later
		this.messageProcessor = messageProcessor;
		this.messageProcessor.start();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient
	 * #closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {
		this.logger.fine( "The DM is closing its connection to RabbitMQ." );

		if( this.messageProcessor != null
				&& this.messageProcessor.isRunning())
			this.messageProcessor.interrupt();

		RabbitMqUtils.closeConnection( this.channel );
		this.channel = null;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IDmClient
	 * #publishMessageToAgent(net.roboconf.core.model.runtime.Application, net.roboconf.core.model.runtime.Instance, net.roboconf.messaging.messages.Message)
	 */
	@Override
	public void sendMessageToAgent( Application application, Instance instance, Message message )
	throws IOException {

		String exchangeName = RabbitMqUtils.buildExchangeName( application, false );
		String routingKey = RabbitMqUtils.buildRoutingKeyForAgent( instance );

		this.logger.fine( "The DM sends a message to " + routingKey + ". Message type: " + message.getClass().getSimpleName());
		this.channel.basicPublish(
				exchangeName, routingKey, null,
				SerializationUtils.serializeObject( message ));

		this.logger.fine( "The DM sent a message to " + routingKey + ". Message type: " + message.getClass().getSimpleName());
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IDmClient
	 * #listenToAgentMessages(net.roboconf.core.model.runtime.Application, net.roboconf.messaging.client.IClient.ListenerCommand)
	 */
	@Override
	public void listenToAgentMessages( Application application, ListenerCommand command )
	throws IOException {

		if( command == ListenerCommand.STOP ) {
			this.logger.fine( "The DM stops listening agents messages for the '" + application.getName() + "' application." );
			String consumerTag = this.applicationNameToConsumerTag.remove( application.getName());
			if( consumerTag != null
					&& this.channel != null
					&& this.channel.isOpen())
				this.channel.basicCancel( consumerTag );

		} else {
			// Already listening? Ignore...
			if( this.applicationNameToConsumerTag.containsKey( application.getName()))
				return;

			this.logger.fine( "The DM starts listening agents messages for the '" + application.getName() + "' application." );

			// Exchange declaration is idem-potent
			RabbitMqUtils.declareApplicationExchanges( application.getName(), this.channel );

			// Queue declaration is idem-potent
			String queueName = application.getName() + ".dm";
			this.channel.queueDeclare( queueName, true, false, true, null );

			// queueBind is idem-potent
			// Every message sent to the "DM" exchange will land into the DM's queue.
			String exchangeName = RabbitMqUtils.buildExchangeName( application, true );
			this.channel.queueBind( queueName, exchangeName, "" );

			// Start to listen to the queue
			final QueueingConsumer consumer = new QueueingConsumer( this.channel );
			String consumerTag = this.channel.basicConsume( queueName, true, consumer );
			this.applicationNameToConsumerTag.put( application.getName(), consumerTag );

			// In the DM, it is performed in a separate thread.
			// So, basically, the DM has a listening thread for every application.
			// Each thread listens for new messages and stores them in the message processor.

			// There is only ONE processor for all the threads. It stores messages
			// and processes them sequentially. DM operations are expected to be short-rabbitMqIsRunning.
			// The DM is just an intermediary between REST clients and agents.
			new Thread( "Roboconf - Queue listener for the DM" ) {
				@Override
				public void run() {
					RabbitMqUtils.listenToRabbitMq(
							"The DM", DmClient.this.logger,
							consumer, DmClient.this.messageProcessor );
				};

			}.start();
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IDmClient
	 * #deleteMessagingServerArtifacts(net.roboconf.core.model.runtime.Application)
	 */
	@Override
	public void deleteMessagingServerArtifacts( Application application )
	throws IOException {

		// We delete the exchanges
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeName( application, true ));
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeName( application, false ));
		// Queues are deleted automatically by RabbitMQ
	}
}
