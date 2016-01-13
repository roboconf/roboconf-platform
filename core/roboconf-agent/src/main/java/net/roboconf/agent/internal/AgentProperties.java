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

package net.roboconf.agent.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import net.roboconf.core.agents.DataHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.MessagingConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class AgentProperties {

	private String applicationName, ipAddress, scopedInstancePath;
	private Map<String,String> messagingConfiguration;


	/**
	 * Constructor.
	 */
	AgentProperties() {
		// nothing
	}


	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return this.applicationName;
	}


	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}


	/**
	 * @return the scopedInstancePath
	 */
	public String getScopedInstancePath() {
		return this.scopedInstancePath;
	}


	/**
	 * @param scopedInstancePath the scopedInstancePath to set
	 */
	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
	}


	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return this.ipAddress;
	}


	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress( String ipAddress ) {
		this.ipAddress = ipAddress;
	}


	/**
	 * @return the messaging configuration.
	 */
	public Map<String, String> getMessagingConfiguration() {
		return this.messagingConfiguration;
	}

	/**
	 * @param messagingConfiguration the messaging configuration to set.
	 */
	public void setMessagingConfiguration( Map<String, String> messagingConfiguration ) {
		this.messagingConfiguration = messagingConfiguration;
	}


	/**
	 * Validates this bean.
	 * @return null if no error was found, false otherwise
	 */
	public String validate() {

		String result = null;
		if( this.messagingConfiguration == null || this.messagingConfiguration.isEmpty())
			result = "The message configuration cannot be null or empty.";
		else if( this.messagingConfiguration.get(MessagingConstants.MESSAGING_TYPE_PROPERTY) == null)
			result = "The message configuration does not contain the messaging type.";
		else if( Utils.isEmptyOrWhitespaces( this.applicationName ))
			result = "The application name cannot be null or empty.";
		else if( Utils.isEmptyOrWhitespaces( this.scopedInstancePath ))
			result = "The scoped instance's path cannot be null or empty.";

		return result;
	}


	/**
	 * Creates a new bean from raw properties that will be parsed.
	 * @param rawProperties a non-null string
	 * @param logger a logger (not null)
	 * @return a non-null bean
	 * @throws IOException
	 */
	public static AgentProperties readIaasProperties( String rawProperties, Logger logger ) throws IOException {

		Properties props = new Properties();
		if( rawProperties != null )
			props = DataHelpers.readUserData( rawProperties );

		return readIaasProperties( props );
	}


	/**
	 * Creates a new bean from properties.
	 * @param props non-null properties
	 * @return a non-null bean
	 */
	public static AgentProperties readIaasProperties( Properties props ) {

		// Given #213, we have to replace some characters escaped by AWS (and probably Openstack too).
		AgentProperties result = new AgentProperties();
		result.setApplicationName( updatedField( props, DataHelpers.APPLICATION_NAME ));
		result.setScopedInstancePath( updatedField( props, DataHelpers.SCOPED_INSTANCE_PATH ));

		final Map<String, String> messagingConfiguration = new LinkedHashMap<> ();
		for (String k : props.stringPropertyNames()) {
			if (! DataHelpers.APPLICATION_NAME.equals(k) && ! DataHelpers.SCOPED_INSTANCE_PATH.equals(k)) {
				// All other properties are considered messaging-specific.
				messagingConfiguration.put(k, updatedField( props, k));
			}
		}

		result.setMessagingConfiguration(Collections.unmodifiableMap(messagingConfiguration));
		return result;
	}


	/**
	 * Gets a property and updates it to prevent escaped characters.
	 * @param props the IAAS properties.
	 * @param fieldName the name of the field to read.
	 * @return an updated string
	 */
	private static String updatedField( Properties props, String fieldName ) {

		String property = props.getProperty( fieldName );
		if( property != null )
			property = property.replace( "\\:", ":" );

		return property;
	}

}
