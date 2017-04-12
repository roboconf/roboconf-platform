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

package net.roboconf.dm.rest.commons.security;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.roboconf.dm.rest.commons.security.AuthenticationManager.IAuthService;
import net.roboconf.dm.rest.commons.security.AuthenticationManager.RoboconfCallbackHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AuthenticationManagerTest {

	@Test
	public void testAuthenticationChain_success() {

		AuthenticationManager mngr = new AuthenticationManager( "realm" );
		IAuthService authService = Mockito.mock( IAuthService.class );
		mngr.setAuthService( authService );

		Assert.assertNull( mngr.findUsername( "whatever" ));
		Assert.assertNull( mngr.findUsername( null ));

		String token = mngr.login( "me", "my password" );
		Assert.assertNotNull( token );
		Assert.assertTrue( mngr.isSessionValid( token, 1 ));
		Assert.assertTrue( mngr.isSessionValid( token, -1 ));
		Assert.assertEquals( "me", mngr.findUsername( token ));

		mngr.logout( token );
		Assert.assertFalse( mngr.isSessionValid( token, 1 ));
		Assert.assertFalse( mngr.isSessionValid( token, -1 ));
		Assert.assertNull( mngr.findUsername( token ));
	}


	@Test
	public void testAuthenticationChain_failure() throws Exception {

		AuthenticationManager mngr = new AuthenticationManager( "realm" );
		IAuthService authService = Mockito.mock( IAuthService.class );
		Mockito.doThrow( new LoginException( "for test" )).when( authService ).authenticate( Mockito.anyString(), Mockito.anyString());
		mngr.setAuthService( authService );

		String token = mngr.login( "me", "my password" );
		Assert.assertNull( token );
		Assert.assertFalse( mngr.isSessionValid( token, 1 ));
		Assert.assertFalse( mngr.isSessionValid( token, -1 ));

		mngr.logout( token );
		Assert.assertFalse( mngr.isSessionValid( token, 1 ));
		Assert.assertFalse( mngr.isSessionValid( token, -1 ));
	}


	@Test
	public void testAuthenticationChain_validityPeriodExpired() throws Exception {

		AuthenticationManager mngr = new AuthenticationManager( "realm" );
		IAuthService authService = Mockito.mock( IAuthService.class );
		mngr.setAuthService( authService );

		String token = mngr.login( "me", "my password" );
		Assert.assertNotNull( token );
		Assert.assertTrue( mngr.isSessionValid( token, 1 ));
		Thread.sleep( 1020 );
		Assert.assertFalse( mngr.isSessionValid( token, 1 ));

		// The session was removed, it should not be marked as valid anymore
		Assert.assertFalse( mngr.isSessionValid( token, 10 ));
	}


	@Test
	public void testAuthenticationChain_withKaraf_butOutsideKaraf() throws Exception {

		AuthenticationManager mngr = new AuthenticationManager( "realm" );

		String token = mngr.login( "me", "my password" );
		Assert.assertNull( token );
		Assert.assertFalse( mngr.isSessionValid( token, -1 ));
	}


	@Test
	public void testRoboconfCallbackHandler_success() throws Exception {

		RoboconfCallbackHandler handler = new RoboconfCallbackHandler( "user", "password" );
		handler.handle( new Callback[] {
				new NameCallback( "Username: " ),
				new PasswordCallback( "Password: ", false )
		});
	}


	@Test( expected = UnsupportedCallbackException.class )
	public void testRoboconfCallbackHandler_failure() throws Exception {

		RoboconfCallbackHandler handler = new RoboconfCallbackHandler( "user", "password" );
		handler.handle( new Callback[] {
				new NameCallback( "Username: " ),
				new PasswordCallback( "Password: ", false ),
				new LanguageCallback()
		});
	}
}
