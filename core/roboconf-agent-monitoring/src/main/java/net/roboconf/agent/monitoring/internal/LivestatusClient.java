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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Livestatus client.
 * @author Pierre-Yves Gibello - Linagora
 */
public class LivestatusClient {

	private String host = "localhost";
	private int port = 50000;
	
	public LivestatusClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	private String buildQuery(String... parts) {
		if(parts == null) return null;

		StringBuffer query = new StringBuffer();
		for(int i=0; i< parts.length; i++) {
			String part = (parts[i] == null ? null : parts[i].trim());
			if(part != null && part.length() > 0) {
				query.append(part + "\n");
			}
		}

		return query.toString();
	}

	public String queryLivestatus(String... parts) throws UnknownHostException, IOException {
		
		Socket livestatus = null;
		StringBuffer result = new StringBuffer();

		String query = buildQuery(parts);
		try {
			livestatus = new Socket(this.host, this.port);
			
			PrintWriter out = new PrintWriter(livestatus.getOutputStream());
					
			out.print(query);
			out.flush();
			livestatus.shutdownOutput();

			BufferedReader in = new BufferedReader(
					new InputStreamReader(livestatus.getInputStream()));
			String info;
			while((info = in.readLine()) != null) {
				result.append(info + "\n");
			}
		} finally {
			try { if(livestatus != null) livestatus.close(); } catch(Exception e) { /*ignore*/ }
		}
		
		String ret = result.toString().trim();
		
		if(ret.length() < 1) return null; // empty result
		else {
			// When columns are specified, Livestatus omits the column names: add them.
			String columns = null;
			for(int i=0; i< parts.length; i++) {
				String part = (parts[i] == null ? null : parts[i].trim());
				if(part != null && part.startsWith("Columns:")) {
					columns = part.substring(8).trim();
				}
			}
			
			if(columns != null) {
				return columns.replace(' ', ';') + "\n" + ret;
			} else {
				return ret;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if(args.length < 2) throw new Exception("Mandatory args: livestatus host + port");
		String livestatusHost = args[0];
		int livestatusPort = Integer.parseInt(args[1]);
		
		LivestatusClient client = new LivestatusClient(livestatusHost, livestatusPort);

		System.out.println(
				client.queryLivestatus("GET hosts",
						"Columns: host_name accept_passive_checks acknowledged",
						"Filter: accept_passive_checks = 1"
					));

	}

}
