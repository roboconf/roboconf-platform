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

package net.roboconf.messaging.internal.utils;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.messages.Message;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqUtils {

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
		Instance rootInstance = InstanceHelpers.findRootInstance( instance );
		return buildRoutingKeyForAgent( rootInstance.getName());
	}


	/**
	 * Builds the routing key for an agent.
	 * @param rootInstanceName the name of the root instance associated with the agent
	 * @return a non-null string
	 */
	public static String buildRoutingKeyForAgent( String rootInstanceName ) {
		return "machine." + rootInstanceName;
	}


	/**
	 * Configures the connection factory with the right settings.
	 * @param factory the connection factory
	 * @param messageServerIp the message server IP (can contain a port)
	 * @throws IOException if something went wrong
	 */
	public static void configureFactory( ConnectionFactory factory, String messageServerIp ) throws IOException {

		Map.Entry<String,Integer> entry = findUrlAndPort( messageServerIp );
		factory.setHost( entry.getKey());
		if( entry.getValue() > 0 )
			factory.setPort( entry.getValue());
	}


	/**
	 * Parses a raw URL and extracts the host and port.
	 * @param messageServerIp a raw URL (here, the message server's IP)
	 * @return a non-null map entry (key = host URL without the port, value = the port, -1 if not specified)
	 */
	public static Map.Entry<String,Integer> findUrlAndPort( String messageServerIp ) {

		Matcher m = Pattern.compile( ".*(:\\d+).*" ).matcher( messageServerIp );
		String portAsString = m.find() ? m.group( 1 ).substring( 1 ) : null;
		Integer port = portAsString == null ? - 1 : Integer.parseInt( portAsString );
		String address = portAsString == null ? messageServerIp : messageServerIp.replace( m.group( 1 ), "" );

		return new AbstractMap.SimpleEntry<String,Integer>( address, port );
	}


	/**
	 * Closes the connection to a channel.
	 * @param channel the channel to close (can be null)
	 * @throws IOException if something went wrong
	 */
	public static void closeConnection( Channel channel ) throws IOException {

		if( channel != null ) {
			if( channel.isOpen()) {
				// channel.basicCancel( consumerTag );
				channel.close();
			}

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
	 * @param messageProcessor the message processor
	 */
	public static void listenToRabbitMq( String sourceName, Logger logger, QueueingConsumer consumer, AbstractMessageProcessor messageProcessor ) {

		// We listen to messages until the consumer is cancelled
		logger.fine( sourceName + " starts listening to new messages." );
		for( ;; ) {

			try {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				Message message = SerializationUtils.deserializeObject( delivery.getBody());

				StringBuilder sb = new StringBuilder();
				sb.append( sourceName );
				sb.append( " received a message " );
				sb.append( message.getClass().getSimpleName());
				sb.append( " on routing key '" );
				sb.append( delivery.getEnvelope().getRoutingKey());
				sb.append( "'." );
				logger.finer( sb.toString());

				messageProcessor.storeMessage( message );

			} catch( ShutdownSignalException e ) {
				logger.warning( sourceName + ": the message server is shutting down." );
				logger.finest( Utils.writeException( e ));
				break;

			} catch( ConsumerCancelledException e ) {
				logger.info( sourceName + " stops listening to new messages." );
				break;

			} catch( InterruptedException e ) {
				logger.finest( Utils.writeException( e ));
				break;

			} catch( ClassNotFoundException e ) {
				logger.severe( sourceName + ": a message could not be deserialized. Class cast exception." );
				logger.finest( Utils.writeException( e ));

			}  catch( IOException e ) {
				logger.severe( sourceName + ": a message could not be deserialized. I/O exception." );
				logger.finest( Utils.writeException( e ));
			}
		}
	}
}
