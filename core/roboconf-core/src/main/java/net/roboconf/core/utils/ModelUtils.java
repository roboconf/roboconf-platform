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

package net.roboconf.core.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.dsl.parsing.AbstractBlockHolder;
import net.roboconf.core.dsl.parsing.BlockProperty;
import net.roboconf.core.internal.dsl.parsing.ExportedVariablesParser;
import net.roboconf.core.model.beans.ExportedVariable;
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

		List<String> result = new ArrayList<> ();
		for( BlockProperty p : holder.findPropertiesBlockByName( propertyName )) {
			result.addAll( Utils.splitNicely( p.getValue(), ParsingConstants.PROPERTY_SEPARATOR ));
		}

		return result;
	}


	/**
	 * Gets and splits data separated by a comma.
	 * @param holder a property holder (not null)
	 * @return a non-null map (key = data name, value = data value, which can be null)
	 */
	public static Map<String,String> getData( AbstractBlockHolder holder ) {

		BlockProperty p = holder.findPropertyBlockByName( ParsingConstants.PROPERTY_INSTANCE_DATA );
		Map<String,String> result = new HashMap<> ();

		String propertyValue = p == null ? null : p.getValue();
		for( String s : Utils.splitNicely( propertyValue, ParsingConstants.PROPERTY_SEPARATOR )) {
			Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
			result.put( entry.getKey(), entry.getValue());
		}

		return result;
	}


	/**
	 * Gets and splits exported variables separated by a comma.
	 * <p>
	 * Variable names are not prefixed by the type's name.<br />
	 * Variable values may also be surrounded by quotes.
	 * </p>
	 *
	 * @param holder a property holder (not null)
	 * @return a non-null map (key = exported variable name, value = the exported variable)
	 */
	public static Map<String,ExportedVariable> getExportedVariables( AbstractBlockHolder holder ) {

		Map<String,ExportedVariable> result = new HashMap<> ();
		for( BlockProperty p : holder.findPropertiesBlockByName( ParsingConstants.PROPERTY_GRAPH_EXPORTS )) {
			result.putAll( findExportedVariables( p.getValue(), p.getFile(), p.getLine()));
		}

		return result;
	}


	/**
	 * Gets and splits exported variables separated by a comma.
	 * <p>
	 * Variable names are not prefixed by the type's name.<br />
	 * Variable values may also be surrounded by quotes.
	 * </p>
	 *
	 * @param exportedVariablesDecl the declaration to parse
	 * @param sourceFile the source file
	 * @param lineNumber the line number
	 * @return a non-null map (key = exported variable name, value = the exported variable)
	 */
	public static Map<String,ExportedVariable> findExportedVariables( String exportedVariablesDecl, File sourceFile, int lineNumber ) {

		Map<String,ExportedVariable> result = new HashMap<> ();
		Pattern pattern = Pattern.compile( ParsingConstants.PROPERTY_GRAPH_RANDOM_PATTERN, Pattern.CASE_INSENSITIVE );
		ExportedVariablesParser exportsParser = new ExportedVariablesParser();
		exportsParser.parse( exportedVariablesDecl, sourceFile, lineNumber );

		for( Map.Entry<String,String> entry : exportsParser.rawNameToVariables.entrySet()) {
			ExportedVariable var = new ExportedVariable();
			String variableName = entry.getKey();

			Matcher m = pattern.matcher( variableName );
			if( m.matches()) {
				var.setRandom( true );
				var.setRawKind( m.group( 1 ));
				variableName = m.group( 2 ).trim();
			}

			var.setName( variableName );
			var.setValue( entry.getValue());
			result.put( var.getName(), var );
		}

		return result;
	}
}
