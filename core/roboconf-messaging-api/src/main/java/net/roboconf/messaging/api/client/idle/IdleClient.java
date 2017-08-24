/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.client.idle;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import net.roboconf.core.model.beans.Application;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.extensions.IMessagingClient;
import net.roboconf.messaging.api.extensions.MessagingContext;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;
import net.roboconf.messaging.api.jmx.RoboconfMessageQueue;
import net.roboconf.messaging.api.messages.Message;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IdleClient implements IMessagingClient {

	private final AtomicBoolean connected = new AtomicBoolean( false );


	@Override
	public void setMessageQueue( RoboconfMessageQueue messageQueue ) {
		// nothing
	}

	@Override
	public boolean isConnected() {
		return this.connected.get();
	}

	@Override
	public void setOwnerProperties( RecipientKind ownerKind, String domain, String applicationName, String scopedInstancePath ) {
		// nothing
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
	public String getMessagingType() {
		return "sauron";
	}

	@Override
	public Map<String,String> getConfiguration() {
		Map<String,String> configuration = new HashMap<>( 1 );
		configuration.put( MessagingConstants.MESSAGING_TYPE_PROPERTY, MessagingConstants.FACTORY_IDLE );
		return Collections.unmodifiableMap( configuration );
	}

	@Override
	public void subscribe( MessagingContext ctx ) throws IOException {
		// nothing
	}

	@Override
	public void unsubscribe( MessagingContext ctx ) throws IOException {
		// nothing
	}

	@Override
	public void publish( MessagingContext ctx, Message msg ) throws IOException {
		// nothing
	}

	@Override
	public void deleteMessagingServerArtifacts( Application application ) throws IOException {
		// nothing
	}
}
