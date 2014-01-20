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
 * The 'facet' instruction.
 * @author Vincent Zurczak - Linagora
 */
public class RelationFacet extends AbstractPropertiesHolder {

	/**
	 * Constructor.
	 * @param declaringFile not null
	 */
	public RelationFacet( AbstractFile declaringFile ) {
		super( declaringFile );
	}

	@Override
	public String[] getSupportedPropertyNames() {
		return new String[] {
			Constants.CHILDREN,
			Constants.EXPORTS,
			Constants.ICON_LOCATION,
			Constants.INSTALLER,
			Constants.FACET_EXTENDS
		};
	}

	@Override
	public int getInstructionType() {
		return AbstractInstruction.FACET;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "facet " + getName();
	}
}
