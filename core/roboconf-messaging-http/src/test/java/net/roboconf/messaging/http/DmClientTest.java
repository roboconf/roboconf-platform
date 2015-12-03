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

package net.roboconf.messaging.http;

import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Assert;
import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.http.internal.HttpDmClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
public class DmClientTest {

	@BeforeClass
	public static void startServices() {
		HttpTestUtils.runWebServer();
		try {
			Thread.sleep(500); //TODO: any way to start webserver quicky (and remove this) ??
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}
	
	@AfterClass
	public static void stopServices() {
		HttpTestUtils.stopWebServer();
		try {
			Thread.sleep(500); //TODO: any way to stop webserver cleanly (and remove this) ??
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}

	@Test
	public void testExceptions() throws Exception {

		HttpDmClient dmClient = new HttpDmClient(null, "127.0.0.1", "8080");

		Assert.assertFalse( dmClient.isConnected());

		LinkedBlockingQueue<Message> messagesQueue = new LinkedBlockingQueue<>();
		dmClient.setMessageQueue( messagesQueue );
		dmClient.openConnection();
		Assert.assertTrue( dmClient.isConnected());

		// openConnection is idem-potent
		dmClient.openConnection();
		Assert.assertTrue( dmClient.isConnected());

		dmClient.listenToAgentMessages( new Application( "app", null ), ListenerCommand.START );

		dmClient.listenToAgentMessages( new Application( "app", null ), ListenerCommand.START ); // should be ignored

		dmClient.listenToAgentMessages( new Application( "app", null ), ListenerCommand.STOP );

		// Check the DM's neutral queue
		dmClient.listenToTheDm( ListenerCommand.START );
		dmClient.listenToTheDm( ListenerCommand.START );
		dmClient.listenToTheDm( ListenerCommand.STOP );
		// Check the idem-potency
		dmClient.listenToTheDm( ListenerCommand.STOP );

		// Close the connection
		dmClient.deleteMessagingServerArtifacts( new Application( "app", null ));
		dmClient.closeConnection();
		Assert.assertFalse( dmClient.isConnected());
		
		// closeConnection is idem-potent
		dmClient.closeConnection();
		Assert.assertFalse( dmClient.isConnected());
	}
}

