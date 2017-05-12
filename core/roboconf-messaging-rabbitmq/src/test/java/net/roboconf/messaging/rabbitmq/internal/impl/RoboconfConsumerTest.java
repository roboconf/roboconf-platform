/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.internal.tests.TestUtils.StringHandler;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfConsumerTest {

	@Test
	public void testBasicLogging_forCodeCoverage() throws Exception {

		RoboconfMessageQueue messageQueue = new RoboconfMessageQueue();
		Channel channel = Mockito.mock( Channel.class );
		RoboconfConsumer rc = new RoboconfConsumer( "DM", channel, messageQueue );

		rc.handleCancel( "tag" );
		rc.handleCancelOk( "tag" );
	}


	@Test
	public void testShutdownSignal() throws Exception {

		RoboconfMessageQueue messageQueue = new RoboconfMessageQueue();
		Channel channel = Mockito.mock( Channel.class );
		RoboconfConsumer rc = new RoboconfConsumer( "DM", channel, messageQueue );

		final StringHandler logHandler = new StringHandler();
		Logger logger = TestUtils.getInternalField( rc, "logger", Logger.class );
		logger.setUseParentHandlers( false );
		logger.setLevel( Level.FINE );
		logger.addHandler( logHandler );

		// Depending on the shutdown signal, we do not log the same message.
		ShutdownSignalException sig = Mockito.mock( ShutdownSignalException.class );
		Mockito.when( sig.isInitiatedByApplication()).thenReturn( true );
		rc.handleShutdownSignal( "tag", sig );
		String log1 = logHandler.getLogs();

		// Not initiated by the application, but the source is a channel.
		Mockito.reset( sig );
		Mockito.when( sig.isInitiatedByApplication()).thenReturn( false );
		Mockito.when( sig.getReference()).thenReturn( channel );
		logHandler.getStringBuilder().setLength( 0 );

		rc.handleShutdownSignal( "tag", sig );
		String log2 = logHandler.getLogs();

		// Not initiated by the application and the source is not a channel.
		Mockito.reset( sig );
		Mockito.when( sig.isInitiatedByApplication()).thenReturn( false );
		logHandler.getStringBuilder().setLength( 0 );

		rc.handleShutdownSignal( "tag", sig );
		String log3 = logHandler.getLogs();

		// The 3 messages should be different
		Assert.assertFalse( Utils.isEmptyOrWhitespaces( log1 ));
		Assert.assertFalse( Utils.isEmptyOrWhitespaces( log2 ));
		Assert.assertFalse( Utils.isEmptyOrWhitespaces( log3 ));

		Assert.assertFalse( log1.equals( log2 ));
		Assert.assertFalse( log1.equals( log3 ));
		Assert.assertFalse( log3.equals( log2 ));
	}


	@Test
	public void testHandleDeliveryWithDeserializationError() throws Exception {

		RoboconfMessageQueue messageQueue = new RoboconfMessageQueue();
		Channel channel = Mockito.mock( Channel.class );
		RoboconfConsumer rc = new RoboconfConsumer( "DM", channel, messageQueue );

		final StringHandler logHandler = new StringHandler();
		Logger logger = TestUtils.getInternalField( rc, "logger", Logger.class );
		logger.setUseParentHandlers( false );
		logger.addHandler( logHandler );

		rc.handleDelivery( "tag", Mockito.mock( Envelope.class ), null, new byte[ 1 ]);
		Assert.assertTrue( logHandler.getLogs().startsWith( "DM: a message could not be deserialized." ));
	}
}
