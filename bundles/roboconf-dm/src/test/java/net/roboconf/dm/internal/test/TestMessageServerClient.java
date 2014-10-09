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

package net.roboconf.dm.internal.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.messaging.client.AbstractMessageProcessor;
import net.roboconf.messaging.client.IAgentClient;
import net.roboconf.messaging.client.IDmClient;
import net.roboconf.messaging.client.MessageServerClientFactory;
import net.roboconf.messaging.messages.Message;

/**
 * A class to mock the messaging server and the IaaS.
 * @author Vincent Zurczak - Linagora
 */
public class TestMessageServerClient implements IDmClient {

	public final List<Message> sentMessages = new ArrayList<Message> ();
	public AtomicBoolean connected = new AtomicBoolean( false );


	@Override
	public void setParameters( String messageServerIp, String messageServerUsername, String messageServerPassword ) {
		// Nothing, we don't care
	}

	@Override
	public void closeConnection() throws IOException {
		this.connected.set( false );
	}

	@Override
	public void openConnection( AbstractMessageProcessor messageProcessor )
	throws IOException {
		this.connected.set( true );
	}

	@Override
	public void sendMessageToAgent( Application application, Instance instance, Message message )
	throws IOException {
		this.sentMessages.add( message );
	}

	@Override
	public void listenToAgentMessages( Application application, ListenerCommand command )
	throws IOException {
		// nothing, we do not care
	}

	@Override
	public void deleteMessagingServerArtifacts( Application application )
	throws IOException {
		// nothing, we do not care
	}

	@Override
	public boolean isConnected() {
		return this.connected.get();
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DmMessageServerClientFactory extends MessageServerClientFactory {
		@Override
		public IAgentClient createAgentClient() {
			return null;
		}

		@Override
		public IDmClient createDmClient() {
			return new TestMessageServerClient();
		}
	}
}
