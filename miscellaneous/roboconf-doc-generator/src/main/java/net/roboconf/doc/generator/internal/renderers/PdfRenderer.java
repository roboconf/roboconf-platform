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

package net.roboconf.doc.generator.internal.renderers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;

import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.RenderingManager;
import net.roboconf.doc.generator.RenderingManager.Renderer;
import net.roboconf.doc.generator.internal.AbstractStructuredRenderer;

/**
 * A renderer that outputs pdf.
 * @author Amadou Diarra - UGA
 */
public class PdfRenderer extends AbstractStructuredRenderer {

	/**
	 * Constructor.
	 * @param outputDirectory
	 * @param applicationTemplate
	 * @param applicationDirectory
	 * @param typeAnnotations
	 */
	public PdfRenderer(
			File outputDirectory,
			ApplicationTemplate applicationTemplate,
			File applicationDirectory,
			Map<String,String> typeAnnotations ) {

		super( outputDirectory, applicationTemplate, applicationDirectory, typeAnnotations );
	}


	@Override
	protected File writeFileContent( String fileContent ) throws IOException {

		// Generate FOP rendering
		new RenderingManager().render(
				this.outputDirectory,
				this.applicationTemplate,
				this.applicationDirectory,
				Renderer.FOP,
				this.options,
				this.typeAnnotations );

		File index_fo = new File( this.outputDirectory, "index.fo" );
		File index_pdf = new File( this.outputDirectory, "index.pdf" );

		// Copy the FOP configuration file in outputDirectory
		InputStream conf = getClass().getResourceAsStream( "/fop.xconf" );
		File fopConfig = new File( this.outputDirectory, "fop.xconf" );
		Utils.copyStream( conf, fopConfig );

		// Generate the PDF rendering
		OutputStream out = null;
		try {
			out = new BufferedOutputStream( new FileOutputStream(index_pdf) );
			FopFactory fopFactory = FopFactory.newInstance( fopConfig );
			Fop fop =  fopFactory.newFop( "application/pdf", out );
			Source src = new StreamSource( index_fo );

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();

			Result res = new SAXResult(fop.getDefaultHandler());
			transformer.transform(src, res);

		} catch ( Exception e) {
			throw new IOException( e );

		} finally {
			Utils.closeQuietly ( out );
		}

		return index_pdf;
	}

	@Override
	protected String renderTitle1(String title) {
		return "";
	}

	@Override
	protected String renderTitle2(String title) {
		return "";
	}

	@Override
	protected String renderTitle3(String title) {
		return "";
	}

	@Override
	protected String renderParagraph(String paragraph) {
		return "";
	}

	@Override
	protected String renderList(Collection<String> listItems) {
		return "";
	}

	@Override
	protected String renderPageBreak() {
		return "";
	}

	@Override
	protected String indent() {
		return "";
	}

	@Override
	protected String startTable() {
		return "";
	}

	@Override
	protected String endTable() {
		return "";
	}

	@Override
	protected String addTableHeader(String... headerEntries) {
		return "";
	}

	@Override
	protected String addTableLine(String... lineEntries) {
		return "";
	}

	@Override
	protected String renderDocumentTitle() {
		return "";
	}

	@Override
	protected String renderDocumentIndex() {
		return "";
	}

	@Override
	protected String renderImage(String componentName, DiagramType type, String relativeImagePath) {
		return "";
	}

	@Override
	protected String applyBoldStyle(String text, String keyword) {
		return "";
	}

	@Override
	protected String applyLink(String text, String linkId) {
		return "";
	}

	@Override
	protected StringBuilder startSection(String sectionName) {
		return new StringBuilder();
	}

	@Override
	protected StringBuilder endSection(String sectionName, StringBuilder sb) {
		return new StringBuilder();
	}

	@Override
	protected String renderSections(List<String> sectionNames) {
		return "";
	}
}
