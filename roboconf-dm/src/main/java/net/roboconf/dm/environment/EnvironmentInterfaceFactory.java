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

/**
 * @author Vincent Zurczak - Linagora
 */
public class EnvironmentInterfaceFactory {

	/**
	 * Creates a environment interface.
	 * <p>
	 * This method will be overridden in tests.
	 * </p>
	 *
	 * @param messageServerIp
	 * @param application
	 * @param applicationFilesDirectory
	 * @return a non-null interface for the environment
	 */
	public IEnvironmentInterface create( String messageServerIp, Application application, File applicationFilesDirectory ) {

		IEnvironmentInterface env = new DmEnvironmentInterface();
		env.setApplication( application );
		env.setApplicationFilesDirectory( applicationFilesDirectory );
		env.setMessageServerIp( messageServerIp );

		return env;
	}
}
