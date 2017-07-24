/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.net.URI;

import javax.security.auth.login.LoginException;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.DebugWsException;
import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.commons.security.AuthenticationManager.IAuthService;
import net.roboconf.dm.rest.services.internal.RestApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AuthenticationWsDelegateTest {

	private static final String REST_URI = "http://localhost:8090";

	private WsClient client;
	private HttpServer httpServer;
	private IAuthService authService;


	@After
	public void after() {

		if( this.httpServer != null )
			this.httpServer.stop();

		if( this.client != null )
			this.client.destroy();
	}


	@Before
	public void before() throws Exception {

		// Prevent NPE during tests
		Manager manager = Mockito.mock( Manager.class );
		IPreferencesMngr preferencesMngr = Mockito.mock( IPreferencesMngr.class );
		Mockito.when( manager.preferencesMngr()).thenReturn( preferencesMngr );

		// Create the application
		URI uri = UriBuilder.fromUri( REST_URI ).build();
		RestApplication restApp = new RestApplication( manager );

		// Configure the authentication part
		AuthenticationManager authenticationMngr = new AuthenticationManager( "whatever" );
		this.authService = Mockito.mock( IAuthService.class );
		authenticationMngr.setAuthService( this.authService );
		restApp.setAuthenticationManager( authenticationMngr );

		// Launch the application in a real web server
		this.httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );
		this.client = new WsClient( REST_URI );
	}


	@Test( expected = DebugWsException.class )
	public void testLogin_failure() throws Exception {

		Mockito.doThrow( new LoginException( "for test" )).when( this.authService ).authenticate( "u", "p" );
		this.client.getAuthenticationWsDelegate().login( "u", "p" );
	}


	@Test
	public void testLogin_success() throws Exception {

		String sessionId = this.client.getAuthenticationWsDelegate().login( "u", "p" );
		Assert.assertNotNull( sessionId );
		this.client.getAuthenticationWsDelegate().logout( sessionId );

		// Log out twice does not result in any error
		this.client.getAuthenticationWsDelegate().logout( sessionId );
	}
}
