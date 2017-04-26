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

package net.roboconf.dm.rest.services.internal.resources;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.services.internal.filters.AuthenticationFilter;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IAuthenticationResource {

	String PATH = "/" + UrlConstants.AUTHENTICATION;
	String LOGIN_PATH = "/e";


	/**
	 * Authenticates a user.
	 * @param username a user name
	 * @param password a password
	 * @return a response (the session ID is returned as a cookie)
	 * @see AuthenticationFilter#SESSION_ID for the cookie's name
	 *
	 * @HTTP 200 Login succeeded.
	 * @HTTP 403 Login failed.
	 * @HTTP 500 Invalid server configuration.
	 */
	@POST
	@Path( LOGIN_PATH )
	@Produces( MediaType.TEXT_PLAIN )
	Response login( @HeaderParam("u") String username, @HeaderParam("p") String password );


	/**
	 * Terminates a user session.
	 * @param sessionId a session ID
	 * @return a response
	 * @HTTP 200 Always successful.
	 */
	@POST
	@Path("/s")
	Response logout( @CookieParam( UrlConstants.SESSION_ID ) String sessionId );
}
