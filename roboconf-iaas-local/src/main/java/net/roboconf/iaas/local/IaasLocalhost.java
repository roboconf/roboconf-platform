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
import java.util.Map;

import net.roboconf.agent.AgentData;
import net.roboconf.agent.AgentLauncher;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.plugin.api.ExecutionLevel;

/**
 * A IaaS emulation on the local host.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class IaasLocalhost implements IaasInterface {

	private AgentLauncher agentLauncher;


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
			String ipMessagingServer,
			String channelName,
			String applicationName,
			String rootInstanceName )
	throws IaasException, CommunicationToIaasException {

		// Create the agent's data.
		final AgentData agentData = new AgentData();
		agentData.setApplicationName( applicationName );
		agentData.setMessageServerIp( ipMessagingServer );
		agentData.setIpAddress( "localhost" );
		agentData.setRootInstanceName( rootInstanceName );

		// Messaging subscriptions are handled automatically in a new thread (see *.messaging).
		String agentName = rootInstanceName + " - In-Memory Agent";
		this.agentLauncher = new AgentLauncher( agentName );

		new Thread() {
			@Override
			public void run() {
				IaasLocalhost.this.agentLauncher.launchAgent(
						agentData,
						ExecutionLevel.RUNNING,
						new File( System.getProperty( "java.io.tmpdir" )));
			};
		}.start();

		return rootInstanceName + " @ localhost";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.iaas.api.IaasInterface
	 * #terminateVM(java.lang.String)
	 */
	@Override
	public void terminateVM( String instanceId )
	throws IaasException, CommunicationToIaasException {

		if( this.agentLauncher != null )
			this.agentLauncher.forceAgentToStop();
	}
}