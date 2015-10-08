/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.autonomic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;

/**
 * A parser for rules.
 * @author Pierre-Yves Gibello - Linagora
 */
public final class RulesParser {

	private static final String COMMENT_DELIMITER = "#";
	static final String RULE_BEGINNING = "[reaction";
	static final String RULE_PATTERN = "\\" + RULE_BEGINNING + "\\s+(\\S+)\\s+(\\S+)(\\s+\\d+)*\\s*\\]";


	/**
	 * Private empty constructor.
	 */
	private RulesParser() {
		// nothing
	}


	/**
	 * Parses autonomic rules.
	 * @param ma a managed application
	 * @return a non-null map (key = event ID, value = a rule)
	 * @throws IOException
	 */
	public static Map<String,AutonomicRule> parseRules( ManagedApplication ma ) throws IOException {

		Map<String,AutonomicRule> result = new HashMap<String,AutonomicRule> ();
		File rulesFile = new File( ma.getDirectory(), Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES);
		if( rulesFile.isFile())
			result.putAll( parseRules( rulesFile ));

		return result;
	}


	/**
	 * Parses autonomic rules.
	 * @param rulesFile a file (that must exist)
	 * @return a non-null map (key = event ID, value = a rule)
	 * @throws IOException
	 */
	public static Map<String,AutonomicRule> parseRules( File rulesFile ) throws IOException {
		Map<String,AutonomicRule> result = new HashMap<String,AutonomicRule> ();

		// Read the file content
		String fileContent = Utils.readFileContent( rulesFile );
		List<String> sections = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for( String s : Arrays.asList( fileContent.trim().split( "\n" ))) {
			s = s.trim();
			if( s.length() == 0
					|| s.startsWith( COMMENT_DELIMITER ))
				continue;

			if( s.toLowerCase().startsWith( RULE_BEGINNING )) {
				sections.add( sb.toString());
				sb.setLength( 0 );
			}

			sb.append( s + "\n" );
		}

		sections.add( sb.toString());

		// Deal with the sections
		Pattern reactionPattern = Pattern.compile( RULE_PATTERN, Pattern.CASE_INSENSITIVE );
		for( String s : sections ) {
			if( s.length() == 0 )
				continue;

			Matcher m = reactionPattern.matcher( s );
			if( ! m.find())
				continue;

			s = s.substring( m.end()).trim();
			String eventId = m.group( 1 );
			String delayAS = m.group( 3 );
			long delay = delayAS != null ? Long.valueOf( delayAS.trim()) : 0L;

			AutonomicRule rule = new AutonomicRule( m.group( 2 ), s, eventId, delay );
			result.put( eventId, rule );
		}

		return result;
	}
}
