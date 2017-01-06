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

package net.roboconf.tooling.core;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class TextUtils {

	/**
	 * Private constructor.
	 */
	private TextUtils() {
		// nothing
	}


	/**
	 * Removes the comments from a line.
	 * @param line a non-null line
	 * @return a non-null line
	 */
	public static String removeComments( String line ) {
		return line.replaceAll( "\\s*#[^\n]*", "" );
	}


	/**
	 * @param c a character
	 * @return true if it is '\n' or '\r'
	 */
	public static boolean isLineBreak( char c ) {
		return c == '\n' || c == '\r';
	}


	/**
	 * Finds the region that includes both the given selection and entire lines.
	 * <p>
	 * Invalid values for the selection settings are fixed by this method.
	 * </p>
	 *
	 * @param fullText the full text
	 * @param selectionOffset the selection's offset
	 * @param selectionLength the selection's length
	 * @return a non-null selection range
	 */
	public static SelectionRange findRegionFromSelection( String fullText, int selectionOffset, int selectionLength ) {

		// Find the whole lines that match the selection (start)
		int newSselectionOffset = TextUtils.fixSelectionOffset( fullText, selectionOffset );

		// Modifying the offset can make the length slide => update it
		selectionLength += selectionOffset - newSselectionOffset;
		selectionOffset = newSselectionOffset;

		// Find the whole lines that match the selection (end)
		selectionLength = TextUtils.fixSelectionLength( fullText, selectionOffset, selectionLength );

		return new SelectionRange( selectionOffset, selectionLength );
	}


	/**
	 * Fixes the selection offset to get the whole line.
	 * <p>
	 * Invalid values result in zero.
	 * </p>
	 *
	 * @param fullText the full text (not null)
	 * @param selectionOffset the selection offset
	 * @return a valid selection offset, set to the beginning of the last line or of the text
	 */
	static int fixSelectionOffset( String fullText, int selectionOffset ) {

		if( selectionOffset < 0 || selectionOffset >= fullText.length())
			selectionOffset = 0;

		// Get the full line
		int lineBreakIndex = selectionOffset - 1;
		for( ; lineBreakIndex >= 0; lineBreakIndex -- ) {
			char c = fullText.charAt( lineBreakIndex );
			if( isLineBreak( c )) {
				lineBreakIndex ++;
				break;
			}
		}

		return lineBreakIndex < 0 ? 0 : lineBreakIndex;
	}


	/**
	 * Fixes the selection length to get the whole line.
	 * @param fullText the full text (not null)
	 * @param selectionOffset a <b>valid</b> selection offset
	 * @param selectionLength the selection length
	 * @return a valid selection length (can be zero)
	 */
	static int fixSelectionLength( String fullText, int selectionOffset, int selectionLength ) {

		if( selectionLength < 0 )
			selectionLength = 0;
		else if( selectionOffset + selectionLength > fullText.length())
			selectionLength = fullText.length() - selectionOffset;

		for( ; selectionOffset + selectionLength < fullText.length(); selectionLength ++ ) {
			char c = fullText.charAt( selectionOffset + selectionLength );
			if( isLineBreak( c ))
				break;
		}

		return selectionLength;
	}
}
