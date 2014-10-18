/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.messaging.processors;

import java.io.IOException;

import junit.framework.Assert;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.internal.client.test.TestClientAgent;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdResynchronize;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractMessageProcessorTest {

	private AbstractMessageProcessorDm processor;


	@Before
	public void initializeProcessor() {

		String name = MessagingConstants.FACTORY_TEST;
		this.processor = new AbstractMessageProcessorDm( name ) {
			@Override
			protected void processMessage( Message message ) {
				// nothing
			}

			@Override
			protected void openConnection( IDmClient newMessagingClient ) throws IOException {
				// nothing
			}
		};
	}


	@After
	public void terminateProcessor() {
		if( this.processor != null )
			this.processor.stopProcessor();
	}


	@Test
	public void testInterrupt() throws Exception {

		Assert.assertFalse( this.processor.isRunning());
		this.processor.start();

		Thread.sleep( 200 );
		Assert.assertTrue( this.processor.isRunning());

		this.processor.interrupt();
		Thread.sleep( 200 );
		Assert.assertFalse( this.processor.isRunning());
	}


	@Test
	public void testStartAndStop() throws Exception {

		Assert.assertFalse( this.processor.isRunning());
		this.processor.start();

		Thread.sleep( 200 );
		Assert.assertTrue( this.processor.isRunning());

		this.processor.stopProcessor();;
		Thread.sleep( 200 );
		Assert.assertFalse( this.processor.isRunning());
	}


	@Test
	public void testProcessing() throws Exception {

		// The message is processed...
		this.processor.start();
		this.processor.storeMessage( new MsgCmdResynchronize());
		Thread.sleep( MessagingConstants.MESSAGE_POLLING_PERIOD );
		this.processor.stopProcessor();

		Assert.assertTrue( this.processor.hasNoMessage());
	}


	@Test
	public void testCloseConnection() throws Exception {

		TestClientAgent client = new TestClientAgent();
		Assert.assertFalse( client.isConnected());
		this.processor.closeConnection( client, "" );

		client = new TestClientAgent();
		client.openConnection();
		Assert.assertTrue( client.isConnected());
		this.processor.closeConnection( client, "" );
		Assert.assertFalse( client.isConnected());

		client = new TestClientAgent() {
			@Override
			public void closeConnection() throws IOException {
				throw new IOException( "For test purpose" );
			}
		};

		client.openConnection();
		Assert.assertTrue( client.isConnected());
		this.processor.closeConnection( client, "" );
	}
}
