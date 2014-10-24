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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.ws.rs.core.UriBuilder;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.commons.json.JSonBindingUtils;
import net.roboconf.messaging.MessagingConstants;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ServletRegistrationComponentTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


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

		ServletContainer jerseyServlet = (ServletContainer) httpService.pathToServlet.get( ServletRegistrationComponent.CONTEXT );
		Assert.assertNotNull( jerseyServlet );

		register.stopping();
		Assert.assertEquals( 0, httpService.pathToServlet.size());
	}


	@Test
	public void testJsonSerialization_application() throws Exception {

		// This test guarantees that in an non-OSGi environment,
		// our REST application uses the properties we define.
		// And, in particular, the JSon serialization that we tailored.

		Application app = new Application( "my-app" );
		Manager manager = new Manager( MessagingConstants.FACTORY_TEST );
		manager.setConfigurationDirectoryLocation( this.folder.newFolder().getAbsolutePath());
		manager.update();
		manager.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		URI uri = UriBuilder.fromUri( "http://localhost/" ).port( 8090 ).build();
		RestApplication restApp = new RestApplication( manager );
		HttpServer httpServer = null;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );
			Assert.assertTrue( httpServer.isStarted());
			URI targetUri = UriBuilder.fromUri( uri ).path( UrlConstants.APPLICATIONS ).build();

			InputStream in = null;
			try {
				in = targetUri.toURL().openStream();
				Utils.copyStream( in, out );

			} finally {
				Utils.closeQuietly( in );
			}

		} finally {
			if( httpServer != null )
				httpServer.stop();
		}

		String received = out.toString();
		String expected = JSonBindingUtils.createObjectMapper().writeValueAsString( Arrays.asList( app ));
		Assert.assertEquals( expected, received );
	}


	@Test
	public void testJsonSerialization_instance() throws Exception {

		// This test guarantees that in an non-OSGi environment,
		// our REST application uses the properties we define.
		// And, in particular, the JSon serialization that we tailored.

		TestApplication app = new TestApplication();
		Manager manager = new Manager( MessagingConstants.FACTORY_TEST );
		manager.setConfigurationDirectoryLocation( this.folder.newFolder().getAbsolutePath());
		manager.update();
		manager.getAppNameToManagedApplication().put( app.getName(), new ManagedApplication( app, null ));

		URI uri = UriBuilder.fromUri( "http://localhost/" ).port( 8090 ).build();
		RestApplication restApp = new RestApplication( manager );
		HttpServer httpServer = null;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );
			Assert.assertTrue( httpServer.isStarted());
			URI targetUri = UriBuilder.fromUri( uri )
					.path( UrlConstants.APP ).path( app.getName()).path( "children" )
					.queryParam( "instance-path", "/tomcat-vm" ).build();

			InputStream in = null;
			try {
				in = targetUri.toURL().openStream();
				Utils.copyStream( in, out );

			} finally {
				Utils.closeQuietly( in );
			}

		} finally {
			if( httpServer != null )
				httpServer.stop();
		}

		String received = out.toString();
		String expected = JSonBindingUtils.createObjectMapper().writeValueAsString( Arrays.asList( app.getTomcat()));
		Assert.assertEquals( expected, received );
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
