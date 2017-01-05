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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.utils.IconUtils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.Manager;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Vincent Zurczak - Linagora
 */
@RunWith(MockitoJUnitRunner.class)
public class IconServletTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Manager manager;


	@After
	public void stopManager() {
		if( this.manager != null )
			this.manager.stop();
	}


	@Test
	public void testDoGet() throws Exception {

		// Prepare the servlet
		File configurationDirectory = this.folder.newFolder();

		this.manager = new Manager();
		this.manager.configurationMngr().setWorkingDirectory( configurationDirectory );

		File appDir = ConfigurationUtils.findApplicationDirectory( "app", configurationDirectory );
		File descDir = new File( appDir, Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.mkdirs());

		IconServlet servlet = new IconServlet( this.manager );

		// Add a fake file
		File trickFile = new File( descDir, "directory.jpg" );
		Assert.assertTrue( trickFile.mkdirs());

		// Prepare our mocks
		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );

		Mockito.when( req.getPathInfo()).thenReturn( "/app/whatever.jpg" );
		servlet.doGet( req, resp );
		Mockito.verify( resp ).setStatus( HttpServletResponse.SC_NOT_FOUND );

		// Now, add a real icon and make sure it is returned
		// (it is returned if a MIME type was set).
		File singleJpgFile = new File( descDir, "whatever.jpg" );
		Assert.assertTrue( singleJpgFile.createNewFile());

		ServletOutputStream out = Mockito.mock( ServletOutputStream.class );
		Mockito.when( resp.getOutputStream()).thenReturn( out );
		servlet.doGet( req, resp );
		Mockito.verify( resp ).setContentType( IconUtils.MIME_JPG );
	}
}
