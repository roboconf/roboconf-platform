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

import static net.roboconf.dm.rest.services.internal.ServletRegistrationComponent.REST_CONTEXT;
import static net.roboconf.dm.rest.services.internal.ServletRegistrationComponent.WEBSOCKET_CONTEXT;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.services.cors.ResponseCorsFilter;
import net.roboconf.dm.rest.services.internal.ServletRegistrationComponent;
import net.roboconf.dm.rest.services.internal.resources.IAuthenticationResource;
import net.roboconf.dm.rest.services.internal.resources.IPreferencesResource;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AuthenticationFilterTest {

	private AuthenticationFilter filter;
	private ServletRegistrationComponent servletRegistrationComponent;


	@Before
	public void setup() {

		this.servletRegistrationComponent = new ServletRegistrationComponent( null );
		this.filter = new AuthenticationFilter( this.servletRegistrationComponent );
	}


	@Test
	public void forCodeCoverage() throws Exception {

		this.filter.init( null );
		this.filter.destroy();
	}


	@Test
	public void testDoFiler_noAuthentication() throws Exception {

		this.filter.setAuthenticationEnabled( false );

		ServletRequest req = Mockito.mock( ServletRequest.class );
		ServletResponse resp = Mockito.mock( ServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		this.filter.doFilter( req, resp, chain );
		Mockito.verifyZeroInteractions( req );
		Mockito.verifyZeroInteractions( resp );
		Mockito.verify( chain, Mockito.only()).doFilter( req, resp );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 0, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie() throws Exception {

		this.filter.setAuthenticationEnabled( true );
		this.filter.setAuthenticationManager( Mockito.mock( AuthenticationManager.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( "/whatever" );

		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req ).getQueryString();
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( resp, Mockito.only()).sendError( 403, "Authentication is required." );
		Mockito.verifyNoMoreInteractions( resp );
		Mockito.verifyZeroInteractions( chain );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie_corsEnabled() throws Exception {

		this.filter.setAuthenticationEnabled( true );
		this.filter.setAuthenticationManager( Mockito.mock( AuthenticationManager.class ));
		this.filter.setEnableCors( true );

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( "/whatever" );

		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req ).getQueryString();
		Mockito.verify( req ).getHeader( ResponseCorsFilter.CORS_REQ_HEADERS );
		Mockito.verify( req ).getHeader( ResponseCorsFilter.ORIGIN );
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( resp, Mockito.times( 3 )).setHeader( Mockito.anyString(), Mockito.anyString());
		Mockito.verify( resp ).sendError( 403, "Authentication is required." );
		Mockito.verifyNoMoreInteractions( resp );
		Mockito.verifyZeroInteractions( chain );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_withCookie_loggedIn() throws Exception {

		final String sessionId = "a1a2a3a4";
		final long sessionPeriod = -1;

		this.filter.setAuthenticationEnabled( true );
		this.filter.setSessionPeriod( sessionPeriod );

		AuthenticationManager authMngr = Mockito.mock( AuthenticationManager.class );
		Mockito.when( authMngr.isSessionValid( sessionId, sessionPeriod )).thenReturn( true );
		this.filter.setAuthenticationManager( authMngr );

		FilterChain chain = Mockito.mock( FilterChain.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( "/whatever" );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[] {
			new Cookie( "as", "as" ),
			new Cookie( UrlConstants.SESSION_ID, sessionId )
		});

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req ).getQueryString();
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( authMngr ).isSessionValid( sessionId, sessionPeriod );
		Mockito.verify( chain, Mockito.only()).doFilter( req, resp );
		Mockito.verifyZeroInteractions( resp );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 0, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_withCookie_notLoggedIn() throws Exception {

		final String sessionId = "a1a2a3a4";
		final long sessionPeriod = -1;

		this.filter.setAuthenticationEnabled( true );
		this.filter.setSessionPeriod( sessionPeriod );

		AuthenticationManager authMngr = Mockito.mock( AuthenticationManager.class );
		Mockito.when( authMngr.isSessionValid( sessionId, sessionPeriod )).thenReturn( false );
		this.filter.setAuthenticationManager( authMngr );

		FilterChain chain = Mockito.mock( FilterChain.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( "/whatever" );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[] {
			new Cookie( "as", "as" ),
			new Cookie( UrlConstants.SESSION_ID, sessionId )
		});

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req ).getQueryString();
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( authMngr ).isSessionValid( sessionId, sessionPeriod );
		Mockito.verifyZeroInteractions( chain );
		Mockito.verify( resp, Mockito.only()).sendError( 403, "Authentication is required." );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie_butLoginPageRequested() throws Exception {

		this.filter.setAuthenticationEnabled( true );
		this.filter.setAuthenticationManager( Mockito.mock( AuthenticationManager.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( IAuthenticationResource.PATH + IAuthenticationResource.LOGIN_PATH );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[ 0 ]);
		Mockito.when( req.getMethod()).thenReturn( "poSt" );

		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req ).getQueryString();
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( chain, Mockito.only()).doFilter( req, resp );
		Mockito.verifyZeroInteractions( resp );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 0, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie_butUserPrefRequested() throws Exception {

		this.filter.setAuthenticationEnabled( true );
		this.filter.setAuthenticationManager( Mockito.mock( AuthenticationManager.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( IPreferencesResource.PATH );
		Mockito.when( req.getQueryString()).thenReturn( "key=" + IPreferencesMngr.USER_LANGUAGE );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[ 0 ]);
		Mockito.when( req.getMethod()).thenReturn( "Get" );

		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req, Mockito.times( 2 )).getQueryString();
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( chain, Mockito.only()).doFilter( req, resp );
		Mockito.verifyZeroInteractions( resp );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 0, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie_butOptionsRequest() throws Exception {

		this.filter.setAuthenticationEnabled( true );
		this.filter.setAuthenticationManager( Mockito.mock( AuthenticationManager.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( IPreferencesResource.PATH );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[ 0 ]);
		Mockito.when( req.getMethod()).thenReturn( "opTions" );

		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req ).getQueryString();
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verify( chain, Mockito.only()).doFilter( req, resp );
		Mockito.verifyZeroInteractions( resp );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 0, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie_butUserPrefRequested_post() throws Exception {

		this.filter.setAuthenticationEnabled( true );
		this.filter.setAuthenticationManager( Mockito.mock( AuthenticationManager.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( IPreferencesResource.PATH );
		Mockito.when( req.getQueryString()).thenReturn( "key=" + IPreferencesMngr.USER_LANGUAGE );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[ 0 ]);
		Mockito.when( req.getMethod()).thenReturn( "post" );

		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req ).getQueryString();
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verifyZeroInteractions( chain );
		Mockito.verify( resp, Mockito.only()).sendError( 403, "Authentication is required." );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testDoFiler_withAuthentication_noCookie_butOtherPrefRequested() throws Exception {

		this.filter.setAuthenticationEnabled( true );
		this.filter.setAuthenticationManager( Mockito.mock( AuthenticationManager.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getRequestURI()).thenReturn( IPreferencesResource.PATH );
		Mockito.when( req.getQueryString()).thenReturn( "key=whatever" );
		Mockito.when( req.getCookies()).thenReturn( new Cookie[ 0 ]);
		Mockito.when( req.getMethod()).thenReturn( "Get" );

		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		this.filter.doFilter( req, resp, chain );
		Mockito.verify( req ).getCookies();
		Mockito.verify( req, Mockito.times( 2 )).getRequestURI();
		Mockito.verify( req, Mockito.times( 2 )).getMethod();
		Mockito.verify( req ).getRemoteAddr();
		Mockito.verify( req, Mockito.times( 2 )).getQueryString();
		Mockito.verify( req ).getHeader( AuthenticationFilter.USER_AGENT );
		Mockito.verifyNoMoreInteractions( req );

		Mockito.verifyZeroInteractions( chain );
		Mockito.verify( resp, Mockito.only()).sendError( 403, "Authentication is required." );

		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsCount());
		Assert.assertEquals( 1, this.servletRegistrationComponent.getRestRequestsWithAuthFailureCount());
	}


	@Test
	public void testCleanPath() {

		Assert.assertEquals( "", AuthenticationFilter.cleanPath( "" ));
		Assert.assertEquals( REST_CONTEXT, AuthenticationFilter.cleanPath( REST_CONTEXT ));
		Assert.assertEquals( "/", AuthenticationFilter.cleanPath( REST_CONTEXT + "/" ));
		Assert.assertEquals( REST_CONTEXT, AuthenticationFilter.cleanPath( REST_CONTEXT + REST_CONTEXT ));
		Assert.assertEquals( "/test", AuthenticationFilter.cleanPath( REST_CONTEXT + "/test" ));

		Assert.assertEquals( WEBSOCKET_CONTEXT, AuthenticationFilter.cleanPath( WEBSOCKET_CONTEXT ));
		Assert.assertEquals( "/", AuthenticationFilter.cleanPath( WEBSOCKET_CONTEXT + "/" ));
		Assert.assertEquals( "/ty", AuthenticationFilter.cleanPath( WEBSOCKET_CONTEXT + "/ty" ));
		Assert.assertEquals( WEBSOCKET_CONTEXT + "ty", AuthenticationFilter.cleanPath( WEBSOCKET_CONTEXT + "ty" ));
		Assert.assertEquals( "/test", AuthenticationFilter.cleanPath( REST_CONTEXT + "/test?p=toto/22" ));
	}
}
