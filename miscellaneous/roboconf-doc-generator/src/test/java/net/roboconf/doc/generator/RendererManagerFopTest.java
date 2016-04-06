/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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
import org.apache.fop.apps.FopFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.RenderingManager.Renderer;



/**
 * @author Amadou Diarra - UGA
 */

public class RendererManagerFopTest extends AbstractTestForRendererManager {
	
	
	@Test
	public void testFopRenderer() throws Exception {
		/*this.rm.render( this.outputDir, this.alr.getApplicationTemplate(), this.applicationDirectory, Renderer.FOP, null );
		System.out.println("Bonjour le monde cruel\n");
		System.out.println(this.outputDir);*/
		try {
			File f = TestUtils.findTestFile( "/lamp" );
			//File outputDir = new File( System.getProperty( "user.home" ), "Bureau/html" );
			File outputDir = new File( System.getProperty( "user.home" ), "AGD/fop" );
			Utils.deleteFilesRecursively( outputDir );
			if( ! outputDir.mkdirs())
				throw new IOException( "Could not create the output directory." );

			ApplicationLoadResult alr = RuntimeModelIo.loadApplication( f );
			Map<String,String> options = new HashMap<String,String> ();
			//new RenderingManager().render( outputDir, alr.getApplicationTemplate(), f, Renderer.HTML, options );
			new RenderingManager().render( outputDir, alr.getApplicationTemplate(), f, Renderer.FOP, options );
			File fop = new File( outputDir, "index.fo" );
			File test = new File( System.getProperty( "user.home" ), "Software/fop-2.1/myExample/test.fo" );
			Assert.assertTrue( fop.exists());
			validateFop(test, outputDir);

		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Verifies that a FOP file is valid.
	 * @param fopFile the fop content
	 * @return true if it is valid, false otherwise
	 * @throws Exception
	 */
	private void validateFop( File fopFile, File outputDir ) throws Exception {
		
		File fopConfig = TestUtils.findTestFile( "/fop.xconf" );
		//	System.out.println(fopConfig);
		OutputStream out = new BufferedOutputStream(new FileOutputStream( new File(outputDir,"res.pdf")) );
		FopFactory fopFactory = FopFactory.newInstance(fopConfig);
		FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
		Fop fop =  fopFactory.newFop( MimeConstants.MIME_PDF, foUserAgent, out );
		Source src = new StreamSource( fopFile );
		
		TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(); 
       
        Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);
        //return false;
	}

}
