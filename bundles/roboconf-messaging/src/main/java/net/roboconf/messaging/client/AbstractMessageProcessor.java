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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import net.roboconf.messaging.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractMessageProcessor extends Thread {

	public static final long MESSAGE_POLLING_PERIOD = 1000;

	private final LinkedBlockingQueue<Message> messages;
	private final AtomicBoolean running = new AtomicBoolean( false );


	/**
	 * Constructor.
	 * @param messages the list used to store the messages to process (not null)
	 * <p>
	 * Since a message processor can be instantiated on every configuration
	 * change, it is better to manage keep the message list outside this class.
	 * This way, the same list can be passed to another message processor. This
	 * avoid the loss of messages that could not be processed.
	 * </p>
	 */
	public AbstractMessageProcessor( LinkedBlockingQueue<Message> messages ) {
		this( messages, "Roboconf - Message Processor" );
	}


	/**
	 * Constructor.
	 * @param threadName the thread name
	 */
	public AbstractMessageProcessor( LinkedBlockingQueue<Message> messages, String threadName ) {
		super( threadName );
		this.messages = messages;
	}


	/**
	 * Stores a message so that it can be processed later.
	 * @param message a message to store
	 */
	public final void storeMessage( Message message ) {
		this.messages.add( message );
	}


	/**
	 * @return true if there is no process to process, false otherwise
	 */
	public final boolean hasNoMessage() {
		return this.messages.isEmpty();
	}


	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public final void run() {

		this.running.set( true );
		while( this.running.get()) {
			try {
				// Check stop condition.
				// It allows sub-classes to indicate they do not want to process messages anymore.
				if( doNotProcessNewMessages())
					break;

				// Non-Blocking call here, we need to be able to stop this thread,
				// even if there is no message to process.

				// So, we first try to get a message.
				Message message = this.messages.peek();

				// No message, let's wait a little bit
				if( message == null )
					Thread.sleep( MESSAGE_POLLING_PERIOD );

				// Otherwise, if the message is processed, remove it from the queue
				else if( processMessage( message ))
					this.messages.remove( message );

				// This implementation allows to treat all the messages without waiting
				// if there are a lot of messages to process.

				// And it supports empty queues too quite efficiently.
				// To summer it up, it polls only when necessary.

			} catch( InterruptedException e ) {
				break;
			}
		}

		Logger.getLogger( getClass().getName()).fine( "Roboconf's message processing thread is stopping." );
		this.running.set( false );
	}


	/**
	 * @return the running
	 */
	public boolean isRunning() {
		return this.running.get();
	}


	/**
	 * Stops the processor.
	 */
	public void stopProcessor() {
		this.running.set( false );
	}


	/**
	 * Processes a message.
	 * @param message the message to process
	 * @return true if the message was processed, false if the message should be processed once AGAIN
	 * <p>
	 * This is the case, in example, when the messaging client has to be replaced after a configuration change.
	 * </p>
	 */
	protected abstract boolean processMessage( Message message );


	/**
	 * @return true if the thread should stop on the next message check, false otherwise
	 */
	protected boolean doNotProcessNewMessages() {
		return false;
	}
}
