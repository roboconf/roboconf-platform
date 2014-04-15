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

package net.roboconf.core.model.validators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.parsing.ParsingConstants;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;

/**
 * A set of methods to validate runtime model objects.
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelValidator {

	/**
	 * Validates a component.
	 * @param component a component
	 * @return a non-null list of errors
	 */
	public static Collection<RoboconfError> validate( Component component ) {
		Collection<RoboconfError> errors = new ArrayList<RoboconfError> ();

		// Basic checks
		if( Utils.isEmptyOrWhitespaces( component.getName()))
			errors.add( new RoboconfError( ErrorCode.RM_EMPTY_COMPONENT_NAME ));
		else if( ! component.getName().matches( ParsingConstants.PATTERN_FLEX_ID ))
			errors.add( new RoboconfError( ErrorCode.RM_INVALID_COMPONENT_NAME ));

		if( Utils.isEmptyOrWhitespaces( component.getAlias()))
			errors.add( new RoboconfError( ErrorCode.RM_EMPTY_COMPONENT_ALIAS ));

		if( Utils.isEmptyOrWhitespaces( component.getInstallerName()))
			errors.add( new RoboconfError( ErrorCode.RM_EMPTY_COMPONENT_INSTALLER ));
		else if( ! component.getInstallerName().matches( ParsingConstants.PATTERN_FLEX_ID ))
			errors.add( new RoboconfError( ErrorCode.RM_INVALID_COMPONENT_INSTALLER ));

		// Facet names
		for( String facetName : component.getFacetNames()) {
			if( Utils.isEmptyOrWhitespaces( facetName )) {
				errors.add( new RoboconfError( ErrorCode.RM_EMPTY_FACET_NAME ));

			} else if( ! facetName.matches( ParsingConstants.PATTERN_FLEX_ID )) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_INVALID_FACET_NAME );
				error.setDetails( "Facet name: " + facetName );
				errors.add( error );
			}
		}

		// A component cannot import variables it exports unless these imports are optional.
		// This covers cluster uses cases (where an element may want to know where are the similar nodes).
		for( Map.Entry<String,Boolean> entry : component.getImportedVariables().entrySet()) {
			String var = entry.getKey();

			if( Utils.isEmptyOrWhitespaces( var )) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_EMPTY_VARIABLE_NAME );
				error.setDetails( "Variable name: " + var );
				errors.add( error );

			} else if( ! var.matches( ParsingConstants.PATTERN_ID )) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_INVALID_VARIABLE_NAME );
				error.setDetails( "Variable name: " + var );
				errors.add( error );
			}

			if( entry.getValue())
				continue;

			if( component.getExportedVariables().containsKey( var )) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS );
				error.setDetails( "Variable name: " + var );
				errors.add( error );
			}
		}

		// Exported variables must either start with the component name or a facet name
		for( String exportedVarName : component.getExportedVariables().keySet()) {
			List<String> prefixes = new ArrayList<String>( component.getFacetNames());
			prefixes.add( component.getName());
			Entry<String,String> varParts = VariableHelpers.parseVariableName( exportedVarName );

			if( Utils.isEmptyOrWhitespaces( exportedVarName )) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_EMPTY_VARIABLE_NAME );
				error.setDetails( "Variable name: " + exportedVarName );
				errors.add( error );

			} else if( ! exportedVarName.matches( ParsingConstants.PATTERN_ID )) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_INVALID_VARIABLE_NAME );
				error.setDetails( "Variable name: " + exportedVarName );
				errors.add( error );

			} else if( ! prefixes.contains( varParts.getKey())) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_INVALID_EXPORT_PREFIX );
				error.setDetails( "Variable name: " + exportedVarName );
				errors.add( error );

			} else if( Utils.isEmptyOrWhitespaces( varParts.getValue())) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_INVALID_EXPORT_NAME );
				error.setDetails( "Variable name: " + exportedVarName );
				errors.add( error );
			}
		}

		return errors;
	}


	/**
	 * Validates a graph.
	 * @param graphs a graphs instance
	 * @return a non-null list of errors
	 */
	public static Collection<RoboconfError> validate( Graphs graphs ) {

		Collection<RoboconfError> errors = new ArrayList<RoboconfError> ();
		if( graphs.getRootComponents().isEmpty())
			errors.add( new RoboconfError( ErrorCode.RM_NO_ROOT_COMPONENT ));

		// Validate all the components
		// Prepare the verification of variable matching
		Map<String,Component> alreadyChecked = new HashMap<String,Component> ();
		Set<Component> toProcess = new HashSet<Component> ();
		Map<String,Boolean> importedVariableNameToExported = new HashMap<String,Boolean> ();

		toProcess.addAll( graphs.getRootComponents());
		while( ! toProcess.isEmpty()) {
			Component c = toProcess.iterator().next();
			toProcess.remove( c );

			// Duplicate component?
			Component associatedComponent = alreadyChecked.get( c.getName());
			if( associatedComponent != null ) {
				if( associatedComponent != c ) {
					RoboconfError error = new RoboconfError( ErrorCode.RM_DUPLICATE_COMPONENT );
					error.setDetails( "Component name: " + c.getName());
					errors.add( error );
				}

				continue;
			}

			// Validate the component
			errors.addAll( validate( c ));
			alreadyChecked.put( c.getName(), c );
			toProcess.addAll( c.getChildren());

			// Process its variables
			for( String importedVariableName : c.getImportedVariables().keySet()) {
				if( ! importedVariableNameToExported.containsKey( importedVariableName ))
					importedVariableNameToExported.put( importedVariableName, Boolean.FALSE );
			}

			for( String exportedVariableName : c.getExportedVariables().keySet()) {
				importedVariableNameToExported.put( exportedVariableName, Boolean.TRUE );
			}
		}

		// Are all the imports and exports resolvable?
		for( Map.Entry<String,Boolean> entry : importedVariableNameToExported.entrySet()) {
			if( entry.getValue())
				continue;

			RoboconfError error = new RoboconfError( ErrorCode.RM_UNRESOLVABLE_VARIABLE );
			error.setDetails( "Variable name: " + entry.getKey());
			errors.add( error );
		}

		// Containment Cycles?
		for( Component c : graphs.getRootComponents()) {
			String s = ComponentHelpers.searchForLoop( c );
			if( s != null ) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_CYCLE_IN_COMPONENTS );
				error.setDetails( s );
				errors.add( error );
			}
		}

		return errors;
	}


	/**
	 * Validates an instance.
	 * @param instance an instance (not null)
	 * @return a non-null list of errors
	 */
	public static Collection<RoboconfError> validate( Instance instance ) {

		Collection<RoboconfError> errors = new ArrayList<RoboconfError> ();
		if( Utils.isEmptyOrWhitespaces( instance.getName()))
			errors.add( new RoboconfError( ErrorCode.RM_EMPTY_INSTANCE_NAME ));
		else if( ! instance.getName().matches( ParsingConstants.PATTERN_FLEX_ID ))
			errors.add( new RoboconfError( ErrorCode.RM_INVALID_INSTANCE_NAME ));

		if( instance.getComponent() == null )
			errors.add( new RoboconfError( ErrorCode.RM_EMPTY_INSTANCE_COMPONENT ));

		else for( String s : instance.getOverriddenExports().keySet()) {
			if( ! instance.getComponent().getExportedVariables().containsKey( s )) {
				RoboconfError error = new RoboconfError( ErrorCode.RM_MAGIC_INSTANCE_VARIABLE );
				error.setDetails( "Variable name: " + s );
				errors.add( error );
			}
		}

		return errors;
	}


	/**
	 * Validates a collection of instances.
	 * @param instances a non-null collection of instances
	 * @return a non-null list of errors
	 */
	public static Collection<RoboconfError> validate( Collection<Instance> instances ) {

		Collection<RoboconfError> errors = new ArrayList<RoboconfError> ();
		for( Instance i : instances )
			errors.addAll( validate( i ));

		return errors;
	}


	/**
	 * Validates an application.
	 * @param app an application (not null)
	 * @return a non-null list of errors
	 */
	public static Collection<RoboconfError> validate( Application app ) {

		Collection<RoboconfError> errors = new ArrayList<RoboconfError> ();
		if( Utils.isEmptyOrWhitespaces( app.getName()))
			errors.add( new RoboconfError( ErrorCode.RM_MISSING_APPLICATION_NAME ));

		if( Utils.isEmptyOrWhitespaces( app.getQualifier()))
			errors.add( new RoboconfError( ErrorCode.RM_MISSING_APPLICATION_QUALIFIER ));

		if( app.getGraphs() == null )
			errors.add( new RoboconfError( ErrorCode.RM_MISSING_APPLICATION_GRAPHS ));
		else
			errors.addAll( validate( app.getGraphs()));

		errors.addAll( validate( app.getRootInstances()));
		return errors;
	}


	/**
	 * Validates an application descriptor.
	 * @param descriptor a descriptor
	 * @return a non-null list of errors
	 */
	public static Collection<RoboconfError> validate( ApplicationDescriptor descriptor ) {

		Collection<RoboconfError> errors = new ArrayList<RoboconfError> ();
		if( Utils.isEmptyOrWhitespaces( descriptor.getName()))
			errors.add( new RoboconfError( ErrorCode.RM_MISSING_APPLICATION_NAME ));

		if( Utils.isEmptyOrWhitespaces( descriptor.getQualifier()))
			errors.add( new RoboconfError( ErrorCode.RM_MISSING_APPLICATION_QUALIFIER ));

		if( Utils.isEmptyOrWhitespaces( descriptor.getGraphEntryPoint()))
			errors.add( new RoboconfError( ErrorCode.RM_MISSING_APPLICATION_GEP ));

		return errors;
	}
}
