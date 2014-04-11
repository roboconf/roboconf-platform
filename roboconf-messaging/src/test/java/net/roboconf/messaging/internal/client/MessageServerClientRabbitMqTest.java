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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.utils.MessagingUtils;

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
 * <p>
 * Notice that we an error collector to continue tests even when
 * there are errors. This way, we can clean the server for the next tests.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class MessageServerClientRabbitMqTest {

	private static final long DELAY = 700;
	private static final String MESSAGE_SERVER_IP = "127.0.0.1";

	@Rule
	public ErrorCollector collector = new ErrorCollector();
	private boolean running = true;


	/**
	 * A method to check whether RabbitMQ is running or not.
	 * <p>
	 * If it is not running, tests in this class will be skipped.
	 * </p>
	 */
	@Before
	public void checkRabbitMQIsRunning() throws Exception {

		Connection connection = null;
		Channel channel = null;
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost( MESSAGE_SERVER_IP );
			connection = factory.newConnection();
			channel = connection.createChannel();

			Object o = connection.getServerProperties().get( "version" );
			String version = String.valueOf( o );
			if( ! isVersionGOEThreeDotTwo( version )) {
				Logger logger = Logger.getLogger( getClass().getName());
				logger.warning( "Tests are skipped because RabbitMQ must be at least in version 3.2.x." );

				this.running = false;
			}

		} catch( Exception e ) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.warning( "Tests are skipped because RabbitMQ is not running." );
			logger.finest( Utils.writeException( e ));

			this.running = false;

		} finally {
			if( channel != null )
				channel.close();

			if( connection != null )
				connection.close();
		}
	}


	@Test
	public void closeConnectionShouldSupportNull() throws Exception {
		Assume.assumeTrue( this.running );

		MessageServerClientRabbitMq client = new MessageServerClientRabbitMq();
		this.collector.checkThat( client.channel, nullValue());
		this.collector.checkThat( client.connection, nullValue());

		client.closeConnection();
	}


	@Test
	public void openAndCloseConnectionShouldWork() throws Exception {
		Assume.assumeTrue( this.running );

		MessageServerClientRabbitMq client = new MessageServerClientRabbitMq();
		client.setMessageServerIp( MESSAGE_SERVER_IP );
		client.setApplicationName( "my-app" );
		client.setSourceName( "the-agent" );

		this.collector.checkThat( client.channel, nullValue());
		this.collector.checkThat( client.connection, nullValue());
		this.collector.checkThat( client.consumerTag, nullValue());

		client.openConnection( new TestMessageProcessor ());

		this.collector.checkThat( client.channel, notNullValue());
		this.collector.checkThat( client.connection, notNullValue());
		this.collector.checkThat( client.consumerTag, notNullValue());

		this.collector.checkThat( client.channel.isOpen(), is( true ));
		this.collector.checkThat( client.connection.isOpen(), is( true ));

		client.closeConnection();

		this.collector.checkThat( client.channel, nullValue());
		this.collector.checkThat( client.connection, nullValue());
		this.collector.checkThat( client.consumerTag, nullValue());
	}


	@Test
	public void dmAndAgentCommunicationShouldWork() throws Exception {
		Assume.assumeTrue( this.running );

		// Create several clients for the agents...
		final int agentsCount = 5;
		this.collector.checkThat( agentsCount, greaterThan( 2 ));	// We use indexes 0 and 1 farther

		TestMessageProcessor[] agentProcessors = new TestMessageProcessor[ agentsCount ];
		MessageServerClientRabbitMq[] agentClients = new MessageServerClientRabbitMq[ agentsCount ];

		for( int i=0; i<agentsCount; i++ ) {
			agentClients[ i ] = new MessageServerClientRabbitMq();
			agentClients[ i ].setMessageServerIp( MESSAGE_SERVER_IP );
			agentClients[ i ].setApplicationName( "my-app" );
			agentClients[ i ].setSourceName( "agent-" + i );

			agentProcessors[ i ] = new TestMessageProcessor ();
			agentClients[ i ].openConnection( agentProcessors[ i ]);
		}

		// ... and one for the DM
		TestMessageProcessor dmProcessor = new TestMessageProcessor ();
		MessageServerClientRabbitMq dmClient = new MessageServerClientRabbitMq();
		dmClient.setMessageServerIp( MESSAGE_SERVER_IP );
		dmClient.setApplicationName( "my-app" );
		dmClient.setSourceName( MessagingUtils.SOURCE_DM );
		dmClient.openConnection( dmProcessor );

		// Check the initial state
		this.collector.checkThat( dmProcessor.messages, empty());
		this.collector.checkThat( dmClient.connected, is( true ));

		for( int i=0; i<agentsCount; i++ ) {
			this.collector.checkThat( agentProcessors[ i ].messages, empty());
			this.collector.checkThat( agentClients[ i ].connected, is( true ));
		}

		// The DM publishes a message - but no routing key was defined anywhere.
		dmClient.publish( false, "my-routing-key", new MsgNotifHeartbeat( "" ));
		this.collector.checkThat( dmProcessor.messages, empty());

		// Same test, but configure the binding first on a single agent
		agentClients[ 0 ].bind( "my-routing-key" );
		dmClient.publish( false, "my-routing-key", new MsgNotifHeartbeat( "" ));

		Thread.sleep( DELAY );	// Wait the message is delivered
		this.collector.checkThat( dmProcessor.messages, empty());
		for( int i=1; i<agentsCount; i++ )
			this.collector.checkThat( agentProcessors[ i ].messages, empty());

		this.collector.checkThat( agentProcessors[ 0 ].messages.size(), is( 1 ));
		this.collector.checkThat( agentProcessors[ 0 ].messages.get( 0 ), instanceOf( MsgNotifHeartbeat.class ));

		// Same test, but add a new binding on the 2 first agents
		agentClients[ 0 ].bind( "another-routing-key" );
		agentClients[ 1 ].bind( "another-routing-key" );
		dmClient.publish( false, "another-routing-key", new MsgNotifHeartbeat( "" ));

		Thread.sleep( DELAY );	// Wait the message is delivered
		this.collector.checkThat( dmProcessor.messages, empty());
		for( int i=2; i<agentsCount; i++ )
			this.collector.checkThat( agentProcessors[ i ].messages, empty());

		this.collector.checkThat( agentProcessors[ 0 ].messages.size(), is( 2 ));
		this.collector.checkThat( agentProcessors[ 1 ].messages.size(), is( 1 ));

		// Eventually, remove a binding and re-run the experience
		agentClients[ 1 ].unbind( "another-routing-key" );
		dmClient.publish( false, "another-routing-key", new MsgNotifHeartbeat( "" ));

		Thread.sleep( DELAY );	// Wait the message is delivered
		this.collector.checkThat( dmProcessor.messages, empty());
		for( int i=2; i<agentsCount; i++ )
			this.collector.checkThat( agentProcessors[ i ].messages, empty());

		this.collector.checkThat( agentProcessors[ 0 ].messages.size(), is( 3 ));
		this.collector.checkThat( agentProcessors[ 1 ].messages.size(), is( 1 ));

		// Close the connections
		dmClient.closeConnection();
		for( int i=0; i<agentsCount; i++ )
			agentClients[ i ].closeConnection();
	}


	@Test
	public void testIsVersionGreaterThanThreeDotTwo() {

		Assert.assertTrue( isVersionGOEThreeDotTwo( "3.2" ));
		Assert.assertTrue( isVersionGOEThreeDotTwo( "3.2.1" ));
		Assert.assertTrue( isVersionGOEThreeDotTwo( "3.3" ));
		Assert.assertTrue( isVersionGOEThreeDotTwo( "4.2" ));

		Assert.assertFalse( isVersionGOEThreeDotTwo( "3.1" ));
		Assert.assertFalse( isVersionGOEThreeDotTwo( "3.1.3" ));
		Assert.assertFalse( isVersionGOEThreeDotTwo( "3.0" ));
		Assert.assertFalse( isVersionGOEThreeDotTwo( "2.1" ));

		Assert.assertFalse( isVersionGOEThreeDotTwo( "whatever" ));
	}



	/**
	 * Checks that the RabbitMQ is greater or equal to 3.2.
	 * @param rabbitMqVersion the Rabbit MQ version
	 * @return true if it is at least a version 3.2, false otherwise
	 */
	private boolean isVersionGOEThreeDotTwo( String rabbitMqVersion ) {

		String[] digits = rabbitMqVersion.split( "\\." );
		boolean result = false;
		try {
			result = Integer.parseInt( digits[ 0 ]) == 3
					&& Integer.parseInt( digits[ 1 ]) >= 2
					|| Integer.parseInt( digits[ 0 ]) > 3;

		} catch( NumberFormatException e ) {
			// nothing
		}

		return result;
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
