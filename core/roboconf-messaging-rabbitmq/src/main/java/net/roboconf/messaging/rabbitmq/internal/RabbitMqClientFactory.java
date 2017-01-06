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
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.GUEST;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_PASSWORD;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_USERNAME;

import java.util.ArrayList;
import java.util.Collections;
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
	String messageServerIp;
	String messageServerUsername;
	String messageServerPassword;

	// The created clients.
	// References to the clients are *weak*, so we never prevent their garbage collection.
	final Set<RabbitMqClient> clients = Collections.newSetFromMap( new WeakHashMap<RabbitMqClient,Boolean> ());

	// The logger
	private final Logger logger = Logger.getLogger(this.getClass().getName());



	public synchronized void setMessageServerIp( final String messageServerIp ) {
		this.messageServerIp = messageServerIp;
		this.logger.finer("Server IP set to " + messageServerIp);
	}


	public synchronized void setMessageServerUsername( final String messageServerUsername ) {
		this.messageServerUsername = messageServerUsername;
		this.logger.finer("Server username set to " + messageServerUsername);
	}


	public synchronized void setMessageServerPassword( final String messageServerPassword ) {
		this.messageServerPassword = messageServerPassword;
		this.logger.finer("Server password set to " + messageServerPassword);
	}


	/**
	 * Reconfigures the client (invoked by iPojo).
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
		synchronized (this) {

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

			} catch (Throwable t) {
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
		final RabbitMqClient client = new RabbitMqClient( parent, this.messageServerIp, this.messageServerUsername, this.messageServerPassword );
		synchronized( this ) {
			this.clients.add( client );
		}

		this.logger.finer( "A new Rabbit MQ client was created." );
		return client;
	}


	@Override
	public boolean setConfiguration( final Map<String, String> configuration ) {

		boolean result = false;;
		final String type = configuration.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ) ;
		if(( result = RabbitMqConstants.FACTORY_RABBITMQ.equals( type ))) {

			// Get the new values
			String ip = Utils.getValue( configuration, RABBITMQ_SERVER_IP, DEFAULT_IP );
			String username = Utils.getValue( configuration, RABBITMQ_SERVER_USERNAME, GUEST );
			String password = Utils.getValue( configuration, RABBITMQ_SERVER_PASSWORD, GUEST );

			// Avoid unnecessary (and potentially problematic) reconfiguration if nothing has changed.
			// First we detect for changes, and set the parameters accordingly.
			boolean hasChanged = false;
			synchronized (this) {

				if (!Objects.equals(this.messageServerIp, ip)) {
					setMessageServerIp(ip);
					hasChanged = true;
				}

				if (!Objects.equals(this.messageServerUsername, username)) {
					setMessageServerUsername(username);
					hasChanged = true;
				}

				if (!Objects.equals(this.messageServerPassword, password)) {
					setMessageServerPassword(password);
					hasChanged = true;
				}
			}

			// Then, if changes has occurred, we reconfigure the factory. This will invalidate every created client.
			// Otherwise, if nothing has changed, we do nothing. Thus we avoid invalidating clients uselessly, and
			// prevent any message loss.
			if (hasChanged)
				reconfigure();
		}

		return result;
	}
}
