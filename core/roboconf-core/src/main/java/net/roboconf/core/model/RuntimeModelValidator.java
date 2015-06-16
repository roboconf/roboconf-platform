/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;

/**
 * A set of methods to validate runtime model objects.
 * @author Vincent Zurczak - Linagora
 */
public final class RuntimeModelValidator {

	/**
	 * Constructor.
	 */
	private RuntimeModelValidator() {
		// nothing
	}


	/**
	 * Validates a component.
	 * <p>
	 * Associated facets, extended components, children and ancestors,
	 * are not validated by this method.
	 * </p>
	 *
	 * @param component a component
	 * @return a non-null list of errors
	 */
	public static Collection<ModelError> validate( Component component ) {
		Collection<ModelError> errors = new ArrayList<ModelError> ();

		// Check the name
		if( Utils.isEmptyOrWhitespaces( component.getName()))
			errors.add( new ModelError( ErrorCode.RM_EMPTY_COMPONENT_NAME, component ));
		else if( ! component.getName().matches( ParsingConstants.PATTERN_FLEX_ID ))
			errors.add( new ModelError( ErrorCode.RM_INVALID_COMPONENT_NAME, component, "Component name: " + component ));
		else if( component.getName().contains( "." ))
			errors.add( new ModelError( ErrorCode.RM_DOT_IS_NOT_ALLOWED, component, "Component name: " + component ));

		// Check the installer
		String installerName = ComponentHelpers.findComponentInstaller( component );
		if( Utils.isEmptyOrWhitespaces( installerName ))
			errors.add( new ModelError( ErrorCode.RM_EMPTY_COMPONENT_INSTALLER, component, "Component name: " + component ));
		else if( ! installerName.matches( ParsingConstants.PATTERN_FLEX_ID ))
			errors.add( new ModelError( ErrorCode.RM_INVALID_COMPONENT_INSTALLER, component, "Component name: " + component ));

		else if( ComponentHelpers.findAllAncestors( component ).isEmpty()
				&& ! Constants.TARGET_INSTALLER.equals( installerName ))
			errors.add( new ModelError( ErrorCode.RM_ROOT_INSTALLER_MUST_BE_TARGET, component, "Component name: " + component ));

		// Check the name of exported variables
		for( Map.Entry<String,String> entry : component.exportedVariables.entrySet()) {
			String exportedVarName = entry.getKey();

			if( Utils.isEmptyOrWhitespaces( exportedVarName ))
				errors.add( new ModelError( ErrorCode.RM_EMPTY_VARIABLE_NAME, component, "Variable name: " + exportedVarName ));
			else if( ! exportedVarName.matches( ParsingConstants.PATTERN_ID ))
				errors.add( new ModelError( ErrorCode.RM_INVALID_VARIABLE_NAME, component, "Variable name: " + exportedVarName ));

			else if( Utils.isEmptyOrWhitespaces( entry.getValue())
					&& ! Constants.SPECIFIC_VARIABLE_IP.equalsIgnoreCase( exportedVarName )
					&& ! exportedVarName.toLowerCase().endsWith( "." + Constants.SPECIFIC_VARIABLE_IP ))
				errors.add( new ModelError( ErrorCode.RM_MISSING_VARIABLE_VALUE, component, "Variable name: " + exportedVarName ));
		}

		// A component cannot import variables it exports unless these imports are optional.
		// This covers cluster uses cases (where an element may want to know where are the similar nodes).
		Map<String,String> allExportedVariables = ComponentHelpers.findAllExportedVariables( component );
		for( Map.Entry<String,Boolean> entry : ComponentHelpers.findAllImportedVariables( component ).entrySet()) {

			String var = entry.getKey();
			String patternForImports = ParsingConstants.PATTERN_ID;
			patternForImports += "(\\.\\*)?";

			if( Utils.isEmptyOrWhitespaces( var ))
				errors.add( new ModelError( ErrorCode.RM_EMPTY_VARIABLE_NAME, component, "Variable name: " + var ));
			else if( ! var.matches( patternForImports ))
				errors.add( new ModelError( ErrorCode.RM_INVALID_VARIABLE_NAME, component, "Variable name: " + var ));

			// If the import is optional...
			if( entry.getValue())
				continue;

			if( allExportedVariables.containsKey( var ))
				errors.add( new ModelError( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, component, "Variable name: " + var ));
		}

		// No cycle in inheritance
		String errorMsg = ComponentHelpers.searchForInheritanceCycle( component );
		if( errorMsg != null )
			errors.add( new ModelError( ErrorCode.RM_CYCLE_IN_COMPONENTS_INHERITANCE, component, errorMsg ));

		// Containment Cycles?
		errorMsg = ComponentHelpers.searchForLoop( component );
		if( errorMsg != null && errorMsg.startsWith( component.getName()))
			errors.add( new ModelError( ErrorCode.RM_CYCLE_IN_COMPONENTS, component, errorMsg ));

		return errors;
	}


	/**
	 * Validates a facet.
	 * <p>
	 * Extended facets, associated components, children and ancestors,
	 * are not validated by this method.
	 * </p>
	 *
	 * @param facet a facet
	 * @return a non-null list of errors
	 */
	public static Collection<ModelError> validate( Facet facet ) {

		// Check the name
		Collection<ModelError> result = new ArrayList<ModelError> ();
		if( Utils.isEmptyOrWhitespaces( facet.getName()))
			result.add( new ModelError( ErrorCode.RM_EMPTY_FACET_NAME, facet ));
		else if( ! facet.getName().matches( ParsingConstants.PATTERN_FLEX_ID ))
			result.add( new ModelError( ErrorCode.RM_INVALID_FACET_NAME, facet, "Facet name: " + facet ));
		else if( facet.getName().contains( "." ))
			result.add( new ModelError( ErrorCode.RM_DOT_IS_NOT_ALLOWED, facet, "Facet name: " + facet ));

		// Check the name of exported variables
		for( Map.Entry<String,String> entry : facet.exportedVariables.entrySet()) {
			String exportedVarName = entry.getKey();

			if( Utils.isEmptyOrWhitespaces( exportedVarName ))
				result.add( new ModelError( ErrorCode.RM_EMPTY_VARIABLE_NAME, facet, "Variable name: " + exportedVarName ));
			else if( ! exportedVarName.matches( ParsingConstants.PATTERN_ID ))
				result.add( new ModelError( ErrorCode.RM_INVALID_VARIABLE_NAME, facet, "Variable name: " + exportedVarName ));

			else if( Utils.isEmptyOrWhitespaces( entry.getValue())
					&& ! Constants.SPECIFIC_VARIABLE_IP.equalsIgnoreCase( exportedVarName )
					&& ! exportedVarName.toLowerCase().endsWith( Constants.SPECIFIC_VARIABLE_IP ))
				result.add( new ModelError( ErrorCode.RM_MISSING_VARIABLE_VALUE, facet, "Variable name: " + exportedVarName ));
		}

		// Look for cycles in inheritance
		String errorMsg = ComponentHelpers.searchForInheritanceCycle( facet );
		if( errorMsg != null )
			result.add( new ModelError( ErrorCode.RM_CYCLE_IN_FACETS_INHERITANCE, facet, errorMsg ));

		return result;
	}


	/**
	 * Validates graph resources.
	 * @param graphs the graph(s)
	 * @param projectDirectory the project's directory
	 * @return a non-null collection of errors
	 */
	public static Collection<ModelError> validate( Graphs graphs, File projectDirectory ) {

		Collection<ModelError> result = new ArrayList<ModelError> ();
		for( Component c : ComponentHelpers.findAllComponents( graphs )) {
			File componentDirectory = ResourceUtils.findInstanceResourcesDirectory( projectDirectory, c );
			if( ! componentDirectory.exists()) {
				ModelError error = new ModelError( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, c );
				error.setDetails( "Component name: " + c.getName());
				result.add( error );

			} else if( Constants.TARGET_INSTALLER.equalsIgnoreCase( c.getInstallerName())) {
				File targetPropertiesFile = new File( componentDirectory, Constants.TARGET_PROPERTIES_FILE_NAME );
				if( ! targetPropertiesFile.exists()) {
					ModelError error = new ModelError( ErrorCode.PROJ_NO_TARGET_PROPERTIES, c, "Component name: " + c.getName());
					result.add( error );
				}

			} else {
				result.addAll( RecipesValidator.validateComponentRecipes( projectDirectory, c ));
			}
		}

		return result;
	}


	/**
	 * Validates a graph.
	 * @param graphs a graphs instance
	 * @return a non-null list of errors
	 */
	public static Collection<ModelError> validate( Graphs graphs ) {

		Collection<ModelError> errors = new ArrayList<ModelError> ();
		if( graphs.getRootComponents().isEmpty())
			errors.add( new ModelError( ErrorCode.RM_NO_ROOT_COMPONENT, graphs ));

		for( Component rootComponent : graphs.getRootComponents()) {
			if( ! ComponentHelpers.findAllAncestors( rootComponent ).isEmpty())
				errors.add( new ModelError( ErrorCode.RM_NOT_A_ROOT_COMPONENT, rootComponent, "Component name: " + rootComponent ));
		}

		// Validate all the components
		// Prepare the verification of variable matching
		Map<String,Boolean> importedVariableNameToExported = new HashMap<String,Boolean> ();
		Map<String,List<Component>> importedVariableToImporters = new HashMap<String,List<Component>> ();
		for( Component component : ComponentHelpers.findAllComponents( graphs )) {

			// Basic checks
			errors.addAll( validate( component ));
			for( Facet facet : ComponentHelpers.findAllFacets( component ))
				errors.addAll( validate( facet ));

			// Process the variables
			for( String importedVariableName : ComponentHelpers.findAllImportedVariables( component ).keySet()) {
				if( ! importedVariableNameToExported.containsKey( importedVariableName ))
					importedVariableNameToExported.put( importedVariableName, Boolean.FALSE );

				List<Component> importers = importedVariableToImporters.get( importedVariableName );
				if( importers == null )
					importers = new ArrayList<Component> ();

				importers.add( component );
				importedVariableToImporters.put( importedVariableName, importers );
			}

			for( String exportedVariableName : ComponentHelpers.findAllExportedVariables( component ).keySet()) {
				importedVariableNameToExported.put( exportedVariableName, Boolean.TRUE );

				// Also add "prefix.*" in the map (for wild cards...)
				String prefix = VariableHelpers.parseVariableName( exportedVariableName ).getKey();
				importedVariableNameToExported.put( prefix + "." + Constants.WILDCARD, Boolean.TRUE );
			}
		}

		// Intermediate step: deal with facet variables
		Set<String> facetVariables = new HashSet<String> ();
		for( Facet f : graphs.getFacetNameToFacet().values()) {
			facetVariables.addAll( f.exportedVariables.keySet());
			facetVariables.add( f.getName() + "." + Constants.WILDCARD );
		}

		// Are all the imports and exports resolvable?
		for( Map.Entry<String,Boolean> entry : importedVariableNameToExported.entrySet()) {

			// Resolved. Great!
			if( entry.getValue())
				continue;

			// Maybe it is a facet variable, with no component associated with this facet.
			// This check is useful for recipes.
			ErrorCode errorCode = ErrorCode.RM_UNRESOLVABLE_VARIABLE;
			if( facetVariables.contains( entry.getKey()))
				errorCode = ErrorCode.RM_UNRESOLVABLE_FACET_VARIABLE;

			// Add an error about unknown variable
			for( Component component : importedVariableToImporters.get( entry.getKey()))
				errors.add( new ModelError( errorCode, component, "Variable name: " + entry.getKey()));
		}

		return errors;
	}


	/**
	 * Validates an instance.
	 * @param instance an instance (not null)
	 * @return a non-null list of errors
	 */
	public static Collection<ModelError> validate( Instance instance ) {

		// Check the name
		Collection<ModelError> errors = new ArrayList<ModelError> ();
		if( Utils.isEmptyOrWhitespaces( instance.getName()))
			errors.add( new ModelError( ErrorCode.RM_EMPTY_INSTANCE_NAME, instance ));
		else if( ! instance.getName().matches( ParsingConstants.PATTERN_FLEX_ID ))
			errors.add( new ModelError( ErrorCode.RM_INVALID_INSTANCE_NAME, instance, "Instance name: " + instance.getName()));

		// Check exports
		if( instance.getComponent() == null )
			errors.add( new ModelError( ErrorCode.RM_EMPTY_INSTANCE_COMPONENT, instance ));

		// Check that it has a valid parent with respect to the graph
		if( instance.getComponent() != null ) {
			ErrorCode errorCode = null;
			Collection<Component> ancestors = ComponentHelpers.findAllAncestors( instance.getComponent());

			if( instance.getParent() == null
					&& ! ancestors.isEmpty())
				errorCode = ErrorCode.RM_MISSING_INSTANCE_PARENT;

			else if( instance.getParent() != null ) {
				if( ! ancestors.contains( instance.getParent().getComponent())
						|| ! ComponentHelpers.findAllChildren( instance.getParent().getComponent()).contains( instance.getComponent()))
					errorCode = ErrorCode.RM_INVALID_INSTANCE_PARENT;
			}

			if( errorCode != null ) {
				StringBuilder sb = new StringBuilder( "One of the following parent was expected: " );
				for( Iterator<Component> it = ancestors.iterator(); it.hasNext(); ) {
					sb.append( it.next().getName());
					if( it.hasNext())
						sb.append( ", " );
				}

				errors.add( new ModelError( errorCode, instance, sb.toString()));
			}
		}

		// Check overridden exports
		// Overridden variables may not contain the facet or component prefix.
		// To remain as flexible as possible, we will try to resolve them as component or facet variables.
		Map<String,Set<String>> localNameToFullNames = new HashMap<String,Set<String>> ();
		Set<String> inheritedVarNames;
		if( instance.getComponent() != null )
		 inheritedVarNames = ComponentHelpers.findAllExportedVariables( instance.getComponent()).keySet();
		else
			inheritedVarNames = new HashSet<>( 0 );
		
		for( String inheritedVarName : inheritedVarNames ) {
			String localName = VariableHelpers.parseVariableName( inheritedVarName ).getValue();
			Set<String> fullNames = localNameToFullNames.get( localName );
			if( fullNames == null )
				fullNames = new HashSet<String> ();

			fullNames.add( inheritedVarName );
			localNameToFullNames.put( localName, fullNames );
		}

		for( Map.Entry<String,String> entry : instance.overriddenExports.entrySet()) {

			// The overridden export is complete: Tomcat.port = ...
			if( inheritedVarNames.contains( entry.getKey()))
					continue;

			// The export is incomplete or does not override anything...
			Set<String> fullNames = localNameToFullNames.get( entry.getKey());
			if( fullNames == null ) {
				errors.add( new ModelError( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE, instance, "Variable name: " + entry.getKey()));

			} else if( fullNames.size() > 1 ) {
				StringBuilder sb = new StringBuilder();
				sb.append( "Variable '" );
				sb.append( entry.getKey());
				sb.append( "' could mean " );

				for( Iterator<String> it = fullNames.iterator(); it.hasNext(); ) {
					sb.append( it.next());
					if( it.hasNext())
						sb.append( ", " );
				}

				errors.add( new ModelError( ErrorCode.RM_AMBIGUOUS_OVERRIDING, instance, sb.toString()));
			}
		}

		return errors;
	}


	/**
	 * Validates a collection of instances.
	 * @param instances a non-null collection of instances
	 * @return a non-null list of errors
	 */
	public static Collection<ModelError> validate( Collection<Instance> instances ) {

		Collection<ModelError> errors = new ArrayList<ModelError> ();
		for( Instance i : instances )
			errors.addAll( validate( i ));

		return errors;
	}


	/**
	 * Validates an application.
	 * @param app an application template (not null)
	 * @return a non-null list of errors
	 */
	public static Collection<ModelError> validate( ApplicationTemplate app ) {

		Collection<ModelError> errors = new ArrayList<ModelError> ();
		if( Utils.isEmptyOrWhitespaces( app.getName()))
			errors.add( new ModelError( ErrorCode.RM_MISSING_APPLICATION_NAME, app ));

		if( Utils.isEmptyOrWhitespaces( app.getQualifier()))
			errors.add( new ModelError( ErrorCode.RM_MISSING_APPLICATION_QUALIFIER, app ));

		if( app.getGraphs() == null )
			errors.add( new ModelError( ErrorCode.RM_MISSING_APPLICATION_GRAPHS, app ));
		else
			errors.addAll( validate( app.getGraphs()));

		errors.addAll( validate( InstanceHelpers.getAllInstances( app )));
		return errors;
	}


	/**
	 * Validates an application descriptor.
	 * @param descriptor a descriptor
	 * @return a non-null list of errors
	 */
	public static Collection<ModelError> validate( ApplicationTemplateDescriptor descriptor ) {

		Collection<ModelError> errors = new ArrayList<ModelError> ();
		if( Utils.isEmptyOrWhitespaces( descriptor.getName()))
			errors.add( new ModelError( ErrorCode.RM_MISSING_APPLICATION_NAME, descriptor ));

		if( Utils.isEmptyOrWhitespaces( descriptor.getQualifier()))
			errors.add( new ModelError( ErrorCode.RM_MISSING_APPLICATION_QUALIFIER, descriptor ));

		if( Utils.isEmptyOrWhitespaces( descriptor.getDslId()))
			errors.add( new ModelError( ErrorCode.RM_MISSING_APPLICATION_DSL_ID, descriptor ));

		if( Utils.isEmptyOrWhitespaces( descriptor.getGraphEntryPoint()))
			errors.add( new ModelError( ErrorCode.RM_MISSING_APPLICATION_GEP, descriptor ));

		return errors;
	}
}
