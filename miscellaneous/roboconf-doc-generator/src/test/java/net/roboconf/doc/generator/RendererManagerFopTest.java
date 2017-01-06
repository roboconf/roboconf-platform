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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.RenderingManager.Renderer;
import net.roboconf.doc.generator.internal.renderers.FopRenderer;

/**
 * @author Amadou Diarra - UGA
 */
public class RendererManagerFopTest extends AbstractTestForRendererManager {


	@Test
	public void testFopRenderer() throws Exception {

		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.FOP, null, null );
		File fofile = new File(this.outputDir,"index.fo");
		Assert.assertTrue( fofile.exists() );
		Assert.assertTrue( fofile.length() > 0 );

	}

	@Test
	public void testPdfRenderer() throws Exception {
		this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.FOP, null, null );
		File fofile = new File(this.outputDir,"index.fo");
		File pdffile = new File(this.outputDir,"index.pdf");
		Assert.assertTrue( validateFop2pdf(fofile, pdffile) );

	}

	/**
	 * Verifies that a FOP file is valid and transform it into pdf file.
	 * @param fopFile the fop content
	 * @param pdffile the output
	 * @return true if it is valid, false otherwise
	 * @throws Exception
	 */
	private boolean validateFop2pdf( File fopFile, File pdffile ) throws Exception {

		InputStream conf = FopRenderer.class.getResourceAsStream( "/fop.xconf" );
		File fopConfig = new File( this.outputDir, "fop.xconf" );
		Utils.copyStream( conf, fopConfig );

		OutputStream out = null;
		try {
				out = new BufferedOutputStream(new FileOutputStream(pdffile));
				FopFactory fopFactory = FopFactory.newInstance(fopConfig);
				Fop fop =  fopFactory.newFop("application/pdf", out);
				Source src = new StreamSource( fopFile );

				TransformerFactory factory = TransformerFactory.newInstance();
				Transformer transformer = factory.newTransformer();

				Result res = new SAXResult(fop.getDefaultHandler());
				transformer.transform(src, res);

		} finally {
				Utils.closeQuietly ( out );
		}

		return pdffile.length() > 0;

	}
}
