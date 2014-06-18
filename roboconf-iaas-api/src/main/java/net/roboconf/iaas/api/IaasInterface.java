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

import java.util.Map;

/**
 * The interface to implement to support a new IaaS.
 * @author Vincent Zurczak - Linagora
 */
public interface IaasInterface {

	/**
	 * Sets all the properties that will be used when instantiating machines.
	 * <p>
	 * Examples: access key, secret key, ami, ssh key, etc.
	 * </p>
	 * @param iaasProperties the IaaS properties (not null)
	 */
	void setIaasProperties( Map<String,String> iaasProperties ) throws IaasException;


	/**
	 * Creates a VM containing a message server in it.
	 * <p>
	 * The VM needs to know at startup some parameters,
	 * including the queue location.
	 * </p>
	 *
	 * @param messagingIp the IP of the messaging server
	 * @param messagingUsername the user name to connect to the messaging server
	 * @param messagingPassword the password to connect to the messaging server
	 * @param applicationName the application name
	 * @param rootInstanceName the name of the root instance associated with this VM
	 * @return the (machine) ID of this VM relative to the IaaS
	 * @throws IaasException
	 */
	String createVM(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName)
	throws IaasException;


	/**
	 * Asks for the termination of the VM identified by its id.
	 * @param machineId the machine ID
	 * @throws IaasException
	 */
	void terminateVM( String machineId ) throws IaasException;
}
