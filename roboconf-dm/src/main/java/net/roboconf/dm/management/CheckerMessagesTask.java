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

package net.roboconf.dm.management;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerMessagesTask extends TimerTask {

	private final IDmClient messagingClient;
	private final Logger logger;


	/**
	 * Constructor.
	 * @param messagingClient
	 */
	public CheckerMessagesTask( IDmClient messagingClient ) {
		this.messagingClient = messagingClient;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/*
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {

		for( ManagedApplication ma : Manager.INSTANCE.getAppNameToManagedApplication().values()) {
			for( Instance rootInstance : ma.getApplication().getRootInstances()) {
				if( rootInstance.getStatus() != InstanceStatus.DEPLOYED_STARTED )
					continue;

				List<Message> messages = ma.removeAwaitingMessages( rootInstance );
				if( ! messages.isEmpty())
					this.logger.fine( "Sending " + messages.size() + " awaiting message(s) for " + rootInstance.getName() + "." );

				for( Message msg : messages ) {

					// If the message could not be send, plan a retry
					try {
						this.messagingClient.sendMessageToAgent( ma.getApplication(), rootInstance, msg );

					} catch( IOException e ) {
						ma.storeAwaitingMessage( rootInstance, msg );
						this.logger.severe( "Error while sending a stored message. Retry planned. " + e.getMessage());
						this.logger.finest( Utils.writeException( e ));
					}
				}
			}
		}
	}
}
