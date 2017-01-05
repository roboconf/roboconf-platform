/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.messaging.RCDm;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class MessagingMngrImpl implements IMessagingMngr {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private RCDm messagingClient;


	/**
	 * @param messagingClient the messagingClient to set
	 */
	public void setMessagingClient( RCDm messagingClient ) {
		this.messagingClient = messagingClient;
	}


	@Override
	public void sendMessageSafely( ManagedApplication ma, Instance instance, Message message ) {

		// We do NOT send directly a message!
		ma.storeAwaitingMessage( instance, message );

		// If the message has been stored, let's try to send all the stored messages.
		sendStoredMessages( ma, instance );
	}


	@Override
	public void sendMessageDirectly( ManagedApplication ma, Instance scopedInstance, Message message )
	throws IOException {
		this.messagingClient.sendMessageToAgent( ma.getApplication(), scopedInstance, message );
	}


	@Override
	public void sendStoredMessages( ManagedApplication ma, Instance instance ) {

		if( messagingIsReady()) {

			// If the VM is online, process awaiting messages to prevent waiting.
			// This can work concurrently with the messages timer.
			Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );
			if( scopedInstance.getStatus() == InstanceStatus.DEPLOYED_STARTED ) {

				List<Message> messages = ma.removeAwaitingMessages( instance );
				if( messages.isEmpty())
					return;

				String path = InstanceHelpers.computeInstancePath( scopedInstance );
				this.logger.fine( "Forcing the sending of " + messages.size() + " awaiting message(s) for " + path + "." );

				for( Message msg : messages ) {
					try {
						sendMessageDirectly( ma, scopedInstance, msg );

					} catch( IOException e ) {

						// If the message could not be send, plan a retry.
						// This preserves message ordering (FIFO).
						ma.storeAwaitingMessage( scopedInstance, msg );
						this.logger.severe( "Error while sending a stored message. A retry is planned. " + e.getMessage());
						Utils.logException( this.logger, e );
					}
				}
			}
		}
	}


	@Override
	public void sendMessageToTheDm( Message message ) throws IOException {
		this.messagingClient.sendMessageToTheDm( message );
	}


	@Override
	public IDmClient getMessagingClient() {
		return this.messagingClient;
	}


	@Override
	public void checkMessagingConfiguration() throws IOException {

		String msg = null;
		if( this.messagingClient == null )
			msg = "The DM was not started.";
		else if( ! this.messagingClient.hasValidClient())
			msg = "The DM's configuration is invalid. Please, review the messaging settings.";

		if( msg != null ) {
			this.logger.warning( msg );
			throw new IOException( msg );
		}
	}


	private boolean messagingIsReady() {

		boolean result = this.messagingClient != null && this.messagingClient.isConnected();
		if( ! result )
			this.logger.severe( "The connection with the messaging server was badly initialized. Message dropped." );

		return result;
	}
}
