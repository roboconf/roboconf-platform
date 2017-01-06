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

import static net.roboconf.dm.web.administration.WebAdminInterceptionServlet.BANNER_IMAGE;
import static net.roboconf.dm.web.administration.WebAdminInterceptionServlet.CSS_STYLESHEET;
import static net.roboconf.dm.web.administration.WebAdminInterceptionServlet.OVERRIDE_CSS;
import static net.roboconf.dm.web.administration.WebAdminInterceptionServlet.OVERRIDE_IMAGE;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WebAdminInterceptionServletTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testInterceptionServlet_noKarafEtc_notFound() throws Exception {

		// Mocks
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getServletPath()).thenReturn( CSS_STYLESHEET );

		ServletConfig sc = Mockito.mock( ServletConfig.class );
		ServletContext ctx = Mockito.mock( ServletContext.class );
		Mockito.when( sc.getServletContext()).thenReturn( ctx );

		// Initialization
		WebAdminInterceptionServlet servlet = new WebAdminInterceptionServlet();
		servlet.init( sc );
		servlet.karafEtc = "";

		// Execution
		servlet.doGet( req, resp );

		// Assertions
		Mockito.verify( req, Mockito.atLeast( 1 )).getServletPath();
		Mockito.verify( resp, Mockito.only()).sendError( HttpServletResponse.SC_NOT_FOUND );
	}


	@Test
	public void testInterceptionServlet_noKarafEtc_forbidden() throws Exception {

		// Mocks
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getServletPath()).thenReturn( "invalid-resource" );

		ServletConfig sc = Mockito.mock( ServletConfig.class );
		ServletContext ctx = Mockito.mock( ServletContext.class );
		Mockito.when( sc.getServletContext()).thenReturn( ctx );

		// Initialization
		WebAdminInterceptionServlet servlet = new WebAdminInterceptionServlet();
		servlet.init( sc );
		servlet.karafEtc = "";

		// Execution
		servlet.doGet( req, resp );

		// Assertions
		Mockito.verify( req, Mockito.atLeast( 1 )).getServletPath();
		Mockito.verify( resp, Mockito.only()).sendError( HttpServletResponse.SC_FORBIDDEN );
	}


	@Test
	public void testInterceptionServlet_noKarafEtc_defaultFound() throws Exception {

		// Mocks
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		Mockito.when( resp.getOutputStream()).thenReturn( Mockito.mock( ServletOutputStream.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getServletPath()).thenReturn( CSS_STYLESHEET );

		ServletConfig sc = Mockito.mock( ServletConfig.class );
		ServletContext ctx = Mockito.mock( ServletContext.class );
		Mockito.when( ctx.getResourceAsStream( Mockito.anyString())).thenReturn( new ByteArrayInputStream( new byte[ 0 ]));
		Mockito.when( sc.getServletContext()).thenReturn( ctx );

		// Initialization
		WebAdminInterceptionServlet servlet = new WebAdminInterceptionServlet();
		servlet.init( sc );

		// Execution
		servlet.doGet( req, resp );

		// Assertions
		Mockito.verify( req, Mockito.atLeast( 1 )).getServletPath();
		Mockito.verify( resp, Mockito.only()).getOutputStream();
	}


	@Test
	public void testInterceptionServlet_withKarafEtc_overrideCss() throws Exception {

		// Mocks
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		Mockito.when( resp.getOutputStream()).thenReturn( Mockito.mock( ServletOutputStream.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getServletPath()).thenReturn( CSS_STYLESHEET );

		ServletConfig sc = Mockito.mock( ServletConfig.class );
		ServletContext ctx = Mockito.mock( ServletContext.class );
		Mockito.when( sc.getServletContext()).thenReturn( ctx );

		// Initialization
		WebAdminInterceptionServlet servlet = new WebAdminInterceptionServlet();
		servlet.init( sc );

		File dir = this.folder.newFolder();
		servlet.karafEtc = dir.getAbsolutePath();

		File f = new File( dir, OVERRIDE_CSS );
		Utils.writeStringInto( "something", f );
		Assert.assertTrue( f.exists());

		// Execution
		servlet.doGet( req, resp );

		// Assertions
		Mockito.verify( req, Mockito.atLeast( 1 )).getServletPath();
		Mockito.verify( resp, Mockito.only()).getOutputStream();
	}


	@Test
	public void testInterceptionServlet_withKarafEtc_overrideBanner() throws Exception {

		// Mocks
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		Mockito.when( resp.getOutputStream()).thenReturn( Mockito.mock( ServletOutputStream.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getServletPath()).thenReturn( BANNER_IMAGE );

		ServletConfig sc = Mockito.mock( ServletConfig.class );
		ServletContext ctx = Mockito.mock( ServletContext.class );
		Mockito.when( sc.getServletContext()).thenReturn( ctx );

		// Initialization
		WebAdminInterceptionServlet servlet = new WebAdminInterceptionServlet();
		servlet.init( sc );

		File dir = this.folder.newFolder();
		servlet.karafEtc = dir.getAbsolutePath();

		File f = new File( dir, OVERRIDE_IMAGE );
		Utils.writeStringInto( "something", f );
		Assert.assertTrue( f.exists());

		// Execution
		servlet.doGet( req, resp );

		// Assertions
		Mockito.verify( req, Mockito.atLeast( 1 )).getServletPath();
		Mockito.verify( resp, Mockito.only()).getOutputStream();
	}


	@Test
	public void testInterceptionServlet_withKarafEtc_overrideCss_cssIsNotAFile() throws Exception {

		// Mocks
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		Mockito.when( resp.getOutputStream()).thenReturn( Mockito.mock( ServletOutputStream.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getServletPath()).thenReturn( CSS_STYLESHEET );

		ServletConfig sc = Mockito.mock( ServletConfig.class );
		ServletContext ctx = Mockito.mock( ServletContext.class );
		Mockito.when( sc.getServletContext()).thenReturn( ctx );

		// Initialization
		WebAdminInterceptionServlet servlet = new WebAdminInterceptionServlet();
		servlet.init( sc );

		File dir = this.folder.newFolder();
		servlet.karafEtc = dir.getAbsolutePath();

		File f = new File( dir, OVERRIDE_CSS );
		Assert.assertTrue( f.mkdir());

		// Execution
		servlet.doGet( req, resp );

		// Assertions
		Mockito.verify( req, Mockito.atLeast( 1 )).getServletPath();
		Mockito.verify( resp, Mockito.only()).sendError( HttpServletResponse.SC_NOT_FOUND );
	}


	@Test
	public void testInterceptionServlet_withKarafEtc_invalidRequestedFile() throws Exception {

		// Mocks
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );
		Mockito.when( resp.getOutputStream()).thenReturn( Mockito.mock( ServletOutputStream.class ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		Mockito.when( req.getServletPath()).thenReturn( "unexpected" );

		ServletConfig sc = Mockito.mock( ServletConfig.class );
		ServletContext ctx = Mockito.mock( ServletContext.class );
		Mockito.when( sc.getServletContext()).thenReturn( ctx );

		// Initialization
		WebAdminInterceptionServlet servlet = new WebAdminInterceptionServlet();
		servlet.init( sc );

		File dir = this.folder.newFolder();
		servlet.karafEtc = dir.getAbsolutePath();

		// Execution
		servlet.doGet( req, resp );

		// Assertions
		Mockito.verify( req, Mockito.atLeast( 1 )).getServletPath();
		Mockito.verify( resp, Mockito.only()).sendError( HttpServletResponse.SC_FORBIDDEN );
	}


	@Test
	public void testWebAdminResourcesExist() throws Exception {

		// This property is set by Maven
		String mavenTargetDir = System.getProperty( "project.build.directory" );
		if( ! Utils.isEmptyOrWhitespaces( mavenTargetDir )) {
			File webAdminDir = new File( mavenTargetDir, "roboconf-web-administration" );

			Assume.assumeTrue( webAdminDir.exists());
			Assert.assertTrue( new File( webAdminDir, "target/dist" ).exists());
			Assert.assertTrue( new File( webAdminDir, "target/dist" + CSS_STYLESHEET ).exists());
			Assert.assertTrue( new File( webAdminDir, "target/dist" + BANNER_IMAGE ).exists());
		}
	}
}
