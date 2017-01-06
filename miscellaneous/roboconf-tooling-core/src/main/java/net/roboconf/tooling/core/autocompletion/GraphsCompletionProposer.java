/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.tooling.core.autocompletion;

import static net.roboconf.core.dsl.ParsingConstants.KEYWORD_FACET;
import static net.roboconf.core.dsl.ParsingConstants.KEYWORD_IMPORT;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_COMPONENT_FACETS;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_COMPONENT_IMPORTS;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_COMPONENT_INSTALLER;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_GRAPH_CHILDREN;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_GRAPH_EXTENDS;
import static net.roboconf.tooling.core.TextUtils.isLineBreak;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.basicProposal;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.buildProposalsFromMap;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.findAllExportedVariables;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.findTypeNames;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.startsWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.SelectionRange;
import net.roboconf.tooling.core.TextUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class GraphsCompletionProposer implements ICompletionProposer {

	static final String FACET_BLOCK = "New facet block";
	static final String COMPONENT_BLOCK = "New component block";

	static final String FACET_PREFIX = KEYWORD_FACET + " ";
	static final String IMPORT_PREFIX = KEYWORD_IMPORT + " ";

	static final String[] KNOWN_INSTALLERS = { "script", "puppet", "logger" };
	static final String[] COMPONENT_PROPERTY_NAMES = {
			ParsingConstants.PROPERTY_GRAPH_CHILDREN,
			ParsingConstants.PROPERTY_GRAPH_EXPORTS,
			ParsingConstants.PROPERTY_GRAPH_EXTENDS,
			ParsingConstants.PROPERTY_COMPONENT_FACETS,
			ParsingConstants.PROPERTY_COMPONENT_IMPORTS,
			ParsingConstants.PROPERTY_COMPONENT_INSTALLER
	};

	static final String[] FACET_PROPERTY_NAMES = {
			ParsingConstants.PROPERTY_GRAPH_CHILDREN,
			ParsingConstants.PROPERTY_GRAPH_EXPORTS,
			ParsingConstants.PROPERTY_GRAPH_EXTENDS
	};


	private final File appDirectory;
	private final File editedFile;



	/**
	 * Constructor.
	 * @param appDirectory
	 * @param editedFile
	 */
	public GraphsCompletionProposer( File appDirectory, File editedFile ) {
		this.appDirectory = appDirectory;
		this.editedFile = editedFile;
	}


	@Override
	public List<RoboconfCompletionProposal> findProposals( String text ) {

		// Find the text to insert
		List<RoboconfCompletionProposal> proposals = new ArrayList<> ();
		Ctx ctx = findContext( text );

		switch( ctx.kind ) {
		case NEUTRAL:

			// Import
			if( startsWith( IMPORT_PREFIX, ctx.property )) {
				// "import" or a part of this word
				if( ! ctx.property.matches( "(?i)" + KEYWORD_IMPORT + "\\s+" ))
					proposals.add( basicProposal( IMPORT_PREFIX, ctx.property, true ));

				// "import "
				else for( String graphImport : CompletionUtils.findGraphFilesToImport( this.appDirectory, this.editedFile, text )) {
					if( startsWith( graphImport, ctx.lastWord ))
						proposals.add( basicProposal( graphImport, "", false ));
				}
			}

			// From here, there cannot be any word before the offset.
			if( ! Utils.isEmptyOrWhitespaces( ctx.lastWord ))
				break;

			// Facet
			ctx.property = ctx.property.trim();
			if( startsWith( FACET_PREFIX, ctx.property )) {

				// Basic proposal: facet
				proposals.add( basicProposal( FACET_PREFIX, ctx.property, true ));

				// More complex proposal: a full block
				String proposalString = FACET_PREFIX + "name {\n\t\n}";
				RoboconfCompletionProposal proposal = new RoboconfCompletionProposal(
						proposalString,
						FACET_BLOCK,
						FACET_BLOCK + "\n\n" + proposalString.trim(),
						ctx.property.length());

				proposal.getSelection().add( new SelectionRange( 6, 4 ));
				proposals.add( proposal );
			}

			// Component
			if( Utils.isEmptyOrWhitespaces( ctx.property )) {

				// More complex proposal: a full block
				String proposalString = "name {\n\t\n}";
				RoboconfCompletionProposal proposal = new RoboconfCompletionProposal(
						proposalString,
						COMPONENT_BLOCK,
						COMPONENT_BLOCK + "\n\n" + proposalString.trim(),
						ctx.property.length());

				proposal.getSelection().add( new SelectionRange( 0, 4 ));
				proposals.add( proposal );
			}

			break;

		case ATTRIBUTE:
			String[] arr = ctx.facet ? FACET_PROPERTY_NAMES : COMPONENT_PROPERTY_NAMES;
			for( String s : arr ) {
				s += ": ";
				if( startsWith( s, ctx.lastWord ))
					proposals.add( basicProposal( s, ctx.lastWord ));
			}

			break;

		case PROPERTY:
			ctx.property = ctx.property.trim();
			Map<String,String> candidates = findPropertyCandidates( ctx );
			proposals.addAll( buildProposalsFromMap( candidates, ctx.lastWord ));
			break;

		default:
			break;
		}

		return proposals;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static enum CtxKind {
		NEUTRAL, 		// Default: beginning of the document or between two types
		COMMENT,		// We are inside a comment
		NOTHING,		// Nothing special here
		ATTRIBUTE,		// At the beginning or right after a colon or an opening curly bracket
		PROPERTY;		// We are defining a property
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class Ctx {

		CtxKind kind = CtxKind.NEUTRAL;
		String lastWord, property;
		boolean facet = false;
	}


	/**
	 * Finds the context from the given text.
	 * @param text
	 * @return a context
	 */
	private Ctx findContext( String text ) {

		// Are we inside a comment?
		Ctx ctx = new Ctx();
		int n;
		for( n = text.length() - 1; n >= 0; n-- ) {
			char c = text.charAt( n );
			if( isLineBreak( c ))
				break;

			if( c == '#' ) {
				ctx.kind = CtxKind.COMMENT;
				return ctx;
			}
		}

		// Simplify the search: remove all the comments.
		text = TextUtils.removeComments( text );

		// Keep on simplifying the search: remove complete components and facets
		// Since instances can contain other instances (recursivity), we must
		// apply the pattern several times, until no more replacement is possible.
		int before = -1;
		int after = -2;
		while( before != after ) {
			before = text.length();
			text = text.replaceAll( "(?i)(?s)(facet\\s+)?[^{]*\\{[^{}]*\\}\r?\n", "" );
			after = text.length();
		}

		// Remove white spaces at the beginning of the string.
		text = text.replaceAll( "^(\n|\r\n)+", "" );

		// Now, find our context.
		// We go back until we find a '{'. Then, we keep rewinding
		// until we find a line break.
		String lastWord = null;
		String lastLine = null;
		StringBuilder sb = new StringBuilder();
		boolean bracketFound = false;

		for( n = text.length() - 1; n >= 0; n-- ) {
			char c = text.charAt( n );

			// White space? We have a our last word.
			if( Character.isWhitespace( c ) && lastWord == null ) {
				lastWord = sb.toString();
				sb.setLength( 0 );
				sb.append( c );
			}

			// Same thing after a curly bracket
			else if( c == '{' )
				bracketFound = true;

			// If we find a closing bracket, then we are at the end of a declaration
			else if( c == '}' ) {
				ctx.kind = CtxKind.NOTHING;
				break;
			}

			// Line break? That depends...
			else if( isLineBreak( c )) {
				if( bracketFound )
					break;

				if( lastLine == null ) {
					lastLine = sb.toString();
					sb.setLength( 0 );
				}
			}

			else {
				sb.insert( 0, c );
			}
		}

		// Update the context
		if( lastLine == null )
			lastLine = sb.toString();

		ctx.lastWord = lastWord == null ? "" : lastWord;
		ctx.property = lastLine;

		// Time to analyze
		if( ctx.property.trim().endsWith( ":" )
				|| ctx.property.trim().endsWith( "," )) {
			ctx.kind = CtxKind.PROPERTY;

		} else if( Utils.isEmptyOrWhitespaces( ctx.property )) {
			if( bracketFound
					&& ! Utils.isEmptyOrWhitespaces( sb.toString()))
				ctx.kind = CtxKind.ATTRIBUTE;
		}

		if( sb.toString().matches( "(?i)\\s*" + KEYWORD_FACET + "\\s+.*" ))
			ctx.facet = true;

		return ctx;
	}


	/**
	 * @param ctx the current context
	 * @return a non-null map of candidate values for this property
	 * <p>
	 * Key = value to insert, value = description.
	 * </p>
	 */
	private Map<String,String> findPropertyCandidates( Ctx ctx ) {

		Map<String,String> result = new TreeMap<> ();
		if( ! ctx.facet && ctx.property.matches( PROPERTY_COMPONENT_INSTALLER + "\\s*:\\s*" )) {
			for( String installer : KNOWN_INSTALLERS )
				result.put( installer, null );

		} else if( ctx.property.matches( PROPERTY_GRAPH_CHILDREN + "\\s*:\\s*.*" )) {
			result.putAll( findTypeNames( this.appDirectory, true, true ));

		} else if( ctx.property.matches( PROPERTY_GRAPH_EXTENDS + "\\s*:\\s*.*" )) {
			result.putAll( findTypeNames( this.appDirectory, ctx.facet, ! ctx.facet ));

		} else if( ! ctx.facet && ctx.property.matches( PROPERTY_COMPONENT_FACETS + "\\s*:\\s*.*" )) {
			result.putAll( findTypeNames( this.appDirectory, true, false ));

		} else if( ! ctx.facet && ctx.property.matches( PROPERTY_COMPONENT_IMPORTS + "\\s*:\\s*.*" )) {
			result.putAll( findAllExportedVariables( this.appDirectory ));
		}

		return result;
	}
}
