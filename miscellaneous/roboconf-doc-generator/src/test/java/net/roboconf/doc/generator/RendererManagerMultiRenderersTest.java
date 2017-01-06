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
import org.junit.Test;

import net.roboconf.doc.generator.RenderingManager.Renderer;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RendererManagerMultiRenderersTest extends AbstractTestForRendererManager {

	@Test
	public void testMulti_nullOptions() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		List<String> renderers = new ArrayList<> ();
		renderers.add( Renderer.HTML.toString());
		renderers.add( Renderer.MARKDOWN.toString());

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, renderers, null, null );
		Assert.assertEquals( 3, this.outputDir.listFiles().length );

		File f = new File( this.outputDir, Renderer.HTML.toString().toLowerCase());
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		f = new File( this.outputDir, Renderer.MARKDOWN.toString().toLowerCase());
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		Assert.assertTrue( new File( this.outputDir, "png" ).exists());
	}


	@Test
	public void testMultiButOnlyOneRenderer() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		List<String> renderers = new ArrayList<> ();
		renderers.add( Renderer.MARKDOWN.toString());

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, renderers, null, null );
		Assert.assertEquals( 1, this.outputDir.listFiles().length );

		File f = new File( this.outputDir, Renderer.MARKDOWN.toString().toLowerCase());
		Assert.assertTrue( f.exists());

		Assert.assertTrue( new File( f, "png" ).exists());
		Assert.assertFalse( new File( this.outputDir, "png" ).exists());
	}


	@Test
	public void testMulti_invalidRendererInside() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		List<String> renderers = new ArrayList<> ();
		renderers.add( Renderer.HTML.toString());
		renderers.add( "oops" );

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, renderers, null, null );
		Assert.assertEquals( 2, this.outputDir.listFiles().length );

		File f = new File( this.outputDir, Renderer.HTML.toString().toLowerCase());
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		Assert.assertTrue( new File( this.outputDir, "png" ).exists());
	}


	@Test
	public void testMulti_withLocale() throws Exception {

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		List<String> renderers = new ArrayList<> ();
		renderers.add( Renderer.HTML.toString());
		renderers.add( Renderer.MARKDOWN.toString());

		Map<String,String> options = new HashMap<> ();
		options.put( DocConstants.OPTION_LOCALE, "fr_FR" );

		Assert.assertEquals( 0, this.outputDir.listFiles().length );
		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, renderers, options, null );
		Assert.assertEquals( 3, this.outputDir.listFiles().length );

		File f = new File( this.outputDir, Renderer.HTML.toString().toLowerCase() + "_fr_FR" );
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		f = new File( this.outputDir, Renderer.MARKDOWN.toString().toLowerCase() + "_fr_FR" );
		Assert.assertTrue( f.exists());
		Assert.assertFalse( new File( f, "png" ).exists());

		Assert.assertTrue( new File( this.outputDir, "png" ).exists());
	}
}
