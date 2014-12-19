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

package net.roboconf.agent.monitoring.internal.rest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.roboconf.agent.monitoring.internal.MonitoringHandler;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * Handler to check the value returned by a REST call on a URL
 * @author Pierre-Yves Gibello - Linagora
 */
public class RestHandler extends MonitoringHandler {

	private final String USER_AGENT = "Mozilla/34.0";
	private final Logger logger = Logger.getLogger(getClass().getName());

	private String url;
	private String condition;

	/**
	 * Constructor.
	 * @param eventId
	 * @param applicationName
	 * @param vmInstanceName
	 * @param fileContent
	 */
	public RestHandler( String eventName, String applicationName, String vmInstanceName, String fileContent ) {
		super( eventName, applicationName, vmInstanceName );

		if(fileContent == null) return;
		fileContent = fileContent.trim();
		if(fileContent.length() < 1) return;
		
		String lines[] = fileContent.split("\\n");
		
		for(String statement : lines) {
			if(statement.toLowerCase().startsWith("url")) { // url: URL
				this.url = getValue(statement);
			} else if(statement.toLowerCase().startsWith("filter")) { // filter: condition
				this.condition = getValue(statement);
			}
		}
	}
	
	public String getUrl() {
		return url;
	}

	public String getCondition() {
		return condition;
	}

	private String getValue(String statement) {
		int pos = statement.indexOf(":");
		if(pos > 0 && pos < statement.length() -1) return statement.substring(pos+1).trim();
		else return null;
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.agent.monitoring.internal.MonitoringHandler
	 * #process()
	 */
	@Override
	public MsgNotifAutonomic process() {

		MsgNotifAutonomic result = null;
		
		String response = (url.startsWith("https:") ? httpsQuery(url) : httpQuery(url));
		if(response == null) return null;

		String rsp = response.replace('{', ' ').replace('}', ' ').trim();
		String keyValuePairs[] = rsp.split("\\n");
		HashMap<String, Double> valueMap = null;

		if(keyValuePairs.length > 0) {
			valueMap = new HashMap<String, Double>();
			try {
				for(String pair : keyValuePairs) {
					String kv[] = pair.split(":");
					if(kv.length == 2) {
						valueMap.put(kv[0].replace("\"", " ").trim(),
								Double.parseDouble(getValue(pair)));
					}
				}
			} catch(Exception e) {
				this.logger.severe("Can\'t parse REST request result: " + response);
				return null;
			}

			if(evalCondition(valueMap)) {
				result = new MsgNotifAutonomic(
						this.eventId,
						this.applicationName,
						this.vmInstanceName,
						response.toString());
			}
		}

		this.logger.finest("Condition: " + condition + ", values: " + rsp);
		return result;
	}

	/**
	 * Evaluate condition (eg. "lag>=100") using data in keypair value map (eg. <"lag", 50>).
	 * @param valueMap The values (keypair) on which to evaluate the condition.
	 * @return true if the condition is met, false otherwise.
	 */
	boolean evalCondition(HashMap<String, Double> valueMap) {
		if(valueMap == null || valueMap.isEmpty()) return false;
		
		String rawCondition = condition.replaceAll("\\s+","");

		// TODO just "lag" is handled, with one comparison operand...
		if(rawCondition.startsWith("lag") && rawCondition.length() >= 5) {
			Double lag = valueMap.get("lag");
			if(lag == null) return false;

			String val = rawCondition.substring(4);
			String operand = rawCondition.substring(3, 4);
			if(rawCondition.length() >= 6 && ! Character.isDigit(val.charAt(0))) {
				val = rawCondition.substring(5);
				operand = rawCondition.substring(3, 5);
			}
			Double v = null;
			try {
				v = Double.parseDouble(val);
			} catch(Exception e) {
				return false;
			}
			if(">".equals(operand)) {
				return(lag.doubleValue() > v.doubleValue());
			} else if(">=".equals(operand)) {
				return(lag.doubleValue() >= v.doubleValue());
			} else if("=".equals(operand)) {
				return(lag.doubleValue() == v.doubleValue());
			} else if("<=".equals(operand)) {
				return(lag.doubleValue() <= v.doubleValue());
			} else if("<".equals(operand)) {
				return(lag.doubleValue() < v.doubleValue());
			}
		}
		
		return false;
	}

	/**
	 * Query a https URL, ignoring certificates.
	 * @param url The https URL to query
	 * @return The query response
	 */
	private String httpsQuery(String url) {

		InputStream in = null;
		String response = null;
		try {
			
			// Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] { new LocalX509TrustManager() };
	        // Install the all-trusting trust manager
	        final SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	        // Create all-trusting host name verifier
	        @SuppressWarnings("unused")
			HostnameVerifier allHostsValid = new LocalHostnameVerifier();
			
			URL restUrl = new URL(this.url);
			HttpsURLConnection conn = (HttpsURLConnection)restUrl.openConnection();
			conn.setRequestMethod("GET");

			//add request header
			conn.setRequestProperty("User-Agent", USER_AGENT);

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			in = conn.getInputStream();
			Utils.copyStream( in, os );
			response = os.toString("UTF-8");
			
		} catch(Exception e) {
			this.logger.severe("Can't issue GET on URL " + this.url + ". Monitoring notification is discarded.");
			Utils.logException(this.logger, e);
			
		} finally {
			Utils.closeQuietly(in);
		}
		
		return response;
	}

	/**
	 * Query a http URL.
	 * @param url The http URL to query
	 * @return The query response
	 */
	private String httpQuery(String url) {
		
		InputStream in = null;
		String response = null;
		try {
			URL restUrl = new URL(this.url);
			HttpURLConnection conn = (HttpURLConnection)restUrl.openConnection();
			conn.setRequestMethod("GET");

			//add request header
			conn.setRequestProperty("User-Agent", USER_AGENT);

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			in = conn.getInputStream();
			Utils.copyStream( in, os );
			response = os.toString("UTF-8");
			
		} catch( Exception e ) {
			this.logger.severe("Can't issue GET on URL " + this.url + ". Monitoring notification is discarded.");
			Utils.logException(this.logger, e);
			
		} finally {
			Utils.closeQuietly(in);
		}
		
		return response.toString();
	}

	public static void main(String args[]) throws Exception {
		
		Thread thread = new Thread() {
			@Override
			public void run() {

				ServerSocket socketServer = null;
				try {
					try {
						System.out.println("The socket server is about to start." );
						socketServer = new ServerSocket( 1234 );
						System.out.println("The socket server was started." );
						Socket socket = socketServer.accept();
						System.out.println("The socket server received a connection." );

						PrintWriter writer = new PrintWriter( new OutputStreamWriter( socket.getOutputStream(), StandardCharsets.UTF_8 ), false );
						writer.print("HTTP/1.1 200 OK\n\n{\"lag\":0}");
						writer.flush();
						socket.shutdownOutput();
						socket.close();

					} finally {
						if( socketServer != null )
							socketServer.close();
					}

				} catch( Exception e ) {
					// nothing
				}
			}
		};

		// Start our server
		thread.start();
		Thread.sleep( 1500 );
		
		RestHandler handler = new RestHandler("testEvent", "app1", "instance1",
				"url:http://localhost:1234\nfilter:lag=0");
				//"url:https://dev.open-paas.org/api/monitoring\nfilter:lag=0");
		System.out.println(handler.process());
	}
	
	private static class LocalX509TrustManager implements X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }
	
	private static class LocalHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			// Never verify
			return false;
		}
	}
}
