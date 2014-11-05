/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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

package net.roboconf.dm.internal.management;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CheckerMessagesTask extends TimerTask {

	private final Logger logger;
	private final IDmClient messagingClient;
	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager
	 * @param messagingClient
	 */
	public CheckerMessagesTask( Manager manager, IDmClient messagingClient ) {
		this.manager = manager;
		this.messagingClient = messagingClient;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/*
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {

		for( ManagedApplication ma : this.manager.getAppNameToManagedApplication().values()) {
			for( Instance rootInstance : ma.getApplication().getRootInstances()) {
				if( rootInstance.getStatus() != InstanceStatus.DEPLOYED_STARTED )
					continue;

				List<Message> messages = ma.removeAwaitingMessages( rootInstance );
				if( ! messages.isEmpty())
					this.logger.fine( "Sending " + messages.size() + " awaiting message(s) for " + rootInstance.getName() + "." );

				for( Message msg : messages ) {
					try {
						this.messagingClient.sendMessageToAgent( ma.getApplication(), rootInstance, msg );

					} catch( IOException e ) {

						// If the message could not be send, plan a retry
						ma.storeAwaitingMessage( rootInstance, msg );
						this.logger.severe( "Error while sending a stored message. A retry is planned. " + e.getMessage());
						this.logger.finest( Utils.writeException( e ));
					}
				}
			}
		}
	}
}
