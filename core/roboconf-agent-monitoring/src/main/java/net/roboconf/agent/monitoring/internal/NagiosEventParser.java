/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.agent.monitoring.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Extract Nagios request.
 * @author Pierre-Yves Gibello - Linagora
 */
public class NagiosEventParser {

	public String[] parse(BufferedReader reader) throws IOException {

		String line;
		List<String> parts = new LinkedList<String>();
		boolean finished = false;
		reader.mark(512);
		while((line = reader.readLine()) != null && ! finished) {
			line = line.trim();
			finished = (line.length() < 1);
			if(!finished) {
				reader.mark(512);
				parts.add(line);
			}
			else reader.reset();
		}
		
		if(parts.size() < 1) return null;
		else {
			String ret[] = new String[parts.size()];
			int pos = 0;
			for (String s : parts) {
				ret[pos++] = s;
			}
			return ret;
		}
	}
}
