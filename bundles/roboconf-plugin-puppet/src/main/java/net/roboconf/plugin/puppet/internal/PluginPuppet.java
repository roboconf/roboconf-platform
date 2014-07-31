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

package net.roboconf.plugin.puppet.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * The plug-in executes a Puppet manifests.
 * <p>
 * WARNING: when this plug-in is used on a local IaaS / host,
 * only the initialization will work. Other actions that require root
 * permissions will fail. They may work on a real IaaS because the agent
 * (which runs this plug-in) must be started with an "init.d" script. Thus,
 * it automatically inherits root permissions.
 * </p>
 * <p>
 * Modules will be installed automatically during the initialization.
 * Although there can be several manifests into the "manifests" directory,
 * only "init.pp" will be used. Other should be referenced through includes.
 * </p>
 * <p>
 * The best solution is to use the default template to mutualize actions.
 * Thus, start and stop can be achieved through a same script that will either
 * have the running state to RUNNING or to STOPPED.
 * </p>
 * <p>
 * The action is one of "deploy", "start", "stop", "undeploy" and "update".<br />
 * Let's take an example with the "start" action to understand the way this plug-in works.
 * </p>
 * <ul>
 * 	<li>The plug-in will load manifests/start.pp</li>
 * 	<li>If it is not found, it will try to load templates/start.pp.template</li>
 * 	<li>If it is not found, it will try to load templates/default.pp.template</li>
 * 	<li>If it is not found, the plug-in will do nothing</li>
 * </ul>
 * <p>
 * The default template is used to factorize actions.
 * </p>
 *
 * @author Noël - LIG
 * @author Vincent Zurczak - Linagora
 * @author Christophe Hamerling - Linagora
 */
public class PluginPuppet implements PluginInterface {

	private static final String MANIFESTS_FOLDER = "manifests";
	//private static final String TEMPLATES_FOLDER = "roboconf_templates";

	private final Logger logger = Logger.getLogger(getClass().getName());
	private String agentId;



	@Override
	public String getPluginName() {
		return "puppet";
	}


	@Override
	public void setNames( String applicationName, String rootInstanceName ) {
		this.agentId = "'" + rootInstanceName + "' agent";
	}


	@Override
	public void initialize( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is initializing the plug-in for " + instance.getName());
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		try {
			installPuppetModules(instance, instanceDirectory);

		} catch( IOException e ) {
			throw new PluginException( e );

		} catch( InterruptedException e ) {
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void deploy( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is deploying instance " + instance.getName());
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		try {
			callPuppetScript( instance, "deploy", PuppetState.STOPPED, null, false, instanceDirectory );

		} catch( IOException e ) {
			throw new PluginException( e );

		} catch( InterruptedException e ) {
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void start( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is starting instance " + instance.getName());
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		try {
			callPuppetScript( instance, "start", PuppetState.RUNNING, null, false, instanceDirectory );

		} catch( IOException e ) {
			throw new PluginException( e );

		} catch( InterruptedException e ) {
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void update(Instance instance, Import importChanged, InstanceStatus statusChanged) throws PluginException {

		this.logger.fine( this.agentId + " is updating instance " + instance.getName());
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		try {
			callPuppetScript(
					instance, "update",
					PuppetState.UNDEF,
					importChanged,
					(statusChanged == InstanceStatus.DEPLOYED_STARTED),
					instanceDirectory );

		} catch( IOException e ) {
			throw new PluginException( e );

		} catch( InterruptedException e ) {
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void stop( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is stopping instance " + instance.getName());
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		try {
			callPuppetScript( instance, "stop", PuppetState.STOPPED, null, false, instanceDirectory );

		} catch( IOException e ) {
			throw new PluginException( e );

		} catch( InterruptedException e ) {
			this.logger.finest( Utils.writeException( e ));
		}
	}


	@Override
	public void undeploy( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is undeploying instance " + instance.getName());
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		try {
			callPuppetScript( instance, "undeploy", PuppetState.UNDEF, null, false, instanceDirectory );

		} catch( IOException e ) {
			throw new PluginException( e );

		} catch( InterruptedException e ) {
			this.logger.finest( Utils.writeException( e ));
		}
	}


	/**
	 * Executes a Puppet command to install the required modules.
	 * @param instance the instance
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void installPuppetModules( Instance instance, File instanceDirectory )
	throws IOException, InterruptedException {

		// Load the modules names
		File modulesFile = new File( instanceDirectory, "modules.properties" );
		if( ! modulesFile.exists())
			return;

		Properties props = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream( modulesFile );
			props.load( in );

		} finally {
			Utils.closeQuietly( in );
		}

        File realInstanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent(instance, getPluginName());
		for( Map.Entry<Object,Object> entry : props.entrySet()) {

			List<String> commands = new ArrayList<String> ();
			commands.add( "puppet" );
			commands.add( "module" );
			commands.add( "install" );

			String value = entry.getValue() == null ? null : entry.getValue().toString();
			if( ! Utils.isEmptyOrWhitespaces( value )) {
				commands.add( "--version" );
				commands.add( value );
			}

			commands.add((String) entry.getKey());
			commands.add( "--target-dir" );
			commands.add( realInstanceDirectory.getAbsolutePath());

			String[] params = commands.toArray( new String[ 0 ]);
			this.logger.fine( "Module installation: " + Arrays.toString( params ));
			ProgramUtils.executeCommand( this.logger, commands, null );
		}
	}


	/**
	 * Invokes Puppet to inject variables into the instance's manifests.
	 * @param instance the instance
     * @param action the name of the action to run
	 * @param puppetState a Puppet state
	 * @param importChanged The import that changed (added or removed) upon update
	 * @param importAdded true if the changed import is added, false if it is removed
     * @param instanceDirectory where to find instance files
     * FIXME: split this method for testability
	 */
	private void callPuppetScript( Instance instance, String action, PuppetState puppetState, Import importChanged, boolean importAdded, File instanceDirectory )
	throws IOException, InterruptedException {

		if( instance == null
				|| instanceDirectory == null
				|| ! instanceDirectory.exists()
				|| ! instanceDirectory.isDirectory()) {
			this.logger.fine("Ignoring null instance" + (instance != null ? " directory" : ""));
			return;
		}

		// Find the action to execute
        // If not found, try init.pp
		this.logger.info("Preparing the invocation of the script for " + action + " and instance " + instance.getName() + ".");

		File moduleDirectory = null;
		for (File f : instanceDirectory.listFiles()) {
			if(f.isDirectory() && f.getName().startsWith("roboconf_")) {
				moduleDirectory = f;
				break;
			}
		}

		if(moduleDirectory != null) {
			String clazz = moduleDirectory.getName() + "::" + action;
			File scriptFile = new File(moduleDirectory, MANIFESTS_FOLDER + "/" + action + ".pp");

			if(! scriptFile.exists()) {
				clazz = moduleDirectory.getName();
				scriptFile = new File(moduleDirectory, MANIFESTS_FOLDER + "/init.pp");
			}

			if(scriptFile.exists()) {
		        // Prepare the command and execute it
				List<String> commands = new ArrayList<String> ();
				commands.add( "puppet" );
				commands.add( "apply" );
				commands.add( "--verbose" );

				String modpath = System.getenv("MODULEPATH");
				if(modpath != null)
					modpath += (modpath.endsWith(File.pathSeparator) ? "" : File.pathSeparator);
				else
					modpath = "";

				modpath += instanceDirectory.getAbsolutePath();
				commands.add( "--modulepath" );
				commands.add(modpath);

				commands.add( "--execute" );
				commands.add( generateCodeToExecute(clazz, instance, puppetState, importChanged, importAdded));

				String[] params = commands.toArray( new String[ 0 ]);
				this.logger.fine( "Module installation: " + Arrays.toString( params ));
				ProgramUtils.executeCommand( this.logger, commands, null );
			}
		}
	}


	/**
	 * Generates the code to be injected by Puppet into the manifest.
	 * @param instance the instance
	 * @param puppetState the Puppet state
	 * @param importChanged The import that changed (added or removed) upon update
	 * @param importAdded true if the changed import is added, false if it is removed
	 * @return a non-null string
	 */
	String generateCodeToExecute( String className, Instance instance, PuppetState puppetState, Import importChanged, boolean importAdded ) {

		// When executed by hand, the "apply" command would expect
		// this string to be returned to be between double quotes.
		// Example: "class{ 'roboconf_redis': ... }"

		// However, this does not work when executed from a Process builder.
		// The double quotes must be removed so that it works.

		StringBuilder sb = new StringBuilder();
		sb.append( "class{'" );
		sb.append( className );
		sb.append( "': runningState => " );
		sb.append( puppetState.toString().toLowerCase());

		// Prepare the injection of variables into the Puppet receipt
		String args = formatExportedVariables( instance.getExports());
		String importedTypes = formatInstanceImports( instance );

		if( ! Utils.isEmptyOrWhitespaces( args ))
			sb.append( ", " + args );

		if( ! Utils.isEmptyOrWhitespaces( importedTypes ))
			sb.append( ", " + importedTypes );

		if(importChanged != null) {
			sb.append(", "
					+ (importAdded ? "importAdded => {" : "importRemoved => {")
					+ formatImport(importChanged) + "}");
		}

		sb.append("}");
		return sb.toString();
	}


	/**
	 * Returns a String representing all the exported variables and their value.
	 * <p>
	 * Must be that way:<br />
	 * {@code varName1 => 'varValue1', varName2 => undef, varName3 => 'varValue3'}
	 * </p>
	 * <p>
	 * It is assumed the prefix of the exported variable (component or facet name)
	 * is not required.
	 * </p>
	 * <p>
	 * As an example...<br />
	 * Export "Redis.port = 4040" will generate "port => 4040".<br />
	 * Export "Redis.port = null" will generate "port => undef".
	 * </p>
	 *
	 * @param instanceExports the instance
	 * @return a non-null string
	 */
	String formatExportedVariables( Map<String,String> instanceExports ) {

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for( Entry<String,String> entry : instanceExports.entrySet()) {
			if( first )
				first = false;
			else
				sb.append(", ");

			String vname = VariableHelpers.parseVariableName( entry.getKey()).getValue();
			sb.append( vname.toLowerCase() );
			sb.append( " => " );
			if( Utils.isEmptyOrWhitespaces( entry.getValue()))
				sb.append( "undef" );
			else
				sb.append( "'" + entry.getValue() + "'" );
		}

		return sb.toString();
	}

	/**
	 * Returns a String representing all the imports and their values.
	 * <p>
	 * Must be that way:
	 * <code>
	 * { importTypeName => { 'importTypeName11' => { 'varName1' => 'varValue1', 'varName2' => 'varValue2' },
	 * 'importTypeName12' => { 'varName1' => 'varValue1', 'varName2' => 'varValue2' } }, $importTypeName2 => undef }
	 * </code>
	 * </p>
	 *
	 * @param instance the instance
	 * @return a non-null string
	 */
	String formatInstanceImports( Instance instance ) {

		StringBuilder sb = new StringBuilder();

		boolean first = true;
		for( String facetOrComponentName : VariableHelpers.findPrefixesForImportedVariables( instance )) {
			if( first )
				first = false;
			else
				sb.append(", ");

			// Declare the first ImportedVar,
			// Eg: "$workers = ..."
			sb.append( facetOrComponentName.toLowerCase() );
			sb.append( " => " );

			Collection<Import> imports = instance.getImports().get( facetOrComponentName );
			if( imports == null || imports.isEmpty()) {
				// No component has exported the variable this component expected.
				// put "undef". Example: "$workers = undef"
				sb.append("undef");

			} else {
				// The component has received configurations from the others.
				// Eg: "$workers = { 'workers1' => {...} , 'workers2' => {...} }"
				sb.append( "{ " );

				for( Iterator<Import> it = imports.iterator(); it.hasNext(); ) {
					sb.append( formatImport( it.next()));

					if( it.hasNext())
						sb.append(", ");
				}

				sb.append( "}" );
			}
		}

		return sb.toString();
	}

	private String formatImport(Import imp) {
		StringBuilder sb = new StringBuilder();
		sb.append( "'" );
		sb.append(imp.getInstancePath());
		sb.append( "' => { "  );
		sb.append( formatExportedVariables( imp.getExportedVars()));
		sb.append(" }");
		return sb.toString();
	}

	/**
	 * The running states for Puppet.
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum PuppetState {
		RUNNING, STOPPED, UNDEF;

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		};
	}
}
