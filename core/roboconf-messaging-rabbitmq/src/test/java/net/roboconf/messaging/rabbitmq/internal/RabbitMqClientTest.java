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

import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.FACTORY_RABBITMQ;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_PASSWORD;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_USERNAME;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_AS_USER_DATA;
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Channel;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqClientTest {

	private static boolean rabbitMqIsRunning = false;

	@BeforeClass
	public static void checkRabbitMqIsRunning() throws Exception {
		rabbitMqIsRunning = RabbitMqTestUtils.checkRabbitMqIsRunning();
	}


	@Test
	public void testConnectAndDisconnect() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "localhost" );
		configuration.put( RABBITMQ_SERVER_USERNAME, "guest" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "guest" );

		RabbitMqClient client = new RabbitMqClient( null, configuration, RecipientKind.DM );
		client.setOwnerProperties( RecipientKind.DM, "domain", "app", "/root" );

		Assert.assertEquals( RabbitMqConstants.FACTORY_RABBITMQ, client.getMessagingType());
		Assert.assertFalse( client.isConnected());
		Assert.assertNull( client.channel );

		RoboconfMessageQueue messagesQueue = new RoboconfMessageQueue();
		client.setMessageQueue( messagesQueue );
		client.openConnection();
		Assert.assertNotNull( client.channel );
		Assert.assertNotNull( client.consumerTag );
		Assert.assertTrue( client.isConnected());

		// openConnection is idem-potent
		Channel oldChannel = client.channel;
		client.openConnection();
		Assert.assertEquals( oldChannel, client.channel );

		// Delete server artifacts
		client.deleteMessagingServerArtifacts( new Application( "app", new ApplicationTemplate()));

		client.closeConnection();
		Assert.assertNull( client.channel );
		Assert.assertNull( client.consumerTag );

		// closeConnection is idem-potent
		client.closeConnection();
		Assert.assertNull( client.channel );
	}


	@Test
	public void testGetQueueName() throws Exception {

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "localhost" );
		configuration.put( RABBITMQ_SERVER_USERNAME, "guest" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "guest" );

		RabbitMqClient client = new RabbitMqClient( null, configuration, RecipientKind.DM );
		client.setOwnerProperties( RecipientKind.DM, "domain", "app", "/root" );
		Assert.assertEquals( "domain.roboconf-dm", client.getQueueName());

		client.setOwnerProperties( RecipientKind.AGENTS, "domain", "app", "/root" );
		Assert.assertEquals( "domain.app.root", client.getQueueName());

		client.setOwnerProperties( RecipientKind.AGENTS, "domain1", "app", "/root" );
		Assert.assertEquals( "domain1.app.root", client.getQueueName());
	}


	@Test
	public void testFilteringOfSslProperties() throws Exception {

		// No value for the "pass.as.user.data"
		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "localhost" );
		configuration.put( RABBITMQ_SERVER_USERNAME, "guest" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "guest" );

		configuration.put( RABBITMQ_SSL_KEY_STORE_PASSPHRASE, "1" );
		configuration.put( RABBITMQ_SSL_KEY_STORE_TYPE, "2" );
		configuration.put( RABBITMQ_SSL_KEY_STORE_PATH, "3" );
		configuration.put( RABBITMQ_SSL_KEY_MNGR_FACTORY, "4" );
		configuration.put( RABBITMQ_SSL_TRUST_MNGR_FACTORY, "5" );
		configuration.put( RABBITMQ_SSL_TRUST_STORE_PASSPHRASE, "6" );
		configuration.put( RABBITMQ_SSL_TRUST_STORE_PATH, "7" );
		configuration.put( RABBITMQ_SSL_TRUST_STORE_TYPE, "8" );
		configuration.put( RABBITMQ_SSL_PROTOCOL, "9" );
		configuration.put( RABBITMQ_USE_SSL, "true" );

		final int beforeCpt = configuration.size();

		// Remember we add the messaging type as a new property.
		// We also set the value for "pass.as.user.data".
		RabbitMqClient client = new RabbitMqClient( null, configuration, RecipientKind.DM );

		Map<String,String> retrievedConfiguration = client.getConfiguration();
		Assert.assertEquals( beforeCpt + 4, retrievedConfiguration.size());
		Assert.assertEquals( "", retrievedConfiguration.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + RABBITMQ_SSL_KEY_STORE_PATH ));
		Assert.assertEquals( "", retrievedConfiguration.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + RABBITMQ_SSL_TRUST_STORE_PATH ));

		// "pass.as.user.data" is true => same behavior
		configuration.put( RABBITMQ_SSL_AS_USER_DATA, "true" );
		client = new RabbitMqClient( null, configuration, RecipientKind.DM );
		retrievedConfiguration = client.getConfiguration();

		Assert.assertEquals( beforeCpt + 4, retrievedConfiguration.size());
		Assert.assertEquals( "", retrievedConfiguration.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + RABBITMQ_SSL_KEY_STORE_PATH ));
		Assert.assertEquals( "", retrievedConfiguration.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + RABBITMQ_SSL_TRUST_STORE_PATH ));

		// "pass.as.user.data" is false => only the SSL protocol is kept
		configuration.put( RABBITMQ_SSL_AS_USER_DATA, "false" );
		client = new RabbitMqClient( null, configuration, RecipientKind.DM );
		retrievedConfiguration = client.getConfiguration();

		Assert.assertEquals( 7, retrievedConfiguration.size());
		Assert.assertEquals( "localhost", retrievedConfiguration.get( RABBITMQ_SERVER_IP ));
		Assert.assertEquals( "guest", retrievedConfiguration.get( RABBITMQ_SERVER_USERNAME ));
		Assert.assertEquals( "guest", retrievedConfiguration.get( RABBITMQ_SERVER_PASSWORD ));
		Assert.assertEquals( "9", retrievedConfiguration.get( RABBITMQ_SSL_PROTOCOL ));
		Assert.assertEquals( FACTORY_RABBITMQ, retrievedConfiguration.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
		Assert.assertEquals( "false", retrievedConfiguration.get( RABBITMQ_SSL_AS_USER_DATA ));
		Assert.assertEquals( "true", retrievedConfiguration.get( RABBITMQ_USE_SSL ));

		// "pass.as.user.data" has an invalid value => same as "true"
		configuration.put( RABBITMQ_SSL_AS_USER_DATA, "oops" );
		client = new RabbitMqClient( null, configuration, RecipientKind.DM );
		retrievedConfiguration = client.getConfiguration();

		Assert.assertEquals( beforeCpt + 4, retrievedConfiguration.size());
		Assert.assertEquals( "true", retrievedConfiguration.get( RABBITMQ_SSL_AS_USER_DATA ));
		Assert.assertEquals( "", retrievedConfiguration.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + RABBITMQ_SSL_KEY_STORE_PATH ));
		Assert.assertEquals( "", retrievedConfiguration.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + RABBITMQ_SSL_TRUST_STORE_PATH ));

		// Disable SSL
		configuration.put( RABBITMQ_USE_SSL, "false" );
		client = new RabbitMqClient( null, configuration, RecipientKind.DM );
		retrievedConfiguration = client.getConfiguration();

		Assert.assertEquals( 4, retrievedConfiguration.size());
		Assert.assertEquals( "localhost", retrievedConfiguration.get( RABBITMQ_SERVER_IP ));
		Assert.assertEquals( "guest", retrievedConfiguration.get( RABBITMQ_SERVER_USERNAME ));
		Assert.assertEquals( "guest", retrievedConfiguration.get( RABBITMQ_SERVER_PASSWORD ));
		Assert.assertEquals( FACTORY_RABBITMQ, retrievedConfiguration.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));

		// Disable SSL
		configuration.remove( RABBITMQ_USE_SSL );
		client = new RabbitMqClient( null, configuration, RecipientKind.DM );
		retrievedConfiguration = client.getConfiguration();

		Assert.assertEquals( 4, retrievedConfiguration.size());
		Assert.assertEquals( "localhost", retrievedConfiguration.get( RABBITMQ_SERVER_IP ));
		Assert.assertEquals( "guest", retrievedConfiguration.get( RABBITMQ_SERVER_USERNAME ));
		Assert.assertEquals( "guest", retrievedConfiguration.get( RABBITMQ_SERVER_PASSWORD ));
		Assert.assertEquals( FACTORY_RABBITMQ, retrievedConfiguration.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
	}
}
