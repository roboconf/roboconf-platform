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

package net.roboconf.agent.internal.misc;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class UserDataHelperTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private final UserDataHelper userDataHelper = new UserDataHelper();


	@After
	public void clearAgentDirectories() throws Exception {
		File f = new File( Constants.WORK_DIRECTORY_AGENT );
		Utils.deleteFilesRecursively( f );
	}


	@Test
	public void testReconfigureMessaging_noAgentConfigurationFile() throws IOException {

		// Prepare
		File karafEtc =  this.folder.newFolder();
		final File msgConf = new File( karafEtc, "net.roboconf.messaging.rabbitmq.cfg" );
		final File agentConf = new File( karafEtc, Constants.KARAF_CFG_FILE_AGENT );

		// Execute
		Map<String,String> msgData = new HashMap<> ();
		msgData.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "rabbitmq" );
		msgData.put("net.roboconf.messaging.rabbitmq.server.ip", "rabbit-server");
		msgData.put("net.roboconf.messaging.rabbitmq.server.username", "user1");
		msgData.put("net.roboconf.messaging.rabbitmq.server.password", "password1");

		Assert.assertFalse( msgConf.exists());
		Assert.assertFalse( agentConf.exists());

		this.userDataHelper.reconfigureMessaging(karafEtc.getAbsolutePath(), msgData);

		// Check
		Assert.assertTrue( msgConf.exists());
		Assert.assertTrue( agentConf.exists());

		Properties p = Utils.readPropertiesFile(msgConf);
		String val = p.getProperty("net.roboconf.messaging.rabbitmq.server.ip");
		Assert.assertEquals(val, "rabbit-server");
		val = p.getProperty("net.roboconf.messaging.rabbitmq.server.username");
		Assert.assertEquals(val, "user1");
		val = p.getProperty("net.roboconf.messaging.rabbitmq.server.password");
		Assert.assertEquals(val, "password1");

		p = Utils.readPropertiesFile( agentConf );
		Assert.assertEquals( "rabbitmq", p.get( Constants.MESSAGING_TYPE ));
		Assert.assertEquals( 1, p.size());
	}


	@Test
	public void testReconfigureMessaging_withAgentConfigurationFile() throws IOException {

		// Prepare
		File karafEtc =  this.folder.newFolder();
		final File msgConf = new File( karafEtc, "net.roboconf.messaging.rabbitmq.cfg" );
		final File agentConf = new File( karafEtc, Constants.KARAF_CFG_FILE_AGENT );

		Properties props = new Properties();
		props.setProperty( "key", "value" );
		props.setProperty( "something", "else" );
		props.setProperty( Constants.MESSAGING_TYPE, "http" );
		Utils.writePropertiesFile( props, agentConf );

		// Execute
		Map<String,String> msgData = new HashMap<> ();
		msgData.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "rabbitmq" );
		msgData.put("net.roboconf.messaging.rabbitmq.server.ip", "rabbit-server");
		msgData.put("net.roboconf.messaging.rabbitmq.server.username", "user1");
		msgData.put("net.roboconf.messaging.rabbitmq.server.password", "password1");
		msgData.put("test", "issue #779");

		Assert.assertFalse( msgConf.exists());
		Assert.assertTrue( agentConf.exists());

		this.userDataHelper.reconfigureMessaging( karafEtc.getAbsolutePath(), msgData);

		// Check
		Assert.assertTrue( msgConf.exists());
		Assert.assertTrue( agentConf.exists());

		Properties p = Utils.readPropertiesFile( msgConf );
		Assert.assertEquals( 4, p.keySet().size());

		String val = p.getProperty("net.roboconf.messaging.rabbitmq.server.ip");
		Assert.assertEquals(val, "rabbit-server");
		val = p.getProperty("net.roboconf.messaging.rabbitmq.server.username");
		Assert.assertEquals(val, "user1");
		val = p.getProperty("net.roboconf.messaging.rabbitmq.server.password");
		Assert.assertEquals(val, "password1");
		val = p.getProperty("test");
		Assert.assertEquals(val, "issue #779");

		p = Utils.readPropertiesFile( agentConf );
		Assert.assertEquals( 3, p.size());
		Assert.assertEquals( "rabbitmq", p.get( Constants.MESSAGING_TYPE ));
		Assert.assertEquals( "else", p.get( "something" ));
		Assert.assertEquals( "value", p.get( "key" ));

		// Verify we do not remove all the properties when we write new ones (#779)
		msgData.remove( "test" );
		msgData.put("net.roboconf.messaging.rabbitmq.server.username", "user2");

		this.userDataHelper.reconfigureMessaging( karafEtc.getAbsolutePath(), msgData);

		p = Utils.readPropertiesFile( msgConf );
		Assert.assertEquals( 4, p.keySet().size());

		val = p.getProperty("net.roboconf.messaging.rabbitmq.server.ip");
		Assert.assertEquals(val, "rabbit-server");
		val = p.getProperty("net.roboconf.messaging.rabbitmq.server.username");
		Assert.assertEquals(val, "user2");
		val = p.getProperty("net.roboconf.messaging.rabbitmq.server.password");
		Assert.assertEquals(val, "password1");
		val = p.getProperty("test");
		Assert.assertEquals(val, "issue #779");
	}


	@Test
	public void testReconfigureMessaging_withAgentConfigurationFile_nullMessagingType() throws IOException {

		// Prepare
		File karafEtc =  this.folder.newFolder();
		final File msgConf = new File( karafEtc, "net.roboconf.messaging.rabbitmq.cfg" );
		final File agentConf = new File( karafEtc, Constants.KARAF_CFG_FILE_AGENT );

		Properties props = new Properties();
		props.setProperty( "key", "value" );
		props.setProperty( "something", "else" );
		props.setProperty( Constants.MESSAGING_TYPE, "http" );
		Utils.writePropertiesFile( props, agentConf );

		// Execute
		Map<String,String> msgData = new HashMap<> ();
		msgData.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, null );
		msgData.put("net.roboconf.messaging.rabbitmq.server.ip", "rabbit-server");
		msgData.put("net.roboconf.messaging.rabbitmq.server.username", "user1");
		msgData.put("net.roboconf.messaging.rabbitmq.server.password", "password1");
		msgData.put("test", "issue #779");

		Assert.assertFalse( msgConf.exists());
		Assert.assertTrue( agentConf.exists());

		this.userDataHelper.reconfigureMessaging( karafEtc.getAbsolutePath(), msgData);

		// Check
		Assert.assertFalse( msgConf.exists());
		Assert.assertTrue( agentConf.exists());

		Properties p = Utils.readPropertiesFile( agentConf );
		Assert.assertEquals( 3, p.size());
		Assert.assertEquals( "http", p.get( Constants.MESSAGING_TYPE ));
		Assert.assertEquals( "else", p.get( "something" ));
		Assert.assertEquals( "value", p.get( "key" ));
	}


	@Test
	public void testReconfigureMessaging_noKarafEtc_noMessagingTypeSpecified() throws IOException {

		this.userDataHelper.reconfigureMessaging(
				"this_is_a_wrong_path",
				new HashMap<String,String>( 0 ));
	}


	@Test( expected = IOException.class )
	public void testReconfigureMessaging_noKarafEtc() throws IOException {

		Map<String,String> msgData = new HashMap<> ();
		msgData.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, "rabbitmq" );
		msgData.put("net.roboconf.messaging.rabbitmq.server.ip", "rabbit-server");
		msgData.put("net.roboconf.messaging.rabbitmq.server.username", "user1");
		msgData.put("net.roboconf.messaging.rabbitmq.server.password", "password1");

		this.userDataHelper.reconfigureMessaging(
				"this_is_a_wrong_path",
				msgData );
	}


	@Test
	public void testReconfigureMessaging_emptyKarafEtc() throws IOException {

		// No exception
		this.userDataHelper.reconfigureMessaging(
				"",
				new HashMap<String,String>( 0 ));
	}
}
