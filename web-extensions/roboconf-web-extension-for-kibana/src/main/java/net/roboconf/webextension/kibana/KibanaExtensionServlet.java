/**
 * Copyright 2016 Linagora, Université Joseph Fourier, Floralis
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

import static net.roboconf.webextension.kibana.KibanaExtensionConstants.CONTEXT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	}


	/**
	 * @param agentDashBoardUrl the agentDashBoardUrl to set
	 */
	public void setAgentDashBoardUrl( String agentDashBoardUrl ) {
		this.agentDashBoardUrl = agentDashBoardUrl;
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
		if( ! Utils.isEmptyOrWhitespaces( reqHead ))
			resp.addHeader( "Access-Control-Allow-Headers", reqHead );

		// Prepare the response
		if( Utils.isEmptyOrWhitespaces( path ) || "/".equals( path )) {
			List<Application> applications = new ArrayList<> ();
			for( ManagedApplication ma : this.manager.applicationMngr().getManagedApplications()) {
				applications.add( ma.getApplication());
			}

			content = applicationsTable( applications, this.appDashBoardUrl );

		} else if( path.startsWith( "/" )) {
			String appName = path.substring( 1 ).trim();
			Application app = this.manager.applicationMngr().findApplicationByName( appName );
			if( app != null ) {
				List<String> instancePaths = new ArrayList<> ();
				for( Instance inst : InstanceHelpers.findAllScopedInstances( app )) {
					instancePaths.add( InstanceHelpers.computeInstancePath( inst ));
				}

				content = agentsTable( app, instancePaths, this.agentDashBoardUrl );
			}
		}

		// Set the response
		if( content == null ) {
			resp.sendError( HttpServletResponse.SC_NOT_FOUND );

		} else {
			ByteArrayInputStream in = new ByteArrayInputStream( content.getBytes( "UTF-8" ));
			Utils.copyStreamUnsafelyUseWithCaution( in, resp.getOutputStream());
		}
	}


	/**
	 * Builds a HTML table with dashboards links for applications.
	 * @param applications a non-null list of applications
	 * @param kibanaLocation Kibana's location (with a link to the dashboard)
	 * @return a non-null string
	 */
	static String applicationsTable( List<Application> applications, String kibanaLocation ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "<p>This page lists all the Kibana Dashboards for Roboconf applications.<br />\n" );
		sb.append( "Notice that all the agents are considered as having Decanter installed and configured to work " );
		sb.append( "with Elastic Search and Kibana. Please, refer to <a href=\"\">this page</a> for more details.</p>\n<br />\n" );

		sb.append( "<table class=\"table table-hover table-rbcf\">\n<tr>\n" );
		sb.append( "\t<th>Application Name</th>\n" );
		sb.append( "\t<th>Application's Dashboard</th>\n" );
		sb.append( "\t<th>Agents List</th>\n" );
		sb.append( "</tr>\n" );

		sb.append( "<tr>\n" );
		sb.append( "\t<td>DM</td>\n" );
		sb.append( "\t<td><a href=\"" );
		sb.append( kibanaLocation );
		sb.append( "?query:(query_string:(analyze_wildcard:!t,query:source%3DDM))" );
		sb.append( "\">here</a></td>\n" );
		sb.append( "\t<td>-</td>\n</tr>\n" );

		for( Application app : applications ) {
			sb.append( "<tr>\n" );

			sb.append( "\t<td>" );
			sb.append( app.getName());
			sb.append( "</td>\n" );

			sb.append( "\t<td><a href=\"" );
			sb.append( kibanaLocation );
			sb.append( "?query:(query_string:(analyze_wildcard:!t,query:source%3Dapp%20" );
			sb.append( app.getName());
			sb.append( "))\">here</a></td>\n" );

			sb.append( "\t<td>" );
			sb.append( CONTEXT );
			sb.append( "/" );
			sb.append( app.getName());
			sb.append( "</td>\n" );
			sb.append( "</tr>\n" );
		}

		sb.append( "</table>\n" );
		return sb.toString();
	}


	/**
	 * Builds a HTML table with dashboards links for agents of a given application.
	 * @param application the application
	 * @param instancePaths the list of instance paths
	 * @param kibanaLocation Kibana's location (with a link to the dashboard)
	 * @return a non-null string
	 */
	static String agentsTable( Application application, List<String> instancePaths, String kibanaLocation ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "<p>This page lists all the Kibana Dashboards for " );
		sb.append( application.getName());
		sb.append( ".<br />\nNotice that all the agents are considered as having Decanter installed and configured to work " );
		sb.append( "with Elastic Search and Kibana. Please, refer to <a href=\"\">this page</a> for more details.</p>\n<br />\n" );

		sb.append( "<table class=\"table table-hover table-rbcf\">\n<tr>\n" );
		sb.append( "\t<th>Instance Path</th>\n" );
		sb.append( "\t<th>Associated Dashboard</th>\n" );
		sb.append( "</tr>\n" );

		sb.append( "<tr>\n" );
		sb.append( "\t<td>DM</td>\n" );
		sb.append( "\t<td><a href=\"" );
		sb.append( kibanaLocation );
		sb.append( "?query:(query_string:(analyze_wildcard:!t,query:source%3DDM))" );
		sb.append( "\">here</a></td>\n</tr>\n" );

		for( String instancePath : instancePaths ) {
			sb.append( "<tr>\n" );

			sb.append( "\t<td>" );
			sb.append( instancePath );
			sb.append( "</td>\n" );

			sb.append( "\t<td><a href=\"" );
			sb.append( kibanaLocation );
			sb.append( "?query:(query_string:(analyze_wildcard:!t,query:source%3Dapp%20" );
			sb.append( application.getName());
			sb.append( "%20" );
			sb.append( instancePath );
			sb.append( "))\">here</a></td>\n" );
			sb.append( "</tr>\n" );
		}

		sb.append( "</table>\n" );
		return sb.toString();
	}
}
