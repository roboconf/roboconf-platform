/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.internal;

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class RabbitMqTestUtils {

	private static final String MESSAGE_SERVER_IP = "127.0.0.1";


	/**
	 * Empty constructor.
	 */
	private RabbitMqTestUtils() {
		// nothing
	}


	/**
	 * A method to check whether RabbitMQ is rabbitMqIsRunning or not.
	 * <p>
	 * Tests that must be skipped if it is not rabbitMqIsRunning must begin with
	 * <code>
	 * Assume.assumeTrue( rabbitMqIsRunning );
	 * </code>
	 * </p>
	 */
	public static boolean checkRabbitMqIsRunning() throws Exception {

		boolean rabbitMqIsRunning = true;
		Channel channel = null;
		try {
			channel = createTestChannel();
			Object o = channel.getConnection().getServerProperties().get( "version" );

			String version = String.valueOf( o );
			if( ! isVersionGOEThreeDotTwo( version )) {
				Logger logger = Logger.getLogger( RabbitMqTestUtils.class.getName());
				logger.warning( "Tests are skipped because RabbitMQ must be at least in version 3.2.x." );

				rabbitMqIsRunning = false;
			}

		} catch( Exception e ) {
			Logger logger = Logger.getLogger( RabbitMqTestUtils.class.getName());
			logger.warning( "Tests are skipped because RabbitMQ is not rabbitMqIsRunning." );
			Utils.logException( logger, e );

			rabbitMqIsRunning = false;

		} finally {
			if( channel != null ) {
				channel.close();
				channel.getConnection().close();
			}
		}

		return rabbitMqIsRunning;
	}


	/**
	 * Checks that the RabbitMQ is greater or equal to 3.2.
	 * @param rabbitMqVersion the Rabbit MQ version
	 * @return true if it is at least a version 3.2, false otherwise
	 */
	static boolean isVersionGOEThreeDotTwo( String rabbitMqVersion ) {

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
	 * Creates a channel to interact with a RabbitMQ server for tests.
	 * @return a non-null channel
	 * @throws IOException if the creation failed
	 */
	public static Channel createTestChannel() throws IOException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost( MESSAGE_SERVER_IP );
		Channel channel = factory.newConnection().createChannel();

		return channel;
	}
}
