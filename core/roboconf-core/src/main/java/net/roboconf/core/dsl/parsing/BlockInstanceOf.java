/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.dsl.parsing;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.utils.Utils;

/**
 * The 'instance of' block.
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
			ParsingConstants.PROPERTY_INSTANCE_NAME,
			ParsingConstants.PROPERTY_INSTANCE_CHANNELS,
			ParsingConstants.PROPERTY_INSTANCE_COUNT,
			ParsingConstants.PROPERTY_INSTANCE_DATA,
			ParsingConstants.PROPERTY_INSTANCE_STATE
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

		BlockProperty p = findPropertyBlockByName( ParsingConstants.PROPERTY_INSTANCE_NAME );
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
