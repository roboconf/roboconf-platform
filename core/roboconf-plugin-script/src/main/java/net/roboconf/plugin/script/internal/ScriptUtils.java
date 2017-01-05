/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.plugin.script.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ScriptUtils {

	/**
	 * Private constructor.
	 */
	private ScriptUtils() {
		// nothing
	}


	/**
	 * Formats imported variables to be used in a script as environment variables.
	 * @param instance the instance whose exported variables must be formatted
	 * @return a non-null map
	 */
	public static Map<String,String> formatExportedVars( Instance instance ) {

		// The map we will return
		Map<String, String> exportedVars = new HashMap<> ();

		// Iterate over the instance and its ancestors.
		// There is no loop in parent relations, so no risk of conflict in variable names.
		for( Instance inst = instance; inst != null; inst = inst.getParent()) {

			String prefix = "";
			if( inst != instance )
				prefix = "ANCESTOR_" + inst.getComponent().getName() + "_";

			Map<String,String> exports = InstanceHelpers.findAllExportedVariables( inst );
			for(Entry<String, String> entry : exports.entrySet()) {

				// FIXME: removing the prefix may result in inconsistencies when a facet
				// and a component export variables with the same "local name".
				// And this has nothing to do with ancestors. This is about inheritance.
				String vname = prefix + VariableHelpers.parseVariableName( entry.getKey()).getValue();
				vname = vname.replaceAll( "(-|%s)+", "_" );
				exportedVars.put( vname, entry.getValue());
			}
		}

		return exportedVars;
	}


	/**
	 * Formats imported variables to be used in a script as environment variables.
	 * <p>
	 * Simple vars are formatted a simple way ("myVarName=varValue"), while imported
	 * variables must be formatted in a more complex way.
	 * </p>
	 * <p>
	 * Taking the example of Apache needing workers in the example Apache-Tomcat-SQL,
	 * we chose to adopt the following style:
	 * <pre>
	 * ==========================
	 * "workers_size=3"
	 * "workers_0_name=tomcat1"
	 * "workers_0_ip=127.0.0.1"
	 * "workers_0_portAJP=8009"
	 * "workers_1_name=tomcat2"
	 * .....
	 * "workers_2_portAJP=8010"
	 * ==========================
	 * </pre>
	 * With this way of formatting vars, the script will know
	 * everything it needs to use these vars
	 * </p>
	 *
	 * @param instance the instance whose imports must be formatted
	 * @return a non-null map
	 */
	public static Map<String,String> formatImportedVars( Instance instance ) {

		// The map we will return
		Map<String, String> importedVars = new HashMap<String, String>();
		for( Map.Entry<String,Collection<Import>> entry : instance.getImports().entrySet()) {
			Collection<Import> importList = entry.getValue();
			String importTypeName = entry.getKey();

			// For each ImportType, put the number of Import it has, so the script knows
			importedVars.put(importTypeName + "_size", "" + importList.size());

			// Now put each var contained in an Import
			int i = 0;
			for( Import imprt : importList ) {
				// "workers_0_name=tomcat1"

				/*int index = imprt.getInstancePath().lastIndexOf( '/' );
                String instanceName = imprt.getInstancePath().substring( index + 1 );
                importedVars.put(importTypeName + "_" + i + "_name", instanceName);*/

				importedVars.put(importTypeName + "_" + i + "_name", imprt.getInstancePath());
				for (Entry<String, String> entry2 : imprt.getExportedVars().entrySet()) {
					// "workers_0_ip=127.0.0.1"
					String vname = VariableHelpers.parseVariableName(entry2.getKey()).getValue();
					importedVars.put(importTypeName + "_" + i + "_" + vname, entry2.getValue());
				}

				++i;
			}
		}

		return importedVars;
	}


	/**
	 * Recursively sets all files and directories executable, starting from a file or base directory.
	 * @param f a file to set executable or a base directory
	 */
	public static void setScriptsExecutable( File fileOrDir ) {

		List<File> files = new ArrayList<> ();
		if( fileOrDir.isDirectory())
			files.addAll( Utils.listAllFiles( fileOrDir, true ));
		else
			files.add( fileOrDir );

		for( File f : files )
			f.setExecutable( true );
	}


	/**
	 * @author Pierre-Yves Gibello - Linagora
	 */
	public static class ActionFileFilter implements FilenameFilter {
		final String prefix;

		public ActionFileFilter(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public boolean accept(File dir, String name) {
			return Utils.isEmptyOrWhitespaces( this.prefix ) ? false : name.startsWith(this.prefix);
		}
	}
}
