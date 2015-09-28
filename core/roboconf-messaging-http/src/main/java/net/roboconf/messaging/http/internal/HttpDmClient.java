/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.http.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.websocket.Session;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_dm_to_dm.MsgEcho;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientDm;

/**
 * The HTTP messaging client for the DM.
 * <p>
 * This client allows to send/retrieve messages to the agents.
 * </p>
 * @author Pierre-Yves Gibello - Linagora
 */
public class HttpDmClient extends HttpClient implements IDmClient {

	private boolean isConnected = false;
	private boolean listening = false;
	private boolean dmListening = false;

	/**
	 * @param reconfigurable
	 * @param agentAddresses
	 */
	public HttpDmClient(final ReconfigurableClientDm reconfigurable, String ipAddress, String port) {
		super(reconfigurable, ipAddress, port);
	}

	@Override
	public String getId() {
		return "DM";
	}

	@Override
	public synchronized void openConnection() throws IOException {

		// Already connected? Do nothing
		if (this.isConnected) {
			this.logger.info("The HTTP DM client is already opened.");
			return;
		}

		this.logger.finest("Opening connection for the HTTP DM client.");
		super.openConnection();

		// TODO maybe trigger the web server registration/startup here, if not already done (calling a method in HttpClientFactory... instead of putting it there in starting() ??
		this.isConnected = true;

		this.logger.finest("Connection for the HTTP DM client opened.");
	}

	@Override
	public synchronized void closeConnection() throws IOException {

		// Already closed? Do nothing
		if (!this.isConnected) {
			this.logger.finest("The HTTP DM client is already closed.");
			return;
		}

		this.logger.finest("Closing connection for the HTTP DM client.");
		super.closeConnection();

		this.isConnected = false;

		this.logger.finest("Connection for the HTTP DM client closed.");
	}


	@Override
	public final synchronized boolean isConnected() {
		return this.isConnected;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IDmClient
	 * #publishMessageToAgent(net.roboconf.core.model.beans.Application, net.roboconf.core.model.beans.Instance, net.roboconf.messaging.api.messages.Message)
	 */
	@Override
	public synchronized void sendMessageToAgent( Application application, Instance instance, Message message )
			throws IOException {

		this.logger.fine("The DM sends a message to TODO. Message type: " + message.getClass().getSimpleName());
		Session sess = openOrFindSession(application.getName(), HttpMessagingUtils.buildRoutingKeyForAgent(instance));
		
		sendMessage(sess, HttpMessagingUtils.buildExchangeName(application, false), HttpMessagingUtils.buildRoutingKeyForAgent(instance), message);
		this.logger.fine("The DM sent a message to TODO. Message type: " + message.getClass().getSimpleName());
	}

	/* (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IDmClient
	 * #listenToAgentMessages(net.roboconf.core.model.beans.Application, net.roboconf.messaging.api.client.IClient.ListenerCommand)
	 */
	@Override
	public synchronized void listenToAgentMessages( Application application, ListenerCommand command )
			throws IOException {

		if (command == ListenerCommand.STOP) {
			this.logger.fine("The DM stops listening agents messages for the '" + application.getName() + "' application.");
			
			closeSession(application.getName(), null);
			
			listening = false;

		} else {
			// Already listening? Ignore...
			listening = true;

			openOrFindSession(application.getName(), null);

			this.logger.fine("The DM starts listening agents messages for the '" + application.getName() + "' application.");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient
	 * #sendMessageToTheDm(net.roboconf.messaging.api.messages.Message)
	 */
	@Override
	public synchronized void sendMessageToTheDm( Message msg ) throws IOException {

		// The DM can send messages to itself (e.g. for debug).
		// This method could also be used to broadcast information to (potential) other DMs.
		this.logger.fine("The DM sends a message to the itself.");

		sendMessage(openOrFindSession("DM", ""), "DM", null, msg);

		this.logger.fine("The DM has sent a message to itself.");
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IClient
	 * #listenToTheDm(net.roboconf.messaging.api.client.IClient.ListenerCommand)
	 */
	@Override
	public synchronized void listenToTheDm( ListenerCommand command )
			throws IOException {
		if (command == ListenerCommand.START) {
			dmListening = true;
		} else {
			dmListening = false;
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IDmClient
	 * #deleteMessagingServerArtifacts(net.roboconf.core.model.beans.Application)
	 */
	@Override
	public synchronized void deleteMessagingServerArtifacts( Application application )
			throws IOException {
		// TODO
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.messaging.api.client.IDmClient
	 * #propagateAgentTermination(net.roboconf.core.model.beans.Application, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public synchronized void propagateAgentTermination( Application application, Instance rootInstance )
			throws IOException {

		this.logger.fine("The DM is propagating the termination of agent '" + rootInstance + "'.");

		// Start with the deepest instances
		List<Instance> instances = InstanceHelpers.buildHierarchicalList(rootInstance);
		Collections.reverse(instances);

		// Roughly, we unpublish all the variables for all the instances that were on the agent's machine.
		// This code is VERY similar to ...ClientAgent#unpublishExports
		for (Instance instance : instances) {
			for (String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables(instance)) {

				MsgCmdRemoveImport message = new MsgCmdRemoveImport(
						facetOrComponentName,
						InstanceHelpers.computeInstancePath(instance));

				sendMessage(HttpMessagingUtils.buildExchangeName(application.getName(), false), "those.that.import." + facetOrComponentName, message);
			}
		}
	}

	@Override
	public void addMessageToMQ(Message message) {
		if(listening) messageQueue.add(message);
		else if(dmListening && message instanceof MsgEcho) messageQueue.add(message);
	}
}
