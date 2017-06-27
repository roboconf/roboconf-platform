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

package net.roboconf.dm.rest.services.internal.resources.impl;

import javax.security.auth.login.LoginException;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.commons.security.AuthenticationManager.IAuthService;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AuthenticationResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testLoginAndLogout() throws Exception {

		// Simple manager
		Manager manager = new Manager();
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		// No authentication manager
		AuthenticationResource res = new AuthenticationResource( manager );
		Response resp = res.login( "kikou", "pwd" );
		Assert.assertEquals( Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());

		resp = res.logout( null );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		// Set one
		AuthenticationManager authMngr = new AuthenticationManager( "my realm" );
		IAuthService authService = Mockito.mock( IAuthService.class );
		authMngr.setAuthService( authService );
		res.setAuthenticationManager( authMngr );

		// Authentication will work for ANY user, except for "u1"
		Mockito.doThrow( new LoginException( "for test" )).when( authService ).authenticate( "u1", "p1" );

		resp = res.login( "u2", "p2" );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		NewCookie cookie = (NewCookie) resp.getMetadata().getFirst( "Set-Cookie" );
		Assert.assertNotNull( cookie );
		Assert.assertEquals( UrlConstants.SESSION_ID, cookie.getName());
		Assert.assertNotNull( cookie.getValue());
		Assert.assertTrue( authMngr.isSessionValid( cookie.getValue(), -1 ));

		// Log out
		res.logout( cookie.getValue());
		Assert.assertFalse( authMngr.isSessionValid( cookie.getValue(), -1 ));

		// Verify "u1" cannot login
		resp = res.login( "u1", "p1" );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
		Assert.assertEquals( 0, resp.getMetadata().size());
	}
}
