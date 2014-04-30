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

import net.roboconf.core.Constants;
import net.roboconf.core.utils.Utils;

/**
 * The 'instanceof' block.
 * @author Vincent Zurczak - Linagora
 */
public class BlockInstanceOf extends AbstractBlockHolder {

	/**
	 * Constructor.
	 * @param declaringFile not null
	 */
	public BlockInstanceOf( FileDefinition declaringFile ) {
		super( declaringFile );
	}

	@Override
	public String[] getSupportedPropertyNames() {
		return new String[] {
			Constants.PROPERTY_INSTANCE_NAME,
			Constants.PROPERTY_INSTANCE_CHANNEL
		};
	}

	@Override
	public int getInstructionType() {
		return AbstractBlock.INSTANCEOF;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		// <componentName> as <instanceName>
		BlockProperty p = findPropertyBlockByName( Constants.PROPERTY_INSTANCE_NAME );
		StringBuilder sb = new StringBuilder();
		String componentName = getName();
		if( ! Utils.isEmptyOrWhitespaces( componentName ))
			sb.append( componentName );
		else
			sb.append( "Unspecified component" );

		if( p != null
				&& ! Utils.isEmptyOrWhitespaces( p.getValue())) {
			sb.append( " as " );
			sb.append( p.getValue());
		}

		return sb.toString();
	}
}
