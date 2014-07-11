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

package net.roboconf.messaging.internal;

import java.io.IOException;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.utils.Utils;

import org.junit.Before;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractRabbitMqTest {

	private static final String MESSAGE_SERVER_IP = "127.0.0.1";
	protected boolean rabbitMqIsRunning = true;


	/**
	 * A method to check whether RabbitMQ is rabbitMqIsRunning or not.
	 * <p>
	 * Tests that must be skipped if it is not rabbitMqIsRunning must begin with
	 * <code>
	 * Assume.assumeTrue( rabbitMqIsRunning );
	 * </code>
	 * </p>
	 */
	@Before
	public void checkRabbitMQIsRunning() throws Exception {

		Channel channel = null;
		try {
			channel = createTestChannel();
			Object o = channel.getConnection().getServerProperties().get( "version" );

			String version = String.valueOf( o );
			if( ! isVersionGOEThreeDotTwo( version )) {
				Logger logger = Logger.getLogger( getClass().getName());
				logger.warning( "Tests are skipped because RabbitMQ must be at least in version 3.2.x." );

				this.rabbitMqIsRunning = false;
			}

		} catch( Exception e ) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.warning( "Tests are skipped because RabbitMQ is not rabbitMqIsRunning." );
			logger.finest( Utils.writeException( e ));

			this.rabbitMqIsRunning = false;

		} finally {
			if( channel != null ) {
				channel.close();
				channel.getConnection().close();
			}
		}
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
	 * Creates a channel to interact with a RabbitMQ server for tests.
	 * @return a non-null channel
	 * @throws IOException if the creation failed
	 */
	protected Channel createTestChannel() throws IOException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost( MESSAGE_SERVER_IP );
		Channel channel = factory.newConnection().createChannel();

		return channel;
	}
}
