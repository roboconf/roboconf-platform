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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.client.IClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

import com.rabbitmq.client.Channel;

/**
 * Common RabbitMQ client-related stuffs.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public abstract class RabbitMqClient implements IClient {

	/**
	 * The default value of the IP address property.
	 * <p>
	 * By default, the client assumes the RabbitMQ server is hosted on {@code localhost}, with the default port.
	 * </p>
	 */
	static final String DEFAULT_IP = "localhost";

	/**
	 * The default value of the username property.
	 * <p>
	 * By default, the client connects as {@code guest}, the default account only available locally.
	 * </p>
	 */
	static final String DEFAULT_USERNAME = "guest";

	/**
	 * The default value of the password property.
	 * <p>
	 * By default, the client connects as {@code guest}, the default account only available locally. This property
	 * holds
	 * the default password for this default account.
	 * </p>
	 */
	static final String DEFAULT_PASSWORD = "guest";


	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	protected final String messageServerIp, messageServerUsername, messageServerPassword;
	private final WeakReference<ReconfigurableClient<?>> reconfigurable;
	protected LinkedBlockingQueue<Message> messageQueue;
	Channel channel;


	/**
	 * Constructor.
	 * @param reconfigurable
	 * @param ip
	 * @param username
	 * @param password
	 */
	protected RabbitMqClient( ReconfigurableClient<?> reconfigurable, String ip, String username, String password ) {
		this.reconfigurable = new WeakReference<ReconfigurableClient<?>>(reconfigurable);
		this.messageServerIp = ip;
		this.messageServerUsername = username;
		this.messageServerPassword = password;
	}


	/**
	 * @return the wrapping reconfigurable client (may be {@code null}).
	 */
	public final ReconfigurableClient<?> getReconfigurableClient() {
		return this.reconfigurable.get();
	}


	@Override
	public final synchronized void setMessageQueue( LinkedBlockingQueue<Message> messageQueue ) {
		this.messageQueue = messageQueue;
	}


	@Override
	public final synchronized boolean isConnected() {
		return this.channel != null;
	}


	@Override
	public final String getMessagingType() {
		return RabbitMqConstants.RABBITMQ_FACTORY_TYPE;
	}


	@Override
	public final Map<String, String> getConfiguration() {
		final Map<String, String> configuration = new LinkedHashMap<>();
		configuration.put(MessagingConstants.MESSAGING_TYPE_PROPERTY, RabbitMqConstants.RABBITMQ_FACTORY_TYPE);
		configuration.put(RabbitMqConstants.RABBITMQ_SERVER_IP, this.messageServerIp);
		configuration.put(RabbitMqConstants.RABBITMQ_SERVER_USERNAME, this.messageServerUsername);
		configuration.put(RabbitMqConstants.RABBITMQ_SERVER_PASSWORD, this.messageServerPassword);
		return Collections.unmodifiableMap(configuration);
	}
}
