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

package net.roboconf.doc.generator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.RenderingManager.Renderer;
import nu.validator.messages.MessageEmitter;
import nu.validator.messages.MessageEmitterAdapter;
import nu.validator.messages.TextMessageEmitter;
import nu.validator.servlet.imagereview.ImageCollector;
import nu.validator.source.SourceCode;
import nu.validator.validation.SimpleDocumentValidator;
import nu.validator.xml.SystemErrErrorHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RendererManagerHtmlTest extends AbstractTestForRendererManager {

	@Test
	public void checkHtmlValidatorWorks() throws Exception {

		String content = "<html>hop</html>";
		Assert.assertFalse( validateHtml( content ));
	}


	@Test
	public void testSingleHtml_nullOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.HTML, null, null );

		verifySingleHtml( true );
	}


	@Test
	public void testSingleHtml_emptyOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<> ();
		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.HTML, options, null );

		verifySingleHtml( true );
	}


	@Test
	public void testSingleHtml_withoutInstances() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.alr.getApplicationTemplate().getRootInstances().clear();
		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.HTML, null, null );

		verifySingleHtml( true );
	}


	@Test
	public void testSingleHtml_withReferencedCss() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<> ();
		options.put(  DocConstants.OPTION_HTML_CSS_REFERENCE, "http://hop.css" );

		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.HTML, options, null );

		String content = verifySingleHtml( false );
		Assert.assertFalse( content.contains( "style.css" ));
		Assert.assertTrue( content.contains( "href=\"http://hop.css\"" ));
	}


	@Test
	public void testSingleHtml_withCustomCss() throws Exception {

		final String cssContent = "* { margin: 0; }";
		File tempCssFile = this.folder.newFile();
		Utils.writeStringInto( cssContent, tempCssFile );

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<> ();
		options.put(  DocConstants.OPTION_HTML_CSS_FILE, tempCssFile.getAbsolutePath());

		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.HTML, options, null );

		String content = verifySingleHtml( true );
		Assert.assertTrue( content.contains( "href=\"style.css\"" ));

		File cssFile = new File( this.outputDir, "style.css" );
		Assert.assertTrue( cssFile.exists());
		String readContent = Utils.readFileContent( cssFile );
		Assert.assertEquals( cssContent, readContent );
	}


	@Test
	public void testExplodedHtml_withReferencedCss() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<> ();
		options.put(  DocConstants.OPTION_HTML_CSS_REFERENCE, "http://hop.css" );
		options.put( DocConstants.OPTION_HTML_EXPLODED, "boom" );
		options.put( DocConstants.OPTION_HTML_HEADER_IMAGE_FILE, "inexisting-will-be-replaced-by-default" );

		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.HTML, options, null );

		List<String> contents = verifyExplodedHtml( false );
		for( int i=0; i<contents.size(); i++ ) {
			String content = contents.get( i );
			Assert.assertFalse( "Index " + i, content.contains( "style.css" ));
			Assert.assertTrue( "Index " + i, content.contains( "href=\"http://hop.css\"" ));
		}
	}


	@Test
	public void testExplodedHtml_withCustomCssAndHeader() throws Exception {

		final String cssContent = "* { padding: 0; }";
		File tempCssFile = this.folder.newFile();
		Utils.writeStringInto( cssContent, tempCssFile );

		File roboconfImage = TestUtils.findTestFile( "/roboconf.jpg" );
		File targetImage = this.folder.newFile();
		Utils.copyStream( roboconfImage, targetImage );
		Assert.assertTrue( targetImage.exists());

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<> ();
		options.put(  DocConstants.OPTION_HTML_CSS_FILE, tempCssFile.getAbsolutePath());
		options.put( DocConstants.OPTION_HTML_EXPLODED, "boom" );
		options.put( DocConstants.OPTION_HTML_HEADER_IMAGE_FILE, targetImage.getAbsolutePath());

		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.HTML, options, null );

		List<String> contents = verifyExplodedHtml( true );
		int dotLevel = 0;
		int twoDotsLevel = 0;
		for( int i=0; i<contents.size(); i++ ) {
			String content = contents.get( i );
			if( content.contains( "href=\"style.css\"" ))
				dotLevel ++;
			else if( content.contains( "href=\"../style.css\"" ))
				twoDotsLevel ++;
		}

		Assert.assertEquals( "Only index.html should contain href=\"style.css\"", 1, dotLevel );
		Assert.assertEquals( "All the sections should contain href=\"../style.css\"", contents.size() - 1, twoDotsLevel );

		File cssFile = new File( this.outputDir, "style.css" );
		Assert.assertTrue( cssFile.exists());
		String readContent = Utils.readFileContent( cssFile );
		Assert.assertEquals( cssContent, readContent );
	}


	@Test
	public void testExplodedHtml_customColorInOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<> ();
		options.put( DocConstants.OPTION_IMG_HIGHLIGHT_BG_COLOR, "#dddddd" );
		options.put( DocConstants.OPTION_IMG_BACKGROUND_COLOR, "#4582ad" );
		options.put( DocConstants.OPTION_IMG_FOREGROUND_COLOR, "#000000" );
		options.put( DocConstants.OPTION_HTML_EXPLODED, "boom" );

		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.HTML, options, null );
		verifyExplodedHtml( true );
	}


	/**
	 * Verifies assertions for the single HTML mode.
	 * @param hasStyleCss true if the output should contain a style.css file
	 * @return the file content (for additional checks)
	 */
	private String verifySingleHtml( boolean hasStyleCss ) throws Exception {

		File f = new File( this.outputDir, "header.jpg" );
		Assert.assertTrue( f.exists());

		if( hasStyleCss ) {
			f = new File( this.outputDir, "style.css" );
			Assert.assertTrue( f.exists());
		}

		f = new File( this.outputDir, "png" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "index.html" );
		Assert.assertTrue( f.exists());

		int fileCount = hasStyleCss ? 4 : 3;
		Assert.assertEquals( fileCount, this.outputDir.listFiles().length );
		return verifyHtml( f );
	}


	/**
	 * Verifies assertions for the exploded HTML mode.
	 * @param hasStyleCss true if the output should contain a style.css file
	 * @return the generated files' content (for additional checks)
	 */
	private List<String> verifyExplodedHtml( boolean hasStyleCss ) throws Exception {
		List<String> result = new ArrayList<> ();

		File f = new File( this.outputDir, "header.jpg" );
		Assert.assertTrue( f.exists());

		if( hasStyleCss ) {
			f = new File( this.outputDir, "style.css" );
			Assert.assertTrue( f.exists());
		}

		f = new File( this.outputDir, "png" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "components" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "instances" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "index.html" );
		Assert.assertTrue( f.exists());

		int fileCount = hasStyleCss ? 6 : 5;
		Assert.assertEquals( fileCount, this.outputDir.listFiles().length );
		result.add( verifyHtml( f ));

		for( Component c : ComponentHelpers.findAllComponents( this.alr.getApplicationTemplate())) {
			f = new File( this.outputDir, DocConstants.SECTION_COMPONENTS + c.getName() + ".html" );
			Assert.assertTrue( f.getAbsolutePath(), f.exists());
			result.add( verifyHtml( f ));
		}

		for( Instance i : this.alr.getApplicationTemplate().getRootInstances()) {
			f = new File( this.outputDir, DocConstants.SECTION_INSTANCES + i.getName() + ".html" );
			Assert.assertTrue( f.getAbsolutePath(), f.exists());
			result.add( verifyHtml( f ));
		}

		return result;
	}


	/**
	 * Verifies a HTML file.
	 * @param f
	 * @return the file content (for additional checks)
	 */
	private String verifyHtml( File f ) throws Exception {

		String content = Utils.readFileContent( f );
		verifyContent( content );
		Assert.assertTrue( "Invalid HTML file: ", validateHtml( content ));

		return content;
	}


	/**
	 * Verifies that a HTML content is valid.
	 * @param content the HTML content
	 * @return true if it is valid, false otherwise
	 * @throws Exception
	 */
	private boolean validateHtml( String content ) throws Exception {

		InputStream in = new ByteArrayInputStream( content.getBytes( StandardCharsets.UTF_8 ));
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
