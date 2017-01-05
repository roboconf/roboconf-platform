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

package net.roboconf.tooling.core.textactions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.SelectionRange;
import net.roboconf.tooling.core.TextUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CorrectIndentationAction implements ITextAction {

	private static final int LB_INDENT = 1;
	private int newCursorPosition;


	@Override
	public int getNewCursorPosition() {
		return this.newCursorPosition;
	}


	@Override
	public String update( String fullText, int selectionOffset, int selectionLength ) {

		SelectionRange range = TextUtils.findRegionFromSelection( fullText, selectionOffset, selectionLength );

		// Now, split the text and work on the selected lines
		String before = fullText.substring( 0, range.getOffset());
		String after = fullText.substring( range.getOffset() + range.getLength());
		String text = fullText.substring( range.getOffset(), range.getOffset() + range.getLength());

		// Find the previous indentation
		int indent = findPreviousIndentation( before );

		// Update the lines
		boolean splitLine = false;
		StringBuilder sb = new StringBuilder();
		List<String> lines = Arrays.asList( text.split( "\\n" ));

		for( Iterator<String> it = lines.iterator(); it.hasNext(); ) {
			String line = it.next();

			// Pre-fix
			String workLine = TextUtils.removeComments( line ).trim();
			if( workLine.startsWith( "}" ))
				indent --;

			// Add the line
			if( ! Utils.isEmptyOrWhitespaces( line )) {
				for( int i=0; i<indent; i++ )
					sb.append( "\t" );
			}

			sb.append( line.replaceFirst( "^\\s*", "" ));
			if( it.hasNext() || text.endsWith( "\n" ))
				sb.append( "\n" );

			// Post-fix: find the next indentation
			if( workLine.endsWith( "{" )) {
				indent ++;

			} else if( ! splitLine && workLine.endsWith( "\\" )) {
				indent += LB_INDENT;
				splitLine = true;

			} else if( splitLine && workLine.endsWith( ";" )) {
				indent -= LB_INDENT;
				splitLine = false;
			}
		}

		// Rebuild the new text
		this.newCursorPosition = before.length() + sb.length();
		sb.insert( 0, before );
		sb.append( after );

		return sb.toString();
	}


	/**
	 * @param text a non-null text
	 * @return a null or positive integer
	 */
	private int findPreviousIndentation( String text ) {

		int indent = 0;
		List<String> lines = Arrays.asList( text.split( "\\n" ));
		Collections.reverse( lines );
		for( String line : lines ) {

			// Find a non-empty line
			if( Utils.isEmptyOrWhitespaces( line ))
				continue;

			// Count the number of tabulations at the beginning
			for( char c : line.toCharArray()) {
				if( c == '\t' )
					indent ++;
				else
					break;
			}

			// If we end with an opening curly bracket, increment by one
			String workLine = TextUtils.removeComments( line ).trim();
			if( workLine.endsWith( "{" ))
				indent ++;

			break;
		}

		return indent;
	}
}
