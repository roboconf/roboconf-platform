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
import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.services.internal.ServletRegistrationComponent;
import net.roboconf.dm.rest.services.internal.annotations.RestIndexer;
import net.roboconf.dm.rest.services.internal.annotations.RestIndexer.RestOperationBean;
import net.roboconf.dm.rest.services.internal.audit.AuditLogRecord;
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

	private final Logger logger = Logger.getLogger( getClass().getName());

	private final RestIndexer restIndexer;
	private AuthenticationManager authenticationMngr;
	private boolean authenticationEnabled;
	private long sessionPeriod;


	/**
	 * Constructor.
	 */
	public AuthenticationFilter() {
		this.restIndexer = new RestIndexer();
	}


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
					if( UrlConstants.SESSION_ID.equals( cookie.getName())) {
						sessionId = cookie.getValue();
						break;
					}
				}
			}

			// Audit
			audit( request, sessionId );

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
			boolean loginRequest = requestedPath.endsWith( IAuthenticationResource.PATH + IAuthenticationResource.LOGIN_PATH );
			if( loggedIn || loginRequest ) {
				chain.doFilter( request, response );
			} else {
				response.sendError( 403, "Authentication is required." );
			}
		}
	}


	/**
	 * @param request
	 * @param sessionId
	 */
	private void audit( HttpServletRequest request, String sessionId ) {

		// Find the right method
		RestOperationBean rightBean = null;
		String restVerb = request.getMethod();
		String uri = request.getRequestURI();
		String path = cleanPath( uri );
		for( RestOperationBean rmb : this.restIndexer.restMethods ) {
			if( path != null
					&& path.matches( rmb.getUrlPattern())
					&& rmb.getRestVerb().equalsIgnoreCase( restVerb )) {

				rightBean = rmb;
				break;
			}
		}

		// TODO; check the permissions

		// TODO: documentation (+ authentication)
		// TODO: web admin

		// Audit
		String ipAddress = request.getRemoteAddr();
		String user = this.authenticationMngr.findUsername( sessionId );
		boolean authorized = user != null;
		if( rightBean != null )
			this.logger.log( new AuditLogRecord( user, rightBean.getJerseyPath(), uri, rightBean.getRestVerb(), ipAddress, authorized ));
		else
			this.logger.log( new AuditLogRecord( user, null, uri, restVerb, ipAddress, authorized ));
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
	public void setAuthenticationManager( AuthenticationManager authenticationMngr ) {
		this.authenticationMngr = authenticationMngr;
	}


	/**
	 * @param sessionPeriod the sessionPeriod to set
	 */
	public void setSessionPeriod( long sessionPeriod ) {
		this.sessionPeriod = sessionPeriod;
	}


	/**
	 * Cleans the path by removing the servlet paths and URL parameters.
	 * @param path a non-null path
	 * @return a non-null path
	 */
	static String cleanPath( String path ) {

		return path
				.replaceFirst( "^" + ServletRegistrationComponent.REST_CONTEXT + "/", "/" )
				.replaceFirst( "^" + ServletRegistrationComponent.WEBSOCKET_CONTEXT + "/", "/" )
				.replaceFirst( "\\?.*", "" );
	}
}
