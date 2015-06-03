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

package net.roboconf.agent.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.agents.DataHelpers;

import net.roboconf.messaging.api.client.IClient;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentPropertiesTest {

	public static final String MESSAGING_IP = "messaging.ip";
	public static final String MESSAGING_USERNAME = "messaging.username";
	public static final String MESSAGING_PASSWORD = "messaging.password";

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

		String s = DataHelpers.writeUserDataAsString( msgCfg("irc", "ip", "user", "pwd"), "my app", "/root/path" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertNull( ad.getIpAddress());
		final Map<String, String> msgCfg = ad.getMessagingConfiguration();
		Assert.assertEquals( "irc", msgCfg.get(IClient.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals( "ip", msgCfg.get(MESSAGING_IP));
		Assert.assertEquals( "pwd", msgCfg.get(MESSAGING_PASSWORD));
		Assert.assertEquals( "user", msgCfg.get(MESSAGING_USERNAME));
		Assert.assertEquals( "/root/path", ad.getScopedInstancePath());
	}


	@Test
	public void testReadIaasProperties_partial() throws Exception {

		String s = DataHelpers.writeUserDataAsString( msgCfg("irc", "ip", "user", null), "my app", "/root/path" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertNull( ad.getIpAddress());
		final Map<String, String> msgCfg = ad.getMessagingConfiguration();
		Assert.assertEquals( "irc", msgCfg.get(IClient.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals( "ip", msgCfg.get(MESSAGING_IP));
		Assert.assertNull(msgCfg.get(MESSAGING_PASSWORD));
		Assert.assertEquals( "user", msgCfg.get(MESSAGING_USERNAME));
		Assert.assertEquals( "/root/path", ad.getScopedInstancePath());
	}


	@Test
	public void testReadIaasProperties_withSpecialCharacters() throws Exception {

		String s = DataHelpers.writeUserDataAsString( msgCfg("irc", "ip\\:port", "user", "pwd:with:two;dots\\:"), "my app", "/root/path" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertNull( ad.getIpAddress());
		final Map<String, String> msgCfg = ad.getMessagingConfiguration();
		Assert.assertEquals( "irc", msgCfg.get(IClient.MESSAGING_TYPE_PROPERTY));
		Assert.assertEquals( "ip:port", msgCfg.get(MESSAGING_IP));
		Assert.assertEquals( "pwd:with:two;dots:", msgCfg.get(MESSAGING_PASSWORD));
		Assert.assertEquals( "user", msgCfg.get(MESSAGING_USERNAME));
		Assert.assertEquals( "/root/path", ad.getScopedInstancePath());
	}


	@Test
	public void testValidate() {

		AgentProperties ad = new AgentProperties();
		ad.setApplicationName( "my app" );
		ad.setScopedInstancePath( "/root" );
		ad.setMessagingConfiguration(msgCfg("irc", "192.168.1.18", "personne", "azerty (;))"));
		ad.setIpAddress( "whatever" );
		Assert.assertNull( ad.validate());

		ad.setIpAddress( null );
		Assert.assertNull( ad.validate());

		ad.setApplicationName( null );
		Assert.assertNotNull( ad.validate());
		ad.setApplicationName( "my app" );

		ad.setScopedInstancePath( "" );
		Assert.assertNotNull( ad.validate());
		ad.setScopedInstancePath( "root" );

		ad.setMessagingConfiguration(msgCfg("irc", null, "personne", "azerty (;))"));
		Assert.assertNull(ad.validate());
		ad.setMessagingConfiguration(msgCfg("irc", "192.168.1.18", "personne", "azerty (;))"));

		ad.setMessagingConfiguration(msgCfg("irc", "192.168.1.18", "personne", "   "));
		Assert.assertNull(ad.validate());
		ad.setMessagingConfiguration(msgCfg("irc", "192.168.1.18", "personne", "azerty (;))"));

		ad.setMessagingConfiguration(msgCfg("irc", "192.168.1.18", "", "azerty (;))"));
		Assert.assertNull(ad.validate());
		ad.setMessagingConfiguration(msgCfg("irc", "192.168.1.18", "personne", "azerty (;))"));

		ad.setMessagingConfiguration(msgCfg(null, "192.168.1.18", "personne", "azerty (;))"));
		Assert.assertNotNull( ad.validate());
		ad.setMessagingConfiguration(msgCfg("irc", "192.168.1.18", "personne", "azerty (;))"));
	}

	/**
	 * Creates a pseudo messaging configuration for the given IP and credentials.
	 * @param messagingType the pseudo messaging type.
	 * @param ip the pseudo IP address.
	 * @param user the pseudo user.
	 * @param pass the pseudo password.
	 * @return the pseudo messaging configuration.
	 */
	private static Map<String, String> msgCfg(String messagingType, String ip, String user, String pass) {
		Map<String, String> result = new LinkedHashMap<>();
		result.put(IClient.MESSAGING_TYPE_PROPERTY, messagingType);
		result.put(MESSAGING_IP, ip);
		result.put(MESSAGING_USERNAME, user);
		result.put(MESSAGING_PASSWORD, pass);
		return Collections.unmodifiableMap(result);
	}

}
