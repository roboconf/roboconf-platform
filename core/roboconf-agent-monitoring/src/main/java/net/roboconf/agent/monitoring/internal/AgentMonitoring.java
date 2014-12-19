/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.monitoring.internal;

import java.util.Timer;
import java.util.logging.Logger;

import net.roboconf.agent.AgentMessagingInterface;

/**
 * The agent monitoring service.
 * @author Pierre-Yves Gibello - Linagora
 */
public class AgentMonitoring {

	// Injected by iPojo
	private AgentMessagingInterface agentInterface;

	private final Logger logger = Logger.getLogger( getClass().getName());
	private Timer timer;


	/**
	 * Starts the POJO (invoked by iPojo).
	 */
	public void start() {

		if( this.timer == null ) {
			this.logger.fine( "Agent Monitoring is being started." );
			this.timer = new Timer( "Monitoring Timer @ Agent", true );
			this.timer.scheduleAtFixedRate( new MonitoringTask( this.agentInterface ), 0, 10000 );
		}
	}


	/**
	 * Stops the POJO (invoked by iPojo).
	 */
	public void stop() {

		this.logger.fine( "Agent Monitoring is being stopped." );
		if( this.timer != null ) {
			this.timer.cancel();
			this.timer = null;
		}
	}


	/**
	 * Force injection of agentInterface field (for tests: normally injected by iPojo).
	 * @param agentInterface
	 */
	public void setAgentInterface( AgentMessagingInterface agentInterface ) {
		this.agentInterface = agentInterface;
	}
}
