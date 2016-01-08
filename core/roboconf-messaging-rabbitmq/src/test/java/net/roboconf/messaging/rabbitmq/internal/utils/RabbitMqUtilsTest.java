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

package net.roboconf.messaging.rabbitmq.internal.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.junit.Assert;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.extensions.MessagingContext.ThoseThat;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RabbitMqUtilsTest {

	private static boolean rabbitMqIsRunning = false;

	@BeforeClass
	public static void checkRabbitMqIsRunning() throws Exception {
		rabbitMqIsRunning = RabbitMqTestUtils.checkRabbitMqIsRunning();
	}


	@Test
	public void testDeclareApplicationExchanges() throws Exception {

		Channel channel = Mockito.mock( Channel.class );
		RabbitMqUtils.declareApplicationExchanges( null, channel );
		Mockito.verifyZeroInteractions( channel );

		RabbitMqUtils.declareApplicationExchanges( "te", channel );
		String exchangeName = RabbitMqUtils.buildExchangeNameForAgent( "te" );
		Mockito.verify( channel, Mockito.times( 1 )).exchangeDeclare( exchangeName, "topic" );
	}


	@Test
	public void testDeclareGlobalExchanges() throws Exception {

		Channel channel = Mockito.mock( Channel.class );
		RabbitMqUtils.declareGlobalExchanges( channel );

		Mockito.verify( channel, Mockito.times( 1 )).exchangeDeclare( RabbitMqConstants.EXHANGE_DM, "topic" );
		Mockito.verify( channel, Mockito.times( 1 )).exchangeDeclare( RabbitMqConstants.EXHANGE_INTER_APP, "topic" );
	}


	@Test
	public void testBuildExchangeNameForAgent() {

		Assert.assertEquals( "test.agents", RabbitMqUtils.buildExchangeNameForAgent( "test" ));
		Assert.assertEquals( "te.agents", RabbitMqUtils.buildExchangeNameForAgent( "te" ));
	}


	@Test
	public void testBuildExchangeName() {

		MessagingContext ctx = new MessagingContext( RecipientKind.DM, "app1" );
		Assert.assertEquals( RabbitMqConstants.EXHANGE_DM, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.DM, "app2" );
		Assert.assertEquals( RabbitMqConstants.EXHANGE_DM, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.INTER_APP, "app1" );
		Assert.assertEquals( RabbitMqConstants.EXHANGE_INTER_APP, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.INTER_APP, "facet", ThoseThat.IMPORT, "app1" );
		Assert.assertEquals( RabbitMqConstants.EXHANGE_INTER_APP, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.AGENTS, "facet", ThoseThat.IMPORT, "app1" );
		Assert.assertEquals( "app1.agents", RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.AGENTS, "facet", ThoseThat.EXPORT, "app2" );
		Assert.assertEquals( "app2.agents", RabbitMqUtils.buildExchangeName( ctx ));
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
	public void testConfigureFactory_nullIp() throws Exception {

		ConnectionFactory factory = new ConnectionFactory();
		String username = "toto";
		String password= "123456789";

		RabbitMqUtils.configureFactory( factory, null, username, password );
		Assert.assertEquals( username, factory.getUsername());
		Assert.assertEquals( password, factory.getPassword());
	}


	@Test
	public void testCloseConnection_withNull() throws Exception {
		RabbitMqUtils.closeConnection( null );
	}


	@Test
	public void testCloseConnection() throws Exception {
		Assume.assumeTrue( rabbitMqIsRunning );

		Channel channel = RabbitMqTestUtils.createTestChannel();
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
		Assume.assumeTrue( rabbitMqIsRunning );

		// In these tests, Rabbit exceptions break the processing loop
		Channel channel = RabbitMqTestUtils.createTestChannel();
		Logger logger = Logger.getLogger( getClass().getName());
		LinkedBlockingQueue<Message> messgesQueue = new LinkedBlockingQueue<>();

		// Shutdown
		QueueingConsumer consumer = new QueueingConsumer( channel ) {
			@Override
			public Delivery nextDelivery()
			throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
				throw new ShutdownSignalException( false, false, null, "for tests" );
			}
		};

		RabbitMqUtils.listenToRabbitMq( "whatever", logger, consumer, messgesQueue );

		// Interrupted
		consumer = new QueueingConsumer( channel ) {
			@Override
			public Delivery nextDelivery()
			throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
				throw new InterruptedException( "for tests" );
			}
		};

		RabbitMqUtils.listenToRabbitMq( "whatever", logger, consumer, messgesQueue );

		// Consumer
		consumer = new QueueingConsumer( channel ) {
			@Override
			public Delivery nextDelivery()
			throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
				throw new ConsumerCancelledException();
			}
		};

		RabbitMqUtils.listenToRabbitMq( "whatever", logger, consumer, messgesQueue );
	}
}
