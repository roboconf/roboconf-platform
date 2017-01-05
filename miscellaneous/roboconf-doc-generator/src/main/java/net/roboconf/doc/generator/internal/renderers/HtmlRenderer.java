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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.DocConstants;
import net.roboconf.doc.generator.internal.AbstractStructuredRenderer;

/**
 * A renderer that outputs HTML files.
 * @author Vincent Zurczak - Linagora
 */
public class HtmlRenderer extends AbstractStructuredRenderer {

	private static final String TITLE_MARKUP = "${TITLE}";
	private static final String MENU_MARKUP = "${MENU}";
	private static final String CONTENT_MARKUP = "${CONTENT}";
	private static final String CSS_MARKUP = "${CSS}";

	private String menu;
	private final Map<String,StringBuilder> sectionNameToContent = new HashMap<> ();


	/**
	 * Constructor.
	 * @param outputDirectory
	 * @param applicationTemplate
	 * @param applicationDirectory
	 * @param typeAnnotations
	 */
	public HtmlRenderer(
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
		return "<h1 id=\"" + createId( title ) + "\">" + title + "</h1>\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderTitle2(java.lang.String)
	 */
	@Override
	protected String renderTitle2( String title ) {
		return "<h2 id=\"" + createId( title ) + "\">" + title + "</h2>\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderTitle3(java.lang.String)
	 */
	@Override
	protected String renderTitle3( String title ) {
		return "<h3>" + title + "</h3>\n";
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
			sb.append( "\n<p>" );
			sb.append( s.trim().replaceAll( "\n", "<br />" ));
			sb.append( "</p>\n\n" );
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
		sb.append( "\n<ul>\n" );
		for( String s : listItems ) {
			sb.append( "\t<li>" );
			sb.append( s );
			sb.append( "</li>\n" );
		}

		sb.append( "</ul>\n\n" );
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

		StringBuilder result = sb;
		if( this.options.containsKey( DocConstants.OPTION_HTML_EXPLODED )) {
			this.sectionNameToContent.put( sectionName, sb );
			result = new StringBuilder();

		} else {
			result.append( "<p class=\"separator\"> &nbsp; </p>\n" );
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderSections(java.util.List)
	 */
	@Override
	protected String renderSections( List<String> sectionNames ) {

		StringBuilder sb = new StringBuilder();
		if( this.options.containsKey( DocConstants.OPTION_HTML_EXPLODED )) {
			if( sectionNames.size() > 0 ) {
				sb.append( "<ul>\n" );
				for( String sectionName : sectionNames ) {

					int index = sectionName.lastIndexOf( '/' );
					String title = sectionName.substring( index + 1 );

					sb.append( "<li><a href=\"" );
					sb.append( sectionName.replace( " ", "%20" ));
					sb.append( ".html\">" );
					sb.append( title );
					sb.append( "</a></li>\n" );
				}

				sb.append( "</ul>\n" );
			}

		} else {
			sb.append( "\n<p class=\"separator\"> &nbsp; </p>\n" );
		}

		return sb.toString();
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
		return "<img src=\"" + relativeImagePath + "\" alt=\"" + alt + "\" />\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderDocumentTitle()
	 */
	@Override
	protected String renderDocumentTitle() {
		return "<h1 id=\"" + this.messages.get( "introduction" ) + "\">" + this.applicationTemplate.getName() + "</h1>\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #renderDocumentIndex()
	 */
	@Override
	protected String renderDocumentIndex() {

		// What keys should we inject in the index?
		List<String> keys = new ArrayList<> ();
		keys.add( "introduction" );
		keys.add( "components" );
		if( this.options.containsKey( DocConstants.OPTION_RECIPE )) {
			if( ! this.applicationTemplate.getGraphs().getFacetNameToFacet().isEmpty())
				keys.add( "facets" );

		} else {
			keys.add( "instances" );
		}

		// Create the index
		StringBuilder sb = new StringBuilder();
		sb.append( "<ul>\n" );
		for( String key : keys ) {
			sb.append( "\t<li><a href=\"index.html#" );
			sb.append( this.messages.get( key ).toLowerCase());
			sb.append( "\">" );
			sb.append( this.messages.get( key ));
			sb.append( "</a></li>\n" );
		}

		sb.append( "</ul>\n" );
		this.menu = sb.toString();
		return "";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #applyBoldStyle(java.lang.String, java.lang.String)
	 */
	@Override
	protected String applyBoldStyle( String text, String keyword ) {
		return text.replaceAll( Pattern.quote( keyword ), "<b>" + keyword + "</b>" );
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
			link = "components/" + linkId + ".html".replace( " ", "%20" );
		else
			link = "#" + createId( linkId );

		return text.replaceAll( Pattern.quote( text ), "<a href=\"" + link + "\">" + text + "</a>" );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #startTable()
	 */
	@Override
	protected String startTable() {
		return "<table>\n";
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.AbstractStructuredRenderer
	 * #endTable()
	 */
	@Override
	protected String endTable() {
		return "</table>\n";
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

		// Load the template
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = getClass().getResourceAsStream( "/html.tpl" );
		Utils.copyStreamSafely( in, out );

		// Create the target directory
		File targetFile = new File( this.outputDirectory, "index.html" );
		Utils.createDirectory( targetFile.getParentFile());

		// Deal with the CSS file
		final String css;
		final String cssReference = this.options.get( DocConstants.OPTION_HTML_CSS_REFERENCE );
		if( cssReference != null ) {
			css = cssReference.trim();

		} else {
			css = "style.css";
			writeCssFile();
		}

		// Write sections
		for( Map.Entry<String,StringBuilder> entry : this.sectionNameToContent.entrySet()) {

			int index = entry.getKey().lastIndexOf( '/' );
			String title = entry.getKey().substring( index + 1 );
			String toWrite = out.toString( "UTF-8" )
					.replace( TITLE_MARKUP, title )
					.replace( CONTENT_MARKUP, entry.getValue().toString())
					.replace( MENU_MARKUP, this.menu )
					.replace( "href=\"", "href=\"../" )
					.replace( "src=\"", "src=\"../" )
					.replaceAll( "\n{3,}", "\n\n" );

			if( cssReference != null )
				toWrite = toWrite.replace( "../" + CSS_MARKUP, css );
			else
				toWrite = toWrite.replace( CSS_MARKUP, css );

			File sectionFile = new File( this.outputDirectory, entry.getKey() + ".html" );
			Utils.createDirectory( sectionFile.getParentFile());
			Utils.writeStringInto( toWrite, sectionFile );
		}

		// Write the main file
		String toWrite = out.toString( "UTF-8" )
				.replace( TITLE_MARKUP, this.applicationTemplate.getName())
				.replace( CSS_MARKUP, css )
				.replace( CONTENT_MARKUP, fileContent )
				.replace( MENU_MARKUP, this.menu )
				.replaceAll( "\n{3,}", "\n\n" );

		Utils.writeStringInto( toWrite, targetFile );

		// And the header image
		String imagePath = this.options.get( DocConstants.OPTION_HTML_HEADER_IMAGE_FILE );
		try {
			File sourceFile = null;
			if( imagePath != null )
				sourceFile = new File( imagePath );

			if( sourceFile != null && sourceFile.exists())
				in = new FileInputStream( sourceFile );
			else
				in = getClass().getResourceAsStream( "/roboconf.jpg" );

			File imgFile = new File( this.outputDirectory, "header.jpg" );
			Utils.copyStream( in, imgFile );

		} finally {
			Utils.closeQuietly( in );
		}

		return targetFile;
	}


	private String createId( String title ) {
		return title.toLowerCase().replaceAll( "\\s+", "-" );
	}


	private void writeCssFile() throws IOException {

		InputStream in = null;
		String location = this.options.get( DocConstants.OPTION_HTML_CSS_FILE );
		try {
			if( ! Utils.isEmptyOrWhitespaces( location ))
				in = new FileInputStream( new File( location ));
			else
				in = getClass().getResourceAsStream( "/style.css" );

			File cssFile = new File( this.outputDirectory, "style.css" );
			Utils.copyStream( in, cssFile );

		} finally {
			Utils.closeQuietly( in );
		}
	}
}
