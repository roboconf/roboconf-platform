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

import net.roboconf.core.internal.utils.ProgramUtils;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.plugin.api.ExecutionLevel;
import net.roboconf.plugin.api.PluginInterface;

/**
 * A plug-in that executes Puppet manifests.
 * <p>
 * Modules will be installed automatically during the initialization.
 * Although there can be several manifests into the "manifests" directory,
 * only "init.pp" will be used. Other should be referenced through includes.
 * </p>
 *
 * @author Noël - LIG
 * @author Vincent Zurczak - Linagora
 * @author Christophe Hamerling - Linagora
 */
public class PluginPuppet implements PluginInterface {

	public static final String MODULES = "modules";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private ExecutionLevel executionLevel;
	private File dumpDirectory;
	private String agentName;



	@Override
	public String getPluginName() {
		return "puppet";
	}


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
	public void initialize( Instance instance ) throws Exception {

		this.logger.fine( this.agentName + " is initializing the plug-in for " + instance.getName());
		if( this.executionLevel == ExecutionLevel.LOG )
			return;

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		installPuppetModules(instance, instanceDirectory);
	}


	@Override
	public void deploy( Instance instance ) throws Exception {

		this.logger.fine( this.agentName + " is deploying instance " + instance.getName());
		if( this.executionLevel == ExecutionLevel.LOG )
			return;

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		callPuppetScript( instance, "deploy", PuppetState.STOPPED, instanceDirectory );
	}


	@Override
	public void start( Instance instance ) throws Exception {

		this.logger.fine( this.agentName + " is starting instance " + instance.getName());
		if( this.executionLevel == ExecutionLevel.LOG )
			return;

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		callPuppetScript( instance, "start", PuppetState.RUNNING, instanceDirectory );
	}


	@Override
	public void update( Instance instance ) throws Exception {

		this.logger.fine( this.agentName + " is updating instance " + instance.getName());
		if( this.executionLevel == ExecutionLevel.LOG )
			return;

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		callPuppetScript( instance, "update", PuppetState.UNDEF, instanceDirectory );
	}



	@Override
	public void stop( Instance instance ) throws Exception {

		this.logger.fine( this.agentName + " is stopping instance " + instance.getName());
		if( this.executionLevel == ExecutionLevel.LOG )
			return;

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		callPuppetScript( instance, "stop", PuppetState.STOPPED, instanceDirectory );
	}


	@Override
	public void undeploy( Instance instance ) throws Exception {

		this.logger.fine( this.agentName + " is undeploying instance " + instance.getName());
		if( this.executionLevel == ExecutionLevel.LOG )
			return;

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName());
		callPuppetScript( instance, "undeploy", PuppetState.UNDEF, instanceDirectory );
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

		// Install them
		File modulesDirectory = new File(
				InstanceHelpers.findInstanceDirectoryOnAgent( instance, getPluginName()),
				MODULES );

		if( ! modulesDirectory.exists()
				&& ! modulesDirectory.mkdirs())
			throw new IOException( "The modules directory could not be created for instance " + instance.getName() + "." );

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
			commands.add( modulesDirectory.getAbsolutePath());

			if( this.executionLevel == ExecutionLevel.LOG ) {
				String[] params = commands.toArray( new String[ 0 ]);
				this.logger.info( "Module installation: " + Arrays.toString( params ));

			} else {
				ProgramUtils.executeCommand( this.logger, commands, null );
			}
		}
	}


	/**
	 * Invokes Puppet to inject variables into the instance's manifests.
	 * @param instance the instance
     * @param step the running step
	 * @param puppetState a Puppet state
     * @param instanceDirectory where to find instance files
	 */
	private void callPuppetScript( Instance instance, String step, PuppetState puppetState, File instanceDirectory )
	throws IOException, InterruptedException {
        if (step == null) {
            final String msg = "Puppet manifest to apply can not be null for instance " + instance.getName();
            this.logger.warning(msg);
            return;
        }

		File modulesDirectory = new File( instanceDirectory, MODULES );
		File manifestFile = new File( instanceDirectory, "manifests/" + step + ".pp" );
		if( ! manifestFile.exists()
				|| ! manifestFile.isFile()) {
            this.logger.warning("Puppet manifest " + manifestFile + " has not been found for instance " + instance.getName());
            return;
        }

		List<String> commands = new ArrayList<String> ();
		commands.add( "puppet" );
		commands.add( "apply" );
		commands.add( "--verbose" );
		commands.add( "--modulepath" );
		commands.add( modulesDirectory.getAbsolutePath());
		commands.add( "--execute" );
		commands.add( generateCodeToExecute( instance, puppetState ));
		commands.add( manifestFile.getAbsolutePath());

		if( this.executionLevel == ExecutionLevel.LOG ) {
			String[] params = commands.toArray( new String[ 0 ]);
			this.logger.info( "Module installation: " + Arrays.toString( params ));

		} else {
			ProgramUtils.executeCommand( this.logger, commands, null );
		}
	}


	/**
	 * Generates the code to be injected by Puppet into the manifest.
	 * @param instance the instance
	 * @param puppetState the Puppet state
	 * @return a non-null string
	 */
	String generateCodeToExecute( Instance instance, PuppetState puppetState ) {

		String className = "roboconf_" + instance.getComponent().getName().toLowerCase();
		StringBuilder sb = new StringBuilder();
		sb.append( "class{'" );
		sb.append( className );
		sb.append( "': runningState => " );
		sb.append( puppetState.toString());

		// Prepare the injection of variables into the Puppet receipt
		String args = formatExportedVariables( instance.getExports());
		String importedTypes = formatInstanceImports( instance );

		if( ! Utils.isEmptyOrWhitespaces( args ))
			sb.append( ", " + args );

		if( ! Utils.isEmptyOrWhitespaces( importedTypes ))
			sb.append( ", " + importedTypes );

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
			sb.append( vname );
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
	 * {@code importTypeName => { 'importTypeName11' => { 'varName1' => 'varValue1', 'varName2' => 'varValue2' }, 'importTypeName12' => { 'varName1' => 'varValue1', 'varName2' => 'varValue2' } }, $importTypeName2 => undef }
	 * </p>
	 *
	 * @param instance the instance
	 * @return a non-null string
	 */
	String formatInstanceImports( Instance instance ) {

		StringBuilder sb = new StringBuilder();

		boolean first = true;
		for( String facetOrComponentName : VariableHelpers.findImportedVariablePrefixes( instance )) {
			if( first )
				first = false;
			else
				sb.append(", ");

			// Declare the first ImportedVar,
			// Eg: "$workers = ..."
			sb.append( facetOrComponentName );
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
					Import imp = it.next();

					int index = imp.getInstancePath().lastIndexOf( '/' );
					String instanceName = imp.getInstancePath().substring( index + 1 );
					sb.append( "'" );
					sb.append( instanceName );
					sb.append( "' => { "  );
					sb.append( formatExportedVariables( imp.getExportedVars()));
					sb.append(" }");

					if( it.hasNext())
						sb.append(", ");
				}

				sb.append( "}" );
			}
		}

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