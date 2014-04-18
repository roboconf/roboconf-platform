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

package net.roboconf.agent.internal;

import java.util.logging.Logger;

import net.roboconf.agent.AgentData;
import net.roboconf.agent.AgentLauncher;
import net.roboconf.plugin.api.ExecutionLevel;

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

		AgentData agentData = null;
		if( args.length == 1 )
			agentData = AgentUtils.findParametersInPropertiesFile( logger, args[ 0 ]);
		else if( args.length == 2 && "platform".equals(args[ 0 ]) && "azure".equals(args[ 1 ]))		// for Azure
			agentData = AgentUtils.findParametersForAzure( logger );
		else if( args.length == 2 && !"platform".equals(args[ 0 ]))
			logger.severe( "If agent's main class has 2 arguments. The first argument must named 'platform' and the second one must be platform's name. Default is EC2." );
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
			new AgentLauncher().launchAgent( agentData, ExecutionLevel.RUNNING, null );
			logger.info( "Agent launched !" );
		}

	}
}
