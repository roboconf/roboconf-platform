/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.rabbitmq.internal.utils;

import java.io.IOException;
import java.util.Map;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class RabbitMqUtils {

	/**
	 * Constructor.
	 */
	private RabbitMqUtils() {
		// nothing
	}


	/**
	 * Configures the connection factory with the right settings.
	 * @param factory the connection factory
	 * @param messageServerIp the message server IP (can contain a port and can but null too)
	 * @param messageServerUsername the user name for the message server
	 * @param messageServerPassword the password for the message server
	 * @throws IOException if something went wrong
	 */
	public static void configureFactory( ConnectionFactory factory, String messageServerIp, String messageServerUsername, String messageServerPassword )
	throws IOException {

		if( messageServerIp != null ) {
			Map.Entry<String,Integer> entry = Utils.findUrlAndPort( messageServerIp );
			factory.setHost( entry.getKey());
			if( entry.getValue() > 0 )
				factory.setPort( entry.getValue());
		}

		factory.setUsername( messageServerUsername );
		factory.setPassword( messageServerPassword );

		// Timeout for connection establishment: 5s
		factory.setConnectionTimeout( 5000 );

		// Configure automatic reconnections
		factory.setAutomaticRecoveryEnabled( true );

		// Recovery interval: 10s
		factory.setNetworkRecoveryInterval( 10000 );

		// Exchanges and so on should be redeclared if necessary
		factory.setTopologyRecoveryEnabled( true );
	}


	/**
	 * Closes the connection to a channel.
	 * @param channel the channel to close (can be null)
	 * @throws IOException if something went wrong
	 */
	public static void closeConnection( Channel channel ) throws IOException {

		if( channel != null ) {
			if( channel.isOpen())
				channel.close();

			if( channel.getConnection().isOpen())
				channel.getConnection().close();
		}
	}


	/**
	 * Declares the required exchanges for an application (only for agents).
	 * @param applicationName the application name
	 * @param channel the RabbitMQ channel
	 * @throws IOException if an error occurs
	 */
	public static void declareApplicationExchanges( String applicationName, Channel channel ) throws IOException {

		// "topic" is a keyword for RabbitMQ.
		if( applicationName != null )
			channel.exchangeDeclare( buildExchangeNameForAgent( applicationName ), "topic" );
	}


	/**
	 * Declares the global exchanges (those that do not depend on an application).
	 * <p>
	 * It includes the DM exchange and the one for inter-application exchanges.
	 * </p>
	 *
	 * @param channel the RabbitMQ channel
	 * @throws IOException if an error occurs
	 */
	public static void declareGlobalExchanges( Channel channel ) throws IOException {

		// "topic" is a keyword for RabbitMQ.
		channel.exchangeDeclare( RabbitMqConstants.EXHANGE_DM, "topic" );
		channel.exchangeDeclare( RabbitMqConstants.EXHANGE_INTER_APP, "topic" );

	}


	/**
	 * Builds the name of an exchange for agents (related to the application name).
	 * @param applicationName the application name
	 * @return a non-null string
	 */
	public static String buildExchangeNameForAgent( String applicationName ) {
		return applicationName + ".agents";
	}


	/**
	 * Builds an exchange name from a messaging context.
	 * @param ctx a non-null context
	 * @return a non-null string
	 */
	public static String buildExchangeName( MessagingContext ctx ) {

		String exchangeName;
		if( ctx.getKind() == RecipientKind.DM )
			exchangeName = RabbitMqConstants.EXHANGE_DM;
		else if( ctx.getKind() == RecipientKind.INTER_APP )
			exchangeName = RabbitMqConstants.EXHANGE_INTER_APP;
		else
			exchangeName = RabbitMqUtils.buildExchangeNameForAgent( ctx.getApplicationName());

		return exchangeName;
	}
}
