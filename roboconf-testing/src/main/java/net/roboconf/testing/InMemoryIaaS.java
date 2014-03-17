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

package net.roboconf.testing;

import java.io.File;
import java.util.Properties;

import net.roboconf.agent.AgentData;
import net.roboconf.agent.AgentLauncher;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;
import net.roboconf.plugin.api.ExecutionLevel;

/**
 * An in-memory IaaS that runs an agent in a thread.
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryIaaS implements IaasInterface {

	public static final String IN_MEMORY_AGENT = "in-memory-agent";

	private AgentLauncher agentLauncher;
	private ExecutionLevel executionLevel;
	private File dumpDirectory;


	@Override
	public void setIaasProperties( Properties iaasProperties )
	throws InvalidIaasPropertiesException {
		// nothing
	}


	@Override
	public String createVM(
			final String ipMessagingServer,
			final String channelName,
			final String applicationName,
			final String rootInstanceName )
	throws IaasException, CommunicationToIaasException {

		// Create the agent's data.
		AgentData agentData = new AgentData();
		agentData.setApplicationName( applicationName );
		agentData.setMessageServerIp( ipMessagingServer );
		agentData.setIpAddress( "localhost" );
		agentData.setRootInstanceName( rootInstanceName );

		// Run the agent directly.
		// Messaging subscriptions are handled automatically in a new thread (see *.messaging).
		String agentName = rootInstanceName + " - In-Memory Agent";
		InMemoryIaaS.this.agentLauncher = new AgentLauncher( agentName );
		InMemoryIaaS.this.agentLauncher.launchAgent(
				agentData,
				InMemoryIaaS.this.executionLevel,
				InMemoryIaaS.this.dumpDirectory );

		return IN_MEMORY_AGENT;
	}


	@Override
	public void terminateVM( String machineId )
	throws IaasException, CommunicationToIaasException {
		this.agentLauncher.forceAgentToStop();
	}


	/**
	 * @param executionLevel the executionLevel to set
	 */
	public void setExecutionLevel( ExecutionLevel executionLevel ) {
		this.executionLevel = executionLevel;
	}


	/**
	 * @param dumpDirectory the dumpDirectory to set
	 */
	public void setDumpDirectory( File dumpDirectory ) {
		this.dumpDirectory = dumpDirectory;
	}
}
