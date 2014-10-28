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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IClient;
import net.roboconf.messaging.messages.Message;

/**
 * A message processor is in charge of (guess what) processing messages.
 * <p>
 * Message processors are supposed to be reusable. They are implemented in such a
 * way that the messaging client can be changed. The effective change will be applied when
 * a new message is about to be processed. It means the current message that is being processed
 * will keep on using the old messaging client.
 * </p>
 * <p>
 * The DM is supposed to have only one message processor running.<br />
 * Same thing for an agent. When the DM or an agent stops, the processor thread should be stopped.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
abstract class AbstractMessageProcessor<T extends IClient> extends Thread {

	protected Logger logger = Logger.getLogger( getClass().getName());

	private final LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message> ();
	private final AtomicBoolean running = new AtomicBoolean( false );
	private T messagingClient, newMessagingClient;
	private final Object lock = new Object();


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
	 * @return true if there is no process to process, false otherwise
	 */
	public final boolean hasNoMessage() {
		return this.messageQueue.isEmpty();
	}


	/**
	 * Processes messages.
	 * <p>
	 * This method is a loop. On each loop, we first check if we need
	 * to switch the messaging client. Then, we try pick up a new message.
	 * If we succeed, we process it.
	 * </p>
	 * <p>
	 * The {@link #stopProcessor()} method will stop the loop on the next iteration, i.e.
	 * when the current message has been processed. When the thread stops, it disconnects
	 * the messaging client, if any.
	 * </p>
	 *
	 * @see java.lang.Thread#run()
	 */
	@Override
	public final void run() {

		this.running.set( true );
		while( this.running.get()) {
			try {
				// Need to switch the messaging client?
				if( this.newMessagingClient != null ) {
					this.logger.info( "Switching the messaging client in the message processor." );
					IClient clientToClose;

					// We do not want someone to get the messaging client while we switch it...
					synchronized( this.lock ) {
						clientToClose = this.messagingClient;
						this.messagingClient = this.newMessagingClient;
						this.newMessagingClient = null;
					}

					closeConnection( clientToClose, "The old messaging client could not be terminated." );
				}

				// Non-Blocking calls here, we need to be able to stop this thread,
				// even if there is no message to process.

				// So, we first try to get a message immediately.
				Message message = this.messageQueue.poll();

				// No message? Let's wait a little bit!
				if( message == null )
					message = this.messageQueue.poll( MessagingConstants.MESSAGE_POLLING_PERIOD, TimeUnit.MILLISECONDS );

				if( message != null )
					processMessage( message );

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

		// Close the connection
		closeConnection( this.messagingClient, "The messaging client could not close its connection after the processor stopped." );
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
	 * Prepares this processor to switch the messaging client.
	 * @param messageServerIp the server's IP
	 * @param messageServerUser the server's user name
	 * @param messageServerPwd the server's password
	 * @return the new messaging client (not yet used by this processor)
	 * @throws IOException if something went wrong
	 */
	public T switchMessagingClient( String messageServerIp, String messageServerUser, String messageServerPwd )
	throws IOException {

		// There may be a "new messaging client" that was not yet switched...
		T oldMessagingClient;
		synchronized( this.lock ) {
			oldMessagingClient = this.newMessagingClient;

			// Create a new client and start listening.
			this.newMessagingClient = createNewMessagingClient( messageServerIp, messageServerUser, messageServerPwd );
			this.newMessagingClient.setMessageQueue( this.messageQueue );
			openConnection( this.newMessagingClient );
		}

		// Terminate the "old new" messaging client.
		closeConnection( oldMessagingClient, "A messaging client could not be terminated." );

		return this.newMessagingClient;
	}


	/**
	 * @return the current messaging client
	 * <p>
	 * This client should not be cached. Since it can change any time,
	 * it is better to get it from this class on every invocation.
	 * </p>
	 */
	public T getMessagingClient() {
		synchronized( this.lock ) {
			return this.messagingClient;
		}
	}


	/**
	 * Processes a message.
	 * @param message the message to process
	 */
	protected abstract void processMessage( Message message );


	/**
	 * Creates a new messaging client and opens a connection with the messaging server.
	 * @param messageServerIp the server's IP
	 * @param messageServerUser the server's user name
	 * @param messageServerPwd the server's password
	 * @return a new messaging client
	 * @throws IOException if something went wrong
	 */
	protected abstract T createNewMessagingClient( String messageServerIp, String messageServerUser, String messageServerPwd )
	throws IOException;


	/**
	 * Configures a newly created client.
	 * <p>
	 * For an agent, it consists into listening to messages coming from the DM.
	 * </p>
	 * <p>
	 * For the DM, it consists in listening to messages coming from various agents.
	 * </p>
	 *
	 * @param newMessagingClient the messaging client to configure
	 * @throws IOException if something went wrong
	 */
	protected abstract void openConnection( T newMessagingClient ) throws IOException;


	/**
	 * Closes the connection of a messaging client.
	 * @param client the client (may be null)
	 * @param errorMessage the error message to log in case of problem
	 */
	void closeConnection( IClient client, String errorMessage ) {

		if( client != null ) {
			try {
				client.closeConnection();

			} catch( Exception e ) {
				this.logger.warning( errorMessage + " " + e.getMessage());
				this.logger.finest( Utils.writeException( e ));
			}
		}
	}
}
