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

package net.roboconf.messaging.api;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import net.roboconf.messaging.api.business.IClient;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClient;

/**
 * A message processor is in charge of (guess what) processing messages.
 * <p>
 * The DM is supposed to have only one message processor running.<br>
 * Same thing for an agent. When the DM or an agent stops, the processor thread should be stopped.
 * </p>
 * <p>
 * This class is a thread that will process messages. The method {@link #stopProcessor()} will stop
 * processing messages after the current message is processed, or right after the next one is received.
 * For an immediate stop, use the {@link #interrupt()} method.
 * </p>
 *
 * @param <T> a sub-class of {@link IMessagingClient}
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractMessageProcessor<T extends IClient> extends Thread {

	private final RoboconfMessageQueue messageQueue = new RoboconfMessageQueue();
	private final AtomicBoolean running = new AtomicBoolean( false );
	protected T messagingClient;



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
		this.messageQueue.add( message );
	}


	/**
	 * @return the messageQueue
	 */
	public RoboconfMessageQueue getMessageQueue() {
		return this.messageQueue;
	}


	/**
	 * This method must be invoked before {@link #start()}.
	 * <p>
	 * It is not recommended to change the messaging client once the thread has started.
	 * It is better to use a {@link ReconfigurableClient} to handle messaging reconfiguration.
	 * </p>
	 * @param messagingClient the messaging client to set
	 */
	public void setMessagingClient( T messagingClient ) {
		this.messagingClient = messagingClient;
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
				Message message = this.messageQueue.take();
				if( this.running.get())
					processMessage( message );

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
	 */
	protected abstract void processMessage( Message message );
}
