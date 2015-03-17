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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.RenderingManager.Renderer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RendererManagerMarkdownTest {

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
	public void testMarkdown_nullOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.rm.render( this.outputDir, this.alr.getApplication(), this.applicationDirectory, Renderer.MARKDOWN, null );

		verifyMarkdown();
	}


	@Test
	public void testMarkdown_emptyOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		Map<String,String> options = new HashMap<String,String> ();
		this.rm.render( this.outputDir, this.alr.getApplication(), this.applicationDirectory, Renderer.MARKDOWN, options );

		verifyMarkdown();
	}


	@Test
	public void testMarkdown_withoutInstances_customColorInOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.alr.getApplication().getRootInstances().clear();

		Map<String,String> options = new HashMap<String,String> ();
		options.put( DocConstants.OPTION_IMG_HIGHLIGHT_BG_COLOR, "#dddddd" );
		options.put( DocConstants.OPTION_IMG_BACKGROUND_COLOR, "#4582ad" );
		options.put( DocConstants.OPTION_IMG_FOREGROUND_COLOR, "#000000" );
		options.put( DocConstants.OPTION_HTML_EXPLODED, "boom" );

		this.rm.render( this.outputDir, this.alr.getApplication(), this.applicationDirectory, Renderer.MARKDOWN, options );

		verifyMarkdown();
	}


	/**
	 * Verifies assertions about markdown files.
	 */
	private void verifyMarkdown() throws Exception {

		File f = new File( this.outputDir, "png" );
		Assert.assertTrue( f.isDirectory());

		f = new File( this.outputDir, "roboconf.md" );
		Assert.assertTrue( f.exists());

		String s = Utils.readFileContent( f ).toLowerCase();
		Assert.assertFalse( s.contains( " null " ));
		Assert.assertFalse( s.contains( ">null " ));
		Assert.assertFalse( s.contains( " null<" ));
		Assert.assertFalse( s.contains( "*null " ));
		Assert.assertFalse( s.contains( " null*" ));
	}
}
