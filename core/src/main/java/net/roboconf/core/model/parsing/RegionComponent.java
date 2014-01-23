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
 * The 'component' instruction.
 * @author Vincent Zurczak - Linagora
 */
public class RegionComponent extends AbstractPropertiesHolder {

	/**
	 * Constructor.
	 * @param declaringFile not null
	 */
	public RegionComponent( FileDefinition declaringFile ) {
		super( declaringFile );
	}

	@Override
	public String[] getSupportedPropertyNames() {
		return new String[] {
			Constants.PROPERTY_GRAPH_CHILDREN,
			Constants.PROPERTY_GRAPH_EXPORTS,
			Constants.PROPERTY_GRAPH_ICON_LOCATION,
			Constants.PROPERTY_GRAPH_INSTALLER,
			Constants.PROPERTY_COMPONENT_ALIAS,
			Constants.PROPERTY_COMPONENT_FACETS,
			Constants.PROPERTY_COMPONENT_IMPORTS
		};
	}

	@Override
	public int getInstructionType() {
		return AbstractRegion.COMPONENT;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}
}
