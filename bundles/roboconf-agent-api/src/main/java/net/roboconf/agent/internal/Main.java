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

import java.io.IOException;
import java.util.logging.Logger;

import net.roboconf.agent.AgentData;
import net.roboconf.agent.AgentLauncher;
import net.roboconf.core.utils.Utils;

/**
 * The main program when the agent runs in stand-alone mode.
 * @author Noël - LIG
 */
public final class Main {

	private static final String PLATFORM = "platform";


	/**
	 * Constructor.
	 */
	private Main() {
		// nothing
	}


	/**
	 * Main method.
	 * @param args the arguments
	 */
	public static void main( String[] args ) {

		Logger logger = Logger.getLogger( Main.class.getName());
		logger.info( "A stand-alone agent is starting." );

		// Get the agent's data
		AgentData agentData = null;
		if( args.length == 1 )
			agentData = AgentUtils.findParametersInPropertiesFile( logger, args[ 0 ]);

		else if( args.length == 2 && PLATFORM.equals(args[ 0 ])
				&& "azure".equalsIgnoreCase( args[ 1 ]))
			agentData = AgentUtils.findParametersForAzure( logger );

		else if( args.length == 2
				&& ! PLATFORM.equals(args[ 0 ]))
			logger.severe( "If the main class has 2 arguments, then the first argument must be named 'platform'." );

		else if( args.length == 6 )
			agentData = AgentUtils.findParametersInProgramArguments( args );

		else if( args.length > 0 )
			logger.severe( "Agent's main class requires 1, 2, 6 or 0 arguments. Any other number of arguments is invalid." );

		else
			agentData = AgentUtils.findParametersInWsInfo( logger );


		// Launch the agent
		String error;
		if( agentData == null ) {
			logger.severe( "No agent data was found." );

		} else if(( error = agentData.validate()) != null ) {
			logger.severe( "Error in the agent's data. " + error );

		} else {
			try {
				new AgentLauncher( agentData ).launchAgent();
				logger.info( "The agent was launched by the main program." );

			} catch( IOException e ) {
				logger.severe( "The agent failed to be launched by the main program. " + e.getMessage());
				logger.finest( Utils.writeException( e ));
			}
		}
	}
}
