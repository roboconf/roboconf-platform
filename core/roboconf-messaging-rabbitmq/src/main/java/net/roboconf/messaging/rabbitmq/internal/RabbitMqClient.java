/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Recoverable;

import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.api.utils.MessagingUtils;
import net.roboconf.messaging.api.utils.SerializationUtils;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;
import net.roboconf.messaging.rabbitmq.internal.impl.RoboconfConsumer;
import net.roboconf.messaging.rabbitmq.internal.impl.RoboconfRecoveryListener;
import net.roboconf.messaging.rabbitmq.internal.impl.RoboconfReturnListener;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqUtils;

/**
 * Common RabbitMQ client-related stuffs.
 * @author Pierre Bourret - Université Joseph Fourier
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqClient implements IMessagingClient {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String messageServerIp, messageServerUsername, messageServerPassword;
	private final WeakReference<ReconfigurableClient<?>> reconfigurable;

	private LinkedBlockingQueue<Message> messageQueue;
	private RecipientKind ownerKind;
	private String applicationName, scopedInstancePath, domain;

	String consumerTag;
	Channel channel;


	/**
	 * Constructor.
	 * @param reconfigurable
	 * @param ip
	 * @param username
	 * @param password
	 */
	protected RabbitMqClient( ReconfigurableClient<?> reconfigurable, String ip, String username, String password ) {
		this( reconfigurable, ip, username, password, reconfigurable.getOwnerKind());
	}


	/**
	 * Constructor.
	 * @param reconfigurable
	 * @param ip
	 * @param username
	 * @param password
	 * @param ownerKind
	 */
	protected RabbitMqClient( ReconfigurableClient<?> reconfigurable, String ip, String username, String password, RecipientKind ownerKind ) {
		this.reconfigurable = new WeakReference<ReconfigurableClient<?>>( reconfigurable );
		this.messageServerIp = ip;
		this.messageServerUsername = username;
		this.messageServerPassword = password;
		this.ownerKind = ownerKind;
	}


	/**
	 * @return the wrapping reconfigurable client (may be {@code null}).
	 */
	public final ReconfigurableClient<?> getReconfigurableClient() {
		return this.reconfigurable.get();
	}


	@Override
	public final void setMessageQueue( LinkedBlockingQueue<Message> messageQueue ) {
		this.messageQueue = messageQueue;
	}


	@Override
	public final synchronized boolean isConnected() {
		return this.channel != null;
	}


	@Override
	public final String getMessagingType() {
		return RabbitMqConstants.FACTORY_RABBITMQ;
	}


	@Override
	public final Map<String, String> getConfiguration() {

		final Map<String, String> configuration = new LinkedHashMap<>();
		configuration.put(MessagingConstants.MESSAGING_TYPE_PROPERTY, RabbitMqConstants.FACTORY_RABBITMQ);
		configuration.put(RabbitMqConstants.RABBITMQ_SERVER_IP, this.messageServerIp);
		configuration.put(RabbitMqConstants.RABBITMQ_SERVER_USERNAME, this.messageServerUsername);
		configuration.put(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD, this.messageServerPassword);

		return Collections.unmodifiableMap(configuration);
	}


	@Override
	public void setOwnerProperties( RecipientKind ownerKind, String domain, String applicationName, String scopedInstancePath ) {

		this.ownerKind = ownerKind;
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
		this.domain = domain;

		this.logger.fine( "Owner properties changed to " + getId());
	}


	@Override
	public void openConnection() throws IOException {

		// Already connected? Do nothing
		this.logger.info( getId() + " is opening a connection to RabbitMQ." );
		if( isConnected()) {
			this.logger.info( getId() + " has already a connection to RabbitMQ." );
			return;
		}

		// Initialize the connection
		ConnectionFactory factory = new ConnectionFactory();
		RabbitMqUtils.configureFactory( factory, this.messageServerIp, this.messageServerUsername, this.messageServerPassword );
		this.channel = factory.newConnection().createChannel();
		this.logger.info( getId() + " established a new connection with RabbitMQ. Channel # " + this.channel.getChannelNumber());

		// Be notified when a message does not arrive in a queue (i.e. nobody is listening)
		this.channel.addReturnListener( new RoboconfReturnListener());

		// Add a recoverable listener (when broken connections are recovered).
		// Given the way the RabbitMQ factory is configured, the channel should be "recoverable".
		((Recoverable) this.channel).addRecoveryListener( new RoboconfRecoveryListener());

		// Declare the exchanges.
		RabbitMqUtils.declareGlobalExchanges( this.domain, this.channel );
		RabbitMqUtils.declareApplicationExchanges( this.domain, this.applicationName, this.channel );

		// Declare the dedicated queue.
		String queueName = getQueueName();
		this.channel.queueDeclare( queueName, true, false, true, null );

		// Start listening to messages.
		RoboconfConsumer consumer = new RoboconfConsumer( getId(), this.channel, this.messageQueue );
		consumer.handleConsumeOk( queueName );
		this.consumerTag = this.channel.basicConsume( queueName, true, consumer );
		this.logger.finer( "A new consumer tag was created: " + this.consumerTag );
	}


	@Override
	public void closeConnection() throws IOException {

		StringBuilder sb = new StringBuilder( getId() + " is closing its connection to RabbitMQ." );
		if( this.channel != null )
			sb.append(" Channel # ").append(this.channel.getChannelNumber());

		this.logger.info( sb.toString());

		// Stop listening messages
		if( this.channel != null
				&& this.channel.isOpen()
				&& this.consumerTag != null ) {

			this.channel.basicCancel( this.consumerTag );
			this.logger.finer( "A consumer tag was cancelled: " + this.consumerTag );
		}

		// Close the connection
		this.consumerTag = null;
		if( isConnected()) {
			this.logger.finer( "Closing the connection and the channel # " + this.channel.getChannelNumber());
			RabbitMqUtils.closeConnection( this.channel );
		}

		this.channel = null;
	}


	@Override
	public void deleteMessagingServerArtifacts( Application application )
	throws IOException {

		// We delete the application exchanges. There is only one now.
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeNameForAgent( this.domain, application.getName()));
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeNameForTheDm( this.domain ));
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeNameForInterApp( this.domain ));
		this.logger.fine( "Messaging artifacts were deleted for application " + application );
		// Queues are deleted automatically by RabbitMQ.
	}


	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {

		// To which exchange?
		String exchangeName = RabbitMqUtils.buildExchangeName( ctx );

		// With which routing key?
		String routingKey = ctx.getTopicName();

		// Log a trace.
		this.logger.fine( "A message is about to be published to " + exchangeName + " with routing key = " + routingKey );

		// Special case for the DM sending to the DM.
		// To prevent spamming and residual messages, messages sent by the DM
		// (to itself or its siblings) have a life span of 500 ms. If there is no
		// client connected during this period, the message will be dropped.
		BasicProperties props = null;
		if( ctx.getKind() == RecipientKind.DM ) {
			props = new BasicProperties.Builder().expiration( "500" ).build();
		}

		// Do we want to be notified when a message is not delivered to anyone?
		// Yes, when the DM sends a message to an agent or when an agent sends a
		// message to the DM. If the message is not delivered to any queue,
		// the client will be notified (RoboconfReturnListener).
		boolean mandatory = false;
		if( this.ownerKind == RecipientKind.DM && this.ownerKind != ctx.getKind()
				|| this.ownerKind == RecipientKind.AGENTS && ctx.getKind() == RecipientKind.DM )
			mandatory = true;

		// Send the message.
		this.channel.basicPublish(
				exchangeName,		// The exchange name
				routingKey, 		// The routing key
				mandatory, 			// Mandatory => we want it to be delivered
				false,				// Useless, RabbitMQ does not support it for now.
				props,				// The publish properties
				SerializationUtils.serializeObject( msg ));
	}


	@Override
	public void subscribe( MessagingContext ctx ) throws IOException {

		// Subscribing means creating a routing key between an exchange and a queue.
		String exchangeName = RabbitMqUtils.buildExchangeName( ctx );
		String queueName = getQueueName();
		this.logger.fine( "Binding queue " + queueName + " and exchange " + exchangeName + " with routing key = " + ctx.getTopicName());
		this.channel.queueBind( queueName, exchangeName, ctx.getTopicName());
	}


	@Override
	public void unsubscribe( MessagingContext ctx ) throws IOException {

		// Un-subscribing means deleting a routing key between an exchange and a queue.
		String exchangeName = RabbitMqUtils.buildExchangeName( ctx );
		String queueName = getQueueName();
		this.logger.fine( "Unbinding queue " + queueName + " and exchange " + exchangeName + " with routing key = " + ctx.getTopicName());
		this.channel.queueUnbind( queueName, exchangeName, ctx.getTopicName());

	}


	String getQueueName() {

		StringBuilder queueName = new StringBuilder();
		queueName.append( this.domain );
		queueName.append( "." );
		if( this.ownerKind == RecipientKind.DM ) {
			queueName.append( "roboconf-dm" );

		} else {
			queueName.append( this.applicationName );
			queueName.append( "." );
			queueName.append( MessagingUtils.escapeInstancePath( this.scopedInstancePath ));
		}

		return queueName.toString();
	}


	String getId() {

		StringBuilder sb = new StringBuilder();
		sb.append( "[ " );
		sb.append( this.domain );
		sb.append( " ] " );

		if( this.ownerKind ==  RecipientKind.DM ) {
			sb.append( "DM" );
		} else {
			sb.append( this.scopedInstancePath );
			sb.append( " @ " );
			sb.append( this.applicationName );
		}

		return sb.toString();
	}
}
