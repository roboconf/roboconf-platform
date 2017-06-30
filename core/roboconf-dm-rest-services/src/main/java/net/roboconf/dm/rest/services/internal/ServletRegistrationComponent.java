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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import javax.servlet.Filter;

import org.ops4j.pax.url.mvn.MavenResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.services.internal.filters.AuthenticationFilter;
import net.roboconf.dm.rest.services.internal.icons.IconServlet;
import net.roboconf.dm.rest.services.internal.websocket.RoboconfWebSocketServlet;
import net.roboconf.dm.rest.services.internal.websocket.WebSocketHandler;
import net.roboconf.dm.rest.services.jmx.RestServicesMBean;
import net.roboconf.dm.scheduler.IScheduler;

/**
 * An iPojo component in charge of injecting the Manager into the REST services.
 * <p>
 * This class also registers and unregisters these REST resources within the
 * HTTP service of the OSGi container.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class ServletRegistrationComponent implements RestServicesMBean {

	// Constants
	public static final String REST_CONTEXT = "/roboconf-dm";
	public static final String WEBSOCKET_CONTEXT = "/roboconf-dm-websocket";
	static final String ICONS_CONTEXT = "/roboconf-icons";

	// Injected by iPojo
	private HttpService httpService;
	private Manager manager;
	private IScheduler scheduler;
	private MavenResolver mavenResolver;

	private boolean enableCors = false;
	private boolean enableAuthentication = false;
	private long sessionPeriod;

	// Internal fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	private final BundleContext bundleContext;

	RestApplication app;
	ServletContainer jerseyServlet;

	ServiceRegistration<Filter> filterServiceRegistration;
	AuthenticationFilter authenticationFilter;
	AuthenticationManager authenticationMngr;


	/**
	 * Constructor.
	 * @param bundleContext
	 */
	public ServletRegistrationComponent( BundleContext bundleContext ) {
		this.bundleContext = bundleContext;
	}


	/**
	 * The method to use when all the dependencies are resolved.
	 * <p>
	 * It means iPojo guarantees that both the manager and the HTTP
	 * service are not null.
	 * </p>
	 *
	 * @throws Exception in case of critical error
	 */
	public void starting() throws Exception {
		this.logger.fine( "iPojo registers REST and icons servlets related to Roboconf's DM." );

		// Create the REST application with its resources.
		// The scheduler may be null, it is optional.
		this.app = new RestApplication( this.manager );
		this.app.setScheduler( this.scheduler );
		this.app.setMavenResolver( this.mavenResolver );
		this.app.enableCors( this.enableCors );
		this.app.setAuthenticationManager( this.authenticationMngr );

		Dictionary<String,String> initParams = new Hashtable<> ();
		initParams.put( "servlet-name", "Roboconf DM (REST)" );

		this.jerseyServlet = new ServletContainer( this.app );
		this.httpService.registerServlet( REST_CONTEXT, this.jerseyServlet, initParams, null );

		// Deal with the icons servlet
		initParams = new Hashtable<> ();
		initParams.put( "servlet-name", "Roboconf DM (icons)" );

		IconServlet iconServlet = new IconServlet( this.manager );
		this.httpService.registerServlet( ICONS_CONTEXT, iconServlet, initParams, null );

		// Register the web socket
		initParams = new Hashtable<> ();
		initParams.put( "servlet-name", "Roboconf DM (websocket)" );

		RoboconfWebSocketServlet websocketServlet = new RoboconfWebSocketServlet();
		this.httpService.registerServlet( WEBSOCKET_CONTEXT, websocketServlet, initParams, null );

		// Register a filter for authentication
		this.authenticationFilter = new AuthenticationFilter( this );
		this.authenticationFilter.setAuthenticationEnabled( this.enableAuthentication );
		this.authenticationFilter.setAuthenticationManager( this.authenticationMngr );
		this.authenticationFilter.setSessionPeriod( this.sessionPeriod );
		this.authenticationFilter.setEnableCors( this.enableCors );

		initParams = new Hashtable<> ();
		initParams.put( "urlPatterns", "*" );

		// Consider the bundle context can be null (e.g. when used outside of OSGi)
		if( this.bundleContext != null )
			this.filterServiceRegistration = this.bundleContext.registerService( Filter.class, this.authenticationFilter, initParams );
		else
			this.logger.warning( "No bundle context was available, the authentication filter was not registered." );
	}


	/**
	 * The method to use when dependencies are invalidated.
	 * @throws Exception in case of critical error
	 */
	public void stopping() throws Exception {

		// Remove the filter
		if( this.filterServiceRegistration != null )
			this.filterServiceRegistration.unregister();

		// Update the HTTP service
		this.logger.fine( "iPojo unregisters REST and icons servlets related to Roboconf's DM." );
		if( this.httpService != null ) {
			this.httpService.unregister( REST_CONTEXT );
			this.httpService.unregister( ICONS_CONTEXT );
			this.httpService.unregister( WEBSOCKET_CONTEXT );

		} else {
			this.logger.fine( "The HTTP service is gone. The servlets were already unregistered." );
		}

		// Reset the application
		this.app = null;
		this.jerseyServlet = null;
		this.filterServiceRegistration = null;
		this.authenticationFilter = null;
	}


	/**
	 * Invoked by iPojo when the scheduler appears.
	 * @throws Exception in case of error
	 */
	public void schedulerAppears() throws Exception {

		// We simply update the "scheduler" resource.
		this.logger.fine( "Roboconf's scheduler is here. Updating the REST resource." );
		if( this.app != null )
			this.app.setScheduler( this.scheduler );
	}


	/**
	 * Invoked by iPojo when the scheduler disappears.
	 * @throws Exception in case of error
	 */
	public void schedulerDisappears() throws Exception {

		// We simply update the "scheduler" resource.
		this.logger.fine( "Roboconf's scheduler vanished. Updating the REST resource." );
		if( this.app != null )
			this.app.setScheduler( null );
	}


	/**
	 * Invoked by iPojo when the Maven resolver appears.
	 * @throws Exception in case of error
	 */
	public void mavenResolverAppears() throws Exception {

		// We simply update the "urlResolver" resource.
		this.logger.fine( "Roboconf's URL resolver is here. Updating the REST resource." );
		if( this.app != null )
			this.app.setMavenResolver( this.mavenResolver );
	}


	/**
	 * Invoked by iPojo when the Maven resolver disappears.
	 * @throws Exception in case of error
	 */
	public void mavenResolverDisappears() throws Exception {

		// We simply update the "urlResolver" resource.
		this.logger.fine( "Roboconf's URL resolver vanished. Updating the REST resource." );
		if( this.app != null )
			this.app.setMavenResolver( null );
	}


	/**
	 * @param httpService the httpService to set
	 */
	public void setHttpService( HttpService httpService ) {
		this.httpService = httpService;
	}


	/**
	 * @param manager the manager to set
	 */
	public void setManager( Manager manager ) {
		this.manager = manager;
	}


	/**
	 * Invoked by iPojo.
	 * @param enableCors the enableCors to set
	 */
	public void setEnableCors( boolean enableCors ) {

		this.logger.fine( "CORS is now " + (enableCors ? "enabled" : "disabled") + ". Updating the REST resource." );
		this.enableCors = enableCors;

		if( this.app != null )
			this.app.enableCors( enableCors );

		if( this.authenticationFilter != null )
			this.authenticationFilter.setEnableCors( enableCors );

		try {
			if( this.jerseyServlet != null )
				this.jerseyServlet.reload();

		} catch( Exception e ) {
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Invoked by iPojo.
	 * @param enableAuthentication the enableAuthentication to set
	 */
	public void setEnableAuthentication( boolean enableAuthentication ) {

		this.logger.fine( "Authentication is now " + (enableAuthentication ? "enabled" : "disabled") + ". Updating the REST resource." );
		this.enableAuthentication = enableAuthentication;

		if( this.authenticationFilter != null )
			this.authenticationFilter.setAuthenticationEnabled( enableAuthentication );
	}


	/**
	 * @param authenticationRealm the authenticationRealm to set
	 */
	public void setAuthenticationRealm( String authenticationRealm ) {

		// Given the way sessions are stored in AuthenticationManager (private map),
		// changing the realm will invalidate all the current sessions
		this.logger.fine( "New authentication realm: " + authenticationRealm );
		this.authenticationMngr = new AuthenticationManager( authenticationRealm );

		// Propagate the change
		if( this.authenticationFilter != null )
			this.authenticationFilter.setAuthenticationManager( this.authenticationMngr );

		if( this.app != null )
			this.app.setAuthenticationManager( this.authenticationMngr );
	}


	/**
	 * @param sessionPeriod the sessionPeriod to set
	 */
	public void setSessionPeriod( long sessionPeriod ) {

		this.logger.fine( "New session period: " + sessionPeriod );
		this.sessionPeriod = sessionPeriod;

		if( this.authenticationFilter != null )
			this.authenticationFilter.setSessionPeriod( sessionPeriod );
	}


	// These setters are not used by iPojo.
	// But they may be useful when using this class outside OSGi.


	/**
	 * @param scheduler the scheduler to set
	 */
	public void setScheduler( IScheduler scheduler ) {
		this.scheduler = scheduler;
	}


	/**
	 * @param mavenResolver the Maven resolver to set
	 */
	public void setMavenResolver( MavenResolver mavenResolver ) {
		this.mavenResolver = mavenResolver;
	}


	// MBeans

	public final AtomicLong restRequestsCount = new AtomicLong();
	public final AtomicLong restRequestsWithAuthFailureCount = new AtomicLong();

	// Web socket handlers are not created by us directly.
	// Static access is the most simple solution.
	public static final AtomicLong WS_CONNECTION_ERRORS_COUNT = new AtomicLong();


	@Override
	public int getCurrentWebSocketClientsCount() {
		return WebSocketHandler.getSessionsCount();
	}


	@Override
	public long getWebSocketConnectionErrorsCount() {
		return WS_CONNECTION_ERRORS_COUNT.get();
	}


	@Override
	public long getRestRequestsWithAuthFailureCount() {
		return this.restRequestsWithAuthFailureCount.get();
	}


	@Override
	public long getRestRequestsCount() {
		return this.restRequestsCount.get();
	}


	@Override
	public boolean isAuthenticationRequired() {
		return this.enableAuthentication;
	}


	@Override
	public boolean isCorsEnabled() {
		return this.enableCors;
	}


	@Override
	public void reset() {
		this.restRequestsCount.set( 0 );
		this.restRequestsWithAuthFailureCount.set( 0 );
		WS_CONNECTION_ERRORS_COUNT.set( 0 );
	}
}
