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

package net.roboconf.messaging.internal.client;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.client.InteractionType;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * These tests assert messaging and message routing work with RabbitMQ.
 * <p>
 * For real exchanges between the DM and agents, other tests are required.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class MessageServerClientRabbitMqTest {

	private static final long DELAY = 700;
	private static final String MESSAGE_SERVER_IP = "127.0.0.1";

	private boolean running = true;
	private MessageServerClientRabbitMq client;

	@Rule
	public ErrorCollector collector = new ErrorCollector();


	/**
	 * A method to check whether RabbitMQ is running or not.
	 * <p>
	 * If it is not running, tests in this class will be skipped.
	 * </p>
	 */
	@Before
	public void checkRabbitMQIsRunning() throws Exception {

		Assume.assumeTrue( this.running );
		Connection connection = null;
		Channel channel = null;
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost( MESSAGE_SERVER_IP );
			connection = factory.newConnection();
			channel = connection.createChannel();

		} catch( Exception e ) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.warning( "Tests are skipped because RabbitMQ is not running." );
			logger.finest( Utils.writeException( e ));

			this.running = false;
			Assume.assumeNoException( e );

		} finally {
			if( channel != null )
				channel.close();

			if( connection != null )
				connection.close();
		}
	}


	@Before
	public void initializeClient() {

		this.client = new MessageServerClientRabbitMq();
		this.client.setMessageServerIp( MESSAGE_SERVER_IP );
		this.client.setApplicationName( "my-app" );
	}


	@Test
	public void closeConnectionShouldSupportNull() throws Exception {

		this.client = new MessageServerClientRabbitMq();
		this.collector.checkThat( this.client.getChannel(), nullValue());
		this.collector.checkThat( this.client.getConnection(), nullValue());

		this.client.closeConnection();
	}


	@Test
	public void openAndCloseConnectionShouldWork() throws Exception {

		this.collector.checkThat( this.client.getChannel(), nullValue());
		this.collector.checkThat( this.client.getConnection(), nullValue());

		this.client.openConnection();
		this.collector.checkThat( this.client.getChannel(), notNullValue());
		this.collector.checkThat( this.client.getConnection(), notNullValue());
		this.collector.checkThat( this.client.getChannel().isOpen(), is( true ));
		this.collector.checkThat( this.client.getConnection().isOpen(), is( true ));

		this.client.closeConnection();
		this.collector.checkThat( this.client.getChannel(), nullValue());
		this.collector.checkThat( this.client.getConnection(), nullValue());
	}


	@Test
	public void publishAndSubscribeShouldWork() throws Exception {

		// Create the client
		this.client.openConnection();
		Assert.assertEquals( 0, this.client.getQueueNames().size());

		// Register two subscribers and make sure they were registered
		TestMessageProcessor p1 = new TestMessageProcessor();
		this.client.subscribeTo( "Processor 1", InteractionType.AGENT_TO_AGENT, "p1", p1 );
		this.collector.checkThat( this.client.getQueueNames().size(), is( 1 ));
		this.collector.checkThat( "my-app.p1", isIn( this.client.getQueueNames()));

		TestMessageProcessor p2 = new TestMessageProcessor();
		this.client.subscribeTo( "Processor 2", InteractionType.AGENT_TO_AGENT, "p2", p2 );
		this.collector.checkThat( this.client.getQueueNames().size(), is( 2 ));

		// Make sure only the right subscriber received the message to be sent
		this.collector.checkThat( p1.messages, empty());
		this.collector.checkThat( p2.messages, empty());

		this.client.publish( InteractionType.AGENT_TO_AGENT, "p1", new MsgNotifHeartbeat( "" ));
		Thread.sleep( DELAY );	// Wait the message is delivered
		this.collector.checkThat( p1.messages.size(), is( 1 ));
		this.collector.checkThat( p2.messages.size(), is( 0 ));

		// Unsubscribe one of them
		this.client.unsubscribeTo( InteractionType.AGENT_TO_AGENT, "p1" );
		this.collector.checkThat( this.client.getQueueNames().size(), is( 1 ));
		this.collector.checkThat( "my-app.p2", isIn( this.client.getQueueNames()));

		this.client.publish( InteractionType.AGENT_TO_AGENT, "p1", new MsgNotifHeartbeat( "" ));
		Thread.sleep( DELAY );	// Wait the message is delivered
		this.collector.checkThat( p1.messages.size(), is( 1 ));
		this.collector.checkThat( p2.messages.size(), is( 0 ));

		// Delete the queues
		this.client.deleteQueueOrTopic( InteractionType.AGENT_TO_AGENT, "p1" );
		this.client.deleteQueueOrTopic( InteractionType.AGENT_TO_AGENT, "p2" );
		this.collector.checkThat( this.client.getQueueNames().size(), is( 0 ));

		this.client.closeConnection();
	}


	@Test
	public void dmAndAgentShouldCommunicate() throws Exception {

		// Create the client
		this.client.openConnection();
		Assert.assertEquals( 0, this.client.getQueueNames().size());

		// Register two subscribers
		TestMessageProcessor agentProcessor = new TestMessageProcessor();
		this.client.subscribeTo( "Agent processor", InteractionType.DM_AND_AGENT, "agent", agentProcessor );

		TestMessageProcessor dmProcessor = new TestMessageProcessor();
		this.client.subscribeTo( "DM Processor", InteractionType.DM_AND_AGENT, "dm", dmProcessor );

		this.collector.checkThat( this.client.getQueueNames().size(), is( 2 ));
		this.collector.checkThat( "my-app.agent", isIn( this.client.getQueueNames()));
		this.collector.checkThat( "my-app.dm", isIn( this.client.getQueueNames()));

		// Now, send a message from the DM to the agent and make sure the agent processor received it
		this.collector.checkThat( dmProcessor.messages.size(), is( 0 ));
		this.collector.checkThat( agentProcessor.messages.size(), is( 0 ));

		this.client.publish( InteractionType.DM_AND_AGENT, "agent", new MsgNotifHeartbeat( "" ));
		Thread.sleep( DELAY );	// Wait the message is delivered
		this.collector.checkThat( dmProcessor.messages.size(), is( 0 ));
		this.collector.checkThat( agentProcessor.messages.size(), is( 1 ));

		// Now, send a message from the agent to the DM
		this.client.publish( InteractionType.DM_AND_AGENT, "dm", new MsgNotifHeartbeat( "" ));
		Thread.sleep( DELAY );	// Wait the message is delivered
		this.collector.checkThat( dmProcessor.messages.size(), is( 1 ));
		this.collector.checkThat( agentProcessor.messages.size(), is( 1 ));

		// Delete the queues
		this.client.deleteQueueOrTopic( InteractionType.DM_AND_AGENT, "agent" );
		this.client.deleteQueueOrTopic( InteractionType.DM_AND_AGENT, "dm" );
		this.collector.checkThat( this.client.getQueueNames().size(), is( 0 ));

		this.client.closeConnection();
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class TestMessageProcessor implements IMessageProcessor {
		List<Message> messages = new ArrayList<Message> ();


		@Override
		public void processMessage( Message message ) {
			this.messages.add( message );
		}
	}
}
