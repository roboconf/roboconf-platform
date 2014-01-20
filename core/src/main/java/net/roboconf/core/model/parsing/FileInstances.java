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

package net.roboconf.core.model.parsing;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A bean that contains information about an 'instances' file.
 * @author Vincent Zurczak - Linagora
 */
public class FileInstances extends AbstractFile {

	private final List<InstanceComponent> instances = new ArrayList<InstanceComponent> ();


	/**
	 * Constructor.
	 * @param editedFile
	 */
	public FileInstances( File editedFile ) {
		super( editedFile );
	}

	/**
	 * Constructor.
	 * @param fileLocation
	 */
	public FileInstances( URI fileLocation ) {
		super( fileLocation );
	}

	/**
	 * @return the instances
	 */
	public Collection<InstanceComponent> getInstances() {
		return this.instances;
	}
}
