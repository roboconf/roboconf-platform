/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.messaging.reconfigurables;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.internal.client.MessageServerClientFactory;
import net.roboconf.messaging.internal.client.dismiss.DismissClientAgent;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifHeartbeat;
import net.roboconf.messaging.processors.AbstractMessageProcessor;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ReconfigurableClientAgent extends ReconfigurableClient<IAgentClient> implements IAgentClient {

	private String applicationName, rootInstanceName, ipAddress;


	// Methods inherited from ReconfigurableClient

	@Override
	protected IAgentClient createNewMessagingClient(
			String messageServerIp,
			String messageServerUser,
			String messageServerPwd,
			String factoryName )
	throws IOException {

		MessageServerClientFactory factory = new MessageServerClientFactory();
		IAgentClient client = factory.createAgentClient( factoryName );
		if( client != null )
			client.setParameters( messageServerIp, messageServerUser, messageServerPwd );

		return client;
	}


	@Override
	protected void openConnection( IAgentClient newMessagingClient ) throws IOException {

		newMessagingClient.setApplicationName( this.applicationName );
		newMessagingClient.setRootInstanceName( this.rootInstanceName );
		newMessagingClient.openConnection();

		newMessagingClient.listenToTheDm( ListenerCommand.START );
		newMessagingClient.sendMessageToTheDm( new MsgNotifHeartbeat( this.applicationName, this.rootInstanceName, this.ipAddress ));
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
	public void setParameters( String messageServerIp, String messageServerUsername, String messageServerPassword ) {
		getMessagingClient().setParameters( messageServerIp, messageServerUsername, messageServerPassword );
	}


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
		getMessagingClient().closeConnection();
		resetInternalClient();
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

	public void setIpAddress( String ipAddress ) {
		this.ipAddress = ipAddress;
	}

	@Override
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	@Override
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}
}
