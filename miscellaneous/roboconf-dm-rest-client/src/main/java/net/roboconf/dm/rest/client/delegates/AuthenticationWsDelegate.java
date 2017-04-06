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

package net.roboconf.dm.rest.client.delegates;

import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.Status.Family;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.DebugWsException;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AuthenticationWsDelegate {

	private final WebResource resource;
	private final WsClient wsClient;
	private final Logger logger;


	/**
	 * Constructor.
	 * @param resource a web resource
	 * @param the WS client
	 */
	public AuthenticationWsDelegate( WebResource resource, WsClient wsClient ) {
		this.resource = resource;
		this.wsClient = wsClient;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Logs in with a user name and a password.
	 * @param username a user name
	 * @param password a password
	 * @return a session ID, or null if login failed
	 * @throws DebugWsException
	 */
	public String login( String username, String password ) throws DebugWsException {

		this.logger.finer( "Logging in as " + username );
		WebResource path = this.resource.path( UrlConstants.AUTHENTICATION ).path( "e" );
		ClientResponse response = path
						.header( "u", username )
						.header( "p", password )
						.post( ClientResponse.class );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new DebugWsException( response.getStatusInfo().getStatusCode(), value );
		}

		// Get the session ID from the cookie that should have been returned
		String sessionId = null;
		List<NewCookie> cookies = response.getCookies();
		if( cookies != null ) {
			for( NewCookie cookie : cookies ) {
				if( UrlConstants.SESSION_ID.equals( cookie.getName())) {
					sessionId = cookie.getValue();
					break;
				}
			}
		}

		// Set the session ID
		this.wsClient.setSessionId( sessionId );
		this.logger.finer( "Session ID: " + sessionId );

		return sessionId;
	}


	/**
	 * Terminates a session.
	 * @param sessionId a session ID
	 * @throws DebugWsException
	 */
	public void logout( String sessionId ) throws DebugWsException {

		this.logger.finer( "Logging out... Session ID = " + sessionId );
		WebResource path = this.resource.path( UrlConstants.AUTHENTICATION ).path( "s" );
		ClientResponse response = this.wsClient.createBuilder( path ).get( ClientResponse.class );
		this.logger.finer( String.valueOf( response.getStatusInfo()));
		this.wsClient.setSessionId( null );
	}
}
