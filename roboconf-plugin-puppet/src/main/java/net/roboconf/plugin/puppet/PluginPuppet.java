/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.plugin.puppet;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.ProgramUtils;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.plugin.api.ExecutionLevel;
import net.roboconf.plugin.api.PluginInterface;

/**
 * TODO: review and update the private methods... :(
 * @author Noël - LIG
 */
public class PluginPuppet implements PluginInterface {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private ExecutionLevel executionLevel;
	private File dumpDirectory;
	private String agentName;


	@Override
	public void setExecutionLevel( ExecutionLevel executionLevel ) {
		this.executionLevel = executionLevel;
	}


	@Override
	public void setDumpDirectory( File dumpDirectory ) {
		this.dumpDirectory = dumpDirectory;
	}


	@Override
	public void setAgentName( String agentName ) {
		this.agentName = agentName;
	}


	@Override
	public void deploy( Instance instance ) throws Exception {
		this.logger.fine( this.agentName + " is deploying instance " + instance.getName());
		callPuppetScript( instance, "stopped" );
	}


	@Override
	public void start( Instance instance ) throws Exception {
		this.logger.fine( this.agentName + " is starting instance " + instance.getName());
		callPuppetScript( instance, "running" );
	}


	@Override
	public void update( Instance instance ) throws Exception {
		this.logger.fine( this.agentName + " is updating instance " + instance.getName());
		callPuppetScript( instance, "undef" );
	}



	@Override
	public void stop( Instance instance ) throws Exception {
		this.logger.fine( this.agentName + " is stopping instance " + instance.getName());
		callPuppetScript( instance, "stopped" );
	}


	@Override
	public void undeploy( Instance instance ) throws Exception {
		// TODO
	}


	/**
	 * Actualy call the Puppet script.
	 * @param instance
	 * @param serviceState either 'running', 'stopped' or 'undef'
	 * @throws Exception
	 */
	private void callPuppetScript(Instance instance, String serviceState) throws Exception {
		// TODO modulepath... requires a constant ??
		// Beggining of the command to execute
		String[] beginningOfCommand = { "puppet", "apply", "--verbose", "--modulepath", "/tmp/roboconf/puppet/modules", /*"--noop",*/"--execute" };
		// Puppet modules have to be in the folder /etc/puppet/modules... unless --modulepath specifies another place.

		// Other part of the command
		String className = instance.getComponent().getName().toLowerCase();
		StringBuilder sb = new StringBuilder();
		sb.append("class{'" + className + "': runningState => " + serviceState);

		// Set vars that will be used in Puppet recipe
		String args = varsToString(instance);
		String importedType = importedTypeToString(instance);
		if (args != null & !args.equals("")) {
			sb.append(", " + args);
		}
		if (importedType != null & !importedType.equals("")) {
			sb.append(", " + importedType);
		}
		sb.append("}");
		String[] endOfCommand = { sb.toString() };

		// If it fails one time, try a second time
		// TODO it is just a trick to handle a Puppet bug, to remove in the future
		int exitValue = ProgramUtils.executeCommand( this.logger, concat(beginningOfCommand, endOfCommand), null );
		if( exitValue != 0 )
			ProgramUtils.executeCommand( this.logger, concat(beginningOfCommand, endOfCommand), null );

		// If it fails the second time, there's a problem
	}

	/**
	 * Return a String representing all vars of the component and their argument.
	 * Must be that way:
	 * {@code varName1 => 'varValue1', varName2 => undef, varName3 => 'varValue3'}
	 * @param instance
	 * @return
	 */
	private String varsToString(Instance instance) {
		// Output local vars that will be used in Puppet recipe to a String
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Entry<String, String> var : instance.getComponent().getExportedVariables().entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			//TODO assume left part of variable name (comp or facet name) is not required...
			String vname = VariableHelpers.parseVariableName(var.getKey()).getValue();
			sb.append(vname + " => ");
			if (var.getValue() == null || "".equals(var.getValue().trim())) {
				sb.append("undef");
			} else {
				sb.append("'" + var.getValue() + "'");
			}
		}
		return sb.toString();
	}

	/**
	 * Output vars to a single String.
	 * Must be that way:
	 * {@code importTypeName => { 'importTypeName11' => { 'varName1' => 'varValue1', 'varName2' => 'varValue2' }, 'importTypeName12' => { 'varName1' => 'varValue1', 'varName2' => 'varValue2' } }, $importTypeName2 => undef }
	 * @param instance
	 * @return
	 */
	private String importedTypeToString(Instance instance) {
		StringBuilder sb = new StringBuilder();
		Map<String, Collection<Import>> importedTypes = instance.getImports();
		boolean firstoffirstfor = true;
		for (String importType : importedTypes.keySet()) {
			// Little trick to handle not putting a ',' before the first var
			if (firstoffirstfor) {
				firstoffirstfor = false;
			} else {
				sb.append(", ");
			}
			// Declare the first ImportedVar,
			// Eg: "$workers = ..."
			sb.append(importType + " => ");
			if ("".equals(importType)) {
				// If nothing in the ImportedType, meaning that no other VM has exported its config,
				// put "undef".
				// Eg: "$workers = undef"
				sb.append("undef");
			} else {
				// If the component has vars, meaning that he has configuration from others.
				// Eg: "$workers = { 'workers1' => {...} , 'workers2' => {...} }"
				sb.append("{ ");

				Collection<Import> imports = importedTypes.get(importType);
				boolean first = true;
				for(Import imp : imports) {
					if(first) first = false;
					else sb.append(", ");

					int index = imp.getInstancePath().lastIndexOf( '/' );
					String instanceName = imp.getInstancePath().substring( index + 1 );
					sb.append("'" + instanceName + "' => { ");

					Map<String, String> varsOfImport = imp.getExportedVars();
					boolean firstofthirdfor = true;
					for (Entry<String, String> entry : varsOfImport.entrySet()) {
						if (firstofthirdfor) {
							firstofthirdfor = false;
						} else {
							sb.append(", ");
						}
						//TODO assume left part of variable name (comp or facet name) is not required...
						String vname = VariableHelpers.parseVariableName(entry.getKey()).getValue();
						sb.append("'" + vname + "' => '" + entry.getValue() + "'");
					}
					sb.append(" }");
				}

				sb.append(" }");
			}
		}
		return sb.toString();
	}

	/**
	 * Concat two arrays and return the result of this operation.
	 * @param first
	 * @param second
	 * @return
	 */
	private String[] concat(String[] first, String[] second) {
		String[] result = new String[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

}