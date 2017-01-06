/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.DockerAndScriptUtils;
import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;
import net.roboconf.plugin.script.internal.ScriptUtils.ActionFileFilter;
import net.roboconf.plugin.script.internal.templating.InstanceTemplateHelper;

/**
 * The plug-in invokes a script (eg. shell) on every life cycle change.
 * <p>
 * The action is one of "deploy", "start", "stop", "undeploy" and "update".<br>
 * Let's take an example with the "start" action to understand the way this plug-in works.
 * </p>
 * <ul>
 * 	<li>The plug-in will load scripts/start.sh</li>
 * 	<li>If it is not found, it will try to load templates/start.sh.template</li>
 * 	<li>If it is not found, it will try to load templates/default.sh.template</li>
 * 	<li>If it is not found, the plug-in will do nothing</li>
 * </ul>
 * <p>
 * The default template is used to factorize actions.
 * </p>
 *
 * @author Noël - LIG
 * @author Linh-Manh Pham - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Christophe Hamerling - Linagora
 */
public class PluginScript implements PluginInterface {

	public static final String PLUGIN_NAME = "script";
	private static final String SCRIPTS_FOLDER_NAME = "scripts";
	private static final String TEMPLATES_FOLDER_NAME = "roboconf-templates";
	private static final String FILES_FOLDER_NAME = "files";

	private final Logger logger = Logger.getLogger( getClass().getName());
	String agentId;
	String applicationName, scopedInstancePath;


	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}


	@Override
	public void setNames( String applicationName, String scopedInstancePath ) {
		this.applicationName = applicationName;
		this.scopedInstancePath = scopedInstancePath;
		this.agentId = "'" + scopedInstancePath + "' agent";
	}


	@Override
	public void initialize( Instance instance ) throws PluginException {
		this.logger.fine(this.agentId + ": initializing the plugin for " + instance);

		// All scripts deployed should be made executable (the agent is supposed to run as root)
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance );
		ScriptUtils.setScriptsExecutable( new File( instanceDirectory, SCRIPTS_FOLDER_NAME ));
	}


	@Override
	public void deploy( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is deploying instance " + instance );
		try {
			prepareAndExecuteCommand( "deploy", instance, null, null );

		} catch( Exception e ) {
			throw new PluginException( e );
		}
	}


	@Override
	public void start( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is starting instance " + instance );
		try {
			prepareAndExecuteCommand( "start", instance, null, null );

		} catch( Exception e ) {
			throw new PluginException( e );
		}
	}


	@Override
	public void update(Instance instance, Import importChanged, InstanceStatus statusChanged) throws PluginException {

		this.logger.fine( this.agentId + " is updating instance " + instance );
		try {
			prepareAndExecuteCommand( "update", instance, importChanged, statusChanged );

		} catch( Exception e ) {
			throw new PluginException( e );
		}
	}


	@Override
	public void stop( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is stopping instance " + instance );
		try {
			prepareAndExecuteCommand( "stop", instance, null, null );

		} catch( Exception e ) {
			throw new PluginException( e );
		}
	}


	@Override
	public void undeploy( Instance instance ) throws PluginException {

		this.logger.fine( this.agentId + " is undeploying instance " + instance );
		try {
			prepareAndExecuteCommand( "undeploy", instance, null, null );

		} catch( Exception e ) {
			throw new PluginException( e );
		}
	}


	private void prepareAndExecuteCommand(String action, Instance instance, Import importChanged, InstanceStatus statusChanged)
	throws IOException, InterruptedException {

		this.logger.info("Preparing the invocation of " + action + " script for instance " + instance );
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance );

		File scriptsFolder = new File(instanceDirectory, SCRIPTS_FOLDER_NAME);
		File templatesFolder = new File(instanceDirectory, TEMPLATES_FOLDER_NAME);

		// Look for action script (default <action>.sh, or any file that starts with <action>)
		File script = new File(scriptsFolder, action + ".sh");
		if(! script.exists()) {
			File[] foundFiles = scriptsFolder.listFiles(new ActionFileFilter(action));
			if(foundFiles != null && foundFiles.length > 0) {
				script = foundFiles[0];
				if(foundFiles.length > 1)
					this.logger.warning("More than one " + action + " script found: taking the 1st one, " + script.getName());
			}
		}

		File template = new File(templatesFolder, action + ".template");
		if( ! template.exists())
			template = new File(templatesFolder, "default.template");

		if (script.exists()) {
			executeScript(script, instance, importChanged, statusChanged, instanceDirectory.getAbsolutePath());

		} else if (template.exists()) {
			File generated = generateTemplate(template, instance);
			executeScript(generated, instance, importChanged, statusChanged, instanceDirectory.getAbsolutePath());
			Utils.deleteFilesRecursively( generated );

		} else {
			this.logger.warning("Can not find a script or a template for action " + action);
		}
	}


	/**
	 * Generates a file from the template and the instance.
	 * @param template
	 * @param instance
	 * @return the generated file
	 * @throws IOException
	 */
	protected File generateTemplate(File template, Instance instance) throws IOException {

		String scriptName = instance.getName().replace( "\\s+", "_" );
		File generated = File.createTempFile( scriptName, ".script");
		InstanceTemplateHelper.injectInstanceImports(instance, template, generated);

		return generated;
	}


	protected void executeScript(
			File script,
			Instance instance,
			Import importChanged,
			InstanceStatus statusChanged,
			String instanceDir )
	throws IOException, InterruptedException {

		String[] command = { script.getAbsolutePath() };
		if(! script.canExecute())
			script.setExecutable(true);

		Map<String, String> environmentVars = new HashMap<String, String>();
		Map<String, String> vars = ScriptUtils.formatExportedVars(instance);
		environmentVars.putAll(vars);

		Map<String, String> importedVars = ScriptUtils.formatImportedVars( instance );
		environmentVars.putAll( DockerAndScriptUtils.buildReferenceMap( instance ));
		environmentVars.putAll( importedVars );
		environmentVars.put("ROBOCONF_FILES_DIR", new File( instanceDir, FILES_FOLDER_NAME ).getAbsolutePath());

		// Upon update, retrieve the status of the instance that triggered the update.
		// Should be either DEPLOYED_STARTED or DEPLOYED_STOPPED...
		if( statusChanged != null )
			environmentVars.put("ROBOCONF_UPDATE_STATUS", statusChanged.toString());

		// Upon update, retrieve the import that changed
		// (removed when an instance stopped, or added when it started)
		if( importChanged != null ) {
			environmentVars.put("ROBOCONF_IMPORT_CHANGED_INSTANCE_PATH", importChanged.getInstancePath());
			environmentVars.put("ROBOCONF_IMPORT_CHANGED_COMPONENT", importChanged.getComponentName());
			for (Entry<String, String> entry : importChanged.getExportedVars().entrySet()) {
				// "ROBOCONF_IMPORT_CHANGED_ip=127.0.0.1"
				String vname = VariableHelpers.parseVariableName(entry.getKey()).getValue();
				importedVars.put("ROBOCONF_IMPORT_CHANGED_" + vname, entry.getValue());
			}
		}

		int exitCode = ProgramUtils.executeCommand( this.logger, command, script.getParentFile(), environmentVars, this.applicationName, this.scopedInstancePath );
		if( exitCode != 0 )
			throw new IOException( "Script execution failed. Exit code: " + exitCode );
	}
}
