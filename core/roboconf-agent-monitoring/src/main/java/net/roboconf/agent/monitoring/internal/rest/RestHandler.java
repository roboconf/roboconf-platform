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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Handler to check the value returned by a REST call on a URL.
 * @author Pierre-Yves Gibello - Linagora
 */
public class RestHandler extends MonitoringHandler {

	private static final String USER_AGENT = "Mozilla/34.0";
	private static final String CHECK = "check";
	private static final String THAT = "that";
	private static final String CONDITION_PATTERN = "(\\w+)\\s+(==|=|>=|>|<=|<)\\s+(\\S+)";
	private static final String WHOLE_PATTERN = CHECK + "\\s+(\\S+)\\s+" + THAT + "\\s+" + CONDITION_PATTERN;

	private final Logger logger = Logger.getLogger(getClass().getName());
	private String url, conditionParameter, conditionOperator, conditionThreshold;


	/**
	 * Constructor.
	 * @param eventId
	 * @param applicationName
	 * @param vmInstanceName
	 * @param fileContent
	 */
	public RestHandler( String eventName, String applicationName, String vmInstanceName, String fileContent ) {
		super( eventName, applicationName, vmInstanceName );

		Matcher m = Pattern.compile( WHOLE_PATTERN, Pattern.CASE_INSENSITIVE ).matcher( fileContent );
		if( m .find()) {
			this.url = m.group( 1 );
			this.conditionParameter = m.group( 2 );
			this.conditionOperator = m.group( 3 );
			this.conditionThreshold = m.group( 4 );

		} else {
			this.logger.severe( "Invalid content for the 'rest' handler in the agent's monitoring." );
		}
	}


	/**
	 * Get the REST URL.
	 * @return The REST url
	 */
	public String getUrl() {
		return this.url;
	}


	/**
	 * @return the conditionParameter
	 */
	public String getConditionParameter() {
		return this.conditionParameter;
	}


	/**
	 * @return the conditionOperator
	 */
	public String getConditionOperator() {
		return this.conditionOperator;
	}


	/**
	 * @return the conditionThreshold
	 */
	public String getConditionThreshold() {
		return this.conditionThreshold;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.agent.monitoring.internal.MonitoringHandler
	 * #process()
	 */
	@Override
	public MsgNotifAutonomic process() {

		MsgNotifAutonomic result = null;
		String response = null;
		if( this.url != null )
			response = this.url.startsWith("https:") ? httpsQuery() : httpQuery();

		if( response != null )
			response = response.replace('{', ' ').replace('}', ' ').trim();
		else
			response = "";

		HashMap<String,String> map = new HashMap<String,String> ();
		for( String s : response.split( "\\n" )) {
			String kv[] = s.split(":");
			if( kv.length == 2 )
				map.put( kv[0].replace("\"", " ").trim(), kv[ 1 ]);
			else
				break;
		}

		if( map.isEmpty()) {
			this.logger.warning( "The REST response could not be parsed." );
			this.logger.finer( "Received response: " + response );

		} else if( evalCondition( map )) {
			result = new MsgNotifAutonomic( this.applicationName, this.vmInstanceName, this.eventId, response.toString());
		}

		return result;
	}


	/**
	 * Evaluates a condition (eg. "lag>=100") using data in key-pair value map (e.g. <"lag","50">).
	 * @param valueMap The values (key-pairs) on which to evaluate the condition.
	 * @return true if the condition is met, false otherwise
	 */
	boolean evalCondition( Map<String,String> map ) {

		boolean result = false;
		String value = map.get( this.conditionParameter );
		if( value != null ) {
			try {
				Double doubleValue = Double.parseDouble( value );
				Double thresholdValue = Double.parseDouble( this.conditionThreshold );

				// Do not use arithmetic operators with doubles...
				int comparison = doubleValue.compareTo( thresholdValue );
				if( ">".equals( this.conditionOperator ))
					result = comparison > 0;
				else if( ">=".equals( this.conditionOperator ))
					result = comparison >= 0;
				else if( "<".equals( this.conditionOperator ))
					result = comparison < 0;
				else if( "<=".equals( this.conditionOperator ))
					result = comparison <= 0;
				else
					result = comparison == 0;

			} catch( NumberFormatException e ) {
				if( "==".equals( this.conditionOperator ) || "=".equals( this.conditionOperator ))
					result = Utils.areEqual( value, this.conditionThreshold );
				else
					this.logger.fine( "Invalid double. " + e.getMessage());
			}
		}

		return result;
	}

	/**
	 * Query a https URL, ignoring certificates.
	 * @return The query response
	 */
	private String httpsQuery() {

		String response = null;
		try {
			// Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] { new LocalX509TrustManager()};

	        // Install the all-trusting trust manager
	        final SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init( null, trustAllCerts, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory());

	        // Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new LocalHostnameVerifier();
			HttpsURLConnection.setDefaultHostnameVerifier( allHostsValid );

			URL restUrl = new URL( this.url );
			HttpsURLConnection conn = (HttpsURLConnection) restUrl.openConnection();
			response = query( conn );

		} catch( Exception e ) {
			this.logger.severe( "Cannot issue GET on URL " + this.url + ". Monitoring notification is discarded." );
			Utils.logException(this.logger, e);
		}

		return response;
	}

	/**
	 * Query a http URL.
	 * @return The query response
	 */
	private String httpQuery() {

		String response = null;
		try {
			URL restUrl = new URL( this.url );
			HttpURLConnection conn = (HttpURLConnection) restUrl.openConnection();
			response = query( conn );

		} catch( Exception e ) {
			this.logger.severe( "Cannot issue GET on URL " + this.url + ". Monitoring notification is discarded." );
			Utils.logException(this.logger, e);
		}

		return response;
	}


	private String query( HttpURLConnection conn ) throws IOException {

		InputStream in = null;
		String response = null;
		try {
			conn.setRequestMethod( "GET" );
			conn.setRequestProperty( "User-Agent", USER_AGENT );

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			in = conn.getInputStream();
			Utils.copyStream( in, os );
			response = os.toString("UTF-8");

		} finally {
			Utils.closeQuietly( in );
		}

		return response == null ? null : response.toString();
	}


	/**
	 * @author Pierre-Yves Gibello - Linagora
	 */
	static class LocalX509TrustManager implements X509TrustManager {
		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			// nothing
        }

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			// nothing
        }
    }


	/**
	 * @author Pierre-Yves Gibello - Linagora
	 */
	static class LocalHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			// Never verify
			return false;
		}
	}
}
