/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model;

import java.io.File;

/**
 * A class to find where a given model object was defined.
 * <p>
 * This class makes sense for parsing to determine where a runtime
 * error is located in source files.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class SourceReference {

	private final Object modelObject;
	private final File sourceFile;
	private final int line;


	/**
	 * Constructor.
	 * @param modelObject
	 * @param sourceFile
	 * @param line
	 */
	public SourceReference( Object modelObject, File sourceFile, int line ) {
		this.modelObject = modelObject;
		this.sourceFile = sourceFile;
		this.line = line;
	}


	/**
	 * @return the modelObject
	 */
	public Object getModelObject() {
		return this.modelObject;
	}


	/**
	 * @return the sourceFile
	 */
	public File getSourceFile() {
		return this.sourceFile;
	}


	/**
	 * @return the line
	 */
	public int getLine() {
		return this.line;
	}
}
