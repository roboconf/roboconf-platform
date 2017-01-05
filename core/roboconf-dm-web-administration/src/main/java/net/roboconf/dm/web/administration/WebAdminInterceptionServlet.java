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

package net.roboconf.dm.web.administration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WebAdminInterceptionServlet extends HttpServlet {

	private static final long serialVersionUID = 6624260733747636486L;

	static final String CSS_STYLESHEET = "/roboconf.min.css";
	static final String BANNER_IMAGE = "/img/logo_roboconf_2.jpg";
	static final String OVERRIDE_CSS = "roboconf.custom.css";
	static final String OVERRIDE_IMAGE = "roboconf.custom.jpg";

	final transient Logger logger = Logger.getLogger( getClass().getName());
	String karafEtc = System.getProperty( Constants.KARAF_ETC );


	/*
	 * This servlet is used to allow one to customize the look and feel of the web administration.
	 * Concretely, it intercepts requests for the two files: the banner image and the CSS stylesheet.
	 *
	 * In the WEB-INF/web.xml file, it is associated with two paths, used as constants in this class.
	 * With these settings, here is a reminder of what the HttpServletRequest methods return.
	 *
	 * - getContextPath() => "/roboconf-web-administration" (as specified in the POM)
	 * - getRequestURL() => a fullURL, e.g. http://localhost:8181/roboconf-web-administration/img/logo_roboconf_2.jpg
	 * - getServletPath() => one of our two constants, e.g. /img/logo_roboconf_2.jpg
	 * - getRequestURI() => a concatenation of the context path and the servlet path, e.g. /roboconf-web-administration/img/logo_roboconf_2.jpg
	 * - Other methods generally return null or are of no interest for us.
	 */


	@Override
	protected void doGet( HttpServletRequest req, HttpServletResponse resp )
	throws ServletException, IOException {

		boolean responseSet = false;
		boolean validRequest = Arrays.asList( CSS_STYLESHEET, BANNER_IMAGE ).contains( req.getServletPath());
		if( ! validRequest )
			this.logger.severe( "An unexpected resource was asked to Roboconf's interception servlet: " + req.getServletPath());

		// If the resource exists in the Karaf ETC directory, then use it.
		if( ! Utils.isEmptyOrWhitespaces( this.karafEtc )) {

			File resource = null;
			if( CSS_STYLESHEET.equals( req.getServletPath()))
				resource = new File( this.karafEtc, OVERRIDE_CSS );
			else if( BANNER_IMAGE.equals( req.getServletPath()))
				resource = new File( this.karafEtc, OVERRIDE_IMAGE );

			if( resource != null && resource.isFile()) {
				this.logger.fine( "A custom resource was found for " + req.getServletPath());
				Utils.copyStream( resource, resp.getOutputStream());
				responseSet = true;
			}
		}

		// Otherwise, return the default file
		if( validRequest && ! responseSet ) {
			InputStream in = getServletContext().getResourceAsStream( req.getServletPath());
			try {
				if( in != null ) {
					Utils.copyStreamUnsafelyUseWithCaution( in, resp.getOutputStream());
					responseSet = true;
				}

			} finally {
				Utils.closeQuietly( in );
			}
		}

		// Complete the result if necessary
		if( ! responseSet ) {
			if( validRequest )
				resp.sendError( HttpServletResponse.SC_NOT_FOUND );
			else
				resp.sendError( HttpServletResponse.SC_FORBIDDEN );
		}
	}
}
