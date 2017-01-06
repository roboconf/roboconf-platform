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

import static com.codeborne.pdftest.PDF.containsText;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.codeborne.pdftest.PDF;

import net.roboconf.doc.generator.RenderingManager.Renderer;

/**
 * @author Amadou Diarra - UGA
 */
public class RendererManagerPdfTest extends AbstractTestForRendererManager {

	@Test
	public void testFopRenderer() throws Exception {

		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.FOP, null, null );
		File fofile = new File(this.outputDir,"index.fo");
		Assert.assertTrue( fofile.exists() );
		Assert.assertTrue( fofile.length() > 0 );
	}


	@Test
	public void testPdfRenderer() throws IOException {

		Locale locale = Locale.getDefault();
		try {
			Locale.setDefault(new Locale.Builder().setLanguage("en").setRegion("US").build());

			this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.PDF, null, null );
			File pdffile = new File(this.outputDir,"index.pdf");
			Assert.assertTrue( pdffile.exists() );
			Assert.assertTrue( pdffile.length() > 0 );

			PDF genPdf = new PDF( pdffile );
			Assert.assertThat( genPdf, containsText("Legacy LAMP") );
			Assert.assertThat( genPdf, containsText("This document lists the Software components used in this application") );

		} finally {
			Locale.setDefault(locale);
		}
	}
}
