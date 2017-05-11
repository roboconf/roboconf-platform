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

package net.roboconf.messaging.api.internal.client.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;

/**
 * A class to mock the messaging server and the IaaS.
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class TestClient implements IMessagingClient {

	public AtomicBoolean connected = new AtomicBoolean( false );
	public AtomicBoolean failClosingConnection = new AtomicBoolean( false );
	public AtomicBoolean failSubscribing = new AtomicBoolean( false );
	public AtomicBoolean failMessageSending = new AtomicBoolean( false );

	public Map<MessagingContext,List<Message>> ctxToMessages = new HashMap<> ();
	public List<Message> messagesForTheDm = new ArrayList<> ();
	public List<Message> messagesForAgents = new ArrayList<> ();
	public List<Message> allSentMessages = new ArrayList<> ();
	public Set<MessagingContext> subscriptions = new HashSet<> ();



	@Override
	public void closeConnection() throws IOException {
		if( this.failClosingConnection.get())
			throw new IOException( "Closing the connection was configured to fail." );

		this.connected.set( false );
	}


	@Override
	public void openConnection() throws IOException {
		this.connected.set( true );
	}


	@Override
	public String getMessagingType() {
		return MessagingConstants.FACTORY_TEST;
	}


	@Override
	public Map<String,String> getConfiguration() {
		return Collections.singletonMap(MessagingConstants.MESSAGING_TYPE_PROPERTY, MessagingConstants.FACTORY_TEST);
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


	@Override
	public void setMessageQueue( RoboconfMessageQueue messageQueue ) {
		// nothing
	}


	@Override
	public void subscribe( MessagingContext ctx ) throws IOException {

		if( this.failSubscribing.get())
			throw new IOException( "Subscribing was configured to fail." );

		this.subscriptions.add( ctx );
	}


	@Override
	public void unsubscribe( MessagingContext ctx ) throws IOException {
		this.subscriptions.remove( ctx );
	}


	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {

		if( this.failMessageSending.get())
			throw new IOException( "Sending a message was configured to fail." );

		List<Message> messages = this.ctxToMessages.get( ctx );
		if( messages == null ) {
			messages = new ArrayList<> ();
			this.ctxToMessages.put( ctx, messages );
		}

		messages.add( msg );

		this.allSentMessages.add( msg );
		if( ctx.getKind() == RecipientKind.DM )
			this.messagesForTheDm.add( msg );
		else
			this.messagesForAgents.add( msg );
	}


	@Override
	public void setOwnerProperties( RecipientKind ownerKind, String domain, String applicationName, String scopedInstancePath ) {
		// We do not care...
	}


	/**
	 * Clears all the stored messages.
	 */
	public void clearMessages() {

		this.ctxToMessages.clear();
		this.messagesForAgents.clear();
		this.messagesForTheDm.clear();
		this.allSentMessages.clear();
	}
}
