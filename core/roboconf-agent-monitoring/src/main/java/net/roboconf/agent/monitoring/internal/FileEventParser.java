package net.roboconf.agent.monitoring.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class FileEventParser {
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
				if(! line.startsWith("#")) parts.add(line);
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
