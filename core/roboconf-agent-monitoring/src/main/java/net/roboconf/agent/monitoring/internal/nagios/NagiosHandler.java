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

package net.roboconf.agent.monitoring.internal.nagios;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.agent.monitoring.api.IMonitoringHandler;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * Handler to check Nagios (polling).
 * @author Pierre-Yves Gibello - Linagora
 */
public class NagiosHandler implements IMonitoringHandler {

	static final String HANDLER_NAME = "nagios";
	static final String NAGIOS_CONFIG = "nagios configuration at";

	private final Logger logger = Logger.getLogger( getClass().getName());

	private String applicationName, scopedInstancePath, eventId;
	String nagiosInstructions, host;
	int port = -1;



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
	public void reset( Instance associatedInstance, String eventId, String fileContent ) {
		this.eventId = eventId;

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


	@Override
	public MsgNotifAutonomic process() {

		LiveStatusClient client = new LiveStatusClient( this.host, this.port );
		MsgNotifAutonomic result = null;
		try {
			String liveStatusResponse = client.queryLivestatus( this.nagiosInstructions );
			result = new MsgNotifAutonomic(
					this.applicationName,
					this.scopedInstancePath,
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
}
