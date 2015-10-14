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

package net.roboconf.messaging.rabbitmq.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.api.utils.SerializationUtils;
import net.roboconf.messaging.rabbitmq.internal.utils.DmReturnListener;
import net.roboconf.messaging.rabbitmq.internal.utils.ListeningThread;
import net.roboconf.messaging.rabbitmq.internal.utils.MessagingContext;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqUtils;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * The RabbitMQ client for the DM.
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class RabbitMqClientDm extends RabbitMqClient implements IDmClient {

	private static final String DM_NEUTRAL_QUEUE_NAME = "roboconf.dm.neutral";

	String neutralConsumerTag;
	final Map<String,String> applicationNameToConsumerTag = new HashMap<>();
	QueueingConsumer consumer;


	/**
	 * Constructor.
	 * @param reconfigurable
	 * @param ip
	 * @param username
	 * @param password
	 */
	public RabbitMqClientDm( final ReconfigurableClientDm reconfigurable, String ip, String username, String password ) {
		super(reconfigurable, ip, username, password);
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient
	 * #openConnection()
	 */
	@Override
	public synchronized void openConnection() throws IOException {

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
		this.channel.queueDeclare( DM_NEUTRAL_QUEUE_NAME, true, false, true, null );

		// Start listening to messages.
		this.consumer = new QueueingConsumer( this.channel );
		String threadName = "Roboconf - Queue listener for the DM";
		String id = "The DM";
		new ListeningThread( threadName, this.logger, this.consumer, this.messageQueue, id ).start();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient
	 * #closeConnection()
	 */
	@Override
	public synchronized void closeConnection() throws IOException {

		StringBuilder sb = new StringBuilder( "The DM is closing its connection to RabbitMQ." );
		if( this.channel != null )
			sb.append(" Channel # ").append(this.channel.getChannelNumber());

		this.logger.info( sb.toString());
		if( isConnected())
			RabbitMqUtils.closeConnection( this.channel );

		this.channel = null;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IDmClient
	 * #publishMessageToAgent(net.roboconf.core.model.beans.Application, net.roboconf.core.model.beans.Instance, net.roboconf.messaging.api.messages.Message)
	 */
	@Override
	public synchronized void sendMessageToAgent( Application application, Instance instance, Message message )
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
	 * @see net.roboconf.messaging.api.client.IDmClient
	 * #listenToAgentMessages(net.roboconf.core.model.beans.Application, net.roboconf.messaging.api.client.IClient.ListenerCommand)
	 */
	@Override
	public synchronized void listenToAgentMessages( Application application, ListenerCommand command )
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
			String consumerTag = this.channel.basicConsume( queueName, true, this.consumer );
			this.applicationNameToConsumerTag.put( application.getName(), consumerTag );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient
	 * #sendMessageToTheDm(net.roboconf.messaging.api.messages.Message)
	 */
	@Override
	public synchronized void sendMessageToTheDm( Message msg ) throws IOException {

		// The DM can send messages to itself (e.g. for debug).
		// This method could also be used to broadcast information to (potential) other DMs.
		this.logger.fine( "The DM sends a message to the DM's neutral queue." );
		this.channel.queueDeclare( DM_NEUTRAL_QUEUE_NAME, true, false, true, null );

		// To prevent spamming and residual messages, messages sent by the DM
		// (to itself or its siblings) have a life span of 500 ms. If there is no
		// client connected during this period, the message will be dropped.
		this.channel.basicPublish(
				"", DM_NEUTRAL_QUEUE_NAME,
				new BasicProperties.Builder().expiration( "500" ).build(),
				SerializationUtils.serializeObject( msg ));

		this.logger.fine( "The DM sent a message to the DM's neutral queue." );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient
	 * #listenToTheDm(net.roboconf.messaging.api.client.IClient.ListenerCommand)
	 */
	@Override
	public synchronized void listenToTheDm( ListenerCommand command )
	throws IOException {

		if( command == ListenerCommand.START ) {
			if( this.neutralConsumerTag != null ) {
				this.logger.finer( "The DM is already listening to the neutral queue." );
				return;
			}

			this.channel.queueDeclare( DM_NEUTRAL_QUEUE_NAME, true, false, true, null );

			// Create the debug message consumer and start consuming.
			// No auto-ACK. Messages must be acknowledged manually by the consumer.
			this.neutralConsumerTag = this.channel.basicConsume(
					DM_NEUTRAL_QUEUE_NAME,  // queue
					true,                   // auto ACK
					DM_NEUTRAL_QUEUE_NAME,  // consumer tag set to the queue name
					false,                  // get local messages (ESSENTIAL!)
					false,                  // consumer is not exclusive
					null,                   // no parameters
					this.consumer );        		// the consumer

		} else {
			this.logger.fine( "The DM stops listening to the neutral queue." );
			if ( this.neutralConsumerTag != null
					&& this.channel != null
					&& this.channel.isOpen())
				this.channel.basicCancel( this.neutralConsumerTag );

			this.neutralConsumerTag = null;
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IDmClient
	 * #deleteMessagingServerArtifacts(net.roboconf.core.model.beans.Application)
	 */
	@Override
	public synchronized void deleteMessagingServerArtifacts( Application application )
	throws IOException {

		// We delete the exchanges
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeName( application, true ));
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeName( application, false ));
		// Queues are deleted automatically by RabbitMQ
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IDmClient
	 * #propagateAgentTermination(net.roboconf.core.model.beans.Application, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public synchronized void propagateAgentTermination( Application application, Instance rootInstance )
	throws IOException {

		this.logger.fine( "The DM is propagating the termination of agent '" + rootInstance + "'." );

		// Start with the deepest instances
		List<Instance> instances = InstanceHelpers.buildHierarchicalList( rootInstance );
		Collections.reverse( instances );

		// Roughly, we unpublish all the variables for all the instances that were on the agent's machine.
		// This code is VERY similar to ...ClientAgent#unpublishExports
		// The messages will go through JUST like if they were coming from other agents.
		for( Instance instance : instances ) {
			for( MessagingContext ctx : MessagingContext.forExportedVariables( application.getName(), instance, application.getExternalExports())) {

				MsgCmdRemoveImport message = new MsgCmdRemoveImport(
						application.getName(),
						ctx.getRoutingKeySuffix(),
						InstanceHelpers.computeInstancePath( instance ));

				this.channel.basicPublish(
						ctx.getExchangeName(),
						RabbitMqClientAgent.THOSE_THAT_IMPORT + ctx.getRoutingKeySuffix(),
						null,
						SerializationUtils.serializeObject( message ));
			}
		}
	}
}
