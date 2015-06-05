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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.utils.SerializationUtils;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * The RabbitMQ client for an agent.
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class RabbitMqClientAgent extends RabbitMqClient implements IAgentClient {

	private static final String THOSE_THAT_EXPORT = "those.that.export.";
	public static final String THOSE_THAT_IMPORT = "those.that.import.";

	private String applicationName, scopedInstancePath;

	String consumerTag;

	public RabbitMqClientAgent( final ReconfigurableClientAgent reconfigurable, String ip, String username, String password ) {
		super(reconfigurable, ip, username, password);
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IAgentClient#setScopedInstancePath(java.lang.String)
	 */
	@Override
	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient#openConnection()
	 */
	@Override
	public synchronized void openConnection() throws IOException {

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

		String threadName = "Roboconf - Queue listener for Agent " + this.scopedInstancePath;
		String id = "Agent '" + getAgentId() + "'";
		new ListeningThread( threadName, this.logger, consumer, this.messageQueue, id ).start();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient#closeConnection()
	 */
	@Override
	public synchronized void closeConnection() throws IOException {

		StringBuilder sb = new StringBuilder( "Agent '" + getAgentId());
		sb.append( "' is closing its connection to RabbitMQ.");
		if( this.channel != null )
			sb.append(" Channel # ").append(this.channel.getChannelNumber());

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
	 * @see net.roboconf.messaging.api.client.IAgentClient#setApplicationName(java.lang.String)
	 */
	@Override
	public synchronized void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IAgentClient
	 * #publishExports(net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public void publishExports( Instance instance ) throws IOException {

		// For all the exported variables...
		// ... find the component or facet name...
		Set<String> names = VariableHelpers.findPrefixesForExportedVariables( instance );
		if( names.isEmpty())
			this.logger.fine( "Agent '" + getAgentId() + "' is publishing its exports." );

		else for( String facetOrComponentName : names ) {
			publishExports( instance, facetOrComponentName );
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IAgentClient
	 * #publishExports(net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public synchronized void publishExports( Instance instance, String facetOrComponentName ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is publishing its exports prefixed by " + facetOrComponentName + "." );

		// Find the variables to export.
		Map<String,String> toPublish = new HashMap<>();
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
	 * @see net.roboconf.messaging.api.client.IAgentClient
	 * #unpublishExports(net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public synchronized void unpublishExports( Instance instance ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is un-publishing its exports." );

		// For all the exported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {

			// Log here, for debug
			this.logger.fine( "Agent '" + getAgentId() + "' is un-publishing its exports (" + facetOrComponentName + ")." );

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
	 * @see net.roboconf.messaging.api.client.IAgentClient
	 * #listenToRequestsFromOtherAgents(net.roboconf.messaging.api.client.IClient.ListenerCommand, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public synchronized void listenToRequestsFromOtherAgents( ListenerCommand command, Instance instance )
	throws IOException {

		// With RabbitMQ, and for agents, listening to others means
		// create a binding between the "agents" exchange and the agent's queue.
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {

			// On which routing key do request go? Those.that.export...
			String routingKey = THOSE_THAT_EXPORT + facetOrComponentName;
			String queueName = getQueueName();
			String exchangeName = RabbitMqUtils.buildExchangeName( this.applicationName, false );

			if( command == ListenerCommand.START ) {
				this.logger.fine( "Agent '" + getAgentId() + "' starts listening requests from other agents (" + facetOrComponentName + ")." );
				this.channel.queueBind( queueName, exchangeName, routingKey );

			} else {
				this.logger.fine( "Agent '" + getAgentId() + "' stops listening requests from other agents (" + facetOrComponentName + ")." );
				this.channel.queueUnbind( queueName, exchangeName, routingKey );
			}
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IAgentClient
	 * #requestExportsFromOtherAgents(net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public synchronized void requestExportsFromOtherAgents( Instance instance ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is requesting exports from other agents." );

		// For all the imported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {

			// Log here, for debug
			this.logger.fine( "Agent '" + getAgentId() + "' is requesting exports from other agents (" + facetOrComponentName + ")." );

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
	 * @see net.roboconf.messaging.api.client.IAgentClient
	 * #listenToExportsFromOtherAgents(net.roboconf.messaging.api.client.IClient.ListenerCommand, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public synchronized void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException {

		// With RabbitMQ, and for agents, listening to others means
		// create a binding between the "agents" exchange and the agent's queue.
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {

			// On which routing key do export go? Those.that.import...
			String routingKey = THOSE_THAT_IMPORT + facetOrComponentName;
			String queueName = getQueueName();
			String exchangeName = RabbitMqUtils.buildExchangeName( this.applicationName, false );

			if( command == ListenerCommand.START ) {
				this.logger.fine( "Agent '" + getAgentId() + "' starts listening exports from other agents (" + facetOrComponentName + ")." );
				this.channel.queueBind( queueName, exchangeName, routingKey );

			} else {
				this.logger.fine( "Agent '" + getAgentId() + "' stops listening exports from other agents (" + facetOrComponentName + ")." );
				this.channel.queueUnbind( queueName, exchangeName, routingKey );
			}
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient
	 * #sendMessageToTheDm(net.roboconf.messaging.api.messages.Message)
	 */
	@Override
	public synchronized void sendMessageToTheDm( Message message ) throws IOException {

		this.logger.fine( "Agent '" + getAgentId() + "' is sending a " + message.getClass().getSimpleName() + " message to the DM." );
		this.channel.basicPublish(
				RabbitMqUtils.buildExchangeName( this.applicationName, true ),
				"", null,
				SerializationUtils.serializeObject( message ));
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient
	 * #listenToTheDm(net.roboconf.messaging.api.client.IClient.ListenerCommand)
	 */
	@Override
	public synchronized void listenToTheDm( ListenerCommand command ) throws IOException {

		// Bind the root instance name with the queue
		String queueName = getQueueName();
		String exchangeName = RabbitMqUtils.buildExchangeName( this.applicationName, false );
		String routingKey = RabbitMqUtils.buildRoutingKeyForAgent( this.scopedInstancePath );

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
		return this.applicationName + RabbitMqUtils.escapeInstancePath( this.scopedInstancePath );
	}


	private String getAgentId() {
		return Utils.isEmptyOrWhitespaces( this.scopedInstancePath ) ? "?" : this.scopedInstancePath;
	}
}
