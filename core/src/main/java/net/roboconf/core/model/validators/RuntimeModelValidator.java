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
import java.util.Map;
import java.util.Set;

import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;

/**
 * A set of methods to validate runtime model objects.
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelValidator {

	public static Collection<RoboconfError> validate( Component component ) {

		Collection<RoboconfError> result = new ArrayList<RoboconfError> ();
		if( Utils.isEmptyOrWhitespaces( component.getAlias())) {
			// TODO:
		}

		// A component cannot import variables it exports

		return result;
	}


	public static Collection<RoboconfError> validate( Graphs graphs ) {

		Collection<String> result = new ArrayList<String> ();
		if( graphs.getRootComponents().isEmpty()) {
			result.add( "No root component was found. A graph of Software elements must be oriented." );
		}

		// Validate all the components
		Set<Component> alreadyChecked = new HashSet<Component> ();
		Set<Component> toProcess = new HashSet<Component> ();
		Map<String,Boolean> importedVariableNameToExported = new HashMap<String,Boolean> ();

		toProcess.addAll( graphs.getRootComponents());
		while( ! toProcess.isEmpty()) {
			Component c = toProcess.iterator().next();
			toProcess.remove( c );
			if( alreadyChecked.contains( c ))
				continue;

			// result.addAll( validate( c ));
			alreadyChecked.add( c );
			toProcess.addAll( c.getChildren());

			for( String importedVariableName : c.getImportedVariableNames()) {
				if( ! importedVariableNameToExported.containsKey( importedVariableName ))
					importedVariableNameToExported.put( importedVariableName, Boolean.FALSE );
			}

			for( String exportedVariableName : c.getExportedVariables().keySet()) {
				importedVariableNameToExported.put( exportedVariableName, Boolean.TRUE );
			}
		}

		// Are all the imports and exports resolvable?


		// Cycles?


		return new ArrayList<RoboconfError> ();
	}


	public static Collection<RoboconfError> validate( Collection<Instance> instances ) {
		// TODO Auto-generated method stub
		return new ArrayList<RoboconfError> ();
	}


	public static Collection<RoboconfError> validate( ApplicationDescriptor descriptor ) {
		// TODO Auto-generated method stub
		return new ArrayList<RoboconfError> ();
	}
}
