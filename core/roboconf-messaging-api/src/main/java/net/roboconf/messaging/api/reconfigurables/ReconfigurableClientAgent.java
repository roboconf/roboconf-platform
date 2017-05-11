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

package net.roboconf.messaging.api.reconfigurables;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.business.IAgentClient;
import net.roboconf.messaging.api.business.ListenerCommand;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.extensions.MessagingContext.ThoseThat;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdAddImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRequestImport;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.api.utils.MessagingUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReconfigurableClientAgent extends ReconfigurableClient<IAgentClient> implements IAgentClient {

	private final ConcurrentHashMap<String,String> externalExports = new ConcurrentHashMap<> ();
	private String applicationName, scopedInstancePath, ipAddress;
	private boolean needsModel = false;


	// Methods inherited from ReconfigurableClient


	@Override
	protected void openConnection( IMessagingClient newMessagingClient ) throws IOException {

		newMessagingClient.setOwnerProperties( getOwnerKind(), this.domain, this.applicationName, this.scopedInstancePath );
		newMessagingClient.openConnection();
		listenToTheDm( newMessagingClient, ListenerCommand.START );

		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( this.applicationName, this.scopedInstancePath, this.ipAddress );
		msg.setModelRequired( this.needsModel );

		MessagingContext ctx = new MessagingContext( RecipientKind.DM, this.domain, this.applicationName );
		newMessagingClient.publish( ctx, msg );
	}


	@Override
	protected void configureMessageProcessor( AbstractMessageProcessor<IAgentClient> messageProcessor ) {
		messageProcessor.setMessagingClient( this );
	}


	@Override
	public RecipientKind getOwnerKind() {
		return RecipientKind.AGENTS;
	}


	// Wrapping of the internal client


	@Override
	public void setMessageQueue( RoboconfMessageQueue messageQueue ) {
		getMessagingClient().setMessageQueue( messageQueue );
	}


	@Override
	public boolean isConnected() {
		return getMessagingClient().isConnected();
	}


	@Override
	public void openConnection() throws IOException {
		openConnection( getMessagingClient());
	}


	@Override
	public void closeConnection() throws IOException {

		final IMessagingClient toClose = resetInternalClient();
		if (toClose != null)
			toClose.closeConnection();
	}


	@Override
	public void publishExports( Instance instance ) throws IOException {

		// For all the exported variables...
		// ... find the component or facet name...
		Set<String> names = VariableHelpers.findPrefixesForExportedVariables( instance );
		if( names.isEmpty())
			this.logger.fine( "Agent '" + getAgentId() + "' is publishing its exports." );

		else for( String facetOrComponentName : names ) {
			publishExports( instance, facetOrComponentName );
		}
	}


	@Override
	public void publishExports( Instance instance, String facetOrComponentName ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is publishing its exports prefixed by " + facetOrComponentName + "." );

		// Find the variables to export.
		Map<String,String> toPublishInternally = new HashMap<> ();
		Map<String,String> toPublishExternally = new HashMap<> ();

		Map<String,String> exports = InstanceHelpers.findAllExportedVariables( instance );
		for( Map.Entry<String,String> entry : exports.entrySet()) {

			// Publishing an export may be about a facet or component name or about an external prefix.
			// If an "internal prefix" is required, export both the internal and associated external, if any.
			String alias = this.externalExports.get( entry.getKey());
			if( entry.getKey().startsWith( facetOrComponentName + "." )) {
				toPublishInternally.put( entry.getKey(), entry.getValue());
				if( alias != null )
					toPublishExternally.put( alias, entry.getValue());
			}

			// If an external prefix is required, only export the external variables
			else if( alias != null && alias.startsWith( facetOrComponentName + "." )) {
				toPublishExternally.put( alias, entry.getValue());
			}
		}

		// Publish the internal exports
		if( ! toPublishInternally.isEmpty()) {
			MsgCmdAddImport message = new MsgCmdAddImport(
					this.applicationName,
					facetOrComponentName,
					InstanceHelpers.computeInstancePath( instance ),
					toPublishInternally );

			MessagingContext ctx = new MessagingContext(
					RecipientKind.AGENTS,
					this.domain,
					facetOrComponentName,
					ThoseThat.IMPORT,
					this.applicationName );

			getMessagingClient().publish( ctx, message );
		}

		// Publish the external ones, if any
		if( ! toPublishExternally.isEmpty()) {
			String varName = toPublishExternally.keySet().iterator().next();
			String appTplName = VariableHelpers.parseVariableName( varName ).getKey();

			MsgCmdAddImport message = new MsgCmdAddImport(
					this.applicationName,
					appTplName,
					InstanceHelpers.computeInstancePath( instance ),
					toPublishExternally );

			MessagingContext ctx = new MessagingContext(
					RecipientKind.INTER_APP,
					this.domain,
					appTplName,
					ThoseThat.IMPORT,
					this.applicationName );

			getMessagingClient().publish( ctx, message );
		}
	}


	@Override
	public void unpublishExports( Instance instance ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is un-publishing its exports." );

		// For all the exported variables...
		// ... find the component or facet name...
		for( MessagingContext ctx : MessagingContext.forExportedVariables(
				this.domain,
				this.applicationName,
				instance,
				this.externalExports,
				ThoseThat.IMPORT )) {

			// Log here, for debug
			this.logger.fine( "Agent '" + getAgentId() + "' is un-publishing its exports (" + ctx + ")." );

			// Un-publish them
			MsgCmdRemoveImport message = new MsgCmdRemoveImport(
					this.applicationName,
					ctx.getComponentOrFacetName(),
					InstanceHelpers.computeInstancePath( instance ));

			getMessagingClient().publish( ctx, message );
		}
	}


	@Override
	public void listenToRequestsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException {

		// Find the right contexts to subscribe.
		// This depends on the exported variables.
		for( MessagingContext ctx : MessagingContext.forExportedVariables(
				this.domain,
				this.applicationName,
				instance,
				this.externalExports,
				ThoseThat.EXPORT )) {

			if( command == ListenerCommand.START ) {
				this.logger.fine( "Agent '" + getAgentId() + "' starts listening requests from other agents (" + ctx + ")." );
				getMessagingClient().subscribe( ctx );

			} else {
				this.logger.fine( "Agent '" + getAgentId() + "' stops listening requests from other agents (" + ctx + ")." );
				getMessagingClient().unsubscribe( ctx );
			}
		}
	}


	@Override
	public void requestExportsFromOtherAgents( Instance instance ) throws IOException {
		this.logger.fine( "Agent '" + getAgentId() + "' is requesting exports from other agents." );

		// For all the imported variables...
		// ... find the component or facet name...
		for( MessagingContext ctx : MessagingContext.forImportedVariables(
				this.domain,
				this.applicationName,
				instance,
				ThoseThat.EXPORT )) {

			// Log here, for debug
			this.logger.fine( "Agent '" + getAgentId() + "' is requesting exports from other agents (" + ctx + ")." );

			// ... and ask to publish them.
			// Grouping variable requests by prefix reduces the number of messages.
			MsgCmdRequestImport message = new MsgCmdRequestImport( this.applicationName, ctx.getComponentOrFacetName());
			getMessagingClient().publish( ctx, message );
		}
	}


	@Override
	public void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException {

		// With RabbitMQ, and for agents, listening to others means
		// create a binding between the "agents" exchange and the agent's queue.
		for( MessagingContext ctx : MessagingContext.forImportedVariables(
				this.domain,
				this.applicationName,
				instance,
				ThoseThat.IMPORT )) {

			// On which routing key do export go? Those.that.import...
			if( command == ListenerCommand.START ) {
				this.logger.fine( "Agent '" + getAgentId() + "' starts listening exports from other agents (" + ctx + ")." );
				getMessagingClient().subscribe( ctx );

			} else {
				this.logger.fine( "Agent '" + getAgentId() + "' stops listening exports from other agents (" + ctx + ")." );
				getMessagingClient().unsubscribe( ctx );
			}
		}
	}


	@Override
	public void sendMessageToTheDm( Message message ) throws IOException {

		// The context match the one used by the DM to listen to messages sent by agents.
		this.logger.fine( "Agent '" + getAgentId() + "' is sending a " + message.getClass().getSimpleName() + " message to the DM." );
		MessagingContext ctx = new MessagingContext( RecipientKind.DM, this.domain, this.applicationName );
		getMessagingClient().publish( ctx, message );
	}


	@Override
	public void listenToTheDm( ListenerCommand command ) throws IOException {
		listenToTheDm( getMessagingClient(), command );
	}


	private void listenToTheDm( IMessagingClient client, ListenerCommand command ) throws IOException {

		// The context match the one used by the DM to send a message to an agent.
		// The agent client MUST have a scoped instance path!
		String topicName = MessagingUtils.buildTopicNameForAgent( this.scopedInstancePath );
		MessagingContext ctx = new MessagingContext( RecipientKind.AGENTS, this.domain, topicName, this.applicationName );
		if( command == ListenerCommand.START ) {
			this.logger.fine( "Agent '" + getAgentId() + "' starts listening to the DM." );
			client.subscribe( ctx );

		} else {
			this.logger.fine( "Agent '" + getAgentId() + "' stops listening to the DM." );
			client.unsubscribe( ctx );
		}
	}


	// Setter methods


	@Override
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;

		// Propagate the information to the internal client.
		getMessagingClient().setOwnerProperties( getOwnerKind(), this.domain, applicationName, this.scopedInstancePath );
	}


	@Override
	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;

		// Propagate the information to the internal client.
		getMessagingClient().setOwnerProperties( getOwnerKind(), this.domain, this.applicationName, scopedInstancePath );
	}


	@Override
	public void setExternalMapping( Map<String,String> externalExports ) {

		this.externalExports.clear();
		if( externalExports != null )
			this.externalExports.putAll( externalExports );
	}


	public void setIpAddress( String ipAddress ) {
		this.ipAddress = ipAddress;
	}


	public void setNeedsModel( boolean needsModel ) {
		this.needsModel = needsModel;
	}


	private String getAgentId() {
		return Utils.isEmptyOrWhitespaces( this.scopedInstancePath ) ? "?" : this.scopedInstancePath;
	}
}
