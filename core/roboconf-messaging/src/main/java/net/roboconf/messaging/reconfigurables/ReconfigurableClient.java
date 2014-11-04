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

package net.roboconf.messaging.reconfigurables;

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.client.IClient;
import net.roboconf.messaging.processors.AbstractMessageProcessor;

/**
 * A class that wraps a messaging client and can reconfigure it.
 * @param <T> a sub-class of {@link IClient}
 * @author Vincent Zurczak - Linagora
 */
public abstract class ReconfigurableClient<T extends IClient> implements IClient {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private AbstractMessageProcessor<T> messageProcessor;
	private T messagingClient;


	/**
	 * Changes the internal messaging client.
	 * @param messageServerIp the IP address of the messaging server
	 * @param messageServerUser the user name for the messaging server
	 * @param messageServerPwd the password for the messaging server
	 * @param factoryName the factory name (see {@link MessagingConstants})
	 */
	public void switchMessagingClient(
			String messageServerIp,
			String messageServerUser,
			String messageServerPwd,
			String factoryName ) {

		// Create a new client
		T newMessagingClient = null;
		try {
			newMessagingClient = createNewMessagingClient( messageServerIp, messageServerUser, messageServerPwd, factoryName );
			if( newMessagingClient != null ) {
				newMessagingClient.setMessageQueue( this.messageProcessor.getMessageQueue());
				openConnection( newMessagingClient );
			}

		} catch( IOException e ) {
			this.logger.warning( "An error occured while creating a new messaging client. " + e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}

		// Replace the current client
		// (the new client may be null, it is not a problem - see #getMessagingClient())
		T oldClient;
		synchronized( this ) {
			oldClient = this.messagingClient;
			this.messagingClient = newMessagingClient;
		}

		closeConnection( oldClient, "The previous client could not be terminated correctly." );
	}


	/**
	 * Creates a new messaging client and opens a connection with the messaging server.
	 * @param messageServerIp the server's IP
	 * @param messageServerUser the server's user name
	 * @param messageServerPwd the server's password
	 * @param factoryName the factory name (see {@link MessagingConstants})
	 * @return a new messaging client
	 * @throws IOException if something went wrong
	 */
	protected abstract T createNewMessagingClient(
			String messageServerIp,
			String messageServerUser,
			String messageServerPwd,
			String factoryName )
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
	 * @return a dismiss client for the case where the internal client is null
	 */
	protected abstract T getDismissedClient();


	/**
	 * Configures the message processor.
	 * @param messageProcessor the message processor
	 */
	protected abstract void configureMessageProcessor( AbstractMessageProcessor<T> messageProcessor );


	/**
	 * Associates a message processor with this instance.
	 * <p>
	 * The message processor cannot be started before. This method will start it.
	 * </p>
	 * <p>
	 * This method must be invoked only once.
	 * </p>
	 * @param messageProcessor the message processor
	 */
	public void associateMessageProcessor( AbstractMessageProcessor<T> messageProcessor ) {

		if( this.messageProcessor != null )
			throw new IllegalArgumentException( "The message processor was already defined." );

		this.messageProcessor = messageProcessor;
		configureMessageProcessor( messageProcessor );
		this.messageProcessor.start();
	}


	/**
	 * @return the message processor
	 */
	public AbstractMessageProcessor<T> getMessageProcessor() {
		return this.messageProcessor;
	}


	/**
	 * @return true if the internal client exists, false otherwise
	 */
	public boolean hasValidClient() {
		return this.messagingClient != null;
	}


	/**
	 * @return a messaging client (never null)
	 */
	protected synchronized T getMessagingClient() {
		return this.messagingClient != null ? this.messagingClient : getDismissedClient();
	}


	/**
	 * Resets the internal client (sets it to null).
	 */
	protected synchronized void resetInternalClient() {
		this.messagingClient = null;
	}


	/**
	 * A method that must be used only in tests.
	 * @return the messaging client
	 */
	@Deprecated
	public T getInternalClient() {
		return this.messagingClient;
	}


	/**
	 * Closes the connection of a messaging client.
	 * @param client the client (may be null)
	 * @param errorMessage the error message to log in case of problem
	 */
	static void closeConnection( IClient client, String errorMessage ) {

		if( client != null ) {
			try {
				client.closeConnection();

			} catch( Exception e ) {
				Logger logger = Logger.getLogger( ReconfigurableClient.class.getName());
				logger.warning( errorMessage + " " + e.getMessage());
				logger.finest( Utils.writeException( e ));
			}
		}
	}
}
