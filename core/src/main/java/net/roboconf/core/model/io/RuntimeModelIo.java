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

package net.roboconf.core.model.io;

import java.io.File;
import java.util.Collection;

import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.runtime.Application;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelIo {

	public static LoadResult loadApplication( File projectDirectory ) {
		// TODO:
		return null;
	}


	public static LoadResult loadApplicationFromArchive( File zipFile ) {
		// TODO: Unzip the archive in a temporary directory, load it and delete it
		return null;
	}


	/**
	 * A bean that stores both the application and loading errors.
	 */
	public static class LoadResult {
		Application application;
		Collection<ModelError> loadErrors;

		public Application getApplication() {
			return this.application;
		}

		public Collection<ModelError> getLoadErrors() {
			return this.loadErrors;
		}
	}
}
