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

import javax.websocket.DeploymentException;
import javax.websocket.Session;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.messaging.api.reconfigurables.ReconfigurableClientAgent;

/**
 * The Http client for an agent (this client is intended to be used from within an agent).
 *
 * @author Pierre-Yves Gibello - Linagora
 */
public class HttpAgentClient extends HttpClient implements IAgentClient {

	private String applicationName, scopedInstancePath;
	private boolean dmListening = false;
	private boolean exportListening = false;
	private boolean requestListening = false;
	
	/**
	 * @param reconfigurable
	 * @param http
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws DeploymentException 
	 */
	public HttpAgentClient( final ReconfigurableClientAgent reconfigurable, final String ip, final String port) throws DeploymentException, IOException, URISyntaxException {
		super(reconfigurable, ip, port);
	}


	@Override
	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
	}

	public String getScopedInstancePath() {
		return this.scopedInstancePath;
	}

	@Override
	public synchronized void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}

	public String getApplicationName() {
		return applicationName;
	}

	@Override
	public String getId() {
		return Utils.isEmptyOrWhitespaces(this.scopedInstancePath) ? "?" : this.scopedInstancePath;
	}

	@Override
	public synchronized void openConnection() throws IOException {

		// Already connected? Do nothing
		if (this.isConnected) {
			this.logger.finest("The HTTP client is already open");
			return;
		}

		this.isConnected = true;
		this.logger.finest("Connection for the HTTP client opened");
	}
	
	@Override
	public synchronized void closeConnection() throws IOException {
		// Already closed? Do nothing
		if (!this.isConnected) {
			this.logger.finest("The HTTP client is already closed");
			return;
		}

		try {
			Session session = openOrFindSession(applicationName, scopedInstancePath);
			SubscriptionMessage msg = new SubscriptionMessage(this.getId(),
				null, HttpMessagingUtils.buildExchangeName(this.applicationName, false), null, false);
			sendMessage(session, msg);
		} catch(IOException e) {
			// ignore
		}

		this.isConnected = false;
		this.logger.finest("Closing connection for the HTTP client");
	}

	@Override
	public void publishExports( Instance instance ) throws IOException {

		// For all the exported variables...
		// ... find the component or facet name...
		Set<String> names = VariableHelpers.findPrefixesForExportedVariables(instance);
		if (names.isEmpty())
			this.logger.fine("Agent '" + getId() + "' is publishing its exports.");

		else for (String facetOrComponentName : names) {
			publishExports(instance, facetOrComponentName);
		}
	}


	@Override
	public synchronized void publishExports( Instance instance, String facetOrComponentName ) throws IOException {
		this.logger.fine("Agent '" + getId() + "' is publishing its exports prefixed by " + facetOrComponentName + ".");

		// Find the variables to export.
		Map<String, String> toPublish = new HashMap<>();
		Map<String, String> exports = InstanceHelpers.findAllExportedVariables(instance);
		for (Map.Entry<String, String> entry : exports.entrySet()) {
			if (entry.getKey().startsWith(facetOrComponentName + "."))
				toPublish.put(entry.getKey(), entry.getValue());
		}

		// Publish them
		if (!toPublish.isEmpty()) {
			MsgCmdAddImport message = new MsgCmdAddImport(
					facetOrComponentName,
					InstanceHelpers.computeInstancePath(instance),
					toPublish);

			//TODO: instance or scopedIP ??
			sendMessage(HttpMessagingUtils.buildExchangeName(this.applicationName, false), "those.that.import." + facetOrComponentName, message);
		}
	}


	@Override
	public synchronized void unpublishExports( Instance instance ) throws IOException {
		this.logger.fine("Agent '" + getId() + "' is un-publishing its exports.");

		// For all the exported variables...
		// ... find the component or facet name...
		for (String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables(instance)) {

			// Log here, for debug
			this.logger.fine("Agent '" + getId() + "' is un-publishing its exports (" + facetOrComponentName + ").");

			// Publish them
			MsgCmdRemoveImport message = new MsgCmdRemoveImport(
					facetOrComponentName,
					InstanceHelpers.computeInstancePath(instance));

			sendMessage(HttpMessagingUtils.buildExchangeName(this.applicationName, false), "those.that.import." + facetOrComponentName, message);
		}
	}

	@Override
	public synchronized void listenToRequestsFromOtherAgents( ListenerCommand command, Instance instance )
			throws IOException {

		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {

			String routingKey = "those.that.export." + facetOrComponentName;
			String exchangeName = HttpMessagingUtils.buildExchangeName( this.applicationName, false );

			if (command == ListenerCommand.START) {
				this.logger.fine("Agent '" + getId() + "' starts listening to requests.");
				SubscriptionMessage message = new SubscriptionMessage(getId(), this.applicationName, exchangeName, routingKey, true);
				sendMessage(openOrFindSession(exchangeName, routingKey), message);
				requestListening = true;
			} else {
				this.logger.fine("Agent '" + getId() + "' stops listening to requests.");
				SubscriptionMessage message = new SubscriptionMessage(getId(), this.applicationName, exchangeName, routingKey, false);
				sendMessage(openOrFindSession(exchangeName, routingKey), message);
				requestListening = false;
			}
		}
	}

	@Override
	public synchronized void requestExportsFromOtherAgents( Instance instance ) throws IOException {
		this.logger.fine( "Agent '" + getId() + "' is requesting exports from other agents." );

		// For all the imported variables...
		// ... find the component or facet name...
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {

			// Log here, for debug
			this.logger.fine( "Agent '" + getId() + "' is requesting exports from other agents (" + facetOrComponentName + ")." );

			// ... and ask to publish them.
			// Grouping variable requests by prefix reduces the number of messages.
			MsgCmdRequestImport message = new MsgCmdRequestImport( facetOrComponentName );
			sendMessage(HttpMessagingUtils.buildExchangeName(this.applicationName, false), "those.that.export." + facetOrComponentName, message);
		}
	}


	@Override
	public synchronized void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException {
		
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {

			String routingKey = "those.that.import." + facetOrComponentName;
			String exchangeName = HttpMessagingUtils.buildExchangeName( this.applicationName, false );

			if (command == ListenerCommand.START) {
				this.logger.fine("Agent '" + getId() + "' starts listening to exports.");
				SubscriptionMessage message = new SubscriptionMessage(getId(), this.applicationName, exchangeName, routingKey, true);
				sendMessage(openOrFindSession(exchangeName, routingKey), message);
				exportListening = true;
			} else {
				this.logger.fine("Agent '" + getId() + "' stops listening to exports.");
				SubscriptionMessage message = new SubscriptionMessage(getId(), this.applicationName, exchangeName, routingKey, false);
				sendMessage(openOrFindSession(exchangeName, routingKey), message);
				exportListening = false;
			}
		}
	}


	@Override
	public synchronized void sendMessageToTheDm( Message message ) throws IOException {

		this.logger.fine("Agent '" + getId() + "' is sending a " + message.getClass().getSimpleName() + " message to the DM.");
		sendMessage("DM", "", message);
	}


	@Override
	public synchronized void listenToTheDm( ListenerCommand command ) throws IOException {

		if (command == ListenerCommand.START) {
			this.logger.fine("Agent '" + getId() + "' starts listening to the DM.");
			Session session = openOrFindSession(applicationName, scopedInstancePath);
			SubscriptionMessage msg = new SubscriptionMessage(this.getId(),
					null, HttpMessagingUtils.buildExchangeName(this.applicationName, false), HttpMessagingUtils.buildRoutingKeyForAgent(this.scopedInstancePath), true);
			sendMessage(session, msg);
			dmListening = true;
		} else {
			this.logger.fine("Agent '" + getId() + "' stops listening to the DM.");
			Session session = openOrFindSession(applicationName, scopedInstancePath);
			SubscriptionMessage msg = new SubscriptionMessage(this.getId(),
					null, HttpMessagingUtils.buildExchangeName(this.applicationName, false), HttpMessagingUtils.buildRoutingKeyForAgent(this.scopedInstancePath), false);
			sendMessage(session, msg);
			dmListening = false;
		}
	}
	
	@Override
	public void addMessageToMQ(Message message) {
		if(message instanceof MsgCmdAddImport || message instanceof MsgCmdRemoveImport) {
			if(exportListening) messageQueue.add(message);
		} else if (message instanceof MsgCmdRequestImport) {
			if(requestListening) messageQueue.add(message);
		} else {
			if(dmListening) messageQueue.add(message);
		}
	}

}
