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

package net.roboconf.messaging.rabbitmq.internal.utils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.utils.SerializationUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

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
	 * Builds the exchange name for RabbitMQ.
	 * @param applicationName the application name
	 * @param dm true if we want the exchange name for the DM, false for the agents
	 * @return a non-null string
	 */
	public static String buildExchangeName( String applicationName, boolean dm ) {
		return applicationName + (dm ? ".admin" : ".agents" );
	}


	/**
	 * Builds the exchange name for RabbitMQ.
	 * @param application an application
	 * @param dm true if we want the exchange name for the DM, false for the agents
	 * @return a non-null string
	 */
	public static String buildExchangeName( Application application, boolean dm ) {
		return buildExchangeName( application.getName(), dm );
	}


	/**
	 * Builds the routing key for an agent.
	 * @param instance an instance managed by the agent
	 * @return a non-null string
	 */
	public static String buildRoutingKeyForAgent( Instance instance ) {
		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );
		return buildRoutingKeyForAgent( InstanceHelpers.computeInstancePath( scopedInstance ));
	}


	/**
	 * Builds the routing key for an agent.
	 * @param scopedInstancePath the path of the (scoped) instance associated with the agent
	 * @return a non-null string
	 */
	public static String buildRoutingKeyForAgent( String scopedInstancePath ) {
		return "machine." + escapeInstancePath( scopedInstancePath );
	}


	/**
	 * Removes unnecessary slashes and transforms the others into dots.
	 * @param instancePath a non-null instance path
	 * @return a non-null string
	 */
	public static String escapeInstancePath( String instancePath ) {
		return instancePath.replaceFirst( "^/*", "" ).replaceFirst( "/*$", "" ).replaceAll( "/+", "." );
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
	 * Declares the required exchanges for an application.
	 * <p>
	 * Every time the DM or an agent must send a message, we must be sure
	 * all the exchanges have been declared. Otherwise, it will result in
	 * an error in the client. And this error will close the channel.
	 * </p>
	 * <p>
	 * To PREVENT stupid errors, it is really important to declare
	 * both exchanges at once!
	 * </p>
	 *
	 * @param applicationName the application name
	 * @param channel the RabbitMQ channel
	 * @throws IOException if an error occurs
	 */
	public static void declareApplicationExchanges( String applicationName, Channel channel ) throws IOException {

		// Exchange declaration is idem-potent
		String dmExchangeName = buildExchangeName( applicationName, true );
		channel.exchangeDeclare( dmExchangeName, "fanout" );
		// "fanout" is a keyword for RabbitMQ.
		// It broadcasts all the messages to all the queues this exchange knows.

		String agentExchangeName = buildExchangeName( applicationName, false );
		channel.exchangeDeclare( agentExchangeName, "topic" );
		// "topic" is a keyword for RabbitMQ.

		// Also create the exchange for inter-application exchanges.
		channel.exchangeDeclare( MessagingContext.INTER_APP, "topic" );
		// "topic" is a keyword for RabbitMQ.
	}


	/**
	 * Listens to RabbitMQ messages.
	 * <p>
	 * Be careful, this method aims at avoiding duplicate code. It starts an
	 * (almost) infinite loop and should be used with caution.
	 * </p>
	 *
	 * @param sourceName the source name (DM, agent name...)
	 * @param logger the logger
	 * @param consumer the RabbitMQ consumer
	 * @param messages the list where to store the received messages
	 */
	public static void listenToRabbitMq( String sourceName, Logger logger, QueueingConsumer consumer, LinkedBlockingQueue<Message> messages ) {

		// We listen to messages until the consumer is cancelled
		logger.fine( sourceName + " starts listening to new messages." );
		for( ;; ) {

			try {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				Message message = SerializationUtils.deserializeObject(delivery.getBody());

				logger.finer(sourceName + " received a message " + message.getClass().getSimpleName()
						+ " on routing key '" + delivery.getEnvelope().getRoutingKey() + "'.");

				messages.add( message );

			} catch( ShutdownSignalException e ) {
				logger.fine( sourceName + ": the message server is shutting down." );
				break;

			} catch( ConsumerCancelledException e ) {
				logger.fine( sourceName + " stops listening to new messages." );
				break;

			} catch( InterruptedException e ) {
				Utils.logException( logger, e );
				break;

			} catch( ClassNotFoundException e ) {
				logger.severe( sourceName + ": a message could not be deserialized. Class cast exception." );
				Utils.logException( logger, e );

			}  catch( IOException e ) {
				logger.severe( sourceName + ": a message could not be deserialized. I/O exception." );
				Utils.logException( logger, e );
			}
		}

		logger.fine( "A message listening thread is now stopped." );
	}
}
