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

/**
 * The instruction for an import.
 * @author Vincent Zurczak - Linagora
 */
public class RegionImport extends AbstractRegion {

	private String uri;


	/**
	 * Constructor.
	 * @param declaringFile
	 */
	public RegionImport( FileDefinition declaringFile ) {
		super( declaringFile );
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri( String uri ) {
		this.uri = uri;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "import " + this.uri + ";";
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.model.parsing.AbstractRegion#getInstructionType()
	 */
	@Override
	public int getInstructionType() {
		return AbstractRegion.IMPORT;
	}
}
