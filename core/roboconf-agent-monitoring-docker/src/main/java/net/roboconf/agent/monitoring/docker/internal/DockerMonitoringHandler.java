/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
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

package net.roboconf.agent.monitoring.docker.internal;

import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.agent.monitoring.api.IMonitoringHandler;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.DockerAndScriptUtils;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.ProgramUtils.ExecutionResult;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DockerMonitoringHandler implements IMonitoringHandler {

	static final String HANDLER_NAME = "docker";
	String applicationName, scopedInstancePath, eventName, containerName;


	@Override
	public String getName() {
		return HANDLER_NAME;
	}


	@Override
	public void setAgentId( String applicationName, String scopedInstancePath ) {
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
	}


	@Override
	public void reset( Instance associatedInstance, String eventName, String rawRulesText ) {
		this.eventName = eventName;

		// Build the container name from the probe config and the instance name
		String name = Utils.isEmptyOrWhitespaces( rawRulesText )
				? DockerAndScriptUtils.ROBOCONF_CLEAN_REVERSED_INSTANCE_PATH
				: rawRulesText.trim();

		for( Map.Entry<String,String> entry : DockerAndScriptUtils.buildReferenceMap( associatedInstance ).entrySet()) {
			name = name.replace( entry.getKey(), entry.getValue());
		}

		this.containerName = name;
	}


	@Override
	public MsgNotifAutonomic process() {

		MsgNotifAutonomic result = null;
		if( ! containerIsRunning( this.containerName ))
			result = new MsgNotifAutonomic( this.applicationName, this.scopedInstancePath, this.eventName, null );

		return result;
	}


	/**
	 * Checks that a Docker container is running.
	 * <p>
	 * This method works even if Docker is not installed. In this case,
	 * it will return <code>false</code>. Otherwise, it will check whether
	 * a container with this name is running. If it is stopped or if it does
	 * not exist, it will return <code>false</code>. It returns <code>true</code>
	 * if and only if a container with this name exists, and if it is running.
	 * </p>
	 *
	 * @param containerName a container name (not null)
	 * @return true if Docker is available and a container with this name is running, false otherwise
	 */
	boolean containerIsRunning( String containerName ) {

		// This method is not static so that we can mock it in tests.

		/*
		 * At the beginning, it was decided we would use Docker's REST
		 * API to query Docker. Just like we do with the Docker target.
		 *
		 * Except this may not be the best solution.
		 * Indeed, using the REST API implies having a socket open. This
		 * could constitute a security breach. Besides, we would have to
		 * specify the (optional) credentials to connect to this API.
		 *
		 * Since for the moment, we only want to trigger a notification when
		 * a container is down, we can use a system command.
		 */

		boolean running = false;
		Logger logger = Logger.getLogger( DockerMonitoringHandler.class.getName());
		try {
			String[] cmd = new String[] {
					"docker",
					"inspect",
					"-f",
					"{{.State.Running}}",
					containerName
			};

			ExecutionResult res = ProgramUtils.executeCommandWithResult( logger, cmd, null, null, this.applicationName, this.scopedInstancePath);
			logger.finest( "Execution's result: " + res.getExitValue());
			logger.finest( "Execution's normal output: " + res.getNormalOutput());
			running = res.getExitValue() == 0 && Boolean.parseBoolean( res.getNormalOutput());

		} catch( Exception e ) {
			logger.severe( "An error occurred while verifying that " + containerName + " was still running (in Docker)." );
			Utils.logException( logger, e );
		}

		return running;
	}
}
