/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.rest.services.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import junit.framework.Assert;

import org.junit.Test;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.sun.jersey.spi.container.servlet.ServletContainer;

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

		ServletRegistrationComponent register = new ServletRegistrationComponent();
		HttpServiceForTest httpService = new HttpServiceForTest();
		register.setHttpService( httpService );

		Assert.assertEquals( 0, httpService.pathToServlet.size());
		register.starting();
		Assert.assertEquals( 1, httpService.pathToServlet.size());

		Servlet servlet = httpService.pathToServlet.get( ServletRegistrationComponent.CONTEXT );
		Assert.assertEquals( ServletContainer.class, servlet.getClass());

		register.stopping();
		Assert.assertEquals( 0, httpService.pathToServlet.size());
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class HttpServiceForTest implements HttpService {
		Map<String,Servlet> pathToServlet = new HashMap<String,Servlet> ();

		@Override
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
