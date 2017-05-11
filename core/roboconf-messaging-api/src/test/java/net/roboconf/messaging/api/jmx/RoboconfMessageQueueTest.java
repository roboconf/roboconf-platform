/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.jmx;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfMessageQueueTest {

	@Test
	public void testMetrics() throws Exception {

		RoboconfMessageQueue queue = new RoboconfMessageQueue();
		Assert.assertEquals( 0, queue.getFailedReceptionCount());
		Assert.assertEquals( 0, queue.getReceivedMessagesCount());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceivedMessage());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceptionFailure());

		queue.add( Mockito.mock( Message.class ));
		Assert.assertEquals( 0, queue.getFailedReceptionCount());
		Assert.assertEquals( 1, queue.getReceivedMessagesCount());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceptionFailure());

		long timestampOfLastReceivedMessage = 0;
		Assert.assertTrue( timestampOfLastReceivedMessage <= queue.getTimestampOfLastReceivedMessage());
		timestampOfLastReceivedMessage = queue.getTimestampOfLastReceivedMessage();

		queue.addAll( Arrays.asList( Mockito.mock( Message.class )));
		Assert.assertEquals( 0, queue.getFailedReceptionCount());
		Assert.assertEquals( 2, queue.getReceivedMessagesCount());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceptionFailure());

		Assert.assertTrue( timestampOfLastReceivedMessage <= queue.getTimestampOfLastReceivedMessage());
		timestampOfLastReceivedMessage = queue.getTimestampOfLastReceivedMessage();

		queue.addAll( Arrays.asList( Mockito.mock( Message.class ), Mockito.mock( Message.class ), Mockito.mock( Message.class )));
		Assert.assertEquals( 0, queue.getFailedReceptionCount());
		Assert.assertEquals( 5, queue.getReceivedMessagesCount());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceptionFailure());

		Assert.assertTrue( timestampOfLastReceivedMessage <= queue.getTimestampOfLastReceivedMessage());
		timestampOfLastReceivedMessage = queue.getTimestampOfLastReceivedMessage();

		queue.offer( Mockito.mock( Message.class ));
		Assert.assertEquals( 0, queue.getFailedReceptionCount());
		Assert.assertEquals( 6, queue.getReceivedMessagesCount());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceptionFailure());

		Assert.assertTrue( timestampOfLastReceivedMessage <= queue.getTimestampOfLastReceivedMessage());
		timestampOfLastReceivedMessage = queue.getTimestampOfLastReceivedMessage();

		queue.errorWhileReceivingMessage();
		Assert.assertEquals( 1, queue.getFailedReceptionCount());
		Assert.assertEquals( 6, queue.getReceivedMessagesCount());
		Assert.assertNotEquals( 0, queue.getTimestampOfLastReceivedMessage());
		Assert.assertNotEquals( 0, queue.getTimestampOfLastReceptionFailure());

		queue.reset();
		Assert.assertEquals( 0, queue.getFailedReceptionCount());
		Assert.assertEquals( 0, queue.getReceivedMessagesCount());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceivedMessage());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceptionFailure());
	}


	@Test
	public void testOffer_whenCapacityIsExceeded() {

		RoboconfMessageQueue queue = new RoboconfMessageQueue( 1 );

		// We are in the capacity's bounds
		queue.offer( Mockito.mock( Message.class ));
		Assert.assertEquals( 0, queue.getFailedReceptionCount());
		Assert.assertEquals( 1, queue.getReceivedMessagesCount());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceptionFailure());
		Assert.assertNotEquals( 0, queue.getTimestampOfLastReceivedMessage());

		// We exceed the capacity
		queue.offer( Mockito.mock( Message.class ));
		Assert.assertEquals( 0, queue.getFailedReceptionCount());
		Assert.assertEquals( 1, queue.getReceivedMessagesCount());
		Assert.assertEquals( 0, queue.getTimestampOfLastReceptionFailure());
		Assert.assertNotEquals( 0, queue.getTimestampOfLastReceivedMessage());
	}


	@Test( expected = RuntimeException.class )
	public void testPutIsForbidden() throws Exception {

		RoboconfMessageQueue queue = new RoboconfMessageQueue();
		queue.put( Mockito.mock( Message.class ));
	}
}
