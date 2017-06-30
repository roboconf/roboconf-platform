/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.webextension.kibana;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;

/**
 * @author Vincent Zurczak - Linagora
 */
public class KibanaExtensionServlet extends HttpServlet {

	private static final long serialVersionUID = 6624260733747636416L;

	final transient Logger logger = Logger.getLogger( getClass().getName());
	transient Manager manager;
	transient String appDashBoardUrl, agentDashBoardUrl;


	/*
	 * In the WEB-INF/web.xml file, it is associated with two paths, used as constants in this class.
	 * With these settings, here is a reminder of what the HttpServletRequest methods return.
	 *
	 * - getContextPath() => ""
	 * - getRequestURL() => a fullURL, e.g. http://localhost:8181/roboconf-web-extension/kibana (+ /optional-sub-path)
	 * - getServletPath() => "/roboconf-web-extension/kibana"
	 * - getRequestURI() => "/roboconf-web-extension/kibana" + "/optional-sub-path"
	 * - Other methods generally return null or are of no interest for us.
	 */


	/**
	 * Constructor.
	 * @param manager
	 */
	public KibanaExtensionServlet( Manager manager ) {
		this.manager = manager;
	}


	/**
	 * @param appDashBoardUrl the appDashBoardUrl to set
	 */
	public void setAppDashBoardUrl( String appDashBoardUrl ) {
		this.appDashBoardUrl = appDashBoardUrl;
		this.logger.finer( "App dashboard URL set to " + appDashBoardUrl );
	}


	/**
	 * @param agentDashBoardUrl the agentDashBoardUrl to set
	 */
	public void setAgentDashBoardUrl( String agentDashBoardUrl ) {
		this.agentDashBoardUrl = agentDashBoardUrl;
		this.logger.finer( "Agent dashboard URL set to " + agentDashBoardUrl );
	}


	@Override
	protected void doGet( HttpServletRequest req, HttpServletResponse resp )
	throws ServletException, IOException {

		String content = null;
		String path = req.getRequestURI().substring( req.getServletPath().length());

		// Handle CORS
		resp.addHeader( "Access-Control-Allow-Origin","*" );
		resp.addHeader( "Access-Control-Allow-Methods", "GET, DELETE, POST, OPTIONS" );
		String reqHead = req.getHeader( "Access-Control-Request-Headers" );
		if( ! Utils.isEmptyOrWhitespaces( reqHead )) {
			reqHead = URLEncoder.encode( reqHead, StandardCharsets.UTF_8.displayName());
			resp.addHeader( "Access-Control-Allow-Headers", reqHead );
		}

		// Prepare the response
		if( Utils.isEmptyOrWhitespaces( path ) || "/".equals( path )) {
			List<Application> applications = new ArrayList<> ();
			for( ManagedApplication ma : this.manager.applicationMngr().getManagedApplications()) {
				applications.add( ma.getApplication());
			}

			JtwigTemplate template = JtwigTemplate.classpathTemplate( "apps.twig" );
			JtwigModel model = JtwigModel.newModel()
					.with( "kibanaLocation", this.appDashBoardUrl )
					.with( "baseLocation", KibanaExtensionConstants.WEB_ADMIN_PATH )
					.with( "apps", applications );

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			template.render( model, os );
			content = os.toString( "UTF-8" );

		} else if( path.startsWith( "/" )) {
			String appName = path.substring( 1 ).trim();
			Application app = this.manager.applicationMngr().findApplicationByName( appName );
			if( app != null ) {
				List<String> instancePaths = new ArrayList<> ();
				for( Instance inst : InstanceHelpers.findAllScopedInstances( app )) {
					instancePaths.add( InstanceHelpers.computeInstancePath( inst ));
				}

				JtwigTemplate template = JtwigTemplate.classpathTemplate( "app.twig" );
				JtwigModel model = JtwigModel.newModel()
						.with( "kibanaLocation", this.agentDashBoardUrl )
						.with( "baseLocation", KibanaExtensionConstants.WEB_ADMIN_PATH )
						.with( "appName", appName )
						.with( "instancePaths", instancePaths );

				ByteArrayOutputStream os = new ByteArrayOutputStream();
				template.render( model, os );
				content = os.toString( "UTF-8" );
			}
		}


		// Set the response
		if( content == null ) {
			resp.sendError( HttpServletResponse.SC_NOT_FOUND );

		} else {
			ByteArrayInputStream in = new ByteArrayInputStream( content.getBytes( StandardCharsets.UTF_8 ));
			Utils.copyStreamUnsafelyUseWithCaution( in, resp.getOutputStream());
		}
	}
}
