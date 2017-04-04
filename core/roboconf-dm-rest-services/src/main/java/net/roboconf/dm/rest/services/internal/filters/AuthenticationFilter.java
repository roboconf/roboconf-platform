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

package net.roboconf.dm.rest.services.internal.filters;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.services.internal.resources.IAuthenticationResource;

/**
 * A filter to determine and request (if necessary) authentication.
 * <p>
 * This filter is registered as an OSGi service. PAX's web extender automatically
 * binds it to the web server (Karaf's Jetty). This filter is only applied to the
 * resources in this bundle, which means the REST API and the web socket. Other web
 * applications are not impacted. As an example, Karaf and Roboconf web administrations
 * are served by other bundles, this filter cannot be applied to them.
 * </p>
 * @author Vincent Zurczak - Linagora
 */
public class AuthenticationFilter implements Filter {

	public static final String SESSION_ID = "sid";
	private final Logger logger = Logger.getLogger( getClass().getName());

	private AuthenticationManager authenticationMngr;
	private boolean authenticationEnabled;
	private long sessionPeriod;


	@Override
	public void doFilter( ServletRequest req, ServletResponse resp, FilterChain chain )
	throws IOException, ServletException {

		if( ! this.authenticationEnabled ) {
			chain.doFilter( req, resp );

		} else {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpServletResponse response = (HttpServletResponse) resp;
			String requestedPath = request.getRequestURI();
			this.logger.info( "Path for auth: " + requestedPath );

			// Find the session ID in the cookies
			String sessionId = null;
			Cookie[] cookies = request.getCookies();
			if( cookies != null ) {
				for( Cookie cookie : cookies ) {
					if( SESSION_ID.equals( cookie.getName())) {
						sessionId = cookie.getValue();
						break;
					}
				}
			}

			// Is there a valid session?
			boolean loggedIn = false;
			if( ! Utils.isEmptyOrWhitespaces( sessionId )) {
				loggedIn = this.authenticationMngr.isSessionValid( sessionId, this.sessionPeriod );
				this.logger.finest( "Session " + sessionId + (loggedIn ? " was successfully " : " failed to be ") + "validated." );
			} else {
				this.logger.finest( "No session ID was found in the cookie. Authentication cannot be performed." );
			}

			// Valid session, go on. Send an error otherwise.
			// No redirection, we mainly deal with our web socket and REST API.
			boolean loginRequest = IAuthenticationResource.LOGIN_PATH.equals( requestedPath );
			if( loggedIn || loginRequest ) {
				chain.doFilter( request, response );
			} else {
				response.sendError( 403, "Authentication is required." );
			}
		}
	}


	@Override
	public void destroy() {
		// nothing
	}


	@Override
	public void init( FilterConfig filterConfig ) throws ServletException {
		// nothing
	}


	/**
	 * @param authenticationEnabled the authenticationEnabled to set
	 */
	public void setAuthenticationEnabled( boolean authenticationEnabled ) {
		this.authenticationEnabled = authenticationEnabled;
	}


	/**
	 * @param authenticationMngr the authenticationMngr to set
	 */
	public void setAuthenticationMngr( AuthenticationManager authenticationMngr ) {
		this.authenticationMngr = authenticationMngr;
	}


	/**
	 * @param sessionPeriod the sessionPeriod to set
	 */
	public void setSessionPeriod( long sessionPeriod ) {
		this.sessionPeriod = sessionPeriod;
	}
}
