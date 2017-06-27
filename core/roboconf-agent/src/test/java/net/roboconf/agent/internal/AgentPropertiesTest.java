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

package net.roboconf.agent.internal;

import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.FACTORY_RABBITMQ;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_PASSWORD;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_USERNAME;
import static net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils.rabbitMqMessagingConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.userdata.UserDataHelpers;
import net.roboconf.messaging.api.MessagingConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentPropertiesTest {

	@Test
	public void testReadIaasProperties_null() throws Exception {

		AgentProperties ad = AgentProperties.readIaasProperties( null, Logger.getAnonymousLogger());
		Assert.assertNull( ad.getApplicationName());
		Assert.assertNull( ad.getIpAddress());
		Assert.assertEquals(Collections.emptyMap(), ad.getMessagingConfiguration());
		Assert.assertNull( ad.getScopedInstancePath());
	}


	@Test
	public void testReadIaasProperties_all() throws Exception {

		String s = UserDataHelpers.writeUserDataAsString(rabbitMqMessagingConfiguration("ip", "user", "pwd"), "domain", "my app", "/root/path" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertEquals( "domain", ad.getDomain());
		Assert.assertNull( ad.getIpAddress());

		final Map<String,String> msgCfg = ad.getMessagingConfiguration();
		Assert.assertEquals( FACTORY_RABBITMQ, msgCfg.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals( "ip", msgCfg.get(RABBITMQ_SERVER_IP));
		Assert.assertEquals( "pwd", msgCfg.get(RABBITMQ_SERVER_PASSWORD));
		Assert.assertEquals( "user", msgCfg.get(RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals( "/root/path", ad.getScopedInstancePath());
	}


	@Test
	public void testReadIaasProperties_partial() throws Exception {

		String s = UserDataHelpers.writeUserDataAsString( rabbitMqMessagingConfiguration("ip", "user", null), "domain", "my app", "/root/path" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertEquals( "domain", ad.getDomain());
		Assert.assertNull( ad.getIpAddress());

		final Map<String,String> msgCfg = ad.getMessagingConfiguration();
		Assert.assertEquals( FACTORY_RABBITMQ, msgCfg.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals( "ip", msgCfg.get(RABBITMQ_SERVER_IP));
		Assert.assertNull(msgCfg.get(RABBITMQ_SERVER_PASSWORD));
		Assert.assertEquals( "user", msgCfg.get(RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals( "/root/path", ad.getScopedInstancePath());
	}


	@Test
	public void testReadIaasProperties_withSpecialCharacters() throws Exception {

		String s = UserDataHelpers.writeUserDataAsString( rabbitMqMessagingConfiguration("ip\\:port", "user", "pwd:with:two;dots\\:"), "domain", "my app", "/root/path" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertEquals( "domain", ad.getDomain());
		Assert.assertNull( ad.getIpAddress());

		final Map<String,String> msgCfg = ad.getMessagingConfiguration();
		Assert.assertEquals( FACTORY_RABBITMQ, msgCfg.get(MessagingConstants.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals( "ip:port", msgCfg.get(RABBITMQ_SERVER_IP));
		Assert.assertEquals( "pwd:with:two;dots:", msgCfg.get(RABBITMQ_SERVER_PASSWORD));
		Assert.assertEquals( "user", msgCfg.get(RABBITMQ_SERVER_USERNAME));
		Assert.assertEquals( "/root/path", ad.getScopedInstancePath());
	}


	@Test
	public void testValidate() {

		AgentProperties ad = new AgentProperties();
		ad.setApplicationName( "my app" );
		ad.setScopedInstancePath( "/root" );
		ad.setDomain( "d1" );
		ad.setIpAddress( "whatever" );
		Assert.assertNotNull( ad.validate());

		ad.setMessagingConfiguration( new HashMap<String,String>( 0 ));
		Assert.assertNotNull( ad.validate());

		ad.setMessagingConfiguration(rabbitMqMessagingConfiguration("192.168.1.18", "personne", "azerty (;))"));
		Assert.assertNull( ad.validate());

		ad.setIpAddress( null );
		Assert.assertNull( ad.validate());

		ad.setApplicationName( null );
		Assert.assertNotNull( ad.validate());
		ad.setApplicationName( "my app" );

		ad.setScopedInstancePath( "" );
		Assert.assertNotNull( ad.validate());
		ad.setScopedInstancePath( "root" );

		ad.setMessagingConfiguration(rabbitMqMessagingConfiguration(null, "personne", "azerty (;))"));
		Assert.assertNull(ad.validate());
		ad.setMessagingConfiguration(rabbitMqMessagingConfiguration("192.168.1.18", "personne", "azerty (;))"));

		ad.setMessagingConfiguration(rabbitMqMessagingConfiguration("192.168.1.18", "personne", "   "));
		Assert.assertNull(ad.validate());
		ad.setMessagingConfiguration(rabbitMqMessagingConfiguration("192.168.1.18", "personne", "azerty (;))"));

		ad.setMessagingConfiguration(rabbitMqMessagingConfiguration("192.168.1.18", "", "azerty (;))"));
		Assert.assertNull(ad.validate());
		ad.setMessagingConfiguration(rabbitMqMessagingConfiguration("192.168.1.18", "personne", "azerty (;))"));

		// Test with no factory type.
		final Map<String, String> msgCfg = new LinkedHashMap<>(rabbitMqMessagingConfiguration("192.168.1.18", "personne", "azerty (;))"));
		msgCfg.remove(MessagingConstants.MESSAGING_TYPE_PROPERTY);
		ad.setMessagingConfiguration(msgCfg);
		Assert.assertNotNull( ad.validate());
		ad.setMessagingConfiguration(rabbitMqMessagingConfiguration("192.168.1.18", "personne", "azerty (;))"));
	}
}
