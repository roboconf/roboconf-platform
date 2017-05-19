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
import java.util.Collections;
import java.util.List;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.messaging.api.AbstractMessageProcessor;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.business.ListenerCommand;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.extensions.MessagingContext.ThoseThat;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_agent.MsgCmdRemoveImport;
import net.roboconf.messaging.api.utils.MessagingUtils;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class ReconfigurableClientDm extends ReconfigurableClient<IDmClient> implements IDmClient {

	// Methods inherited from ReconfigurableClient

	@Override
	protected void openConnection( IMessagingClient newMessagingClient ) throws IOException {
		newMessagingClient.setOwnerProperties( RecipientKind.DM, this.domain, null, null );
		newMessagingClient.openConnection();
	}


	@Override
	protected void configureMessageProcessor( AbstractMessageProcessor<IDmClient> messageProcessor ) {
		messageProcessor.setMessagingClient( this );
	}


	@Override
	public RecipientKind getOwnerKind() {
		return RecipientKind.DM;
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
		getMessagingClient().openConnection();
	}


	@Override
	public void deleteMessagingServerArtifacts( Application application ) throws IOException {
		getMessagingClient().deleteMessagingServerArtifacts( application );
	}


	@Override
	public void closeConnection() throws IOException {

		final IMessagingClient toClose = resetInternalClient();
		if (toClose != null)
			toClose.closeConnection();
	}


	@Override
	public void sendMessageToAgent( Application application, Instance instance, Message message ) throws IOException {

		// The context match the one used by agents to listen to messages sent by the DM.
		String topicName = MessagingUtils.buildTopicNameForAgent( instance );
		MessagingContext ctx = new MessagingContext( RecipientKind.AGENTS, this.domain, topicName, application.getName());
		getMessagingClient().publish( ctx, message );
	}


	@Override
	public void listenToAgentMessages( Application application, ListenerCommand command ) throws IOException {
		listenToAgentMessages( getMessagingClient(), application, command );
	}


	@Override
	public void sendMessageToTheDm( Message msg ) throws IOException {

		MessagingContext ctx = new MessagingContext( RecipientKind.DM, this.domain, null );
		getMessagingClient().publish( ctx, msg );
	}


	@Override
	public void listenToTheDm( ListenerCommand command ) throws IOException {

		MessagingContext ctx = new MessagingContext( RecipientKind.DM, this.domain, null );
		if( command == ListenerCommand.STOP )
			getMessagingClient().unsubscribe( ctx );
		else
			getMessagingClient().subscribe( ctx );
	}


	@Override
	public void propagateAgentTermination( Application application, Instance rootInstance ) throws IOException {

		// Start with the deepest instances
		List<Instance> instances = InstanceHelpers.buildHierarchicalList( rootInstance );
		Collections.reverse( instances );

		// Roughly, we unpublish all the variables for all the instances that were on the agent's machine.
		// This code is VERY similar to ...ClientAgent#unpublishExports
		// The messages will go through JUST like if they were coming from other agents.
		this.logger.fine( "The DM is un-publishing exports related to agent of " + rootInstance + " (termination propagation)." );
		for( Instance instance : instances ) {
			for( MessagingContext ctx : MessagingContext.forExportedVariables(
					this.domain,
					application.getName(),
					instance,
					application.getExternalExports(),
					ThoseThat.IMPORT )) {

				MsgCmdRemoveImport message = new MsgCmdRemoveImport(
						application.getName(),
						ctx.getComponentOrFacetName(),
						InstanceHelpers.computeInstancePath( instance ));

				// FIXME: external exports are not handled here!!!!
				getMessagingClient().publish( ctx, message );
			}
		}
	}


	protected void listenToAgentMessages( IMessagingClient messagingClient, Application application, ListenerCommand command )
	throws IOException {

		// The context match the one used by agents to send messages to the DM.
		MessagingContext ctx = new MessagingContext( RecipientKind.DM, this.domain, application.getName());
		if( command == ListenerCommand.STOP )
			messagingClient.unsubscribe( ctx );
		else
			messagingClient.subscribe( ctx );
	}
}
