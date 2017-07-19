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

package net.roboconf.dm.rest.services.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.commons.json.JSonBindingUtils;
import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.services.internal.filters.AuthenticationFilter;
import net.roboconf.messaging.api.MessagingConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ServletRegistrationComponentTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private TestApplication app;

	private ServletRegistrationComponent register;
	private BundleContext bundleContext;


	@Before
	@SuppressWarnings( "unchecked" )
	public void initializeManager() throws Exception {

		this.manager = new Manager();
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		this.app = new TestApplication();
		this.app.setDirectory( this.folder.newFolder());

		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.addManagedApplication( new ManagedApplication( this.app ));

		this.bundleContext = Mockito.mock( BundleContext.class );
		Mockito.when( this.bundleContext.registerService(
				Mockito.eq( Filter.class ),
				Mockito.any( Filter.class ),
				Mockito.any( Dictionary.class ))).thenReturn( Mockito.mock( ServiceRegistration.class ));

		this.register = new ServletRegistrationComponent( this.bundleContext );
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testStop_httpServiceIsNull() throws Exception {

		this.register.stopping();
		Mockito.verifyZeroInteractions( this.bundleContext );
	}


	@Test
	@SuppressWarnings( "unchecked" )
	public void testStartAndStop() throws Exception {

		// No error if these methods are called before the component is started.
		this.register.schedulerAppears();
		this.register.schedulerDisappears();

		this.register.mavenResolverAppears();
		this.register.mavenResolverDisappears();

		// Deal with the HTTP service.
		HttpServiceForTest httpService = new HttpServiceForTest();
		this.register.setHttpService( httpService );

		Assert.assertEquals( 0, httpService.pathToServlet.size());
		Mockito.verifyZeroInteractions( this.bundleContext );
		this.register.starting();

		Assert.assertEquals( 3, httpService.pathToServlet.size());
		Mockito.verify( this.bundleContext, Mockito.only()).registerService(
				Mockito.eq( Filter.class ),
				Mockito.any( AuthenticationFilter.class ),
				Mockito.any( Dictionary.class ));

		ServletContainer jerseyServlet = (ServletContainer) httpService.pathToServlet.get( ServletRegistrationComponent.REST_CONTEXT );
		Assert.assertNotNull( jerseyServlet );

		HttpServlet iconsServlet = (HttpServlet) httpService.pathToServlet.get( ServletRegistrationComponent.ICONS_CONTEXT );
		Assert.assertNotNull( iconsServlet );

		HttpServlet websocketServlet = (HttpServlet) httpService.pathToServlet.get( ServletRegistrationComponent.WEBSOCKET_CONTEXT );
		Assert.assertNotNull( websocketServlet );

		// Check there is no authentication manager
		Assert.assertNull( this.register.authenticationMngr );
		this.register.setAuthenticationRealm( "realm" );
		Assert.assertNotNull( this.register.authenticationMngr );

		// Update the scheduler...
		this.register.schedulerAppears();
		this.register.schedulerDisappears();

		// Update the URL resolver
		this.register.mavenResolverAppears();
		this.register.mavenResolverDisappears();

		// Stop...
		this.register.stopping();

		Assert.assertEquals( 0, httpService.pathToServlet.size());
		Assert.assertNull( this.register.app );
		Assert.assertNull( this.register.authenticationFilter );
		Assert.assertNull( this.register.jerseyServlet );
		Assert.assertNull( this.register.filterServiceRegistration );
		Assert.assertNotNull( this.register.authenticationMngr );
	}


	@Test
	public void testJsonSerialization_application() throws Exception {

		// This test guarantees that in an non-OSGi environment,
		// our REST application uses the properties we define.
		// And, in particular, the JSon serialization that we tailored.

		URI uri = UriBuilder.fromUri( "http://localhost/" ).port( 8090 ).build();
		RestApplication restApp = new RestApplication( this.manager );
		HttpServer httpServer = null;
		String received = null;

		try {
			httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );
			Assert.assertTrue( httpServer.isStarted());
			URI targetUri = UriBuilder.fromUri( uri ).path( UrlConstants.APPLICATIONS ).build();
			received = Utils.readUrlContent( targetUri.toString());

		} finally {
			if( httpServer != null )
				httpServer.stop();
		}

		String expected = JSonBindingUtils.createObjectMapper().writeValueAsString( Arrays.asList( this.app ));
		Assert.assertEquals( expected, received );
	}


	@Test
	public void testJsonSerialization_instance() throws Exception {

		// This test guarantees that in an non-OSGi environment,
		// our REST application uses the properties we define.
		// And, in particular, the JSon serialization that we tailored.

		URI uri = UriBuilder.fromUri( "http://localhost/" ).port( 8090 ).build();
		RestApplication restApp = new RestApplication( this.manager );
		HttpServer httpServer = null;
		String received = null;

		try {
			httpServer = GrizzlyServerFactory.createHttpServer( uri, restApp );
			Assert.assertTrue( httpServer.isStarted());
			URI targetUri = UriBuilder.fromUri( uri )
					.path( UrlConstants.APP ).path( this.app.getName()).path( "instances" )
					.queryParam( "instance-path", "/tomcat-vm" ).build();

			received = Utils.readUrlContent( targetUri.toString());

		} finally {
			if( httpServer != null )
				httpServer.stop();
		}

		String expected = JSonBindingUtils.createObjectMapper().writeValueAsString( Arrays.asList( this.app.getTomcat()));
		Assert.assertEquals( expected, received );
	}


	@Test
	public void testSetEnableCors() throws Exception {

		// No NPE
		this.register.setEnableCors( true );
		this.register.setEnableCors( false );

		// Act like if the component had been started
		this.register.app = Mockito.spy( new RestApplication( this.manager ));
		this.register.jerseyServlet = Mockito.mock( ServletContainer.class );

		this.register.setEnableCors( true );
		Mockito.verify( this.register.app, Mockito.times( 1 )).enableCors( true );
		Mockito.verify( this.register.jerseyServlet, Mockito.only()).reload();

		Mockito.reset( this.register.app );
		Mockito.reset( this.register.jerseyServlet );

		this.register.setEnableCors( false );
		Mockito.verify( this.register.app, Mockito.times( 1 )).enableCors( false );
		Mockito.verify( this.register.jerseyServlet, Mockito.only()).reload();

		// Stop...
		this.register.stopping();

		// No NPE
		this.register.setEnableCors( true );
		Assert.assertTrue( this.register.isCorsEnabled());
		this.register.setEnableCors( false );
		Assert.assertFalse( this.register.isCorsEnabled());
	}


	@Test
	public void testSetSessionPeriod() throws Exception {

		// No NPE
		this.register.setSessionPeriod( 50 );

		// Act like if the component had been started
		this.register.authenticationFilter = Mockito.mock( AuthenticationFilter.class );

		this.register.setSessionPeriod( 500 );
		Mockito.verify( this.register.authenticationFilter, Mockito.only()).setSessionPeriod( 500 );

		// Stop...
		this.register.stopping();

		// No NPE
		this.register.setSessionPeriod( -1 );
	}


	@Test
	public void testSetEnableAuthentication() throws Exception {

		// No NPE
		this.register.setEnableAuthentication( true );
		this.register.setEnableAuthentication( false );

		// Act like if the component had been started
		this.register.authenticationFilter = Mockito.mock( AuthenticationFilter.class );

		this.register.setEnableAuthentication( true );
		Mockito.verify( this.register.authenticationFilter, Mockito.only()).setAuthenticationEnabled( true );

		// Stop...
		this.register.stopping();

		// No NPE
		this.register.setEnableAuthentication( false );
		Assert.assertFalse( this.register.isAuthenticationRequired());
		this.register.setEnableAuthentication( true );
		Assert.assertTrue( this.register.isAuthenticationRequired());
	}


	@Test
	public void testSetAuthenticationRealm() throws Exception {

		// No NPE
		Assert.assertNull( this.register.authenticationMngr );
		this.register.setAuthenticationRealm( "realm" );
		Assert.assertNotNull( this.register.authenticationMngr );

		// Act like if the component had been started
		this.register.authenticationFilter = Mockito.mock( AuthenticationFilter.class );

		this.register.setAuthenticationRealm( "realm2" );
		Mockito.verify( this.register.authenticationFilter, Mockito.only()).setAuthenticationManager( Mockito.any( AuthenticationManager.class ));

		// Stop...
		this.register.stopping();

		// No NPE
		this.register.setAuthenticationRealm( "realm" );
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
