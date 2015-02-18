/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.internal.client.rabbitmq;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.internal.utils.RabbitMqUtils;
import net.roboconf.messaging.internal.utils.SerializationUtils;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.messages.from_dm_to_dm.MsgEcho;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * The RabbitMQ client for the DM.
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class RabbitMqClientDm implements IDmClient {

	private static final String DEBUG_QUEUE_NAME = "dm.debug";
	private final Logger logger = Logger.getLogger( getClass().getName());
	private String messageServerIp, messageServerUsername, messageServerPassword;
	private LinkedBlockingQueue<Message> messageQueue;

	final Map<String,String> applicationNameToConsumerTag = new HashMap<String,String> ();
	Channel channel;

	// The consumer tag for the debug queue consumer.
	// Also used as a flag to check if debug message listener is started or not.
	private String debugConsumerTag;


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient
	 * #setParameters(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void setParameters( String messageServerIp, String messageServerUsername, String messageServerPassword ) {
		this.messageServerIp = messageServerIp;
		this.messageServerUsername = messageServerUsername;
		this.messageServerPassword = messageServerPassword;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient
	 * #setMessageQueue(java.util.concurrent.LinkedBlockingQueue)
	 */
	@Override
	public void setMessageQueue( LinkedBlockingQueue<Message> messageQueue ) {
		this.messageQueue = messageQueue;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return this.channel != null;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient#openConnection()
	 */
	@Override
	public void openConnection() throws IOException {

		// Already connected? Do nothing
		this.logger.info( "The DM is opening a connection to RabbitMQ." );
		if( isConnected()) {
			this.logger.info( "The DM has already a connection to RabbitMQ." );
			return;
		}

		// Initialize the connection
		ConnectionFactory factory = new ConnectionFactory();
		RabbitMqUtils.configureFactory( factory, this.messageServerIp, this.messageServerUsername, this.messageServerPassword );
		this.channel = factory.newConnection().createChannel();
		this.logger.info( "The DM established a new connection with RabbitMQ. Channel # " + this.channel.getChannelNumber());

		// Be notified when a message does not arrive in a queue (i.e. nobody is listening)
		this.channel.addReturnListener( new DmReturnListener());

		// Declare the DM debug-dedicated queue.
		this.channel.queueDeclare( DEBUG_QUEUE_NAME, true, false, true, null );
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient
	 * #closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {

		StringBuilder sb = new StringBuilder( "The DM is closing its connection to RabbitMQ." );
		if( this.channel != null )
			sb.append( " Channel # " + this.channel.getChannelNumber());

		this.logger.info( sb.toString());
		if( isConnected())
			RabbitMqUtils.closeConnection( this.channel );

		this.channel = null;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IDmClient
	 * #publishMessageToAgent(net.roboconf.core.model.beans.Application, net.roboconf.core.model.beans.Instance, net.roboconf.messaging.messages.Message)
	 */
	@Override
	public void sendMessageToAgent( Application application, Instance instance, Message message )
	throws IOException {

		String exchangeName = RabbitMqUtils.buildExchangeName( application, false );
		String routingKey = RabbitMqUtils.buildRoutingKeyForAgent( instance );
		this.logger.fine( "The DM sends a message to " + routingKey + ". Message type: " + message.getClass().getSimpleName());

		// We are requesting mandatory publication.
		// It means we expect this message to reach at least one queue.
		// If not, we want to be notified about it.
		this.channel.basicPublish(
				exchangeName, routingKey,
				true, false, null,
				SerializationUtils.serializeObject( message ));

		this.logger.fine( "The DM sent a message to " + routingKey + ". Message type: " + message.getClass().getSimpleName());
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IDmClient
	 * #listenToAgentMessages(net.roboconf.core.model.beans.Application, net.roboconf.messaging.client.IClient.ListenerCommand)
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
			if( this.applicationNameToConsumerTag.containsKey( application.getName())) {
				this.logger.finer( "Application " + application + " is already listened to by a messaging client." );
				return;
			}

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

			// The DM has a listening thread for every application.
			// Each thread listens for new messages and stores them in the message processor.

			// But there is only one message queue for the entire DM.
			// And the DM should only have ONE message processor.
			String threadName = "Roboconf - Queue listener for the DM";
			String id = "The DM";
			new ListeningThread( threadName, this.logger, consumer, this.messageQueue, id ).start();
		}
	}


	@Override
	public void sendMessageToDebug( MsgEcho echo, long ttl ) throws IOException {
		this.logger.fine( "The DM sends a debug message to " + DEBUG_QUEUE_NAME + ". Message: " + echo.getContent() );

		this.channel.basicPublish(
				"",                                             // no exchange
				DEBUG_QUEUE_NAME,                               // the queue
				new BasicProperties.Builder()                   // Message properties (ttl)
						.expiration( Long.toString( ttl ) )
						.build(),
				SerializationUtils.serializeObject( echo ) );    // Message content

		this.logger.fine( "The DM sent a debug message to " + DEBUG_QUEUE_NAME + ". Message: " + echo.getContent() );
	}


	@Override
	public void listenToDebugMessages( ListenerCommand command ) throws IOException {
		switch (command) {
			case START:
				// Check we're not already listening!
				if ( this.debugConsumerTag != null ) {
					this.logger.finer( "The DM is already listening to its debug queue." );
					return;
				}
				if ( this.channel != null && this.channel.isOpen() ) {
					// Create the debug message consumer and start consuming.
					// No auto-ACK. Messages must be acknowledged manually by the consumer.
					QueueingConsumer debugConsumer = new QueueingConsumer( this.channel );
					debugConsumerTag = this.channel.basicConsume(
							DEBUG_QUEUE_NAME,       // queue
							false,                  // no auto ACK
							DEBUG_QUEUE_NAME,       // consumer tag set to the queue name
							false,                  // get local messages (ESSENTIAL!)
							false,                  // consumer is not exclusive
							null,                   // no parameters
							debugConsumer );        // the consumer
					this.logger.fine( "The DM starts listening to its debug queue" );
					// Starts the listening thread!
					new ListeningThread(
							"Roboconf - Debug queue listener",
							this.logger,
							debugConsumer,
							this.messageQueue,
							"The DM (debug)" ).start();
				}
				else {
					this.logger.warning( "Cannot start listening to debug queue: channel closed" );
				}
				break;
			case STOP:
				// Check we are listening!
				if ( this.debugConsumerTag == null ) {
					this.logger.finer( "The DM is not listening to its debug queue." );
					return;
				}
				if ( this.channel != null && this.channel.isOpen() ) {
					// Cancel the consumer.
					this.channel.basicCancel( this.debugConsumerTag );
					this.debugConsumerTag = null;
					this.logger.fine( "The DM stops listening to its debug queue" );
				}
				else {
					this.logger.warning( "Cannot stop listening to debug queue: channel closed" );
				}
				break;
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IDmClient
	 * #deleteMessagingServerArtifacts(net.roboconf.core.model.beans.Application)
	 */
	@Override
	public void deleteMessagingServerArtifacts( Application application )
	throws IOException {

		// We delete the exchanges
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeName( application, true ));
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeName( application, false ));
		// Queues are deleted automatically by RabbitMQ
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IDmClient
	 * #propagateAgentTermination(net.roboconf.core.model.beans.Application, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void propagateAgentTermination( Application application, Instance rootInstance )
	throws IOException {

		this.logger.fine( "The DM is propagating the termination of agent '" + rootInstance + "'." );

		// The messages will go through JUST like if they were coming from other agents.
		String exchangeName = RabbitMqUtils.buildExchangeName( application, false );

		// Start with the deepest instances
		List<Instance> instances = InstanceHelpers.buildHierarchicalList( rootInstance );
		Collections.reverse( instances );

		// Roughly, we unpublish all the variables for all the instances that were on the agent's machine.
		// This code is VERY similar to ...ClientAgent#unpublishExports
		for( Instance instance : instances ) {
			for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {

				MsgCmdRemoveImport message = new MsgCmdRemoveImport(
						facetOrComponentName,
						InstanceHelpers.computeInstancePath( instance ));

				this.channel.basicPublish(
						exchangeName,
						RabbitMqClientAgent.THOSE_THAT_IMPORT + facetOrComponentName,
						null,
						SerializationUtils.serializeObject( message ));
			}
		}
	}
}
