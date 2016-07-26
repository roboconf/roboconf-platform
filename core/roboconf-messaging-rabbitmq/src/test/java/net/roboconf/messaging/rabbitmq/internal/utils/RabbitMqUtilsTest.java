/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.extensions.MessagingContext.ThoseThat;
import net.roboconf.messaging.rabbitmq.RabbitMqConstants;

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
		RabbitMqUtils.declareApplicationExchanges( null, null, channel );
		Mockito.verifyZeroInteractions( channel );

		RabbitMqUtils.declareApplicationExchanges( "domain", "te", channel );
		String exchangeName = RabbitMqUtils.buildExchangeNameForAgent( "domain", "te" );
		Mockito.verify( channel, Mockito.times( 1 )).exchangeDeclare( exchangeName, "topic" );
	}


	@Test
	public void testDeclareGlobalExchanges() throws Exception {

		Channel channel = Mockito.mock( Channel.class );
		RabbitMqUtils.declareGlobalExchanges( "d", channel );

		Mockito.verify( channel, Mockito.times( 1 )).exchangeDeclare( "d." + RabbitMqConstants.EXCHANGE_DM, "topic" );
		Mockito.verify( channel, Mockito.times( 1 )).exchangeDeclare( "d." + RabbitMqConstants.EXCHANGE_INTER_APP, "topic" );
	}


	@Test
	public void testBuildExchangeNameForAgent() {

		Assert.assertEquals( "d1.test.agents", RabbitMqUtils.buildExchangeNameForAgent( "d1", "test" ));
		Assert.assertEquals( "d2.te.agents", RabbitMqUtils.buildExchangeNameForAgent( "d2", "te" ));
	}


	@Test
	public void testBuildExchangeName() {

		MessagingContext ctx = new MessagingContext( RecipientKind.DM, "domain", "app1" );
		Assert.assertEquals( "domain." + RabbitMqConstants.EXCHANGE_DM, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.DM, "domain", "app2" );
		Assert.assertEquals( "domain." + RabbitMqConstants.EXCHANGE_DM, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "app1" );
		Assert.assertEquals( "domain." + RabbitMqConstants.EXCHANGE_INTER_APP, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.IMPORT, "app1" );
		Assert.assertEquals( "domain." + RabbitMqConstants.EXCHANGE_INTER_APP, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain", "facet", ThoseThat.IMPORT, "app1" );
		Assert.assertEquals( "domain.app1.agents", RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.AGENTS, "domain1", "facet", ThoseThat.EXPORT, "app2" );
		Assert.assertEquals( "domain1.app2.agents", RabbitMqUtils.buildExchangeName( ctx ));
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
}
