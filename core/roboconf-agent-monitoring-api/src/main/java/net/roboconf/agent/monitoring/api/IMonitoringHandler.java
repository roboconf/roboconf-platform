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

package net.roboconf.agent.monitoring.api;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * A handler to verify monitoring assertions on an agent's machine.
 * <p>
 * Every handler implementation has only one class instance. It is used
 * for all the Roboconf instance models. Therefore, if it has to manage states,
 * it has to do it internally.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IMonitoringHandler {

	/**
	 * @return the handler name (expected to be unique)
	 */
	String getName();

	/**
	 * Sets the agent's identifiers.
	 * @param applicationName the application name
	 * @param scopedInstancePath the application instance associated with the agent
	 */
	void setAgentId( String applicationName, String scopedInstancePath );

	/**
	 * Prepares the handler for the next {@link #process()} invocation.
	 * @param associatedInstance the instance associated with the next run.
	 * <p>
	 * This instance has monitoring rules for this handler.
	 * And this instance's status must be {@value InstanceStatus#DEPLOYED_STARTED}.
	 * </p>
	 *
	 * @param eventId the event ID associated with the next run
	 * @param rawRulesText the monitoring rules as raw text (never null, to be parsed)
	 */
	void reset( Instance associatedInstance, String eventId, String rawRulesText );

	/**
	 * Processes and fires events if needed.
	 * @return a notification to be sent to the manager, or null if nothing to send.
	 */
	MsgNotifAutonomic process();
}
