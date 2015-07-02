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

package net.roboconf.messaging.api.internal.client.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.client.IAgentClient;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestClientAgent implements IAgentClient {

	public final List<Message> messagesForTheDm = new ArrayList<> ();
	public AtomicInteger messagesForAgentsCount = new AtomicInteger();
	public AtomicBoolean connected = new AtomicBoolean( false );
	public AtomicBoolean failMessageSending = new AtomicBoolean( false );
	// TODO(?) : May be a good idea to make the previous parameters configurable via setConfiguration().

	private String applicationName, scopedInstancePath;


	@Override
	public boolean isConnected() {
		return this.connected.get();
	}

	@Override
	public void openConnection() throws IOException {
		this.connected.set( true );
	}

	@Override
	public void closeConnection() throws IOException {
		this.connected.set( false );
	}

	@Override
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}

	@Override
	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
	}

	@Override
	public void publishExports( Instance instance ) throws IOException {

		if( this.failMessageSending.get())
			throw new IOException( "Message sending was configured to fail." );

		this.messagesForAgentsCount.incrementAndGet();
	}

	@Override
	public void publishExports( Instance instance, String facetOrComponentName ) throws IOException {

		if( this.failMessageSending.get())
			throw new IOException( "Message sending was configured to fail." );

		this.messagesForAgentsCount.incrementAndGet();
	}

	@Override
	public void unpublishExports( Instance instance ) throws IOException {

		if( this.failMessageSending.get())
			throw new IOException( "Message sending was configured to fail." );

		this.messagesForAgentsCount.incrementAndGet();
	}

	@Override
	public void listenToRequestsFromOtherAgents( ListenerCommand command, Instance instance )
	throws IOException {
		// nothing
	}

	@Override
	public void requestExportsFromOtherAgents( Instance instance ) throws IOException {

		if( this.failMessageSending.get())
			throw new IOException( "Message sending was configured to fail." );

		this.messagesForAgentsCount.incrementAndGet();
	}

	@Override
	public void listenToExportsFromOtherAgents( ListenerCommand command, Instance instance )
	throws IOException {
		// nothing
	}

	@Override
	public void sendMessageToTheDm( Message message ) throws IOException {

		if( this.failMessageSending.get())
			throw new IOException( "Message sending was configured to fail." );

		this.messagesForTheDm.add( message );
	}

	@Override
	public void listenToTheDm( ListenerCommand command ) throws IOException {
		// nothing
	}

	@Override
	public String getMessagingType() {
		return MessagingConstants.TEST_FACTORY_TYPE;
	}

	@Override
	public Map<String, String> getConfiguration() {
		return Collections.singletonMap(MessagingConstants.MESSAGING_TYPE_PROPERTY, MessagingConstants.TEST_FACTORY_TYPE);
	}

	@Override
	public void setMessageQueue( LinkedBlockingQueue<Message> messageQueue ) {
		// nothing
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
	}

	/**
	 * @return the scopedInstancePath
	 */
	public String getScopedInstancePath() {
		return this.scopedInstancePath;
	}
}
