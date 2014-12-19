/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A parser for rules.
 * @author Pierre-Yves Gibello - Linagora
 *
 */
public class RulesParser {
	
	/**
	 * Parse autonomic rules.
	 * Syntax is: EVENT_ID, REACTION_ID, TEMPLATE
	 * Other lines are ignored (normally, only comment lines, starting with "#", and empty ones are legitimate).
	 * @param appRootDir
	 * @return A map, whose key is an EVENT_ID and value a Rule.
	 * @throws IOException
	 */
	public static Map<String, AutonomicRule> parseRules(File appRootDir) throws IOException {
		File rulesFile = new File(appRootDir.getCanonicalPath() + "/autonomic/rules.cfg");
		if(! rulesFile.exists() || ! rulesFile.isFile() || ! rulesFile.canRead())
			throw new IOException("Rules file " + rulesFile.getAbsolutePath() + " not accessible or read-only");

		String line;
		Map<String, AutonomicRule> ret = null;
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(rulesFile));
			while((line = in.readLine()) != null) {
				line = line.trim();
				if(line.length() > 3 && ! line.startsWith("#")) { // Min length 5 (eg. a,b,c)
					String tokens[] = line.split(",");
					if(tokens.length >= 3) {
						if(ret == null) ret = new HashMap<String, AutonomicRule>();
						ret.put(tokens[0].trim(), new AutonomicRule(tokens[1].trim(), tokens[2].trim()));
					}
				}
			}
		} finally {
			if(in != null) try { in.close(); } catch(Exception e) { /*ignore*/ }
		}
		
		return ret;
	}

}
