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

package net.roboconf.messaging.api.reconfigurables;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.factory.MessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.dismiss.DismissClientAgent;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.api.processors.AbstractMessageProcessor;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReconfigurableClientAgent extends ReconfigurableClient<IAgentClient> implements IAgentClient {

	private String applicationName, scopedInstancePath, ipAddress;
	private boolean needsModel = false;


	// Methods inherited from ReconfigurableClient

	@Override
	protected IAgentClient createMessagingClient( String factoryName )
	throws IOException {
		IAgentClient client = null;
		MessagingClientFactoryRegistry registry = getRegistry();
		if (registry != null) {
			MessagingClientFactory factory = registry.getMessagingClientFactory(factoryName);
			if (factory != null) {
				client = factory.createAgentClient(this);
			}
		}
		return client;
	}


	@Override
	protected void openConnection( IAgentClient newMessagingClient ) throws IOException {

		newMessagingClient.setApplicationName( this.applicationName );
		newMessagingClient.setScopedInstancePath( this.scopedInstancePath );
		newMessagingClient.openConnection();

		newMessagingClient.listenToTheDm( ListenerCommand.START );

		MsgNotifHeartbeat msg = new MsgNotifHeartbeat( this.applicationName, this.scopedInstancePath, this.ipAddress );
		msg.setModelRequired( this.needsModel );
		newMessagingClient.sendMessageToTheDm( msg );
	}


	@Override
	protected IAgentClient getDismissedClient() {
		return new DismissClientAgent();
	}


	@Override
	protected void configureMessageProcessor( AbstractMessageProcessor<IAgentClient> messageProcessor ) {
		messageProcessor.setMessagingClient( this );
	}


	// Wrapping of the internal client


	@Override
	public void setMessageQueue( LinkedBlockingQueue<Message> messageQueue ) {
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
	public void closeConnection() throws IOException {
		final IAgentClient toClose = resetInternalClient();
		if (toClose != null) {
			toClose.closeConnection();
		}
	}


	@Override
	public void publishExports( Instance instance ) throws IOException {
		getMessagingClient().publishExports( instance );
	}


	@Override
	public void publishExports( Instance instance, String facetOrComponentName ) throws IOException {
		getMessagingClient().publishExports( instance, facetOrComponentName );
	}


	@Override
	public void unpublishExports( Instance instance ) throws IOException {
		getMessagingClient().unpublishExports( instance );
	}


	@Override
	public void listenToRequestsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException {
		getMessagingClient().listenToRequestsFromOtherAgents( command, instance );
	}


	@Override
	public void requestExportsFromOtherAgents( Instance instance ) throws IOException {
		getMessagingClient().requestExportsFromOtherAgents( instance );
	}


	@Override
	public void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance ) throws IOException {
		getMessagingClient().listenToExportsFromOtherAgents( command, instance );
	}


	@Override
	public void sendMessageToTheDm( Message message ) throws IOException {
		getMessagingClient().sendMessageToTheDm( message );
	}


	@Override
	public void listenToTheDm( ListenerCommand command ) throws IOException {
		getMessagingClient().listenToTheDm( command );
	}


	// Setter methods

	@Override
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	@Override
	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
	}


	public void setIpAddress( String ipAddress ) {
		this.ipAddress = ipAddress;
	}


	public void setNeedsModel( boolean needsModel ) {
		this.needsModel = needsModel;
	}
}
