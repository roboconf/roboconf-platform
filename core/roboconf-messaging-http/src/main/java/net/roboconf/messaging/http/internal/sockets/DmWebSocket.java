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

package net.roboconf.messaging.http.internal.sockets;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.http.internal.HttpClientFactory;
import net.roboconf.messaging.http.internal.messages.HttpSerializationUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmWebSocket implements WebSocketListener {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private Session session;
	private final HttpClientFactory httpClientFactory;


	/**
	 * Constructor.
	 * @param httpClientFactory
	 */
	public DmWebSocket( HttpClientFactory httpClientFactory ) {
		this.httpClientFactory = httpClientFactory;
	}


	@Override
	public void onWebSocketBinary( byte[] payload, int offset, int len ) {

		this.logger.finest( "A binary message was received." );
		try {
			Message msg = HttpSerializationUtils.deserializeObject( payload );
			this.logger.finest( "The received message was deserialized as an instance of " + msg.getClass().getSimpleName());

			this.httpClientFactory.getDmClient().processReceivedMessage( msg, this.session );

		} catch( ClassNotFoundException | IOException e ) {
			this.logger.severe( "A message could not be deserialized. => " + e.getClass().getSimpleName());
			Utils.logException( this.logger, e );
			this.httpClientFactory.getDmClient().errorWhileReceivingMessage();
		}
	}


	@Override
	public void onWebSocketClose( int statusCode, String reason ) {
		this.logger.finest( "Websocket closed: " + reason );
		this.session = null;
	}


	@Override
	public void onWebSocketConnect( Session session ) {
		this.logger.finest( "Socket Connected: " + session );
		this.session = session;
	}


	@Override
	public void onWebSocketError( Throwable cause ) {
		this.logger.finest( "Websocket error: " + cause );
		this.session = null;
	}


	@Override
	public void onWebSocketText( String message ) {
		this.logger.finest( "A text message was received but will be ignored: " + message );
	}
}
