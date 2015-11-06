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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.factory.IMessagingClientFactory;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

/**
 * Messaging client factory for Rabbit MQ.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class RabbitMqClientFactory implements IMessagingClientFactory {

	// The connection properties.
	private String messageServerIp;
	private String messageServerUsername;
	private String messageServerPassword;

	// The created clients.
	// References to the clients are *weak*, so we never prevent their garbage collection.
	private final Set<RabbitMqClient> clients = Collections.newSetFromMap(new WeakHashMap<RabbitMqClient, Boolean>());

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


	public void reconfigure() {
		// Set the properties for all created clients.
		resetClients(false);
	}


	public void stop() {
		resetClients(true);
	}


	private void resetClients(boolean shutdown) {
		// Make fresh snapshots of the clients, as we don't want to reconfigure them while holding the lock.
		final ArrayList<RabbitMqClient> clients;
		synchronized (this) {
			clients = new ArrayList<>(this.clients);
		}

		// Now reconfigure all the clients.
		for (RabbitMqClient client : clients) {
			try {
				final ReconfigurableClient<?> reconfigurable = client.getReconfigurableClient();
				if (reconfigurable != null)
					if (shutdown)
						reconfigurable.closeConnection();
					else
						reconfigurable.switchMessagingType(RabbitMqConstants.RABBITMQ_FACTORY_TYPE);

			} catch (Throwable t) {
				// Warn but continue to reconfigure the next clients!
				this.logger.warning("A client has thrown an exception on reconfiguration: " + client);
				Utils.logException(this.logger, new RuntimeException(t));
			}
		}
	}


	@Override
	public String getType() {
		return RabbitMqConstants.RABBITMQ_FACTORY_TYPE;
	}


	@Override
	public synchronized IDmClient createDmClient( final ReconfigurableClientDm parent ) {
		final RabbitMqClientDm client = new RabbitMqClientDm(parent, this.messageServerIp, this.messageServerUsername, this.messageServerPassword);
		this.clients.add(client);
		this.logger.finer("Created a new DM client");
		return client;
	}


	@Override
	public synchronized IAgentClient createAgentClient( final ReconfigurableClientAgent parent ) {
		final RabbitMqClientAgent client = new RabbitMqClientAgent(parent, this.messageServerIp, this.messageServerUsername, this.messageServerPassword);
		this.clients.add(client);
		this.logger.finer("Created a new Agent client");
		return client;
	}


	@Override
	public boolean setConfiguration( final Map<String, String> configuration ) {

		final boolean result;
		final String type = configuration.get(MESSAGING_TYPE_PROPERTY);
		if (RabbitMqConstants.RABBITMQ_FACTORY_TYPE.equals(type)) {
			String ip = configuration.get(RabbitMqConstants.RABBITMQ_SERVER_IP);
			String username = configuration.get(RabbitMqConstants.RABBITMQ_SERVER_USERNAME);
			String password = configuration.get(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD);

			// Handles default values.
			if (ip == null)
				ip = RabbitMqClient.DEFAULT_IP;
			if (username == null)
				username = RabbitMqClient.DEFAULT_USERNAME;
			if (password == null)
				password = RabbitMqClient.DEFAULT_PASSWORD;

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
			result = true;
		} else {

			// Not a configuration for this RabbitMQ client factory.
			result = false;
		}

		return result;
	}
}
