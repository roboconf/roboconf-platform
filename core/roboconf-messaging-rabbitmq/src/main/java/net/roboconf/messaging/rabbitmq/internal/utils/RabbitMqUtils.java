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

package net.roboconf.messaging.rabbitmq.internal.utils;

import static net.roboconf.core.utils.Utils.getValue;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.DEFAULT_SSL_KEY_STORE_TYPE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.DEFAULT_SSL_MNGR_FACTORY;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.DEFAULT_SSL_PROTOCOL;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.DEFAULT_SSL_TRUST_STORE_TYPE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_PASSWORD;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_USERNAME;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_KEY_MNGR_FACTORY;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PASSPHRASE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PATH;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_TYPE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_PROTOCOL;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_MNGR_FACTORY;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PASSPHRASE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PATH;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_TYPE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_USE_SSL;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

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
	 * @param configuration the messaging configuration
	 * @throws IOException if something went wrong
	 * @see RabbitMqConstants
	 */
	public static void configureFactory( ConnectionFactory factory, Map<String,String> configuration )
	throws IOException {

		final Logger logger = Logger.getLogger( RabbitMqUtils.class.getName());
		logger.fine( "Configuring a connection factory for RabbitMQ." );

		String messageServerIp = configuration.get( RABBITMQ_SERVER_IP );
		if( messageServerIp != null ) {
			Map.Entry<String,Integer> entry = Utils.findUrlAndPort( messageServerIp );
			factory.setHost( entry.getKey());
			if( entry.getValue() > 0 )
				factory.setPort( entry.getValue());
		}

		factory.setUsername( configuration.get( RABBITMQ_SERVER_USERNAME ));
		factory.setPassword( configuration.get( RABBITMQ_SERVER_PASSWORD ));

		// Timeout for connection establishment: 5s
		factory.setConnectionTimeout( 5000 );

		// Configure automatic reconnection
		factory.setAutomaticRecoveryEnabled( true );

		// Recovery interval: 10s
		factory.setNetworkRecoveryInterval( 10000 );

		// Exchanges and so on should be redeclared if necessary
		factory.setTopologyRecoveryEnabled( true );

		// SSL
		if( Boolean.parseBoolean( configuration.get( RABBITMQ_USE_SSL ))) {
			logger.fine( "Connection factory for RabbitMQ: SSL is used." );

			InputStream clientIS = null;
			InputStream storeIS = null;
			try {
				clientIS = new FileInputStream( configuration.get( RABBITMQ_SSL_KEY_STORE_PATH ));
				storeIS = new FileInputStream( configuration.get( RABBITMQ_SSL_TRUST_STORE_PATH ));

				char[] keyStorePassphrase = configuration.get( RABBITMQ_SSL_KEY_STORE_PASSPHRASE ).toCharArray();
				KeyStore ks = KeyStore.getInstance( getValue( configuration, RABBITMQ_SSL_KEY_STORE_TYPE, DEFAULT_SSL_KEY_STORE_TYPE ));
				ks.load( clientIS, keyStorePassphrase );

				String value = getValue( configuration, RABBITMQ_SSL_KEY_MNGR_FACTORY, DEFAULT_SSL_MNGR_FACTORY );
				KeyManagerFactory kmf = KeyManagerFactory.getInstance( value );
				kmf.init( ks, keyStorePassphrase );

				char[] trustStorePassphrase = configuration.get( RABBITMQ_SSL_TRUST_STORE_PASSPHRASE ).toCharArray();
				KeyStore tks = KeyStore.getInstance( getValue( configuration, RABBITMQ_SSL_TRUST_STORE_TYPE, DEFAULT_SSL_TRUST_STORE_TYPE ));
				tks.load( storeIS, trustStorePassphrase );

				value = getValue( configuration, RABBITMQ_SSL_TRUST_MNGR_FACTORY, DEFAULT_SSL_MNGR_FACTORY );
				TrustManagerFactory tmf = TrustManagerFactory.getInstance( value );
				tmf.init( tks );

				SSLContext c = SSLContext.getInstance( getValue( configuration, RABBITMQ_SSL_PROTOCOL, DEFAULT_SSL_PROTOCOL ));
				c.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
				factory.useSslProtocol( c );

			} catch( GeneralSecurityException e ) {
				throw new IOException( "SSL configuration for the RabbitMQ factory failed.", e );

			} finally {
				Utils.closeQuietly( storeIS );
				Utils.closeQuietly( clientIS );
			}
		}
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
	 * @param domain the domain name
	 * @param applicationName the application name
	 * @param channel the RabbitMQ channel
	 * @throws IOException if an error occurs
	 */
	public static void declareApplicationExchanges( String domain, String applicationName, Channel channel )
	throws IOException {

		// "topic" is a keyword for RabbitMQ.
		if( applicationName != null ) {
			String exch = buildExchangeNameForAgent( domain, applicationName );
			channel.exchangeDeclare( exch, "topic" );
		}
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
	public static void declareGlobalExchanges( String domain, Channel channel )
	throws IOException {

		// "topic" is a keyword for RabbitMQ.
		channel.exchangeDeclare( buildExchangeNameForTheDm( domain ), "topic" );
		channel.exchangeDeclare( buildExchangeNameForInterApp( domain ), "topic" );
	}


	/**
	 * Builds the name of an exchange for agents (related to the application name).
	 * @param domain the domain
	 * @param applicationName the application name
	 * @return a non-null string
	 */
	public static String buildExchangeNameForAgent( String domain, String applicationName ) {
		return domain + "." + applicationName + ".agents";
	}


	/**
	 * Builds the name of the exchange for the DM.
	 * @param domain the domain
	 * @return a non-null string
	 */
	public static String buildExchangeNameForTheDm( String domain ) {
		return domain + "." + RabbitMqConstants.EXCHANGE_DM;
	}


	/**
	 * Builds the name of the exchange for inter-application exchanges.
	 * @param domain the domain
	 * @return a non-null string
	 */
	public static String buildExchangeNameForInterApp( String domain ) {
		return domain + "." + RabbitMqConstants.EXCHANGE_INTER_APP;
	}


	/**
	 * Builds an exchange name from a messaging context.
	 * @param ctx a non-null context
	 * @return a non-null string
	 */
	public static String buildExchangeName( MessagingContext ctx ) {

		String exchangeName;
		if( ctx.getKind() == RecipientKind.DM )
			exchangeName = buildExchangeNameForTheDm( ctx.getDomain());
		else if( ctx.getKind() == RecipientKind.INTER_APP )
			exchangeName = buildExchangeNameForInterApp( ctx.getDomain());
		else
			exchangeName = RabbitMqUtils.buildExchangeNameForAgent( ctx.getDomain(), ctx.getApplicationName());

		return exchangeName;
	}
}
