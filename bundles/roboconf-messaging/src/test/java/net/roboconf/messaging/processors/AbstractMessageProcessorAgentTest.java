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

package net.roboconf.messaging.processors;

import java.io.IOException;

import junit.framework.Assert;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.messages.Message;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractMessageProcessorAgentTest {

	@Test
	public void testCreation_valid() throws Exception {

		String name = MessagingConstants.FACTORY_TEST;
		AbstractMessageProcessorAgent processor = new AbstractMessageProcessorAgent( name ) {

			@Override
			protected void processMessage( Message message ) {
				// nothing
			}

			@Override
			protected void openConnection( IAgentClient newMessagingClient ) throws IOException {
				// nothing
			}
		};

		String messageServerIp = "127.0.0.1";
		String messageServerUser = "me";
		String messageServerPwd = "123456789 (kidding)";
		IAgentClient client = processor.createNewMessagingClient( messageServerIp, messageServerUser, messageServerPwd );
		Assert.assertNotNull( client );
	}
}
