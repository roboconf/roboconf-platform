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

package net.roboconf.core.autonomic;

import static net.roboconf.core.errors.ErrorDetails.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuleParser {

	private static final Pattern RULE_PATTERN = Pattern.compile( "(?is)^\\s*rule\\s+\"([^\"]*)\"\\s+(.*)when\\s+(.+)\\s+then\\s+(.+)\\s+end\\s*$" );
	private static final Pattern SLEEP_PERIOD_PATTERN = Pattern.compile( "(?i)\\bsleep period is\\s+(\\d+)s?" );
	private static final Pattern TIME_WINDOW_PATTERN = Pattern.compile( "(?i)\\btime window is\\s+(\\d+)s?" );

	private static final String SINGLE_COMMENT_PATTERN = "//.*\r?\n";
	private static final String SINGLE_SHARP_COMMENT_PATTERN = "#.*\r?\n";
	private static final String MULTILINE_COMMENT_PATTERN =  "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)";

	private final List<ParsingError> parsingErrors = new ArrayList<> ();
	private final Rule rule = new Rule();


	/**
	 * Constructor.
	 * @param ruleFile
	 */
	public RuleParser( File ruleFile ) {

		ErrorDetails details = file( ruleFile );
		try {
			String s = Utils.readFileContent( ruleFile );
			s = s.replaceAll( MULTILINE_COMMENT_PATTERN, "" );
			s = s.replaceAll( SINGLE_COMMENT_PATTERN, "" );
			s = s.replaceAll( SINGLE_SHARP_COMMENT_PATTERN, "" );
			s = s.trim();

			Matcher m = RULE_PATTERN.matcher( s );
			if( ! m.matches()) {
				this.parsingErrors.add( new ParsingError( ErrorCode.RULE_INVALID_SYNTAX, ruleFile, -1 ));

			} else {
				// Build the rule
				String ruleName = m.group( 1 ).trim();
				this.rule.setRuleName( ruleName );

				String eventName = m.group( 3 ).trim();
				this.rule.setEventName( eventName );

				String commandNames = m.group( 4 );
				for( String commandName : Utils.splitNicelyWithPattern( commandNames, ";|\r?\n" )) {
					if( ! Utils.isEmptyOrWhitespaces( commandName ))
						this.rule.getCommandsToInvoke().add( commandName );
				}

				String properties = m.group( 2 );
				if(( m = TIME_WINDOW_PATTERN.matcher( properties )).find())
					this.rule.setTimingWindow( Integer.parseInt( m.group( 1 )));

				if(( m = SLEEP_PERIOD_PATTERN.matcher( properties )).find())
					this.rule.setDelayBetweenSucceedingInvocations( Integer.parseInt( m.group( 1 )));

				// Validate the rule
				if( Utils.isEmptyOrWhitespaces( ruleName ))
					this.parsingErrors.add( new ParsingError( ErrorCode.RULE_EMPTY_NAME, ruleFile, -1, details ));

				if( Utils.isEmptyOrWhitespaces( eventName ))
					this.parsingErrors.add( new ParsingError( ErrorCode.RULE_EMPTY_WHEN, ruleFile, -1, details ));

				if( this.rule.getCommandsToInvoke().isEmpty())
					this.parsingErrors.add( new ParsingError( ErrorCode.RULE_EMPTY_THEN, ruleFile, -1, details ));
			}

		} catch( IOException e ) {
			this.parsingErrors.add( new ParsingError( ErrorCode.RULE_IO_ERROR, ruleFile, -1, details ));
		}
	}

	/**
	 * @return the parsingErrors
	 */
	public List<ParsingError> getParsingErrors() {
		return this.parsingErrors;
	}

	/**
	 * @return the parsed rule
	 */
	public Rule getRule() {
		return this.rule;
	}
}
