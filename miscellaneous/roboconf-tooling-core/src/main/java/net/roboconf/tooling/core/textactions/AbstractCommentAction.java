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
import java.util.Iterator;
import java.util.List;

import net.roboconf.tooling.core.SelectionRange;
import net.roboconf.tooling.core.TextUtils;

/**
 * An abstract (and robust) action to manage comments in text selection.
 * <p>
 * Text selections are modified if incorrect.<br />
 * They are also edited to capture all the lines that include the selected text.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractCommentAction implements ITextAction {

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

		// Analyze the lines
		List<String> lines = Arrays.asList( text.split( "\\n" ));
		for( String line : lines )
			analyzeLine( line );

		// Then, update them
		StringBuilder sb = new StringBuilder();
		for( Iterator<String> it = lines.iterator(); it.hasNext(); ) {
			String newLine = processLine( it.next());
			sb.append( newLine );
			if( it.hasNext() || text.endsWith( "\n" ))
				sb.append( "\n" );
		}

		// Rebuild the new text
		this.newCursorPosition = before.length() + sb.length();
		sb.insert( 0, before );
		sb.append( after );

		return sb.toString();
	}


	/**
	 * All the lines are pre-processed.
	 * @param line a non-null line
	 */
	protected void analyzeLine( String line ) {
		// nothing
	}


	/**
	 * Processes / Updates a line.
	 * @param line a non-null line
	 * @return a non-null line
	 */
	protected abstract String processLine( String line );
}
