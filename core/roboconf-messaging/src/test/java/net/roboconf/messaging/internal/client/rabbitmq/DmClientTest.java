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

package net.roboconf.messaging.internal.client.rabbitmq;

import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.internal.RabbitMqTestUtils;
import net.roboconf.messaging.messages.Message;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Channel;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmClientTest {
	private static boolean rabbitMqIsRunning = false;

	@BeforeClass
	public static void checkRabbitMqIsRunning() throws Exception {
		rabbitMqIsRunning = RabbitMqTestUtils.checkRabbitMqIsRunning();
	}


	@Test
	public void testExceptions() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );

		RabbitMqClientDm dmClient = new RabbitMqClientDm();
		dmClient.setParameters( "localhost", "guest", "guest" );

		Assert.assertFalse( dmClient.isConnected());
		Assert.assertNull( dmClient.channel );

		LinkedBlockingQueue<Message> messagesQueue = new LinkedBlockingQueue<Message> ();
		dmClient.setMessageQueue( messagesQueue );
		dmClient.openConnection();
		Assert.assertNotNull( dmClient.channel );

		// openConnection is idem-potent
		Channel oldChannel = dmClient.channel;
		dmClient.openConnection();
		Assert.assertEquals( oldChannel, dmClient.channel );

		Assert.assertEquals( 0, dmClient.applicationNameToConsumerTag.size());
		dmClient.listenToAgentMessages( new Application( "app" ), ListenerCommand.START );
		Assert.assertEquals( 1, dmClient.applicationNameToConsumerTag.size());

		String consumerTag = dmClient.applicationNameToConsumerTag.get( "app" );
		Assert.assertNotNull( consumerTag );

		dmClient.listenToAgentMessages( new Application( "app" ), ListenerCommand.START ); // should be ignored
		Assert.assertEquals( 1, dmClient.applicationNameToConsumerTag.size());
		Assert.assertEquals( consumerTag, dmClient.applicationNameToConsumerTag.get( "app" ));

		dmClient.listenToAgentMessages( new Application( "app" ), ListenerCommand.STOP );
		Assert.assertEquals( 0, dmClient.applicationNameToConsumerTag.size());

		dmClient.deleteMessagingServerArtifacts( new Application( "app" ));
		dmClient.closeConnection();
		Assert.assertNull( dmClient.channel );

		// closeConnection is idem-potent
		dmClient.closeConnection();
		Assert.assertNull( dmClient.channel );

		consumerTag = dmClient.applicationNameToConsumerTag.get( "app" );
		Assert.assertNull( consumerTag );
	}
}
