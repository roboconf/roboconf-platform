/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.logging.Logger;

import org.osgi.service.http.HttpService;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.services.internal.icons.IconServlet;
import net.roboconf.dm.rest.services.internal.websocket.RoboconfWebSocketServlet;
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
public class ServletRegistrationComponent {

	// Constants
	static final String REST_CONTEXT = "/roboconf-dm";
	static final String WEBSOCKET_CONTEXT = "/roboconf-dm-websocket";
	static final String ICONS_CONTEXT = "/roboconf-icons";

	// Injected by iPojo
	private HttpService httpService;
	private Manager manager;
	private IScheduler scheduler;
	private boolean enableCors = false;

	// Internal fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	RestApplication app;
	ServletContainer jerseyServlet;


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
		this.app.enableCors( this.enableCors );

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
	}


	/**
	 * The method to use when dependencies are invalidated.
	 * @throws Exception in case of critical error
	 */
	public void stopping() throws Exception {

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
	 * @param httpService the httpService to set
	 */
	void setHttpService( HttpService httpService ) {
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

		try {
			if( this.jerseyServlet != null )
				this.jerseyServlet.reload();

		} catch( Exception e ) {
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * @param scheduler the scheduler to set
	 */
	public void setScheduler( IScheduler scheduler ) {
		this.scheduler = scheduler;
	}
}
