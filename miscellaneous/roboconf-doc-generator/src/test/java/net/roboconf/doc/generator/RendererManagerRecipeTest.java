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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.RenderingManager.Renderer;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RendererManagerRecipeTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testMulti_withRecipeAsApplication() throws Exception {

		// Load the application
		File applicationDirectory = TestUtils.findTestFile( "/recipe" );
		Assert.assertNotNull( applicationDirectory );
		Assert.assertTrue( applicationDirectory.exists());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( applicationDirectory );
		RoboconfErrorHelpers.filterErrorsForRecipes( alr );
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( alr.getLoadErrors()));

		// Prepare the generation
		File outputDir = this.folder.newFolder();
		Assert.assertEquals( 0, outputDir.listFiles().length );

		List<String> renderers = new ArrayList<> ();
		renderers.add( Renderer.HTML.toString());
		renderers.add( Renderer.MARKDOWN.toString());

		Map<String,String> options = new HashMap<> ();
		options.put( DocConstants.OPTION_LOCALE, "fr_FR" );

		// Generate...
		RenderingManager rm = new RenderingManager();
		Assert.assertEquals( 0, outputDir.listFiles().length );
		rm.render( outputDir, alr.getApplicationTemplate(), applicationDirectory, renderers, options, null );

		// With this example, there is only one component.
		// So, no graph and no generated diagram.
		Assert.assertEquals( 2, outputDir.listFiles().length );

		File f = new File( outputDir, Renderer.HTML.toString().toLowerCase() + "_fr_FR" );
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		String content = Utils.readFileContent( new File( f, "index.html" ));
		Assert.assertFalse( content.toLowerCase().contains( "facettes" ));

		f = new File( outputDir, Renderer.MARKDOWN.toString().toLowerCase() + "_fr_FR" );
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		content = Utils.readFileContent( new File( f, "index.md" ));
		Assert.assertFalse( content.toLowerCase().contains( "facettes" ));
		Assert.assertFalse( content.contains( "=> MySQL database" ));

		Assert.assertFalse( new File( outputDir, "png" ).exists());

		// Test with annotations
		Map<String,String> annotations = new HashMap<> ();
		annotations.put( "MySQL", "=> MySQL database" );
		rm.render( outputDir, alr.getApplicationTemplate(), applicationDirectory, renderers, options, annotations );

		content = Utils.readFileContent( new File( f, "index.md" ));
		Assert.assertFalse( content.toLowerCase().contains( "facettes" ));
		Assert.assertTrue( content.contains( "=> MySQL database" ));
	}


	@Test
	public void testMulti_withRecipeAsRecipe() throws Exception {

		// Load the application
		File applicationDirectory = TestUtils.findTestFile( "/recipe" );
		Assert.assertNotNull( applicationDirectory );
		Assert.assertTrue( applicationDirectory.exists());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( applicationDirectory );
		RoboconfErrorHelpers.filterErrorsForRecipes( alr );
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( alr.getLoadErrors()));

		// Prepare the generation
		File outputDir = this.folder.newFolder();
		Assert.assertEquals( 0, outputDir.listFiles().length );

		List<String> renderers = new ArrayList<> ();
		renderers.add( Renderer.HTML.toString());
		renderers.add( Renderer.MARKDOWN.toString());

		Map<String,String> options = new HashMap<> ();
		options.put( DocConstants.OPTION_LOCALE, "fr_FR" );
		options.put( DocConstants.OPTION_RECIPE, "yes" );

		// Generate...
		RenderingManager rm = new RenderingManager();
		Assert.assertEquals( 0, outputDir.listFiles().length );
		rm.render( outputDir, alr.getApplicationTemplate(), applicationDirectory, renderers, options, null );

		// With this example, there is only one component.
		// So, no graph and no generated diagram.
		Assert.assertEquals( 2, outputDir.listFiles().length );

		File f = new File( outputDir, Renderer.HTML.toString().toLowerCase() + "_fr_FR" );
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		String content = Utils.readFileContent( new File( f, "index.html" ));
		Assert.assertFalse( content.toLowerCase().contains( "facettes" ));

		f = new File( outputDir, Renderer.MARKDOWN.toString().toLowerCase() + "_fr_FR" );
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		content = Utils.readFileContent( new File( f, "index.md" ));
		Assert.assertFalse( content.toLowerCase().contains( "facettes" ));

		Assert.assertFalse( new File( outputDir, "png" ).exists());
	}


	@Test
	public void testMulti_withComplexRecipeAsRecipe() throws Exception {

		// Load the application
		File applicationDirectory = TestUtils.findTestFile( "/recipe2" );
		Assert.assertNotNull( applicationDirectory );
		Assert.assertTrue( applicationDirectory.exists());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( applicationDirectory );
		RoboconfErrorHelpers.filterErrorsForRecipes( alr );
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( alr.getLoadErrors()));

		// Prepare the generation
		File outputDir = this.folder.newFolder();
		Assert.assertEquals( 0, outputDir.listFiles().length );

		List<String> renderers = new ArrayList<> ();
		renderers.add( Renderer.HTML.toString());
		renderers.add( Renderer.MARKDOWN.toString());

		Map<String,String> options = new HashMap<> ();
		options.put( DocConstants.OPTION_LOCALE, "fr_FR" );
		options.put( DocConstants.OPTION_RECIPE, "yes" );

		// Generate...
		RenderingManager rm = new RenderingManager();
		Assert.assertEquals( 0, outputDir.listFiles().length );
		rm.render( outputDir, alr.getApplicationTemplate(), applicationDirectory, renderers, options, null );

		// One component, one facet, everything is fine.
		Assert.assertEquals( 3, outputDir.listFiles().length );

		File f = new File( outputDir, Renderer.HTML.toString().toLowerCase() + "_fr_FR" );
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		String content = Utils.readFileContent( new File( f, "index.html" ));
		Assert.assertTrue( content.toLowerCase().contains( "facettes" ));
		Assert.assertTrue( content.contains( "Peu importe" ));

		f = new File( outputDir, Renderer.MARKDOWN.toString().toLowerCase() + "_fr_FR" );
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		content = Utils.readFileContent( new File( f, "index.md" ));
		Assert.assertTrue( content.toLowerCase().contains( "facettes" ));
		Assert.assertTrue( content.contains( "Peu importe" ));

		Assert.assertTrue( new File( outputDir, "png" ).exists());
	}
}
