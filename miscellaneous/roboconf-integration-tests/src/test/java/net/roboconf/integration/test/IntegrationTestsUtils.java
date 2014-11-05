/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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

package net.roboconf.integration.test;

import java.io.IOException;

import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.processors.AbstractMessageProcessor;
import net.roboconf.messaging.reconfigurables.ReconfigurableClientDm;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IntegrationTestsUtils {

	/**
	 * Private constructor.
	 */
	private IntegrationTestsUtils() {
		// nothing
	}


	/**
	 * @return true if a local Rabbit MQ server is running
	 */
	public static boolean rabbitMqIsRunning() {

		// We cannot access RabbitMQ classes since they are internal to the messaging bundle.
		// And it is better to not duplicate the messaging configuration in this bundle.
		// So, let's rely on our messaging API classes.
		ReconfigurableClientDm client = new ReconfigurableClientDm();
		boolean result = false;
		try {
			client.associateMessageProcessor( new MyMessageProcessor());
			client.switchMessagingClient( "localhost", "guest", "guest", MessagingConstants.FACTORY_RABBIT_MQ );
			result = client.hasValidClient();

		} finally {
			client.getMessageProcessor().stopProcessor();
			try {
				client.closeConnection();

			} catch( IOException e ) {
				// nothing
			}
		}

		return result;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class MyMessageProcessor extends AbstractMessageProcessor<IDmClient> {
		public MyMessageProcessor() {
			super( "test thread" );
		}

		@Override
		protected void processMessage( Message message ) {
			// nothing
		}
	}
}
