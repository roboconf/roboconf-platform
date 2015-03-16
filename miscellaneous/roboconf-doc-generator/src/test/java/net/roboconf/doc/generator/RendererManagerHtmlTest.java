/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.doc.generator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.RenderingManager.Renderer;
import nu.validator.messages.MessageEmitter;
import nu.validator.messages.MessageEmitterAdapter;
import nu.validator.messages.TextMessageEmitter;
import nu.validator.servlet.imagereview.ImageCollector;
import nu.validator.source.SourceCode;
import nu.validator.validation.SimpleDocumentValidator;
import nu.validator.xml.SystemErrErrorHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.InputSource;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RendererManagerHtmlTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File applicationDirectory;
	private File outputDir;
	private ApplicationLoadResult alr;
	private RenderingManager rm;


	@Before
	public void before() throws Exception {

		this.applicationDirectory = TestUtils.findTestFile( "/lamp" );
		Assert.assertNotNull( this.applicationDirectory );
		Assert.assertTrue( this.applicationDirectory.exists());

		this.alr = RuntimeModelIo.loadApplication( this.applicationDirectory );
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( this.alr.getLoadErrors()));

		this.outputDir = this.folder.newFolder();
		this.rm = new RenderingManager();
	}


	@Test
	public void checkHtmlValidatorWorks() throws Exception {

		String content = "<html>hop</html>";
		Assert.assertFalse( validateHtml( content ));
	}


	@Test
	public void testSingleHtml_nullOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.rm.render( this.outputDir, this.alr.getApplication(), this.applicationDirectory, Renderer.HTML, null );

		verifySingleHtml();
	}


	@Test
	public void testSingleHtml_emptyOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<String,String> ();
		this.rm.render( this.outputDir, this.alr.getApplication(), this.applicationDirectory, Renderer.HTML, options );

		verifySingleHtml();
	}


	@Test
	public void testSingleHtml_withoutInstances() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.alr.getApplication().getRootInstances().clear();
		this.rm.render( this.outputDir, this.alr.getApplication(), this.applicationDirectory, Renderer.HTML, null );

		verifySingleHtml();
	}


	@Test
	public void testExplodedHtml_customColorInOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<String,String> ();
		options.put( DocConstants.OPTION_IMG_HIGHLIGHT_BG_COLOR, "#dddddd" );
		options.put( DocConstants.OPTION_IMG_BACKGROUND_COLOR, "#4582ad" );
		options.put( DocConstants.OPTION_IMG_FOREGROUND_COLOR, "#000000" );
		options.put( DocConstants.OPTION_HTML_EXPLODED, "boom" );

		this.rm.render( this.outputDir, this.alr.getApplication(), this.applicationDirectory, Renderer.HTML, options );
		verifyExplodedHtml();
	}


	/**
	 * Verifies assertions for the single HTML mode.
	 */
	private void verifySingleHtml() throws Exception {

		File f = new File( this.outputDir, "roboconf.jpg" );
		Assert.assertTrue( f.exists());

		f = new File( this.outputDir, "style.css" );
		Assert.assertTrue( f.exists());

		f = new File( this.outputDir, "png" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "roboconf.html" );
		Assert.assertTrue( f.exists());

		Assert.assertEquals( 4, this.outputDir.listFiles().length );
		verifyHtml( f );
	}


	/**
	 * Verifies assertions for the exploded HTML mode.
	 */
	private void verifyExplodedHtml() throws Exception {

		File f = new File( this.outputDir, "roboconf.jpg" );
		Assert.assertTrue( f.exists());

		f = new File( this.outputDir, "style.css" );
		Assert.assertTrue( f.exists());

		f = new File( this.outputDir, "png" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "components" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "instances" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "roboconf.html" );
		Assert.assertTrue( f.exists());

		Assert.assertEquals( 6, this.outputDir.listFiles().length );
		verifyHtml( f );

		for( Component c : ComponentHelpers.findAllComponents( this.alr.getApplication())) {
			f = new File( this.outputDir, DocConstants.SECTION_COMPONENTS + c.getName() + ".html" );
			Assert.assertTrue( f.getAbsolutePath(), f.exists());
			verifyHtml( f );
		}

		for( Instance i : this.alr.getApplication().getRootInstances()) {
			f = new File( this.outputDir, DocConstants.SECTION_INSTANCES + i.getName() + ".html" );
			Assert.assertTrue( f.getAbsolutePath(), f.exists());
			verifyHtml( f );
		}
	}


	/**
	 * Verifies a HTML file.
	 * @param f
	 */
	private void verifyHtml( File f ) throws Exception {

		String content = Utils.readFileContent( f );
		String loweredContent = content.toLowerCase();
		Assert.assertFalse( loweredContent.contains( " null " ));
		Assert.assertFalse( loweredContent.contains( ">null" ));
		Assert.assertFalse( loweredContent.contains( "null<" ));

		Assert.assertTrue( "Invalid HTML file: ", validateHtml( content ));
	}


	/**
	 * Verifies that a HTML content is valid.
	 * @param content the HTML content
	 * @return true if it is valid, false otherwise
	 * @throws Exception
	 */
	private boolean validateHtml( String content ) throws Exception {

		InputStream in = new ByteArrayInputStream( content.getBytes( "UTF-8" ));
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		SourceCode sourceCode = new SourceCode();
		ImageCollector imageCollector = new ImageCollector(sourceCode);
		boolean showSource = false;
		MessageEmitter emitter = new TextMessageEmitter( out, false );
		MessageEmitterAdapter errorHandler = new MessageEmitterAdapter( sourceCode, showSource, imageCollector, 0, false, emitter );
		errorHandler.setErrorsOnly( true );

		SimpleDocumentValidator validator = new SimpleDocumentValidator();
		validator.setUpMainSchema( "http://s.validator.nu/html5-rdfalite.rnc", new SystemErrErrorHandler());
		validator.setUpValidatorAndParsers( errorHandler, true, false );
		validator.checkHtmlInputSource( new InputSource( in ));

		return 0 == errorHandler.getErrors();
	}
}
