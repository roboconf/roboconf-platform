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

package net.roboconf.messaging.client;

import junit.framework.Assert;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AbstractMessageProcessorTest {

	@Test
	public void testCustomName() {

		AbstractMessageProcessor processor = new AbstractMessageProcessor( "yo" ) {
			@Override
			protected void processMessage( Message message ) {
				// nothing
			}
		};

		Assert.assertEquals( "yo", processor.getName());
		Assert.assertTrue( processor.hasNoMessage());

		processor.storeMessage( new MsgCmdSendInstances());
		Assert.assertFalse( processor.hasNoMessage());
	}
}
