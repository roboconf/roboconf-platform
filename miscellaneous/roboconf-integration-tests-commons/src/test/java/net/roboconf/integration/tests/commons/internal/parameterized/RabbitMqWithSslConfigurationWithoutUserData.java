/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.tests.commons.internal.parameterized;

import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.DEFAULT_SSL_KEY_STORE_TYPE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.FACTORY_RABBITMQ;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_AS_USER_DATA;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PASSPHRASE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PATH;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PASSPHRASE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PATH;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_TYPE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_USE_SSL;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.util.ArrayList;
import java.util.List;

import org.ops4j.pax.exam.Option;

import net.roboconf.core.Constants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqWithSslConfigurationWithoutUserData implements IMessagingConfiguration {

	@Override
	public List<Option> options() {

		// The DM and agents use RabbitMQ
		List<Option> options = new ArrayList<> ();
		options.add( editConfigurationFilePut(
				"etc/" + Constants.KARAF_CFG_FILE_AGENT,
				Constants.MESSAGING_TYPE,
				FACTORY_RABBITMQ ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.dm.configuration.cfg",
				Constants.MESSAGING_TYPE,
				FACTORY_RABBITMQ ));

		// Update the configuration for RabbitMQ clients
		// (taken from net.roboconf.messaging.rabbitmq.internal.RabbitMqTestWithSsl)
		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				RABBITMQ_USE_SSL,
				"true" ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				RABBITMQ_SSL_AS_USER_DATA,
				"false" ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				RABBITMQ_SSL_KEY_STORE_PASSPHRASE,
				"roboconf" ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				RABBITMQ_SSL_TRUST_STORE_PASSPHRASE,
				"roboconf" ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				RABBITMQ_SSL_TRUST_STORE_TYPE,
				DEFAULT_SSL_KEY_STORE_TYPE ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				RABBITMQ_SSL_TRUST_STORE_PATH,
				"/tmp/docker-test/trust-store.p12" ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				RABBITMQ_SSL_KEY_STORE_PATH,
				"/tmp/docker-test/key-store.p12" ));

		options.add( editConfigurationFilePut(
				"etc/net.roboconf.messaging.rabbitmq.cfg",
				RABBITMQ_SERVER_IP,
				"127.0.0.1:12000" ));

		return options;
	}
}
