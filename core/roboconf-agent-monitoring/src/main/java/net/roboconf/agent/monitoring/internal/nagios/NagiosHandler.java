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

package net.roboconf.agent.monitoring.internal.nagios;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.agent.monitoring.internal.MonitoringHandler;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * Handler to check Nagios (polling).
 * @author Pierre-Yves Gibello - Linagora
 */
public class NagiosHandler extends MonitoringHandler {

	static final String NAGIOS_CONFIG = "nagios configuration at";
	private final Logger logger = Logger.getLogger( getClass().getName());

	private String nagiosInstructions, host;
	private int port = -1;


	/**
	 * Constructor.
	 * @param eventId
	 * @param applicationName
	 * @param vmInstanceName
	 * @param fileContent
	 */
	public NagiosHandler( String eventName, String applicationName, String vmInstanceName, String fileContent ) {
		super( eventName, applicationName, vmInstanceName );

		this.nagiosInstructions = fileContent.trim();
		if( this.nagiosInstructions.toLowerCase().startsWith( NAGIOS_CONFIG )) {

			String nagiosConfig = this.nagiosInstructions.substring( NAGIOS_CONFIG.length());
			this.nagiosInstructions = "";
			int pos = nagiosConfig.indexOf( '\n' );

			if( pos > 0 ) {
				this.nagiosInstructions = nagiosConfig.substring( pos ).trim();
				nagiosConfig = nagiosConfig.substring( 0, pos ).trim();
			}

			Map.Entry<String,Integer> entry = Utils.findUrlAndPort( nagiosConfig );
			this.host = entry.getKey();
			this.port = entry.getValue();
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.agent.monitoring.internal.MonitoringHandler
	 * #process()
	 */
	@Override
	public MsgNotifAutonomic process() {

		LiveStatusClient client = new LiveStatusClient( this.host, this.port );
		MsgNotifAutonomic result = null;
		try {
			String liveStatusResponse = client.queryLivestatus( this.nagiosInstructions );
			result = new MsgNotifAutonomic(
					this.applicationName,
					this.vmInstanceName,
					this.eventId,
					liveStatusResponse );

		} catch( UnknownHostException e ) {
			this.logger.warning( "Uknown host exception. " + e.getMessage());
			Utils.logException( this.logger, e );

		} catch( IOException e ) {
			this.logger.warning( "I/O exception. " + e.getMessage());
			Utils.logException( this.logger, e );
		}

		return result;
	}


	/**
	 * @return the nagiosInstructions
	 */
	public String getNagiosInstructions() {
		return this.nagiosInstructions;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * @param host the host to set
	 */
	void setHost( String host ) {
		this.host = host;
	}

	/**
	 * @param port the port to set
	 */
	void setPort( int port ) {
		this.port = port;
	}
}
