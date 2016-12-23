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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;

/**
 * @author Vincent Zurczak - Linagora
 */
public class KibanaExtensionServletTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testDoGet_404() throws Exception {

		Manager manager = new Manager();
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );

		Mockito.when( req.getRequestURI()).thenReturn( "/roboconf-web-extension/kibana/invalid-app" );
		Mockito.when( req.getServletPath()).thenReturn( "/roboconf-web-extension/kibana" );

		KibanaExtensionServlet servlet = new KibanaExtensionServlet( manager );
		servlet.doGet( req, resp );
		Mockito.verify( resp ).sendError( HttpServletResponse.SC_NOT_FOUND );
	}


	@Test
	public void testDoGet_invalidPath() throws Exception {

		Manager manager = new Manager();
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );

		Mockito.when( req.getRequestURI()).thenReturn( "/roboconf-web-extension/kibana2" );
		Mockito.when( req.getServletPath()).thenReturn( "/roboconf-web-extension/kibana" );

		KibanaExtensionServlet servlet = new KibanaExtensionServlet( manager );
		servlet.doGet( req, resp );
		Mockito.verify( resp ).sendError( HttpServletResponse.SC_NOT_FOUND );
	}


	@Test
	public void testDoGet_baseRequest() throws Exception {

		Manager manager = new Manager();
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		TestManagerWrapper wrapper = new TestManagerWrapper( manager );

		TestApplication app = new TestApplication();
		wrapper.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );

		Mockito.when( req.getRequestURI()).thenReturn( "/roboconf-web-extension/kibana" );
		Mockito.when( req.getServletPath()).thenReturn( "/roboconf-web-extension/kibana" );

		TestServletOutputStream wrappedOs = new TestServletOutputStream();
		Mockito.when( resp.getOutputStream()).thenReturn( wrappedOs );

		KibanaExtensionServlet servlet = new KibanaExtensionServlet( manager );
		servlet.doGet( req, resp );

		Mockito.verify( resp ).getOutputStream();
		String s = wrappedOs.os.toString( "UTF-8" );
		Assert.assertTrue( s.contains( "\t<th>Agents List</th>\n" ));
	}


	@Test
	public void testDoGet_baseRequestWithSlash() throws Exception {

		Manager manager = new Manager();
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );

		Mockito.when( req.getRequestURI()).thenReturn( "/roboconf-web-extension/kibana/" );
		Mockito.when( req.getServletPath()).thenReturn( "/roboconf-web-extension/kibana" );

		TestServletOutputStream wrappedOs = new TestServletOutputStream();
		Mockito.when( resp.getOutputStream()).thenReturn( wrappedOs );

		KibanaExtensionServlet servlet = new KibanaExtensionServlet( manager );
		servlet.doGet( req, resp );

		Mockito.verify( resp ).getOutputStream();
		String s = wrappedOs.os.toString( "UTF-8" );
		Assert.assertTrue( s.contains( "\t<th>Agents List</th>\n" ));
	}


	@Test
	public void testDoGet_requestForApplication() throws Exception {

		Manager manager = new Manager();
		manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		TestManagerWrapper wrapper = new TestManagerWrapper( manager );

		TestApplication app = new TestApplication();
		wrapper.getNameToManagedApplication().put( app.getName(), new ManagedApplication( app ));

		HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse resp = Mockito.mock( HttpServletResponse.class );

		Mockito.when( req.getRequestURI()).thenReturn( "/roboconf-web-extension/kibana/" + app.getName());
		Mockito.when( req.getServletPath()).thenReturn( "/roboconf-web-extension/kibana" );

		TestServletOutputStream wrappedOs = new TestServletOutputStream();
		Mockito.when( resp.getOutputStream()).thenReturn( wrappedOs );

		KibanaExtensionServlet servlet = new KibanaExtensionServlet( manager );
		servlet.doGet( req, resp );

		Mockito.verify( resp ).getOutputStream();
		String s = wrappedOs.os.toString( "UTF-8" );
		Assert.assertFalse( s.contains( "\t<th>Agents List</th>\n" ));
		Assert.assertTrue( s.contains( "\t<th>Associated Dashboard</th>\n" ));
	}


	@Test
	public void testInvalidXmlIsFound() throws Exception {

		try {
			validateHtml( "<a>boo" );
			Assert.fail( "This should not be considered as valid HTML." );

		} catch( Exception e ) {
			// validate
		}

		// This is considered as valid
		validateHtml( "boo" );
		validateHtml( "<a>boo</a>" );
		validateHtml( "<p>boo<br /></p>" );
		validateHtml( "<p>boo<br /></p> ok" );
	}


	@Test
	public void testApplicationsTable() throws Exception {

		List<Application> apps = new ArrayList<> ();
		String s = KibanaExtensionServlet.applicationsTable( apps, "http://something" );
		validateHtml( s );

		apps.add( new TestApplication());
		s = KibanaExtensionServlet.applicationsTable( apps, "http://something" );
		validateHtml( s );
	}


	@Test
	public void testAgentsTable() throws Exception {

		TestApplication app = new TestApplication();
		List<String> instancePaths = new ArrayList<> ();
		String s = KibanaExtensionServlet.agentsTable( app, instancePaths, "http://something" );
		validateHtml( s );

		instancePaths.add( InstanceHelpers.computeInstancePath( app.getMySqlVm()));
		s = KibanaExtensionServlet.agentsTable( app, instancePaths, "http://something" );
		validateHtml( s );
	}


	private void validateHtml( String html )
	throws SAXException, IOException, ParserConfigurationException {

		final String wrappedHtml = "<root>" + html + "</root>";
		final DocumentBuilderFactory newFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder documentBuilder = newFactory.newDocumentBuilder();
		Document document = documentBuilder.parse( new InputSource( new StringReader( wrappedHtml )));
		Assert.assertNotNull( document );
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class TestServletOutputStream extends ServletOutputStream {
		private final ByteArrayOutputStream os = new ByteArrayOutputStream();

		@Override
		public void write( int arg0 ) throws IOException {
			this.os.write( arg0 );
		}
	}
}
