/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.EXCHANGE_DM;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.EXCHANGE_INTER_APP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_IP;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_PASSWORD;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SERVER_USERNAME;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_AS_USER_DATA;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PASSPHRASE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_KEY_STORE_PATH;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PASSPHRASE;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_SSL_TRUST_STORE_PATH;
import static net.roboconf.messaging.rabbitmq.RabbitMqConstants.RABBITMQ_USE_SSL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import net.roboconf.core.utils.Utils;
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



	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


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

		Mockito.verify( channel, Mockito.times( 1 )).exchangeDeclare( "d." + EXCHANGE_DM, "topic" );
		Mockito.verify( channel, Mockito.times( 1 )).exchangeDeclare( "d." + EXCHANGE_INTER_APP, "topic" );
	}


	@Test
	public void testBuildExchangeNameForAgent() {

		Assert.assertEquals( "d1.test.agents", RabbitMqUtils.buildExchangeNameForAgent( "d1", "test" ));
		Assert.assertEquals( "d2.te.agents", RabbitMqUtils.buildExchangeNameForAgent( "d2", "te" ));
	}


	@Test
	public void testBuildExchangeName() {

		MessagingContext ctx = new MessagingContext( RecipientKind.DM, "domain", "app1" );
		Assert.assertEquals( "domain." + EXCHANGE_DM, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.DM, "domain", "app2" );
		Assert.assertEquals( "domain." + EXCHANGE_DM, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "app1" );
		Assert.assertEquals( "domain." + EXCHANGE_INTER_APP, RabbitMqUtils.buildExchangeName( ctx ));

		ctx = new MessagingContext( RecipientKind.INTER_APP, "domain", "facet", ThoseThat.IMPORT, "app1" );
		Assert.assertEquals( "domain." + EXCHANGE_INTER_APP, RabbitMqUtils.buildExchangeName( ctx ));

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

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "http://roboconf.net:" + port + "/some/path" );
		configuration.put( RABBITMQ_SERVER_USERNAME, username );
		configuration.put( RABBITMQ_SERVER_PASSWORD, password );

		RabbitMqUtils.configureFactory( factory, configuration );
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

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, null );
		configuration.put( RABBITMQ_SERVER_USERNAME, username );
		configuration.put( RABBITMQ_SERVER_PASSWORD, password );

		RabbitMqUtils.configureFactory( factory, configuration );
		Assert.assertEquals( username, factory.getUsername());
		Assert.assertEquals( password, factory.getPassword());
	}


	@Test
	public void testConfigureFactory_withoutSslEnabled() throws Exception {

		int port = 18547;
		ConnectionFactory factory = Mockito.mock( ConnectionFactory.class );

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "http://roboconf.net:" + port );
		configuration.put( RABBITMQ_SERVER_USERNAME, "toto" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "123456789" );
		configuration.put( RABBITMQ_SSL_KEY_STORE_PATH, this.folder.newFile().getAbsolutePath());
		configuration.put( RABBITMQ_SSL_KEY_STORE_PASSPHRASE, "key store" );
		configuration.put( RABBITMQ_SSL_TRUST_STORE_PATH, this.folder.newFile().getAbsolutePath());
		configuration.put( RABBITMQ_SSL_TRUST_STORE_PASSPHRASE, "trust store" );
		configuration.put( RABBITMQ_SSL_AS_USER_DATA, "true" );

		RabbitMqUtils.configureFactory( factory, configuration );

		Mockito.verify( factory ).setUsername( "toto" );
		Mockito.verify( factory ).setPassword( "123456789" );
		Mockito.verify( factory ).setHost( "http://roboconf.net" );
		Mockito.verify( factory ).setPort( port );

		Mockito.verify( factory ).setConnectionTimeout( Mockito.anyInt());
		Mockito.verify( factory ).setAutomaticRecoveryEnabled( true );
		Mockito.verify( factory ).setNetworkRecoveryInterval( Mockito.anyInt());
		Mockito.verify( factory ).setTopologyRecoveryEnabled( true );

		Mockito.verifyZeroInteractions( factory );
	}


	@Test
	public void testConfigureFactory_withSslEnabled() throws Exception {

		int port = 18547;
		ConnectionFactory factory = Mockito.mock( ConnectionFactory.class );

		String keyPassPhrase = "key store";
		File keyStore = generateKeyStore( keyPassPhrase, RabbitMqConstants.DEFAULT_SSL_KEY_STORE_TYPE );

		String trustPassPhrase = "trust store";
		File trustStore = generateKeyStore( trustPassPhrase, RabbitMqConstants.DEFAULT_SSL_TRUST_STORE_TYPE );

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "http://roboconf.net:" + port );
		configuration.put( RABBITMQ_SERVER_USERNAME, "toto" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "123456789" );
		configuration.put( RABBITMQ_SSL_KEY_STORE_PATH, keyStore.getAbsolutePath());
		configuration.put( RABBITMQ_SSL_KEY_STORE_PASSPHRASE, keyPassPhrase );
		configuration.put( RABBITMQ_SSL_TRUST_STORE_PATH, trustStore.getAbsolutePath());
		configuration.put( RABBITMQ_SSL_TRUST_STORE_PASSPHRASE, trustPassPhrase );
		configuration.put( RABBITMQ_SSL_AS_USER_DATA, "true" );
		configuration.put( RABBITMQ_USE_SSL, "true" );

		RabbitMqUtils.configureFactory( factory, configuration );

		Mockito.verify( factory ).setUsername( "toto" );
		Mockito.verify( factory ).setPassword( "123456789" );
		Mockito.verify( factory ).setHost( "http://roboconf.net" );
		Mockito.verify( factory ).setPort( port );

		Mockito.verify( factory ).setConnectionTimeout( Mockito.anyInt());
		Mockito.verify( factory ).setAutomaticRecoveryEnabled( true );
		Mockito.verify( factory ).setNetworkRecoveryInterval( Mockito.anyInt());
		Mockito.verify( factory ).setTopologyRecoveryEnabled( true );

		Mockito.verify( factory ).useSslProtocol( Mockito.any( SSLContext.class ));
		Mockito.verifyZeroInteractions( factory );
	}


	@Test( expected = IOException.class )
	public void testConfigureFactory_withSslEnabled_ioException() throws Exception {

		ConnectionFactory factory = Mockito.mock( ConnectionFactory.class );

		String trustPassPhrase = "trust store";
		File trustStore = generateKeyStore( trustPassPhrase, RabbitMqConstants.DEFAULT_SSL_TRUST_STORE_TYPE );

		Map<String,String> configuration = new HashMap<> ();
		configuration.put( RABBITMQ_SERVER_IP, "http://roboconf.net:18547/some/path" );
		configuration.put( RABBITMQ_SERVER_USERNAME, "toto" );
		configuration.put( RABBITMQ_SERVER_PASSWORD, "123456789" );
		configuration.put( RABBITMQ_SSL_KEY_STORE_PATH, "does not exist" );
		configuration.put( RABBITMQ_SSL_KEY_STORE_PASSPHRASE, "key store" );
		configuration.put( RABBITMQ_SSL_TRUST_STORE_PATH, trustStore.getAbsolutePath());
		configuration.put( RABBITMQ_SSL_TRUST_STORE_PASSPHRASE, trustPassPhrase );
		configuration.put( RABBITMQ_SSL_AS_USER_DATA, "true" );
		configuration.put( RABBITMQ_USE_SSL, "true" );

		RabbitMqUtils.configureFactory( factory, configuration );
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


	/**
	 * Generates an empty key store.
	 * @param passPhrase the pass phrase (not null)
	 * @param type the type of the store
	 * @return the key store file
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	private File generateKeyStore( String passPhrase, String type ) throws IOException, GeneralSecurityException {

		File result = this.folder.newFile();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream( result.getAbsolutePath());
			KeyStore ks = KeyStore.getInstance( type );
			ks.load( null, null );
			ks.store( fos, passPhrase.toCharArray());

		} finally {
			Utils.closeQuietly( fos );
		}

		Assert.assertTrue( result.isFile());
		return result;
	}
}
