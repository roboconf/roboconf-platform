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

import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.DEFAULT_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.FACTORY_RABBITMQ;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.GUEST;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_PASSWORD;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_USERNAME;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_USE_SSL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

/**
 * Messaging client factory for Rabbit MQ.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class RabbitMqClientFactory implements IMessagingClientFactory {

	// The connection properties.
	Map<String,String> configuration = Collections.emptyMap();

	// The created clients.
	// References to the clients are *weak*, so we never prevent their garbage collection.
	final Set<RabbitMqClient> clients = Collections.newSetFromMap( new WeakHashMap<RabbitMqClient,Boolean> ());

	// The logger
	private final Logger logger = Logger.getLogger( this.getClass().getName());



	/**
	 * Reconfigures the client.
	 */
	public void reconfigure() {
		this.logger.fine( "Rabbit MQ clients are about to be reconfigured." );

		// Set the properties for all created clients.
		resetClients( false );
	}


	/**
	 * Stops the client (invoked by iPojo).
	 */
	public void stop() {
		resetClients(true);
	}


	private void resetClients( boolean shutdown ) {

		// Make fresh snapshots of the clients, as we don't want to reconfigure them while holding the lock.
		final ArrayList<RabbitMqClient> clients;
		synchronized( this ) {

			// Get the snapshot.
			clients = new ArrayList<>( this.clients );

			// Remove the clients, new ones will be created if necessary.
			this.clients.clear();
		}

		// Now reconfigure all the clients.
		for( RabbitMqClient client : clients ) {
			try {
				final ReconfigurableClient<?> reconfigurable = client.getReconfigurableClient();

				// The reconfigurable can never be null.
				// If it was, a NPE would have been thrown when the Rabbit MQ client was created.
				if( shutdown )
					reconfigurable.closeConnection();
				else
					reconfigurable.switchMessagingType( RabbitMqConstants.FACTORY_RABBITMQ );

			} catch( Throwable t ) {
				// Warn but continue to reconfigure the next clients!
				this.logger.warning("A client has thrown an exception on reconfiguration: " + client);
				Utils.logException(this.logger, new RuntimeException(t));
			}
		}
	}


	@Override
	public String getType() {
		return RabbitMqConstants.FACTORY_RABBITMQ;
	}


	@Override
	public IMessagingClient createClient( final ReconfigurableClient<?> parent ) {
		this.logger.fine( "Creating a new Rabbit MQ client with owner = " + parent.getOwnerKind());

		// The parent cannot be null. A NPE MUST be thrown otherwise.
		// That's what the RabbitMqClient constructor does. There is unit test for this.
		final RabbitMqClient client = new RabbitMqClient( parent, this.configuration );
		synchronized( this ) {
			this.clients.add( client );
		}

		this.logger.finer( "A new Rabbit MQ client was created." );
		return client;
	}


	/**
	 * Invoked by iPojo when one or several properties were updated from Config Admin.
	 * @param properties
	 */
	public void setConfiguration( Dictionary<?,?> properties ) {

		// Ignore iPojo properties
		final List<String> propertiesToSkip = Arrays.asList(
				"component",
				"felix.fileinstall.filename" );

		// Convert the dictionary into a map
		Map<String,String> map = new LinkedHashMap<> ();
		for( Enumeration<?> en = properties.keys(); en.hasMoreElements(); ) {

			Object key = en.nextElement();
			String keyAsString = String.valueOf( key );
			if( propertiesToSkip.contains( keyAsString ))
				continue;

			// "null" are not acceptable values in dictionaries
			// (OSGi often use Hash tables)
			Object value = properties.get( key );
			map.put( keyAsString, String.valueOf( value ));
		}

		// Invoked by iPojo => the messaging type is the right one in this method
		map.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, FACTORY_RABBITMQ );
		setConfiguration( map );
	}


	@Override
	public boolean setConfiguration( final Map<String,String> configuration ) {

		boolean result = false;
		final String type = configuration.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ) ;
		if(( result = FACTORY_RABBITMQ.equals( type ))) {

			Map<String,String> configurationWithDefaults = new LinkedHashMap<> ();
			configurationWithDefaults.put( RABBITMQ_SERVER_IP, DEFAULT_IP );
			configurationWithDefaults.put( RABBITMQ_SERVER_USERNAME, GUEST );
			configurationWithDefaults.put( RABBITMQ_SERVER_PASSWORD, GUEST );
			configurationWithDefaults.put( RABBITMQ_USE_SSL, Boolean.FALSE.toString());
			configurationWithDefaults.putAll( configuration );

			// Avoid unnecessary (and potentially problematic) reconfiguration if nothing has changed.
			// First we detect for changes, and set the parameters accordingly.
			boolean hasChanged = false;
			synchronized( this ) {

				for( Map.Entry<String,String> configEntry : configurationWithDefaults.entrySet()) {
					String value = this.configuration.get( configEntry.getKey());
					if( ! Objects.equals( value, configEntry.getValue())) {
						hasChanged = true;
						break;
					}
				}

				if( hasChanged ) {
					this.configuration = configurationWithDefaults;
					this.logger.finer( "Updating the messaging properties." );
				}
			}

			// Then, if changes has occurred, we reconfigure the factory. This will invalidate every created client.
			// Otherwise, if nothing has changed, we do nothing. Thus we avoid invalidating clients uselessly, and
			// prevent any message loss.
			if( hasChanged )
				reconfigure();
		}

		return result;
	}
}
