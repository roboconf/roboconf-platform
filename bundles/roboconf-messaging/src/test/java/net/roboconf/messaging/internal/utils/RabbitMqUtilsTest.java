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

package net.roboconf.messaging.internal.utils;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.internal.AbstractRabbitMqTest;
import net.roboconf.messaging.messages.Message;

import org.junit.Assume;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqUtilsTest extends AbstractRabbitMqTest {

	@Test
	public void testBuildExchangeName_String() {

		Assert.assertNotNull( RabbitMqUtils.buildExchangeName( "app", true ));
		Assert.assertNotNull( RabbitMqUtils.buildExchangeName( "app", false ));
		Assert.assertNotSame(
				RabbitMqUtils.buildExchangeName( "app", false ),
				RabbitMqUtils.buildExchangeName( "app", true ));

		Assert.assertNotSame(
				RabbitMqUtils.buildExchangeName( "app1", false ),
				RabbitMqUtils.buildExchangeName( "app2", false ));

		Assert.assertNotSame(
				RabbitMqUtils.buildExchangeName( "app1", true ),
				RabbitMqUtils.buildExchangeName( "app2", true ));
	}


	@Test
	public void testBuildExchangeName_Application() {
		Application app = new Application( "my-app" );

		Assert.assertNotNull( RabbitMqUtils.buildExchangeName( app, true ));
		Assert.assertNotNull( RabbitMqUtils.buildExchangeName( app, false ));

		Assert.assertEquals(
				RabbitMqUtils.buildExchangeName( app, false ),
				RabbitMqUtils.buildExchangeName( app.getName(), false ));

		Assert.assertEquals(
				RabbitMqUtils.buildExchangeName( app, true),
				RabbitMqUtils.buildExchangeName( app.getName(), true ));
	}


	@Test
	public void testBuildRoutingKeyForAgent_String() {

		Assert.assertNotNull( RabbitMqUtils.buildRoutingKeyForAgent( "root" ));
		Assert.assertNotSame(
				RabbitMqUtils.buildRoutingKeyForAgent( "root1" ),
				RabbitMqUtils.buildRoutingKeyForAgent( "root2" ));
	}


	@Test
	public void testBuildRoutingKeyForAgent_Instance() {
		Instance inst = new Instance( "my-root" );

		Assert.assertNotNull( RabbitMqUtils.buildRoutingKeyForAgent( inst ));
		Assert.assertEquals(
				RabbitMqUtils.buildRoutingKeyForAgent( inst ),
				RabbitMqUtils.buildRoutingKeyForAgent( inst.getName()));

		Instance childInstance = new Instance( "child" );
		InstanceHelpers.insertChild( inst, childInstance );
		Assert.assertEquals(
				RabbitMqUtils.buildRoutingKeyForAgent( childInstance ),
				RabbitMqUtils.buildRoutingKeyForAgent( inst ));
	}


	@Test
	public void testFindUrlAndPort() throws Exception {

		Map.Entry<String,Integer> entry = RabbitMqUtils.findUrlAndPort( "http://localhost" );
		Assert.assertEquals( "http://localhost", entry.getKey());
		Assert.assertEquals( -1, entry.getValue().intValue());

		entry = RabbitMqUtils.findUrlAndPort( "http://localhost:9989" );
		Assert.assertEquals( "http://localhost", entry.getKey());
		Assert.assertEquals( 9989, entry.getValue().intValue());

		entry = RabbitMqUtils.findUrlAndPort( "http://roboconf.net/some/arbitrary/path" );
		Assert.assertEquals( "http://roboconf.net/some/arbitrary/path", entry.getKey());
		Assert.assertEquals( -1, entry.getValue().intValue());

		entry = RabbitMqUtils.findUrlAndPort( "http://roboconf.net:2727/some/arbitrary/path" );
		Assert.assertEquals( "http://roboconf.net/some/arbitrary/path", entry.getKey());
		Assert.assertEquals( 2727, entry.getValue().intValue());

		File f = new File( System.getProperty( "java.io.tmpdir" ));
		entry = RabbitMqUtils.findUrlAndPort( f.toURI().toString());
		Assert.assertEquals( f.toURI(), new URI( entry.getKey()));
		Assert.assertEquals( -1, entry.getValue().intValue());

		entry = RabbitMqUtils.findUrlAndPort( "ftp://some.host.com:4811/path" );
		Assert.assertEquals( "ftp://some.host.com/path", entry.getKey());
		Assert.assertEquals( 4811, entry.getValue().intValue());
	}


	@Test
	public void testConfigureFactory() throws Exception {

		String address = "http://roboconf.net/some/path";
		int port = 18547;
		String username = "toto";
		String password= "123456789";

		ConnectionFactory factory = new ConnectionFactory();
		Assert.assertNotSame( address, factory.getHost());
		Assert.assertNotSame( port, factory.getPort());

		RabbitMqUtils.configureFactory( factory, "http://roboconf.net:" + port + "/some/path", username, password );
		Assert.assertEquals( address, factory.getHost());
		Assert.assertEquals( port, factory.getPort());
		Assert.assertEquals( username, factory.getUsername());
		Assert.assertEquals( password, factory.getPassword());
	}


	@Test
	public void testCloseConnection_withNull() throws Exception {
		RabbitMqUtils.closeConnection( null );
	}


	@Test
	public void testCloseConnection() throws Exception {
		Assume.assumeTrue( this.rabbitMqIsRunning );

		Channel channel = createTestChannel();
		Assert.assertTrue( channel.isOpen());
		Assert.assertTrue( channel.getConnection().isOpen());

		// Close it
		RabbitMqUtils.closeConnection( channel );
		Assert.assertFalse( channel.isOpen());
		Assert.assertFalse( channel.getConnection().isOpen());

		// Make sure closing an already closed channel does not throw an exception
		RabbitMqUtils.closeConnection( channel );
		Assert.assertFalse( channel.isOpen());
		Assert.assertFalse( channel.getConnection().isOpen());
	}


	@Test( timeout = 2000 )
	public void testListenToRabbitMq_rabbitExceptions() throws Exception {

		// In these tests, Rabbit exceptions break the processing loop
		Channel channel = createTestChannel();
		Logger logger = Logger.getLogger( getClass().getName());
		AbstractMessageProcessor messageProcessor = new AbstractMessageProcessor() {
			@Override
			protected void processMessage( Message message ) {
				// nothing
			}
		};

		// Shutdown
		QueueingConsumer consumer = new QueueingConsumer( channel ) {
			@Override
			public Delivery nextDelivery()
			throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
				throw new ShutdownSignalException( false, false, null, "for tests" );
			}
		};

		RabbitMqUtils.listenToRabbitMq( "whatever", logger, consumer, messageProcessor );

		// Interrupted
		consumer = new QueueingConsumer( channel ) {
			@Override
			public Delivery nextDelivery()
			throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
				throw new InterruptedException( "for tests" );
			}
		};

		RabbitMqUtils.listenToRabbitMq( "whatever", logger, consumer, messageProcessor );

		// Consumer
		consumer = new QueueingConsumer( channel ) {
			@Override
			public Delivery nextDelivery()
			throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
				throw new ConsumerCancelledException();
			}
		};

		RabbitMqUtils.listenToRabbitMq( "whatever", logger, consumer, messageProcessor );
	}
}
