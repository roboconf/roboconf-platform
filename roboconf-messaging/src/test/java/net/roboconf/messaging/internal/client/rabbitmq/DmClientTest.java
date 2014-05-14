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

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IClient.ListenerCommand;
import net.roboconf.messaging.internal.MessagingTestUtils.StorageMessageProcessor;

import org.junit.Test;

import com.rabbitmq.client.Channel;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmClientTest {

	@Test
	public void testExceptions() throws Exception {

		DmClient dmClient = new DmClient();
		dmClient.setMessageServerIp( "127.0.0.1" );

		Assert.assertNull( dmClient.channel );
		Assert.assertNull( dmClient.messageProcessor );
		dmClient.openConnection( new StorageMessageProcessor());
		Assert.assertNotNull( dmClient.channel );
		Assert.assertNotNull( dmClient.messageProcessor );
		Assert.assertTrue( dmClient.messageProcessor instanceof StorageMessageProcessor );
		Assert.assertTrue( dmClient.messageProcessor.isRunning());

		Channel oldChannel = dmClient.channel;
		AbstractMessageProcessor oldProcessor = dmClient.messageProcessor;
		dmClient.openConnection( new StorageMessageProcessor());
		Assert.assertEquals( oldChannel, dmClient.channel );
		Assert.assertEquals( oldProcessor, dmClient.messageProcessor );

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

		Assert.assertTrue( dmClient.messageProcessor.isRunning());
		dmClient.closeConnection();
		Assert.assertFalse( dmClient.messageProcessor.isRunning());
		Assert.assertNull( dmClient.channel );
	}


	@Test
	public void testCloseConnectionOnNull() throws Exception {

		DmClient dmClient = new DmClient();
		dmClient.setMessageServerIp( "127.0.0.1" );

		Assert.assertNull( dmClient.channel );
		Assert.assertNull( dmClient.messageProcessor );
		dmClient.closeConnection();
	}
}
