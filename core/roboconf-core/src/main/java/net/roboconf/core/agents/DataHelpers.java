/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.agents;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

/**
 * A set of helpers to write and read data for agents.
 * @author Vincent Zurczak - Linagora
 */
public final class DataHelpers {

	public static final String SCOPED_INSTANCE_PATH = "scoped.instance.path";
	public static final String APPLICATION_NAME = "application.name";
	public static final String DOMAIN = "domain";


	/**
	 * Constructor.
	 */
	private DataHelpers() {
		// nothing
	}


	/**
	 * Writes user data as a string.
	 * @param domain the domain (used among other things in the messaging)
	 * @param messagingConfiguration the messaging configuration
	 * @param applicationName the application name
	 * @param scopedInstancePath the scoped instance's path (the instance associated with the agent)
	 * @return a non-null string
	 * @throws IOException if something went wrong
	 */
	public static String writeUserDataAsString(
			Map<String,String> messagingConfiguration,
			String domain,
			String applicationName,
			String scopedInstancePath )
	throws IOException {

		Properties props = writeUserDataAsProperties( messagingConfiguration, domain, applicationName, scopedInstancePath );
		StringWriter writer = new StringWriter();
		props.store( writer, "" );

		return writer.toString();
	}


	/**
	 * Writes user data as properties.
	 * @param domain the domain (used among other things in the messaging)
	 * @param applicationName the application name
	 * @param scopedInstancePath the scoped instance's path (the instance associated with the agent)
	 * @param messagingConfiguration a map containing the messaging configuration
	 * @return a non-null object
	 */
	public static Properties writeUserDataAsProperties(
			Map<String,String> messagingConfiguration,
			String domain,
			String applicationName,
			String scopedInstancePath ) {

		Properties result = new Properties();
		if( applicationName != null )
			result.setProperty( APPLICATION_NAME, applicationName );

		if( scopedInstancePath != null )
			result.setProperty( SCOPED_INSTANCE_PATH, scopedInstancePath );

		if( domain != null )
			result.setProperty( DOMAIN, domain );

		if( messagingConfiguration != null ) {
			for( Map.Entry<String,String> e : messagingConfiguration.entrySet()) {
				if( e.getValue() != null )
					result.setProperty( e.getKey(), e.getValue());
			}
		}

		return result;
	}


	/**
	 * Reads user data.
	 * @param rawProperties the user data as a string
	 * @return a non-null object
	 * @throws IOException if something went wrong
	 */
	public static Properties readUserData( String rawProperties ) throws IOException {

		Properties result = new Properties();
		StringReader reader = new StringReader( rawProperties );
		result.load( reader );

		return result;
	}
}
