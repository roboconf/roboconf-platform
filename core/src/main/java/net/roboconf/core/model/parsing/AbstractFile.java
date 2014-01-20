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

import net.roboconf.core.model.ModelError;

/**
 * A bean that contains information about a configuration file.
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractFile {

	private File editedFile;
	private final URI fileLocation;
	private final Collection<ModelError> parsingErrors = new ArrayList<ModelError> ();


	/**
	 * Constructor.
	 * @param editedFile
	 */
	public AbstractFile( File editedFile ) {
		this.editedFile = editedFile;
		this.fileLocation = editedFile.toURI();
	}

	/**
	 * Constructor.
	 * @param fileLocation
	 */
	public AbstractFile( URI fileLocation ) {
		this.fileLocation = fileLocation;
	}

	/**
	 * @param editedFile the editedFile to set
	 */
	public void setEditedFile( File editedFile ) {
		this.editedFile = editedFile;
	}

	/**
	 * @return the editedFile
	 */
	public File getEditedFile() {
		return this.editedFile;
	}

	/**
	 * @return the fileLocation
	 */
	public URI getFileLocation() {
		return this.fileLocation;
	}

	/**
	 * @return the parsingErrors
	 */
	public Collection<ModelError> getParsingErrors() {
		return this.parsingErrors;
	}
}
