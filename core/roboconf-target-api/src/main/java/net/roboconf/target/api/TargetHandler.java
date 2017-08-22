/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

import net.roboconf.core.model.beans.Instance;

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
	 * Creates a machine.
	 * <p>
	 * The machine must have a Roboconf agent installed on it.<br>
	 * This method only deals with the creation of a VM. Configuring the network,
	 * storage and so on, should be done {@link #configureMachine(Map, Map, String, String, String, Instance)}.
	 * </p>
	 *
	 * @param parameters the target parameters
	 * @return the (machine) ID of this machine (should be unique for the target manager)
	 * @throws TargetException
	 */
	String createMachine( TargetHandlerParameters parameters ) throws TargetException;


	/**
	 * Configures a machine identified by its id.
	 * <p>
	 * Configuration can be adding storage, setting up the network settings, etc.
	 * </p>
	 * <p>
	 * Messaging and other information are passed too. Most of the time, they will not
	 * be necessary in the configuration, only in the machine creation. However, some systems
	 * may use them at this level (in particular, those that do not support user data mechanisms).
	 * </p>
	 *
	 * @param parameters the target parameters
	 * @param machineId the ID machine of the machine to configure
	 * @throws TargetException
	 */
	void configureMachine( TargetHandlerParameters parameters, String machineId )
	throws TargetException;


	/**
	 * Asks for the termination of the machine identified by its id.
	 * @param parameters the target parameters
	 * @param machineId the ID machine of the machine to terminate
	 * @throws TargetException
	 */
	void terminateMachine( TargetHandlerParameters parameters, String machineId ) throws TargetException;


	/**
	 * Determines whether a machine is running or not.
	 * <p>
	 * This method is invoked by the DM when it is restarted, to retrieve the state
	 * of the machines it used to know. States are persisted by the DM. If this method returns true,
	 * the restored state is kept and a message is sent to the agent to refresh the current statuses.
	 * If this method returns false, then the machine is considered as the associated instances are all
	 * marked as "not deployed". This method cannot make any assumption about whether the agent is running
	 * or not. It should only verify whether the machine is running or not.
	 * </p>
	 *
	 * @param parameters the target parameters
	 * @param machineId the ID machine of the machine to configure
	 * @return true if the machine is running, false otherwise
	 * @throws TargetException
	 */
	boolean isMachineRunning( TargetHandlerParameters parameters, String machineId ) throws TargetException;


	/**
	 * Retrieves a public IP address for a given machine.
	 * @param parameters the target parameters
	 * @param machineId the ID machine of the machine to configure
	 * @return an IP address (can be null)
	 * @throws TargetException
	 */
	String retrievePublicIpAddress( TargetHandlerParameters parameters, String machineId ) throws TargetException;
}
