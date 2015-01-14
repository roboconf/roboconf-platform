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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.utils.RabbitMqUtils;
import net.roboconf.messaging.internal.utils.SerializationUtils;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.messages.from_agent_to_agent.MsgCmdRequestImport;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * The RabbitMQ client for an agent.
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqClientAgent implements IAgentClient {

	private static final String THOSE_THAT_EXPORT = "those.that.export.";
	public static final String THOSE_THAT_IMPORT = "those.that.import.";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String applicationName, rootInstanceName, messageServerIp, messageServerUsername, messageServerPassword;
	private LinkedBlockingQueue<Message> messageQueue;

	String consumerTag;
	Channel	channel;


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
	 * @see net.roboconf.messaging.client.IAgentClient#setRootInstanceName(java.lang.String)
	 */
	@Override
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient#openConnection()
	 */
	@Override
	public void openConnection() throws IOException {

		// Already connected? Do nothing
		this.logger.info( "Agent '" + getAgentId() + "' is opening a connection to RabbitMQ." );
		if( this.channel != null ) {
			this.logger.info( "Agent '" + getAgentId() + "' has already a connection to RabbitMQ." );
			return;
		}

		// Initialize the connection
		ConnectionFactory factory = new ConnectionFactory();
		RabbitMqUtils.configureFactory( factory, this.messageServerIp, this.messageServerUsername, this.messageServerPassword );
		this.channel = factory.newConnection().createChannel();
		this.logger.info( "Agent '" + getAgentId() + "' established a new connection with RabbitMQ. Channel # " + this.channel.getChannelNumber());

		// We start listening the queue here
		// We declare both exchanges.
		// This is for cases where the agent would try to contact the DM
		// before the DM was started. In such cases, the RabbitMQ client
		// will get an error. This error will in turn close the channel and it
		// won't be usable anymore.
		RabbitMqUtils.declareApplicationExchanges( this.applicationName, this.channel );
		// This is really important.

		// Queue declaration is idem-potent
		String queueName = getQueueName();
		this.channel.queueDeclare( queueName, true, false, true, null );

		// Start to listen to the queue
		final QueueingConsumer consumer = new QueueingConsumer( this.channel );
		this.consumerTag = this.channel.basicConsume( queueName, true, consumer );

		String threadName = "Roboconf - Queue listener for Agent " + this.rootInstanceName;
		String id = "Agent '" + getAgentId() + "'";
		new ListeningThread( threadName, this.logger, consumer, this.messageQueue, id ).start();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient#closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {

		StringBuilder sb = new StringBuilder( "Agent '" + getAgentId());
		sb.append( "' is closing its connection to RabbitMQ.");
		if( this.channel != null )
			sb.append( " Channel # " + this.channel.getChannelNumber());

		this.logger.info( sb.toString());

		// Stop listening messages
		if( this.channel != null
				&& this.channel.isOpen()
				&& this.consumerTag != null )
			this.channel.basicCancel( this.consumerTag );

		// Close the connection
		this.consumerTag = null;
		if( isConnected())
			RabbitMqUtils.closeConnection( this.channel );

		this.channel = null;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient#setApplicationName(java.lang.String)
	 */
	@Override
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #publishExports(net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void publishExports( Instance instance ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is publishing its exports." );

		// For all the exported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance ))
			publishExports( instance, facetOrComponentName );
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #publishExports(net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void publishExports( Instance instance, String facetOrComponentName ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is publishing its exports prefixed by " + facetOrComponentName + "." );

		// Find the variables to export.
		Map<String,String> toPublish = new HashMap<String,String> ();
		Map<String,String> exports = InstanceHelpers.findAllExportedVariables( instance );
		for( Map.Entry<String,String> entry : exports.entrySet()) {
			if( entry.getKey().startsWith( facetOrComponentName + "." ))
				toPublish.put( entry.getKey(), entry.getValue());
		}

		// Publish them
		if( ! toPublish.isEmpty()) {
			MsgCmdAddImport message = new MsgCmdAddImport(
					facetOrComponentName,
					InstanceHelpers.computeInstancePath( instance ),
					toPublish );

			this.channel.basicPublish(
					RabbitMqUtils.buildExchangeName( this.applicationName, false ),
					THOSE_THAT_IMPORT + facetOrComponentName,
					null,
					SerializationUtils.serializeObject( message ));
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #unpublishExports(net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void unpublishExports( Instance instance ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is un-publishing its exports." );

		// For all the exported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {

			// Publish them
			MsgCmdRemoveImport message = new MsgCmdRemoveImport(
					facetOrComponentName,
					InstanceHelpers.computeInstancePath( instance ));

			this.channel.basicPublish(
					RabbitMqUtils.buildExchangeName( this.applicationName, false ),
					THOSE_THAT_IMPORT + facetOrComponentName,
					null,
					SerializationUtils.serializeObject( message ));
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #listenToRequestsFromOtherAgents(net.roboconf.messaging.client.IClient.ListenerCommand, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void listenToRequestsFromOtherAgents( ListenerCommand command, Instance instance )
	throws IOException {

		// With RabbitMQ, and for agents, listening to others means
		// create a binding between the "agents" exchange and the agent's queue.
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {

			// On which routing key do request go? Those.that.export...
			String routingKey = THOSE_THAT_EXPORT + facetOrComponentName;
			String queueName = getQueueName();
			String exchangeName = RabbitMqUtils.buildExchangeName( this.applicationName, false );

			if( command == ListenerCommand.START ) {
				this.logger.fine( "Agent '" + getAgentId() + "' starts listening requests from other agents." );
				this.channel.queueBind( queueName, exchangeName, routingKey );

			} else {
				this.logger.fine( "Agent '" + getAgentId() + "' stops listening requests from other agents." );
				this.channel.queueUnbind( queueName, exchangeName, routingKey );
			}
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #requestExportsFromOtherAgents(net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void requestExportsFromOtherAgents( Instance instance ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is requesting exports from other agents." );

		// For all the imported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {

			// ... and ask to publish them.
			// Grouping variable requests by prefix reduces the number of messages.
			MsgCmdRequestImport message = new MsgCmdRequestImport( facetOrComponentName );
			this.channel.basicPublish(
					RabbitMqUtils.buildExchangeName( this.applicationName, false ),
					THOSE_THAT_EXPORT + facetOrComponentName,
					null,
					SerializationUtils.serializeObject( message ));
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #listenToExportsFromOtherAgents(net.roboconf.messaging.client.IClient.ListenerCommand, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException {

		// With RabbitMQ, and for agents, listening to others means
		// create a binding between the "agents" exchange and the agent's queue.
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {

			// On which routing key do export go? Those.that.import...
			String routingKey = THOSE_THAT_IMPORT + facetOrComponentName;
			String queueName = getQueueName();
			String exchangeName = RabbitMqUtils.buildExchangeName( this.applicationName, false );

			if( command == ListenerCommand.START ) {
				this.logger.fine( "Agent '" + getAgentId() + "' starts listening exports from other agents." );
				this.channel.queueBind( queueName, exchangeName, routingKey );

			} else {
				this.logger.fine( "Agent '" + getAgentId() + "' stops listening exports from other agents." );
				this.channel.queueUnbind( queueName, exchangeName, routingKey );
			}
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #sendMessageToTheDm(net.roboconf.messaging.messages.Message)
	 */
	@Override
	public void sendMessageToTheDm( Message message ) throws IOException {

		this.logger.fine( "Agent '" + getAgentId() + "' is sending a " + message.getClass().getSimpleName() + " message to the DM." );
		this.channel.basicPublish(
				RabbitMqUtils.buildExchangeName( this.applicationName, true ),
				"", null,
				SerializationUtils.serializeObject( message ));
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #listenToTheDm(net.roboconf.messaging.client.IClient.ListenerCommand)
	 */
	@Override
	public void listenToTheDm( ListenerCommand command ) throws IOException {

		// Bind the root instance name with the queue
		String queueName = getQueueName();
		String exchangeName = RabbitMqUtils.buildExchangeName( this.applicationName, false );
		String routingKey = RabbitMqUtils.buildRoutingKeyForAgent( this.rootInstanceName );

		// queueBind is idem-potent
		if( command == ListenerCommand.START ) {
			this.logger.fine( "Agent '" + getAgentId() + "' starts listening to the DM." );
			this.channel.queueBind( queueName, exchangeName, routingKey );

		} else {
			this.logger.fine( "Agent '" + getAgentId() + "' stops listening to the DM." );
			this.channel.queueUnbind( queueName, exchangeName, routingKey );
		}
	}


	private String getQueueName() {
		return this.applicationName + "." + this.rootInstanceName;
	}


	private String getAgentId() {
		return Utils.isEmptyOrWhitespaces( this.rootInstanceName ) ? "?" : this.rootInstanceName;
	}
}
