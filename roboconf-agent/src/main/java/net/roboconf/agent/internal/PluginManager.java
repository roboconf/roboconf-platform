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

package net.roboconf.agent.internal;

import java.io.File;
import java.util.logging.Logger;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.plugin.api.ExecutionLevel;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;
import net.roboconf.plugin.bash.PluginBash;
import net.roboconf.plugin.logger.PluginLogger;
import net.roboconf.plugin.puppet.PluginPuppet;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginManager {

	private ExecutionLevel executionLevel = ExecutionLevel.RUNNING;
	private File dumpDirectory;


	/**
	 * Finds the right plug-in.
	 * @param instance an instance (not null)
	 * @param logger a logger (not null)
	 * @return the right plug-in, or null if none was found
	 */
	public PluginInterface findPlugin( Instance instance, Logger logger ) {

		PluginInterface result = null;
		String installerName = instance.getComponent().getInstallerName();
		PluginInterface[] plugins = new PluginInterface[] {
				new PluginBash(),
				new PluginPuppet(),
				new PluginLogger()
		};

		for( PluginInterface pi : plugins ) {
			if( pi.getPluginName().equalsIgnoreCase( installerName )) {
				result = pi;
				break;
			}
		}

		if( result == null ) {
			logger.severe( "No plugin was found for instance " + instance.getName() + " with installer " + installerName + "." );

		} else {
			result.setExecutionLevel( this.executionLevel );
			result.setDumpDirectory( this.dumpDirectory );
			result.setAgentName( "Agent " + InstanceHelpers.findRootInstance( instance ).getName());
		}

		return result;
	}


	/**
	 * @param executionLevel the executionLevel to set
	 */
	public void setExecutionLevel( ExecutionLevel executionLevel ) {
		this.executionLevel = executionLevel;
	}


	/**
	 * @return the execution level
	 */
	public ExecutionLevel getExecutionLevel() {
		return this.executionLevel;
	}


	/**
	 * @param dumpDirectory the dumpDirectory to set.
	 * p>
	 * Only required if execution level is
	 * {@link ExecutionLevel#GENERATE_FILES}.
	 * </p>
	 */
	public void setDumpDirectory( File dumpDirectory ) {
		this.dumpDirectory = dumpDirectory;
	}


	/**
	 * Initializes the plug-in for a given instance.
	 * <p>
	 * Plug-ins may require to be initialized.
	 * As an example, the Puppet plug-ins may have to install modules
	 * so that the manifests can run. If the plug-in initialization fails,
	 * then the other plug-ins actions will not work.
	 * </p>
	 * <p>
	 * If the instance to add contains children, the initialization
	 * is also performed on the children.
	 * </p>
	 *
	 * @param instanceToAdd the instance to add on this agent
	 * @param executionLevel the execution level
	 * @throws PluginException if the initialization fails or if no plug-in was found
	 */
	public static void initializePluginForInstance( Instance instanceToAdd, ExecutionLevel executionLevel )
	throws PluginException {

		Logger logger = Logger.getLogger( PluginManager.class.getName());
		PluginManager pluginManager = new PluginManager();
		pluginManager.setExecutionLevel( executionLevel );

		for( Instance instance : InstanceHelpers.buildHierarchicalList( instanceToAdd )) {

			String installerName = instance.getComponent().getInstallerName();
			PluginInterface plugin = pluginManager.findPlugin( instance, logger );
			if( plugin == null )
				throw new PluginException( "No plugin was found for " + instance.getName() + ". Installer name:" + installerName );

			plugin.initialize( instance );
		}
	}
}
