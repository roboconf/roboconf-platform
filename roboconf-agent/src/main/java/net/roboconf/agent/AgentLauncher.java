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

package net.roboconf.agent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import net.roboconf.agent.internal.Agent;
import net.roboconf.agent.internal.messaging.AgentMessageProcessor;
import net.roboconf.agent.internal.messaging.MessagingService;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.plugin.api.ExecutionLevel;


/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentLauncher {

	private String agentName;


	/**
	 * Constructor.
	 */
	public AgentLauncher() {
		// nothing
	}


	/**
	 * Constructor.
	 */
	public AgentLauncher( String agentName ) {
		this.agentName = agentName;
	}


	/**
	 * Launches an agent.
	 * @param agentData the agent data
	 * @param executionLevel the execution level
	 * @param dumpDirectory the dump directory (if execution level is {@link ExecutionLevel#GENERATE_FILES})
	 */
	public void launchAgent( AgentData agentData, ExecutionLevel executionLevel, File dumpDirectory ) {
		Logger logger = Logger.getLogger( getClass().getName());

		// The messaging client
		IMessageServerClient client = new MessageServerClientFactory().create();
		client.setMessageServerIp( agentData.getMessageServerIp());

		// The agent
		String aName;
		try {
			if( this.agentName != null )
				aName = this.agentName;
			else
				aName = "Roboconf Agent - " + InetAddress.getLocalHost().getHostName();

		} catch( UnknownHostException e ) {
			logger.warning( "Network information could not be retrieved. Setting the agent name to default." );
			aName = "Roboconf Agent";
		}

		Agent agent = new Agent( aName );
		agent.setClient( client );

		// Set the message processor
		AgentMessageProcessor messageProcessor = new AgentMessageProcessor( agent );
		client.setMessageProcessor( messageProcessor );

		// Initialize the agent's connections
		try {
			new MessagingService().initializeAgentConnection( agentData, client );

		} catch( IOException e ) {
			logger.severe( "A connection could not be established with the message server. " + e.getMessage());
			logger.finest( Utils.writeException( e ));

		} finally {
			try {
				client.closeConnection();

			} catch( IOException e ) {
				logger.severe( "The connection could not be closed with the message server. " + e.getMessage());
				logger.finest( Utils.writeException( e ));
			}
		}
	}
}
