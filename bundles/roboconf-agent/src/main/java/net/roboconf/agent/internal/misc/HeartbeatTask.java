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

package net.roboconf.agent.internal.misc;

import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;

/**
 * @author Vincent Zurczak - Linagora
 */
public class HeartbeatTask extends TimerTask {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String applicationName, rootInstanceName;
	private final IAgentClient messagingClient;


	/**
	 * Constructor.
	 * @param applicationName
	 * @param rootInstanceName
	 * @param messagingClient
	 */
	public HeartbeatTask( String applicationName, String rootInstanceName, IAgentClient messagingClient ) {
		this.applicationName = applicationName;
		this.rootInstanceName = rootInstanceName;
		this.messagingClient = messagingClient;
	}


	@Override
	public void run() {
		try {
			MsgNotifHeartbeat heartBeat = new MsgNotifHeartbeat( this.applicationName, this.rootInstanceName );
			this.messagingClient.sendMessageToTheDm( heartBeat );

		} catch( IOException e ) {
			this.logger.severe( e.getMessage());
			this.logger.finest( Utils.writeException( e ));
		}
	}
}
