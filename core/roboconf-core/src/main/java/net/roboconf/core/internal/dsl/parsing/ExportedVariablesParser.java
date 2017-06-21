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

package net.roboconf.core.internal.dsl.parsing;

import static net.roboconf.core.errors.ErrorDetails.variable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ExportedVariablesParser {

	public final Map<String,String> rawNameToVariables = new LinkedHashMap<> ();
	public final List<ParsingError> errors = new ArrayList<> ();

	private int cursor;


	/**
	 * Parses a line and extracts exported variables.
	 * @param line
	 * @param sourceFile
	 * @param lineNumber
	 */
	public void parse( String line, File sourceFile, int lineNumber ) {

		// Reset
		this.rawNameToVariables.clear();
		this.errors.clear();
		this.cursor = 0;

		// Parse
		while( this.cursor < line.length()) {

			char c = line.charAt( this.cursor );
			if( Character.isWhitespace( c ) || c == ',' )
				this.cursor ++;

			recognizeVariable( line, sourceFile, lineNumber );
		}
	}


	/**
	 * @param line
	 * @param sourceFile
	 * @param lineNumber
	 */
	private void recognizeVariable( String line, File sourceFile, int lineNumber ) {

		char c = '#';
		StringBuilder sb = new StringBuilder();
		while( this.cursor < line.length()
				&& (c = line.charAt( this.cursor )) != ','
				&& c != '=' ) {

			sb.append( c );
			this.cursor ++;
		}

		// Did we reach the end of the declaration?
		String variableName = sb.toString().trim();
		if( c == ',' || this.cursor >= line.length()) {
			if( Utils.isEmptyOrWhitespaces( variableName ))
				this.errors.add( new ParsingError( ErrorCode.PM_EMPTY_VARIABLE_NAME, sourceFile, lineNumber ));
			else
				this.rawNameToVariables.put( variableName, null );

			return;
		}

		// c is '='
		this.cursor ++;

		// Parse the other parts
		sb = new StringBuilder();
		while( this.cursor < line.length()
				&& (c = line.charAt( this.cursor )) != ','
				&& c != '"' ) {

			sb.append( c );
			this.cursor ++;
		}

		// Did we reach the end of the declaration?
		if( c == ',' || this.cursor >= line.length()) {

			// Quotes are the only solution to specify the empty string as a value.
			// Otherwise, we set the value to "null".
			String value = sb.toString().trim();
			if( value.isEmpty())
				value = null;

			this.rawNameToVariables.put( variableName, value );
			return;
		}

		// c is '"'
		if( ! Utils.isEmptyOrWhitespaces( sb.toString())) {
			this.errors.add( new ParsingError( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, sourceFile, lineNumber, variable( variableName )));

			// Move the cursor forward so that we do not read the quote character, again
			this.cursor ++;
			return;
		}

		// We should have a value between quotes
		this.cursor ++;
		sb = new StringBuilder();
		while( this.cursor < line.length()
				&& (c = line.charAt( this.cursor )) != '"' ) {

			sb.append( c );
			this.cursor ++;
		}

		// We expect a closing quote
		if( c != '"' ) {
			this.errors.add( new ParsingError( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, sourceFile, lineNumber, variable( variableName )));
			return;
		}

		// We should find a comma or the end of the line after
		this.cursor ++;
		while( this.cursor < line.length()) {
			c = line.charAt( this.cursor );
			if( c == ',' ) {
				break;

			} if( Character.isWhitespace( c )) {
				this.cursor ++;

			} else {
				this.errors.add( new ParsingError( ErrorCode.PM_INVALID_EXPORT_COMPLEX_VALUE, sourceFile, lineNumber, variable( variableName )));
				return;
			}
		}

		// Then, we have our value
		this.rawNameToVariables.put( variableName, sb.toString());
		this.cursor ++;
	}
}
