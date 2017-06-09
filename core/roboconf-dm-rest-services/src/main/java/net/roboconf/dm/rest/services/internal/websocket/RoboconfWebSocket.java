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

import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.services.internal.ServletRegistrationComponent;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfWebSocket implements WebSocketListener {

	private final Logger logger = Logger.getLogger( getClass().getName());
	Session session;


	@Override
	public void onWebSocketClose( int statusCode, String reason ) {
		this.logger.info( "A web socket connection is about to be closed. Session origin: " + this.session.getRemoteAddress());
		WebSocketHandler.removeSession( this.session );
	}

	@Override
	public void onWebSocketConnect( Session session ) {
		this.session = session;
		session.setIdleTimeout( -1 );
		this.logger.info( "A web socket connection was established. Session origin: " + session.getRemoteAddress());
		WebSocketHandler.addSession( session );
	}

	@Override
	public void onWebSocketError( Throwable cause ) {
		this.logger.severe( "An error related to web sockets occurred. Session origin: " + this.session.getRemoteAddress());
		Utils.logException( this.logger, new Exception( cause ));
		ServletRegistrationComponent.WS_CONNECTION_ERRORS_COUNT.incrementAndGet();
	}

	@Override
	public void onWebSocketText( String message ) {
		this.logger.finest( "An unexpected text message was received on the web socket. Session origin: " + this.session.getRemoteAddress());
		// nothing else, we do not expect requests on the web socket
	}

	@Override
	public void onWebSocketBinary( byte[] payload, int offset, int len ) {
		this.logger.finest( "An unexpected binary message was received on the web socket. Session origin: " + this.session.getRemoteAddress());
		// nothing else, we do not expect requests on the web socket
	}
}
