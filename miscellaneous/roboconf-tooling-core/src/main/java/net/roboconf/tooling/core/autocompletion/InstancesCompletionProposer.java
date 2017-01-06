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

import static net.roboconf.core.dsl.ParsingConstants.KEYWORD_IMPORT;
import static net.roboconf.core.dsl.ParsingConstants.KEYWORD_INSTANCE_OF;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_INSTANCE_CHANNELS;
import static net.roboconf.core.dsl.ParsingConstants.PROPERTY_INSTANCE_NAME;
import static net.roboconf.tooling.core.TextUtils.isLineBreak;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.basicProposal;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.buildProposalsFromMap;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.findAllTypes;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.resolveStringDescription;
import static net.roboconf.tooling.core.autocompletion.CompletionUtils.startsWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.SelectionRange;
import net.roboconf.tooling.core.TextUtils;
import net.roboconf.tooling.core.autocompletion.CompletionUtils.RoboconfTypeBean;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstancesCompletionProposer implements ICompletionProposer {

	static final String COMPONENT_NAME = "component";
	static final String IMPORT_PREFIX = KEYWORD_IMPORT + " ";
	static final String INSTANCE_OF_PREFIX = KEYWORD_INSTANCE_OF + " ";
	static final String INSTANCE_OF_BLOCK = "New instance block";

	private String errorMsg;
	private final File appDirectory;
	private final File editedFile;



	/**
	 * Constructor.
	 * @param appDirectory
	 * @param editedFile
	 */
	public InstancesCompletionProposer( File appDirectory, File editedFile ) {
		this.appDirectory = appDirectory;
		this.editedFile = editedFile;
	}


	/**
	 * @return a message indicating a potential error met while computing proposals
	 */
	public String getErrorMsg() {
		return this.errorMsg;
	}


	@Override
	public List<RoboconfCompletionProposal> findProposals( String text ) {

		// Find the text to insert
		List<RoboconfCompletionProposal> proposals = new ArrayList<> ();
		Ctx ctx = findContext( text );

		boolean addImport = true;
		switch( ctx.kind ) {
		case IMPORT:
			for( String instanceImport : CompletionUtils.findInstancesFilesToImport( this.appDirectory, this.editedFile, text )) {
				if( startsWith( instanceImport, ctx.lastWord ))
					proposals.add( basicProposal( instanceImport, "", false ));
			}

			break;

		case ATTRIBUTE:
			addImport = false;
			Map<String,String> candidates = findExportedVariableNames( ctx );
			proposals.addAll( buildProposalsFromMap( candidates, ctx.lastWord ));

			// No break statement!

		case NEUTRAL:
			// Import
			if( addImport && startsWith( IMPORT_PREFIX, ctx.lastWord ))
				proposals.add( basicProposal( IMPORT_PREFIX, ctx.lastWord, true ));

			// Instances
			if( startsWith( INSTANCE_OF_PREFIX, ctx.lastWord )) {

				// Basic proposal: instance of
				proposals.add( basicProposal( INSTANCE_OF_PREFIX, ctx.lastWord, true ));

				// More complex proposal: a full block
				StringBuilder sb = new StringBuilder( INSTANCE_OF_PREFIX );
				sb.append( "component" );
				sb.append( " {\n\t" );
				sb.append( ctx.parentIndentation );

				sb.append( PROPERTY_INSTANCE_NAME );
				sb.append( ": name;\n" );

				sb.append( ctx.parentIndentation );
				sb.append( "}" );

				String proposalString = sb.toString();
				SelectionRange sel1 = new SelectionRange( KEYWORD_INSTANCE_OF.length() + 1, COMPONENT_NAME.length());
				SelectionRange sel2 = new SelectionRange(
						sel1.getOffset() + sel1.getLength() + PROPERTY_INSTANCE_NAME.length() + 6 + ctx.parentIndentation.length(),
						4 );

				RoboconfCompletionProposal proposal = new RoboconfCompletionProposal(
						proposalString,
						INSTANCE_OF_BLOCK,
						INSTANCE_OF_BLOCK + "\n\n" + proposalString.trim(),
						ctx.lastWord.length());

				proposal.getSelection().add( sel1 );
				proposal.getSelection().add( sel2 );
				proposals.add( proposal );
			}

			break;

		case COMPONENT_NAME:
			candidates = findComponentNames( ctx );
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
		NEUTRAL, 			// Default: beginning of the document or within an instance
		COMMENT,			// We are inside a comment
		COMPONENT_NAME, 	// Right after "instance of"
		ATTRIBUTE, 			// At the beginning or right after a colon or an opening curly bracket
		ATTRIBUTE_VALUE,	// After a colon
		IMPORT,				// If we are in a file import
		NOTHING;			// Eliminate some cases
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class Ctx {

		CtxKind kind = CtxKind.NEUTRAL;
		String lastWord, parentInstanceType = "", parentIndentation = "";
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

		// Keep on simplifying the search: remove complete instances
		// Since instances can contain other instances (recursivity), we must
		// apply the pattern several times, until no more replacement is possible.
		int before = -1;
		int after = -2;
		while( before != after ) {
			before = text.length();
			text = text.replaceAll( "(?i)(?s)instance\\s+of[^{]*\\{[^{}]*\\}", "" );
			after = text.length();
		}

		// Remove white spaces at the beginning of the string.
		text = text.replaceAll( "^(\n|\r\n)+", "" );

		// Now, find our context.
		String lastWord = null, lastLine = null;
		StringBuilder sb = new StringBuilder();

		for( n = text.length() - 1; n >= 0 && ctx.kind == CtxKind.NEUTRAL; n-- ) {
			char c = text.charAt( n );

			// After a "colon" => we are in a property, forget it...
			if( c == ':' && lastWord != null ) {
				ctx.kind = CtxKind.ATTRIBUTE_VALUE;
				break;
			}

			// White space? We have a our last word.
			if( Character.isWhitespace( c ) && lastWord == null )
				lastWord = sb.toString();

			// Store the last line.
			if( isLineBreak( c ) && lastLine == null )
				lastLine = sb.toString();

			// After a semicolon: we should suggest new attributes
			if( c == ';' )
				ctx.kind = CtxKind.ATTRIBUTE;

			// Same thing after a curly bracket
			else if( c == '{' )
				ctx.kind = CtxKind.ATTRIBUTE;

			else {
				sb.insert( 0, c );
				String cmp = sb.toString();
				if( lastWord != null )
					cmp = cmp.substring( 0, cmp.length() - lastWord.length());

				cmp = cmp.trim().replaceAll( "\\s{2,}", " " );
				if( cmp.equals( KEYWORD_INSTANCE_OF ))
					ctx.kind = CtxKind.COMPONENT_NAME;

				else if( cmp.equals( KEYWORD_IMPORT )
						&& ! sb.toString().matches( ".*" + KEYWORD_IMPORT ))
					ctx.kind = CtxKind.IMPORT;
			}
		}

		// Update the context
		ctx.lastWord = lastWord == null ? sb.toString() : lastWord;

		// If we are in the attribute state, we should verify the last line.
		// Case "instance of C " would otherwise indicate
		if( lastLine == null )
			lastLine = sb.toString();

		if( lastLine.matches( "(?i).*\\binstance\\s+of\\s+\\S+\\s+" )) {
			ctx.kind = CtxKind.NOTHING;
		}

		// Find the parent instance (which is the first declared instance at the end of 'text').
		Pattern p = Pattern.compile( "(?i)([\\t ]*)instance\\s+of\\s+([^{]+)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( text );
		while( m.find()) {
			// instance of t| => 'lastWord == t' and 'm.group( 2 ) == t''
			// instance of => 'lastWord' is not relevant...
			if( ! Objects.equals( ctx.lastWord, m.group( 2 ).trim())) {
				ctx.parentIndentation = m.group( 1 );
				ctx.parentIndentation += "\t";
				ctx.parentInstanceType = m.group( 2 ).trim();
			}
		}

		return ctx;
	}


	/**
	 * @param ctx the current context
	 * @return a non-null list of component names
	 */
	Map<String,String> findComponentNames( Ctx ctx ) {

		Map<String,String> result = new TreeMap<> ();
		if( this.appDirectory != null ) {

			// Ignore parsing errors, propose the most accurate and possible results
			ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( this.appDirectory );
			BLOCK: if( alr.getApplicationTemplate().getGraphs() != null ) {

				// If there is a parent component...
				Component parentComponent = ComponentHelpers.findComponent( alr.getApplicationTemplate(), ctx.parentInstanceType );

				// ... then find out the right potential children.
				Collection<Component> candidates = Collections.emptyList();
				if( parentComponent != null )
					candidates = ComponentHelpers.findAllChildren( parentComponent );
				else if( Utils.isEmptyOrWhitespaces( ctx.parentInstanceType ))
					candidates = alr.getApplicationTemplate().getGraphs().getRootComponents();
				else
					this.errorMsg = "Component " + ctx.parentInstanceType + " does not exist.";

				// Retrieve descriptions from raw graph files
				if( candidates.isEmpty())
					break BLOCK;

				Map<String,RoboconfTypeBean> types = findAllTypes( this.appDirectory );
				for( Component c : candidates ) {
					RoboconfTypeBean type = types.get( c.getName());
					result.put( c.getName(), type == null || type.isFacet() ? null : type.getDescription());
				}

			} else {
				this.errorMsg = "The graph contains errors. It could not be parsed.";
			}
		}

		return result;
	}


	/**
	 * @param ctx the current context
	 * @return a non-null list of variable names
	 */
	Map<String,String> findExportedVariableNames( Ctx ctx ) {

		Map<String,String> result = new TreeMap<> ();
		result.put( PROPERTY_INSTANCE_NAME + ": ", null );
		result.put( PROPERTY_INSTANCE_CHANNELS + ": ", null );

		if( this.appDirectory != null ) {

			// Ignore parsing errors, propose the most accurate and possible results
			ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( this.appDirectory );
			if( alr.getApplicationTemplate().getGraphs() != null ) {

				// If there is a owner component...
				Component ownerComponent = ComponentHelpers.findComponent( alr.getApplicationTemplate(), ctx.parentInstanceType );

				// ... then find out the exported variables that can be overridden.
				if( ownerComponent == null ) {
					this.errorMsg = "Component " + ctx.parentInstanceType + " does not exist.";

				} else for( Map.Entry<String,String> entry : ComponentHelpers.findAllExportedVariables( ownerComponent ).entrySet()) {
					String desc = resolveStringDescription( entry.getKey(), entry.getValue());
					result.put( entry.getKey() + ": ", desc );
				}

			} else {
				this.errorMsg = "The graph contains errors. It could not be parsed.";
			}
		}

		return result;
	}
}
