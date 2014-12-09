package net.roboconf.dm.internal.autonomic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
