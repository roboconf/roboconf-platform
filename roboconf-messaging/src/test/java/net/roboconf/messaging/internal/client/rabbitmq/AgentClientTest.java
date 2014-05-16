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
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.internal.MessagingTestUtils.StorageMessageProcessor;

import org.junit.Test;

import com.rabbitmq.client.Channel;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentClientTest {

	@Test
	public void testExceptions() throws Exception {

		AgentClient agentClient = new AgentClient();
		agentClient.setMessageServerIp( "127.0.0.1" );
		agentClient.setApplicationName( "app" );
		agentClient.setRootInstanceName( "root" );

		Assert.assertNull( agentClient.channel );
		Assert.assertNull( agentClient.messageProcessor );
		agentClient.openConnection( new StorageMessageProcessor());
		Assert.assertNotNull( agentClient.channel );
		Assert.assertNotNull( agentClient.messageProcessor );
		Assert.assertTrue( agentClient.messageProcessor.isRunning());

		Channel oldChannel = agentClient.channel;
		AbstractMessageProcessor oldProcessor = agentClient.messageProcessor;
		String oldConsumerTag = agentClient.consumerTag;
		agentClient.openConnection( new StorageMessageProcessor());
		Assert.assertEquals( oldChannel, agentClient.channel );
		Assert.assertEquals( oldProcessor, agentClient.messageProcessor );
		Assert.assertEquals( oldConsumerTag, agentClient.consumerTag );

		Assert.assertTrue( agentClient.messageProcessor.isRunning());
		agentClient.closeConnection();
		Assert.assertFalse( agentClient.messageProcessor.isRunning());
		Assert.assertNull( agentClient.channel );
		Assert.assertNull( agentClient.consumerTag );
	}


	@Test
	public void testCloseConnectionOnNull() throws Exception {

		AgentClient agentClient = new AgentClient();
		agentClient.setMessageServerIp( "127.0.0.1" );

		Assert.assertNull( agentClient.channel );
		Assert.assertNull( agentClient.messageProcessor );
		agentClient.closeConnection();
	}
}
