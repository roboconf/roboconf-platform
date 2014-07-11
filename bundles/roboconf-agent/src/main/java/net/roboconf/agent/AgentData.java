/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.agent;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import net.roboconf.core.agents.DataHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentData {

	private String applicationName, ipAddress, rootInstanceName;
	private String messageServerIp, messageServerUsername, messageServerPassword;


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
	 * @return the rootInstanceName
	 */
	public String getRootInstanceName() {
		return this.rootInstanceName;
	}


	/**
	 * @param rootInstanceName the rootInstanceName to set
	 */
	public void setRootInstanceName( String rootInstanceName ) {
		this.rootInstanceName = rootInstanceName;
	}


	/**
	 * @return the messageServerIp
	 */
	public String getMessageServerIp() {
		return this.messageServerIp;
	}


	/**
	 * @param messageServerIp the messageServerIp to set
	 */
	public void setMessageServerIp( String messageServerIp ) {
		this.messageServerIp = messageServerIp;
	}


	/**
	 * @return the messageServerUsername
	 */
	public String getMessageServerUsername() {
		return this.messageServerUsername;
	}


	/**
	 * @param messageServerUsername the messageServerUsername to set
	 */
	public void setMessageServerUsername( String messageServerUsername ) {
		this.messageServerUsername = messageServerUsername;
	}


	/**
	 * @return the messageServerPassword
	 */
	public String getMessageServerPassword() {
		return this.messageServerPassword;
	}


	/**
	 * @param messageServerPassword the messageServerPassword to set
	 */
	public void setMessageServerPassword( String messageServerPassword ) {
		this.messageServerPassword = messageServerPassword;
	}


	/**
	 * Validates this bean.
	 * @return null if no error was found, false otherwise
	 */
	public String validate() {

		String result = null;
		if( Utils.isEmptyOrWhitespaces( this.messageServerIp ))
			result = "The message server IP cannot be null or empty.";
		else if( Utils.isEmptyOrWhitespaces( this.messageServerPassword ))
			result = "The message server's password cannot be null or empty.";
		else if( Utils.isEmptyOrWhitespaces( this.messageServerUsername ))
			result = "The message server's user name cannot be null or empty.";
		else if( Utils.isEmptyOrWhitespaces( this.applicationName ))
			result = "The application name cannot be null or empty.";
		else if( Utils.isEmptyOrWhitespaces( this.rootInstanceName ))
			result = "The root instance name cannot be null or empty.";

		return result;
	}


	/**
	 * Creates a new bean from raw properties that will be parsed.
	 * @param rawProperties a non-null string
	 * @param logger a logger (not null)
	 * @return a non-null bean
	 */
	public static AgentData readIaasProperties( String rawProperties, Logger logger ) {

		Properties props = new Properties();
		try {
			if( rawProperties != null )
				props = DataHelpers.readIaasData( rawProperties );

		} catch( IOException e ) {
			logger.severe( "The agent data could not be read. " + e.getMessage());
			logger.finest( Utils.writeException( e ));
		}

		return readIaasProperties( props );
	}


	/**
	 * Creates a new bean from properties.
	 * @param props non-null properties
	 * @return a non-null bean
	 */
	public static AgentData readIaasProperties( Properties props ) {

		AgentData result = new AgentData();
		result.setApplicationName( props.getProperty( DataHelpers.APPLICATION_NAME ));
		result.setRootInstanceName( props.getProperty( DataHelpers.ROOT_INSTANCE_NAME ));
		result.setMessageServerIp( props.getProperty( DataHelpers.MESSAGING_IP ));
		result.setMessageServerUsername( props.getProperty( DataHelpers.MESSAGING_USERNAME ));
		result.setMessageServerPassword( props.getProperty( DataHelpers.MESSAGING_PASSWORD ));

		return result;
	}
}
