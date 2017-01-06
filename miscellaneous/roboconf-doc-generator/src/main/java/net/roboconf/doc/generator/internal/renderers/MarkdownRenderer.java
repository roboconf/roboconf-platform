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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.DocConstants;
import net.roboconf.doc.generator.internal.AbstractStructuredRenderer;

/**
 * A renderer that outputs markdown.
 * @author Vincent Zurczak - Linagora
 */
public class MarkdownRenderer extends AbstractStructuredRenderer {

	/**
	 * Constructor.
	 * @param outputDirectory
	 * @param applicationTemplate
	 * @param applicationDirectory
	 * @param typeAnnotations
	 */
	public MarkdownRenderer(
			File outputDirectory,
			ApplicationTemplate applicationTemplate,
			File applicationDirectory,
			Map<String,String> typeAnnotations ) {

		super( outputDirectory, applicationTemplate, applicationDirectory, typeAnnotations );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderTitle1(java.lang.String)
	 */
	@Override
	protected String renderTitle1( String title ) {
		return "\n# " + title + "\n\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderTitle2(java.lang.String)
	 */
	@Override
	protected String renderTitle2( String title ) {
		return "\n## " + title + "\n\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderTitle3(java.lang.String)
	 */
	@Override
	protected String renderTitle3( String title ) {
		return "\n### " + title + "\n\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderParagraph(java.lang.String)
	 */
	@Override
	protected String renderParagraph( String paragraph ) {

		StringBuilder sb = new StringBuilder();
		for( String s : paragraph.trim().split( "\n\n" )) {
			sb.append( s.trim().replaceAll( "\n", "  \n" ));
			sb.append( "\n" );
		}

		return sb.toString();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderList(java.util.Collection)
	 */
	@Override
	protected String renderList( Collection<String> listItems ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "\n" );
		for( String s : listItems ) {
			sb.append( "* " );
			sb.append( s );
			sb.append( "\n" );
		}

		sb.append( "\n" );
		return sb.toString();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #startSection(java.lang.String)
	 */
	@Override
	protected StringBuilder startSection( String sectionName ) {
		return new StringBuilder();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #endSection(java.lang.String, java.lang.StringBuilder)
	 */
	@Override
	protected StringBuilder endSection( String sectionName, StringBuilder sb ) {
		sb.append( "<br />\n" );
		return sb;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderSections(java.util.List)
	 */
	@Override
	protected String renderSections( List<String> sectionNames ) {
		return "";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderPageBreak()
	 */
	@Override
	protected String renderPageBreak() {
		return "";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderImage(java.lang.String, net.roboconf.doc.generator.internal.AbstractStructuredRenderer.DiagramType, java.lang.String)
	 */
	@Override
	protected String renderImage( String componentName, DiagramType type, String relativeImagePath ) {
		String alt = componentName + " - " + type;
		return "![" + alt + "](" + relativeImagePath + " \"" + componentName + "\")\n\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderDocumentTitle()
	 */
	@Override
	protected String renderDocumentTitle() {
		return "# " + this.applicationTemplate.getName() + "\n\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderDocumentIndex()
	 */
	@Override
	protected String renderDocumentIndex() {
		return "";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #applyBoldStyle(java.lang.String, java.lang.String)
	 */
	@Override
	protected String applyBoldStyle( String text, String keyword ) {
		return text.replaceAll( Pattern.quote( keyword ), "**" + keyword + "**" );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #applyLink(java.lang.String, java.lang.String)
	 */
	@Override
	protected String applyLink( String text, String linkId ) {

		String link;
		if( this.options.containsKey( DocConstants.OPTION_HTML_EXPLODED ))
			link = "components/" + linkId + ".md".replace( " ", "%20" );
		else
			link = "#" + linkId;

		return text.replaceAll( Pattern.quote( text ), "[" + text + "](" + link + ")" );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #startTable()
	 */
	@Override
	protected String startTable() {
		return "\n<table>\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #endTable()
	 */
	@Override
	protected String endTable() {
		return "</table>\n\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #addTableHeader(java.util.List)
	 */
	@Override
	protected String addTableHeader( String... headerEntries ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "<tr>\n" );
		for( String s : headerEntries ) {
			sb.append( "\t<th>" );
			sb.append( s );
			sb.append( "</th>\n" );
		}

		sb.append( "</tr>\n" );
		return sb.toString();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #addTableLine(java.util.List)
	 */
	@Override
	protected String addTableLine( String... lineEntries ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "<tr>\n" );
		for( String s : lineEntries ) {
			sb.append( "\t<td>" );
			sb.append( s );
			sb.append( "</td>\n" );
		}

		sb.append( "</tr>\n" );
		return sb.toString();
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #indent()
	 */
	@Override
	protected String indent() {
		return " &nbsp; &nbsp; &nbsp; ";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #writeFileContent(java.lang.String)
	 */
	@Override
	protected File writeFileContent( String fileContent ) throws IOException {

		File targetFile = new File( this.outputDirectory, "index.md" );
		Utils.createDirectory( targetFile.getParentFile());
		Utils.writeStringInto( fileContent.replaceAll( "\n{3,}", "\n\n" ), targetFile );

		return targetFile;
	}
}
