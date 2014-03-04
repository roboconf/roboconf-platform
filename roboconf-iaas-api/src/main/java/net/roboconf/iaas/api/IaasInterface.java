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

package net.roboconf.iaas.api;

import java.util.Properties;

import net.roboconf.iaas.api.exceptions.CommunicationToIaasException;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.iaas.api.exceptions.InvalidIaasPropertiesException;

/**
 * The interface to implement to support a new IaaS.
 * @author Vincent Zurczak - Linagora
 */
public interface IaasInterface {

	String DEFAULT_IAAS_PROPERTIES_FILE_NAME = "iaas.properties";


	/**
	 * Sets all the properties that will be used when instantiating machines.
	 * <p>
	 * Examples: access key, secret key, ami, ssh key, etc.
	 * </p>
	 * @param iaasProperties the IaaS properties (not null)
	 */
	void setIaasProperties( Properties iaasProperties ) throws InvalidIaasPropertiesException;

	/**
	 * Creates a VM containing a message server in it.
	 * <p>
	 * The VM needs to know at startup some parameters,
	 * including the queue location.
	 * </p>
	 *
	 * @param ipMessagingServer the IP of the messaging server
	 * @param channelName the channel name
	 * @param applicationName the application name
	 * @return the instance id of this VM relative to the IaaS
	 * @throws IaasException
	 * @throws CommunicationToIaasException
	 */
	String createVM(
			String ipMessagingServer,
			String channelName,
			String applicationName )
	throws IaasException, CommunicationToIaasException;


	/**
	 * Asks for the termination of the VM identified by its instance id.
	 * @param instanceId the instance ID
	 * @throws IaasException
	 * @throws CommunicationToIaasException
	 */
	void terminateVM( String instanceId ) throws IaasException, CommunicationToIaasException;
}
