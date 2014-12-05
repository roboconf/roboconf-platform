/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.dsl.parsing.AbstractBlockHolder;
import net.roboconf.core.dsl.parsing.BlockProperty;
import net.roboconf.core.model.helpers.VariableHelpers;

/**
 * Internal utilities related to model parsing and conversion.
 * @author Vincent Zurczak - Linagora
 */
public final class ModelUtils {

	/**
	 * Private empty constructor.
	 */
	private ModelUtils() {
		// nothing
	}


	/**
	 * Gets the value of a property.
	 * @param holder a property holder (not null)
	 * @param propertyName a property name (not null)
	 * @return the property value, which can be null, or null if the property was not found
	 */
	public static String getPropertyValue( AbstractBlockHolder holder, String propertyName ) {
		BlockProperty p = holder.findPropertyBlockByName( propertyName );
		return p == null ? null : p.getValue();
	}


	/**
	 * Gets and splits property values separated by a comma.
	 * @param holder a property holder (not null)
	 * @return a non-null list of non-null values
	 */
	public static List<String> getPropertyValues( AbstractBlockHolder holder, String propertyName ) {
		BlockProperty p = holder.findPropertyBlockByName( propertyName );
		String propertyValue = p == null ? null : p.getValue();
		return Utils.splitNicely( propertyValue, ParsingConstants.PROPERTY_SEPARATOR );
	}


	/**
	 * Gets and splits data separated by a comma.
	 * @param holder a property holder (not null)
	 * @return a non-null map (key = data name, value = data value, which can be null)
	 */
	public static Map<String,String> getData( AbstractBlockHolder holder ) {

		BlockProperty p = holder.findPropertyBlockByName( ParsingConstants.PROPERTY_INSTANCE_DATA );
		Map<String,String> result = new HashMap<String,String> ();

		String propertyValue = p == null ? null : p.getValue();
		for( String s : Utils.splitNicely( propertyValue, ParsingConstants.PROPERTY_SEPARATOR )) {
			Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
			result.put( entry.getKey(), entry.getValue());
		}

		return result;
	}


	/**
	 * Gets and splits exported variables separated by a comma.
	 * @param holder a property holder (not null)
	 * @return a non-null map (key = exported variable name, value = default value, which can be null)
	 */
	public static Map<String,String> getExportedVariables( AbstractBlockHolder holder ) {

		BlockProperty p = holder.findPropertyBlockByName( ParsingConstants.PROPERTY_GRAPH_EXPORTS );
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
