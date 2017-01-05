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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;

/**
 * A set of helpers for instance imports.
 * @author Vincent Zurczak - Linagora
 */
public final class ImportHelpers {

	/**
	 * Constructor.
	 */
	private ImportHelpers() {
		// nothing
	}


	/**
	 * Determines whether an instance has all the imports it needs.
	 * <p>
	 * Master of Obvious said:<br>
	 * By definition, optional imports are not considered to be required.
	 * </p>
	 *
	 * @param instance a non-null instance
	 * @param logger a logger (can be null)
	 * @return true if all its (mandatory) imports are resolved, false otherwise
	 */
	public static boolean hasAllRequiredImports( Instance instance, Logger logger ) {

		boolean haveAllImports = true;
		for( String facetOrComponentName : VariableHelpers.findPrefixesForMandatoryImportedVariables( instance )) {
			Collection<Import> imports = instance.getImports().get( facetOrComponentName );
			if( imports != null && ! imports.isEmpty())
				continue;

			haveAllImports = false;
			if( logger != null )
				logger.fine( InstanceHelpers.computeInstancePath( instance ) + " is still missing dependencies '" + facetOrComponentName + ".*'." );

			break;
		}

		return haveAllImports;
	}


	/**
	 * Builds an import where only the right variables are contained.
	 * <p>
	 * As an example, if a component imports Toto.var1 and Toto.var2, and that
	 * Toto exports var1, var2 and var3, then the component only needs to see
	 * var1 and var2 in its imports. No need to keep var3.
	 * </p>
	 * <p>
	 * This method also deals with wild card imports.
	 * </p>
	 *
	 * @param instanceThatUseTheImport the instance that will use the import
	 * @param exportingInstancePath the path of the instance that exports variables
	 * @param exportingInstanceComponent the component name of the instance that exports variables
	 * @param exportedVariables the variables to add in the imports
	 * @return a non-null import
	 */
	public static Import buildTailoredImport(
			Instance instanceThatUseTheImport,
			String exportingInstancePath,
			String exportingInstanceComponent,
			Map<String,String> exportedVariables ) {

		Import imp = new Import( exportingInstancePath, exportingInstanceComponent );
		if( exportedVariables != null && ! exportedVariables.isEmpty()) {

			Component comp = instanceThatUseTheImport.getComponent();
			for( ImportedVariable var : ComponentHelpers.findAllImportedVariables( comp ).values()) {
				String importedVariable = var.getName();

				// Deal with imports
				if( var.getName().endsWith( "." + Constants.WILDCARD )) {
					String prefix = VariableHelpers.parseVariableName( importedVariable ).getKey();
					for( Map.Entry<String,String> entry : exportedVariables.entrySet()) {
						String exportedVariable = entry.getKey();
						if( Utils.isEmptyOrWhitespaces( exportedVariable ))
							continue;

						String exportedVarPrefix = VariableHelpers.parseVariableName( exportedVariable ).getKey();
						if( prefix.equals( exportedVarPrefix ))
							imp.getExportedVars().put( entry.getKey(), entry.getValue());
					}
				}

				// Deal with the usual (and most simple!) case
				else if( exportedVariables.containsKey( importedVariable ))
					imp.getExportedVars().put( importedVariable, exportedVariables.get( importedVariable ));
			}
		}

		return imp;
	}


	/**
	 * Adds an import to an instance (provided this import was not already set).
	 * @param instance the instance whose imports must be updated
	 * @param componentOrFacetName the component or facet name associated with the import
	 * @param imp the import to add
	 */
	public static void addImport( Instance instance, String componentOrFacetName, Import imp ) {

		Collection<Import> imports = instance.getImports().get( componentOrFacetName );
		if(imports == null) {
			imports = new LinkedHashSet<Import> ();
			instance.getImports().put( componentOrFacetName, imports );
		}

		if( ! imports.contains( imp ))
			imports.add( imp );
	}


	/**
	 * Updates the imports of an instance with new values.
	 * @param instance the instance whose imports must be updated
	 * @param variablePrefixToImports the new imports (can be null)
	 */
	public static void updateImports( Instance instance, Map<String,Collection<Import>> variablePrefixToImports ) {
		instance.getImports().clear();
		if( variablePrefixToImports != null )
			instance.getImports().putAll( variablePrefixToImports );
	}


	/**
	 * Finds a specific import from the path of the instance that exports it.
	 * @param imports a collection of imports (that can be null)
	 * @param exportingInstancePath the path of the exporting instance
	 * @return an import, or null if none was found
	 */
	public static Import findImportByExportingInstance( Collection<Import> imports, String exportingInstancePath ) {

		Import result = null;
		if( imports != null && exportingInstancePath != null ) {
			for( Import imp : imports ) {
				if( exportingInstancePath.equals( imp.getInstancePath())) {
					result = imp;
					break;
				}
			}
		}

		return result;
	}
}
