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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.DocConstants;
import net.roboconf.doc.generator.internal.AbstractStructuredRenderer;

/**
 * A renderer that outputs Fop.
 * @author Amadou Diarra - UGA
 */
public class FopRenderer extends AbstractStructuredRenderer {

	private static final String TITLE_MARKUP = "${TITLE}";
	private static final String CONTENT_MARKUP = "${CONTENT}";


	/**
	 * Constructor.
	 * @param outputDirectory
	 * @param applicationTemplate
	 * @param applicationDirectory
	 * @param typeAnnotations
	 */
	public FopRenderer(
			File outputDirectory,
			ApplicationTemplate applicationTemplate,
			File applicationDirectory,
			Map<String,String> typeAnnotations ) {

		super( outputDirectory, applicationTemplate, applicationDirectory, typeAnnotations );
	}


	@Override
	protected String renderTitle1(String title) {
		return "<fo:block font-size=\"18pt\" font-weight=\"bold\">" + title + "</fo:block>\n";
	}


	@Override
	protected String renderTitle2(String title) {
		return "<fo:block font-size=\"14pt\" font-weight=\"bold\">" + title + "</fo:block>\n";
	}


	@Override
	protected String renderTitle3(String title) {
		return "<fo:block font-size=\"12pt\" font-weight=\"bold\">" + title + "</fo:block>\n";
	}


	@Override
	protected String renderParagraph(String paragraph) {

		StringBuilder sb = new StringBuilder();
		for( String s : paragraph.trim().split( "\n\n" )) {
			sb.append( "\n<fo:block>" );
			sb.append( s.trim().replaceAll( "\n", "<br />" ));
			sb.append( "</fo:block>\n\n" );
		}

		return sb.toString();
	}


	@Override
	protected String renderList(Collection<String> listItems) {

		StringBuilder sb = new StringBuilder();
		sb.append( "\n<fo:list-block provisional-label-separation=\"2pt\"" );
		sb.append( " provisional-distance-between-starts=\"8pt\">\n" );
		for( String s : listItems ) {
			sb.append( "\t<fo:list-item>\n" );
			sb.append( "\t\t<fo:list-item-label end-indent=\"label-end()\">\n" );
			sb.append( "\t\t  <fo:block> - </fo:block>\n" );
			sb.append( "\t\t</fo:list-item-label>\n" );
			sb.append( "\t\t<fo:list-item-body start-indent=\"body-start()\">\n" );
			sb.append( "\t\t  <fo:block>" );
			sb.append( s );
			sb.append( "</fo:block>\n" );
			sb.append( "\t\t</fo:list-item-body>\n" );
			sb.append( "\t</fo:list-item>\n");
		}

		sb.append( "</fo:list-block>\n" );
		return sb.toString();
	}


	@Override
	protected String renderPageBreak() {
		return "<fo:block break-after=\"page\"/>";
	}


	@Override
	protected String indent() {
		return "\t";
	}


	@Override
	protected String startTable() {
		return "\n<fo:table text-align=\"center\" space-before=\"1em\" table-layout=\"fixed\" border=\"0.5pt solid black\">\n";
	}


	@Override
	protected String endTable() {
		return "</fo:table>\n";
	}


	@Override
	protected String addTableHeader(String... headerEntries) {

		StringBuilder sb = new StringBuilder();
		sb.append( "<fo:table-body>\n" );
		sb.append( "\t<fo:table-row>\n" );
		for( String s : headerEntries ) {
			sb.append( "\t  <fo:table-cell border=\"0.5pt solid black\">" );
			sb.append( "<fo:block>" );
			sb.append( s );
			sb.append( "</fo:block>" );
			sb.append( "</fo:table-cell>\n" );
		}

		sb.append( "\t</fo:table-row>\n" );
		sb.append( "</fo:table-body>\n" );
		return sb.toString();
	}


	@Override
	protected String addTableLine(String... lineEntries) {

		StringBuilder sb = new StringBuilder();
		sb.append( "\t<fo:table-body>\n" );
		sb.append( "\t\t<fo:table-row>\n" );
		for( String s : lineEntries ) {
			sb.append( "\t\t  <fo:table-cell border=\"0.5pt solid black\" padding=\"2pt 5pt\">" );
			sb.append( "<fo:block>" );
			sb.append( s );
			sb.append( "</fo:block>" );
			sb.append( "\n</fo:table-cell>" );
		}

		sb.append( "\n\t\t</fo:table-row>\n" );
		sb.append( "\t</fo:table-body>\n" );
		return sb.toString();
	}


	@Override
	protected String renderDocumentTitle() {
		return "<fo:block font-size=\"18pt\" font-weight=\"bold\">" + this.applicationTemplate.getName() + "</fo:block>\n";
	}


	@Override
	protected String renderDocumentIndex() {

		// What keys should we inject in the index?
		List<String> keys = new ArrayList<> ();
		keys.add( "introduction" );
		keys.add( "components" );
		if( this.options.containsKey( DocConstants.OPTION_RECIPE ) ) {
			if( ! this.applicationTemplate.getGraphs().getFacetNameToFacet().isEmpty() )
				keys.add( "facets" );

		} else {
			keys.add( "instances" );
		}

		// Create the index
		StringBuilder sb = new StringBuilder();
		sb.append( "\n<fo:list-block provisional-label-separation=\"2pt\"" );
		sb.append( " provisional-distance-between-starts=\"8pt\">\n" );
		for( String key : keys ) {
			sb.append( "\t<fo:list-item>\n" );
			sb.append( "\t\t<fo:list-item-label end-indent=\"label-end()\">\n" );
			sb.append( "\t\t  <fo:block> - </fo:block>\n" );
			sb.append( "\t\t</fo:list-item-label>\n" );
			sb.append( "\t\t<fo:list-item-body start-indent=\"body-start()\">\n" );
			sb.append( "\t\t  <fo:block>" );
			sb.append( this.messages.get( key ));
			sb.append( "</fo:block>\n" );
			sb.append( "\t\t</fo:list-item-body>\n" );
			sb.append( "\t</fo:list-item>\n");
		}

		sb.append( "</fo:list-block>\n" );
		return sb.toString();
	}


	@Override
	protected String renderImage(String componentName, DiagramType type, String relativeImagePath) {

		StringBuilder sb = new StringBuilder();
		sb.append( "<fo:block>\n" );
		sb.append( "\t<fo:external-graphic src=\"" + relativeImagePath + "\" />\n" );
		sb.append( "</fo:block>" );
		return sb.toString();
	}


	@Override
	protected String applyBoldStyle(String text, String keyword) {
		return text.replaceAll( Pattern.quote( keyword ), "<fo:block font-weight=\"bold\">" + keyword + "</fo:block>" );
	}


	@Override
	protected String applyLink(String text, String linkId) {
		return text.replaceAll( Pattern.quote( text ), "<fo:basic-link external-destination=\"" + "url(" + createId( linkId ) + ")" +"\">" + text + "</fo:basic-link>" );
	}


	@Override
	protected File writeFileContent(String fileContent) throws IOException {

		// Load the template
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = getClass().getResourceAsStream( "/fop.tpl" );
		Utils.copyStreamSafely( in, out );

		//Copy the header in outputDirectory
		InputStream h = getClass().getResourceAsStream( "/roboconf.jpg" );
		File imgFile = new File( this.outputDirectory, "header.jpg" );
		Utils.copyStream( h, imgFile );

		// Create the target directory
		File targetFile = new File( this.outputDirectory, "index.fo" );
		Utils.createDirectory( targetFile.getParentFile());

		// Write the main file
		String toWrite = out.toString( "UTF-8" )
				.replace( TITLE_MARKUP, this.applicationTemplate.getName())
				.replace( CONTENT_MARKUP, fileContent )
				.replace( "header.jpg", this.outputDirectory.getAbsolutePath() + "/header.jpg" )
				.replace( "png/", this.outputDirectory.getAbsolutePath() + "/png/" )
				.replaceAll( "\n{3,}", "\n\n" );

		Utils.writeStringInto( toWrite, targetFile );
		return targetFile;
	}


	@Override
	protected StringBuilder endSection(String sectionName, StringBuilder sb) {
		StringBuilder result = sb;
		return result;
	}


	@Override
	protected String renderSections(List<String> sectionNames) {
		return "";
	}


	@Override
	protected StringBuilder startSection(String sectionName) {
		return new StringBuilder();
	}

	private String createId( String title ) {
		return title.toLowerCase().replaceAll( "\\s+", "-" );
	}
}
