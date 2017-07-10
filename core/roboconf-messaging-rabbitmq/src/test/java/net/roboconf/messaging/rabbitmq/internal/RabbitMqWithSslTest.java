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

import java.util.Map;

import org.junit.Ignore;

import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

/**
 * Test messaging with RabbitMQ over SSL.
 * <p>
 * User data do not matter here as it only tests messaging clients.
 * There is no real agent behind.
 * </p>
 * <p>
 * These tests should be run locally and are disabled by default.
 * </p>
 * <p>
 * Clone https://github.com/roboconf/rabbitmq-with-ssl-in-docker and
 * follow the instructions to launch a Docker container with RabbitMQ and
 * SSL configuration. These tests natively run with such a container.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@Ignore
public class RabbitMqWithSslTest extends RabbitMqTest {

	@Override
	protected Map<String,String> getMessagingConfiguration() {

		Map<String,String> configuration = super.getMessagingConfiguration();
		configuration.put( RabbitMqConstants.RABBITMQ_USE_SSL, "true" );
		configuration.put( RabbitMqConstants.RABBITMQ_SSL_AS_USER_DATA, "false" );

		// Use default values
		// configuration.put( RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_TYPE, null );
		// configuration.put( RabbitMqConstants.RABBITMQ_SSL_TRUST_MNGR_FACTORY, null );
		// configuration.put( RabbitMqConstants.RABBITMQ_SSL_KEY_MNGR_FACTORY, null );
		// configuration.put( RabbitMqConstants.RABBITMQ_SSL_PROTOCOL, null );

		configuration.put( RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PASSPHRASE, "roboconf" );
		configuration.put( RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PASSPHRASE, "roboconf" );

		configuration.put( RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_TYPE, RabbitMqConstants.DEFAULT_SSL_KEY_STORE_TYPE );
		configuration.put( RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PATH, "/tmp/docker-test/trust-store.p12" );
		configuration.put( RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PATH, "/tmp/docker-test/key-store.p12" );

		// We do not need a user name and a password.
		// configuration.put( RabbitMqConstants.RABBITMQ_SERVER_USERNAME, "roboconf" );
		// configuration.put( RabbitMqConstants.RABBITMQ_SERVER_PASSWORD, "roboconf" );
		configuration.put( RabbitMqConstants.RABBITMQ_SERVER_IP, "127.0.0.1:12000" );

		return configuration;
	}
}
