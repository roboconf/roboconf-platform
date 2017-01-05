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

package net.roboconf.webextension.kibana.internal;

import static net.roboconf.webextension.kibana.KibanaExtensionConstants.CONTEXT;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.webextension.kibana.KibanaExtensionServlet;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ServletRegistrationComponentTest {

	@Test
	public void testStop_httpServiceIsNull() throws Exception {

		ServletRegistrationComponent register = new ServletRegistrationComponent();
		register.stopping();
	}


	@Test
	public void testStartAndStop() throws Exception {

		Manager manager = Mockito.mock( Manager.class );
		IPreferencesMngr preferencesMngr = Mockito.mock( IPreferencesMngr.class );
		Mockito.when( manager.preferencesMngr()).thenReturn( preferencesMngr );

		ServletRegistrationComponent register = new ServletRegistrationComponent();
		register.setManager( manager );

		// Deal with the HTTP service.
		HttpServiceForTest httpService = new HttpServiceForTest();
		register.setHttpService( httpService );

		Assert.assertEquals( 0, httpService.pathToServlet.size());
		register.starting();
		Assert.assertEquals( 1, httpService.pathToServlet.size());

		Mockito.verify( preferencesMngr, Mockito.only()).addToList( IPreferencesMngr.WEB_EXTENSIONS, CONTEXT );
		Mockito.reset( preferencesMngr );

		KibanaExtensionServlet servlet = (KibanaExtensionServlet) httpService.pathToServlet.get( CONTEXT );
		Assert.assertNotNull( servlet );

		// Change path at runtime
		servlet.setAgentDashBoardUrl( "agent url" );
		servlet.setAppDashBoardUrl( "app url" );

		// Stop...
		register.stopping();
		Assert.assertEquals( 0, httpService.pathToServlet.size());
		Mockito.verify( preferencesMngr, Mockito.only()).removeFromList( IPreferencesMngr.WEB_EXTENSIONS, CONTEXT );
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class HttpServiceForTest implements HttpService {
		Map<String,Servlet> pathToServlet = new HashMap<> ();

		@Override
		@SuppressWarnings( "rawtypes" )
		public void registerServlet( String alias, Servlet servlet, Dictionary initparams, HttpContext context )
		throws ServletException, NamespaceException {
			this.pathToServlet.put( alias, servlet );
		}

		@Override
		public void registerResources( String alias, String name, HttpContext context ) throws NamespaceException {
			// nothing
		}

		@Override
		public void unregister( String alias ) {
			this.pathToServlet.remove( alias );
		}

		@Override
		public HttpContext createDefaultHttpContext() {
			return null;
		}
	}
}
