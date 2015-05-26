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

package net.roboconf.messaging.rabbitmq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.factory.MessagingClientFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Updated;

/**
 * Messaging client factory for Rabbit MQ.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
@Component(name = "roboconf-messaging-client-factory-rabbitmq", publicFactory = false,
		managedservice = "net.roboconf.messaging.rabbitmq")
@Provides(specifications = MessagingClientFactory.class,
		properties = @StaticServiceProperty(name = MessagingClientFactory.MESSAGING_TYPE_PROPERTY, type = "string",
				value = MessagingConstants.FACTORY_RABBIT_MQ))
@Instantiate(name = "Roboconf RabbitMQ Messaging Client Factory")
public class RabbitMqClientFactory implements MessagingClientFactory {

	// The connection properties.
	private String messageServerIp;
	private String messageServerUsername;
	private String messageServerPassword;

	// The created clients.
	// References to the clients are *weak*, so we never prevent their garbage collection.
	private final Set<RabbitMqClient> clients = Collections.newSetFromMap(new WeakHashMap<RabbitMqClient, Boolean>());

	// The logger
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@Property(name = MessagingConstants.RABBITMQ_SERVER_IP, value = RabbitMqClient.DEFAULT_IP)
	public void setMessageServerIp( final String messageServerIp ) {
		this.messageServerIp = messageServerIp;
		this.logger.finer("Server IP set to " + messageServerIp);
	}

	@Property(name = MessagingConstants.RABBITMQ_SERVER_USERNAME, value = RabbitMqClient.DEFAULT_USERNAME)
	public void setMessageServerUsername( final String messageServerUsername ) {
		this.messageServerUsername = messageServerUsername;
		this.logger.finer("Server username set to " + messageServerUsername);
	}

	@Property(name = MessagingConstants.RABBITMQ_SERVER_PASSWORD, value = RabbitMqClient.DEFAULT_PASSWORD)
	public void setMessageServerPassword( final String messageServerPassword ) {
		this.messageServerPassword = messageServerPassword;
		this.logger.finer("Server password set to " + messageServerPassword);
	}

	@Updated
	public void reconfigure() {
		// Set the properties for all created clients.

		// Make fresh snapshots of the clients, as we don't want to reconfigure them while holding the lock.
		final ArrayList<RabbitMqClient> clients;
		synchronized (this) {
			clients = new ArrayList<>(this.clients);
		}

		// Now reconfigure all the clients.
		for (RabbitMqClient client : clients) {
			try {
				client.setParameters(this.messageServerIp, this.messageServerUsername, this.messageServerPassword);
			} catch (Throwable t) {
				// Warn but continue to reconfigure the next clients!
				this.logger.warning("A client has thrown an exception on reconfiguration: " + client);
				Utils.logException(this.logger, new RuntimeException(t));
			}
		}
	}

	@Override
	public String getType() {
		return MessagingConstants.FACTORY_RABBIT_MQ;
	}

	@Override
	public synchronized IDmClient createDmClient() {
		final RabbitMqClientDm client = new RabbitMqClientDm();
		client.setParameters(this.messageServerIp, this.messageServerUsername, this.messageServerPassword);
		clients.add(client);
		this.logger.finer("Created a new DM client");
		return client;
	}

	@Override
	public synchronized IAgentClient createAgentClient() {
		final RabbitMqClientAgent client = new RabbitMqClientAgent();
		client.setParameters(this.messageServerIp, this.messageServerUsername, this.messageServerPassword);
		clients.add(client);
		this.logger.finer("Created a new Agent client");
		return client;
	}

}
