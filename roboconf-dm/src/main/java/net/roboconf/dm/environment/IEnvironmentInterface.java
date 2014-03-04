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

package net.roboconf.dm.environment;

import java.io.File;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.messaging.messages.Message;

/**
 * An interface to interact with the environment.
 * <p>
 * Basically, it is used to abstract (and allow mocking) of
 * message queues and IaaS environments.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IEnvironmentInterface {

	/**
	 * Sets the message server IP.
	 * @param messageServerIp the message server IP
	 */
	void setMessageServerIp( String messageServerIp );

	/**
	 * Sets the application associated with this instance.
	 * @param application an application
	 */
	void setApplication( Application application );

	/**
	 * Sets the directory where resources for this application are stored.
	 * @param applicationFilesDirectory an existing directory
	 */
	void setApplicationFilesDirectory( File applicationFilesDirectory );

	/**
	 * Initializes the resources managed by this class (listeners, clients, etc).
	 */
	void initializeResources();

	/**
	 * Cleans up the resources managed by this class (listeners, clients, etc).
	 */
	void cleanResources();

	/**
	 * Sends a message to a machine's agent.
	 * @param message the message to send
	 * @param rootInstance the root instance associated with the machine
	 */
	void sendMessage( Message message, Instance rootInstance );

	/**
	 * Terminates a machine.
	 * @param rootInstance the root instance associated with the machine
	 * @throws IaasException if something went wrong
	 */
	void terminateMachine( Instance rootInstance ) throws IaasException;

	/**
	 * Creates a machine.
	 * @param rootInstance the root instance associated with the machine
	 * @throws IaasException if something went wrong
	 */
	void createMachine( Instance rootInstance ) throws IaasException;
}