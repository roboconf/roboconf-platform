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

import net.roboconf.messaging.api.MessagingConstants;

/**
 * Constants related to the RabbitMQ messaging client factory.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public final class RabbitMqConstants {

	/**
	 * Constructor.
	 */
	private RabbitMqConstants() {
		// nothing
	}

	/**
	 * The factory's name for RabbitMQ clients.
	 */
	public static final String RABBITMQ_FACTORY_TYPE = "rabbitmq";

	/**
	 * The prefix for all RabbitMQ-related properties.
	 */
	private static final String RABBITMQ_PROPERTY_PREFIX = MessagingConstants.MESSAGING_PROPERTY_PREFIX + "." + RABBITMQ_FACTORY_TYPE;

	/**
	 * Messaging property holding the RabbitMQ server IP (or host). Defaults to {@code "localhost"}.
	 */
	public static final String RABBITMQ_SERVER_IP = RABBITMQ_PROPERTY_PREFIX + ".server.ip";

	/**
	 * Messaging property holding the RabbitMQ server username. Defaults to {@code "guest"}.
	 */
	public static final String RABBITMQ_SERVER_USERNAME = RABBITMQ_PROPERTY_PREFIX + ".server.username";

	/**
	 * Messaging property holding the RabbitMQ server password. Defaults to {@code "guest"}.
	 */
	public static final String RABBITMQ_SERVER_PASSWORD = RABBITMQ_PROPERTY_PREFIX + ".server.password";
}
