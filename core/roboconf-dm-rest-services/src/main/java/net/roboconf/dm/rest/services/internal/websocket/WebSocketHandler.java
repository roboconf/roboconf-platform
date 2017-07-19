/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.websocket;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.dm.rest.commons.beans.WebSocketMessage;
import net.roboconf.dm.rest.commons.json.JSonBindingUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WebSocketHandler implements IDmListener {

	private final static Set<Session> SESSIONS = new HashSet<> ();

	private final AtomicBoolean enabled = new AtomicBoolean( false );
	private final Logger logger = Logger.getLogger( getClass().getName());


	// Sessions management

	public static void addSession( Session session ) {
		synchronized( SESSIONS ) {
			SESSIONS.add( session );
		}
	}


	public static void removeSession( Session session ) {
		synchronized( SESSIONS ) {
			SESSIONS.remove( session );
		}
	}

	static Set<Session> getSessions() {

		Set<Session> result;
		synchronized( SESSIONS ) {
			result = Collections.unmodifiableSet( SESSIONS );
		}

		return result;
	}

	public static int getSessionsCount() {
		synchronized( SESSIONS ) {
			return SESSIONS.size();
		}
	}


	// IDmListener

	@Override
	public String getId() {
		return "DM's Websocket";
	}


	@Override
	public void enableNotifications() {
		this.enabled.set( true );
	}


	@Override
	public void disableNotifications() {
		this.enabled.set( false );
	}


	@Override
	public void application( Application application, EventType eventType ) {
		WebSocketMessage message = new WebSocketMessage( application, eventType );
		String msg = asJson( message );
		send( msg );
	}


	@Override
	public void applicationTemplate( ApplicationTemplate tpl, EventType eventType ) {
		WebSocketMessage message = new WebSocketMessage( tpl, eventType );
		String msg = asJson( message );
		send( msg );
	}


	@Override
	public void instance( Instance instance, Application application, EventType eventType ) {
		WebSocketMessage message = new WebSocketMessage( instance, application, eventType );
		String msg = asJson( message );
		send( msg );
	}


	@Override
	public void raw( String message, Object... data ) {

		// We do not use "data"
		if( message != null ) {
			WebSocketMessage wrappedMessage = new WebSocketMessage( message );
			String msg = asJson( wrappedMessage );
			send( msg );
		}
	}


	/**
	 * Creates a JSon/string representation from an object.
	 * @param message a web socket message
	 * @return a non-null string
	 */
	String asJson( WebSocketMessage message ) {

		String result = null;
		try {
			ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
			StringWriter writer = new StringWriter();
			mapper.writeValue( writer, message );
			result = writer.toString();

		} catch( IOException e ) {
			this.logger.severe( "A notification could not be prepared. It will not be sent. " + e.getMessage());
			Utils.logException( this.logger, e );
		}

		return result;
	}


	/**
	 * Sends a message to all the connected sessions.
	 * @param message the message to send
	 */
	private void send( String message ) {

		if( ! this.enabled.get()) {
			this.logger.finest( "Notifications were disabled by the DM." );

		} else if( message == null ) {
			this.logger.finest( "No message to send to web socket clients." );

		} else synchronized( SESSIONS ) {
			for( Session session : SESSIONS ) {
				try {
					this.logger.finest( "Sending a message to a web socket client..." );
					session.getRemote().sendString( message );

				} catch( IOException e ) {
					StringBuilder sb = new StringBuilder( "A notification could not be propagated for session " );
					sb.append( session.getRemoteAddress());
					sb.append( "." );
					if( ! Utils.isEmptyOrWhitespaces( e.getMessage()))
						sb.append( " " + e.getMessage());

					this.logger.severe( sb.toString());
					Utils.logException( this.logger, e );
				}
			}
		}
	}
}
