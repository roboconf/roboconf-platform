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

package net.roboconf.iaas.local;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.agent.AgentData;
import net.roboconf.agent.AgentLauncher;
import net.roboconf.core.utils.Utils;
import net.roboconf.iaas.api.IaasException;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.local.internal.utils.AgentManager;
import net.roboconf.plugin.api.ExecutionLevel;

/**
 * A IaaS emulation that runs agents in memory.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class IaasInMemory implements IaasInterface {

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #setIaasProperties(java.util.Properties)
	 */
	@Override
	public void setIaasProperties(Map<String, String> iaasProperties) {
		// nothing
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #createVM(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String createVM(
			String machineImageId,
			final String ipMessagingServer,
			String rootInstanceName,
			String applicationName )
	throws IaasException {

		// Create the agent's data.
		final AgentData agentData = new AgentData();
		agentData.setApplicationName( applicationName );
		agentData.setMessageServerIp( ipMessagingServer );
		agentData.setIpAddress( "localhost" );
		agentData.setRootInstanceName( rootInstanceName );

		// Messaging subscriptions are handled automatically in a new thread (see *.messaging).
		String agentName = rootInstanceName + " - In-Memory Agent";
		AgentLauncher agentLauncher = new AgentLauncher( agentName, agentData );
		try {
			File tempDir = new File( System.getProperty( "java.io.tmpdir" ));
			agentLauncher.launchAgent( ExecutionLevel.RUNNING, tempDir );

		} catch( IOException e ) {
			Logger logger = Logger.getLogger( getClass().getName());
			logger.severe( "An error occurred in an agent (in-memory). " + e.getMessage());
			logger.finest( Utils.writeException( e ));
		}

		// Store the agent's launcher, so that we can stop it later
		String machineId = rootInstanceName + " @ localhost";
		AgentManager.INSTANCE.registerMachine( machineId, agentLauncher );

		return machineId;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM( String machineId )
	throws IaasException {

		AgentLauncher agentLauncher = AgentManager.INSTANCE.unregisterMachine( machineId );
		if( agentLauncher != null )
			agentLauncher.stopAgent();
	}
}
