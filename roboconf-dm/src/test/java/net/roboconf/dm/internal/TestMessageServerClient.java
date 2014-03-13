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

package net.roboconf.dm.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import net.roboconf.messaging.client.IMessageProcessor;
import net.roboconf.messaging.client.IMessageServerClient;
import net.roboconf.messaging.client.InteractionType;
import net.roboconf.messaging.messages.Message;

/**
 * A class to mock the messaging server and the IaaS.
 * @author Vincent Zurczak - Linagora
 */
public class TestMessageServerClient implements IMessageServerClient {

	public final Map<Message,String> messageToRoutingKey = new HashMap<Message,String> ();
	public AtomicBoolean connectionOpen = new AtomicBoolean( false );
	public AtomicBoolean connectionClosed = new AtomicBoolean( false );



	@Override
	public void setMessageServerIp( String messageServerIp ) {
		// nothing, we do not care
	}


	@Override
	public void setApplicationName( String applicationName ) {
		// nothing, we do not care
	}


	@Override
	public void openConnection() throws IOException {
		this.connectionOpen.set( true );
	}


	@Override
	public void closeConnection() throws IOException {
		this.connectionClosed.set( true );
	}


	@Override
	public void subscribeTo( String sourceName, InteractionType interactionType, String routingKey, IMessageProcessor messageprocessor )
	throws IOException {
		// nothing, we do not care
	}


	@Override
	public void unsubscribeTo( InteractionType interactionType, String routingKey )
	throws IOException {
		// nothing, we do not care
	}


	@Override
	public void publish( InteractionType interactionType, String routingKey, Message message )
	throws IOException {
		this.messageToRoutingKey.put( message, routingKey );
	}


	@Override
	public void deleteQueueOrTopic( InteractionType interactionType, String routingKey )
	throws IOException {
		// nothing
	}
}
