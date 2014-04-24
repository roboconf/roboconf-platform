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

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.messages.from_agent_to_agent.MsgCmdImportAdd;
import net.roboconf.messaging.internal.messages.from_agent_to_agent.MsgCmdImportRemove;
import net.roboconf.messaging.internal.messages.from_agent_to_agent.MsgCmdImportRequest;
import net.roboconf.messaging.internal.utils.RabbitMqUtils;
import net.roboconf.messaging.internal.utils.SerializationUtils;
import net.roboconf.messaging.messages.Message;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * The RabbitMQ client for an agent.
 * @author Vincent Zurczak - Linagora
 */
public class AgentClient implements IAgentClient {

	private static final String THOSE_THAT_EXPORT = "those.that.export.";
	private static final String THOSE_THAT_IMPORT = "those.that.import.";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String applicationName, rootInstanceName, messageServerIp;

	String consumerTag;
	Channel	channel;
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
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #setRootInstanceName(java.lang.String)
	 */
	@Override
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient
	 * #openConnection(net.roboconf.messaging.client.AbstractMessageProcessor)
	 */
	@Override
	public void openConnection( AbstractMessageProcessor messageProcessor )
	throws IOException {

		// Already connected? Do nothing
		this.logger.fine( "Agent " + this.rootInstanceName + " is opening a connection to RabbitMQ." );
		if( this.channel != null ) {
			this.logger.info( "Agent " + this.rootInstanceName + " has already a connection to RabbitMQ." );
			return;
		}

		// Initialize the connection
		ConnectionFactory factory = new ConnectionFactory();
		RabbitMqUtils.configureFactory( factory, this.messageServerIp );
		this.channel = factory.newConnection().createChannel();

		// Store the message processor for later
		this.messageProcessor = messageProcessor;
		this.messageProcessor.start();

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

		new Thread( "Roboconf - Queue listener for Agent " + this.rootInstanceName ) {
			@Override
			public void run() {
				RabbitMqUtils.listenToRabbitMq(
						AgentClient.this.rootInstanceName, AgentClient.this.logger,
						consumer, AgentClient.this.messageProcessor );
			};

		}.start();
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IClient#closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {
		this.logger.fine( "Agent " + this.rootInstanceName + " is closing its connection to RabbitMQ." );

		// Stop listening messages
		if( this.channel != null
				&& this.channel.isOpen()
				&& this.consumerTag != null )
			this.channel.basicCancel( this.consumerTag );

		// Stop processing messages
		if( this.messageProcessor != null
				&& this.messageProcessor.isAlive())
			this.messageProcessor.interrupt();

		// Close the connection
		this.consumerTag = null;
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
	 * #publishExports(net.roboconf.core.model.runtime.Instance)
	 */
	@Override
	public void publishExports( Instance instance ) throws IOException {
		this.logger.fine( "Agent " + this.rootInstanceName + " is publishing its exports." );

		// For all the exported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {

			// Find the variables to export.
			Map<String,String> toPublish = new HashMap<String,String> ();
			for( Map.Entry<String,String> entry : InstanceHelpers.getExportedVariables( instance ).entrySet()) {
				if( entry.getKey().startsWith( facetOrComponentName + "." ))
					toPublish.put( entry.getKey(), entry.getValue());
			}

			// Publish them
			MsgCmdImportAdd message = new MsgCmdImportAdd(
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
	 * #unpublishExports(net.roboconf.core.model.runtime.Instance)
	 */
	@Override
	public void unpublishExports( Instance instance ) throws IOException {
		this.logger.fine( "Agent " + this.rootInstanceName + " is un-publishing its exports." );

		// For all the exported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {

			// Publish them
			MsgCmdImportRemove message = new MsgCmdImportRemove(
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
	 * #listenToRequestsFromOtherAgents(net.roboconf.messaging.client.IClient.ListenerCommand, net.roboconf.core.model.runtime.Instance)
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
				this.logger.fine( "Agent " + this.rootInstanceName + " starts listening requests from other agents." );
				this.channel.queueBind( queueName, exchangeName, routingKey );

			} else {
				this.logger.fine( "Agent " + this.rootInstanceName + " stops listening requests from other agents." );
				this.channel.queueUnbind( queueName, exchangeName, routingKey );
			}
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #requestExportsFromOtherAgents(net.roboconf.core.model.runtime.Instance)
	 */
	@Override
	public void requestExportsFromOtherAgents( Instance instance ) throws IOException {
		this.logger.fine( "Agent " + this.rootInstanceName + " is requesting exports from other agents." );

		// For all the imported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {

			// ... and ask to publish them.
			// Grouping variable requests by prefix reduces the number of messages.
			MsgCmdImportRequest message = new MsgCmdImportRequest( facetOrComponentName );
			this.channel.basicPublish(
					RabbitMqUtils.buildExchangeName( this.applicationName, false ),
					THOSE_THAT_EXPORT + facetOrComponentName,
					null,
					SerializationUtils.serializeObject( message ));
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.client.IAgentClient
	 * #listenToExportsFromOtherAgents(net.roboconf.messaging.client.IClient.ListenerCommand, net.roboconf.core.model.runtime.Instance)
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
				this.logger.fine( "Agent " + this.rootInstanceName + " starts listening exports from other agents." );
				this.channel.queueBind( queueName, exchangeName, routingKey );

			} else {
				this.logger.fine( "Agent " + this.rootInstanceName + " stops listening exports from other agents." );
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

		this.logger.fine( "Agent " + this.rootInstanceName + " is sending a " + message.getClass().getSimpleName() + " message to the DM." );
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
			this.logger.fine( "Agent " + this.rootInstanceName + " starts listening to the DM." );
			this.channel.queueBind( queueName, exchangeName, routingKey );

		} else {
			this.logger.fine( "Agent " + this.rootInstanceName + " stops listening to the DM." );
			this.channel.queueUnbind( queueName, exchangeName, routingKey );
		}
	}


	private String getQueueName() {
		return this.applicationName + "." + this.rootInstanceName;
	}
}
