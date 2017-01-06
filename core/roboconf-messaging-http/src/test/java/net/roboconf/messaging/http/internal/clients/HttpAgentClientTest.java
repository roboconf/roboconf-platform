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

package net.roboconf.messaging.http.internal.clients;

import java.io.IOException;
import java.util.Map;

import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.http.HttpConstants;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HttpAgentClientTest {

	@Test
	public void testBasics() throws Exception {

		// Invalid connection, etc.
		HttpAgentClient client = new HttpAgentClient( null, "localhost", 9898 );
		Assert.assertFalse( client.isConnected());
		client.closeConnection();

		try {
			client.openConnection();
			Assert.fail( "An exception was expected." );

		} catch( IOException e ) {
			// nothing
		}

		Assert.assertFalse( client.isConnected());
		client.closeConnection();
		client.deleteMessagingServerArtifacts( null );

		// Configuration
		Assert.assertEquals( HttpConstants.FACTORY_HTTP, client.getMessagingType());
		Map<String,String> config = client.getConfiguration();
		Assert.assertEquals( 3, config.size());

		Assert.assertEquals( HttpConstants.FACTORY_HTTP, config.get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));
		Assert.assertEquals( "localhost", config.get( HttpConstants.HTTP_SERVER_IP ));
		Assert.assertEquals( "9898", config.get( HttpConstants.HTTP_SERVER_PORT ));
	}
}
