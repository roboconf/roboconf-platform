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

package net.roboconf.messaging.rabbitmq.internal.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.internal.tests.TestUtils.StringHandler;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdGatherLogs;
import net.roboconf.messaging.api.utils.SerializationUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfReturnListenerTest {

	@Test
	public void testDeserializationError() throws Exception {

		RoboconfReturnListener listener = new RoboconfReturnListener();
		listener.handleReturn( 0, "reply", "exchange", "routingKey", null, new byte[ 1 ]);
	}


	@Test
	public void testLogging() throws Exception {

		final StringHandler logHandler = new StringHandler();
		RoboconfReturnListener listener = new RoboconfReturnListener();
		Logger logger = TestUtils.getInternalField( listener, "logger", Logger.class );

		Assert.assertNotNull( logger );
		logger.setUseParentHandlers( false );
		logger.addHandler( logHandler );

		Message msg = new MsgCmdGatherLogs();
		byte[] bytes = SerializationUtils.serializeObject( msg );

		// We have NOT the right logging level
		logger.setLevel( Level.SEVERE );
		listener.handleReturn( 0, "reply", "exchange", "routingKey", null, bytes );
		Assert.assertEquals( 0, logHandler.getLogs().length());

		// We have the right logging level
		logger.setLevel( Level.INFO );
		listener.handleReturn( 0, "reply", "exchange", "routingKey", null, bytes );
		Assert.assertNotSame( 0, logHandler.getLogs().length());
	}
}
