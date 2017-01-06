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

package net.roboconf.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.DocConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class GenerateDocumentationMojoTest extends AbstractTest {

	@Test
	public void testSingleHtml() throws Exception {

		File dir = findAndExecuteMojo( Arrays.asList( "html" ), null, null );
		Assert.assertEquals( 1, dir.listFiles().length );

		File html = new File( dir, "html" );
		Assert.assertTrue( html.exists());

		Assert.assertEquals( 4, html.listFiles().length );
		Assert.assertTrue( new File( html, "index.html" ).isFile());
		Assert.assertTrue( new File( html, "header.jpg" ).isFile());
		Assert.assertTrue( new File( html, "style.css" ).isFile());
		Assert.assertTrue( new File( html, "png" ).isDirectory());
	}


	@Test
	public void testSingleHtmlWithLocale() throws Exception {

		File dir = findAndExecuteMojo( Arrays.asList( "html" ), Arrays.asList( "fr_FR" ), null );
		Assert.assertEquals( 1, dir.listFiles().length );

		File html = new File( dir, "html_fr_FR" );
		Assert.assertTrue( html.exists());

		Assert.assertEquals( 4, html.listFiles().length );
		Assert.assertTrue( new File( html, "index.html" ).isFile());
		Assert.assertTrue( new File( html, "header.jpg" ).isFile());
		Assert.assertTrue( new File( html, "style.css" ).isFile());
		Assert.assertTrue( new File( html, "png" ).isDirectory());
	}


	@Test
	public void testSeveralRenderers() throws Exception {

		File dir = findAndExecuteMojo( Arrays.asList( "html", "markdown", "unknown" ), new ArrayList<String>( 0 ), null );
		Assert.assertEquals( 3, dir.listFiles().length );

		File html = new File( dir, "html" );
		Assert.assertTrue( html.exists());

		Assert.assertEquals( 3, html.listFiles().length );
		Assert.assertTrue( new File( html, "index.html" ).isFile());
		Assert.assertTrue( new File( html, "header.jpg" ).isFile());
		Assert.assertTrue( new File( html, "style.css" ).isFile());

		File md = new File( dir, "markdown" );
		Assert.assertTrue( md.exists());

		Assert.assertEquals( 1, md.listFiles().length );
		Assert.assertTrue( new File( md, "index.md" ).isFile());

		Assert.assertTrue( new File( dir, "png" ).isDirectory());
	}


	@Test
	public void testSeveralRenderersAndLocalesWithOptions() throws Exception {

		List<String> locales = Arrays.asList( "fr_FR", "en_US" );
		Map<String,String> options = new HashMap<> ();
		options.put( DocConstants.OPTION_IMG_BACKGROUND_COLOR, "#dddddd" );
		options.put( "foreground.color", "#000000" );

		File dir = findAndExecuteMojo( Arrays.asList( "html", "markdown", "unknown" ), locales, options );
		Assert.assertEquals( 5, dir.listFiles().length );
		Assert.assertTrue( new File( dir, "png" ).isDirectory());

		for( String locale : locales ) {
			File html = new File( dir, "html_" + locale );
			Assert.assertTrue( locale, html.exists());

			Assert.assertEquals( locale, 3, html.listFiles().length );
			Assert.assertTrue( new File( html, "index.html" ).isFile());
			Assert.assertTrue( new File( html, "header.jpg" ).isFile());
			Assert.assertTrue( new File( html, "style.css" ).isFile());

			File md = new File( dir, "markdown_" + locale );
			Assert.assertTrue( locale, md.exists());

			Assert.assertEquals( locale, 1, md.listFiles().length );
			Assert.assertTrue( locale, new File( md, "index.md" ).isFile());
		}
	}


	private File findAndExecuteMojo( List<String> renderers, List<String> locales, Map<String,String> options ) throws Exception {

		// Find the mojo.
		GenerateDocumentationMojo docMojo = (GenerateDocumentationMojo) super.findMojo( "project--valid", "documentation" );
		MavenProject project = (MavenProject) this.rule.getVariableValueFromObject( docMojo, "project" );
		Assert.assertNotNull( project );

		this.rule.setVariableValueToObject( docMojo, "locales", locales );
		this.rule.setVariableValueToObject( docMojo, "renderers", renderers );
		this.rule.setVariableValueToObject( docMojo, "options", options );

		// Copy the resources
		Utils.copyDirectory(
				new File( project.getBasedir(), MavenPluginConstants.SOURCE_MODEL_DIRECTORY ),
				new File( project.getBuild().getOutputDirectory()));

		// Initial check
		File docDirectory = new File( project.getBasedir(), MavenPluginConstants.TARGET_DOC_DIRECTORY );
		Assert.assertFalse( docDirectory.exists());

		// Execute the mojo
		docMojo.execute();

		// Should we have a result file?
		Assert.assertTrue( docDirectory.exists());
		return docDirectory;
	}
}
