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

package net.roboconf.core.model.helpers;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Helpers related to variables.
 * @author Vincent Zurczak - Linagora
 */
public final class VariableHelpers {

	/**
	 * Private empty constructor.
	 */
	private VariableHelpers() {
		// nothing
	}


	/**
	 * Parses a variable name (&lt;facetOrComponentName&gt;.&lt;simpleName&gt;).
	 * <p>
	 * All the variables (imported, or exported - after resolution) must be
	 * prefixed by a component or facet name.
	 * </p>
	 * <p>
	 * It is assumed the variable name has been validated before calling this method.
	 * </p>
	 *
	 * @param variableName a variable name (not null)
	 * @return a map entry (key = facet or component name, value = simple name)
	 */
	public static Map.Entry<String,String> parseVariableName( String variableName ) {

		String componentOrFacetName = variableName, simpleName = "";
		int index = variableName.indexOf( '.' );
		if( index >= 0 ) {
			componentOrFacetName = variableName.substring( 0, index ).trim();
			simpleName = variableName.substring( index + 1 ).trim();
		}

		return new AbstractMap.SimpleEntry<String,String>( componentOrFacetName, simpleName );
	}


	/**
	 * Parses an exported variable (&lt;variableName&gt; = &lt;defaultValue&gt;).
	 * <p>
	 * The equal symbol and default value are optional.
	 * </p>
	 *
	 * @param exportedVariable an exported variable (not null)
	 * @return a map entry (key = variable name, value = default value)
	 */
	public static Map.Entry<String,String> parseExportedVariable( String exportedVariable ) {

		int index = exportedVariable.indexOf( '=' );
		String varName = exportedVariable, defaultValue = null;
		if( index > 0 ) {
			varName = exportedVariable.substring( 0, index ).trim();
			defaultValue = exportedVariable.substring( index + 1 ).trim();
		}

		return new AbstractMap.SimpleEntry<String,String>( varName, defaultValue );
	}
}
