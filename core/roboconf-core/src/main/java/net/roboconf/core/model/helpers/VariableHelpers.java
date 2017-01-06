/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.helpers;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;

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
	 * If the variable name was not prefixed by a component or a facet name, then
	 * the couple ( "", &lt; originalVariableName &gt; ) is returned.
	 * </p>
	 *
	 * @param variableName a variable name (not null)
	 * @return a map entry (key = facet or component name, value = simple name)
	 */
	public static Map.Entry<String,String> parseVariableName( String variableName ) {

		String componentOrFacetName = "", simpleName = variableName;
		int index = variableName.indexOf( '.' );
		if( index >= 0 ) {
			componentOrFacetName = variableName.substring( 0, index ).trim();
			simpleName = variableName.substring( index + 1 ).trim();
		}

		return new AbstractMap.SimpleEntry<>( componentOrFacetName, simpleName );
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

		return new AbstractMap.SimpleEntry<>( varName, defaultValue );
	}


	/**
	 * Parse a list of exported variables.
	 * @param line a non-null line
	 * @return a non-null map of exported variables
	 */
	public static Map<String,ExportedVariable> parseExportedVariables( String line ) {

		Pattern randomPattern = Pattern.compile( ParsingConstants.PROPERTY_GRAPH_RANDOM_PATTERN, Pattern.CASE_INSENSITIVE );
		Pattern varPattern = Pattern.compile( "([^,=]+)(\\s*=\\s*(\"([^\",]+)\"|([^,]+)))?" );

		Map<String,ExportedVariable> result = new LinkedHashMap<> ();
		Matcher varMatcher = varPattern.matcher( line );
		while( varMatcher.find()) {

			String key = varMatcher.group( 1 ).trim();
			if( Utils.isEmptyOrWhitespaces( key ))
				continue;

			String value = null;
			if( varMatcher.group( 3 ) != null ) {

				// We do not always trim!
				// Surrounding white spaces are kept when defined between quotes.
				if( varMatcher.group( 5 ) != null )
					value = varMatcher.group( 5 ).trim();
				else
					value = varMatcher.group( 4 );
			}

			ExportedVariable var = new ExportedVariable();
			Matcher m = randomPattern.matcher( key );
			if( m.matches()) {
				var.setRandom( true );
				var.setRawKind( m.group( 1 ));
				key = m.group( 2 ).trim();
			}

			var.setName( key.trim());
			if( value != null )
				var.setValue( value );

			result.put( var.getName(), var );
		}

		return result;
	}


	/**
	 * Finds the component and facet names that prefix the variables an instance exports.
	 * @param instance an instance
	 * @return a non-null set with all the component and facet names this instance exports
	 */
	public static Set<String> findPrefixesForExportedVariables( Instance instance ) {

		Set<String> result = new HashSet<> ();
		for( String exportedVariableName : InstanceHelpers.findAllExportedVariables( instance ).keySet())
			result.add( VariableHelpers.parseVariableName( exportedVariableName ).getKey());

		return result;
	}


	/**
	 * Finds the component and facet names that prefix the variables an instance imports.
	 * @param instance an instance
	 * @return a non-null set with all the component and facet names this instance imports
	 */
	public static Set<String> findPrefixesForImportedVariables( Instance instance ) {
		Set<String> result = new HashSet<> ();

		for( ImportedVariable var : ComponentHelpers.findAllImportedVariables( instance.getComponent()).values())
			result.add( VariableHelpers.parseVariableName( var.getName()).getKey());

		return result;
	}


	/**
	 * Finds the component and facet names that prefix the variables an instance requires.
	 * <p>
	 * Only the mandatory variables are returned. Optional imports are not considered by this method.
	 * </p>
	 *
	 * @param instance an instance
	 * @return a non-null set with all the component and facet names this instance imports
	 */
	public static Set<String> findPrefixesForMandatoryImportedVariables( Instance instance ) {
		Set<String> result = new HashSet<> ();

		for( ImportedVariable var : ComponentHelpers.findAllImportedVariables( instance.getComponent()).values()) {
			if( ! var.isOptional())
				result.add( VariableHelpers.parseVariableName( var.getName()).getKey());
		}

		return result;
	}


	/**
	 * Finds the prefixes of the external variables for a given application or template.
	 * <p>
	 * If this list is not empty, it means this application depends on
	 * other ones.
	 * </p>
	 *
	 * @param app an application or an application template
	 * @return a non-null application
	 */
	public static Set<String> findPrefixesForExternalImports( AbstractApplication app ) {

		Set<String> result = new TreeSet<> ();
		for( Component c : ComponentHelpers.findAllComponents( app )) {
			for( ImportedVariable var : ComponentHelpers.findAllImportedVariables( c ).values()) {
				if( ! var.isExternal())
					continue;

				String prefix = VariableHelpers.parseVariableName( var.getName()).getKey();
				result.add( prefix );
			}
		}

		return result;
	}


	/**
	 * Updates the exports of an instance with network values.
	 * <p>
	 * For the moment, only IP is supported.
	 * </p>
	 *
	 * @param instanceExports a non-null map of instance exports
	 * @param ipAddress the IP address to set
	 */
	static void updateNetworkVariables( Map<String,String> instanceExports, String ipAddress ) {

		// Find the keys to update ( xxx.ip )
		Set<String> keysToUpdate = new HashSet<> ();
		for( Map.Entry<String,String> entry : instanceExports.entrySet()) {
			String suffix = parseVariableName( entry.getKey()).getValue();
			if( Constants.SPECIFIC_VARIABLE_IP.equalsIgnoreCase( suffix ))
				keysToUpdate.add( entry.getKey());
		}

		// Update them
		for( String key : keysToUpdate )
			instanceExports.put( key, ipAddress );
	}
}
