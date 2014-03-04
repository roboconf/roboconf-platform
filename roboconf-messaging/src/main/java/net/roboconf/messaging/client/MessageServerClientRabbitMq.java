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

package net.roboconf.messaging.client;

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.utils.MessagingUtils;
import net.roboconf.messaging.utils.SerializationUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * @author Noël - LIG
 */
public final class MessageServerClientRabbitMq implements IMessageServerClient {

	private Connection connection;
	private Channel	channel;

	private final String messageServerIp;
	private final String applicationName;
	private final Logger logger = Logger.getLogger( MessagingConstants.ROBOCONF_LOGGER_NAME );


	/**
	 * Constructor.
	 * @param messageServerIp
	 * @param applicationName
	 */
	public MessageServerClientRabbitMq( String messageServerIp, String applicationName ) {
		this.messageServerIp = messageServerIp;
		this.applicationName = applicationName;
	}


	@Override
	public String getMessageServerIp() {
		return this.messageServerIp;
	}


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
	public void declareChannel( String rootInstanceName ) {
		try {
			String queueName = MessagingUtils.buildMachineQueueName( this.applicationName, rootInstanceName );
			this.channel.queueDeclare( queueName, true, false, true, null );

		} catch( IOException e ) {
			this.logger.severe( "A channel could not be created on the message server for the application " + this.applicationName + " and the root instance " + rootInstanceName + "." );
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void sendMessage( Message message, Instance rootInstance ) {
		try {
			String queueName = MessagingUtils.buildMachineQueueName( this.applicationName, rootInstance.getName());
			this.channel.basicPublish( "", queueName, null, SerializationUtils.serializeObject( message ));
			this.logger.fine( "A message was sent to " + rootInstance.getName() + " through the channel " + queueName + "." );

		} catch( IOException e ) {
			this.logger.severe( "A message could not be published on the message server for the application " + this.applicationName + " and the root instance " + rootInstance.getName() + "." );
			this.logger.finest( Utils.writeException( e ));
		}
	}
}
