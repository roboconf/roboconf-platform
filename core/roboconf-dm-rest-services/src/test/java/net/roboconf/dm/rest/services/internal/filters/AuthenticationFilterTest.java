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

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.services.internal.resources.IAuthenticationResource;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AuthenticationFilterTest {

	@Test
	public void forCodeCoverage() throws Exception {

		AuthenticationFilter filter = new AuthenticationFilter();
		filter.init( null );
		filter.destroy();
	}


	@Test
	public void testDoFiler_noAuthentication() throws Exception {

		AuthenticationFilter filter = new AuthenticationFilter();
		filter.setAuthenticationEnabled( false );

		ServletRequest req = Mockito.mock( ServletRequest.class );
		ServletResponse resp = Mockito.mock( ServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		filter.doFilter( req, resp, chain );
		Mockito.verifyZeroInteractions( req );
		Mockito.verifyZeroInteractions( resp );
		Mockito.verify( chain, Mockito.only()).doFilter( req, resp );
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie() throws Exception {

		AuthenticationFilter filter = new AuthenticationFilter();
		filter.setAuthenticationEnabled( true );

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req ).getRequestURI();
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( resp, Mockito.only()).sendError( 403, "Authentication is required." );
		Mockito.verifyZeroInteractions( chain );
	}


	@Test
	public void testDoFiler_withAuthentication_withCookie_loggedIn() throws Exception {

		final String sessionId = "a1a2a3a4";
		final long sessionPeriod = -1;

		AuthenticationFilter filter = new AuthenticationFilter();
		filter.setAuthenticationEnabled( true );
		filter.setSessionPeriod( sessionPeriod );

		AuthenticationManager authMngr = Mockito.mock( AuthenticationManager.class );
		Mockito.when( authMngr.isSessionValid( sessionId, sessionPeriod )).thenReturn( true );
		filter.setAuthenticationMngr( authMngr );

		FilterChain chain = Mockito.mock( FilterChain.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[] {
			new Cookie( "as", "as" ),
			new Cookie( AuthenticationFilter.SESSION_ID, sessionId )
		});

		filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req ).getRequestURI();
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( authMngr ).isSessionValid( sessionId, sessionPeriod );
		Mockito.verify( chain, Mockito.only()).doFilter( req, resp );
		Mockito.verifyZeroInteractions( resp );
	}


	@Test
	public void testDoFiler_withAuthentication_withCookie_notLoggedIn() throws Exception {

		final String sessionId = "a1a2a3a4";
		final long sessionPeriod = -1;

		AuthenticationFilter filter = new AuthenticationFilter();
		filter.setAuthenticationEnabled( true );
		filter.setSessionPeriod( sessionPeriod );

		AuthenticationManager authMngr = Mockito.mock( AuthenticationManager.class );
		Mockito.when( authMngr.isSessionValid( sessionId, sessionPeriod )).thenReturn( false );
		filter.setAuthenticationMngr( authMngr );

		FilterChain chain = Mockito.mock( FilterChain.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[] {
			new Cookie( "as", "as" ),
			new Cookie( AuthenticationFilter.SESSION_ID, sessionId )
		});

		filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req ).getRequestURI();
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( authMngr ).isSessionValid( sessionId, sessionPeriod );
		Mockito.verifyZeroInteractions( chain );
		Mockito.verify( resp, Mockito.only()).sendError( 403, "Authentication is required." );
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie_butLoginPageRequested() throws Exception {

		AuthenticationFilter filter = new AuthenticationFilter();
		filter.setAuthenticationEnabled( true );

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( IAuthenticationResource.LOGIN_PATH );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[ 0 ]);

		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req ).getRequestURI();
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( chain, Mockito.only()).doFilter( req, resp );
		Mockito.verifyZeroInteractions( resp );
	}
}
