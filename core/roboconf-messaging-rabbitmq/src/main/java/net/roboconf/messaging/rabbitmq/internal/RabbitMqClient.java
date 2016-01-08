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
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

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
import net.roboconf.messaging.rabbitmq.internal.utils.ListeningThread;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqUtils;
import net.roboconf.messaging.rabbitmq.internal.utils.RoboconfReturnListener;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Common RabbitMQ client-related stuffs.
 * @author Pierre Bourret - Université Joseph Fourier
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqClient implements IMessagingClient {

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private final String messageServerIp, messageServerUsername, messageServerPassword;
	private final WeakReference<ReconfigurableClient<?>> reconfigurable;

	private LinkedBlockingQueue<Message> messageQueue;
	private RecipientKind ownerKind;
	private String applicationName, scopedInstancePath;

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
	public void setOwnerProperties( RecipientKind ownerKind, String applicationName, String scopedInstancePath ) {

		this.ownerKind = ownerKind;
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;

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

		// Declare the exchanges.
		RabbitMqUtils.declareGlobalExchanges( this.channel );
		RabbitMqUtils.declareApplicationExchanges( this.applicationName, this.channel );

		// Declare the dedicated queue.
		String queueName = getQueueName();
		this.channel.queueDeclare( queueName, true, false, true, null );

		// Start listening to messages.
		QueueingConsumer consumer = new QueueingConsumer( this.channel );
		this.consumerTag = this.channel.basicConsume( queueName, true, consumer );

		String threadName = "Roboconf - Queue listener for " + getId();
		new ListeningThread( threadName, this.logger, consumer, this.messageQueue, getId()).start();
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
				&& this.consumerTag != null )
			this.channel.basicCancel( this.consumerTag );

		// Close the connection
		this.consumerTag = null;
		if( isConnected())
			RabbitMqUtils.closeConnection( this.channel );

		this.channel = null;
	}


	@Override
	public void deleteMessagingServerArtifacts( Application application )
	throws IOException {

		// We delete the application exchanges. There is only one now.
		this.channel.exchangeDelete( RabbitMqUtils.buildExchangeNameForAgent( application.getName()));
		this.logger.fine( "Messaging artifacts were deleted for application " + application );
		// Queues are deleted automatically by RabbitMQ.
		// Global exchanges do not need to be deleted. There are only 2.
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

		String queueName;
		if( this.ownerKind == RecipientKind.DM )
			queueName = "roboconf.queue.dm";
		else
			queueName = this.applicationName + "." + MessagingUtils.escapeInstancePath( this.scopedInstancePath );

		return queueName;
	}


	String getId() {
		return this.ownerKind ==  RecipientKind.DM ? "DM" : this.scopedInstancePath + " @ " + this.applicationName;
	}
}
