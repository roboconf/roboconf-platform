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

package net.roboconf.dm.internal.management;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.ManagerConfiguration;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ManagementHelpers {

	/**
	 * Empty private constructor.
	 */
	private ManagementHelpers() {
		// nothing
	}


	/**
	 * @return a new manager
	 */
	public static Manager createConfiguredManager() {

		ManagerConfiguration conf = new ManagerConfiguration();
		Manager manager = new Manager();

		manager.setConfiguration( conf );
		conf.setManager( manager );

		return manager;
	}


	/**
	 * @return a new manager
	 */
	public static Manager createConfiguredManager(
			String messageServerIp,
			String messageServerUsername,
			String messageServerPassword,
			String configurationDirectoryLocation ) {

		ManagerConfiguration conf = new ManagerConfiguration(
				messageServerIp,
				messageServerUsername,
				messageServerPassword,
				configurationDirectoryLocation );

		Manager manager = new Manager();
		manager.setConfiguration( conf );
		conf.setManager( manager );

		return manager;
	}
}
