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

package net.roboconf.doc.generator.internal.renderers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.doc.generator.internal.AbstractStructuredRenderer;

/**
 * A renderer that outputs Fop.
 * @author Amadou Diarra - UGA
 */
public class FopRenderer extends AbstractStructuredRenderer {

	/**
	 * Constructor.
	 * @param outputDirectory
	 * @param applicationTemplate
	 * @param applicationDirectory
	 */
	public FopRenderer(File outputDirectory, ApplicationTemplate applicationTemplate, File applicationDirectory) {
		super(outputDirectory, applicationTemplate, applicationDirectory);
	}

	@Override
	protected String renderTitle1(String title) {
		return "<axf:document-info name=\"document-title\" value=\""+ title + "\" />\n";
	}

	@Override
	protected String renderTitle2(String title) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String renderTitle3(String title) {
		// TODO Auto-generated method stub
		return null;
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
		sb.append( "\n<list-block>\n" );
		for( String s : listItems ) {
			sb.append( "\t<list-item-label>" );
			sb.append( "</>\n" );
			sb.append( "\t<list-item-body>" );
			sb.append( s );
			sb.append( "</>\n" );
		}

		sb.append( "</list-block>\n\n" );
		return sb.toString();
	}

	@Override
	protected String renderPageBreak() {

		return "<fo:block break-after=\"page\"/>";
	}

	@Override
	protected String indent() {

		return "";
	}

	@Override
	protected String startTable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String endTable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String addTableHeader(String... headerEntries) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String addTableLine(String... lineEntries) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String renderDocumentTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String renderDocumentIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String renderImage(String componentName, DiagramType type, String relativeImagePath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String applyBoldStyle(String text, String keyword) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String applyLink(String text, String linkId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected File writeFileContent(String fileContent) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StringBuilder startSection(String sectionName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StringBuilder endSection(String sectionName, StringBuilder sb) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String renderSections(List<String> sectionNames) {
		// TODO Auto-generated method stub
		return null;
	}

}
