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
import java.util.Hashtable;
import java.util.logging.Logger;

import org.osgi.service.http.HttpService;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.webextension.kibana.KibanaExtensionServlet;

/**
 * An iPojo component in charge of injecting the Manager into the servlet.
 * <p>
 * This class also registers and unregisters these servlet within the
 * HTTP service.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class ServletRegistrationComponent {

	// Injected by iPojo
	private HttpService httpService;
	private Manager manager;
	private String appDashBoardUrl, agentDashBoardUrl;

	// Internal fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	private KibanaExtensionServlet servlet;


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

		// Register the servlet
		Dictionary<String,String> initParams = new Hashtable<> ();
		initParams.put( "servlet-name", "Roboconf Web Extension (Kibana)" );

		this.servlet = new KibanaExtensionServlet( this.manager );
		this.servlet.setAgentDashBoardUrl( this.agentDashBoardUrl );
		this.servlet.setAppDashBoardUrl( this.appDashBoardUrl );
		this.httpService.registerServlet( CONTEXT, this.servlet, initParams, null );

		// Register a new preference
		this.manager.preferencesMngr().addToList( IPreferencesMngr.WEB_EXTENSIONS, CONTEXT );
	}


	/**
	 * The method to use when dependencies are invalidated.
	 * @throws Exception in case of critical error
	 */
	public void stopping() throws Exception {

		// Update the HTTP service
		this.logger.fine( "iPojo unregisters REST and icons servlets related to Roboconf's DM." );
		if( this.httpService != null ) {
			this.httpService.unregister( CONTEXT );

		} else {
			this.logger.fine( "The HTTP service is gone. The servlets were already unregistered." );
		}

		// Unregister the preference
		if( this.manager != null ) {
			this.manager.preferencesMngr().removeFromList( IPreferencesMngr.WEB_EXTENSIONS, CONTEXT );
		}
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
	 * @param appDashBoardUrl the appDashBoardUrl to set
	 */
	public void setAppDashBoardUrl( String appDashBoardUrl ) {
		this.appDashBoardUrl = appDashBoardUrl;
		if( this.servlet != null )
			this.servlet.setAppDashBoardUrl( appDashBoardUrl );
	}


	/**
	 * @param agentDashBoardUrl the agentDashBoardUrl to set
	 */
	public void setAgentDashBoardUrl( String agentDashBoardUrl ) {
		this.agentDashBoardUrl = agentDashBoardUrl;
		if( this.servlet != null )
			this.servlet.setAgentDashBoardUrl( agentDashBoardUrl );
	}
}
