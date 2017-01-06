/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.agent.monitoring.api.IMonitoringHandler;
import net.roboconf.agent.monitoring.internal.file.FileHandler;
import net.roboconf.agent.monitoring.internal.nagios.NagiosHandler;
import net.roboconf.agent.monitoring.internal.rest.RestHandler;
import net.roboconf.core.Constants;

/**
 * The agent monitoring service.
 * @author Pierre-Yves Gibello - Linagora
 */
public class AgentMonitoring {

	// Injected by iPojo
	private AgentMessagingInterface agentInterface;
	private final List<IMonitoringHandler> handlers = new ArrayList<> ();

	// Internal fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	private ScheduledThreadPoolExecutor timer;


	/**
	 * Constructor.
	 */
	public AgentMonitoring() {

		// Register predefined monitoring handlers
		handlerAppears( new FileHandler());
		handlerAppears( new NagiosHandler());
		handlerAppears( new RestHandler());
	}


	/**
	 * Starts the POJO (invoked by iPojo).
	 */
	public void start() {

		if( this.timer == null ) {
			this.logger.fine( "Agent Monitoring is being started." );

			this.timer = new ScheduledThreadPoolExecutor( 1 );
			this.timer.scheduleWithFixedDelay(
					new MonitoringRunnable( this.agentInterface, this.handlers ),
					0, Constants.PROBES_POLLING_PERIOD, TimeUnit.MILLISECONDS );
		}
	}


	/**
	 * Stops the POJO (invoked by iPojo).
	 */
	public void stop() {

		this.logger.fine( "Agent Monitoring is being stopped." );
		if( this.timer != null ) {
			this.timer.shutdownNow();
			this.timer = null;
		}
	}


	/**
	 * This method is invoked by iPojo every time a new monitoring handler appears.
	 * @param handler the appearing monitoring handler
	 */
	public void handlerAppears( IMonitoringHandler handler ) {

		if( handler != null ) {
			this.logger.info( "Monitoring handler '" + handler.getName() + "' is now available." );
			this.handlers.add( handler );
			listHandlers( this.handlers, this.logger );
		}
	}


	/**
	 * This method is invoked by iPojo every time a monitoring handler disappears.
	 * @param handler the disappearing monitoring handler
	 */
	public void handlerDisappears( IMonitoringHandler handler ) {

		// May happen if a target could not be instantiated
		// (iPojo uses proxies). In this case, it results in a NPE here.
		if( handler == null ) {
			this.logger.info( "An invalid monitoring handler is removed." );
		} else {
			this.handlers.remove( handler );
			this.logger.info( "Monitoring handler '" + handler.getName() + "' is not available anymore." );
		}

		listHandlers( this.handlers, this.logger );
	}


	/**
	 * Force injection of agentInterface field (for tests: normally injected by iPojo).
	 * @param agentInterface
	 */
	public void setAgentInterface( AgentMessagingInterface agentInterface ) {
		this.agentInterface = agentInterface;
	}


	/**
	 * This method lists the available handlers and logs them.
	 */
	public static void listHandlers( List<IMonitoringHandler> handlers, Logger logger ) {

		if( handlers.isEmpty()) {
			logger.info( "No monitoring handler was found." );

		} else {
			StringBuilder sb = new StringBuilder( "Available monitoring handlers: " );
			for( Iterator<IMonitoringHandler> it = handlers.iterator(); it.hasNext(); ) {
				sb.append( it.next().getName());
				if( it.hasNext())
					sb.append( ", " );
			}

			sb.append( "." );
			logger.info( sb.toString());
		}
	}
}
