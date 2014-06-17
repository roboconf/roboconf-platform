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

package net.roboconf.agent.tests;

import java.io.IOException;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class NilMessagingClient implements IAgentClient {

	@Override
	public void setParameters( String messageServerIp, String messageServerUsername, String messageServerPassword ) {
		// nothing
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public void openConnection( AbstractMessageProcessor messageProcessor ) throws IOException {
		// nothing

	}

	@Override
	public void closeConnection() throws IOException {
		// nothing
	}

	@Override
	public void setApplicationName( String applicationName ) {
		// nothing
	}

	@Override
	public void setRootInstanceName( String rootInstanceName ) {
		// nothing
	}

	@Override
	public void publishExports( Instance instance ) throws IOException {
		// nothing
	}

	@Override
	public void publishExports( Instance instance, String facetOrComponentName ) throws IOException {
		// nothing
	}

	@Override
	public void unpublishExports( Instance instance ) throws IOException {
		// nothing
	}

	@Override
	public void listenToRequestsFromOtherAgents( ListenerCommand command, Instance instance )
	throws IOException {
		// nothing
	}

	@Override
	public void requestExportsFromOtherAgents( Instance instance ) throws IOException {
		// nothing
	}

	@Override
	public void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance )
	throws IOException {
		// nothing
	}

	@Override
	public void sendMessageToTheDm( Message message ) throws IOException {
		// nothing
	}

	@Override
	public void listenToTheDm( ListenerCommand command ) throws IOException {
		// nothing
	}
}
