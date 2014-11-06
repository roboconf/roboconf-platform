/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.api;

import java.util.Map;

/**
 * The interface to implement to support a new deployment target.
 * @author Vincent Zurczak - Linagora
 */
public interface TargetHandler {

	/**
	 * @return the target identifier
	 */
	String getTargetId();


	/**
	 * Sets all the properties that will be used when instantiating machines.
	 * <p>
	 * Examples: access key, secret key, ami, ssh key, etc.
	 * </p>
	 * @param targetProperties the target properties (not null)
	 */
	void setTargetProperties( Map<String,String> targetProperties ) throws TargetException;


	/**
	 * Creates or configures a machine.
	 * <p>
	 * The machine must have a Roboconf agent installed on it.
	 * </p>
	 *
	 * @param messagingIp the IP of the messaging server
	 * @param messagingUsername the user name to connect to the messaging server
	 * @param messagingPassword the password to connect to the messaging server
	 * @param applicationName the application name
	 * @param rootInstanceName the name of the root instance associated with this VM
	 * @return the (machine) ID of this machine (should be unique for the target manager)
	 * @throws TargetException
	 */
	String createOrConfigureMachine(
			String messagingIp,
			String messagingUsername,
			String messagingPassword,
			String rootInstanceName,
			String applicationName)
	throws TargetException;


	/**
	 * Asks for the termination of the machine identified by its id.
	 * @param machineId the machine ID
	 * @throws TargetException
	 */
	void terminateMachine( String machineId ) throws TargetException;
}
