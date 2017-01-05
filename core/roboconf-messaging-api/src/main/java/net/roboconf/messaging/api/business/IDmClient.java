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

package net.roboconf.messaging.api.business;

import java.io.IOException;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.api.messages.Message;

/**
 * A client for the DM.
 * <p>
 * The DM has one client for ALL the applications.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface IDmClient extends IClient {

	/**
	 * Sends a message to an agent.
	 * @param application the application associated with the given agent
	 * @param instance an instance managed on the agent (used to find the root instance name)
	 * @param message the message to send
	 * @throws IOException if something went wrong
	 */
	void sendMessageToAgent( Application application, Instance instance, Message message ) throws IOException;

	/**
	 * Configures the listener for messages sent by agents.
	 * @param application the application associated with the given agents
	 * @param command {@link ListenerCommand#START} to start listening, {@link ListenerCommand#STOP} to stop listening
	 * @throws IOException if something went wrong
	 */
	void listenToAgentMessages( Application application, ListenerCommand command ) throws IOException;

	/**
	 * Deletes all the server artifacts related to this application.
	 * <p>
	 * This method must be called when ALL the agents have closed
	 * their connection and BEFORE the DM's connection is closed.
	 * </p>
	 *
	 * @param application the application whose messaging artifacts must be deleted
	 * @throws IOException if something went wrong
	 */
	void deleteMessagingServerArtifacts( Application application ) throws IOException;

	/**
	 * Propagates to all the agents the fact that a given agent is not here anymore.
	 * <p>
	 * Generally, when an agent undeploys something, it sends notifications to other
	 * agents, which themselves invoke their "update" method. However, when the DM
	 * terminates a virtual machine, the agent does not always send these notifications
	 * to the other agents. Therefore, the DM has to do it. Since it has the application
	 * model, it can do it.
	 * </p>
	 * <p>
	 * The DM should notify all the potential agents that may be concerned. Roughly, it is
	 * equivalent to unpublishing all the exports of all the instances that were available on
	 * the terminated machine.
	 * </p>
	 *
	 * @param application the application the agent is associated with
	 * @param rootInstance the root instance associated with the agent (not null)
	 * @throws IOException if something went wrong
	 */
	void propagateAgentTermination( Application application, Instance rootInstance ) throws IOException;
}
