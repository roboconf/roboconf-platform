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

package net.roboconf.core.internal.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.parsing.AbstractBlockHolder;
import net.roboconf.core.model.parsing.BlockProperty;
import net.roboconf.core.model.parsing.ParsingConstants;

/**
 * Internal utilities related to model parsing and conversion.
 * @author Vincent Zurczak - Linagora
 */
public class ModelUtils {

	public static String getPropertyValue( AbstractBlockHolder holder, String propertyName ) {
		BlockProperty p = holder.findPropertyBlockByName( propertyName );
		return p == null ? null : p.getValue();
	}


	public static Collection<String> getPropertyValues( AbstractBlockHolder holder, String propertyName ) {
		BlockProperty p = holder.findPropertyBlockByName( propertyName );
		String propertyValue = p == null ? null : p.getValue();
		return Utils.splitNicely( propertyValue, ParsingConstants.PROPERTY_SEPARATOR );
	}


	public static Map<String,String> getExportedVariables( AbstractBlockHolder holder ) {
		BlockProperty p = holder.findPropertyBlockByName( Constants.PROPERTY_GRAPH_EXPORTS );
		Map<String,String> result = new HashMap<String,String> ();

		String propertyValue = p == null ? null : p.getValue();
		for( String s : Utils.splitNicely( propertyValue, ParsingConstants.PROPERTY_SEPARATOR )) {
			Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
			// Prefix with the facet or component name.
			result.put( holder.getName() + "." + entry.getKey(), entry.getValue());
		}

		return result;
	}
}
