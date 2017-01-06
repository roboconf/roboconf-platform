/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.karaf.commands.agent.status;

import java.util.List;
import java.util.logging.Logger;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.utils.Utils;

/**
 * @author Amadou Diarra - UGA
 */
@Command( scope = "roboconf", name = "agent-status", description="Gives an agent status" )
@Service
public class AgentStatusCommand implements Action {

	// Other fields
	final Logger logger = Logger.getLogger( getClass().getName());

	@Reference
	List<AgentMessagingInterface> agents;



	@Override
	public Object execute() throws Exception {

		if( this.agents != null ) {
			for( AgentMessagingInterface agent : this.agents ) {
				try {
					System.out.println( agent.agentStatus());
					System.out.println( "" );

				} catch( Exception e ) {
					this.logger.warning( "An error occurred while giving agent status. " + e.getMessage());
					Utils.logException( this.logger, e );
				}
			}
		}

		return null;
	}
}
