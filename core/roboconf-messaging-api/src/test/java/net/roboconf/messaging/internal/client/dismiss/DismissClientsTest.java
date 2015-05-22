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

package net.roboconf.messaging.internal.client.dismiss;

import java.util.Collections;

import junit.framework.Assert;
import net.roboconf.messaging.client.IClient.ListenerCommand;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DismissClientsTest {

	@Test
	public void testDm() throws Exception {

		DismissClientDm client = new DismissClientDm();
		client.closeConnection();
		client.deleteMessagingServerArtifacts( null );
		Assert.assertFalse( client.isConnected());
		client.listenToAgentMessages( null, ListenerCommand.START );;
		client.openConnection();
		client.propagateAgentTermination( null, null );
		client.sendMessageToAgent( null, null, null );
		client.sendMessageToTheDm( null );
		client.listenToTheDm( ListenerCommand.START );
		client.setMessageQueue( null );
		client.getMessagingType();
		client.getConfiguration();
		client.setConfiguration(Collections.<String, String>emptyMap());
	}


	@Test
	public void testAgent() throws Exception {

		DismissClientAgent client = new DismissClientAgent();
		client.closeConnection();
		Assert.assertFalse( client.isConnected());
		client.openConnection();
		client.setMessageQueue( null );
		client.listenToExportsFromOtherAgents( ListenerCommand.STOP, null );
		client.listenToRequestsFromOtherAgents( ListenerCommand.STOP, null );
		client.listenToTheDm( ListenerCommand.START );
		client.publishExports( null );
		client.publishExports( null, "" );
		client.requestExportsFromOtherAgents( null );
		client.setScopedInstancePath( "/root" );
		client.setApplicationName( "app" );
		client.sendMessageToTheDm( null );
		client.unpublishExports( null );
		client.getMessagingType();
		client.getConfiguration();
		client.setConfiguration(Collections.<String, String>emptyMap());
	}
}
