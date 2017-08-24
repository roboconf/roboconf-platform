/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.client.idle;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.messaging.api.MessagingConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IdleClientTest {

	@Test
	public void testClientMethods() throws Exception {

		IdleClient client = new IdleClient();

		// Basic checks
		Assert.assertFalse( client.isConnected());
		client.openConnection();
		Assert.assertTrue( client.isConnected());
		client.closeConnection();
		Assert.assertFalse( client.isConnected());

		Assert.assertNotNull( client.getMessagingType());
		Assert.assertNotNull( client.getConfiguration());

		Assert.assertEquals( 1, client.getConfiguration().size());
		Assert.assertEquals(
				MessagingConstants.FACTORY_IDLE,
				client.getConfiguration().get( MessagingConstants.MESSAGING_TYPE_PROPERTY ));

		// Not checkable
		client.deleteMessagingServerArtifacts( null );
		client.publish( null, null );
		client.subscribe( null );
		client.unsubscribe( null );
		client.setMessageQueue( null );
		client.setOwnerProperties( null, null, null, null );
	}
}
