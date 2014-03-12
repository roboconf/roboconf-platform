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

package net.roboconf.dm.management;

import java.io.File;
import java.util.logging.Logger;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.messaging.client.IMessageServerClient;

/**
 * A class to store runtime information for an application.
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public class ManagedApplication {

	private final Application application;
	private final File applicationFilesDirectory;
	private final Logger logger;

	private final MachineMonitor monitor;
	private final IMessageServerClient messagingClient;


	/**
	 * Constructor.
	 */
	public ManagedApplication(
			Application application,
			File applicationFilesDirectory,
			IMessageServerClient messagingClient ) {

		this.applicationFilesDirectory = applicationFilesDirectory;
		this.application = application;
		this.messagingClient = messagingClient;

		this.monitor = new MachineMonitor( application );
		this.logger = Logger.getLogger( Manager.class.getName() + "." + application.getName());
	}


	public File getApplicationFilesDirectory() {
		return this.applicationFilesDirectory;
	}


	public Application getApplication() {
		return this.application;
	}


	public MachineMonitor getMonitor() {
		return this.monitor;
	}


	public IMessageServerClient getMessagingClient() {
		return this.messagingClient;
	}


	public Logger getLogger() {
		return this.logger;
	}
}