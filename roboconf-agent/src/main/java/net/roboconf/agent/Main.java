/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import net.roboconf.agent.messaging.AgentMessageProcessor;
import net.roboconf.agent.messaging.MessagingService;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.MessageServerClientFactory;

/**
 * The main program when the agent runs in stand-alone mode.
 * @author Noël - LIG
 */
public class Main {

	/**
	 * Main method.
	 * @param args the arguments
	 */
	public static void main( String[] args ) {

		Logger logger = Logger.getLogger( Main.class.getName());
		logger.info( "A stand-alone agent is starting." );

		// TODO: get the IP address (but get the right one is not easy ;))
		AgentData agentData = null;
		if( args.length == 1 )
			agentData = AgentUtils.findParametersInPropertiesFile( logger, args[ 0 ]);
		else if( args.length == 4 )
			agentData = AgentUtils.findParametersInProgramArguments( args );
		else if( args.length > 0 )
			logger.severe( "Agent's main class requires 1, 4 or 0 arguments. Any other number of arguments is invalid." );
		else
			agentData = AgentUtils.findParametersInWsInfo( logger );

		// TODO: validate the agent's data
		if( agentData == null
				|| agentData.getMessageServerIp() == null ) {
			logger.severe( "The agent's data (message server IP) could not be retrieved." );

		} else {
			IMessageServerClient client = new MessageServerClientFactory().create();
			client.setMessageServerIp( agentData.getMessageServerIp());

			Agent agent = new Agent();
			agent.setClient( client );
			try {
				agent.setAgentName( "Roboconf Agent - " + InetAddress.getLocalHost().getHostName());

			} catch( UnknownHostException e ) {
				logger.warning( "Network information could not be retrieved. Setting the agent name to default." );
				agent.setAgentName( "Roboconf Agent" );
			}

			AgentMessageProcessor messageProcessor = new AgentMessageProcessor( agent );
			client.setMessageProcessor( messageProcessor );

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

		logger.info( "A stand-alone agent is stopping." );
	}
}
