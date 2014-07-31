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
import java.util.logging.Logger;

import net.roboconf.messaging.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractMessageProcessor extends Thread {

	private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<Message> ();
	private boolean running = true;


	/**
	 * Constructor.
	 */
	public AbstractMessageProcessor() {
		super( "Roboconf - Message Processor" );
	}


	/**
	 * Constructor.
	 * @param threadName the thread name
	 */
	public AbstractMessageProcessor( String threadName ) {
		super( threadName );
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

		for( ;; ) {
			try {
				// Blocking call
				Message message = this.messages.take();
				processMessage( message );

			} catch( InterruptedException e ) {
				Logger.getLogger( getClass().getName()).fine( "Roboconf's message processing thread is stopping." );
				break;
			}
		}

		this.running = false;
	}


	/**
	 * Processes a message.
	 * @param message the message to process
	 */
	protected abstract void processMessage( Message message );


	/**
	 * @return the running
	 */
	public boolean isRunning() {
		return this.running;
	}
}
