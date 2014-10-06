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
import java.util.Hashtable;
import java.util.logging.Logger;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.services.internal.resources.ApplicationResource;
import net.roboconf.dm.rest.services.internal.resources.IApplicationResource;
import net.roboconf.dm.rest.services.internal.resources.IManagementResource;
import net.roboconf.dm.rest.services.internal.resources.ManagementResource;

import org.osgi.service.http.HttpService;

import com.sun.jersey.spi.container.servlet.ServletContainer;

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
	private static final String CONTEXT = "/roboconf-dm";

	// Injected by iPojo
	private HttpService httpService;
	private Manager manager;

	// Internal fields
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * The method to use when all the dependencies are resolved.
	 * @throws Exception
	 */
	public void starting() throws Exception {

		this.logger.fine( "iPojo registers a servlet to serve REST resources for the DM." );

		// Create the REST application with its resources
		IApplicationResource applicationResource = new ApplicationResource( this.manager );
		IManagementResource managementResource = new ManagementResource( this.manager );
		RestApplication app = new RestApplication( applicationResource, managementResource );

		// Prepare the parameters for the servlet
		Dictionary<String,String> initParams = new Hashtable<String,String> ();
		initParams.put( "com.sun.jersey.api.json.POJOMappingFeature", "true" );
		initParams.put( "com.sun.jersey.config.feature.DisableWADL", "true" );
		initParams.put(
				"com.sun.jersey.spi.container.ContainerResponseFilters",
				"net.roboconf.dm.internal.rest.services.internal.cors.ResponseCorsFilter" );

		// FIXME: not sure it will work... Our application does not scan.
		initParams.put(
				"com.sun.jersey.config.property.packages",
				"com.fasterxml.jackson.jaxrs.json;net.roboconf.dm.internal.rest.commons.json" );

		// Register a servlet to serve these REST resources
		ServletContainer jerseyServlet = new ServletContainer( app );
		this.httpService.registerServlet( CONTEXT, jerseyServlet, initParams, null );
	}


	/**
	 * The method to use when dependencies are invalidated.
	 * @throws Exception
	 */
	public void stopping() throws Exception {

		this.logger.fine( "iPojo unregisters a servlet to serve REST resources for the DM." );
		if( this.httpService != null )
			this.httpService.unregister( CONTEXT );
		else
			this.logger.fine( "The HTTP service is gone. The servlet was already unregistered." );
	}
}
