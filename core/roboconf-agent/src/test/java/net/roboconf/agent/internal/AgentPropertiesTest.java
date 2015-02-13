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

import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.agents.DataHelpers;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentPropertiesTest {

	@Test
	public void testReadIaasProperties_null() throws Exception {

		AgentProperties ad = AgentProperties.readIaasProperties( null, Logger.getAnonymousLogger());
		Assert.assertNull( ad.getApplicationName());
		Assert.assertNull( ad.getIpAddress());
		Assert.assertNull( ad.getMessageServerIp());
		Assert.assertNull( ad.getMessageServerPassword());
		Assert.assertNull( ad.getMessageServerUsername());
		Assert.assertNull( ad.getRootInstanceName());
	}


	@Test
	public void testReadIaasProperties_all() throws Exception {

		String s = DataHelpers.writeUserDataAsString( "ip", "user", "pwd", "my app", "root name" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertNull( ad.getIpAddress());
		Assert.assertEquals( "ip", ad.getMessageServerIp());
		Assert.assertEquals( "pwd", ad.getMessageServerPassword());
		Assert.assertEquals( "user", ad.getMessageServerUsername());
		Assert.assertEquals( "root name", ad.getRootInstanceName());
	}


	@Test
	public void testReadIaasProperties_partial() throws Exception {

		String s = DataHelpers.writeUserDataAsString( "ip", "user", null, "my app", "root name" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertNull( ad.getIpAddress());
		Assert.assertEquals( "ip", ad.getMessageServerIp());
		Assert.assertNull( ad.getMessageServerPassword());
		Assert.assertEquals( "user", ad.getMessageServerUsername());
		Assert.assertEquals( "root name", ad.getRootInstanceName());
	}


	@Test
	public void testReadIaasProperties_withSpecialCharacters() throws Exception {

		String s = DataHelpers.writeUserDataAsString( "ip\\:port", "user", "pwd:with:two;dots\\:", "my app", "root name" );

		AgentProperties ad = AgentProperties.readIaasProperties( s, Logger.getAnonymousLogger());
		Assert.assertEquals( "my app", ad.getApplicationName());
		Assert.assertNull( ad.getIpAddress());
		Assert.assertEquals( "ip:port", ad.getMessageServerIp());
		Assert.assertEquals( "pwd:with:two;dots:", ad.getMessageServerPassword());
		Assert.assertEquals( "user", ad.getMessageServerUsername());
		Assert.assertEquals( "root name", ad.getRootInstanceName());
	}


	@Test
	public void testValidate() {

		AgentProperties ad = new AgentProperties();
		ad.setApplicationName( "my app" );
		ad.setRootInstanceName( "root" );
		ad.setMessageServerIp( "192.168.1.18" );
		ad.setMessageServerPassword( "azerty (;))" );
		ad.setMessageServerUsername( "personne" );
		ad.setIpAddress( "whatever" );
		Assert.assertNull( ad.validate());

		ad.setIpAddress( null );
		Assert.assertNull( ad.validate());

		ad.setApplicationName( null );
		Assert.assertNotNull( ad.validate());
		ad.setApplicationName( "my app" );

		ad.setRootInstanceName( "" );
		Assert.assertNotNull( ad.validate());
		ad.setRootInstanceName( "root" );

		ad.setMessageServerIp( null );
		Assert.assertNotNull( ad.validate());
		ad.setMessageServerIp( "192.168.1.18" );

		ad.setMessageServerPassword( "   " );
		Assert.assertNotNull( ad.validate());
		ad.setMessageServerPassword( "azerty" );

		ad.setMessageServerUsername( "" );
		Assert.assertNotNull( ad.validate());
		ad.setMessageServerUsername( "personne" );
	}
}
