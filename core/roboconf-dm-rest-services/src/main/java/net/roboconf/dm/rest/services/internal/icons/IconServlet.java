/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.icons;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.roboconf.core.utils.IconUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;

/**
 * A servlet to serve icons associated with applications.
 * @author Vincent Zurczak - Linagora
 */
public class IconServlet extends HttpServlet {

	private static final long serialVersionUID = -5151659440091869460L;
	private final transient Manager manager;
	private final transient Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Constructor.
	 * @param manager the manager
	 */
	public IconServlet( Manager manager ) {
		this.manager = manager;
	}


	@Override
	protected void doGet( HttpServletRequest req, HttpServletResponse resp )
	throws ServletException, IOException {

		this.logger.finer( "An icon was requested for " + req.getPathInfo());
		File f = this.manager.configurationMngr().findIconFromPath( req.getPathInfo());
		if( f == null ) {
			resp.setStatus( HttpServletResponse.SC_NOT_FOUND );

		} else {
			resp.setContentType( IconUtils.findMimeType( f ));
			resp.setContentLength((int) f.length());

			OutputStream out = resp.getOutputStream();
			Utils.copyStream( f, out );
			out.flush();
			Utils.closeQuietly( out );
		}
	}
}
