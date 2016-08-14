/**
 * Copyright 2013-2016 Linagora, Université Joseph Fourier, Floralis
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
	 * @param scopedInstance the scoped instance
	 * @throws TargetException
	 */
	void configureMachine( TargetHandlerParameters parameters, String machineId, Instance scopedInstance )
	throws TargetException;


	/**
	 * Asks for the termination of the machine identified by its id.
	 * @param targetProperties the target properties (e.g. access key, secret key, etc.)
	 * @param machineId the ID machine of the machine to terminate
	 * @throws TargetException
	 */
	void terminateMachine( Map<String,String> targetProperties, String machineId ) throws TargetException;


	/**
	 * Determines whether a machine is running or not.
	 * <p>
	 * This method is invoked by the DM when it is restarted, to retrieve the state
	 * of the machines it used to know.
	 * </p>
	 *
	 * @param targetProperties the target properties (e.g. access key, secret key, etc.)
	 * @param machineId the ID machine of the machine to configure
	 * @throws TargetException
	 */
	boolean isMachineRunning( Map<String,String> targetProperties, String machineId ) throws TargetException;
}
