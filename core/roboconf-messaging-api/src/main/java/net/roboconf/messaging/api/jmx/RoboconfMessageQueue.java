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

import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import net.roboconf.messaging.api.messages.Message;

/**
 * A blocking queue with additional attributes to measure activity.
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfMessageQueue extends LinkedBlockingQueue<Message> {

	private static final long serialVersionUID = 6728334259953445852L;

	private final AtomicLong receivedMessagesCount = new AtomicLong();
	private final AtomicLong timestampOfLastReceivedMessage = new AtomicLong();

	private final AtomicLong failedReceptionCount = new AtomicLong();
	private final AtomicLong timestampOfLastReceptionFailure = new AtomicLong();


	/**
	 * Constructor.
	 */
	public RoboconfMessageQueue() {
		super();
	}


	/**
	 * Constructor (used only for tests).
	 * @param capacity
	 */
	RoboconfMessageQueue( int capacity ) {
		super( capacity );
	}


	// Methods to track activity.

	// Take a look at the unit tests.
	// - addAll() invokes add().
	// - add() invokes offer().
	//
	// - put() is ignored.
	//
	// So, we only override offer().

	@Override
	public boolean offer( Message e ) {

		boolean result = super.offer( e );
		if( result ) {
			this.receivedMessagesCount.incrementAndGet();
			this.timestampOfLastReceivedMessage.set( new Date().getTime());
		}

		return result;
	}


	@Override
	public void put( Message e ) throws InterruptedException {
		// As we cannot get precise metrics when this method is used, we just forbid its use.
		throw new RuntimeException( "This method is disabled for Roboconf." );
	}


	// Custom methods

	/**
	 * Resets the count of messages.
	 */
	public void reset() {
		this.receivedMessagesCount.set( 0 );
		this.timestampOfLastReceivedMessage.set( 0 );
		this.failedReceptionCount.set( 0 );
		this.timestampOfLastReceptionFailure.set( 0 );
	}


	/**
	 * Method to notify a message was incorrectly received (e.g. error during deserialization).
	 */
	public void errorWhileReceivingMessage() {
		this.failedReceptionCount.incrementAndGet();
		this.timestampOfLastReceptionFailure.set( new Date().getTime());
	}


	// Getters

	public long getFailedReceptionCount() {
		return this.failedReceptionCount.get();
	}


	public long getReceivedMessagesCount() {
		return this.receivedMessagesCount.get();
	}


	public long getTimestampOfLastReceptionFailure() {
		return this.timestampOfLastReceptionFailure.get();
	}


	public long getTimestampOfLastReceivedMessage() {
		return this.timestampOfLastReceivedMessage.get();
	}
}
