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

import net.roboconf.agent.internal.MessagingService;
import net.roboconf.agent.internal.PluginManager;
import net.roboconf.core.internal.utils.Utils;
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

		// The agent
		String finalAgentName;
		try {
			if( this.agentName != null )
				finalAgentName = this.agentName;
			else
				finalAgentName = "Roboconf Agent - " + InetAddress.getLocalHost().getHostName();

		} catch( UnknownHostException e ) {
			logger.warning( "Network information could not be retrieved. Setting the agent name to default." );
			finalAgentName = "Roboconf Agent";
		}

		PluginManager pluginManager = new PluginManager();
		pluginManager.setDumpDirectory( dumpDirectory );
		pluginManager.setExecutionLevel( executionLevel );

		// Initialize the agent's connections
		try {
			MessagingService msgService = new MessagingService();
			msgService.initializeAgentConnection( agentData, finalAgentName, pluginManager );

		} catch( IOException e ) {
			logger.severe( "A connection could not be established with the message server. " + e.getMessage());
			logger.finest( Utils.writeException( e ));
		}
	}
}
